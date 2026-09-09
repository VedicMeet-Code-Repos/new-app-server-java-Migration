# Consultant Discovery — Porting Guide (Phase 2.1)

Companion to `WEEK3_DISCOVERY_SPEC.md`. This is the exact Node→Java source map so each
step can be ported and **harness-diff-verified on the TEST server** (the repo's rule: pricing/
ranking is high-stakes and must never be shipped blind). Node source root:
`D:\SHIVAM_VEDIC_MEET_WORK\new-app-server` (branch: production is source of truth).

## Status
- ✅ **Step 1 — `checkUserHaveMembership`** → `discovery/MembershipService.java` (DONE, faithful 1:1).
  Added `Collections.WALLET_TRANSACTIONS = "wallet_transactions"` to `AppConstants`.
  Leaf, no callers yet; discovery controllers call `applyMembership(user)` after `AuthUserService.load(...)`.
- ✅ **Step 2 — `topConsultant`** → `discovery/ConsultantRankingService.java` (DONE, faithful; diff-pending).
  Added `Collections.BROADCASTS`. Preserves parseInt-truncated rating thresholds, `totalRatig` misspelling, NaN revenuePercent.
- ✅ **Step 4a — `cons/get-list`** → `discovery/ConsDiscoveryService.getList` + `DiscoveryController` (DONE, faithful; diff-pending).
  Added `Collections.USER_CONS_RELS`. Returns Node's RAW `{success, data:{list, pagination}}` shape (not the envelope).
- ✅ **Step 4b — `consultant_details`** → `discovery/ConsultantDetailsService` + `UserDiscoveryController` (DONE, faithful; diff-pending).
  Added `Collections.FOLLOWS`, `REVIEW_REPLIES`. 5 aggregations via `Document.parse` text blocks (main details, similar, review facet,
  order history) with placeholders; per-consultant `topConsultant` (score-less) + `OfferService` pricing; envelope response.
  Preserves the similar-dedup Node bug and the expertise-never-filters-similar quirk.
- ✅ **Step 3 (`cons/popular`)** → `discovery/ConsultantPopularService` + `DiscoveryController` GET `/popular` (DONE, faithful; diff-pending).
  Added `Collections.CONSULTANT_SLOTS_BOOKS`. 3 verbatim pipelines (`OFFERS_AGG`, `POPULAR_REVIEW_FACET` sorted by review length,
  `ORDER_HISTORY`) + 6 programmatic finds/counts. The two waitlist reads (`getProgressingWaitlist(null, consultantId)` +
  `getConsultantWaitlist`) live on `WaitlistStatusService` and read the Redis billing clock via `TimerReadService`.
  Returns Node's RAW `{success, data}` shape. Quirks preserved: `topConsultantStatus` hardcoded `"top"`, `consAvailable` always 0,
  `similarConsultant[].rating` hardcoded 4, `discountPrice == price.default`.
- ✅ **Step 5 (`consultant_list`/`top_consultant_list`)** → `discovery/ConsultantListService` + `UserDiscoveryController` POSTs (DONE, faithful; diff-pending).
  Both endpoints call the SAME service (only the success message differs). Encrypted `reqData` decrypted via `CryptoService`;
  dynamic `$match` built programmatically; verbatim tail pipeline; seed-master extra reviews; waitlist status batch via
  `WaitlistStatusService.getConsultantStatusBatch` → `TimerReadService`.

**➡ Prompt B is COMPLETE — all 5 discovery endpoints ported and JSON-validated. Next: Prompt C (idempotent writes).**

---

## Step 2 — `ConsultantService.topConsultant` → `discovery/ConsultantRankingService`
Node: `utils/classes/consultant-service.js`
- `topConsultant(consultantId, score)` @ **L3800–3883** — returns a tag string `"top" | "second" | "third" | "new" | ""`.
- Depends on 3 leaf aggregations (port each as a private method):
  - `consultantRevenueAndOnlineAvailability` @ **L3685** → `$facet` over `consultantformrequests`
    (`Collections.CONSULTANT_FORM_REQUESTS`), `isConsultantCompleted:"complete"`; returns
    `{totalHours (min/60, 1dp), totalRevenueGeneration, revenuePercent}`.
  - `consultantRating` @ **L3749** → `review_and_ratings` (`Collections.REVIEW_AND_RATINGS`),
    `{consultantId, status:true}`, `$group avgRating/$sum:1`; returns `{totalRating, avgRating}`.
    ⚠ Node field is misspelled `totalRatig` in the pipeline — read it back with that exact key.
  - `consultantLiveAvailability` @ **L3775** → `broadcasts` (**ADD** `Collections.BROADCASTS = "broadcasts"`),
    `{consultantId, status:2}`, `$sum totalTimeSpentInLiveMinutes`; returns `{totalLiveHours (min/60,1dp)}`.
  - `checkConsIsNew(consultantId)` @ **L5635** (75-day "new" check) — port faithfully.
- Ranking thresholds: constants `TOP_CONSULTANT_* / SECOND_CONSULTANT_* / THIRD_CONSULTANT_*`
  (LIVE_AVAILABILITY, ONLINE_AVAILABILITY, TOTAL_REVENUE, REVENUE_PERCENT, AVERAGE_RATING, TOTAL_RATING).
  `grep -rn "TOP_CONSULTANT_LIVE_AVAILABILITY" utils/` to find their values; put them in a
  `RankingConstants` class (or `AppConstants`). The `checkStatus(...)` predicate at L3813–3830
  must be copied exactly (note `totalRevenueGeneration + score >= threshold`).
- Unit-testable: given mocked sub-aggregation outputs, assert the tag boundaries.

## Step 3 — `GET /v2/user/cons/popular`
Node: `utils/classes/user/cons.js` **L353–762** (~400 lines).
- ⚠ **Deep dependency chain discovered:** the handler calls `waitlistManger.getProgressingWaitlist(null, consultantId)`
  (waitlist.js **L569**, the `else if (consultantId)` branch **L672–704**) and `getConsultantWaitlist(consultantId)`
  (waitlist.js **L167**). BOTH read the **Redis billing-clock** via `isTimerActive(roomId)` + `getRemainingTime(roomId)`
  (`roomId = "<waitlistId>:duration"`). So popular is NOT a pure read — port the Redis timer read helpers first
  (this overlaps Phase 2.4/2.5). Accessibilities logic (chat/audio/video) keys off `coupon._id` (two hardcoded ObjectIds
  `67d195c835f0c73d7a488f7d`, `680218a0e65f3c49bcc45cc2`) and `session_info.mode` — copy exactly.
- Plus 8 direct DB reads (offers aggregation, popularConsultants find, userConsRel, consultantSlotsBook,
  isConsultancyTaken count, gallery, and a big review/reply `$facet`). Copy each pipeline verbatim via `parsePipeline`.

## Step 4b — `consultant_details` (Node user.js `consultantDetails` L4866–~5560, ~600 lines)
- Returns the **envelope** `{code,success,message,result}` → `ApiResponse.ok("Consultant details fetched successfully", result)`.
- Pipeline stages contain embedded JS that is NOT valid JSON — substitute when copying to text blocks:
  `new ObjectId(user._id)` → `{"$oid":"__USER_OID__"}`; `AWS_CREDS.MEDIA_URL` → `__MEDIA_URL__`;
  `new Date(new Date().setDate(getDate()-360))` → compute in Java and inject; `Number(CONSULTANT_EMERGENCY_CALL_CHARGE_TIMES||1.5)` → inject constant.
- Post-processing (programmatic, reuse ported services):
  - per-consultant `topConsultant(element._id)` — **use the score-less overload** (already added; passes NaN, faithful to Node).
  - per-consultant `getConsOfferDiscountForUser(user._id, element._id, element.price)` → `OfferService` (already ported);
    `discountPrice = offer.status ? offer.discountPrice : element.price`.
  - ⚠ **Node bug preserved:** the similar-consultant dedup maps `JSON.stringify(consultantId)` (the REQUEST id, not
    `element._id`) so `uniqueSimilarConsultant` never actually filters — reproduce as-is (`similarConsIds` all equal the
    request id) unless the team decides to fix-forward.
  - review/reply `$facet` (L5235–5456) is the same shape as popular's — share one text block.

## Step 5 — POST `consultant_list` / `top_consultant_list` (Node user.js `consultantList` L4270–4866, ~596 lines)
- Largest; **encrypted `reqData`** → `CryptoService.decryptToMap(body.get("reqData"))`. Membership-gated (`MembershipService`).
- Big ranking aggregation + per-consultant `OfferService` pricing + `ConsultantRankingService.topConsultant` (score-less overload).
- Port last; fixture uses an encrypted `reqData` body; diff response AND spot-check per-consultant pricing.

---

## Per-step checklist (from PARITY_SIGNOFF.md)
1. Port service + controller under `/v2`, reuse `@RequireRole`/`@CurrentUser`/`AuthUserService`/`CryptoService`/`OfferService`.
2. Add fixture to `contract-tests/fixtures/discovery.json`.
3. Record golden from TEST Node, run `contract-tests/run.js`, fix Java until diff-clean (never edit the golden).
4. Verify collection names on TEST DB (`db.getCollectionNames()`): confirm `broadcasts`, `wallet_transactions`.
5. Enable on `/v2` (canary) only when diff-clean. Pricing endpoints (Steps 4–5) also need a per-consultant price spot-check.

## Not in this phase (staged in PHASE2_PLAN.md — do NOT blind-port)
2.2 idempotent writes · 2.3 wallet/payments (shadow-reconcile before cutover) · 2.4 call lifecycle +
waitlist · 2.5 Socket.IO + BullMQ + 15 crons. These are money/real-time paths gated behind the
shadow/reconcile discipline and get their own phases.
