# Implementation Tasks — Student OS

Tasks are ordered for sequential delivery: each group builds on the one before it. Within a group, tasks marked **[parallel]** can be worked concurrently. Estimated sizes are S (< 4 h), M (4–12 h), L (1–3 days).

---

## Group 0 — Project Scaffolding

- [ ] **0.1** [M] Initialise Android project with Kotlin, Jetpack Compose, Gradle Kotlin DSL, and a `libs.versions.toml` version catalog. Configure `minSdk 26`, `targetSdk 35`.
- [ ] **0.2** [M] Create the Gradle multi-module structure: `:app`, `:core:database`, `:core:ui`, `:core:notifications`, `:core:sync`, `:core:events`, `:core:intelligence`, `:feature:attendance`, `:feature:assignments`, `:feature:coding`, `:feature:projects`, `:feature:intelligence`, `:feature:settings`. Enforce that no `:feature:*` module may depend on another `:feature:*` module via a lint rule.
- [ ] **0.3** [S] Add Hilt dependency injection across all modules. Add `@HiltAndroidApp` to `App`, `@AndroidEntryPoint` to `MainActivity`.
- [ ] **0.4** [S] Set up Navigation Component (Compose) in `:app`. Define the `ModuleRegistry` interface and a stub implementation that `:app` reads to build the nav graph and bottom navigation.
- [ ] **0.5** [S] Configure Room in `:core:database`: create the `AppDatabase` class, enable WAL mode, and add the `settings` table with `SettingsRepository`. Define all typed settings keys listed in the design document with their defaults.
- [ ] **0.6** [S] Configure CI: add a GitHub Actions workflow that runs `./gradlew build lint test` on every push. Gate merges on green.

---

## Group 1 — Core Database Schema

- [ ] **1.1** [M] [parallel] Create Room entities and DAOs for the Attendance module: `Subject`, `TimetableSlot`, `ClassEvent`. Include all columns from the design document. Write Room migration baseline (version 1).
- [ ] **1.2** [M] [parallel] Create Room entities and DAOs for the Assignment module: `Assignment`. Include `attachment_uri` column and `updated_at INTEGER NOT NULL DEFAULT 0` column. Update on every write.
- [ ] **1.3** [M] [parallel] Create Room entities and DAOs for the Coding module: `CpProfile`, `CpContest`, `CpReflection`, `DsaCategory`, `DsaTopic`. Schema requirements: (a) `cp_profiles.current_rating` is nullable (NULL until first sync); (b) `cp_contests` must include `UNIQUE(profile_id, contest_name, contest_date)` to drive safe upsert on re-sync; (c) `dsa_topics` must include `updated_at INTEGER NOT NULL DEFAULT 0`.
- [ ] **1.4** [M] [parallel] Create Room entities and DAOs for the Project module: `Project`, `Milestone`, `Bug`, `ProjectTask`, `ProjectResource`. Schema requirement: `project_tasks` must define a partial unique index `CREATE UNIQUE INDEX idx_one_next_action ON project_tasks(project_id) WHERE is_next_action = 1 AND is_parallel = 0` in the baseline migration SQL. Do NOT use a SQLite trigger for this constraint.
- [ ] **1.5** [S] [parallel] Create Room entities and DAOs for the Intelligence module: `DailyBrief` (with `snapshot_hash`, `llm_guidance`, `guidance_source`, `guidance_updated_at` columns), `RecommendationCache`, `AiCallLog`.
- [ ] **1.6** [S] Write a database unit test suite: verify all foreign-key constraints (`ON DELETE RESTRICT`), verify the `is_next_action` partial-unique-index invariant (confirm two tasks in the same project cannot both have `is_next_action = 1` when `is_parallel = 0`), and verify that `AttendanceCalculator` produces correct output for 10 known input combinations.

---

## Group 2 — Attendance Engine

*Depends on: Groups 0, 1*

- [ ] **2.1** [M] Implement `OcrProcessor`: integrate ML Kit Text Recognition, implement `TimetableFieldMapper` to convert raw text blocks into `(day, start_time, end_time, subject, location, confidence)` rows. Unit-test the mapper with five sample timetable layouts.
- [ ] **2.2** [M] Implement `ImportTimetableUseCase`: transactional insert of `timetable_slots` + `class_events` generation. Guard against overwriting existing events without explicit confirmation. Implement `OcrPreviewScreen` with editable rows and field-level confidence highlighting (fields below threshold shown in amber). **Generation horizon guard:** clamp the event generation end date to a maximum of 365 days from the import date, regardless of any caller-supplied value, to prevent runaway row generation.
- [ ] **2.3** [M] Implement `ClassEventRepository` and `AttendanceCalculator` (pure Kotlin). All five status values (`PRESENT`, `ABSENT`, `CANCELLED`, `HOLIDAY`, `EXTRA_CLASS`) handled correctly. Write unit tests verifying the formula for edge cases (all cancelled, 0 classes held, threshold boundaries). After each status write, emit `AppEvent.AttendanceMarked` to `AppEventBus`.
- [ ] **2.4** [S] Implement `BunkCalculator` with `canSkip` and `mustAttend` functions. Write a property-based test (Kotest `forAll`) proving consistency with the attendance formula across all valid input ranges.
- [ ] **2.5** [M] Build `WeeklyView` and `CalendarView` screens. Each updates reactively via a `StateFlow` from the ViewModel. Subjects below the `attendanceThreshold` are highlighted in red. Attendance percentages shown per subject.
- [ ] **2.6** [S] Build `AttendanceAnalyticsScreen`: per-subject breakdown (total held, present, absent, cancelled, percentage). Include `BunkCalculatorWidget` inline.
- [ ] **2.7** [S] Implement `RecalibrationUseCase`: called at App startup, recomputes all percentages from raw `ClassEvent` records and corrects drift. Must complete before the Attendance home screen is displayed.
- [ ] **2.8** [S] Implement timetable customisation: custom time slots, odd/even week parity (`week_parity` column), date range validity (`valid_from`, `valid_until`). UI: `EditTimetableScreen`.
- [ ] **2.9** [S] Implement subject management: add, rename, archive a subject. On archive, retain all `class_events` with `subjects.archived_at` set; historical data remains visible in read-only analytics.

---

## Group 3 — Assignment Engine

*Depends on: Group 1*

- [ ] **3.1** [M] Implement `AssignmentRepository` with CRUD operations. Implement `CreateAssignmentUseCase` and `UpdateAssignmentStatusUseCase` with the status transition rules from the design document. After each status write, emit `AppEvent.AssignmentStatusChanged` to `AppEventBus`.
- [ ] **3.2** [S] Implement the four filtered views (`Today`, `This_Week`, `Overdue`, `Completed`) as DAO queries using epoch-ms comparisons. `OVERDUE` is a query filter, not a persisted status value.
- [ ] **3.3** [M] Build `AssignmentListScreen` and `AssignmentDetailScreen` (Compose). Show priority badge, deadline countdown, status chip. Confirmation prompt on delete of `PENDING` or `IN_PROGRESS` assignments.
- [ ] **3.4** [M] Implement file attachment support: copy picked file into `filesDir/attachments/<uuid>.<ext>`, store relative path in `attachment_uri`. Use `ActivityResultContracts.GetContent` for the file picker. Display attachment name and a "remove" action in the detail screen.
- [ ] **3.5** [M] Implement `AssignmentReminderWorker` (WorkManager one-time): schedule at `(deadline - lead_ms)`. Cancel on status change to `SUBMITTED`/`COMPLETED` using worker tag `assignment_<id>`. Respect per-assignment `reminder_lead_ms`, falling back to the global setting.

---

## Group 4 — Coding Engine

*Depends on: Group 1*

- [ ] **4.1** [M] Implement CodeChef and Codeforces API clients in `:core:sync` using Retrofit + Kotlin serialization. Map responses to `CpProfile` and `CpContest` domain objects. Handle invalid handle errors and API errors gracefully, preserving last-known data.
- [ ] **4.2** [M] Implement `CpSyncWorker` (WorkManager periodic, `NETWORK_REQUIRED`): upsert profile + contests, update `last_synced_at`, schedule `ContestReminderWorker` for upcoming contests within the lookahead window. On completion, emit `AppEvent.CpSyncCompleted` to `AppEventBus`. Document best-effort Doze-mode behaviour in a code comment.
- [ ] **4.3** [M] Build `CpDashboardScreen`: current rating, contest history list (name, date, rank, rating change, problems solved), last-synced timestamp, offline indicator. All data from Room `Flow`.
- [ ] **4.4** [S] Implement `ContestReflectionScreen`: form with "what went wrong", "what to revise", self-rating 1–5. Shown automatically after a contest result appears for the first time. Stored in `cp_reflections`. On save, emit `AppEvent.ContestReflectionAdded`.
- [ ] **4.5** [M] Implement `KnowledgeTreeScreen`: two-level expandable list (`DsaCategory` → `DsaTopic`). Per-topic: confidence level display (1–5 star or coloured badge), revision status chip, notes field. Add/rename/delete categories and topics. On topic update, emit `AppEvent.DsaTopicUpdated`.
- [ ] **4.6** [S] Implement Knowledge Tree filters: filter by `revision_status` and/or `confidence_level`. Applied in `KnowledgeTreeViewModel` in-memory.
- [ ] **4.7** [S] Implement DSA topic suggestion logic in `DsaTopicSuggester`: returns the topic with the lowest `confidence_level` among those with `revision_status` in (`NOT_STARTED`, `IN_PROGRESS`). Used by both the Daily Intelligence Engine and the Free_Slot notification.

---

## Group 5 — Project Engine

*Depends on: Group 1*

- [ ] **5.1** [M] Implement `ProjectRepository` with full CRUD for projects, milestones, bugs, tasks, and resources. Implement the `completeNextAction` transaction (mark current complete → require new next action selection before commit). On task completion, emit `AppEvent.ProjectTaskCompleted`.
- [ ] **5.2** [S] Implement the `is_parallel` escape hatch: a project-level toggle in `ProjectDetailScreen` that allows multiple tasks to have `is_next_action = 1` simultaneously.
- [ ] **5.3** [M] Build `ProjectListScreen` and `ProjectDetailScreen`. Detail screen sections: Next_Immediate_Action (prominent), future tasks (de-emphasised list), milestones, bugs, resources, GitHub URL. On completing the next action, show a bottom sheet to select/create the replacement.
- [ ] **5.4** [S] Build `MilestoneScreen` and `BugScreen` as sub-screens of the project detail.
- [ ] **5.5** [S] Implement `ProjectInactivityWorker` (WorkManager periodic, daily): compare `last_activity_at` to `inactivity_threshold_days` setting, fire `INACTIVE_PROJECT_REMINDER` notification for qualifying projects. Update `last_activity_at` on every project write.
- [ ] **5.6** [S] Implement project archive: sets `archived_at`; archived projects are visible in a separate "Archived" tab in `ProjectListScreen` in read-only mode.

---

## Group 6 — Event Bus and Intelligence Core

*Depends on: Group 1. Can begin in parallel with Groups 2–5.*

- [ ] **6.1** [S] Implement `AppEventBus` in `:core:events`: a `SharedFlow<AppEvent>` exposed as a Hilt `@Singleton`. Define all `AppEvent` sealed class variants: `AttendanceMarked`, `AssignmentStatusChanged`, `ProjectTaskCompleted`, `CpSyncCompleted`, `ContestReflectionAdded`, `DsaTopicUpdated`, `DailyScoreChanged`. Write unit tests confirming events flow to subscribers without replay on new subscriptions.

- [ ] **6.2** [M] Implement `LLMProvider` interface and `LLMResult` / `FailureReason` types in `:core:intelligence`. Implement `MockProvider` (returns fixed templated strings). Implement `LLMProviderFactory` (reads `aiProvider` from `SettingsRepository`, returns the correct implementation via Hilt). Write unit tests for the factory switch logic.

- [ ] **6.3** [M] Implement `DeepSeekProvider` in `:core:intelligence` using Retrofit. Target the DeepSeek chat completions endpoint. Add an OkHttp interceptor for: 30-second timeout, exponential backoff retry on HTTP 429/503 (max 2 retries, 5-second initial delay), and request logging (debug builds only, no key leakage). Map all API errors to the appropriate `FailureReason`. Write unit tests using a mock HTTP server (MockWebServer).

- [ ] **6.4** [M] Implement `SnapshotBuilder` in `:core:intelligence`: builds an `IntelligenceSnapshot` from Room DAOs. Implement all fields from the schema in the design document including `student_context`, `can_skip`/`must_attend` in attendance_warnings, and `hours_remaining` in assignments_urgent. Must complete in < 200 ms. Write unit tests with mock DAOs for all edge-case snapshot states (no classes, all DSA mastered, no active project, offline CP data).

- [ ] **6.5** [S] Implement `SnapshotDiffer` in `:core:intelligence`: `diff(old, new): SnapshotDelta` returning only changed top-level keys and their new values. An empty delta means no LLM call is needed. Write unit tests covering identical snapshots, single-field change, and full replacement.

- [ ] **6.6** [M] Implement `PromptBuilder` in `:core:intelligence`: converts `IntelligenceSnapshot` → full prompt (≤ 400 tokens) and `SnapshotDelta` → delta prompt (≤ 150 tokens). Include the constant system prompt. Apply key abbreviation mapping. Enforce token budget by dropping low-priority sections. Write unit tests asserting token counts stay within budget for worst-case inputs.

- [ ] **6.7** [S] Implement `DeterministicFallback` in `:core:intelligence` (this is the original `DailyBriefComposer` refactored to accept `IntelligenceSnapshot` and return a `GuidanceText` domain object). Its output is structurally identical whether or not the LLM is used. Zero network dependencies. Write unit tests for all template branches including "no project action" and "DSA mastery" fallbacks.

- [ ] **6.8** [S] Implement `RecommendationCache` in `:core:intelligence`: wraps the `recommendation_cache` DAO. Implements `get(hash): CachedRecommendation?` (with max-age check) and `put(hash, response, tokenCount)`. Enforces a 7-entry retention limit. Write unit tests for cache hit, cache miss, and age expiry.

- [ ] **6.9** [S] Implement `RateLimiter` in `:core:intelligence`: reads today's `ai_call_log` count from Room; blocks calls when `aiMaxCallsPerDay` is reached; always allows the deterministic fallback path. Write unit tests for limit boundary conditions.

---

## Group 6a — Intelligence Orchestrator and Daily Brief UI

*Depends on: Groups 2, 3, 4, 5, 6*

- [ ] **6a.1** [M] Implement `IntelligenceOrchestrator` in `:feature:intelligence`: coordinates the morning brief flow (`DailyBriefWorker` → `SnapshotBuilder` → `RecommendationCache` → `PromptBuilder` → `LLMProvider` with fallback to `DeterministicFallback` → persist to `daily_briefs` → write `ai_call_log`). The orchestrator is a Hilt `@Singleton`, not a ViewModel. Write integration tests with `MockProvider`.

- [ ] **6a.2** [M] Implement intra-day event subscription in `IntelligenceOrchestrator`: collect from `AppEventBus`, debounce 30 seconds, rebuild snapshot, compute delta, skip if empty, apply cache check, call LLM or fallback, update `daily_briefs.llm_guidance` and `guidance_updated_at`. Write integration tests simulating rapid event bursts (verify exactly one LLM call per 30-second window).

- [ ] **6a.3** [S] Implement `DailyBriefWorker` (WorkManager periodic, scheduled at `dailyBriefTimeHHmm`): calls `IntelligenceOrchestrator.generateMorningBrief()`, then fires `DAILY_BRIEF` notification.

- [ ] **6a.4** [M] Build `DailyBriefScreen`: structured sections (classes, attendance warnings, urgent assignments, free slots, DSA suggestion, project action, score progress bar). Display `llm_guidance` text in a dedicated "Today's Guidance" card. If `guidance_source == DETERMINISTIC`, show a subtle "AI offline" badge. If guidance is stale, show "last updated HH:mm". Each section deep-links into the relevant module screen. Score progress bar updates in real time.

- [ ] **6a.5** [S] Build `BriefHistoryScreen`: list of past `daily_briefs` ordered by date. Show score target vs. actual and `guidance_source` badge per day. Tappable to view the full brief.

- [ ] **6a.6** [S] Implement real-time score progress: `DailyScoreViewModel` subscribes to changes in `class_events`, `assignments`, `project_tasks`, and `dsa_topics`, recomputes `score_actual` live, persists to `daily_briefs.score_actual`, and emits `AppEvent.DailyScoreChanged`.

---

## Group 7 — Notification System

*Depends on: Groups 2, 3, 4, 5, 6, 6a (workers exist; this group wires them up end-to-end)*

- [ ] **7.1** [S] Define all six `NotificationChannel`s in `NotificationChannelRegistry` (called at App startup): `DAILY_BRIEF` (DEFAULT), `ASSIGNMENT_REMINDER` (HIGH), `CLASS_REMINDER` (DEFAULT), `CONTEST_REMINDER` (HIGH), `FREE_SLOT_RECOMMENDATION` (LOW), `INACTIVE_PROJECT_REMINDER` (DEFAULT). None use `PRIORITY_MAX`. All respect Do Not Disturb.
- [ ] **7.2** [S] Implement `ClassReminderWorker`: scheduled per `class_event`, fires `CLASS_REMINDER` notification N minutes before class (Student-configurable, default 15 min). Re-scheduled whenever the timetable changes.
- [ ] **7.3** [S] Implement `FreeSlotWorker`: triggered as part of `DailyBriefWorker`, delivers a `FREE_SLOT_RECOMMENDATION` notification for each identified free slot containing the DSA topic or project action suggestion.
- [ ] **7.4** [M] Implement `NotificationRescheduler`: called in `App.onCreate()`. Queries all pending assignments, class events, and contests; re-enqueues any WorkManager jobs that are missing. Must complete within 30 seconds of App launch (measure with a startup trace in debug builds).
- [ ] **7.5** [S] Build notification settings UI in `:feature:settings`: per-category enable/disable toggles, global assignment reminder lead time, contest reminder lookahead, class reminder lead time.

---

## Group 8 — Backup and Restore

*Depends on: Group 1 (all tables exist)*

- [ ] **8.1** [M] Implement `ExportUseCase`: serialise all Room tables to the structured JSON format. Embed file attachments ≤ 10 MB as Base64; emit a `{ "external_path": "...", "warning": "file not embedded" }` object for larger files. Use `ACTION_CREATE_DOCUMENT` to let the user choose the save location.
- [ ] **8.2** [M] Implement `ImportUseCase`: parse export JSON, show confirmation dialog, run a single Room transaction (truncate all tables → re-insert all records → restore attachments). Validate JSON schema before starting the transaction; surface a descriptive error on validation failure.
- [ ] **8.3** [S] Add Export / Import entry points in `:feature:settings` (`BackupScreen`).

---

## Group 9 — Settings and Polish

*Depends on: All previous groups*

- [ ] **9.1** [M] Build `SettingsScreen` with all Student-configurable parameters: Attendance_Threshold, CP sync interval, daily brief time, score weights, OCR confidence threshold, notification settings link, backup link, and AI settings section (see 9.1a).
- [ ] **9.1a** [M] Build the AI settings section within `SettingsScreen`: enable/disable AI toggle, AI provider selector (DeepSeek only in V1), API key entry field (writes to `EncryptedSharedPreferences`, displays masked), intra-day updates toggle, tone preference (motivational/concise/neutral), max calls per day, cache max age. Add an AI Diagnostics sub-screen showing the `ai_call_log` table: date, trigger, cache hit, delta, tokens, latency, success/error. Include a "daily estimated cost" calculation based on token counts. All values persisted through `SettingsRepository` except the API key.
- [ ] **9.2** [S] Implement the `ModuleRegistry` fully: each feature module registers its `NavGraphBuilder` extension, `NotificationCategory` list, and Room migration list. Verify a new stub module can be added by editing only its own Gradle module and the `:app` dependency list.
- [ ] **9.3** [M] Accessibility pass: verify all Compose screens have `contentDescription` on icon-only buttons, correct heading semantics, minimum 48 dp touch targets, and sufficient colour contrast (WCAG AA). Note: full WCAG validation requires manual testing with TalkBack.
- [ ] **9.4** [S] Implement first-run onboarding flow: prompt for handles (optional), set Attendance_Threshold, schedule daily brief time, request notification permission (`POST_NOTIFICATIONS` on API 33+), and optionally configure AI (provider + API key with a "skip for now" option that defaults to deterministic mode).
- [ ] **9.5** [S] Add empty-state screens for all list views (no classes, no assignments, no projects, no DSA topics, no CP profile configured).
- [ ] **9.6** [S] Performance: verify `AttendanceCalculator` recalculation completes within 500 ms on a device with 500 class events (write an instrumented benchmark).

---

## Group 10 — Testing and Release Prep

*Depends on: All previous groups*

- [ ] **10.1** [M] Write integration tests for the six critical transaction paths: timetable import, `completeNextAction`, backup export + import round-trip, notification reschedule on reboot, daily brief generation with a fully populated DB (both LLM and deterministic paths), and intra-day event → snapshot delta → recommendation update.
- [ ] **10.2** [S] Write end-to-end UI tests (Espresso or Compose testing APIs) for the three most-used flows: mark attendance, create and submit an assignment, complete a project next action.
- [ ] **10.3** [S] Verify `BunkCalculator` property-based tests pass with 10,000 random valid inputs (Kotest).
- [ ] **10.4** [M] AI-specific tests: (a) verify `MockProvider` is used when `aiEnabled = false`; (b) verify `DeterministicFallback` output is used when `MockProvider` returns `Failure`; (c) verify `RecommendationCache` prevents a second LLM call when snapshot is unchanged; (d) verify `RateLimiter` blocks LLM calls after reaching `aiMaxCallsPerDay` and falls back gracefully; (e) verify API key is absent from JSON backup export; (f) verify `SnapshotDiffer` produces an empty delta when nothing changed.
- [ ] **10.5** [S] Lint and static analysis: resolve all `lint` warnings, run `detekt` with default rules, fix all errors.
- [ ] **10.6** [S] Prepare release build: configure ProGuard rules for Retrofit + Room + ML Kit + Jetpack Security, generate a signed APK, verify Room database migrations run cleanly from version 1 to current.
- [ ] **10.7** [S] Write a brief `CONTRIBUTING.md` documenting: (a) how to add a new module using the `ModuleRegistry` extension point; (b) how to add a new `LLMProvider` implementation; (c) how to add a new `AppEvent` type.

---

## Dependency Summary

```
Group 0 (Scaffolding)
  └── Group 1 (Schema)
        ├── Group 2 (Attendance)    ──┐
        ├── Group 3 (Assignments)   ──┤
        ├── Group 4 (Coding)        ──┤──> Group 6a (Orchestrator + UI)
        ├── Group 5 (Projects)      ──┘         │
        └── Group 6 (Event Bus + Intelligence Core) ─> Group 6a
                                                        │
                                              Group 7 (Notifications)
                                                        │
                                   ┌────────────────────┘
                              Group 8 (Backup)    [needs 1 only]
                              Group 9 (Settings & Polish)
                              Group 10 (Testing & Release)
```

- Groups 2–5 and Group 6 can all be developed in parallel after Group 1 completes.
- Group 6a requires Groups 2–6 to be complete.
- Group 8 can begin as soon as Group 1 is done, independently of all other groups.
- Group 9 and Group 10 require everything before them.
