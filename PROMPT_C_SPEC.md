# Prompt C — Idempotent writes (status)

Strangler migration of the low-blast-radius write endpoints from Node `new-app-server`
(`production`) into `new-app-server-java`. Same rules as banner/discovery: faithful port,
preserve Node quirks, native driver needs **explicit `$set`/`$push`** (Mongoose auto-wraps,
the driver does not), and **explicit ObjectId casting** (the driver does not apply Mongoose
schema casting). All endpoints are **diff-pending** — wire onto `/v2` only after the contract
harness diff is clean on the TEST server. Because these mutate, the harness must also assert the
resulting document (see `contract-tests/fixtures/writes.json` `_assertDoc`), not just the response.

## ✅ Ported (7 of 7 endpoints)

| Endpoint | Java | Node source | Notes / preserved quirks |
|---|---|---|---|
| `PUT /v2/v1/notification/mark_read` | `notification/NotificationService.markRead` + `NotificationController` | `classes/notification.js` L543 | Returns the PRE-update doc. **No idempotency guard** in Node — a repeat call `$push`es the id again (duplicate). Not added. |
| `POST /v2/v1/notification/mark_all_read` | `NotificationService.markAllAsRead` | `classes/notification.js` L352 | `$addToSet` — idempotent by construction. Returns driver update result mirrored to Mongoose's `{acknowledged,matchedCount,modifiedCount,...}`. |
| `PUT /v2/v1/explore/action` | `content/ExploreService.action` + `ExploreController` | `classes/explore.js` L1630 | 7 `actionType`s. `notRecommand` → misspelled DB field **`notRecommed`** (preserved). `comments` native-insert keeps all input fields (Mongoose would strip); `exploreId` cast to ObjectId. |
| `PUT /v2/user/live_like` | `social/SocialWriteService.likeLiveEvent` + `SocialController` | `classes/user.js` L4512 | Broadcast like toggle; on like inserts a `comments` doc `"<name> liked event"`. |
| `POST /v2/user/follow` | `social/SocialWriteService.followCons` + `SocialController` | `classes/user.js` L4556 | `FOLLOW` (toggle) + `COUNT` ported. `user_cons_rels.isUserFollowed` mirrors follow state. Latent Node **TDZ bug** in the `broadcastId && followStatus` comment branch (`let user = await user.findOne` self-reference) → effectively no comment written; reproduced by not writing it. `FOLLOWLIST` **NOT ported** (throws `FOLLOWLIST_NOT_PORTED`) — large faceted read, out of the write scope. Trusts `body.userId` like Node. |
| `PUT /v2/user/update` | `user/UserProfileService.updateUser` + `UserController` | `classes/user.js` L4161 | Field-merge → `findOneAndUpdate({new:true})`. Fields written only when truthy AND not literal `"null"`; email-uniqueness `$nin:[userId]` (Mongoose wraps scalar); `details` = `{...old, ...new}`; streak increments. Multer route: `reqData` field + optional `profileImage` file (S3 via `MediaUploadService` seam). |
| `POST /v2/v1/support/initiate_query` | `support/SupportService.customerQueryInitiate` + `SupportController` | `classes/support.js` L507 | Creates query + **2 `notificationns` docs** (the typo collection, via `notificationRecord`), ticket `#QUERY_<n+1>`, then chat-server `POST /chats/create-thread-support` (`ChatServerClient`, trust-all TLS = Node `rejectUnauthorized:false`); FCM = `PushNotificationService` seam. Dedup: `QUERY_EXIST`. |
| `POST /v2/v1/support/chat_send` | `support/SupportService.supportChat` + `SupportController` | `classes/support.js` L730 | Appends a `customer_support_chats` doc; admin first-reply also creates the chat-server thread (`/support/create-query`). **Node bug preserved**: user path sets `input.userId = user._id` where `user` is the MODEL (undefined) → not stored. Multer route (`reqData` + optional `file`). |

Localized error strings are reproduced verbatim (`"This Email is already in use"`,
`"Query is already exist in pending state"`, `"Query does not exist"`, `"Query is already resolved"`)
because the Node response is `message: error.message`.

New `AppConstants` collections: `NOTIFICATION_RECORDS` (`notificationns`), `CUSTOMER_SUPPORT_QUERIES`,
`CUSTOMER_SUPPORT_CHATS`, `ADMINS`. New config: `vedicmeet.chat.server-url` (env `CHAT_SERVER_URL`).
The earlier five reused existing collections.

Encrypted `reqData` is decrypted with `CryptoService.decryptToMap` in each controller (mirroring
Node's `validateRequest`).

## Seams (isolated, single-method, documented)

So the DB/response logic stays faithful and compiles before the heavy infra is wired:
- `media/MediaUploadService` — S3 `awsUpload` (throws until wired; only reached when a real file part is attached).
- `support/ChatServerClient` — JDK `HttpClient` POST to the chat-server, **trust-all TLS matching Node's `rejectUnauthorized:false`** (drop when the chat-server gets a valid cert).
- `notification/PushNotificationService` — FCM/APNs `sendNotificationAndCons` (no-op; push isn't harness-observable).

> Three controllers now map base `/v2/user` (`UserController`, `discovery/UserDiscoveryController`,
> `social/SocialController`). Sub-paths are all distinct (`available_balance`, `update`,
> `consultant_details`, `consultant_list`, `top_consultant_list`, `live_like`, `follow`) so Spring
> starts fine — but if any two ever declare the same sub-path Spring fails on startup. Consolidate later.

## Next (per COPILOT_HANDOFF.md)
Prompt D (wallet/payments) and Prompt E (call lifecycle) — **money + real-time state**. Both MUST go
through the contract harness AND a human review; do not blind-accept. See `COPILOT_HANDOFF.md`.
