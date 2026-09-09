# `/v1/banner` — Phase-2 porting spec (deliberately NOT in the read slice)

`GET /v1/banner` (`banner.home` → `_fetchBannerHome`) is **not** a low-risk read and
is intentionally left on Node for now. Under the strangler model that is the correct
default: unmigrated routes keep serving from Node until ported and proven. This file is
the complete plan to port it as a dedicated slice, with the contract harness live.

## Why it is not in the read slice

1. **It writes.** The consultant branch calls
   `boostModel.findOneAndUpdate({consultantId}, {callActive:false,chatActive:false,videoActive:false})`
   when there is no boost record for today, and `ConsultantService.getCurrentTimeStringAndCheckBoostTime("updateTimeDuration", ...)` mutates boost state. Porting it means porting write behavior — must be verified, not guessed.
2. **It prices.** The user branch calls `UserService.getConsOfferDiscountForUser(...)` per
   listed consultant to compute `discountPrice`. Pricing logic shown to users is high-stakes.
3. **Large transitive surface.** It depends on `ConsultantService` (217-line boost-time
   method), `UserService` (57-line offer method), and local helpers
   `loadConsultantBannerMetrics` / `buildConsultantBannerData` / `CONSULTANT_BANNER_CONFIG`
   / `getConsultantBannerDayRange` / several pipeline helpers, plus five aggregations
   (popular-consultant ranking, explore feed, promocode, feedback) and `seed_master` reads.

Shipping ~600 lines of this blind (no compile/diff available here) on money/behavior
paths is exactly the risk the migration plan exists to prevent.

## Port order (each step diffed via the harness before the next)

1. **Port the leaf dependencies first, with unit tests:**
   - `UserService.getConsOfferDiscountForUser(userId, consultantId, charge)` — reads
     `coupons` (OFFERS), `consultant_requests`, `consultant_request_forms`; returns
     `{status, discountPrice}`. Pure read; port + unit test against known coupon rows.
   - `ConsultantService.getCurrentTimeStringAndCheckBoostTime(type, boostData, boostType)`
     — port the `currentTime` and `updateTimeDuration` branches; **preserve the boost
     update write exactly**; unit-test the time math and the write payload.
   - Banner metric helpers (`loadConsultantBannerMetrics`, `buildConsultantBannerData`,
     day-range + pipeline helpers, `CONSULTANT_BANNER_CONFIG`, `seed_master` reads).
2. **Port `_fetchBannerHome` branch by branch:**
   - Shared: the banner-list read (`banner.find(params).populate(categoryId).sort({_id:-1})`,
     1h cache under `banner:list:{userType}:{status}`) — note Node populates `categoryId`
     (a `$lookup` on categories) — reproduce with an aggregation `$lookup`.
   - `cons` branch: notice count, master `applicationDetails`, availability, active fixed
     sessions, boost read + the write, `consDetails`, metrics/bannerData, warning count.
   - `user` branch: `seed_master` userMasterSettings, dynamic banner list vs problem-filter
     + shuffle, popular-consultant aggregation (language priority, ranking, rating, offers),
     explore aggregation (reuse the one already ported in `ExploreService`), promocode,
     feedback (video from seed_master + text feedback aggregation), `newUserInPlatform`,
     per-consultant `getConsOfferDiscountForUser`, membership.
3. **Fixtures:** add `contract-tests/fixtures/banner.json` for both `userType=user` and
   `userType=cons` (the latter must assert the boost write side-effect separately, since
   the response diff alone won't catch a wrong write).
4. Enable on `/v2` only when the diff is clean AND the boost write is verified.

## Collections / lookups it touches
`banners`, `categories`, `masters`, `notice_boards`, `consultant_availables`, `waitlists`,
`consultant_boosts`, `consultants`, `stats`, `seed_masters`, `review_and_ratings`,
`coupons`, `explores`, `comments`, `users`, `feedbacks`, `warnings`,
`consultant_requests`, `consultant_request_forms`. Confirm each name on the test DB.
