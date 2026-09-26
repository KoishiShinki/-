# Preservation matrix (source code audit)

| Module | Preserved behavior | Storage |
|---|---|---|
| Account | Register, login, logout, profile, avatar; standalone roles and sessions | app_user, app_session, tl_user_profile |
| Worldline | Private/public CRUD, cover, country/year/style, statistics/divergence score | tl_worldline |
| Screenshot | Upload, recognition, draft inspection/confirmation/rejection/deletion | tl_screenshot |
| Events | CRUD, filters/timeline, generated narrative fragments, divergence, causal relations | tl_event, tl_event_relation |
| Stages | CRUD, automatic split, event binding, summary, chapter generation | tl_stage |
| Chapters | CRUD, publishing, Markdown and DOCX exports, cover selection | tl_chapter |
| Illustrations | Prompt/image generation, character references, adoption/discard/deletion | tl_illustration |
| World knowledge | Country-state and character CRUD, portraits | tl_nation_state, tl_character |
| Chat / AI | Contextual questions, history, task states/retry | tl_ai_task |
| Public site | Paginated discovery, worldline, timeline, published chapters, adopted illustrations, creator | existing business tables |
| Business admin | All existing /timeline moderation and management routes | same tables; ADMIN role |

Smallest complete standalone architecture: one Spring Boot API with Spring Security, MyBatis and MySQL; one Vue 3/Vite app for public, creator and business-admin screens. No Redis, code generator, scheduler, RuoYi menu/dept/role framework dependency is required for these custom features. Generic RuoYi framework administration is explicitly out of scope per user clarification.

Migration risks: preserve numeric business IDs and creator usernames; replace sys_user joins with app_user; do not publish original dump INSERTs, uploads, secrets, built distributions, Git history or documents. Original task retry only reset a status without dispatching; public action view/like/favorite were no-ops. Enforce record ownership and same-worldline relations, ensure private media is protected, validate uploads, disable AI truthfully when no provider is configured.

| Verb | Route |
|---|---|
| GET | `/app` + "/worldline/myList" |
| GET | `/app` + "/worldline/{worldlineId}" |
| POST | `/app` + "/worldline" |
| PUT | `/app` + "/worldline" |
| DELETE | `/app` + "/worldline/{worldlineId}" |
| POST | `/app` + "/worldline/uploadCover" |
| POST | `/app` + "/worldline/setVisibility" |
| GET | `/app` + "/worldline/stat/{worldlineId}" |
| GET | `/app` + "/screenshot/list/{worldlineId}" |
| POST | `/app` + "/screenshot/upload" |
| POST | `/app` + "/screenshot/recognize/{screenshotId}" |
| GET | `/app` + "/screenshot/draft/{screenshotId}" |
| POST | `/app` + "/screenshot/confirmDraft" |
| POST | `/app` + "/screenshot/rejectDraft" |
| DELETE | `/app` + "/screenshot/{screenshotId}" |
| GET | `/app` + {"/event/list/{worldlineId}", "/event/timeline/{worldlineId}"} |
| GET | `/app` + "/event/{eventId}" |
| POST | `/app` + "/event" |
| PUT | `/app` + "/event" |
| DELETE | `/app` + "/event/{eventId}" |
| POST | `/app` + "/event/generateFragment/{eventId}" |
| POST | `/app` + "/event/markDivergence/{eventId}" |
| POST | `/app` + "/event/relation" |
| GET | `/app` + "/event/relation/{worldlineId}" |
| GET | `/app` + "/stage/list/{worldlineId}" |
| GET | `/app` + "/stage/{stageId}" |
| POST | `/app` + "/stage" |
| PUT | `/app` + "/stage" |
| DELETE | `/app` + "/stage/{stageId}" |
| POST | `/app` + "/stage/autoSplit/{worldlineId}" |
| POST | `/app` + "/stage/bindEvents" |
| POST | `/app` + "/stage/generateSummary/{stageId}" |
| POST | `/app` + "/stage/generateNovel/{stageId}" |
| GET | `/app` + "/chapter/list/{worldlineId}" |
| GET | `/app` + "/chapter/{chapterId}" |
| POST | `/app` + "/chapter" |
| PUT | `/app` + "/chapter" |
| DELETE | `/app` + "/chapter/{chapterId}" |
| POST | `/app` + "/chapter/generate" |
| POST | `/app` + "/chapter/generateByStage/{stageId}" |
| POST | `/app` + "/chapter/setPublic" |
| POST | `/app` + "/chapter/exportMarkdown/{chapterId}" |
| POST | `/app` + "/chapter/exportWord/{chapterId}" |
| GET | `/app` + "/illustration/list/{worldlineId}" |
| GET | `/app` + "/illustration/{illustrationId}" |
| POST | `/app` + "/illustration/generatePrompt" |
| POST | `/app` + "/illustration/generateImage" |
| POST | `/app` + "/illustration/generateByChapter/{chapterId}" |
| POST | `/app` + "/illustration/adopt/{illustrationId}" |
| POST | `/app` + "/illustration/setChapterCover" |
| POST | `/app` + "/illustration/discard/{illustrationId}" |
| DELETE | `/app` + "/illustration/{illustrationId}" |
| GET | `/app` + "/nation/list/{worldlineId}" |
| POST | `/app` + "/nation" |
| PUT | `/app` + "/nation" |
| DELETE | `/app` + "/nation/{stateId}" |
| GET | `/app` + "/character/list/{worldlineId}" |
| POST | `/app` + "/character/uploadPortrait" |
| POST | `/app` + "/character" |
| PUT | `/app` + "/character" |
| DELETE | `/app` + "/character/{characterId}" |
| POST | `/app` + "/chat/ask" |
| GET | `/app` + "/chat/history/{worldlineId}" |
| DELETE | `/app` + "/chat/history/{recordId}" |
| GET | `/app` + "/aiTask/status/{taskId}" |
| GET | `/app` + "/aiTask/recent" |
| POST | `/app` + "/aiTask/retry/{taskId}" |
| GET | `/public` + "/worldline/list" |
| GET | `/public` + "/worldline/{worldlineId}" |
| GET | `/public` + "/worldline/timeline/{worldlineId}" |
| GET | `/public` + "/chapter/list/{worldlineId}" |
| GET | `/public` + "/chapter/{chapterId}" |
| GET | `/public` + "/illustration/list/{worldlineId}" |
| GET | `/public` + "/creator/{userId}" |
| POST | `/public` + "/action/view" |
| POST | `/public` + "/action/like" |
| POST | `/public` + "/action/favorite" |
| GET | `/timeline` + "/worldline/list" |
| GET | `/timeline` + "/worldline/{worldlineId}" |
| PUT | `/timeline` + "/worldline" |
| DELETE | `/timeline` + "/worldline/{worldlineIds}" |
| GET | `/timeline` + "/worldline/stat/{worldlineId}" |
| POST | `/timeline` + "/worldline/auditVisibility" |
| GET | `/timeline` + "/screenshot/list" |
| GET | `/timeline` + "/screenshot/{screenshotId}" |
| DELETE | `/timeline` + "/screenshot/{screenshotIds}" |
| GET | `/timeline` + "/event/list" |
| GET | `/timeline` + "/event/{eventId}" |
| POST | `/timeline` + "/event" |
| PUT | `/timeline` + "/event" |
| DELETE | `/timeline` + "/event/{eventIds}" |
| POST | `/timeline` + "/event/fragment/{eventId}" |
| POST | `/timeline` + "/event/divergence/{eventId}" |
| GET | `/timeline` + "/stage/list" |
| GET | `/timeline` + "/stage/{id}" |
| POST | `/timeline` + "/stage" |
| PUT | `/timeline` + "/stage" |
| DELETE | `/timeline` + "/stage/{ids}" |
| GET | `/timeline` + "/nation/list" |
| GET | `/timeline` + "/character/list" |
| GET | `/timeline` + "/analysis/{worldlineId}" |
| GET | `/timeline` + "/chapter/list" |
| GET | `/timeline` + "/chapter/{chapterId}" |
| POST | `/timeline` + "/chapter" |
| PUT | `/timeline` + "/chapter" |
| DELETE | `/timeline` + "/chapter/{chapterIds}" |
| POST | `/timeline` + "/chapter/public" |
| GET | `/timeline` + "/illustration/list" |
| GET | `/timeline` + "/illustration/{illustrationId}" |
| POST | `/timeline` + "/illustration" |
| PUT | `/timeline` + "/illustration" |
| DELETE | `/timeline` + "/illustration/{illustrationIds}" |
| POST | `/timeline` + "/illustration/adopt/{illustrationId}" |
| POST | `/timeline` + "/illustration/discard/{illustrationId}" |
| GET | `/timeline` + "/audit/worldline/list" |
| GET | `/timeline` + "/audit/chapter/list" |
| GET | `/timeline` + "/audit/illustration/list" |
| POST | `/timeline` + "/audit/pass" |
| POST | `/timeline` + "/audit/reject" |
| POST | `/timeline` + "/audit/offShelf" |
| GET | `/timeline` + "/aiTask/list" |
| GET | `/timeline` + "/aiTask/{taskId}" |
| GET | `/timeline` + "/aiTask/status/{taskId}" |
| POST | `/timeline` + "/aiTask/retry/{taskId}" |
| DELETE | `/timeline` + "/aiTask/{taskIds}" |
| GET | `/timeline` + "/aiConfig/list" |
| POST | `/timeline` + "/aiConfig/test" |
| POST | `/timeline` + {"/aiConfig", "/aiConfig/testOnly"} |
| PUT | `/timeline` + "/aiConfig" |
| DELETE | `/timeline` + "/aiConfig/{configIds}" |
