# Prompt F — Real-time: Socket.IO + BullMQ + crons (STARTED — infra core only; MOSTLY BLOCKED here)

> COPILOT_HANDOFF.md Prompt F: match the Socket.IO protocol exactly (namespace `/app`, JWT handshake
> `auth.token`, byte-identical event catalog); keep BullMQ queues on Node initially and migrate
> producers/consumers gradually with the SAME Redis keys/job names. Most of this needs external
> infrastructure that is not available in this environment.

## What was ported (the portable + testable pieces — no secrets, no live connections)

### 1. The cron distributed lock
Faithful port of Node `withRedisLock` / `withCronLock` (utils/classes/redis-lock.js) — the safety
primitive that guarantees a scheduled job runs on **exactly one** instance at a time.

| Java | Role |
|---|---|
| `cron/RedisLockService` | `withRedisLock(key, fn, ttl)` + `withCronLock(jobName, task, ttl)`. **Unwired.** |
| `cron/RedisLock` (port) + `cron/RedisLockImpl` (impl) | `SET NX EX` acquire + the exact Node Lua compare-and-delete safe release. |

Tests — `cron/RedisLockServiceTest` — **4/4 passing** (winner-runs-then-releases, holder-skips, release-on-exception, returns-result).

### 2. The Socket.IO handshake JWT authenticator
Faithful, library-agnostic core of the Socket.IO auth gate: verify the JWT the client sends in the
handshake (`handshake.auth.token`) with the SAME HS256 `JwtService` the REST layer uses, so socket and
HTTP accept identical tokens.

| Java | Role |
|---|---|
| `realtime/SocketHandshakeAuthenticator` | `authenticate(rawToken)` → `AuthPrincipal` (strips `Bearer`, rejects missing/invalid). **Unwired.** |

Tests — `realtime/SocketHandshakeAuthenticatorTest` — **4/4 passing** (valid token → principal, `Bearer`
stripped, missing rejected, tampered rejected).

## Genuinely blocked in this environment (needs external infra + a running Node peer)
- **Socket.IO server wiring**: the `/app` namespace + `ConnectListener` (which calls the authenticator
  above) + the **event catalog** need a Java Socket.IO **server library** (e.g. `netty-socketio`, added to
  `pom.xml`) and a **live Node socket server** to verify byte-for-byte protocol parity. The authoritative
  event catalog lives in the KT docs / a socket-server file not in this snapshot — I will not fabricate
  event names. **Not** unblocked by credentials, and I must not point a socket/Redis client at production.
- **BullMQ**: keep the queues on Node and migrate gradually; a Java worker must use the SAME Redis
  keys/job names (e.g. `timer-fired`). Needs Redis + the BullMQ wire format; not portable/verifiable here.
- **@Scheduled cron triggers + task bodies**: the cron tasks (Interakt/CleverTap re-engagement events)
  are external-integration heavy; the `RedisLockService` above is what every cron needs first.

> A note on credentials: real production secrets are **not** required to finish Prompt F, and must not be
> used from this environment — the blockers are a client library + a TEST Node peer + TEST Redis, in a
> proper dev environment, never production keys.

All real-time work stays behind the harness + human-review gate.

---

## UPDATE — more of Prompt F now built by referencing the Node source (still shadow-only)

After reading the Node socket layer (`sockets/index.js`, `sockets/namespaces/app.js`) and the
integration clients (`interakt-helper.js`, `clever.js`), the following are now in place:

### 3. Socket.IO server scaffold + event catalog (dependency added)
- **`pom.xml`**: added `com.corundumstudio.socketio:netty-socketio:2.0.9`.
- **`realtime/SocketEvents`**: the byte-identical event catalog (23 incoming + 8 outgoing) transcribed
  from `app.js` + `call.js`. **Tested** (`IntegrationClientsTest.socketEventNames_matchNodeCatalog`).
- **`realtime/SocketIoConfig`** (✅ now COMPILES against `netty-socketio:2.0.9` — verified by `mvn clean compile`): the `/app`
  namespace, handshake JWT auth via `SocketHandshakeAuthenticator`, `join room = userId` on connect,
  leave on disconnect, and the event-listener registration skeleton. **Gated OFF** by
  `vedicmeet.socket.enabled=false` so it never auto-starts.
- Still needed: the Redis pub/sub adapter for multi-instance fan-out, and **verification against a live Node peer**.

### 5. Call socket handler bodies (the app-namespace CALL events) — NOW PORTED
Faithful port of the call-lifecycle handler bodies in `sockets/namespaces/app.js` L321-473 as a pure
routing layer (`realtime/CallSocketDispatcher` + `realtime/CallSocketBackend` seam). It resolves the
actor from the authenticated handshake (`socket.userType`/`socket.user`), runs the same guard queries,
and delegates to `CallManager` exactly as Node does. Events covered: **accept_call, cancel_call,
end_call, verify_end_call, extend_call, join_room, leave_room**.

Node quirks preserved verbatim:
- accept_call maps the actor to `userId` XOR `consultantId` (the other is `null`).
- cancel_call: a **consultant** cancelling a live **session** forces `status='accepted'` then
  `handleCallTimeout(callEndedBy:'cons')` (settlement), while everyone else hits plain `cancelCall`.
- end_call: the session-consultant **retry-then-force-accept** path — first cancel re-invites the
  consultant (`initiateSessionCallToConsultant`, no settlement); only the second (`isConsultantCancelled`)
  forces `status='accepted'` before `handleCallTimeout(callStatus:'ended', callEndedBy:userType)`.
- verify_end_call is true **only** when redis status === 'completed'; extend_call emits
  `extend_call_failed` when the clock can't be extended.

Tests — `realtime/CallSocketDispatcherTest` — **10/10 passing** (actor mapping ×2, cancel session/plain/no-op,
end not-found/session-retry/normal-settle, verify, extend-failed). The transport that would invoke this
stays disabled (`vedicmeet.socket.enabled=false`); the delegate targets (`CallManager.*`) are the E
services and remain unwired.

### 4. Third-party clients (Interakt via micro-service, CleverTap) — for cron task bodies
- **`integrations/InteraktClient`** → `POST ${MICRO_SERVICE_URL}/segmentation/interakt-event` (Interakt
  creds live in the micro-service, not here). **Tested.**
- **`integrations/CleverTapClient`** → `POST https://api.clevertap.com/1/upload` with account-id/passcode
  **from config env (never hardcoded)**. **Tested.**
- **`integrations/HttpJsonClient`** (port) + **`JdkHttpJsonClient`** (JDK HttpClient impl).

### BullMQ — deliberately still on Node
Per the handoff, producers/consumers stay on Node for now; a future Java worker must reuse the same
Redis keys/job names (e.g. `timer-fired`). `TimerWriteService` leaves that enqueue as a documented seam.

**What still truly needs YOU / a real environment:** run `mvn` (fetch netty-socketio + build), stand up a
**TEST** Node socket peer + **TEST** Redis to verify Socket.IO/BullMQ parity, and the team decision on
the call-race behaviours. No production secrets required at any point.
