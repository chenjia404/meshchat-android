# AGENTS.md

## Project overview

This repository contains an Android native client for the meshproxy chat API.

The app is a client only. It does not implement the decentralized protocol itself.
It talks to an existing meshproxy service over:
- HTTP API
- WebSocket at `/api/v1/chat/ws`

Primary goals:
- direct chat
- group chat
- text messages
- file messages
- image/video/audio/file rendering in chat UI
- local cache with Room
- realtime sync with WebSocket
- stable, maintainable Android architecture

---

## Tech stack

Use these unless a task explicitly requires otherwise:
- Kotlin
- Jetpack Compose
- MVVM
- Repository pattern
- Room
- Retrofit
- OkHttp
- OkHttp WebSocket
- Hilt
- DataStore
- Coroutines + Flow
- Coil
- Media3

Do not introduce:
- XML-based primary UI
- unnecessary multi-module splitting
- alternative networking stacks
- alternative local database libraries

---

## Architecture rules

Follow this data flow:

Remote DTO -> Mapper -> Domain Model -> Local Entity/DAO -> Repository -> ViewModel -> UI

Rules:
- UI must not consume Retrofit DTOs directly.
- UI must not call Retrofit directly.
- WebSocket parsing must not live inside screens.
- All server data should land in Room before driving UI whenever practical.
- direct chat and group chat are separate backend models; do not collapse them into one oversized shared transport model.
- shared UI components are fine, but do not force direct/group into one giant ViewModel.

Preferred package layout:

- `core/`
- `data/remote/`
- `data/local/`
- `data/mapper/`
- `data/repository/`
- `domain/model/`
- `domain/usecase/`
- `feature/chatlist/`
- `feature/contacts/`
- `feature/groups/`
- `feature/chatroom/`
- `feature/media/`
- `feature/settings/`
- `navigation/`
- `service/`

---

## meshproxy protocol constraints

These are protocol facts. Do not rename fields in DTOs. Do not invent alternate routes.

### Profile
Endpoints:
- `GET /api/v1/chat/me`
- `GET /api/v1/chat/profile`
- `POST /api/v1/chat/profile`
- `POST /api/v1/chat/profile/avatar`
- `GET /api/v1/chat/avatars/{name}`

### Friend requests
Endpoints:
- `GET /api/v1/chat/requests`
- `POST /api/v1/chat/requests`
- `POST /api/v1/chat/requests/{request_id}/accept`
- `POST /api/v1/chat/requests/{request_id}/reject`

### Contacts
Endpoints:
- `GET /api/v1/chat/contacts`
- `DELETE /api/v1/chat/contacts/{peer_id}`
- `POST /api/v1/chat/contacts/{peer_id}/nickname`
- `POST /api/v1/chat/contacts/{peer_id}/block`

### Direct conversations
Endpoints:
- `GET /api/v1/chat/conversations`
- `DELETE /api/v1/chat/conversations/{conversation_id}`
- `POST /api/v1/chat/conversations/{conversation_id}/sync`
- `POST /api/v1/chat/conversations/{conversation_id}/read`
- `POST /api/v1/chat/conversations/{conversation_id}/retention`

DirectConversation DTO fields:
- `conversation_id`
- `peer_id`
- `state`
- `last_message_at`
- `last_transport_mode`
- `unread_count`
- `retention_minutes`
- `retention_sync_state`
- `retention_synced_at`
- `created_at`
- `updated_at`

### Direct messages
Endpoints:
- `GET /api/v1/chat/conversations/{conversation_id}/messages`
- `POST /api/v1/chat/conversations/{conversation_id}/messages`
- `POST /api/v1/chat/conversations/{conversation_id}/files`
- `GET /api/v1/chat/conversations/{conversation_id}/messages/{msg_id}/file`
- `POST /api/v1/chat/conversations/{conversation_id}/messages/{msg_id}/revoke`

DirectMessage DTO fields:
- `msg_id`
- `conversation_id`
- `sender_peer_id`
- `receiver_peer_id`
- `direction`
- `msg_type`
- `plaintext`
- `file_name`
- `mime_type`
- `file_size`
- `file_cid`
- `transport_mode`
- `state`
- `counter`
- `created_at`
- `delivered_at`

Important:
- direct messages use `limit` + `offset` pagination
- do not implement `beforeMessageId` pagination
- no-query response may be an array
- paged response may be an object with:
  - `messages`
  - `total`
  - `limit`
  - `offset`
  - `has_more`

Create a parser that can handle both response shapes.

### Groups
Endpoints:
- `GET /api/v1/groups`
- `POST /api/v1/groups`
- `GET /api/v1/groups/{group_id}`
- `POST /api/v1/groups/{group_id}/invite`
- `POST /api/v1/groups/{group_id}/join`
- `POST /api/v1/groups/{group_id}/leave`
- `POST /api/v1/groups/{group_id}/remove`
- `POST /api/v1/groups/{group_id}/title`
- `POST /api/v1/groups/{group_id}/retention`
- `POST /api/v1/groups/{group_id}/dissolve`
- `POST /api/v1/groups/{group_id}/controller`

Group DTO fields:
- `group_id`
- `title`
- `avatar`
- `controller_peer_id`
- `current_epoch`
- `retention_minutes`
- `state`
- `last_event_seq`
- `last_message_at`
- `member_count`
- `local_member_role`
- `local_member_state`
- `created_at`
- `updated_at`

### Group messages
Endpoints:
- `GET /api/v1/groups/{group_id}/messages`
- `POST /api/v1/groups/{group_id}/messages`
- `POST /api/v1/groups/{group_id}/files`
- `GET /api/v1/groups/{group_id}/messages/{msg_id}/file`
- `POST /api/v1/groups/{group_id}/messages/{msg_id}/revoke`
- `POST /api/v1/groups/{group_id}/sync`

GroupMessage DTO fields:
- `msg_id`
- `group_id`
- `epoch`
- `sender_peer_id`
- `sender_seq`
- `msg_type`
- `plaintext`
- `file_name`
- `mime_type`
- `file_size`
- `file_cid`
- `signature`
- `state`
- `delivery_summary`
- `created_at`

GroupDeliverySummary fields:
- `total`
- `pending`
- `sent_to_transport`
- `queued_for_retry`
- `delivered_remote`
- `failed`

### WebSocket
Endpoint:
- `/api/v1/chat/ws`

Supported event types only:
- `message`
- `message_state`
- `friend_request`
- `conversation_deleted`
- `contact_deleted`

Event field set may include:
- `type`
- `kind`
- `conversation_id`
- `msg_id`
- `msg_type`
- `request_id`
- `from_peer_id`
- `to_peer_id`
- `state`
- `at_unix_millis`
- `plaintext`
- `file_name`
- `mime_type`
- `file_size`
- `sender_peer_id`
- `receiver_peer_id`
- `direction`
- `counter`
- `transport_mode`
- `message_state`
- `created_at_unix_millis`
- `delivered_at_unix_millis`
- `epoch`
- `sender_seq`
- `delivery_summary`

Do not invent event names like:
- `message.new`
- `conversation.updated`
- `message.updated`

---

## Message rendering rules

Backend message type is not the same as UI media type.

Map to UI like this:
- `chat_text` -> text
- `group_chat_text` -> text
- `chat_file` -> inspect `mime_type`
- `group_chat_file` -> inspect `mime_type`

For file messages:
- `image/*` -> image bubble
- `video/*` -> video bubble
- `audio/*` -> audio bubble
- otherwise -> generic file bubble

Unknown or protocol/system message types should render as system messages.

---

## File transfer rules

Do not design upload as a separate generic upload-then-send pipeline.

Use the actual protocol endpoints directly:
- direct file send: `POST /api/v1/chat/conversations/{conversation_id}/files`
- group file send: `POST /api/v1/groups/{group_id}/files`

Multipart field name:
- `file`

Client behavior:
- insert optimistic local sending message
- send actual request
- replace/update local pending message with server response
- mark as failed on error
- support retry

---

## Local storage rules

Use Room tables for:
- profiles
- contacts
- friend_requests
- direct_conversations
- direct_messages
- groups
- group_members
- group_messages

Use app-private storage for downloaded/cached media.

Store local file path metadata for downloaded content.

---

## UI and product rules

The UI should feel similar to WeChat in structure, but do not copy the design literally.

Primary screens:
- splash/init
- chat list
- contacts
- groups
- direct chat room
- group chat room
- media preview
- settings

Must support:
- conversation list
- unread badges
- direct chat
- group chat
- text sending
- file sending
- image preview
- video playback
- audio playback
- file download
- connection status banner
- resend failed messages

For direct conversations:
- display title/avatar by joining `peer_id` with contact/profile data
- do not assume conversation payload contains all display fields

---

## WebSocket behavior rules

Implement a dedicated `WsManager` / realtime service.

Responsibilities:
- connect
- reconnect automatically
- expose connection state
- parse events
- write updates to local database
- trigger HTTP refresh when event payload is partial

Suggested connection states:
- `CONNECTING`
- `CONNECTED`
- `RECONNECTING`
- `DISCONNECTED`
- `FAILED`

---

## Coding style and implementation rules

- Prefer simple, readable code over abstraction-heavy code.
- Do not introduce unnecessary generic frameworks.
- Keep files focused.
- Avoid giant utility files.
- Add comments only where they help explain protocol quirks or non-obvious logic.
- Prefer explicit mappers over reflection-based conversion.
- Keep DTO names protocol-specific.
- Keep domain models clean and UI-oriented.
- Use stable keys in Compose lazy lists.
- Do not block the main thread for I/O, parsing, or database work.

---

## Build and quality expectations

Before calling a task done:
- project compiles
- no broken imports
- no placeholder stubs for critical flows
- new code follows existing package layout
- protocol field names in DTOs are correct
- direct/group behavior matches protocol rules
- core user flows are wired end-to-end

When making significant changes:
- explain which files changed
- explain any protocol assumptions
- note any intentionally deferred items

---

## How to work on tasks in this repo

Default workflow:
1. inspect existing structure
2. propose or infer the smallest viable plan
3. implement in small, compilable steps
4. keep architecture boundaries intact
5. prefer MVP correctness before polish
6. summarize what changed and what remains

For large tasks:
- first output a concise plan
- then implement phase by phase

---

## Do-not rules

Do not:
- rename meshproxy DTO fields
- replace limit/offset pagination with cursor/before-id pagination for direct messages
- invent a separate upload API
- put Retrofit DTOs directly into Compose UI
- merge direct and group transport models into one confusing data model
- put WebSocket business logic in screens
- over-engineer with many Gradle modules early
- silently ignore partial/ambiguous WebSocket payloads
- assume direct conversations already include title/avatar display data

---

## Done means

A change is done when:
- it compiles
- it respects the protocol
- it respects architecture boundaries
- it is locally coherent and reviewable
- it does not break direct/group distinctions
- it is easy to extend later