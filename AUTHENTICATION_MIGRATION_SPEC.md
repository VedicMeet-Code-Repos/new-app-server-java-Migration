# VedicMeet Authentication Migration Specification

**Node contract:** `new-app-server` production snapshot `bf308ee`  
**Java target:** Spring Boot 3 / Java 17 under `/v2`  
**Status:** implemented and locally tested; disabled for writes; TEST-environment contract verification is still mandatory.

## Decision legend

- **COMPATIBLE** — must remain unchanged until mobile/admin clients are deliberately upgraded.
- **SECURITY FIX** — unsafe Node behavior is not copied, while route and successful response compatibility are retained.
- **CLIENT CHANGE** — cannot be enabled until clients are changed and released.

## 1. Endpoint inventory

Java uses the same relative paths under the strangler prefix `/v2`.

| Method | Existing Node path | Java path | Authentication | Purpose |
|---|---|---|---|---|
| POST | `/api/auth/check-consultation-status-with-this-device` | `/v2/auth/check-consultation-status-with-this-device` | Public | Count completed consultations for a device UUID |
| POST | `/api/auth/send-otp` | `/v2/auth/send-otp` | Public, write-gated | Send user/consultant OTP |
| POST | `/api/auth/resend_otp` | `/v2/auth/resend_otp` | Public, write-gated | Resend OTP |
| POST | `/api/auth/cons/verify-otp` | `/v2/auth/cons/verify-otp` | Public, write-gated | Verify consultant primary/secondary phone OTP |
| POST | `/api/auth/user/verify-otp` | `/v2/auth/user/verify-otp` | Public, write-gated | Verify user OTP and decide existing/new user |
| POST | `/api/auth/user/register` | `/v2/auth/user/register` | Public, write-gated | Register user and create dependent records |
| GET | `/api/auth/verify` | `/v2/auth/verify` | Optional JWT | Validate token and return account + app details |
| GET | `/api/auth/details` | `/v2/auth/details` | User/consultant | Return current account details |
| POST | `/api/auth/login` | `/v2/auth/login` | Public, write-gated | User/consultant phone or legacy email login |
| POST | `/api/auth/social-login` | `/v2/auth/social-login` | Public, write-gated | Social identity lookup/login |
| DELETE | `/api/auth/delete` | `/v2/auth/delete` | User/consultant, write-gated | Soft-delete account and cancel active work |
| PUT | `/api/auth/log_out` | `/v2/auth/log_out` | User/consultant, write-gated | Single-device or all-device logout |
| GET | `/api/auth/version` | `/v2/auth/version` | Public | Read app version record |
| POST | — | `/v2/auth/device/register` | User/consultant, write-gated | Additive safe endpoint to rotate device credentials |
| POST | `/api/v1/cons/signUp` | `/v2/v1/cons/signUp` | Public, write-gated | Consultant registration; JSON and multipart |
| PUT | `/api/admin/consultant/approve` | `/v2/admin/consultant/approve` | Admin, write-gated | Approve or reject consultant |
| POST | `/api/admin/auth/login` | `/v2/admin/auth/login` | Public | Admin/sub-admin BCrypt login |
| GET | `/api/admin/auth/list` | `/v2/admin/auth/list` | Admin | List sub-admins |
| POST | `/api/admin/auth/sign_up` | `/v2/admin/auth/sign_up` | Admin or gated first-admin bootstrap | Create admin/sub-admin |
| PUT | `/api/admin/auth/update` | `/v2/admin/auth/update` | Admin, write-gated | Update sub-admin |
| PUT | `/api/admin/auth/status_update` | `/v2/admin/auth/status_update` | Admin, write-gated | Block/unblock sub-admin |
| GET | `/api/admin/auth/details` | `/v2/admin/auth/details` | Admin | Read sub-admin details |
| PUT | `/api/admin/auth/change_password` | `/v2/admin/auth/change_password` | Admin/sub-admin, write-gated | Change own password |
| GET | `/api/admin/auth/forgot_password` | `/v2/admin/auth/forgot_password` | Public, write-gated | Issue one-time password-reset token by email |
| PUT | `/api/admin/auth/reset_password` | `/v2/admin/auth/reset_password` | Public, write-gated | Consume reset token and change password |

The Node `/api/auth/create-consultant` diagnostic route is intentionally omitted: its useful code is unreachable and it is not a supported client contract.

## 2. Request DTOs

### Mobile authentication

- `SendOtpRequest`: `userType`, `phone|mobile`, `phonePrefix|countryCode`, `deviceToken`, `isSecondaryNumber`.
- `VerifyOtpRequest`: same phone aliases, `otp`, optional `isSecondaryNumber`.
- `UserRegistrationRequest`: phone aliases, `name`, `email`, birth/profile fields, problems, device/FCM identifiers, source and UTM fields; unknown legacy fields are tolerated.
- `LoginRequest`: `userType`, exactly one of email or phone aliases, OTP for phone login, `deviceToken`, `fcmToken`, `deviceType`, optional UUID/VoIP/preferences.
- `SocialLoginRequest`: `userType`, `email`, optional `provider`, `identityToken`, and device fields.
- `LogoutRequest`: `userType`, `logOutFrom=single|all`; single logout also needs FCM and device tokens.
- `DeviceRegistrationRequest`: user type plus FCM, device, UUID, platform, and VoIP token.

### Consultant registration

`ConsultantSignupRequest` preserves the existing broad, schemaless payload: identity, phone aliases, profile, skills, languages, problems, experience, price, source, referral, address, social links, FCM/device values, and additional legacy fields.

### Admin

- Login: email/password.
- Signup: role/name/email/password/rights. Password is required for `admin`; a temporary password is generated for `sub-admin` to preserve Node behavior.
- Update/status/details: `subAdminId` plus relevant fields.
- Change password: old/new/confirm.
- Forgot: email.
- Reset: token/password/confirmPassword.

## 3. Response DTOs

### Mobile/consultant envelope

```json
{"success":true,"code":200,"message":"...","data":{}}
```

Fields that Node omits remain nullable and are omitted. In particular, user OTP failures remain HTTP 200 without `code`.

### Admin envelope

```json
{"success":true,"code":200,"message":"...","result":{},"error":false}
```

Do not globally replace `data` with `result` or normalize every error to HTTP 4xx; existing apps rely on the historical shapes.

## 4. Validation and alias rules

- `phone` and `mobile` are accepted; conflicting non-empty values are rejected.
- `phonePrefix` and `countryCode` are accepted; conflicting values are rejected.
- Phone is exactly ten numeric digits. Prefix is required for phone flows.
- `userType` accepts only `user` or `cons`; internal role is normalized to `user` or `consultant`.
- Login requires exactly one identity: email XOR phone. Phone login requires OTP.
- Email is trimmed, lowercased, and validated.
- Admin passwords require at least eight characters, uppercase, number, and special character.
- Object IDs are validated before Mongo operations.
- Device credentials required by the existing login contract remain mandatory.

## 5. MongoDB document structures

The migration uses raw `Document` operations because the Node Mongoose models are `strict:false` and contain historical fields.

### `users`

- Root: `_id`, `name`, optional legacy `email`, `userId`, `referId`, `status`, `isDeleted`, `isVerified`, `wallet`, `savedAmount`, `deviceType`, `voipToken`, `authTokenVersion`.
- `details`: `email`, `phone`, `phonePrefix`, gender/birth/location/profile fields.
- `device`: FCM tokens, device tokens, UUIDs, last device, optional VoIP token.
- Existing extra fields are preserved.

### `consultants`

- Root aliases: `email`, `mobile`, `countryCode` plus `details.email/phone/phonePrefix`.
- Approval/state: `isAdminVerify`, `status`, `isDeleted`, `isActive`, `authTokenVersion`.
- Registration profile, skills, languages, prices, certificate list, device data, referral ID and consultant business ID.

### `admins`

- `_id`, `role=admin|sub-admin`, `name`, normalized `email`, BCrypt `password`, `rights`, `status`, `authTokenVersion`.
- Temporary reset fields: `passwordResetNonceHash`, `passwordResetExpiresAt`; removed atomically on use.

### Dependent collections

- `wallets`: account wallet created during user/consultant registration.
- `communitymappings`: family mapping created/repaired for users (**VERIFY exact name in TEST**).
- `consultant_boosts`: default consultant boost row.
- `refers`: consultant referral relation.
- `notificationns`: persisted notification records.
- `waitlists`: active user sessions cancelled during account deletion.
- `consultantformrequests`: consultant requests cancelled during deletion.
- `app_version_models`: version endpoint (**VERIFY exact name in TEST**).

## 6. User registration flow

1. Normalize aliases and validate phone/email.
2. Optionally require a short-lived OTP-verification receipt; disabled initially for old-client compatibility.
3. Check existing user by phone, then email. Existing active account returns its token/document.
4. Build the Node-compatible user document, including business ID, referral code, zodiac, device/profile defaults and status flags.
5. In one Mongo transaction: insert user, create wallet, ensure family mapping, save welcome notification records.
6. Commit first; only then run best-effort FCM, CleverTap and Interakt side effects.
7. Return token, user, and the legacy navigation object.

## 7. User OTP login flow

1. `send-otp` validates identity/device limit and invokes `OtpProvider` (MSG91 adapter).
2. `user/verify-otp` verifies the OTP. Unknown phone returns `isNewUser:true`; no account is silently inserted here.
3. General `/login` preserves the Node behavior: valid phone OTP may auto-create a minimal user, wallet and community mapping.
4. Account must have `isDeleted:false` and not have `status:false`.
5. Device registration is an atomic update enforcing the 15-device limit.
6. JWT is returned with both `role` and legacy `type` claims.

## 8. Consultant registration and approval

1. Validate and normalize payload.
2. Validate referral, phone, email and username uniqueness.
3. Insert consultant as `isAdminVerify:false`, `status:true`, `isDeleted:false`, `isActive:offline`.
4. Create consultant wallet, boost record and optional referral relation transactionally.
5. Send best-effort notification after commit.
6. Admin approval sets `isAdminVerify:true`. Rejection retains the existing Node hard-delete behavior for parity; this should later become soft rejection through a separately approved product change.

## 9. Consultant OTP/login flow

- A primary phone must already belong to a consultant before OTP verification.
- Login requires `isAdminVerify:true`, `status:true`, `isDeleted:false`.
- Successful primary-phone login marks `details.primaryMobileVerify:true`.
- Secondary-phone verification returns the legacy empty-data success.
- Email login is retained only behind the legacy compatibility switch because Node does not verify a consultant password.

## 10. Admin login and signup

- Password comparison and storage use BCrypt.
- Blocked admins cannot login.
- Management routes require admin; own password change allows admin/sub-admin.
- Public creation is closed. Optional first-admin bootstrap is disabled by default and can only create the first admin.
- Blocking or password changing increments `authTokenVersion`, invalidating Java tokens immediately.

## 11. Social login

- **COMPATIBLE:** email-only login can be temporarily enabled because existing clients use it; the nested Node response is preserved.
- **SECURITY FIX:** Google `identityToken` can be verified for signature-equivalent tokeninfo result, audience, verified email, subject, and email match.
- **CLIENT CHANGE:** disable `legacy-social-email-enabled` only after apps send provider and identity token.
- Existing accounts are now checked for blocked/deleted/unapproved state; Node contains unreachable checks in one branch.

## 12. JWT claims and security

- Algorithm: HS256 using raw UTF-8 secret, matching Node `jsonwebtoken` compatibility.
- Expiry: 30 days for account JWTs.
- Claims: identity fields, `_id`, `role`, legacy `type`, `tokenVersion`, `iat`, `exp`, and Java-added `jti`.
- Accept `Authorization: Bearer <token>` and raw/Bearer `vm-user-auth` according to existing clients.
- Never log a JWT, OTP, provider token or reset token.
- Existing Node tokens without `jti` or `tokenVersion` remain valid subject to account status; Java tokens support immediate revocation.

## 13. Device-limit algorithm

Use one Mongo `findOneAndUpdate`, not read-then-write:

1. Match account ID and either an already-known device token or current device count `<15`.
2. `$addToSet` device/FCM/UUID values and `$set` platform/VoIP metadata.
3. A null update result means `DEVICE_LIMIT`.

This prevents two simultaneous logins both seeing 14 devices and creating 16.

## 14. Logout and token invalidation

- Single-device logout pulls the provided FCM/device values and clears live flags; it revokes the current Java `jti`.
- All-device logout clears all device arrays, increments `authTokenVersion`, invalidates HTTP/socket cache variants, and revokes the current token.
- Old Node JWTs do not carry a revocable ID; account status and version checks provide compatibility, but complete immediate revocation of previously issued Node tokens requires a planned cutover.

## 15. Account deletion

- User: set `isDeleted:true`, zero embedded wallet, clear device data, increment token version, cancel waiting/missed waitlists.
- Consultant: set `isDeleted:true`, clear device/live state, increment token version, cancel waiting consultant requests.
- Redis auth entries and current token are invalidated.
- This is soft deletion; dependent financial/audit records are retained.

## 16. Password reset

Only admin/sub-admin accounts have passwords in the current Node product.

1. Forgot-password always locates normalized email.
2. Generate a random nonce; store only its SHA-256 hash with a short expiry.
3. Email a short-lived JWT carrying `purpose=password_reset`, admin ID, email and nonce.
4. Reset validates JWT purpose/expiry/account/email, atomically matches the nonce hash and expiry, writes BCrypt password, removes reset fields and increments token version.
5. Reusing the same token fails.

## 17. Redis cache design

- TTL: ten minutes, matching Node.
- User: `auth:user:email:<email>` or `auth:user:phone:<prefix>:<phone>`.
- Consultant: corresponding `auth:consultant:*` key.
- Admin: `auth:admin:<id>`.
- Socket entries use `:socket` suffix.
- Status, approval, deletion, device, logout, password or role-sensitive changes invalidate all matching HTTP/socket keys.
- Cache failures are fail-open to Mongo, never authorization-success without Mongo/account-state validation.

## 18. Socket authentication

1. Read `handshake.auth.token`; accept optional `Bearer ` prefix.
2. Verify the same HS256 JWT as HTTP.
3. Reject revoked Java `jti`.
4. Resolve current account through socket cache/Mongo.
5. Enforce user active/not-deleted or consultant active/approved/not-deleted.
6. Enforce token version when present.

Socket infrastructure remains separately disabled until the Socket.IO protocol/event contract is diff-tested with the React Native clients.

## 19. Error and HTTP mapping

| Case | Required mapping |
|---|---|
| Missing protected-route token | HTTP 200, `success:false` legacy envelope |
| Present malformed/expired JWT | HTTP 401 |
| User OTP invalid | HTTP 200, no body `code` |
| Consultant phone/OTP invalid | HTTP 400, body `code:400` |
| General login/send-OTP internal/business failure | HTTP 500, body `code:500` |
| Admin bad credentials | HTTP 200, admin `code:400`, `error:true` |
| Unauthorized admin signup | HTTP 401 (**SECURITY FIX**) |
| Migration write disabled | HTTP 200 failure plus migration-block header; no side effect |

## 20. External integrations

- MSG91 through `OtpProvider`.
- Google through `SocialIdentityVerifier`.
- SMTP through `PasswordResetMailProvider`.
- FCM, CleverTap and Interakt after successful registration commit.
- MongoDB and Redis through existing Java configuration.

Every external adapter is disabled/config-driven by default and must use TEST/sandbox credentials during verification.

## 21. Migration compatibility strategy

1. Keep Node routes live.
2. Deploy Java `/v2` with `vedicmeet.migration.writes-enabled=false`.
3. Contract-test read/error envelopes against TEST Node.
4. Run isolated provider sandbox tests.
5. Shadow safe lookups; never send duplicate OTPs/notifications.
6. Enable one route family at a time: verify/version, admin login, OTP send/verify, login, registration, logout/delete, consultant signup/approval.
7. Canary by controlled test users, monitor, then expand.
8. Rollback is routing back to Node; shared documents remain schema-compatible.

## 22. Security weaknesses found in Node

- Hard-coded OTP bypass identities.
- OTP/JWT/phone information written to logs.
- Social login trusts caller-supplied email.
- User/consultant email login has no password or identity proof.
- Admin management router is insufficiently protected at the parent level.
- Reset JWT is reusable and exposed by unsafe handling.
- Logout cannot revoke existing Node JWTs.
- Some account-state checks occur after early returns and never execute.
- Device limit uses non-atomic reasoning in old flows.

## 23. Improvements and compatibility classification

### Preserve now

- Paths under `/v2`, request aliases, Mongo field aliases, 30-day HS256 tokens, both auth headers, response envelopes, error-code quirks, user auto-creation, consultant approval semantics, 15-device limit.

### Fix without requiring a normal client-flow change

- No secret/token/OTP logs, protected admin management, BCrypt, atomic device update, cache invalidation, `jti` revocation, one-time reset nonce, account checks, post-commit external side effects, write kill switch.

### Requires coordinated client/product change

- Mandatory provider identity token for social login.
- Disable passwordless email login.
- Mandatory OTP receipt before registration.
- Replace consultant rejection hard-delete with an auditable state.
- Normalize legacy HTTP errors to standard 4xx responses.

## 24. Integration test and release plan

### Automated tests already present

- Alias/validation matrices.
- Node-compatible JWT claims and expiry.
- Redis HTTP/socket key parity and invalidation.
- OTP send/verify/proof behavior with mocked provider.
- Atomic device-limit decisions.
- User existing/new/register/auto-create/login/social/logout/delete decisions.
- Consultant duplicate/referral/signup/approval decisions.
- Admin BCrypt login, secured signup, reset nonce, password change and cache/token invalidation.
- Legacy controller HTTP/body mappings.
- Socket revoked-token rejection.
- Reflection test proving every migrated write remains kill-switched and protected management endpoints have role guards.

### Mandatory TEST-environment tests before enabling routes

1. Confirm exact collection names (`communitymappings`, `app_version_models`, notification record collection).
2. Record Node golden responses and compare Java for every endpoint and failure branch.
3. Test real MSG91 sandbox send/verify/resend without bypass numbers.
4. Test Google valid/expired/wrong-audience/email-mismatch tokens.
5. Test SMTP reset link and one-time consumption.
6. Test 16 simultaneous device registrations; exactly 15 unique devices may persist.
7. Test two simultaneous registrations for the same phone/email and add unique partial indexes if the current database can support them.
8. Verify Mongo transaction support and rollback for account plus wallet/dependent rows.
9. Connect both mobile apps to `/v2` and test both headers, legacy aliases and logout/delete.
10. Verify Socket.IO handshake compatibility and blocked/deleted account rejection.
11. Verify registration side effects occur once and only after commit.
12. Run security tests for admin role escalation, reset replay, forged social email and token leakage.

### Release gate

Passing Maven tests means the Java logic compiles and its isolated decisions are verified. It does **not** authorize production traffic. Production remains **NO-GO** until the TEST contract diff, provider sandbox tests, collection verification, mobile/socket tests and canary rollback exercise are signed off.
