# VedicMeet Node → Java (Spring Boot) Migration Audit

**Repo:** `new-app-server-java`  
**Reference Node repo:** `new-app-server`  
**Date:** 2026-09-09  
**Scope:** Static + build/test audit only. No feature code was written and no business logic was changed to produce this report.

---

## Verdict (one line)

**The Java port BUILDS and all 428 tests pass, but it is NOT production-ready: ~76% of admin modules are compile-only scaffolds (routes present, logic = `TODO`, no tests), and ZERO endpoints have been diff-verified against Node (the `contract-tests/golden/` directory does not exist).**

---

## 1. Build & Tests

| Item | Result |
|---|---|
| Command | `mvn -o clean test` (isolated repo `C:\Users\tiwaris5\vedicmeet-build\repo`, JDK 17) |
| Compiles? | **Yes** — `BUILD SUCCESS` |
| Full Spring context boot (`AppServerContextLoadTest`) | **Passes** (no duplicate request mappings, all beans satisfiable) |
| Tests run | **428** |
| Passed | **428** |
| Failed | **0** |
| Errors | **0** |
| Skipped | **0** |

Surefire summary (tail):
```
[INFO] Tests run: 428, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
**Failing tests:** none.

> ⚠️ Caveat: `mvn verify` was not run (no integration-test / failsafe phase configured beyond `test`). The 428 tests are unit + Spring-slice + one full-context-load test. **None of the 32 newly scaffolded admin modules has any test** — the only admin tests are `AdminContentController/ServiceTest` and `AdminAuth*Test`.

---

## 2. Endpoint Coverage (Java vs Node)

**Method:** counted route declarations — Java `@Get/@Post/@Put/@DeleteMapping` (577 across 79 files) vs Node `router.get/post/put/delete` (674 across 89 files). "Route-presence" = a Java mapping exists at the equivalent path. "Verified" = has passing test **and** a recorded Node diff. Per the brief, nothing is marked done on code alone.

### 2.1 Totals by area

| Area | Node routes | Java mappings | Route-presence | Verified (test + Node diff) |
|---|---|---|---|---|
| Admin panel (`modules/admin/*`) | 355 | 334 | ~94% | **~14%** (only content-group + admin-auth have tests; **0 Node diffs**) |
| Consultant app (`modules/consultant/*`) | 91 | 92 | ~100% | Partial — logic ported **with tests**, but call/session **write execution gated OFF**; **0 Node diffs** |
| User app (`modules/user/*` + shared `modules/*`) | 57 + 159 shared | content 57 + session 18 + discovery 5 + user/master/banner/notification/support/payment | ~60–75% (approx) | Low — mixture of ported + scaffold; **0 Node diffs** |

> Route-presence ≠ parity. A scaffold route "exists" but returns a `_todo` placeholder.

### 2.2 Admin module status (42 Node admin modules)

| Status | Count | Modules |
|---|---|---|
| **Logic ported + dedicated test** | 6 | `banner`, `gift`, `feedback`, `refer`, `commonmessage` (all in `AdminContentController`/`Service`, tested), `auth` (`AdminAuthController`/`Service`, tested) |
| **Logic ported, NO dedicated test** | 4 | `user`, `support`, `transaction`, `waitlist` |
| **SCAFFOLD ONLY — routes present, logic = `TODO`, no test, no Node diff** | 32 | (see MISSING list below) |
| **No Java controller at all** | 0 | — |

**Admin verified-done = 6/42 (~14%). Logic-present = 10/42 (~24%). Scaffold-only = 32/42 (~76%).**

### 2.3 MISSING (logic not ported — scaffold only) — every admin module by name

These 32 admin modules have a Java controller with the correct routes/verbs/guards, but the service returns placeholder data (`_todo`). They are **NOT** done:

`analytics`, `category`, `cms`, `community`, `concern-remedy`, `cons`, `consultant`, `consultant-insights`, `count-tracking`, `coupons-v2`, `dashboard`, `enhanced-dashboard`, `event`, `explore`, `flag`, `gallery`, `growth-training`, `influencer-portal`, `influencers`, `master`, `meditation`, `membership-discount`, `music`, `notification`, `notification-message`, `payout`, `request`, `session-breakdown`, `system`, `user-insights`, `vastu-compass`, `zodiac-message`.

> The heaviest real logic still to port lives in Node `utils/classes/*` (e.g. `consultant-insights-service.js` 1526 lines, `user-insights-service.js` 1614 lines, `dashboard.js`, `enhanced-dashboard.js`, admin `consultant.js` 1187 lines, `analytics.js` 789, `notification-message.js` 720).

---

## 3. Stubs & Gaps

**Markers searched:** `UnsupportedOperationException`, `NOT_PORTED`, `TODO`, `FIXME`, `not wired`, `not implemented`, `_todo`.

- `UnsupportedOperationException`: **0**
- `NOT_PORTED`: **0**
- **`_todo` placeholder returns (scaffold services, logic not ported): 32 files**, e.g.:
  - `admin/AdminDashboardService.java` (8 methods) — `PORT dashboard.js #...`
  - `admin/AdminEnhancedDashboardService.java` (9) — `PORT enhanced-dashboard.js #...`
  - `admin/AdminUserInsightsService.java` (3) / `admin/AdminConsultantInsightsService.java` (2)
  - `admin/AdminConsultantService.java`, `AdminAnalyticsService.java`, `AdminNotificationMessageService.java`, `AdminCountTrackingService.java`, and 24 more admin `*Service` classes (`category`, `cms`, `community`, `concern-remedy`, `cons`, `coupons-v2`, `event`, `explore`, `flag`, `gallery`, `growth-training`, `influencer-portal`, `influencers`, `master`, `meditation`, `membership-discount`, `music`, `notification`, `request`, `session-breakdown`, `system`, `vastu-compass`, `zodiac-message`, `payout`).
- **`admin/AdminPayoutController.java` / `AdminPayoutService.java`** — Javadoc: *"MONEY-MOVING: intentionally not implemented; must not move funds; requires human sign-off."* Placeholder only.
- **`updates/UpdatesService.java:70`** — pre-existing: *"BUNDLE_USE_S3=true but S3 bundle storage is not wired in the Java port yet."*

Every `_todo` service method returns empty/placeholder data (`{list:[], total:0, _todo:...}` or `{_todo:...}`).

---

## 4. Write-Safety / Enablement

### 4.1 Feature switches in `application.yml` (defaults)

**All execution/write/transport switches default `false`:**

| Switch | Default |
|---|---|
| `vedicmeet.migration.writes-enabled` | `false` |
| `vedicmeet.socket.enabled` / `redis-fanout` / `app-session-writes-enabled` | `false` |
| `vedicmeet.call.execution-enabled` / `timer-worker` / `outbox-worker` | `false` |
| `vedicmeet.live-event.execution-enabled` / `timer-worker` | `false` |
| `vedicmeet.livekit.enabled` | `false` |
| `vedicmeet.consultant-session.execution-enabled` / `immediate-refund` | `false` |
| `vedicmeet.session.booking / commands / fixed-booking-enabled` | `false` |
| `vedicmeet.task-reminder.worker-enabled` | `false` |
| `vedicmeet.notification.fcm.enabled` / `apns.enabled` | `false` |
| `vedicmeet.cron.enabled` + all 9 sub-families + `payment-reconcile-enabled` | `false` |
| `vedicmeet.payment.phonepe.enabled` / `paytm.enabled` | `false` |
| `vedicmeet.integrations.pabbly.enabled` / `meta.enabled` | `false` |
| `vedicmeet.auth.msg91.enabled` / `social.google-enabled` / `mail.enabled` / `admin-bootstrap-enabled` | `false` |

**Defaults `true` (flag for review):** `vedicmeet.auth.legacy-email-login-enabled`, `vedicmeet.auth.legacy-social-email-enabled` — legacy trust modes that authenticate on possession of an email address. They are additionally behind `migration.writes-enabled=false`, but should be turned off once clients send a password/verifiable identity token.

### 4.2 Mutating routes not gated by `@MigrationWrite`

Scan: 289 `@Post/@Put/@Delete` mappings; **29 lack an adjacent `@MigrationWrite`.** On inspection:

- **~26 are POST/PUT-for-READ (correctly ungated):** `AdminUserController` `/list`, `/list/*` (8), `AdminConsultantController` `/{id}/wallet-details`, `/avg/rating`, `AdminCouponsV2Controller` `/validate`, `AdminAuthController` `/login`, `ConsultantLegacyController` `/website_top_consultant_list`, `/dashboard/performance/*`, `ConsultantSessionController` `/get_live_event`, `KundaliController` `/yogas`, `MobileAuthController` `/check-consultation-status-with-this-device`, `NewCommunityController` `/horoscope_personal`, `UserDiscoveryController` `/consultant_list`, `/top_consultant_list`, `UserSessionController` `/coupon-status`, `/get-progress-call`, `ExampleSecuredController` `/echo`.
- **Genuine writes missing the gate (⚠ fix before logic is ported) — currently INERT (return `_todo`):**
  - `AdminConsultantController` `POST /online_offline`
  - `AdminConsultantController` `POST /wallet`
  - `AdminConsultantController` `POST` (base create)
  - `UserSessionController` `POST /generate_token_1to1` (verify intent)

> No **live** mutating route is ungated: existing ported writes all carry `@MigrationWrite`; the flagged scaffold writes execute no logic yet. **Action:** add `@MigrationWrite` to any scaffold write before porting its body, because a route without the annotation is not covered by the global `migration.writes-enabled` switch.

---

## 5. Verification State

- **`contract-tests/golden/` does NOT exist → NO endpoint has been diff-verified against Node.** The harness (`contract-tests/run.js`, `record.js`, `diff.js`, `normalize.js`) exists but has recorded **0** golden Node responses.
- **Input fixtures present (10, not Node responses):** `banner.json`, `category.json`, `cms.json`, `discovery.json`, `explore.json`, `gallery.json`, `master.json`, `updates.json`, `week3.json`, `writes.json`.
- **Java-side "contract" tests (assert Java response shape only, NOT a Node diff):** `AdminAuthControllerContractTest`, `MobileAuthControllerContractTest`, `UserSessionBookingControllerContractTest`.
- **Endpoints with NO fixture / NO diff:** all admin scaffold modules (§2.3), all insights/dashboard, consultant app, most user app.

---

## 6. Parity Risks

### 6.1 Unconfirmed Mongo collection names (`config/AppConstants.java`, `// VERIFY`)

None confirmed against the live/TEST DB (`db.getCollectionNames()`):

`meditation-categories`, `meditation_media`, `cms`, `galleries`, `explores`, `banners`, `seed_masters`, `notifications`, `consultant-leave-a-message-mappings`, `communitymappings` (model `communityMappings`). A wrong pluralization = silent empty reads / mis-targeted writes.

### 6.2 Crypto / JWT / response envelope

- **JWT (`security/JwtService.java`):** custom raw HMAC-SHA256 to match Node `jsonwebtoken`. Method `verifyAndDecryptJwtToken` is a **misnomer** — the payload is signed, not encrypted (same as Node; anyone can base64-decode claims). `AuthPrincipal` normalizes `role`/`type` and adds `tokenVersion` additively. Risk: low if secret + alg match Node; **not** diff-tested end-to-end with a Node-issued token in TEST.
- **Response envelopes:** `AdminResponses.execute` → `{code,success,message,result}`; `AdminResponses.simple` → `{success,data}`. **All 32 scaffolds standardize on `execute(...)`**, but individual Node routes vary (e.g. `dashboard/quick-stats` builds a nested `result` with specific fields; some routes return `{success,data}`; some return bare objects). Until diff-verified, scaffold envelope parity is **unproven**.
- **Crypto (`crypto/CryptoUtil`):** AES; unit-tested (`CryptoUtilTest`).

---

## 7. Prioritized Remaining Work

**P0 — Verification gate (blocks any go-live claim)**
1. Record Node golden responses into `contract-tests/golden/` and diff every `/v2` endpoint. Until then, treat **no** endpoint as parity-complete.
2. Confirm all `// VERIFY` collection names against the TEST DB (§6.1).

**P1 — Admin logic porting (the 32 scaffold modules, §2.3)**
3. Port real logic from Node `utils/classes/*` for each scaffold, starting with high-value read analytics: `dashboard`, `enhanced-dashboard`, `user-insights`, `consultant-insights`, admin `consultant`, `analytics`, `notification-message`, `count-tracking`.
4. Add a unit/slice test per module (none exist today).
5. Add `@MigrationWrite` to scaffold write routes before porting bodies (§4.2).
6. Keep `payout` fail-closed until dedicated money-movement review.

**P2 — Coverage gaps**
7. Add dedicated tests for already-ported-but-untested admin modules: `user`, `support`, `transaction`, `waitlist`.
8. Close the user-app route gap (audit exact Node `modules/user/*` + shared routes vs Java).

**P3 — Enablement (operational, needs TEST env + real credentials)**
9. Validate + enable gated machinery one switch at a time (sockets → calls → live-event → consultant-session → crons → FCM/APNs → payment reconciliation), each behind its own TEST soak test.
10. Turn off `legacy-email-login` / `legacy-social-email` once clients send verifiable identity.

---

### Definition-of-done reminder
Nothing in this repo should be marked "done" that only has code. Done = **logic ported + passing test + recorded Node diff**. Today that set = admin content-group + admin-auth (tests) — and **even those lack a Node diff**, so strictly **0 endpoints are fully parity-verified.**
