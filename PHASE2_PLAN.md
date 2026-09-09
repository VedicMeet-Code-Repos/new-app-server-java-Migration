# Phase 2 plan — beyond the Month-1 read migration

Month 1 stood up the Java service alongside Node (shared Mongo/Redis), proved the
foundation (crypto/JWT/error-contract/gateway/harness), and migrated the low-risk **read**
surface to `/v2`. Phase 2 takes on the harder, higher-stakes modules. Same discipline
throughout: strangler, `/v2` per-endpoint, contract-harness gate, canary, instant rollback.

## Guiding rules (unchanged)
- One endpoint at a time; only enabled on `/v2` after a clean harness diff.
- **Writes and money paths get extra verification** — assert the DB side-effect, not just
  the response.
- Reuse what's built: `AuthUserService`, `CryptoService`, `OfferService`, the
  `Document.parse` pipeline approach, `ApiResponse`, `@RequireRole`.

## Order (lowest risk first)

### 2.1 Remaining reads (finish the read surface)
- Consultant discovery: `cons/popular`, `cons/get-list`, `consultant_details`,
  `consultant_list`, `top_consultant_list` — see `WEEK3_DISCOVERY_SPEC.md`. Port
  `checkUserHaveMembership` + `ConsultantService.topConsultant` first.
- Banner: finish diff + **boost-write** verification, then enable (see `BANNER_PHASE2_SPEC.md`).

### 2.2 Idempotent-ish writes (low blast radius)
- Profile updates, follow/unfollow, likes/bookmarks, mark-notification-read,
  support query create. Each: port validators, reuse `CryptoService` for `reqData`,
  assert the write in the harness.

### 2.3 Wallet & payments (highest stakes) — do NOT rush
- Wallet read/debit/credit, recharge, coupon apply, settlement.
- Port the payment init/create/confirm/webhook idempotency logic (a Node test suite for
  this already exists — mirror those cases in Java).
- Gate: reconcile Java-computed balances against Node for a shadow-traffic window before
  any real cutover. Consider dual-write/shadow-read before switching the source of truth.

### 2.4 Call lifecycle (`call.js`) + waitlist
- Initiate → accept (two-phase) → progress → end → settlement. Reproduce the timer/state
  machine exactly; this is where the known race conditions live (see production-facts).
- Fix-forward vs faithful-port decision per bug (some current behaviors are bugs).

### 2.5 Real-time: Socket.IO + BullMQ queues + crons
- Socket.IO protocol parity (namespaces `/app`, handshake `auth.token`, event catalog).
- BullMQ job semantics (Java worker or keep queues on Node initially and only migrate
  producers/consumers gradually).
- The 15 crons: move one at a time; ensure single-owner (avoid double-firing across
  Node + Java).

## Cross-cutting Phase-2 work
- **AuthCache read-through in Redis** — AuthUserService currently reads Mongo directly;
  add the Redis cache layer Node uses, for load parity.
- **S3 bundle backend** for `/updates` (currently local-disk only; S3 path stubbed).
- **Observability**: wire Sentry + metrics dashboards for `/v2` on par with Node.
- **Collection-name audit** completed and the `VERIFY` markers removed from `AppConstants`.
- **Secret hygiene**: all secrets via env/secret-manager; rotate anything exposed.
- **Load & soak tests** extended to write paths (carefully, on TEST only).

## Exit criteria for Phase 2
- Read surface 100% on `/v2`; idempotent writes migrated and reconciled; wallet/payments
  running in shadow with matched balances; a dated, risk-assessed plan for the call
  lifecycle + real-time cutover. No big-bang switch — Node remains the fallback per route
  until each is proven.
