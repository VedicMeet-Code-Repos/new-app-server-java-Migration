# Consultant discovery reads — scoped slice (not in the first Week-3 batch)

The consultant-discovery endpoints are the one Week-3 group deliberately deferred, for the
same reasons banner was: they are large, gated, and pricing/ranking-sensitive. They should
be ported as their own focused slice with the harness live.

## Endpoints

| Endpoint | Node | Notes |
|---|---|---|
| `POST /user/consultant_list` | `user/index.js` → `UserService.consultantList` (~900 lines, user.js 4270+) | membership-gated (`checkUserHaveMembership`), encrypted `reqData`, big ranking aggregation + per-consultant offer pricing + `topConsultant` checks |
| `POST /user/top_consultant_list` | same service family | membership-gated, encrypted |
| `GET  /user/consultant_details` | `user/index.js` | membership-gated; single-consultant detail with rating/offers |
| `GET  /user/cons/get-list` | `user/cons.js` (lines 19–125) | consultant list variant |
| `GET  /user/cons/popular` | `user/cons.js` (lines 353–762, ~400 lines) | popular ranking, similar to the banner popular aggregation |

## Why deferred

- **Pricing + ranking are user-visible and high-stakes** — a subtle diff changes who is
  shown and at what price. Must be diff-verified, not shipped blind.
- **`checkUserHaveMembership` middleware** must be ported first (it gates and may branch
  behavior on membership).
- **Encrypted `reqData` request bodies** (POST) — reuse `CryptoService`, but each schema
  must be matched.
- **Depends on `UserService.getConsOfferDiscountForUser`** (already ported in
  `pricing/OfferService`) and `ConsultantService.topConsultant` (not yet ported).

## Port order (harness-gated, each step diffed)

1. Port `checkUserHaveMembership` as a guard/interceptor; unit-test its branching.
2. Port `ConsultantService.topConsultant` (leaf); unit-test.
3. Port `cons/popular` (closest to the already-ported banner popular aggregation — reuse
   that pipeline structure) → fixture → diff.
4. Port `cons/get-list` and `consultant_details` → fixtures → diff.
5. Port `consultant_list` / `top_consultant_list` last (largest; encrypted POST + pricing)
   → fixtures (encrypted `reqData`) → diff.
6. Enable on `/v2` per-endpoint only when diff-clean.

Reuse: `AuthUserService` (req.user), `CryptoService` (reqData), `OfferService` (pricing),
`Document.parse` for the big aggregations (as done for banner/explore).
