# Prompt D — Wallet & payments (STARTED — SHADOW ONLY, HUMAN REVIEW REQUIRED)

> ⚠ Highest-risk work in the migration. This slice is **not wired to any live route** and must NOT
> become a source of truth until it has been shadow-run and reconciled against Node, and a human has
> reviewed it. Per COPILOT_HANDOFF.md Prompt D: *"Do NOT cut over live. First run Java in shadow…
> only switch the source of truth after balances match."*

## What was ported (first slice: the Razorpay webhook money path)

Faithful port of Node `TransactionService.confirmPaymentWithWebhook` + the credit half of
`creditAndDebitOnWallet` (`utils/classes/transaction.js`) — i.e. `POST /confirm/webhook`.

| Java | Role |
|---|---|
| `payment/PaymentWebhookService` | Idempotency orchestration (the money logic). **Unwired.** |
| `payment/WalletGateway` (port) + `payment/MongoWalletGateway` (impl) | The exact, auditable set of DB effects, behind an interface so the logic is unit-testable without a DB. |
| `payment/RazorpaySignatureVerifier` | HMAC-SHA256 webhook signature check. |
| `payment/WebhookResult` | Return shape (credited / already-processed / failed). |

New collections: `TRANSACTIONS` (`transactions`), `PAYMENT_WEBHOOKS` (`paymentwebhooks`).

## Second slice: wallet credit/debit (`creditAndDebitOnWallet`)

Faithful port of the full Node `creditAndDebitOnWallet` (credit + debit, user + consultant) + the
pure `getAdminCommissionAmount`:

| Java | Role |
|---|---|
| `payment/WalletService` | credit/debit money math, low-balance guard, ledger row, session-extend seam. **Unwired.** |
| `payment/WalletStore` (port) + `payment/MongoWalletStore` (impl) | wallet read/write + ledger insert. |

Node quirks preserved: the consult low-balance guard reproduces the JS coercion
`(remaining == 0 || (remaining*5)) < coins` (remaining==0 ⇒ `1 < coins`); `adminCommision` defaults
to 50; gift credit re-assigns `coins` to the platform-share before the ledger row. Credit writes
`transactionType:0`, debit `transactionType:1`.

**KEY REVIEWER FINDING — wallet field vs AES coins:** `creditAndDebitOnWallet` reads/writes the PLAIN
numeric `wallet` field on the user/consultant doc (exactly what Node + the Jest webhook test use). The
AES-encrypted `coins` field (`wallets` collection) is a SEPARATE balance that Node only DECRYPTS for
read-only time estimates (`transaction.js` L766/L1365). `dataEncryption` in this class (L419/432/511)
encrypts the payment/order RESPONSE for the client, NOT the wallet. **No canonical encrypted-`coins`
WRITE was found in transaction.js** — so the "coins written with encrypt" path is intentionally NOT
ported; the reviewer must confirm where/if `wallet.coins` is written encrypted (likely the
call-settlement / top-up flow) before that path is built.

## Idempotency contract (proven by tests)

Mirrors `tests-integration/payment/payment-webhook.test.js` exactly:
1. **captured + tx INITIATED** → credit `user.wallet` ONCE, write ONE ledger row, mark `COMPLETED`,
   return the invoice email/payload. Guarded by the **atomic `INITIATED→COMPLETED` transition**
   (`findOneAndUpdate({_id,status:'INITIATED'})`) — only one duplicate/concurrent webhook wins.
2. **duplicate delivery** → NO wallet credit, NO ledger row, but the webhook IS still audited; no email.
3. **non-captured** → mark tx `FAILED`, no credit, no email.
4. **unknown order** → throw `"Order is not present or this order id and user"` (audit already written).
5. **already COMPLETED/paid** (pre-check) → short-circuit, no second credit.

The webhook audit row is written FIRST, unconditionally, like Node.

## Tests — WRITTEN AND PASSING

`src/test/java/com/vedicmeet/appserver/payment/`:
- `PaymentWebhookServiceTest` — 5 idempotency cases against an in-memory `FakeWalletGateway`.
- `RazorpaySignatureVerifierTest` — 3 signature cases.
- `WalletServiceTest` — 6 credit/debit cases (balance math, low-balance guard incl. coercion quirk,
  consultant commission, gift platform-share) against an in-memory `FakeWalletStore`.
- `PaymentFromWalletServiceTest` — 4 orchestration cases (on-call guard, consult debit+credit,
  topup debit-only, gift-with-broadcast comment) over a real `WalletService` + fakes.
- `AesWalletServiceTest` — 6 AES-`coins` cases over a REAL `CryptoUtil`: debit subtract, floor-at-zero,
  consultant credit, missing-wallet=0, **never-stored-plaintext (asserts crypto-js `Salted__` output)**,
  and `jsNumber` == `JSON.stringify`.

**Executed here: 24/24 passing** (compiled against the `.m2` bson + JUnit jars and run with a reflective
runner, since there is no `mvn`/embedded-Mongo in this environment). Under Maven these run as normal
JUnit 5 tests.

## Slices now ported (all shadow-only / unwired)

| Slice | Java | Node |
|---|---|---|
| Webhook idempotency | `PaymentWebhookService`, `WalletGateway`/`MongoWalletGateway`, `RazorpaySignatureVerifier`, `WebhookResult` | `confirmPaymentWithWebhook` |
| Plain wallet credit/debit | `WalletService`, `WalletStore`/`MongoWalletStore` | `creditAndDebitOnWallet` + `getAdminCommissionAmount` |
| Spend orchestration | `PaymentFromWalletService`, `PaymentFromWalletStore`/`MongoPaymentFromWalletStore` | `paymentFromWallet` |
| **AES `coins` credit/debit** | `AesWalletService`, `WalletCoinsStore`/`MongoWalletCoinsStore` | `userWalletUpdate`/`consultantWalletUpdate` (db-query.js L870/L915) |

The AES-`coins` slice satisfies the reviewer's #1 rule: coins are READ via `CryptoUtil.decrypt` and
WRITTEN via `CryptoUtil.encrypt` (crypto-js `Salted__`, byte-compatible both directions), never stored
plain; user debit floors at 0; numbers are `JSON.stringify`-formatted before encryption.

## ⚠ Sharp edges for the reviewer (must resolve before wiring a live route)

1. **Signature is over `JSON.stringify(req.body)`** — the re-serialized parsed body, not raw bytes. The
   controller (not yet written) must HMAC over a string byte-identical to Node's `JSON.stringify`
   (no spaces, insertion order). Otherwise valid webhooks get rejected / invalid ones slip through.
2. **Two wallets**: the webhook + `creditAndDebitOnWallet` path uses the PLAIN `user.wallet`/`consultant.wallet`
   number; the AES `coins` (`wallets` collection) is a SEPARATE balance updated by the call-settlement
   path (`AesWalletService`). Confirm which is authoritative for each flow before cutover — Node uses
   BOTH and even `purchaseMembership` checks AES `coins` but debits plain `wallet` (see below).
3. **`extendOngoingSessionTime`, CleverTap, Meta, coupon rewards, PDF-invoice, FCM** are Node side-effects
   left as seams/omitted — confirm they aren't required for parity.
4. **Shadow/reconcile**: run in shadow and diff balances + ledger rows against Node before it writes as
   the source of truth.

## Not yet ported in Prompt D (remaining) — genuinely blocked on external infra / a reviewer decision
- **`purchaseMembership`** — DATA-MODEL AMBIGUITY: Node reads `coins`/`savedAmount` from a variable named
  `waitlistModel` (`require('../models').waitlist`), yet those fields live on the wallet doc. A reviewer
  must confirm the collection before this is encoded (didn't want to guess in money code).
- **payment init / create / confirm (non-webhook)** — creates Razorpay orders via the Razorpay SDK
  (external API + credentials) and encrypts the order response (`dataEncryption`); needs the SDK
  dependency + a TEST gateway to port and verify.
- **gateway callbacks (phonepe/paytm)** + **recharge-coupon rewards** — external-integration heavy.

Prompt E (call lifecycle/waitlist) and F (Socket.IO/BullMQ/crons) are untouched. All money/real-time
work stays behind the harness +
human-review gate.

---
## BLOCKED — PhonePe / Paytm gateway callbacks (source NOT in the Aug-26 snapshot)
The faithful Node source for these routes is **not present** in the production snapshot I have
(`sync-2026-08-26/new-app-server`). Verified:
- `rest-apis/modules/transaction.js` in this snapshot ends at `POST /confirm/webhook` (Razorpay only) —
  it has NO `phonepe-callback`, `paytm-callback` or `active-gateways` routes.
- The only references to phonepe/paytm/active-gateways in the whole tree are the **tests-integration**
  specs (`payment/gateway-callbacks.test.js`, `payment/active-gateways.test.js`), authored on the
  live branch AFTER this snapshot. `buildPaymentApp` mounts `transaction.js` — i.e. those routes exist
  on a NEWER `transaction.js` than the one captured here.

Per the FAITHFUL-ONLY rule I will **not fabricate** the gateway handlers from test assertions. To port
them exactly I need the newer source from the live branch:
- `rest-apis/modules/transaction.js` (the version with `/phonepe-callback`, `/paytm-callback`,
  `/active-gateways`), plus any `utils/classes` PhonePe/Paytm service it calls.

Contract captured from the tests (for when the source arrives — NOT yet implemented):
- `GET /api/v1/payment/active-gateways` → no settings doc defaults to `['paytm','phonepe','razorpay']`;
  otherwise read `userMasterSettings.data.activePaymentGateways`, lowercase, filter to the valid set.
- `POST /api/v1/payment/phonepe-callback` → event `checkout.order.completed`, body
  `payload.state==='COMPLETED'`, `payload.merchantOrderId` = our orderId,
  `payload.paymentDetails[0].transactionId`; host `https://api-preprod.phonepe.com`.
- `POST /api/v1/payment/paytm-callback` → host `https://securestage.paytmpayments.com`.

Razorpay (order create + webhook credit) IS ported and tested; that covers the v1 money path. PhonePe/Paytm
remain a documented gap pending the newer source.
---
## UPDATE — PhonePe/Paytm blocker RESOLVED (source found in bf308ee) + gateway surface ported
The newer transaction.js WAS available in `_migration_audit_20260831/node-production-bf308ee` (the same
folder as the newer call.js). Ported faithfully from it (shadow-only; the wallet-credit money core stays
UNWIRED):
- `payment/GatewayService` (+ `GatewayStore` / `MongoGatewayStore`): `getActivePaymentGateways`
  (seed_masters userMasterSettings, lowercase+filter to [paytm,phonepe,razorpay], default all three);
  `phonePeCallback` (state COMPLETED -> confirmPayment(merchantOrderId, paymentDetails[0].transactionId));
  `paytmCallback` (orderId resolution; TXN_SUCCESS/RESPCODE 01 -> confirmPayment(orderId, TXNID); PENDING/402
  and failure branches). `GatewayServiceTest` = 9 cases.
- `payment/PaymentGatewayController` (`/v2/v1/payment`): `GET /active-gateways` (authed, functional),
  `POST /phonepe-callback` + `POST /paytm-callback` (public like Node, `@MigrationWrite` gated).
- `confirmPayment` is a seam returning `PAYMENT_CONFIRM_NOT_WIRED` -> **no money moves**. The idempotent
  wallet-credit core (mirror `PaymentWebhookService`) is the remaining money step, behind human review.

Still open (need creds/HTTP or review): `phonepe-initiate` / `paytm-initiate` (gateway order-create APIs),
the `confirmPayment` credit wiring, and the outbox/reconciliation scheduler.