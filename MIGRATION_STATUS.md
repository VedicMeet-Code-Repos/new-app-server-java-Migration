# Migration status

Tracks progress against `Java_Migration_Month1_Sprint_Plan.md`. Strangler model:
Java serves `/v2/*`; everything else stays on Node; each endpoint is gated by the
contract-test harness before it goes live.

## Week 1 — Foundation & safety net ✅ (built)

| Item | Status | Where |
|---|---|---|
| Spring Boot scaffold (Java 17, Web, Data Mongo, Redis, Validation) | ✅ | `pom.xml`, `AppServerApplication` |
| Shared Mongo connection (pool, secondary reads, timeouts) | ✅ | `config/MongoConfig` |
| Auth/JWT parity (`vm-user-auth`/`authorization`, HS256) | ✅ | `security/JwtService`, `JwtAuthFilter`, `RoleInterceptor` |
| Encryption parity (crypto-js `Salted__` AES) | ✅ | `crypto/CryptoUtil`, `CryptoService` |
| Error-contract wrapper (HTTP-200-`{success:false}`) | ✅ | `web/ApiResponse`, `GlobalExceptionHandler` |
| `/v2/health` + versioning | ✅ | `health/HealthController` |
| Contract-test harness | ✅ | `contract-tests/` |
| Parity unit tests (crypto + JWT) | ✅ | `src/test/.../CryptoUtilTest`, `JwtServiceTest` |

## Authentication feature migration — implemented, gated, awaiting TEST parity

The complete user/consultant/admin authentication surface from Node production snapshot
`bf308ee` is now implemented under `/v2`; see `AUTHENTICATION_MIGRATION_SPEC.md`.

| Area | Code | Isolated tests | Enabled for traffic |
|---|---:|---:|---:|
| Mobile OTP, registration, login, social, device, logout, deletion | ✅ | ✅ | ❌ |
| Consultant signup, approval and OTP/login gates | ✅ | ✅ | ❌ |
| Admin/sub-admin BCrypt login and management | ✅ | ✅ | ❌ |
| JWT headers, legacy `type`/current `role`, Redis cache and revocation | ✅ | ✅ | ❌ |
| HTTP contract/error compatibility and route safety annotations | ✅ | ✅ | ❌ |
| Provider sandbox, TEST Mongo names, Node-vs-Java diff, mobile/socket canary | — | ⏳ | ❌ |

Verification on 2026-09-08: `mvn test` = **235 tests, 0 failures, 0 errors,
0 skipped**. All mutating/external-effect auth routes remain protected by
`@MigrationWrite`; `vedicmeet.migration.writes-enabled` is still false by default.
This is code-complete for the scoped authentication feature, but production remains
**NO-GO** until the gates listed in section 24 of the specification pass on TEST.

## Week 2 — First low-risk READ modules on /v2 (in progress)

| Endpoint | Node source | Java | Ported | Harness fixture | Diff-green* |
|---|---|---|---|---|---|
| `GET /master` | `modules/master.js` → `utils/classes/master.js` | `master/MasterController`+`MasterService` | ✅ | `fixtures/master.json` | ⏳ needs test server |
| `GET /v1/category` | `modules/category.js` → `utils/classes/category.js` | `content/CategoryController`+`CategoryService` | ✅ | `fixtures/category.json` | ⏳ needs test server |
| `GET /v1/cms/details` | `modules/cms.js` → `cms.cmsDetails` | `content/CmsController`+`CmsService` | ✅ | `fixtures/cms.json` | ⏳ needs test server |
| `GET /v1/gallery` | `modules/gallery.js` → `gallery.listGalleries` | `content/GalleryController`+`GalleryService` | ✅ | `fixtures/gallery.json` | ⏳ needs test server |
| `GET /v1/explore` | `modules/explore.js` → `explore.listExplore` | `content/ExploreController`+`ExploreService` (+`AuthUserService`) | ✅ | `fixtures/explore.json` | ⏳ needs test server |
| `GET /updates/manifest.json` | `modules/app-updates.js` (+ BundleStorage) | `updates/UpdatesController`+`UpdatesService` | ✅ | `fixtures/updates.json` | ⏳ needs test server |

\* Diff-green requires recording golden responses from the TEST Node server and
running `contract-tests/run.js` against the Java service — needs test-server access
(and a test JWT) that I don't have from here. Everything up to that gate is done.

| `GET /v1/banner` | `modules/banner.js` → `banner.home`/`_fetchBannerHome` | `banner/BannerController`+`BannerService` (+`BoostTimeService`,`BannerMetricsService`,`pricing/OfferService`) | ✅ | `fixtures/banner.json` | ⏳ needs test server |

**All 7 Week-2 endpoints are now ported.** `banner` was completed as a full faithful port
following `BANNER_PHASE2_SPEC.md`: leaf deps first (`OfferService` = pricing/offer discount;
`BoostTimeService` = boost-time update **write**), then `BannerMetricsService` (waitlist/stats
metrics + banner-data builder), then `BannerService` (both branches). The five big
aggregations are the exact Node Mongo JSON parsed via `Document.parse` (validated); dynamic
reads/writes are programmatic.

> ⚠️ `banner` is the one ported endpoint that is NOT idempotent — the consultant branch
> **writes** to `consultant_boosts`, and the user branch computes **pricing**. Its contract
> gate must verify the boost write separately (the response diff alone won't), and its
> `banner_cons` fixture ignores the wall-clock-dependent boost fields. See
> `BANNER_PHASE2_SPEC.md`. Do not enable it on `/v2` until both the diff and the write are
> verified against the test server.

## Week 3 — Medium READ modules (authed + real data)

| Endpoint | Node | Java | Ported | Fixture |
|---|---|---|---|---|
| `GET /v1/membership-discount` (+ `/details`) | `membership-discount.js` | `content/MembershipDiscount*` | ✅ | `fixtures/week3.json` |
| `GET /v1/notification` (+ `/count`) | `notification.js` | `notification/Notification*` | ✅ | `fixtures/week3.json` |
| `GET /v1/support/i18n` (+ list) | `support.js` | `support/Support*` (+ locale resources) | ✅ | `fixtures/week3.json` |
| `GET /user/available_balance` | `user/index.js` → `myAvailableBalance` | `user/User*` | ✅ | (add) |

`available_balance` exercises **encrypted-field decryption** (wallet `coins` is AES at rest →
`CryptoUtil`). i18n bundles the `en/hi/kn` locale JSON as resources (te/ta are commented out
in Node). All authed reads reuse `AuthUserService`.

**Deferred (scoped): consultant discovery** — `consultant_list`, `top_consultant_list`,
`consultant_details`, `cons/get-list`, `cons/popular`. Large (~900-line `consultantList`),
membership-gated, encrypted, pricing/ranking-sensitive. Full plan: `WEEK3_DISCOVERY_SPEC.md`.

### Phase-2.1 progress (consultant discovery) — started 31-Aug-2026

| Step | Item | Java | Status |
|---|---|---|---|
| 1 | `checkUserHaveMembership` guard | `discovery/MembershipService` (+ `WALLET_TRANSACTIONS` in AppConstants) | ✅ ported (leaf, self-contained) |
| 2 | `ConsultantService.topConsultant` | `discovery/ConsultantRankingService` | ✅ ported (read-only ranking helper; diff-pending like banner) |
| 3 | `GET /v2/user/cons/popular` | `discovery/ConsultantPopularService` + `DiscoveryController` GET `/popular` (+ `CONSULTANT_SLOTS_BOOKS`) | ✅ ported (3 verbatim pipelines + 6 finds/counts; 2 waitlist reads via `WaitlistStatusService`→`TimerReadService`; raw shape; quirks: topConsultantStatus hardcoded "top", consAvailable 0, similar rating 4) |
| 4 | `cons/get-list` + `consultant_details` | `discovery/ConsDiscoveryService` (get-list) + `discovery/ConsultantDetailsService` + `UserDiscoveryController` | ✅ both ported (get-list raw shape; consultant_details envelope + 5 aggregations via Document.parse, diff-pending) |
| 5 | POST `consultant_list` / `top_consultant_list` | `discovery/ConsultantListService` + `UserDiscoveryController` POSTs (+ `TimerReadService`, `WaitlistStatusService`) | ✅ ported (both share one service; encrypted reqData via CryptoService; dynamic `$match` + verbatim tail; waitlist status batch; diff-pending) |

**✅ Phase-2.1 (Prompt B) COMPLETE — all 5 discovery endpoints ported + JSON-validated (0 invalid). Next: Prompt C (idempotent writes).**

`MembershipService` is a faithful 1:1 port of the Node middleware (utils/middle-wares.js L219),
including the quirk that `walletTransactionId` is only set when `planType` is truthy. It is a
leaf (no callers yet) and will be invoked by the discovery controllers after
`AuthUserService.load(...)`, mirroring how the Node middleware augments `req.user`.

> Steps 2–5 are **pricing/ranking-sensitive** and, per this repo's rules, must be
> harness-diff-verified on the TEST server before they are enabled on `/v2` — they are NOT
> shipped blind. See `DISCOVERY_PORTING_GUIDE.md` for the exact Node source map per step.
> Phases 2.2 (writes), 2.3 (wallet/payments), 2.4 (call lifecycle), 2.5 (real-time) remain
> staged in `PHASE2_PLAN.md` and require the shadow/reconcile gate — deliberately not
> blind-ported.

### Phase-2.2 progress (Prompt C — idempotent writes) — started 26-Aug-2026

| Endpoint | Java | Node | Status |
|---|---|---|---|
| `PUT /v2/v1/notification/mark_read` | `notification/NotificationService.markRead` + controller | `classes/notification.js` L543 | ✅ ported (returns pre-image; no idempotency guard, as Node) |
| `POST /v2/v1/notification/mark_all_read` | `NotificationService.markAllAsRead` + controller | `classes/notification.js` L352 | ✅ ported (`$addToSet`, idempotent) |
| `PUT /v2/v1/explore/action` | `content/ExploreService.action` + controller | `classes/explore.js` L1630 | ✅ ported (7 actionTypes; `notRecommand`→misspelled `notRecommed`; native-insert drift flagged) |
| `PUT /v2/user/live_like` | `social/SocialWriteService.likeLiveEvent` + `SocialController` | `classes/user.js` L4512 | ✅ ported (broadcast like toggle + comment insert) |
| `POST /v2/user/follow` | `social/SocialWriteService.followCons` + `SocialController` | `classes/user.js` L4556 | ✅ ported FOLLOW+COUNT; FOLLOWLIST throws NOT_PORTED (read out of scope); TDZ comment bug documented |
| `POST /v2/v1/support/initiate_query` | `support/SupportService.customerQueryInitiate` + `SupportController` | `classes/support.js` L507 | ✅ ported — chat-server via `ChatServerClient` (JDK HttpClient, trust-all TLS = Node); FCM = `PushNotificationService` seam; 2 `notificationns` writes + query create; dedup `QUERY_EXIST` |
| `POST /v2/v1/support/chat_send` | `support/SupportService.supportChat` + `SupportController` | `classes/support.js` L730 | ✅ ported — `customer_support_chats` write; admin first-reply thread; Node `user._id` (model) bug preserved; media via `MediaUploadService` seam |
| `PUT /v2/user/update` (profile) | `user/UserProfileService.updateUser` + `UserController` | `classes/user.js` L4161 | ✅ ported — field-merge `findOneAndUpdate({new:true})`; `"null"`-string guard; email `$nin:[userId]`; `details` merge; `profileImage` via `MediaUploadService` seam |

**7 of 7 Prompt-C endpoints ported.** New seams: `MediaUploadService` (S3), `ChatServerClient`
(chat-server HTTP), `PushNotificationService` (FCM). New collections: `NOTIFICATION_RECORDS`,
`CUSTOMER_SUPPORT_QUERIES`, `CUSTOMER_SUPPORT_CHATS`, `ADMINS`. New config `vedicmeet.chat.server-url`.
Fixtures + `_assertDoc` for all 7 in `contract-tests/fixtures/writes.json`. See `PROMPT_C_SPEC.md`.

### Phase-2.3 progress (Prompt D — wallet/payments) — STARTED, SHADOW-ONLY

⚠ **Highest risk. Unwired to any live route. Human review + shadow-reconcile REQUIRED before cutover.**

First slice = the Razorpay webhook money path (`POST /confirm/webhook`), ported from
`classes/transaction.js` `confirmPaymentWithWebhook` + credit half of `creditAndDebitOnWallet`:

| Java | Role |
|---|---|
| `payment/PaymentWebhookService` | Idempotency orchestration (unwired) |
| `payment/WalletGateway` + `payment/MongoWalletGateway` | DB port + Mongo impl (auditable effects) |
| `payment/RazorpaySignatureVerifier` | HMAC-SHA256 webhook signature |
| `payment/WebhookResult` | credited / already-processed / failed |

Idempotency guard = **atomic `INITIATED→COMPLETED` transition**; audit row always written first.
New collections `TRANSACTIONS`, `PAYMENT_WEBHOOKS`. Four shadow-only slices are now ported, all
unwired: (1) webhook idempotency, (2) plain wallet credit/debit (`WalletService`), (3) spend
orchestration (`PaymentFromWalletService` = Node `paymentFromWallet`), (4) **AES-encrypted `coins`
credit/debit** (`AesWalletService` = Node `userWalletUpdate`/`consultantWalletUpdate`) — coins READ via
`CryptoUtil.decrypt` and WRITTEN via `CryptoUtil.encrypt` (crypto-js `Salted__`, never stored plain;
user debit floors at 0). **JUnit: 24/24 passing** — compiled + executed here via `.m2` jars (no `mvn`).
Full detail + the two-wallets reviewer note in **`PROMPT_D_STATUS.md`**. Since then, **Razorpay order-create**
(`RazorpayOrderService` + SDK-backed `RazorpayGatewayImpl`) and **`purchaseMembership`** (reverted to the
FAITHFUL waitlist-`{userId}` coins read — the Node quirk is kept, not fixed) are also ported + tested. The
one remaining D gap is the **PhonePe/Paytm gateway callbacks** (source not in the Aug-26 snapshot — need the
newer live-branch `transaction.js`; see `PROMPT_D_STATUS.md`). All money/real-time work stays behind the
harness + human-review gate.

### Phase-2.4 progress (Prompt E — call lifecycle) — STARTED, SHADOW-ONLY

First slice = the `initiateCall` validation gates (Node `CallManager.initiateCall`, call.js L381) →
`call/CallLifecycleService` + `call/CallGuardStore`/`MongoCallGuardStore`. Reproduces the exact
transition order + cancel/miss log strings: party-not-found, already-closed no-op, insufficient
balance, mode-off, party-busy→`missed`, else→`openCall` (seam). Pricing + Redis + Socket.IO are honest
seams (throw until wired). **Now also ported (all FAITHFUL, shadow-only):** the two-phase **`acceptCall`**
(`CallAcceptService`, call.js L1494 — ghost/partial/reconnect/progress-start+billing-clock/no-op/re-join,
incl. the phase-2-vs-rejoin accessbilities coupon quirk), the user-pressed **`endCall`** (`CallEndService`,
L1224 — stop timer + `status='ended'` + notify, does NOT settle; `hgetall {}`-truthy quirk kept), and the
live **settlement math** (`CallTimeoutSettlementService`, `handleCallTimeout` L631-749 — platform-rate
selection + every coupon/session/offer branch, plain-`wallet` deltas). Remaining (progress/extend wiring,
the known-race / `'missed'` mislabel — kept faithful per the team decision) in **`PROMPT_E_STATUS.md`**.
All real-time/money work stays behind the harness + human-review gate.

### Phase-2.5 progress (Prompt F — real-time) — infra core only; mostly BLOCKED here

The one portable + testable piece is the **cron distributed lock** — Node `withRedisLock`/`withCronLock`
(redis-lock.js) → `cron/RedisLockService` + `RedisLock`/`RedisLockImpl` (SET NX EX + the exact Lua
compare-and-delete safe release). **JUnit: 4/4 passing**. Also ported the **Socket.IO handshake JWT
authenticator** (`realtime/SocketHandshakeAuthenticator`) — verifies `handshake.auth.token` via the same
HS256 `JwtService` as REST (strips Bearer, rejects missing/tampered), **4/4 passing**. Plus the Prompt-E
billing-clock **writes** (`call/TimerWriteService`, 4 tests). **Now also ported:** the **call socket
handler bodies** (`realtime/CallSocketDispatcher` + `CallSocketBackend` seam) — a faithful port of the
app-namespace call events (accept_call, cancel_call, end_call, verify_end_call, extend_call,
join/leave_room) from `sockets/namespaces/app.js` L321-473, delegating to `CallManager` (the E services),
10/10 tests. The **Socket.IO server wiring** (needs a
Java socket lib + a live Node peer for byte-identical protocol parity + the event catalog from the KT
docs), **BullMQ** (keep on Node, same Redis keys/job names), and the `@Scheduled` task bodies
(Interakt/CleverTap) are genuinely blocked on external infra — see **`PROMPT_F_STATUS.md`**.

**Session total: 142/142 JUnit passing under REAL Maven** (incl. a full `AppServerContextLoadTest` that
boots the whole Spring context) — verified with `mvn clean test` using an
**isolated Maven 3.9.16 + a dedicated local repo** (`C:\Users\tiwaris5\vedicmeet-build`, JDK 17), kept
completely separate from the shared `~/.m2` (see `BUILD_ISOLATED.md`). Prompt E now covers initiate-gates,
two-phase accept, endCall, and the live settlement math; Prompt F adds the call-socket dispatcher. **One
documented gap** (faithful-only): PhonePe/Paytm gateway callbacks + `active-gateways` are NOT in the
Aug-26 snapshot's `transaction.js` (Razorpay-only there) — they live on a newer live-branch version;
will not fabricate, need the newer source (contract captured in `PROMPT_D_STATUS.md`). The only
verification step left is the **contract-harness diff against the TEST server** (needs the running TEST
env + tokens). Everything money/real-time remains unwired and gated. (Production secrets were NOT used.)

### Bootability & review response (31-Aug-2026)
A production review returned **NO-GO** (correct — this stays a shadow/strangler service, not a Node
replacement). Two of its findings were real defects and are now **fixed**:
- **App would not start.** (a) `CallAcceptService` was a `@Service` whose `CallAcceptStore` had no Spring
  impl → made it a plain (unwired) class like the other call-transition services. (b) `ChatServerClient`
  had two constructors with none `@Autowired` (Spring: *No default constructor found*) → added `@Autowired`
  to the injection constructor. A new **`AppServerContextLoadTest`** (`@SpringBootTest`) now boots the full
  context in the suite, so this class of bug fails the build instead of hiding.
- **Misleading readiness.** `management.endpoint.health.group.readiness` now includes `mongo,redis`, so an
  instance that cannot reach its datastores reports **DOWN** (no traffic) instead of UP.

The review's remaining findings are **accurate and already tracked as intentional deferrals**, NOT
regressions: payments HLD ownership split (Java owns gateway/webhook/reconcile; Node keeps wallet
ledger/coupons/invoice/Meta) → see `PROMPT_D_STATUS.md`; full call lifecycle (`openCall`, queue
advancement, progress/extend wiring) → `PROMPT_E_STATUS.md`; Socket.IO transport / BullMQ / crons →
`PROMPT_F_STATUS.md`; side-effect seams (S3/push/extend/follow-list); and the contract-harness diff
(needs TEST env). PhonePe/Paytm callbacks await the newer live-branch source. Nothing money/real-time is
wired; all of it stays behind the human-review + harness gate.
All diff-pending; because they mutate, the harness must assert the resulting document, not just the
response. Next: **Prompt D (wallet/payments)** and **E (call lifecycle)** — money + real-time state,
harness + human review mandatory (do NOT blind-accept).

## Week 4 — Harden, load-test, sign off, plan Phase 2

- ✅ **Load test** for the Java `/v2` reads: `load-tests/k6-v2-read-endpoints.js` (run against
  Node `/api` and Java `/v2`, compare).
- ✅ **Parity sign-off checklist**: `PARITY_SIGNOFF.md` (per-endpoint harness diff, k6 perf,
  collection-name verification, canary rollout — the gate before `/v2` goes live).
- ✅ **Phase-2 plan**: `PHASE2_PLAN.md` (remaining reads → idempotent writes → wallet/payments
  → call lifecycle → sockets/queues/crons).

> Running the load test, recording golden responses, and diff-verifying all need the TEST
> server + tokens — they can't be executed from the build machine. Everything up to that
> gate (code + configs + checklists) is done.

## Not in Month 1 (deliberately)

Call lifecycle (`call.js`), payment writes (`transaction.js`), wallet/settlement,
Socket.IO, BullMQ queues, the 15 crons, and non-authentication admin writes. These are the money/real-time
paths and get their own later phases.

## Known items to confirm before go-live

- Two Mongo collection names are Mongoose-pluralization guesses — confirm on the
  TEST DB (`db.getCollectionNames()`): `meditation-categories`, `meditation_media`
  (see `config/AppConstants.Collections`). Wrong name → empty list, which the
  harness diff will flag immediately.
- `/master` cache key uses `userType` from the full user doc in Node. Authentication
  read-through caching is now wired, but `MasterController` still passes null, so this
  endpoint continues to use the `default` bucket (response is identical; only the cache
  bucket differs). See `MasterService`.
