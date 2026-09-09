# Parity sign-off checklist (Week 4)

The gate before any migrated endpoint serves real traffic on `/v2`. An endpoint is
signed off only when every box for it is checked. Nothing here can be run from the build
machine alone — it needs the TEST server, a test JWT/consultant token, and the running
Java service.

## Always-on parity gates (run every build)

- [ ] `mvn test` green — `CryptoUtilTest` (crypto-js AES) + `JwtServiceTest` (HS256).
      These prove the two hard compatibility gates on fixed vectors.

## Per-endpoint gate (contract harness)

For each endpoint, record golden from Node then diff Java:

```
NODE_BASE_URL=https://<test-node> USER_JWT=<t> CONS_JWT=<t> CONSULTANT_ID=<id> node contract-tests/record.js
JAVA_BASE_URL=http://<java> USER_JWT=<t> CONS_JWT=<t> CONSULTANT_ID=<id> node contract-tests/run.js
```

| Endpoint | Diff clean | Notes |
|---|---|---|
| `GET /master` | ☐ | |
| `GET /v1/category` | ☐ | |
| `GET /v1/cms/details` | ☐ | |
| `GET /v1/gallery` | ☐ | needs a real `CONSULTANT_ID` |
| `GET /v1/explore` | ☐ | needs `USER_JWT` |
| `GET /updates/manifest.json` | ☐ | needs `update-bundles/manifest.json` present |
| `GET /v1/membership-discount` (+ `/details`) | ☐ | |
| `GET /v1/notification` (+ `/count`) | ☐ | |
| `GET /v1/support/i18n` (+ list) | ☐ | i18n needs the locale resources bundled |
| `GET /user/available_balance` | ☐ | verify a wallet with encrypted `coins` decrypts correctly |
| `GET /v1/banner` (user) | ☐ | |
| `GET /v1/banner` (cons) | ☐ + **write check** | ALSO assert the `consultant_boosts` write; ignore wall-clock boost fields |

## Load / performance (k6)

- [ ] Run `load-tests/k6-v2-read-endpoints.js` against Java `/v2` and against Node `/api`.
- [ ] Java p95 latency ≤ Node p95 (or within an agreed margin) for each endpoint.
- [ ] `http_req_failed` < 1% at 100 VUs.
- [ ] No error/latency regression in Sentry during a canary window.

## Collection-name verification (one-time, on the TEST DB)

Confirm the Mongoose-derived names flagged `VERIFY` in `AppConstants.Collections`:

- [ ] `meditation-categories`  [ ] `meditation_media`  [ ] `cms`  [ ] `galleries`
- [ ] `explores`  [ ] `banners`  [ ] `seed_masters`  [ ] `notifications`

```
// on the test DB:
db.getCollectionNames().sort()
```

## Rollout

- [ ] Enable each endpoint on `/v2` at the gateway for a canary % only after its row above
      is fully checked.
- [ ] Instant rollback path confirmed (gateway routes the path back to Node).
- [ ] `banner` enabled LAST, only after its write is verified.
