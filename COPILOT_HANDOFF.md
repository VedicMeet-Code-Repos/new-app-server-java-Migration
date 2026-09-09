# GitHub Copilot handoff — finish the VedicMeet Java migration

Paste the prompts below into GitHub Copilot Chat **inside the `new-app-server-java` repo**
(so it can see the existing files). Do them in order. Each prompt tells Copilot the pattern
to copy, which existing files to imitate, and how to verify.

---

## 0. First, give Copilot the project context (paste this once at the start)

```
You are working in a Java 17 / Spring Boot 3 project that is a strangler-pattern rewrite of
a Node/Express service ("new-app-server"). Rules you MUST follow:

1. Keep business logic IDENTICAL to the Node source. Only add the /v2 path prefix. Do not
   "improve" logic.
2. Share the same MongoDB — read collections with the native driver via
   MongoTemplate.getCollection(name) and org.bson.Document (NOT typed @Document entities),
   because the Node schemas use strict:false loose typing.
3. For big aggregation pipelines, copy the exact Mongo JSON from Node into a Java text block
   and parse it with Document.parse, substituting only dynamic bits. Use placeholders:
   __MEDIA_URL__ for the S3 media URL, and MongoDB extended JSON {"$oid":"<hex>"} for
   ObjectIds. See ExploreService.java and BannerService.java for the exact technique.
4. Every response uses the ApiResponse envelope: success responses via
   ApiResponse.ok(message, result); error responses via
   `new ApiResponse<>(false, 500, error.getMessage(), java.util.Collections.emptyMap())`
   inside a try/catch — matching the Node modules' {code, success, message, result} shape,
   HTTP 200 even on logical failure.
5. Auth: annotate endpoints with @RequireRole(Role.USER) / @RequireRole({Role.USER,
   Role.CONSULTANT}); public routes get no annotation. When you need the caller's Mongo
   document (for user._id etc.), inject @CurrentUser AuthPrincipal and call
   AuthUserService.load(principal).
6. reqData encryption/decryption: use CryptoService.decryptToMap / decrypt(reqData, Class) /
   encrypt(obj). AES field decryption (e.g. wallet.coins) uses CryptoUtil.decrypt.
7. Add every new Mongo collection name to config/AppConstants.Collections (mark any Mongoose
   pluralization you are unsure of with a // VERIFY comment).
8. Read the matching Node source before porting. The Node repo is the source of truth.

Study these existing files first as templates:
- content/CategoryService.java + CategoryController.java  (simple paginated read)
- content/ExploreService.java  (big aggregation via Document.parse + $facet + user._id)
- banner/BannerService.java  (two-branch service, writes, pricing, 5 aggregations)
- security/AuthUserService.java, security/RoleInterceptor.java, crypto/CryptoService.java
- web/ApiResponse.java, config/AppConstants.java

The plan and per-slice specs are in these markdown files at the repo root:
MIGRATION_STATUS.md, WEEK3_DISCOVERY_SPEC.md, BANNER_PHASE2_SPEC.md, PHASE2_PLAN.md,
PARITY_SIGNOFF.md. Follow them.
```

---

## What is already DONE (don't redo)

Ported to `/v2`: master, v1/category, v1/cms/details, v1/gallery, v1/explore,
updates/manifest.json, v1/banner (user+cons), v1/membership-discount(+details),
v1/notification(+count), v1/support(i18n+list), user/available_balance. Foundation
(crypto/JWT/auth/error-contract/gateway health), the contract-test harness
(`contract-tests/`), unit tests, Dockerfile, and the k6 `/v2` load script all exist.

## What REMAINS (the prompts below cover these)

A. Build & verify what's already ported (compile, run tests, run the harness).
B. Consultant discovery reads (WEEK3_DISCOVERY_SPEC.md).
C. Idempotent writes (profile update, follow, like/bookmark, mark-notification-read).
D. Wallet & payments (highest risk — shadow/reconcile).
E. Call lifecycle + waitlist.
F. Real-time: Socket.IO + BullMQ + crons.

---

## PROMPT A — Build, test, and wire the harness (do this before writing more code)

```
Build this project with `mvn clean test` (Java 17). Fix any compilation errors WITHOUT
changing business logic — most likely fixes are missing imports or generics. Then:
1. Confirm CryptoUtilTest and JwtServiceTest pass.
2. Start the app (`mvn spring-boot:run`) with env vars from .env.example pointed at the TEST
   Mongo/Redis (never production), and CRYPTO_SECRET_KEY/JWT_SECRET matching the Node test
   env. Hit GET /v2/health and confirm {"success":true,...,"mongo":"up"}.
3. In contract-tests/, record golden responses from the TEST Node server and diff each
   ported endpoint against Java, per PARITY_SIGNOFF.md. Fix any diffs by correcting the
   Java port (not by editing the golden). Common diff causes: wrong Mongoose collection
   name (check the // VERIFY entries in AppConstants.Collections against
   `db.getCollectionNames()` on the test DB), or a date/ObjectId serialization mismatch.
Report which endpoints are diff-clean.
```

---

## PROMPT B — Consultant discovery reads

```
Port the consultant-discovery endpoints described in WEEK3_DISCOVERY_SPEC.md, in the order
listed there. Source of truth: the Node files rest-apis/modules/user/cons.js (get-list ~L19,
popular ~L353) and rest-apis/modules/user/index.js (consultant_list, top_consultant_list,
consultant_details) with the service methods in utils/classes/user.js (consultantList ~L4270)
and utils/classes/consultant-service.js (topConsultant).

Steps:
1. Port the `checkUserHaveMembership` middleware as a small guard the controllers call (read
   its Node source first); unit-test its branching.
2. Port ConsultantService.topConsultant as a leaf service (pricing/ranking helper); unit-test.
3. Port cons/popular by copying the Node aggregation into a Document.parse text block exactly
   like the POPULAR_CONSULTANT pipeline already in banner/BannerService.java (reuse that as a
   template). Endpoint: GET /v2/user/cons/popular.
4. Port cons/get-list and consultant_details the same way.
5. Port consultant_list / top_consultant_list last. These are POST with an encrypted reqData
   body — use CryptoService.decryptToMap(body.get("reqData")) to read the request, and reuse
   pricing/OfferService (already ported) for per-consultant discount. Endpoints under
   /v2/user/... matching the Node paths.
For each: reuse @RequireRole, @CurrentUser + AuthUserService, the ApiResponse envelope, and
add a fixture to contract-tests/fixtures/discovery.json, then diff vs Node until clean.
```

---

## PROMPT C — Idempotent writes

```
Port these low-blast-radius write endpoints from Node, one at a time, keeping logic identical:
- user profile update (PUT /user/update), follow/unfollow, live_like, explore action
  (like/bookmark), notification mark_read + mark_all_read, support initiate_query / chat_send.
Find each in rest-apis/modules/**; port the service method from utils/classes/**.
Rules:
- Requests carry encrypted reqData — decrypt with CryptoService.
- Use MongoTemplate.getCollection(name).updateOne/insertOne/findOneAndUpdate with explicit
  $set/$push operators (Mongoose auto-wraps plain objects in $set; the native driver does
  NOT, so wrap them yourself). See the boost write in banner/BannerService.java for the
  pattern.
- Preserve any idempotency guard the Node code has; if it has none, do NOT add one.
- Add a contract fixture AND, because these mutate, assert the resulting document separately
  (the response diff alone won't catch a wrong write). See BANNER_PHASE2_SPEC.md note.
```

---

## PROMPT D — Wallet & payments (HIGH RISK — do carefully)

```
Port wallet and payment logic from Node, treating it as the highest-risk work. Source:
utils/classes/transaction.js, wallet logic, and the payment init/create/confirm/webhook flow.
A Node Jest test suite for payment idempotency already exists in the Node repo — mirror those
exact test cases as JUnit tests in Java first, then make them pass.
Hard rules:
- The wallet `coins` field is AES-encrypted at rest (crypto-js). Read it with
  CryptoUtil.decrypt and WRITE it back encrypted with CryptoUtil.encrypt — never store plain.
- Preserve webhook idempotency exactly (dedupe by the same key Node uses).
- Do NOT cut over live. First run Java in shadow: compute balances in Java from the same DB
  and reconcile against Node for a window; only switch the source of truth after balances
  match. See PHASE2_PLAN.md section 2.3.
```

---

## PROMPT E — Call lifecycle + waitlist

```
Port the call lifecycle (Node utils/classes/call.js) and waitlist logic. This is a state
machine: initiate -> accept (two-phase) -> progress -> end -> settlement, governed by
Redis timers. Reproduce the state transitions and timers EXACTLY.
Before porting, read the known race conditions and the waitlist race taxonomy in the team's
production-facts notes. Some current Node behaviors are bugs (e.g. status mislabelled
'missed'); for each, decide with the team whether to port faithfully or fix-forward, and
document the choice. Verify with the existing race tests referenced in TEST_SCENARIO_CATALOG.
Do this only after wallet/payments are stable, since settlement touches the wallet.
```

---

## PROMPT F — Real-time: Socket.IO + BullMQ + crons

```
Port the real-time layer per PHASE2_PLAN.md section 2.5:
- Socket.IO: match the protocol exactly — namespace /app, JWT in handshake auth.token, and
  the full event catalog (see the socket catalog in the KT docs). Use a Java Socket.IO
  server library; keep event names/payloads byte-identical.
- BullMQ queues: initially keep the queues on Node and only migrate producers/consumers
  gradually; a Java worker must use the same Redis keys/job names.
- The 15 crons: migrate one at a time and ensure single ownership so a job never fires on
  BOTH Node and Java simultaneously.
Each piece is verified by observing identical behavior against the test environment, not by
the HTTP contract harness.
```

---

## Guardrails to remind Copilot of (repeat as needed)

- Never point MONGO_READ_URI / Redis at production from a laptop or CI. Test env only.
- Never commit secrets; they come from env (.env is gitignored). Rotate anything exposed.
- Never edit golden files to make a diff pass — fix the Java.
- Keep the /v2 prefix; leave every un-ported route on Node (the gateway handles routing).
- After each endpoint: `mvn test`, run the harness, and only then enable it on /v2 (canary
  first), per PARITY_SIGNOFF.md. Banner goes live LAST and needs its boost write verified.
