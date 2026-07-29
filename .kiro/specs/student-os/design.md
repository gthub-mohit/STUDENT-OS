# Design Document — Student OS

## Overview

Student OS is an offline-first Android application built with Kotlin, Jetpack Compose, Room (SQLite), and WorkManager. It follows Clean Architecture — three independent layers (data, domain, presentation) — with each of the six product modules implemented as a Gradle feature module. Modules communicate only through the shared Room database and a thin interface layer; no module holds a direct reference to another module's classes.

The Daily Intelligence Engine has been upgraded from a purely deterministic engine to an event-driven, LLM-assisted intelligence layer. All business logic (attendance, scores, deadlines, ratings) remains deterministic and computed locally. The LLM's sole responsibility is to interpret that data and generate human-friendly guidance. The deterministic engine remains as a full offline fallback; the Student never loses functionality when offline.

---

## Architecture

### Layer Model

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                   │
│  (Jetpack Compose UI, ViewModels, Navigation Component) │
├─────────────────────────────────────────────────────────┤
│                      Domain Layer                       │
│  (Use Cases, business rules, module interface contracts)│
├─────────────────────────────────────────────────────────┤
│                       Data Layer                        │
│  (Room DAOs, Repositories, Sync_Service, OCR_Processor) │
└─────────────────────────────────────────────────────────┘
```

Each Gradle module exposes only its Use Case and Repository interfaces to the `:app` shell. The `:app` module owns navigation and the `ModuleRegistry`.

### Module Graph

```
:app (shell)
 ├── :core:database          (Room schema, shared entities, migrations)
 ├── :core:ui                (design system, shared Compose components)
 ├── :core:notifications     (Notification_Manager implementation)
 ├── :core:sync              (Sync_Service / WorkManager workers)
 ├── :core:events            (App_Event definitions, AppEventBus)
 ├── :core:intelligence      (LLMProvider interface, SnapshotBuilder, PromptBuilder,
 │                            DeterministicFallback, RecommendationCache)
 ├── :feature:attendance
 ├── :feature:assignments
 ├── :feature:coding
 ├── :feature:projects
 ├── :feature:intelligence   (Daily_Intelligence_Engine UI + orchestration)
 └── :feature:settings
```

No `:feature:*` module may import another `:feature:*` module. Cross-feature data flows entirely through `:core:database`. App_Events flow through `:core:events`, which every module may depend on without creating a feature-to-feature cycle.

### ModuleRegistry

The `:app` module owns a `ModuleRegistry` singleton populated at startup. Each feature module registers:

- A `NavGraphBuilder` extension (navigation entry point)
- Its `NotificationCategory` constants
- Its Room `@Database` migration list

This is the sole extension point for adding future modules without editing `:app` source files beyond the Gradle dependency declaration.

---

## Database Design

### Guiding Rules

1. No hardcoded subject names, semester names, or class counts anywhere.
2. A single `subjects` table is the canonical subject record; all other tables reference it via foreign key.
3. All multi-step writes use Room transactions.
4. Referential integrity is enforced with `ON DELETE RESTRICT` by default; historical records are archived, never hard-deleted from attendance or assignment tables.

### Core Tables

#### `subjects`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `name` | TEXT NOT NULL | Student-defined, mutable |
| `archived_at` | INTEGER | NULL = active; epoch ms = archived |

#### `timetable_slots`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `subject_id` | INTEGER FK → subjects | |
| `day_of_week` | INTEGER | 1 (Mon) – 7 (Sun) |
| `start_time` | TEXT | "HH:mm" |
| `end_time` | TEXT | "HH:mm" |
| `location` | TEXT | nullable |
| `week_parity` | TEXT | NULL = every week, "ODD", "EVEN" |
| `valid_from` | INTEGER | epoch ms |
| `valid_until` | INTEGER | epoch ms, nullable |

#### `class_events`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `timetable_slot_id` | INTEGER FK → timetable_slots | nullable (for Extra_Class) |
| `subject_id` | INTEGER FK → subjects | denormalised for query speed |
| `scheduled_at` | INTEGER | epoch ms |
| `end_at` | INTEGER | epoch ms |
| `status` | TEXT | `UNMARKED`, `PRESENT`, `ABSENT`, `CANCELLED`, `HOLIDAY`, `EXTRA_CLASS` |
| `is_extra` | INTEGER | 0/1 |
| `linked_slot_id` | INTEGER FK → timetable_slots | for shifted classes |
| `updated_at` | INTEGER | epoch ms |

#### `assignments`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `subject_id` | INTEGER FK → subjects | |
| `title` | TEXT NOT NULL | |
| `deadline` | INTEGER | epoch ms |
| `priority` | TEXT | `HIGH`, `MEDIUM`, `LOW` |
| `notes` | TEXT | nullable |
| `attachment_uri` | TEXT | nullable (see §Attachment Strategy) |
| `status` | TEXT | `PENDING`, `IN_PROGRESS`, `SUBMITTED`, `COMPLETED` |
| `reminder_lead_ms` | INTEGER | ms before deadline; NULL = use global default |
| `created_at` | INTEGER | epoch ms |
| `updated_at` | INTEGER | epoch ms; updated on every write |

#### `cp_profiles`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `platform` | TEXT | `CODECHEF`, `CODEFORCES` |
| `handle` | TEXT NOT NULL | |
| `current_rating` | INTEGER | nullable; NULL until first sync |
| `last_synced_at` | INTEGER | epoch ms |

#### `cp_contests`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `profile_id` | INTEGER FK → cp_profiles | |
| `contest_name` | TEXT | |
| `contest_date` | INTEGER | epoch ms |
| `rank` | INTEGER | |
| `rating_change` | INTEGER | |
| `problems_solved` | INTEGER | |
| | | **UNIQUE(profile_id, contest_name, contest_date)** — prevents duplicate rows on re-sync |

#### `cp_reflections`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `contest_id` | INTEGER FK → cp_contests | UNIQUE |
| `went_wrong` | TEXT | |
| `to_revise` | TEXT | |
| `self_rating` | INTEGER | 1–5 |

#### `dsa_categories`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `name` | TEXT NOT NULL | |
| `sort_order` | INTEGER | |

#### `dsa_topics`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `category_id` | INTEGER FK → dsa_categories | |
| `name` | TEXT NOT NULL | |
| `confidence_level` | INTEGER | 1–5, default 1 |
| `revision_status` | TEXT | `NOT_STARTED`, `IN_PROGRESS`, `REVISED` |
| `notes` | TEXT | nullable |
| `updated_at` | INTEGER | epoch ms; updated on every write |

#### `projects`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `title` | TEXT NOT NULL | |
| `overview` | TEXT | |
| `github_url` | TEXT | nullable |
| `notes` | TEXT | nullable |
| `archived_at` | INTEGER | nullable |
| `inactivity_threshold_days` | INTEGER | default 7 |
| `last_activity_at` | INTEGER | epoch ms |

#### `milestones`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `project_id` | INTEGER FK → projects | |
| `title` | TEXT NOT NULL | |
| `description` | TEXT | nullable |
| `target_date` | INTEGER | epoch ms |
| `status` | TEXT | `PENDING`, `IN_PROGRESS`, `DONE` |

#### `bugs`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `project_id` | INTEGER FK → projects | |
| `description` | TEXT NOT NULL | |
| `severity` | TEXT | `LOW`, `MEDIUM`, `HIGH` |
| `status` | TEXT | `OPEN`, `RESOLVED` |

#### `project_tasks`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `project_id` | INTEGER FK → projects | |
| `title` | TEXT NOT NULL | |
| `is_next_action` | INTEGER | 0/1; enforced by partial UNIQUE index: UNIQUE(project_id) WHERE is_next_action = 1 AND is_parallel = 0 |
| `is_parallel` | INTEGER | 0/1; escape hatch for multi-stream projects |
| `completed_at` | INTEGER | nullable |
| `sort_order` | INTEGER | |

#### `project_resources`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `project_id` | INTEGER FK → projects | |
| `label` | TEXT | nullable |
| `url` | TEXT NOT NULL | |

#### `daily_briefs`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `date` | TEXT | "YYYY-MM-DD", UNIQUE |
| `json_snapshot` | TEXT | full Intelligence_Snapshot JSON used for generation |
| `snapshot_hash` | TEXT | SHA-256 of `json_snapshot`; used to detect cache hits |
| `brief_json` | TEXT | structured output JSON (deterministic fields) |
| `llm_guidance` | TEXT | nullable; human-friendly text from LLM; NULL if offline |
| `guidance_source` | TEXT | `LLM` or `DETERMINISTIC` |
| `score_target` | INTEGER | |
| `score_actual` | INTEGER | default 0 |
| `generated_at` | INTEGER | epoch ms |
| `guidance_updated_at` | INTEGER | epoch ms; updated on each intra-day refresh |

#### `recommendation_cache`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `snapshot_hash` | TEXT UNIQUE | SHA-256 of the snapshot that produced this response |
| `llm_response` | TEXT | raw text from LLM |
| `provider` | TEXT | e.g., `DEEPSEEK` |
| `created_at` | INTEGER | epoch ms |
| `token_count` | INTEGER | total tokens used; for cost monitoring |

#### `ai_call_log`
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | |
| `triggered_by` | TEXT | App_Event type or `DAILY_BRIEF_WORKER` |
| `snapshot_hash` | TEXT | |
| `was_cache_hit` | INTEGER | 0/1 |
| `was_delta` | INTEGER | 0/1 |
| `latency_ms` | INTEGER | |
| `token_count` | INTEGER | |
| `success` | INTEGER | 0/1 |
| `error_message` | TEXT | nullable |
| `created_at` | INTEGER | epoch ms |

This log is used for cost monitoring and AI diagnostics in the Settings screen.

#### `settings`
| Column | Type | Notes |
|---|---|---|
| `key` | TEXT PK | |
| `value` | TEXT NOT NULL | |

All configurable values (Attendance_Threshold, sync interval, brief time, score weights, notification lead times, inactivity threshold) live in `settings` as typed key-value pairs. A typed `SettingsRepository` wraps this table.

---

## Module Designs

### Attendance Engine

**OCR Flow**

1. User picks image (camera or gallery) → `OcrViewModel` calls `OcrProcessor.extract(bitmap): OcrResult`.
2. `OcrProcessor` uses ML Kit Text Recognition to get `Text` blocks, then a post-processing pass (`TimetableFieldMapper`) maps raw text blocks to `(day, start_time, end_time, subject, location)` rows, assigning a composite `confidence: Float` per field by averaging block-level confidence scores within each cell boundary.
3. Fields with `confidence < threshold` (default 0.80) are flagged; UI renders them with a warning highlight.
4. User reviews/edits the `OcrPreviewScreen`. On confirm, `ImportTimetableUseCase` executes a single Room transaction: insert/update `timetable_slots` → generate `class_events` for the next N days → write to DB.
5. Existing `class_events` are never overwritten unless the user explicitly approves a replacement dialog.

**Generation horizon guard:** `ImportTimetableUseCase` enforces a hard maximum of 365 days from the import date when generating `class_events`. Any caller-supplied end date beyond this limit is silently clamped to `importDate + 365 days`. This prevents an invalid input from generating millions of rows in a single transaction.

**Attendance Calculation**

Computed on-demand by `AttendanceCalculator` (domain layer, pure Kotlin, no Android dependencies):

```
percentage = (present + extra_present) / (total - cancelled - holiday) × 100
```

Recalculated within 500 ms of any status change via a `Flow` collector in `AttendanceViewModel`. On App startup, `RecalibrationUseCase` recomputes all percentages from raw `class_events` and corrects any cached drift.

**Bunk Calculator**

Pure functions in `BunkCalculator`:
- `canSkip(present, total, cancelled, holiday, threshold) → Int`
- `mustAttend(present, total, cancelled, holiday, threshold) → Int`

Both are unit-tested against a property-based suite to verify formula consistency with the attendance formula.

---

### Assignment Engine

**Attachment Strategy**

Files are copied into App-private internal storage (`context.filesDir/attachments/<uuid>.<ext>`). The `attachment_uri` column stores the relative path. This ensures the file survives the original location being deleted and is included in the JSON backup (as a Base64 blob capped at 10 MB per file; files exceeding this are stored by reference only with a warning in the export). This decision resolves Architecture Gap #6 from the requirements document.

**Status Transitions**

```
PENDING → IN_PROGRESS → SUBMITTED
    └──────────────────→ COMPLETED
```

Auto-transition to `OVERDUE` is a view-level filter (`deadline < now AND status NOT IN (SUBMITTED, COMPLETED)`), not a DB status value. `OVERDUE` is never written to the `status` column — it is computed at query time only. This avoids a background job and keeps the state machine simple.

**Notifications**

`AssignmentReminderWorker` (WorkManager, one-time) is scheduled at `(deadline - lead_ms)`. On status change to `SUBMITTED` or `COMPLETED`, the worker is cancelled by tag `assignment_<id>`.

---

### Coding Engine

**Sync Architecture**

`CpSyncWorker` is a `CoroutineWorker` registered with WorkManager using `PeriodicWorkRequest` (minimum 15-minute interval, `NETWORK_REQUIRED` constraint). On each execution it:

1. Fetches rating + contest list from CodeChef API and Codeforces API.
2. Upserts into `cp_profiles`, `cp_contests` using `INSERT OR REPLACE`.
3. Updates `last_synced_at`.
4. Checks for upcoming contests within the contest-reminder lookahead window; if found, schedules a `ContestReminderWorker`.

Regarding Architecture Gap #2: contest reminders are explicitly documented as **best-effort**. WorkManager with `NETWORK_REQUIRED` is the implementation; Doze-mode delays of up to 15 minutes are accepted. This is noted in the Settings screen.

**Knowledge Tree**

`KnowledgeTreeScreen` renders a two-level expandable list (LazyColumn with nested items). Filter state (by `revision_status`, `confidence_level`) is held in `KnowledgeTreeViewModel` and applied in-memory since the tree is expected to be small (< 500 topics).

---

### Project Engine

**Next_Immediate_Action Enforcement**

A Room `@Transaction` in `ProjectRepository.completeNextAction()`:
1. Sets current `is_next_action = 1` row to `completed_at = now`.
2. Requires caller to provide the `id` of the new next action before committing; if none provided, the transaction commits with no next action (all `is_next_action = 0`), and the UI shows a "No next action — tap to set one" prompt.
3. The `is_parallel = 1` escape hatch allows multiple tasks to be designated "active" for a project that explicitly opts into parallel mode, resolving Architecture Gap #3.

**Inactivity Reminder**

`ProjectInactivityWorker` (periodic, daily) checks `last_activity_at` for each active project. If `now - last_activity_at > inactivity_threshold_days * 86400000`, it fires an `INACTIVE_PROJECT_REMINDER` notification.

---

### Daily Intelligence Engine

The Daily Intelligence Engine is the only module in Student OS that optionally uses network access beyond CP sync. It is composed of five sub-components: `AppEventBus`, `SnapshotBuilder`, `LLMProvider` (with `DeterministicFallback`), `PromptBuilder`, and `RecommendationCache`. All business logic lives in the deterministic layer. The LLM only receives a snapshot and produces human-friendly text.

---

#### App Event Bus

`AppEventBus` is a `SharedFlow<AppEvent>` singleton in `:core:events`, exposed via a Hilt `@Singleton`. Every feature module emits events to this bus after completing a meaningful state change. No feature module consumes events from another feature module directly — only `:feature:intelligence` subscribes.

```kotlin
// :core:events
sealed class AppEvent {
    data class AttendanceMarked(val subjectId: Long, val status: String) : AppEvent()
    data class AssignmentStatusChanged(val assignmentId: Long, val newStatus: String) : AppEvent()
    data class ProjectTaskCompleted(val taskId: Long, val projectId: Long) : AppEvent()
    object CpSyncCompleted : AppEvent()
    data class ContestReflectionAdded(val contestId: Long) : AppEvent()
    data class DsaTopicUpdated(val topicId: Long) : AppEvent()
    data class DailyScoreChanged(val newScore: Int) : AppEvent()
}
```

Each feature module injects `AppEventBus` and calls `bus.emit(event)` after its repository write completes. The bus is never used to carry data payloads that duplicate the database — it carries only identifiers and status values sufficient to identify what changed.

---

#### Snapshot Builder

`SnapshotBuilder` lives in `:core:intelligence`. It reads the Local_Database and produces an `IntelligenceSnapshot` (a data class serialised to JSON). This is the only component that queries the DB for intelligence purposes.

**Intelligence_Snapshot schema** (formal contract — unchanged from original; extended with `snapshot_version` and `student_context`):

```json
{
  "snapshot_version": 1,
  "date": "YYYY-MM-DD",
  "student_context": {
    "name": "string|null",
    "tone_preference": "motivational|concise|neutral"
  },
  "classes_today": [
    { "subject": "string", "time": "HH:mm–HH:mm", "location": "string|null" }
  ],
  "attendance_warnings": [
    { "subject": "string", "percentage": 0.0, "threshold": 75.0, "can_skip": 0, "must_attend": 0 }
  ],
  "assignments_urgent": [
    { "id": 0, "subject": "string", "title": "string", "deadline": "ISO-8601", "status": "string", "hours_remaining": 0 }
  ],
  "free_slots": [
    { "start": "HH:mm", "end": "HH:mm", "duration_minutes": 0 }
  ],
  "suggested_dsa_topic": { "category": "string", "topic": "string", "confidence": 0, "revision_status": "string" },
  "suggested_project_action": { "project": "string", "action": "string" },
  "score": { "target": 0, "actual": 0, "weights": { "class": 10, "assignment": 20, "project_action": 15, "dsa": 10 } },
  "cp_summary": { "codechef_rating": 0, "codeforces_rating": 0, "last_synced": "ISO-8601|null" }
}
```

`SnapshotBuilder.build(): IntelligenceSnapshot` is a suspend function that must complete in under 200 ms on a cold DB. It is pure with respect to Android — only Room DAO calls and pure Kotlin logic.

**Delta computation:** `SnapshotDiffer.diff(old: IntelligenceSnapshot, new: IntelligenceSnapshot): SnapshotDelta` produces a minimal diff object containing only the changed top-level keys. This delta is what gets sent to the LLM for intra-day updates instead of the full snapshot.

---

#### LLM Provider Interface

Lives in `:core:intelligence`. The rest of the App (including `:feature:intelligence`) calls only this interface. No class outside `:core:intelligence` imports a provider implementation.

```kotlin
// :core:intelligence
interface LLMProvider {
    val name: String
    suspend fun generateBrief(prompt: String): LLMResult
    suspend fun updateGuidance(prompt: String): LLMResult
    fun isAvailable(): Boolean  // quick connectivity + key check, no network call
}

sealed class LLMResult {
    data class Success(val text: String, val tokenCount: Int) : LLMResult()
    data class Failure(val reason: FailureReason, val message: String) : LLMResult()
}

enum class FailureReason { OFFLINE, API_ERROR, RATE_LIMITED, INVALID_KEY, TIMEOUT }
```

**Implementations (all in `:core:intelligence`):**

| Class | Provider | Notes |
|---|---|---|
| `DeepSeekProvider` | DeepSeek API | V1 default; chat completions endpoint |
| `MockProvider` | None | Returns templated strings; used in tests and when no key is configured |

V2 additions (`GeminiProvider`, `OpenAIProvider`) are drop-in by implementing `LLMProvider` and registering in `LLMProviderFactory`. No other file changes needed.

**Provider selection:** `LLMProviderFactory` reads `aiProvider` from `SettingsRepository` and returns the correct implementation. Injected via Hilt as `@Singleton`.

**API key security:** The DeepSeek API key is stored in `EncryptedSharedPreferences` (Jetpack Security). It is never stored in the Room `settings` table, never logged, and never included in the JSON backup export.

---

#### Prompt Builder

`PromptBuilder` in `:core:intelligence` converts an `IntelligenceSnapshot` or `SnapshotDelta` into a token-efficient prompt string.

**System prompt (constant, ~120 tokens):**
```
You are a concise academic coach for an engineering student. 
You receive a structured JSON snapshot of their academic state.
Your job: prioritise their day, explain what matters most, and 
motivate them in 3–5 sentences. Never invent facts. 
Never output JSON. Output only human-friendly guidance text.
```

**User prompt:** The snapshot or delta JSON, minified (no whitespace). Keys are abbreviated to reduce tokens (e.g., `"att_warn"` instead of `"attendance_warnings"`). `PromptBuilder` owns this abbreviation mapping so the LLM and the schema remain decoupled.

**Token budget:** Full snapshot prompt targets ≤ 400 tokens total (system + user). Delta prompt targets ≤ 150 tokens. These limits are enforced by `PromptBuilder` — if the snapshot exceeds budget, lower-priority sections (cp_summary, free_slots) are omitted first.

---

#### Deterministic Fallback Engine

`DeterministicFallback` is the original `DailyBriefComposer` promoted to be the fallback path. It accepts an `IntelligenceSnapshot` and produces structured guidance text using hard-coded templates.

This component has zero network dependencies. It is always available. Its output is always structurally complete (no missing sections). The LLM path replaces the *text* of the guidance sections; the *structure* is always deterministic.

---

#### Recommendation Cache

`RecommendationCache` in `:core:intelligence` wraps the `recommendation_cache` Room DAO.

- Before any LLM call, the current snapshot hash (SHA-256) is checked against `recommendation_cache.snapshot_hash`.
- If a match is found and the entry is less than `cacheMaxAgeHours` old (default 6 hours), the cached response is returned without an API call.
- After a successful LLM call, the response is written to `recommendation_cache` (upsert on `snapshot_hash`). The table retains only the last 7 entries to limit storage.

---

#### Intelligence Orchestrator

`IntelligenceOrchestrator` in `:feature:intelligence` is the single coordinator that wires all sub-components. It is a `@Singleton` ViewModel-layer service (not a ViewModel itself).

**Morning brief flow:**
```
DailyBriefWorker
  → SnapshotBuilder.build()
  → RecommendationCache.check(hash)      // cache hit? return cached
  → PromptBuilder.buildFullPrompt(snapshot)
  → LLMProvider.generateBrief(prompt)
      ├── Success → persist to daily_briefs.llm_guidance + recommendation_cache
      └── Failure → DeterministicFallback.compose(snapshot) → persist with guidance_source=DETERMINISTIC
  → NotificationManager.sendDailyBrief()
  → ai_call_log entry written
```

**Intra-day event flow:**
```
AppEventBus.collect(event)
  → debounce 30 seconds
  → SnapshotBuilder.build()
  → SnapshotDiffer.diff(lastSnapshot, newSnapshot)
  → if delta is empty: skip
  → RecommendationCache.check(newHash)   // cache hit? update brief silently
  → PromptBuilder.buildDeltaPrompt(delta)
  → LLMProvider.updateGuidance(prompt)
      ├── Success → update daily_briefs.llm_guidance + daily_briefs.guidance_updated_at
      └── Failure → DeterministicFallback.compose(newSnapshot) → update with guidance_source=DETERMINISTIC
  → ai_call_log entry written
```

**Rate limiting:** `RateLimiter` in `:core:intelligence` enforces a maximum of 10 LLM calls per day (configurable via `settings`, default 10). When the limit is reached, the deterministic fallback is used silently and the UI shows a subtle "AI quota reached" badge. This bounds cost to roughly 10 × (DeepSeek ~$0.0001/400 tokens) = $0.001/day, well within the 3–4 call/day expected usage.

---

**Score Target Formula**

Default weights (all Student-configurable via `settings`):

| Input | Default Weight |
|---|---|
| Each class to attend today | 10 pts |
| Each assignment due within 48 h | 20 pts |
| Active project Next_Immediate_Action exists | 15 pts |
| DSA revision topic available | 10 pts |

Score target = sum of applicable weighted items. Range is 0–unbounded; displayed as a point target (e.g., "Earn 65 pts today"). Actual score increments as the user completes items and is persisted to `daily_briefs.score_actual`.

---

### Notification Manager

All notifications use Android's `NotificationCompat` with `WorkManager` one-time workers for scheduled delivery. No remote push service is used.

| Category | Channel Priority | WorkManager type |
|---|---|---|
| `DAILY_BRIEF` | DEFAULT | Periodic (daily) |
| `ASSIGNMENT_REMINDER` | HIGH | One-time (per assignment) |
| `CLASS_REMINDER` | DEFAULT | One-time (per class event) |
| `CONTEST_REMINDER` | HIGH | One-time (per upcoming contest) |
| `FREE_SLOT_RECOMMENDATION` | LOW | One-time (generated with daily brief) |
| `INACTIVE_PROJECT_REMINDER` | DEFAULT | Periodic (daily check) |

`PRIORITY_MAX` is not used for any channel. All channels respect Do Not Disturb.

On App startup, `NotificationRescheduler` queries all pending assignment/class/contest workers and re-enqueues any that were dropped, completing within 30 seconds of launch.

---

## Data Backup and Restore

**Export:** `ExportUseCase` serialises all Room tables to a structured JSON file. File attachments ≤ 10 MB are embedded as Base64; larger files are exported as a `{ "external_path": "...", "warning": "file not embedded" }` object. The export file is written to the user's selected destination via the Storage Access Framework (`ACTION_CREATE_DOCUMENT`).

**Import:** `ImportUseCase` parses the JSON, shows the user a confirmation dialog ("This will overwrite all existing data"), then runs a single Room transaction: truncate all tables → re-insert all exported records → restore attachment files.

---

## Settings Architecture

A typed `SettingsRepository` wraps the `settings` key-value table and exposes named properties:

```kotlin
var attendanceThreshold: Int          // default 75
var cpSyncIntervalMinutes: Int        // default 360, min 15
var dailyBriefTimeHHmm: String        // default "07:00"
var defaultAssignmentReminderLeadMs: Long  // default 86_400_000 (24 h)
var contestReminderLookaheadMs: Long  // default 86_400_000 (24 h)
var projectInactivityThresholdDays: Int   // default 7
var scoreWeightClass: Int             // default 10
var scoreWeightAssignment: Int        // default 20
var scoreWeightProjectAction: Int     // default 15
var scoreWeightDsa: Int               // default 10
var ocrConfidenceThreshold: Float     // default 0.80
// AI settings
var aiEnabled: Boolean                // default true
var aiProvider: String                // default "DEEPSEEK"
var aiIntradayUpdatesEnabled: Boolean // default true
var aiMaxCallsPerDay: Int             // default 10
var aiCacheMaxAgeHours: Int           // default 6
var aiTonePreference: String          // default "motivational" ("motivational"|"concise"|"neutral")
```

The DeepSeek API key is stored separately in `EncryptedSharedPreferences` under key `"deepseek_api_key"` and is never exposed through `SettingsRepository`.

---

## Key Technology Choices

| Concern | Choice | Rationale |
|---|---|---|
| Language | Kotlin | Standard Android; coroutines for async |
| UI | Jetpack Compose | Declarative, testable, modern Android |
| Database | Room (SQLite) | Offline-first, typed DAOs, migration support |
| Background work | WorkManager | Survives reboots, handles Doze mode |
| OCR | ML Kit Text Recognition | On-device, no internet required |
| DI | Hilt | Gradle module-friendly, Compose-integrated |
| Navigation | Navigation Component (Compose) | Single-activity, type-safe routes |
| Build | Gradle (Kotlin DSL) | Multi-module, version catalog |
| LLM HTTP client | Retrofit + OkHttp | Consistent with CP sync; timeout and retry interceptors |
| Event bus | Kotlin `SharedFlow` | Zero-dependency, coroutine-native, in-process only |
| API key storage | Jetpack Security `EncryptedSharedPreferences` | AES-256-GCM, Android Keystore backed |

---

## Security and Privacy

- All data is stored on-device in App-private storage. No data leaves the device except CP sync API calls (user-configured handles only) and LLM API calls (Intelligence_Snapshot only, when AI is enabled and internet is available).
- The Intelligence_Snapshot sent to the LLM contains no personally identifiable information. Subject names, assignment titles, and project names are included as the Student entered them — the Student is informed of this in the AI settings screen.
- The DeepSeek API key is stored in `EncryptedSharedPreferences` and is never written to the Room database, never included in the JSON backup, and never logged.
- File attachments are stored in `filesDir` (not accessible to other apps without root).
- The JSON export is written to user-chosen external storage only on explicit user action.
- No analytics, crash reporting, or telemetry SDKs are included in V1.
