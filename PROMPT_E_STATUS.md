# Prompt E — Call lifecycle + waitlist (STARTED — SHADOW-ONLY, HUMAN REVIEW + HARNESS REQUIRED)

> ⚠ Real-time state machine that also touches money at settlement. Like Prompt D it is **not wired**
> and must go through the contract/race harness + a human review before enabling. COPILOT_HANDOFF.md
> Prompt E: reproduce transitions + timers EXACTLY, and for each known Node bug decide *with the team*
> whether to port faithfully or fix-forward.

## What was ported (first slice: `initiateCall` validation gates)

Faithful port of the entry gates of Node `CallManager.initiateCall` (utils/classes/call.js L381),
mirroring the transition ORDER and the exact cancel/miss log strings.

| Java | Role |
|---|---|
| `call/CallLifecycleService` | the `initiateCall` state machine → `InitiateOutcome` enum. **Unwired.** |
| `call/CallGuardStore` (port) + `call/MongoCallGuardStore` (impl) | DB gates 1:1 with Node; pricing + redis/socket are honest seams that throw until wired. |
| `call/CallSettlementService` | call-end settlement (db-query.js L520-573): bill beyond free trial, split commission, then debit user + credit consultant **AES `coins`** via the tested `payment/AesWalletService`. **Unwired.** |
| `call/TimerWriteService` + `call/RedisTimerClock`/`RedisTimerClockImpl` | the Redis **billing-clock WRITE** ops (timer-queue.js): `registerTimer` (SETEX), `extendTimer` (TTL+EXPIRE, no-op if expired), `stopTimer` (DEL), `getRemainingTime`. Counterpart to Prompt B's read-side `TimerReadService`. **Unwired.** |
| `call/CallAcceptService` + `call/CallAcceptStore` (port) | **the two-phase `acceptCall`** (call.js L1494) as-is: ghost / partially_accepted / reconnect / progress-start (+billing clock via `TimerWriteService`) / same-acceptor no-op / already-accepted re-join. **Unwired.** |

Transitions (exactly as Node, same order):
1. user OR consultant missing → waitlist `canceled` (`"cancelled - <who> not found"`) + `callToNextUser`.
2. waitlist already `canceled`/`completed` → hard no-op (Node throws `WAITLIST_ALREADY_CLOSED`, caught → undefined).
3. `private_call` & below minimum balance → `canceled` (`"cancelled due to insufficient balance"`) + `callToNextUser`.
4. consultant toggled the mode OFF → `canceled` (`"cancelled - consultant no longer available for this mode"`).
5. either party busy (user `progress`, or consultant `progress`/`initiated`) → `missed` (`"call deferred - <who> is busy"`).
6. all gates pass → `openCall` (redis call-hash + `initiated` + Socket.IO emit + notify) — a seam.

The balance decision is a port seam (`hasMinimumBalance`) because Node computes it several ways by
coupon type (`calculateSessionPriceAndDuration` + first/second-purchase master settings); the control
flow — which transition fires — is what this slice pins.

## Tests — WRITTEN AND PASSING

`src/test/java/com/vedicmeet/appserver/call/` — **19/19 passing** (isolated Maven, JDK 17):
- `CallLifecycleServiceTest` — 6 cases mirroring the Jest "initiateCall — validation gates" suite
  (user-not-found, insufficient-balance, already-canceled no-op, mode-off, party-busy→missed, all-pass→open).
- `CallSettlementServiceTest` — 3 cases: bill-beyond-free-trial + commission split, under-free-trial = free,
  and end-to-end `settle` moving the **AES `coins`** (user debited, consultant credited, still Salted-encrypted).
- `TimerWriteServiceTest` — 4 cases: register sets TTL, extend adds while active, extend no-op when
  expired/missing, stop clears the clock.
- `CallAcceptServiceTest` — 6 cases pinning the two-phase machine: empty-redis→ghost, initiated→partially
  (emits to both + waitlist log), partially+`session_connect_again`→reconnect, partially+different-acceptor
  →progress-started (+missed-timer cancel +billing clock started for `requested_time` +`join_call_room`),
  partially+same-acceptor→no-op, accepted→re-join.

## Team decision (recorded 2026-08-31): PORT FAITHFULLY — no fix-forward
Everything in Prompt E is migrated **exactly** like the Node dev/live branch, preserving quirks/bugs
(e.g. the phase-2 accessbilities treat coupon `67d1…` as +audio while the `accepted` re-join path treats
the same coupon as a no-op — reproduced verbatim). The `missed` mislabel + waitlist-overtake races are
therefore left AS-IS pending an explicit team call to fix-forward.

## Not yet ported (remaining) — needs the transition wiring
- **`progress` / `endCall`** transitions: `endCall` stops `TimerWriteService` then triggers
  `CallSettlementService` (db-query.js `deductAmountFromUserWallet` L472). Building blocks all done + tested;
  what remains is stitching end→settlement + the Socket.IO emit (Prompt F).
- **Known races + the `missed` mislabel bug**: ported faithfully (kept), per the team decision above.

---
## CRITICAL UPDATE (2nd review) — settlement re-ported to CURRENT production (bf308ee)
The live settlement (`CallTimeoutSettlementService`) was re-ported from the newer production source
`_migration_audit_20260831/node-production-bf308ee/utils/classes/call.js` `computeCallSettlement`
(L129-261). The earlier version matched an OLD snapshot (fixed_time/first_purchase/5-coin) and was WRONG
vs production. The current model, now faithfully ported + tested (9 cases):
- `typeOfSession` = NORMAL / SESSION (used_for=session) / coupon.type when the coupon has a variantId
  (PER_MINUTE / FIXED / PERCENT), the offer loaded from offerVariant.
- platform-fee rate boosted 70 / consultant-offer 50 / else `100 - platformShare`, plus a v2 CONSULTANT
  coupon `consultantShare` override.
- base amount per type; extra-duration beyond the offer priced at `basePrice`; `consultantWallet =
  base - platform` except SESSION (0, as prod's SESSION block is commented out); `roundMoney` 2dp.

Also (finding #3): `MongoCallGuardStore.openCall` no longer throws `CALL_OPEN_NOT_PORTED` and
`callToNextUser` reference the faithful ports (`CallOpenService` / `CallQueueService`); they stay the
deliberate unwired boundary. Everything remains shadow-only behind the human-review + harness gate.