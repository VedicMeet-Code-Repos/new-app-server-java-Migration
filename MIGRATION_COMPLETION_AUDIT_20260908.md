# new-app-server Java migration — completion audit

Date: 2026-09-08  
Reference contract: cached Node production snapshot `bf308ee`  
Decision: **NO-GO for full Node replacement; GO for continued disabled shadow verification**

This is the current evidence record. Older prompt/status documents are historical and may contain stale counts.

## 1. What was completed and verified in this pass

### Build and deployment safety

- `mvn clean package` succeeds offline.
- **185 tests passed; 0 failed, 0 errors, 0 skipped.**
- The executable Spring Boot JAR was produced and started successfully in an isolated smoke run.
- The smoke run used deliberately unreachable localhost Mongo/Redis addresses and all write, call, socket, cron, and payment-reconciliation switches disabled. It made no production connection.
- JAR inspection passed: only `BOOT-INF/classes/application.yml` is packaged; no local profile, `.env`, private-key, or credential-named resource is present.
- Local profile auto-activation was removed. A developer's local profile remains outside the packaged JAR and must never be committed.

### Payments

- Payment completion now uses one Mongo transaction for the transaction status claim, wallet update, and ledger write. Any failure rolls the unit of work back.
- Wallet mutation uses atomic database updates instead of a read-decide-write balance race.
- Razorpay webhook verification uses the raw request body and HMAC before any state change.
- PhonePe callback confirmation performs an independent provider status check before wallet confirmation.
- Paytm callback confirmation validates the checksum and performs an independent provider status check before wallet confirmation.
- Duplicate confirmation is stopped by the atomic `INITIATED -> COMPLETED` claim.
- Reconciliation has three independent controls: the global migration-write switch, the cron switch, and the payment-reconciliation switch. All default to `false`.
- The distributed Redis cron lock prevents multiple Java instances from running the same reconciliation tick.

### Call and real-time safety

- Two-party acceptance uses one atomic Redis Lua transition, removing the Java port's read-decide-write race.
- Cancellation removes both the missed-call timer and the duration timer.
- Extension changes `<waitlistId>:duration`, the active billing clock, rather than the expired missed-call timer.
- Read-side Socket.IO behavior is wired and tested for `verify_end_call`, `check_call_status`, `room_remaining_time`, room join, and room leave.
- Mobile-visible edge cases are test-pinned: empty-call verification, completed-waitlist cleanup, Exotel end-call rejection, and duration clock behavior.
- Mutating call events remain deliberately unregistered. Setting `CALL_EXECUTION_ENABLED=true` fails startup with a clear error instead of exposing a partial call owner.

### Media

- `MediaUploadService` is no longer a throwing stub. It uses S3 with the default AWS credential chain and is covered by isolated tests.
- A real TEST-bucket upload/download/delete check is still required before enabling media routes.

## 2. Verified test evidence

| Area | Evidence |
|---|---|
| Full build | 185 tests green and executable JAR produced |
| Call lifecycle primitives | accept, cancel, end, extend, open, queue, settlement, timeout, timer tests |
| Socket contract | 14 dispatcher tests plus 4 handshake tests |
| Payment | initiate, order, confirmation, wallet, webhook, reconciliation, Razorpay, PhonePe, Paytm tests |
| Cron safety | dedicated off-switch test and distributed-lock execution test |
| Media | 5 isolated S3 service tests |
| Security/compatibility | JWT, role interceptor, crypto, migration-write interceptor |
| Packaging | JAR resource-name inspection passed |
| Runtime | isolated executable-JAR startup and graceful shutdown passed |
| Contract harness | 5 JavaScript files pass syntax validation; **0 recorded golden responses** |

## 3. What is still genuinely incomplete

### A. Full write-side call ownership — blocking

Java must not accept call writes until it owns the complete lifecycle as one unit. The following are still missing or deliberately fail-closed:

1. The booking/initiation HTTP owner and atomic next-waitlist claim.
2. Production implementations behind the call socket write dispatcher (`accept_call`, `cancel_call`, `end_call`, `extend_call`).
3. A durable delayed-job worker equivalent to Node's missed-call and duration BullMQ jobs.
4. The end-to-end timeout settlement orchestrator: atomic waitlist completion, consultant credit, guarded user debit, both ledger rows, cleanup, and retry behavior.
5. Working FCM/VoIP transport. `PushNotificationService` currently builds the payload but intentionally sends nothing.
6. Next-user queue chaining, reconnect grace, socket room eviction, analytics, call summary, and missed-call retry integration.
7. Multi-instance Socket.IO fan-out through Redis.

Why it remains blocked: Node delayed jobs survive simple Redis TTL deletion. If Node and Java both partly own one call, both may timeout or settle it. Controller wiring alone would create ghost calls or double settlement.

### B. Background workers and crons — partial

- Java has the distributed lock, expired-recharge-offer job, and payment reconciliation job.
- The remaining Node notification, availability, ranking, lifecycle-marketing, Meta retry, and missed-call retry schedules are not fully ported.
- BullMQ delayed-call ownership is not ported.
- Cron execution requires both `CRON_ENABLED=true` and `MIGRATION_WRITES_ENABLED=true`; payment polling additionally requires `PAYMENT_RECONCILIATION_ENABLED=true`.

### C. REST surface — partial

- Java currently contains **55 Spring mapping annotations** across 17 controllers.
- The production Node snapshot contains **688 static route declarations** and 98 router mounts.
- The existing Java surface covers core foundation, content/discovery reads, selected idempotent writes, payment, support/social portions, and health.
- Long-tail domains such as community, horoscope/kundali, music/meditation, remedies, complete session history, complete wallet history, and many consultant/admin operations remain incomplete.
- The separate admin panel/backend surface is not migrated.

Counts are a navigation indicator, not a percentage: one Node declaration and one Spring annotation are not always equivalent units of functionality.

### D. External verification — blocking production enablement

The contract harness has no golden responses yet. These gates require controlled TEST infrastructure and cannot be replaced by unit tests:

1. Record Node TEST responses and diff every Java `/v2` response.
2. Confirm every `VERIFY` Mongo collection name against the TEST database.
3. Run payment sandbox callbacks, duplicate deliveries, delayed deliveries, provider timeout, and reconciliation shadow reports.
4. Run Socket.IO protocol tests using TEST mobile clients and a TEST Redis deployment.
5. Run 50/100-user load and concurrency scenarios against TEST only.
6. Perform a TEST S3 upload/download/delete smoke test using least-privilege IAM.
7. Obtain human sign-off for wallet/ledger totals and call settlements before routing traffic.

## 4. Required activation order

Do not enable everything together.

1. Keep Node as the owner; run Java read endpoints in shadow mode.
2. Record and pass contract diffs for one endpoint at a time.
3. Canary low-risk read endpoints on `/v2`, with instant route-back to Node.
4. Validate media in a TEST bucket, then canary media separately.
5. Validate payments in provider sandboxes and run reconciliation in shadow/report-only mode.
6. Enable payment creation first; enable callback ownership only after duplicate and recovery tests pass.
7. Build one Java-owned call runtime including booking, sockets, durable timers, notifications, and settlement.
8. Transfer call ownership atomically at the gateway; never split one live call between Node and Java.
9. Port the long-tail modules by business domain.
10. Treat admin migration as a separate project.

## 5. Current safety switches

All high-risk switches must stay off by default:

```text
MIGRATION_WRITES_ENABLED=false
SOCKET_ENABLED=false
CALL_EXECUTION_ENABLED=false
CRON_ENABLED=false
PAYMENT_RECONCILIATION_ENABLED=false
PHONEPE_ENABLED=false
PAYTM_ENABLED=false
```

No production rollout is approved by this audit.

## 6. Repeatable local verification

```powershell
# Uses the already-populated isolated Maven cache.
& '.\.maven-home\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd' `
  '-Dmaven.repo.local=C:\Users\tiwaris5\vedicmeet-build\repo' -o clean package

# Contract harness syntax only (the real diff still needs TEST URLs/tokens/fixtures).
Get-ChildItem .\contract-tests\*.js | ForEach-Object { node --check $_.FullName }
```

Expected build result: `Tests run: 185, Failures: 0, Errors: 0, Skipped: 0`.

## 7. Senior-review handover

### What was unsafe

- Local deployment credentials could be packaged/activated.
- Payment status, wallet, and ledger changes could become partially committed.
- Callback trust was incomplete for PhonePe/Paytm.
- Two simultaneous call accepts could race.
- Call cancel/extend targeted incomplete or incorrect timer keys.
- Operators could mistake partial Socket.IO code for a complete call owner.
- Payment reconciliation could be enabled too broadly with the general cron switch.

### What changed

- Packaging now excludes local credentials and has no automatic local profile.
- Payment state changes are transactional and callback confirmation is provider-backed.
- Payment reconciliation is lock-protected and independently gated.
- Call acceptance is atomic; cancellation and extension use the correct clocks.
- Safe read-side sockets are wired; all write-side calls fail closed.
- Media S3 code is implemented behind environment/IAM configuration.
- Tests and packaging evidence were expanded to 185 green tests plus a clean executable JAR.

### Review decision requested

- Approve the safety hardening for continued shadow/testing use.
- Do **not** approve full Node cutover.
- Approve a separate call-owner milestone and TEST contract/payment verification window before any write traffic is routed to Java.
