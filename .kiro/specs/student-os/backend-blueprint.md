
# Backend Blueprint — Student OS
### Pre-Implementation Production Review
**Role:** Staff Android Engineer / Senior Database Engineer  
**Source of Truth:** requirements.md · design.md · tasks.md  
**Date:** 2026-07-25

---

## Section 1 — Complete ER Diagram

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                        STUDENT OS — ENTITY RELATIONSHIP DIAGRAM             ║
╚══════════════════════════════════════════════════════════════════════════════╝

  ┌───────────────┐         ┌──────────────────┐
  │   subjects    │1───────N│  timetable_slots  │
  │───────────────│         │──────────────────│
  │ PK id         │         │ PK id            │
  │ name          │         │ FK subject_id    │
  │ archived_at   │         │ day_of_week      │
  └───────┬───────┘         │ start_time       │
          │                 │ end_time         │
          │                 │ location         │
          │                 │ week_parity      │
          │                 │ valid_from       │
          │                 │ valid_until      │
          │                 └────────┬─────────┘
          │                          │
          │    1                     │ 1
          │    │    ┌────────────────┤
          │    │    │                │
          ├────┼────┤         ┌──────┴──────────────┐
          │    │    │         │    class_events      │
          │    │    │         │─────────────────────│
          │    │    └─────────│ PK id               │
          │    └──────────────│ FK timetable_slot_id│ (nullable — Extra_Class)
          │                   │ FK subject_id        │ (denorm, RESTRICT)
          │                   │ FK linked_slot_id    │ (nullable — shifted class)
          │                   │ scheduled_at         │
          │                   │ end_at               │
          │                   │ status               │
          │                   │ is_extra             │
          │                   │ updated_at           │
          │                   └─────────────────────┘
          │
          │1─────────────────N┌──────────────────────┐
          │                   │     assignments       │
          │                   │──────────────────────│
          │                   │ PK id                │
          │                   │ FK subject_id (RESTR) │
          │                   │ title                │
          │                   │ deadline             │
          │                   │ priority             │
          │                   │ notes                │
          │                   │ attachment_uri        │
          │                   │ status               │
          │                   │ reminder_lead_ms     │
          │                   │ created_at           │
          │                   └──────────────────────┘
          │
  ════════╪═══════════════════ CODING MODULE ════════════════════════
          │
          │         ┌────────────────────┐
          │         │    cp_profiles     │1──────────N┌──────────────────┐
          │         │────────────────────│            │   cp_contests     │
          │         │ PK id             │            │──────────────────│
          │         │ platform           │            │ PK id            │
          │         │ handle             │            │ FK profile_id    │
          │         │ current_rating     │            │ contest_name     │
          │         │ last_synced_at     │            │ contest_date     │
          │         └────────────────────┘            │ rank             │
          │                                           │ rating_change    │
          │                              1            │ problems_solved  │
          │                              └────────────┤                  │
          │                                      ┌────┤ (RESTRICT)       │
          │                                      │    └──────────────────┘
          │                                      │
          │                              ┌───────┴──────────┐
          │                              │  cp_reflections  │
          │                              │──────────────────│
          │                              │ PK id            │
          │                              │ FK contest_id    │
          │                              │   (UNIQUE,RESTR) │
          │                              │ went_wrong       │
          │                              │ to_revise        │
          │                              │ self_rating      │
          │                              └──────────────────┘
          │
          │    ┌──────────────────┐1────────N┌────────────────┐
          │    │  dsa_categories  │          │   dsa_topics   │
          │    │──────────────────│          │────────────────│
          │    │ PK id            │          │ PK id          │
          │    │ name             │          │ FK category_id │
          │    │ sort_order       │          │ name           │
          │    └──────────────────┘          │ confidence_lvl │
          │                                  │ revision_status│
          │                                  │ notes          │
          │                                  └────────────────┘
          │
  ════════╪═══════════════════ PROJECT MODULE ═════════════════════
          │
          │    ┌──────────────────┐
          │    │    projects      │
          │    │──────────────────│
          │    │ PK id            │
          │    │ title            │
          │    │ overview         │
          │    │ github_url       │
          │    │ notes            │
          │    │ archived_at      │
          │    │ inactivity_days  │
          │    │ last_activity_at │
          │    └────────┬─────────┘
          │             │1
          │    ┌────────┼──────────────────────────────────────┐
          │    │        │                                       │
          │    │  N     │  N                            N       │  N
          │    │        │                                       │
          │ ┌──┴──────┐ │┌───────────┐  ┌───────────────┐ ┌───┴──────────────┐
          │ │milestone│ ││   bugs    │  │ project_tasks │ │project_resources │
          │ │─────────│ ││───────────│  │───────────────│ │──────────────────│
          │ │PK id    │ ││PK id      │  │PK id          │ │PK id             │
          │ │FK proj  │ ││FK proj    │  │FK project_id  │ │FK project_id     │
          │ │title    │ ││description│  │title          │ │label             │
          │ │desc     │ ││severity   │  │is_next_action │ │url               │
          │ │target_dt│ ││status     │  │is_parallel    │ └──────────────────┘
          │ │status   │ │└───────────┘  │completed_at   │
          │ └─────────┘ │               │sort_order     │
          │             │               └───────────────┘
          │
  ════════╪═══════════ INTELLIGENCE MODULE ════════════════════════
          │
          │    ┌───────────────────────────┐
          │    │       daily_briefs         │
          │    │───────────────────────────│
          │    │ PK id                     │
          │    │ date (UNIQUE)             │
          │    │ json_snapshot             │
          │    │ snapshot_hash             │
          │    │ brief_json                │
          │    │ llm_guidance              │
          │    │ guidance_source           │
          │    │ score_target              │
          │    │ score_actual              │
          │    │ generated_at              │
          │    │ guidance_updated_at       │
          │    └───────────────────────────┘
          │
          │    ┌───────────────────────────┐
          │    │    recommendation_cache    │
          │    │───────────────────────────│
          │    │ PK id                     │
          │    │ snapshot_hash (UNIQUE)    │
          │    │ llm_response              │
          │    │ provider                  │
          │    │ created_at               │
          │    │ token_count              │
          │    └───────────────────────────┘
          │
          │    ┌───────────────────────────┐
          │    │       ai_call_log          │
          │    │───────────────────────────│
          │    │ PK id                     │
          │    │ triggered_by              │
          │    │ snapshot_hash             │
          │    │ was_cache_hit             │
          │    │ was_delta                 │
          │    │ latency_ms               │
          │    │ token_count              │
          │    │ success                  │
          │    │ error_message            │
          │    │ created_at               │
          │    └───────────────────────────┘
          │
  ════════╪═══════════ SETTINGS (SINGLETON KV) ═══════════════════
          │
          │    ┌───────────────┐
          └────│    settings   │
               │───────────────│
               │ PK key        │
               │ value         │
               └───────────────┘

═══════════════════════════════════════════════════════════════════
RELATIONSHIP SUMMARY

Table                FK Target           Cardinality   On Delete
─────────────────── ─────────────────── ───────────── ──────────
timetable_slots     subjects            N:1           RESTRICT
class_events        timetable_slots     N:1 (null ok) RESTRICT
class_events        subjects            N:1           RESTRICT
class_events        timetable_slots     N:1 (null ok) RESTRICT (linked_slot_id)
assignments         subjects            N:1           RESTRICT
cp_contests         cp_profiles         N:1           CASCADE
cp_reflections      cp_contests         1:1 UNIQUE    CASCADE
dsa_topics          dsa_categories      N:1           RESTRICT
milestones          projects            N:1           CASCADE
bugs                projects            N:1           CASCADE
project_tasks       projects            N:1           CASCADE
project_resources   projects            N:1           CASCADE

Notes:
  RESTRICT = cannot delete parent while children exist
  CASCADE  = delete parent deletes children (safe for owned sub-entities)
  No FKs between intelligence tables and other modules —
    intelligence reads via SnapshotBuilder, decoupled by design.
═══════════════════════════════════════════════════════════════════
```

---

## Section 2 — Database Schema Review

### 2.1 Normalization Analysis

| Table | Normal Form | Finding |
|---|---|---|
| `subjects` | 3NF ✓ | Clean. Single responsibility. |
| `timetable_slots` | 3NF ✓ | `start_time` / `end_time` as TEXT strings is acceptable for a schedule domain where arithmetic on times is minimal. |
| `class_events` | **2NF concern** | `subject_id` is denormalised (derivable via `timetable_slot_id → timetable_slots.subject_id`). This is an **intentional and correct trade-off** for query performance on attendance aggregates — document it as a deliberate denormalisation, not a flaw. Must be kept in sync on every write. |
| `assignments` | 3NF ✓ | Clean. |
| `cp_profiles` | 3NF ✓ | `current_rating` is a snapshot column (latest known value), not derivable — correct. |
| `cp_contests` | 3NF ✓ | Clean. |
| `cp_reflections` | 3NF ✓ | UNIQUE FK on `contest_id` correctly enforces 1:1. |
| `dsa_categories` | 3NF ✓ | Clean. |
| `dsa_topics` | 3NF ✓ | Clean. |
| `projects` | 3NF ✓ | `last_activity_at` is a managed column, not derivable cheaply — correct to store. |
| `milestones` | 3NF ✓ | Clean. |
| `bugs` | 3NF ✓ | Clean. |
| `project_tasks` | 3NF ✓ | `is_next_action` flag is a controlled denormalisation; valid if enforced transactionally. |
| `project_resources` | 3NF ✓ | Clean. |
| `daily_briefs` | **Noted** | `json_snapshot` + `brief_json` + `llm_guidance` are large TEXT blobs. Acceptable for a log table with bounded rows (one per day). No normalisation issue — these are opaque payloads. |
| `recommendation_cache` | 3NF ✓ | Small table, bounded to 7 rows. Clean. |
| `ai_call_log` | 3NF ✓ | Append-only log. Clean. |
| `settings` | Key-Value ✓ | EAV pattern intentionally chosen. Acceptable given typed wrapper. |

### 2.2 Issues Found and Fixes Required

**Issue 1 — Missing `updated_at` on `assignments`**  
The design includes `created_at` but not `updated_at` on `assignments`. The backup restore path needs a reliable modification timestamp to detect conflicts in future V2 sync scenarios. Also useful for sorting in "recently modified" views.  
**Fix:** Add `updated_at INTEGER NOT NULL DEFAULT 0` to `assignments`. Update on every write.

**Issue 2 — `dsa_topics` has no `updated_at`**  
`DsaTopicUpdated` events carry only a `topicId`. The snapshot builder needs to know when a topic was last changed to compute `hours_since_revision` in future versions. Without a timestamp, the snapshot cannot include recency data.  
**Fix:** Add `updated_at INTEGER NOT NULL DEFAULT 0` to `dsa_topics`.

**Issue 3 — `cp_contests` has no uniqueness constraint**  
If `CpSyncWorker` runs twice (race condition, retry), it can insert duplicate contest rows for the same contest from the same profile. The design says "upsert via INSERT OR REPLACE" but there is no UNIQUE constraint defined to drive the replace behaviour.  
**Fix:** Add `UNIQUE(profile_id, contest_name, contest_date)`. This makes `INSERT OR REPLACE` deterministic.

**Issue 4 — `project_tasks.is_next_action` flag integrity**  
The design says "enforced by trigger: only one row per project may be 1." SQLite does not support conditional UNIQUE constraints for this pattern natively. A partial unique index `CREATE UNIQUE INDEX idx_one_next_action ON project_tasks(project_id) WHERE is_next_action = 1 AND is_parallel = 0` achieves this in SQLite 3.8.9+. Android's bundled SQLite version supports this.  
**Fix:** Define the partial unique index in the migration SQL. This replaces the trigger approach.  
Note: when `is_parallel = 1`, the constraint is intentionally relaxed — correct.

**Issue 5 — `ai_call_log` has no retention policy at schema level**  
The design bounds `recommendation_cache` to 7 rows, but `ai_call_log` is unbounded. At 10 calls/day × 365 days = 3,650 rows/year. Each row is small (~200 bytes), so ~730 KB/year — not a storage crisis, but the diagnostics screen would need pagination.  
**Fix:** Add a `TRIGGER` or background cleanup in `RateLimiter` to delete entries older than 90 days. Document this as a maintenance responsibility in code comments.

**Issue 6 — `class_events.status` uses TEXT with no CHECK constraint**  
SQLite doesn't enforce TEXT enum values without a CHECK constraint. A typo or bug could silently write an invalid status, corrupting attendance calculations.  
**Fix:** Add `CHECK (status IN ('UNMARKED','PRESENT','ABSENT','CANCELLED','HOLIDAY','EXTRA_CLASS'))` to the column definition. Apply the same pattern to `assignments.status`, `assignments.priority`, `milestones.status`, `bugs.status`, `bugs.severity`, `dsa_topics.revision_status`, `daily_briefs.guidance_source`.

**Issue 7 — `timetable_slots` has no compound uniqueness**  
Nothing prevents inserting two slots for the same subject on the same day/time/parity. An accidental double-import creates phantom class events.  
**Fix:** Add `UNIQUE(subject_id, day_of_week, start_time, week_parity, valid_from)`.

**Issue 8 — `settings.value` is `TEXT NOT NULL` but booleans and integers are stored as strings**  
This is the documented approach with a typed wrapper, but it means there's no DB-level validation that `attendanceThreshold` is between 1 and 100. Corruption is possible if the wrapper is bypassed (e.g., direct DB inspection by the user during development).  
**Accepted risk.** The typed `SettingsRepository` is the only write path in production. No schema fix needed, but add a validation guard in `SettingsRepository.set()`.

### 2.3 Nullable Column Audit

| Column | Table | Nullable? | Correct? |
|---|---|---|---|
| `timetable_slots.location` | timetable_slots | YES | ✓ Location is optional |
| `timetable_slots.week_parity` | timetable_slots | YES | ✓ NULL = every week |
| `timetable_slots.valid_until` | timetable_slots | YES | ✓ Open-ended timetable |
| `class_events.timetable_slot_id` | class_events | YES | ✓ Extra_Class has no slot |
| `class_events.linked_slot_id` | class_events | YES | ✓ Only for shifted classes |
| `assignments.notes` | assignments | YES | ✓ Optional |
| `assignments.attachment_uri` | assignments | YES | ✓ Optional |
| `assignments.reminder_lead_ms` | assignments | YES | ✓ NULL = use global default |
| `projects.github_url` | projects | YES | ✓ Optional |
| `projects.archived_at` | projects | YES | ✓ NULL = active |
| `project_tasks.completed_at` | project_tasks | YES | ✓ NULL = not yet done |
| `daily_briefs.llm_guidance` | daily_briefs | YES | ✓ NULL = deterministic only |
| `ai_call_log.error_message` | ai_call_log | YES | ✓ NULL = success |
| `cp_profiles.current_rating` | cp_profiles | Should be nullable | ⚠ New profile before first sync has no rating. Mark nullable. |

---

## Section 3 — Final Room Entity List

### 3.1 subjects
- **Table name:** `subjects`
- **Responsibility:** Canonical registry of all academic subjects. Shared FK target for attendance and assignment modules.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** None
- **Important columns:** `name TEXT NOT NULL`, `archived_at INTEGER` (NULL = active; epoch ms = soft-deleted)
- **Indexes:** `idx_subjects_active` on `(archived_at)` for filtering active subjects
- **Constraints:** None beyond PK

---

### 3.2 timetable_slots
- **Table name:** `timetable_slots`
- **Responsibility:** Defines the recurring weekly schedule template. Each row represents one recurring class slot. Not a calendar event — a template from which `class_events` are generated.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `subject_id → subjects(id) ON DELETE RESTRICT`
- **Important columns:** `day_of_week INTEGER` (1–7), `start_time TEXT`, `end_time TEXT`, `week_parity TEXT` (NULL / 'ODD' / 'EVEN'), `valid_from INTEGER`, `valid_until INTEGER`
- **Indexes:** `idx_slots_subject` on `(subject_id)`, `idx_slots_day` on `(day_of_week, week_parity)`
- **Constraints:** `UNIQUE(subject_id, day_of_week, start_time, week_parity, valid_from)` — prevents duplicate slot import

---

### 3.3 class_events
- **Table name:** `class_events`
- **Responsibility:** Every individual occurrence of a class (generated from timetable slots, or manually created as Extra_Class). This is the record that gets marked Present/Absent/etc. The source of truth for all attendance calculations.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `timetable_slot_id → timetable_slots(id) ON DELETE RESTRICT` (nullable), `subject_id → subjects(id) ON DELETE RESTRICT`, `linked_slot_id → timetable_slots(id) ON DELETE RESTRICT` (nullable, for shifted classes)
- **Important columns:** `scheduled_at INTEGER NOT NULL`, `end_at INTEGER NOT NULL`, `status TEXT NOT NULL DEFAULT 'UNMARKED'`, `is_extra INTEGER NOT NULL DEFAULT 0`, `updated_at INTEGER NOT NULL`
- **Indexes:** `idx_events_subject_date` on `(subject_id, scheduled_at)` — primary attendance query, `idx_events_date` on `(scheduled_at)` — daily schedule queries, `idx_events_status` on `(subject_id, status)` — attendance count aggregates
- **Constraints:** `CHECK(status IN ('UNMARKED','PRESENT','ABSENT','CANCELLED','HOLIDAY','EXTRA_CLASS'))`, `CHECK(is_extra IN (0,1))`

---

### 3.4 assignments
- **Table name:** `assignments`
- **Responsibility:** Tracks all student assignments with their deadlines, priorities, and lifecycle status.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `subject_id → subjects(id) ON DELETE RESTRICT`
- **Important columns:** `deadline INTEGER NOT NULL`, `priority TEXT NOT NULL DEFAULT 'MEDIUM'`, `status TEXT NOT NULL DEFAULT 'PENDING'`, `attachment_uri TEXT`, `reminder_lead_ms INTEGER`, `created_at INTEGER NOT NULL`, `updated_at INTEGER NOT NULL`
- **Indexes:** `idx_assignments_deadline` on `(deadline, status)` — overdue and upcoming queries, `idx_assignments_subject` on `(subject_id)`
- **Constraints:** `CHECK(status IN ('PENDING','IN_PROGRESS','SUBMITTED','COMPLETED'))`, `CHECK(priority IN ('HIGH','MEDIUM','LOW'))`

---

### 3.5 cp_profiles
- **Table name:** `cp_profiles`
- **Responsibility:** Stores the student's competitive programming handles and current ratings for CodeChef and Codeforces (max 2 rows).
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** None
- **Important columns:** `platform TEXT NOT NULL`, `handle TEXT NOT NULL`, `current_rating INTEGER` (nullable — unsynced), `last_synced_at INTEGER`
- **Indexes:** `idx_cp_platform` on `(platform)` — UNIQUE
- **Constraints:** `UNIQUE(platform)` — at most one profile per platform, `CHECK(platform IN ('CODECHEF','CODEFORCES'))`

---

### 3.6 cp_contests
- **Table name:** `cp_contests`
- **Responsibility:** Stores contest history fetched from CP APIs. Append-and-replace on sync.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `profile_id → cp_profiles(id) ON DELETE CASCADE`
- **Important columns:** `contest_name TEXT NOT NULL`, `contest_date INTEGER NOT NULL`, `rank INTEGER`, `rating_change INTEGER`, `problems_solved INTEGER`
- **Indexes:** `idx_contests_profile_date` on `(profile_id, contest_date DESC)` — contest history ordered list
- **Constraints:** `UNIQUE(profile_id, contest_name, contest_date)` — prevents sync duplicates

---

### 3.7 cp_reflections
- **Table name:** `cp_reflections`
- **Responsibility:** Stores the student's post-contest self-reflection. One reflection per contest (enforced).
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `contest_id → cp_contests(id) ON DELETE CASCADE`
- **Important columns:** `went_wrong TEXT`, `to_revise TEXT`, `self_rating INTEGER NOT NULL`
- **Indexes:** Covered by UNIQUE constraint on `contest_id`
- **Constraints:** `UNIQUE(contest_id)`, `CHECK(self_rating BETWEEN 1 AND 5)`

---

### 3.8 dsa_categories
- **Table name:** `dsa_categories`
- **Responsibility:** Top-level groupings in the Knowledge Tree (e.g., "Trees", "Graphs").
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** None
- **Important columns:** `name TEXT NOT NULL`, `sort_order INTEGER NOT NULL DEFAULT 0`
- **Indexes:** None needed at V1 scale (< 30 categories expected)
- **Constraints:** `UNIQUE(name)`

---

### 3.9 dsa_topics
- **Table name:** `dsa_topics`
- **Responsibility:** Individual DSA topics within a category. Tracks mastery state.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `category_id → dsa_categories(id) ON DELETE RESTRICT`
- **Important columns:** `name TEXT NOT NULL`, `confidence_level INTEGER NOT NULL DEFAULT 1`, `revision_status TEXT NOT NULL DEFAULT 'NOT_STARTED'`, `notes TEXT`, `updated_at INTEGER NOT NULL`
- **Indexes:** `idx_dsa_suggestion` on `(revision_status, confidence_level)` — topic suggestion query (lowest confidence among non-revised topics)
- **Constraints:** `CHECK(confidence_level BETWEEN 1 AND 5)`, `CHECK(revision_status IN ('NOT_STARTED','IN_PROGRESS','REVISED'))`, `UNIQUE(category_id, name)`

---

### 3.10 projects
- **Table name:** `projects`
- **Responsibility:** Project records — the root entity for milestones, bugs, tasks, and resources.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** None
- **Important columns:** `title TEXT NOT NULL`, `archived_at INTEGER` (NULL = active), `inactivity_threshold_days INTEGER NOT NULL DEFAULT 7`, `last_activity_at INTEGER NOT NULL`
- **Indexes:** `idx_projects_active` on `(archived_at)` — filter active/archived
- **Constraints:** `CHECK(inactivity_threshold_days > 0)`

---

### 3.11 milestones
- **Table name:** `milestones`
- **Responsibility:** Time-bounded goals within a project.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `project_id → projects(id) ON DELETE CASCADE`
- **Important columns:** `target_date INTEGER`, `status TEXT NOT NULL DEFAULT 'PENDING'`
- **Indexes:** `idx_milestones_project` on `(project_id, status)`
- **Constraints:** `CHECK(status IN ('PENDING','IN_PROGRESS','DONE'))`

---

### 3.12 bugs
- **Table name:** `bugs`
- **Responsibility:** Issue tracker scoped to a project.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `project_id → projects(id) ON DELETE CASCADE`
- **Important columns:** `description TEXT NOT NULL`, `severity TEXT NOT NULL DEFAULT 'MEDIUM'`, `status TEXT NOT NULL DEFAULT 'OPEN'`
- **Indexes:** `idx_bugs_project_status` on `(project_id, status)`
- **Constraints:** `CHECK(severity IN ('LOW','MEDIUM','HIGH'))`, `CHECK(status IN ('OPEN','RESOLVED'))`

---

### 3.13 project_tasks
- **Table name:** `project_tasks`
- **Responsibility:** Task list for a project. Exactly one task may be the Next_Immediate_Action (unless parallel mode is enabled).
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `project_id → projects(id) ON DELETE CASCADE`
- **Important columns:** `title TEXT NOT NULL`, `is_next_action INTEGER NOT NULL DEFAULT 0`, `is_parallel INTEGER NOT NULL DEFAULT 0`, `completed_at INTEGER` (NULL = pending), `sort_order INTEGER NOT NULL DEFAULT 0`
- **Indexes:** `idx_tasks_project_next` on `(project_id, is_next_action)`, partial UNIQUE index: `idx_one_next_action UNIQUE ON (project_id) WHERE is_next_action=1 AND is_parallel=0`
- **Constraints:** `CHECK(is_next_action IN (0,1))`, `CHECK(is_parallel IN (0,1))`

---

### 3.14 project_resources
- **Table name:** `project_resources`
- **Responsibility:** Reference links attached to a project.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** `project_id → projects(id) ON DELETE CASCADE`
- **Important columns:** `url TEXT NOT NULL`, `label TEXT`
- **Indexes:** `idx_resources_project` on `(project_id)`
- **Constraints:** None

---

### 3.15 daily_briefs
- **Table name:** `daily_briefs`
- **Responsibility:** One record per calendar day. Stores the Intelligence_Snapshot, structured brief output, LLM guidance text, score tracking, and generation metadata. Provides historical brief review.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** None (intelligence module reads other tables via SnapshotBuilder; no FK coupling)
- **Important columns:** `date TEXT NOT NULL UNIQUE`, `json_snapshot TEXT NOT NULL`, `snapshot_hash TEXT NOT NULL`, `brief_json TEXT NOT NULL`, `llm_guidance TEXT` (nullable), `guidance_source TEXT NOT NULL`, `score_target INTEGER NOT NULL DEFAULT 0`, `score_actual INTEGER NOT NULL DEFAULT 0`, `generated_at INTEGER NOT NULL`, `guidance_updated_at INTEGER NOT NULL`
- **Indexes:** `idx_brief_date` on `(date DESC)` — history list, `idx_brief_hash` on `(snapshot_hash)` — cache lookup
- **Constraints:** `UNIQUE(date)`, `CHECK(guidance_source IN ('LLM','DETERMINISTIC'))`

---

### 3.16 recommendation_cache
- **Table name:** `recommendation_cache`
- **Responsibility:** Caches the most recent LLM responses keyed by snapshot hash. Prevents redundant API calls when the student's state has not changed. Bounded to 7 rows.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** None
- **Important columns:** `snapshot_hash TEXT NOT NULL UNIQUE`, `llm_response TEXT NOT NULL`, `provider TEXT NOT NULL`, `created_at INTEGER NOT NULL`, `token_count INTEGER NOT NULL`
- **Indexes:** Covered by UNIQUE constraint on `snapshot_hash`
- **Constraints:** `UNIQUE(snapshot_hash)`, `CHECK(token_count >= 0)`

---

### 3.17 ai_call_log
- **Table name:** `ai_call_log`
- **Responsibility:** Append-only diagnostic log of every LLM call attempt (hit or miss, success or failure). Drives the AI diagnostics screen and cost estimation. Retained for 90 days.
- **Primary key:** `id` INTEGER AUTOINCREMENT
- **Foreign keys:** None
- **Important columns:** `triggered_by TEXT NOT NULL`, `snapshot_hash TEXT NOT NULL`, `was_cache_hit INTEGER NOT NULL`, `was_delta INTEGER NOT NULL`, `latency_ms INTEGER`, `token_count INTEGER NOT NULL DEFAULT 0`, `success INTEGER NOT NULL`, `error_message TEXT`, `created_at INTEGER NOT NULL`
- **Indexes:** `idx_ai_log_date` on `(created_at DESC)` — diagnostics screen ordering and 90-day cleanup
- **Constraints:** `CHECK(was_cache_hit IN (0,1))`, `CHECK(was_delta IN (0,1))`, `CHECK(success IN (0,1))`

---

### 3.18 settings
- **Table name:** `settings`
- **Responsibility:** Key-value store for all student-configurable parameters. Wraps attendance threshold, sync intervals, score weights, AI settings, and notification preferences.
- **Primary key:** `key TEXT`
- **Foreign keys:** None
- **Important columns:** `value TEXT NOT NULL`
- **Indexes:** PK is the only needed index (single-row lookups by key)
- **Constraints:** None (type validation enforced by `SettingsRepository` wrapper)

---

## Section 4 — DAO Blueprint

### 4.1 SubjectDao
**Insert:** insert single subject; insert list (timetable import transaction)  
**Update:** update name; set archived_at (soft-delete)  
**Delete:** no hard deletes — archive only  
**Flow queries:** `getActiveSubjects(): Flow<List<Subject>>`; `getAllSubjectsIncludingArchived(): Flow<List<Subject>>`; `getSubjectById(id): Flow<Subject?>`  
**Aggregate:** `getSubjectCount(): Int` (used by SnapshotBuilder)

---

### 4.2 TimetableSlotDao
**Insert:** insert single slot; insert list (bulk import — used inside timetable import transaction)  
**Update:** update slot fields (time, location, parity, valid_until)  
**Delete:** delete slot by id (cascades to class_events via RESTRICT — must delete events first in transaction)  
**Flow queries:** `getSlotsForSubject(subjectId): Flow<List<TimetableSlot>>`; `getSlotsForDay(dayOfWeek, parity): Flow<List<TimetableSlot>>`; `getActiveSlotsOnDate(epochMs, parity): List<TimetableSlot>` (suspend, used by SnapshotBuilder)  
**Aggregate:** none

---

### 4.3 ClassEventDao
**Insert:** insert single event; insert list (batch generation during timetable import)  
**Update:** update status + updated_at; update end_at  
**Delete:** no hard deletes  
**Flow queries:** `getEventsForWeek(startEpoch, endEpoch): Flow<List<ClassEvent>>`; `getEventsForSubject(subjectId): Flow<List<ClassEvent>>`; `getEventsForDay(startEpoch, endEpoch): Flow<List<ClassEvent>>`  
**Aggregate queries (all suspend, for attendance calculation):**  
- `countByStatus(subjectId, status): Int`  
- `getAttendanceSummary(subjectId): AttendanceSummary` — single query returning present, absent, cancelled, holiday, extra_present counts  
- `getAttendanceSummaryAllSubjects(): List<SubjectAttendanceSummary>` — used by RecalibrationUseCase at startup  
**Analytics:** `getUnmarkedFutureEvents(nowEpoch): List<ClassEvent>` — for notification rescheduling

---

### 4.4 AssignmentDao
**Insert:** insert assignment  
**Update:** update status + updated_at; update deadline + updated_at; update reminder_lead_ms  
**Delete/Archive:** hard delete (with confirmation guard in Use Case layer, not DAO)  
**Flow queries:**  
- `getAssignmentsByStatus(status): Flow<List<Assignment>>`  
- `getAssignmentsToday(startEpoch, endEpoch): Flow<List<Assignment>>`  
- `getAssignmentsThisWeek(startEpoch, endEpoch): Flow<List<Assignment>>`  
- `getOverdueAssignments(nowEpoch): Flow<List<Assignment>>` — deadline < now AND status NOT IN (SUBMITTED, COMPLETED)  
- `getAssignmentById(id): Flow<Assignment?>`  
**Aggregate:** `getUrgentAssignments(withinEpoch): List<Assignment>` — suspend, used by SnapshotBuilder for assignments due within 48 h

---

### 4.5 CpProfileDao
**Insert/Upsert:** `upsert(profile)` — INSERT OR REPLACE  
**Update:** update current_rating + last_synced_at  
**Delete:** none (user removes handle via Settings, which updates the row)  
**Flow queries:** `getProfileByPlatform(platform): Flow<CpProfile?>`; `getAllProfiles(): Flow<List<CpProfile>>`  
**Aggregate:** `getProfilesForSnapshot(): List<CpProfile>` — suspend, used by SnapshotBuilder

---

### 4.6 CpContestDao
**Insert/Upsert:** `upsertContests(contests: List<CpContest>)` — bulk INSERT OR REPLACE  
**Update:** none (contests are immutable once synced)  
**Delete:** none directly; CASCADE from profile deletion  
**Flow queries:** `getContestsByProfile(profileId): Flow<List<CpContest>>`; `getRecentContests(profileId, limit): Flow<List<CpContest>>`  
**Aggregate:** `getUpcomingContests(nowEpoch, lookaheadEpoch): List<CpContest>` — for contest reminder scheduling

---

### 4.7 CpReflectionDao
**Insert:** insert reflection  
**Update:** update all fields (student can edit reflection)  
**Delete:** none (CASCADE from contest deletion handles cleanup)  
**Flow queries:** `getReflectionForContest(contestId): Flow<CpReflection?>`  
**Aggregate:** none

---

### 4.8 DsaCategoryDao
**Insert:** insert category  
**Update:** update name, sort_order  
**Delete:** delete by id (RESTRICT — must have no topics)  
**Flow queries:** `getAllCategories(): Flow<List<DsaCategory>>`; `getCategoryById(id): Flow<DsaCategory?>`  
**Aggregate:** `getCategoryCount(): Int`

---

### 4.9 DsaTopicDao
**Insert:** insert topic  
**Update:** update confidence_level + revision_status + notes + updated_at  
**Delete:** delete by id  
**Flow queries:** `getTopicsByCategory(categoryId): Flow<List<DsaTopic>>`; `getTopicsByRevisionStatus(status): Flow<List<DsaTopic>>`; `getTopicsFilteredBy(revisionStatus, confidenceLevel): Flow<List<DsaTopic>>`  
**Aggregate (all suspend, used by SnapshotBuilder and DsaTopicSuggester):**  
- `getSuggestedTopic(): DsaTopic?` — MIN(confidence_level) WHERE revision_status IN (NOT_STARTED, IN_PROGRESS) ORDER BY confidence_level ASC, id ASC LIMIT 1  
- `getAllMastered(): Boolean` — count WHERE confidence_level < 5 OR revision_status != REVISED → 0 means mastered  
- `getTopicCount(): Int`

---

### 4.10 ProjectDao
**Insert:** insert project  
**Update:** update fields; update last_activity_at; set archived_at  
**Delete:** none (archive only)  
**Flow queries:** `getActiveProjects(): Flow<List<Project>>`; `getArchivedProjects(): Flow<List<Project>>`; `getProjectById(id): Flow<Project?>`  
**Aggregate:** `getActiveProjectsForInactivityCheck(): List<Project>` — suspend, used by ProjectInactivityWorker; `getProjectsWithNextAction(): List<ProjectWithNextAction>` — suspend, used by SnapshotBuilder

---

### 4.11 ProjectTaskDao
**Insert:** insert task  
**Update:** update title, sort_order; set is_next_action; set completed_at; set is_parallel  
**Delete:** delete completed tasks (optional cleanup, inside transaction)  
**Flow queries:** `getTasksForProject(projectId): Flow<List<ProjectTask>>`; `getNextAction(projectId): Flow<ProjectTask?>` — WHERE is_next_action = 1 LIMIT 1  
**Aggregate:** `getPendingTaskCount(projectId): Int`

---

### 4.12 MilestoneDao
**Insert:** insert milestone  
**Update:** update status, target_date, description  
**Delete:** handled by CASCADE from project  
**Flow queries:** `getMilestonesForProject(projectId): Flow<List<Milestone>>`  
**Aggregate:** none

---

### 4.13 BugDao
**Insert:** insert bug  
**Update:** update status, severity, description  
**Delete:** handled by CASCADE from project  
**Flow queries:** `getBugsForProject(projectId): Flow<List<Bug>>`; `getOpenBugsForProject(projectId): Flow<List<Bug>>`  
**Aggregate:** `getOpenBugCount(projectId): Int`

---

### 4.14 ProjectResourceDao
**Insert:** insert resource  
**Update:** update url, label  
**Delete:** delete by id; CASCADE from project  
**Flow queries:** `getResourcesForProject(projectId): Flow<List<ProjectResource>>`  
**Aggregate:** none

---

### 4.15 DailyBriefDao
**Insert:** insert daily brief  
**Update:** update llm_guidance + guidance_source + guidance_updated_at (intra-day refresh); update score_actual  
**Delete:** none  
**Flow queries:** `getBriefForDate(date): Flow<DailyBrief?>`; `getAllBriefs(): Flow<List<DailyBrief>>` (history screen, ordered by date DESC)  
**Aggregate:** `getBriefByHash(hash): DailyBrief?` — suspend, cache lookup; `getScoreHistory(limit): List<DailyBrief>` — for trend analysis

---

### 4.16 RecommendationCacheDao
**Insert/Upsert:** `upsert(cache)` — INSERT OR REPLACE on snapshot_hash  
**Update:** none (immutable once written; replace via upsert)  
**Delete:** `deleteOldestBeyondLimit(keepCount: Int)` — retains only last 7 entries  
**Queries (all suspend):** `getByHash(hash): RecommendationCache?`; `getAll(): List<RecommendationCache>` — for limit enforcement  
**Aggregate:** none

---

### 4.17 AiCallLogDao
**Insert:** insert log entry  
**Update:** none (append-only)  
**Delete:** `deleteOlderThan(epochMs: Long)` — 90-day cleanup  
**Flow queries:** `getRecentLogs(limit): Flow<List<AiCallLog>>` — diagnostics screen  
**Aggregate (all suspend):**  
- `countTodaysCalls(startOfDayEpoch): Int` — used by RateLimiter  
- `sumTodaysTokens(startOfDayEpoch): Int` — for cost estimation  
- `getSuccessRate(startEpoch, endEpoch): Float` — diagnostics screen

---

### 4.18 SettingsDao
**Insert/Upsert:** `set(key, value)` — INSERT OR REPLACE  
**Update:** covered by upsert  
**Delete:** none  
**Queries (all suspend, no Flow — settings are read synchronously at startup or via typed wrapper):** `get(key): String?`; `getAll(): List<Setting>`

---

## Section 5 — Repository Blueprint

### 5.1 SubjectRepository
**Responsibilities:** Single source of truth for subject lifecycle. Enforces archive-over-delete policy. Emits no AppEvents (subjects are not intelligence triggers).  
**Exposed methods:**  
- `getActiveSubjects(): Flow<List<Subject>>`  
- `getAllSubjects(): Flow<List<Subject>>`  
- `addSubject(name: String): Long` — returns new id  
- `renameSubject(id: Long, newName: String)`  
- `archiveSubject(id: Long)` — sets archived_at; does NOT delete  
**Depends on:** `SubjectDao`  
**Transaction boundaries:** `archiveSubject` is a single-row update — no transaction needed. Timetable import uses this DAO inside the `TimetableRepository` transaction.

---

### 5.2 TimetableRepository
**Responsibilities:** Manages timetable slot creation and the generation of class events from those slots. Owns the most complex transaction in the attendance module.  
**Exposed methods:**  
- `getSlotsForSubject(subjectId: Long): Flow<List<TimetableSlot>>`  
- `importTimetable(slots: List<TimetableSlotInput>, generateUntilEpoch: Long, replaceExisting: Boolean)` — **TRANSACTION**  
- `updateSlot(slot: TimetableSlot)`  
- `deleteSlot(slotId: Long)` — only if no class events reference it  
- `setSlotValidity(slotId: Long, validFrom: Long, validUntil: Long?)`  
**Depends on:** `TimetableSlotDao`, `ClassEventDao`, `SubjectDao`  
**Transaction boundaries:** `importTimetable` — see Section 6.

---

### 5.3 ClassEventRepository
**Responsibilities:** Manages class event status lifecycle. Emits `AttendanceMarked` event after each status write. Provides all data needed for attendance views.  
**Exposed methods:**  
- `getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEvent>>`  
- `getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEvent>>`  
- `getEventsForSubject(subjectId: Long): Flow<List<ClassEvent>>`  
- `updateStatus(eventId: Long, status: String)` — emits `AppEvent.AttendanceMarked`  
- `addExtraClass(subjectId: Long, scheduledAt: Long, endAt: Long, linkedSlotId: Long?): Long`  
- `getAttendanceSummary(subjectId: Long): Flow<AttendanceSummary>`  
- `getAttendanceSummaryAllSubjects(): List<SubjectAttendanceSummary>` — used by RecalibrationUseCase  
**Depends on:** `ClassEventDao`, `AppEventBus`  
**Transaction boundaries:** `addExtraClass` — single insert, no multi-step transaction needed.

---

### 5.4 AssignmentRepository
**Responsibilities:** Full CRUD for assignments. Emits `AssignmentStatusChanged` on every status transition. Enforces status validity.  
**Exposed methods:**  
- `getAssignmentById(id: Long): Flow<Assignment?>`  
- `getAssignmentsByStatus(status: String): Flow<List<Assignment>>`  
- `getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<Assignment>>`  
- `getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<Assignment>>`  
- `getOverdueAssignments(nowEpoch: Long): Flow<List<Assignment>>`  
- `getUrgentAssignments(withinEpoch: Long): List<Assignment>`  
- `createAssignment(input: AssignmentInput): Long`  
- `updateStatus(id: Long, newStatus: String)` — emits `AppEvent.AssignmentStatusChanged`  
- `updateDeadline(id: Long, deadline: Long)`  
- `updateReminderLead(id: Long, leadMs: Long?)`  
- `deleteAssignment(id: Long)` — called only after UI confirmation  
- `setAttachment(id: Long, uri: String?)`  
**Depends on:** `AssignmentDao`, `AppEventBus`  
**Transaction boundaries:** None required — each assignment operation is a single-row write.

---

### 5.5 CpRepository
**Responsibilities:** Manages CP profiles and contest history. Supports upsert-on-sync pattern. Emits `CpSyncCompleted` (done in the Worker layer, not here — repository is pure data).  
**Exposed methods:**  
- `getProfileByPlatform(platform: String): Flow<CpProfile?>`  
- `getAllProfiles(): Flow<List<CpProfile>>`  
- `upsertProfile(profile: CpProfile)`  
- `updateRating(profileId: Long, rating: Int, syncedAt: Long)`  
- `upsertContests(contests: List<CpContest>)` — bulk upsert  
- `getContestsByProfile(profileId: Long): Flow<List<CpContest>>`  
- `getUpcomingContests(nowEpoch: Long, lookaheadEpoch: Long): List<CpContest>`  
- `getReflectionForContest(contestId: Long): Flow<CpReflection?>`  
- `saveReflection(reflection: CpReflection)` — emits `AppEvent.ContestReflectionAdded`  
**Depends on:** `CpProfileDao`, `CpContestDao`, `CpReflectionDao`, `AppEventBus`  
**Transaction boundaries:** `upsertContests` — bulk INSERT OR REPLACE should be wrapped in a transaction for atomicity.

---

### 5.6 DsaRepository
**Responsibilities:** Knowledge Tree CRUD. Emits `DsaTopicUpdated` on every topic write.  
**Exposed methods:**  
- `getAllCategories(): Flow<List<DsaCategory>>`  
- `getTopicsByCategory(categoryId: Long): Flow<List<DsaTopic>>`  
- `getTopicsFiltered(revisionStatus: String?, confidenceLevel: Int?): Flow<List<DsaTopic>>`  
- `addCategory(name: String, sortOrder: Int): Long`  
- `renameCategory(id: Long, name: String)`  
- `deleteCategory(id: Long)` — only if no topics (RESTRICT enforced by DB)  
- `addTopic(categoryId: Long, name: String): Long`  
- `updateTopic(id: Long, confidenceLevel: Int, revisionStatus: String, notes: String?)` — emits `AppEvent.DsaTopicUpdated`  
- `deleteTopic(id: Long)`  
- `getSuggestedTopic(): DsaTopic?`  
- `isAllMastered(): Boolean`  
**Depends on:** `DsaCategoryDao`, `DsaTopicDao`, `AppEventBus`  
**Transaction boundaries:** None — all single-row writes.

---

### 5.7 ProjectRepository
**Responsibilities:** Full project lifecycle including the critical `completeNextAction` transaction. Emits `ProjectTaskCompleted` on task completion. Updates `last_activity_at` on every mutating call.  
**Exposed methods:**  
- `getActiveProjects(): Flow<List<Project>>`  
- `getArchivedProjects(): Flow<List<Project>>`  
- `getProjectById(id: Long): Flow<Project?>`  
- `createProject(input: ProjectInput): Long`  
- `updateProject(project: Project)` — updates last_activity_at  
- `archiveProject(id: Long)`  
- `getMilestonesForProject(projectId: Long): Flow<List<Milestone>>`  
- `upsertMilestone(milestone: Milestone)`  
- `getBugsForProject(projectId: Long): Flow<List<Bug>>`  
- `upsertBug(bug: Bug)`  
- `getTasksForProject(projectId: Long): Flow<List<ProjectTask>>`  
- `getNextAction(projectId: Long): Flow<ProjectTask?>`  
- `addTask(projectId: Long, title: String, sortOrder: Int): Long`  
- `setNextAction(taskId: Long, projectId: Long)` — **TRANSACTION**  
- `completeNextAction(taskId: Long, projectId: Long, newNextActionId: Long?)` — **TRANSACTION**, emits `AppEvent.ProjectTaskCompleted`  
- `setParallelMode(projectId: Long, enabled: Boolean)`  
- `getResourcesForProject(projectId: Long): Flow<List<ProjectResource>>`  
- `addResource(projectId: Long, url: String, label: String?): Long`  
- `deleteResource(resourceId: Long)`  
- `getProjectsForInactivityCheck(): List<Project>`  
- `getProjectsWithNextAction(): List<ProjectWithNextAction>`  
**Depends on:** `ProjectDao`, `ProjectTaskDao`, `MilestoneDao`, `BugDao`, `ProjectResourceDao`, `AppEventBus`  
**Transaction boundaries:** `completeNextAction`, `setNextAction` — see Section 6.

---

### 5.8 DailyBriefRepository
**Responsibilities:** Persistence of daily briefs and intra-day guidance updates. Does not perform any intelligence logic — that belongs to `IntelligenceOrchestrator`.  
**Exposed methods:**  
- `getBriefForDate(date: String): Flow<DailyBrief?>`  
- `getAllBriefs(): Flow<List<DailyBrief>>`  
- `saveBrief(brief: DailyBrief): Long`  
- `updateGuidance(date: String, guidance: String, source: String, updatedAt: Long)`  
- `updateScoreActual(date: String, scoreActual: Int)` — emits `AppEvent.DailyScoreChanged`  
- `getBriefByHash(hash: String): DailyBrief?`  
**Depends on:** `DailyBriefDao`, `AppEventBus`  
**Transaction boundaries:** None — single-row writes.

---

### 5.9 RecommendationCacheRepository
**Responsibilities:** Cache read/write with max-age enforcement and row-count limiting.  
**Exposed methods:**  
- `get(snapshotHash: String, maxAgeMs: Long): RecommendationCache?`  
- `put(snapshotHash: String, response: String, provider: String, tokenCount: Int)`  
- `evictOldEntries(keepCount: Int)`  
**Depends on:** `RecommendationCacheDao`  
**Transaction boundaries:** `put` + `evictOldEntries` — should execute atomically to keep the 7-row invariant. **TRANSACTION.**

---

### 5.10 AiCallLogRepository
**Responsibilities:** Append-only write of call log entries. Supports rate limiting and diagnostics reads.  
**Exposed methods:**  
- `logCall(entry: AiCallLog)`  
- `countTodaysCalls(startOfDayEpoch: Long): Int`  
- `sumTodaysTokens(startOfDayEpoch: Long): Int`  
- `getRecentLogs(limit: Int): Flow<List<AiCallLog>>`  
- `purgeOlderThan(epochMs: Long)` — 90-day cleanup  
**Depends on:** `AiCallLogDao`  
**Transaction boundaries:** None — append-only.

---

### 5.11 SettingsRepository
**Responsibilities:** Typed key-value store. Provides named properties for every configurable parameter. Single write path to `settings` table for all non-key data.  
**Exposed methods:** Named typed properties (get/set) for all 17 settings keys defined in design.md. `reset()` — restores all defaults. `exportAll(): Map<String, String>` — for backup.  
**Depends on:** `SettingsDao`  
**Transaction boundaries:** `reset()` — bulk writes should be wrapped in a transaction.

---

### 5.12 BackupRepository
**Responsibilities:** Orchestrates full DB export and import. Depends on all DAOs. Owns the backup restore transaction.  
**Exposed methods:**  
- `exportToJson(): BackupPayload` — serialises all tables  
- `importFromJson(payload: BackupPayload)` — **TRANSACTION** — truncate + re-insert all  
- `validatePayload(payload: BackupPayload): ValidationResult`  
**Depends on:** All DAOs  
**Transaction boundaries:** `importFromJson` — see Section 6.

---

## Section 6 — Required Transactions

### T1 — Timetable Import
**Operation:** `TimetableRepository.importTimetable()`  
**Steps inside transaction:**
1. For each slot in the input list: INSERT or UPDATE `timetable_slots`
2. For each new slot: generate `class_events` rows for every occurrence from `valid_from` to `generateUntilEpoch`
3. If `replaceExisting = true`: DELETE existing future `class_events` for affected subjects first, then insert new ones
4. Commit

**Why a transaction:** Failure after step 1 but before step 2 would leave timetable slots with no events — the attendance module would appear empty. The user would see no weekly schedule. Rollback ensures either all slots+events exist, or none.

---

### T2 — Complete Next Action
**Operation:** `ProjectRepository.completeNextAction(taskId, projectId, newNextActionId?)`  
**Steps inside transaction:**
1. SET `project_tasks.completed_at = now` WHERE id = taskId
2. SET `project_tasks.is_next_action = 0` WHERE id = taskId
3. If `newNextActionId != null`: SET `project_tasks.is_next_action = 1` WHERE id = newNextActionId
4. SET `projects.last_activity_at = now` WHERE id = projectId
5. Commit — then emit `AppEvent.ProjectTaskCompleted` **outside** the transaction

**Why a transaction:** Without atomicity, a crash between steps 1 and 3 could leave the project with a completed task still flagged as the next action, or with two next actions simultaneously. Either state corrupts the Daily Brief and the project view.

---

### T3 — Set Next Action (standalone designation)
**Operation:** `ProjectRepository.setNextAction(taskId, projectId)`  
**Steps inside transaction:**
1. SET `project_tasks.is_next_action = 0` WHERE project_id = projectId AND is_next_action = 1
2. SET `project_tasks.is_next_action = 1` WHERE id = taskId
3. SET `projects.last_activity_at = now`
4. Commit

**Why a transaction:** Two-step flag swap. A crash between steps 1 and 2 leaves the project with zero next actions, silently dropping guidance from the Daily Brief. The partial unique index prevents step 2 alone from running without step 1 completing first when `is_parallel = 0`.

---

### T4 — Backup Restore (Import)
**Operation:** `BackupRepository.importFromJson()`  
**Steps inside transaction:**
1. Validate JSON schema and referential integrity of the payload (done BEFORE opening transaction)
2. DELETE all rows from all tables (in FK-safe order: child tables first)
3. Re-insert all records from the payload in FK-safe order
4. Restore attachment files to `filesDir/attachments/`
5. Commit

**Why a transaction:** If the import fails at step 3 (e.g., malformed row, constraint violation), the user would be left with a partially-restored DB — some old data deleted, some new data inserted. Rollback ensures either the full restore succeeds or the original DB is intact. File restoration (step 4) happens after commit; if file restoration partially fails, data is intact and the user is shown a warning.

---

### T5 — Recommendation Cache Put + Evict
**Operation:** `RecommendationCacheRepository.put()`  
**Steps inside transaction:**
1. INSERT OR REPLACE into `recommendation_cache` (upsert on `snapshot_hash`)
2. DELETE oldest rows WHERE COUNT > 7 (keep most recent 7 by `created_at DESC`)
3. Commit

**Why a transaction:** Without atomicity, a crash between steps 1 and 2 could leave 8+ rows, or the eviction could delete the row just inserted. The 7-row invariant must be maintained to bound storage.

---

### T6 — Attendance Recalibration (Startup)
**Operation:** `RecalibrationUseCase` — read-only computation, but writes corrected percentages if a cached view is ever added  
**Note:** In the current V1 design, attendance percentages are computed on-the-fly from raw `class_events` — there is no cached percentage column. Recalibration is therefore a read-only verification pass. No transaction required. If a cached percentage column is added in V2, this becomes a batch UPDATE transaction.

---

### T7 — CP Contest Bulk Upsert
**Operation:** `CpRepository.upsertContests()`  
**Steps inside transaction:**
1. For each contest in the list: INSERT OR REPLACE into `cp_contests`
2. Commit

**Why a transaction:** Sync delivers a list of contests atomically. A partial insert (some contests written, sync crashes) would show an inconsistent contest history. Rollback restores to the last-known-good state until the next successful sync.

---

### T8 — Settings Reset to Defaults
**Operation:** `SettingsRepository.reset()`  
**Steps inside transaction:**
1. INSERT OR REPLACE all default key-value pairs in a single batch
2. Commit

**Why a transaction:** Partial reset (some keys updated, device loses power) would leave the app in an undefined settings state, potentially mixing old and new defaults.

---

### T9 — Subject Archive with Orphan Check
**Operation:** `SubjectRepository.archiveSubject()`  
**Note:** Archive is a single UPDATE (set `archived_at`). No transaction needed for the archive step itself. However, any future cascade to historical events is intentionally NOT performed — class_events retain their subject_id and are readable in analytics. The RESTRICT FK ensures no accidental deletion of events occurs. This is correct by design.

---

## Section 7 — Index Optimization

### Index Strategy Principles
1. Index columns that appear in WHERE clauses of frequent queries.
2. Index columns used in ORDER BY on large tables.
3. Avoid indexing columns with very low cardinality (booleans, 3-value enums) in isolation — combine with a higher-cardinality column.
4. Do not add indexes on tables with expected row counts under 50.

---

### I1 — `idx_events_subject_date` on `class_events(subject_id, scheduled_at)`
**Queries benefiting:** Attendance summary per subject (all-time), weekly view filtered by subject, bunk calculator input  
**Without index:** Full table scan on 500 events = O(n). With index: O(log n) seek + range scan.  
**Priority: CRITICAL** — this index powers the core attendance calculation.

---

### I2 — `idx_events_date` on `class_events(scheduled_at)`
**Queries benefiting:** Daily schedule view (all subjects for today), weekly view (all events for a week range), SnapshotBuilder `classes_today`  
**Without index:** Full table scan every time the weekly/daily view loads.  
**Priority: CRITICAL** — loaded on every app open.

---

### I3 — `idx_events_status` on `class_events(subject_id, status)`
**Queries benefiting:** Count by status for attendance formula (present count, absent count, etc.)  
**Without index:** Full scan + filter per subject. With index: narrow seek.  
**Priority: HIGH**

---

### I4 — `idx_assignments_deadline` on `assignments(deadline, status)`
**Queries benefiting:** Overdue query (`deadline < now AND status NOT IN (...)`), today query, this-week query, urgent assignments for SnapshotBuilder  
**Without index:** Full scan on every deadline-based filter.  
**Priority: HIGH** — queried every time the assignment list opens and every time SnapshotBuilder runs.

---

### I5 — `idx_assignments_subject` on `assignments(subject_id)`
**Queries benefiting:** Subject-filtered assignment views  
**Priority: MEDIUM** — relatively small table, but good practice.

---

### I6 — `idx_dsa_suggestion` on `dsa_topics(revision_status, confidence_level)`
**Queries benefiting:** `DsaTopicSuggester.getSuggestedTopic()` — MIN confidence among NOT_STARTED/IN_PROGRESS  
**Without index:** Full scan of 500 topics, sort, take first.  
**Priority: HIGH** — called on every SnapshotBuilder run and every free-slot notification.

---

### I7 — `idx_contests_profile_date` on `cp_contests(profile_id, contest_date DESC)`
**Queries benefiting:** Contest history list (ordered), upcoming contest check for reminders  
**Priority: MEDIUM** — small table (< 200 rows), but ordering benefits from the index.

---

### I8 — `idx_projects_active` on `projects(archived_at)`
**Queries benefiting:** Active project list, inactivity check worker  
**Priority: MEDIUM** — small table.

---

### I9 — `idx_tasks_project_next` on `project_tasks(project_id, is_next_action)`
**Queries benefiting:** `getNextAction(projectId)`, SnapshotBuilder `suggested_project_action`, `completeNextAction` transaction  
**Priority: HIGH** — called on every project detail screen load and every SnapshotBuilder run.

---

### I10 — `idx_brief_date` on `daily_briefs(date DESC)`
**Queries benefiting:** Brief history screen (ordered list), today's brief lookup  
**Priority: MEDIUM** — small table (365 rows/year max), but queried on every Daily Brief screen open.

---

### I11 — `idx_ai_log_date` on `ai_call_log(created_at DESC)`
**Queries benefiting:** Diagnostics screen (recent log), RateLimiter today-count query, 90-day purge  
**Priority: MEDIUM** — grows to ~3,650 rows/year, purge query needs this.

---

### I12 — Partial Unique Index: `idx_one_next_action` on `project_tasks(project_id) WHERE is_next_action=1 AND is_parallel=0`
**Purpose:** DB-level enforcement of the "one next action per project" rule when parallel mode is off. Replaces an application-level trigger.  
**Priority: CRITICAL** — without this, a bug in `setNextAction` could silently create two next actions, corrupting the entire project UX and Daily Brief.

---

### I13 — `UNIQUE(profile_id, contest_name, contest_date)` on `cp_contests`
**Purpose:** Prevents sync duplicates. Drives INSERT OR REPLACE upsert behaviour.  
**Priority: HIGH** — without this, repeated sync creates duplicate contest rows.

---

### Indexes NOT added (and why)
- `subjects.name` — not queried by name in code; subjects are loaded as full lists
- `settings.key` — already the PK; a separate index would be redundant
- `dsa_categories.name` — cardinality < 30, full table scan is trivially fast
- `recommendation_cache.snapshot_hash` — covered by the UNIQUE constraint
- `cp_profiles.platform` — covered by the UNIQUE constraint

---

## Section 8 — Performance Review

### Dataset Assumptions
| Table | Expected Rows (1 year) | Row Size (est.) | Total Size |
|---|---|---|---|
| subjects | 8–12 | ~100 B | < 2 KB |
| timetable_slots | 40–80 | ~150 B | < 15 KB |
| class_events | ~500 | ~120 B | ~60 KB |
| assignments | ~100 | ~300 B | ~30 KB |
| cp_profiles | 2 | ~100 B | < 1 KB |
| cp_contests | ~100 | ~150 B | ~15 KB |
| cp_reflections | ~100 | ~500 B | ~50 KB |
| dsa_categories | ~20 | ~80 B | < 2 KB |
| dsa_topics | ~500 | ~200 B | ~100 KB |
| projects | ~30 | ~300 B | ~10 KB |
| project_tasks | ~200 | ~100 B | ~20 KB |
| milestones | ~60 | ~200 B | ~12 KB |
| bugs | ~100 | ~200 B | ~20 KB |
| project_resources | ~90 | ~200 B | ~18 KB |
| daily_briefs | ~365 | ~5 KB (blobs) | ~1.8 MB |
| recommendation_cache | 7 | ~2 KB | ~14 KB |
| ai_call_log | ~1,000 | ~200 B | ~200 KB |
| settings | ~18 | ~100 B | < 2 KB |
| **Total DB** | | | **~2.4 MB** |

This is well within SQLite's comfort zone for a mobile app. No pagination concerns at V1 scale.

---

### P1 — Attendance Calculation (most frequent hot path)
**Query:** `getAttendanceSummary(subjectId)` — counts by status for one subject  
**With `idx_events_subject_date`:** Index seek to subject's rows → count by status. For 500 events spread across ~10 subjects, this is ~50 rows per subject. **Expected: < 5 ms.**  
**Risk: LOW.** No bottleneck.

---

### P2 — SnapshotBuilder (runs on morning brief + every App_Event debounce)
**Queries involved (all suspend, sequential):**
1. `getActiveSlotsOnDate` → ~5 rows, indexed
2. `getUrgentAssignments` → ~5 rows, indexed on deadline
3. `getAttendanceSummaryAllSubjects` → ~10 subjects × 50 events = ~500 row scans, indexed
4. `getSuggestedTopic` → MIN query on 500 rows, indexed on (revision_status, confidence_level)
5. `getProjectsWithNextAction` → ~10 active projects
6. `getProfilesForSnapshot` → 2 rows

**Total expected time:** 30–80 ms on a mid-range device (Pixel 3a tier).  
**Target: < 200 ms.** This is achievable but tight — query 3 is the only concern.  
**Mitigation:** Run SnapshotBuilder on an IO dispatcher. If profiling shows > 150 ms, pre-aggregate attendance counts into a `attendance_cache` column on `subjects` (V2 optimisation only if needed).

---

### P3 — Weekly View / Calendar View render
**Query:** `getEventsForWeek` — 7 days × ~5 events/day = ~35 rows  
**With `idx_events_date`:** Index range scan. **Expected: < 10 ms.**  
**Risk: LOW.**

---

### P4 — RecalibrationUseCase at startup
**Query:** `getAttendanceSummaryAllSubjects` — scans all class_events  
**For 500 events:** Full indexed scan. **Expected: < 30 ms.**  
**Risk: LOW.** Runs once at startup on IO dispatcher, not on main thread.

---

### P5 — DSA Knowledge Tree (500 topics)
**Query:** `getTopicsByCategory` repeated for each category, OR full list with in-memory filter  
**For 500 topics:** Full table read = ~100 KB. LazyColumn renders only visible rows.  
**Risk: LOW** for display. Filter is in-memory (< 1 ms). No DB concern.

---

### P6 — Daily Brief History Screen
**Query:** `getAllBriefs()` — 365 rows, each with ~5 KB `json_snapshot` blob  
**Total payload:** ~1.8 MB loaded into memory if all rows returned at once.  
**Risk: MEDIUM.** Loading all 365 rows with their full JSON blobs for a list screen is wasteful.  
**Fix:** Define a lightweight `DailyBriefSummary` projection DAO query that returns only `id, date, score_target, score_actual, guidance_source` (no blobs). Load full blobs only when a specific brief is tapped. This is a DAO-level projection — no schema change needed.

---

### P7 — AI Call Log Diagnostics Screen
**Table size:** ~1,000 rows after 1 year.  
**Query:** `getRecentLogs(limit=50)` with `idx_ai_log_date` — fast.  
**Risk: LOW.** The 90-day purge keeps this table from growing unboundedly.

---

### P8 — Notification Rescheduler at Startup
**Queries:** upcoming class_events (date range), pending assignments, upcoming contests  
**Total rows scanned:** ~100 rows across 3 queries.  
**Risk: LOW.** Must complete within 30 seconds — easily met in < 200 ms.

---

### P9 — Backup Export
**Operation:** Serialise all tables to JSON, embed attachments as Base64  
**Data volume:** ~2.4 MB DB + attachment files  
**Risk: MEDIUM** if the student has many large attachments. The 10 MB per-file cap and the file-by-reference fallback mitigate this.  
**Mitigation:** Run export on an IO dispatcher with progress indication. Do not block the UI thread.

---

### P10 — `inactivity_threshold_days` worker query
**Query:** `getActiveProjectsForInactivityCheck()` — scans ~30 active projects  
**Risk: LOW.** Trivially fast.

---

### Summary of Bottlenecks
| Risk | Severity | Fix |
|---|---|---|
| SnapshotBuilder > 200 ms | MEDIUM | IO dispatcher; attendance pre-agg if needed in V2 |
| Brief history loads full blobs | MEDIUM | Projection DAO query (fix before coding) |
| Backup export with large attachments | MEDIUM | Progress indicator + IO dispatcher |
| All others | LOW | No action needed at V1 scale |

---

## Section 9 — Migration Strategy

### Philosophy
- Every schema change ships as a numbered `Migration` object in Room.
- No destructive migrations (DROP TABLE, DROP COLUMN) unless a column is provably unused and the app has never shipped to users.
- Additive migrations (ADD COLUMN with DEFAULT, CREATE TABLE, CREATE INDEX) are always safe.
- Migration SQL is written and reviewed before any feature code that depends on the new schema.
- Each migration is integration-tested using `MigrationTestHelper` before release.

---

### Version 1 — Initial Schema (baseline)
**Tables created:** subjects, timetable_slots, class_events, assignments, cp_profiles, cp_contests, cp_reflections, dsa_categories, dsa_topics, projects, milestones, bugs, project_tasks, project_resources, daily_briefs, recommendation_cache, ai_call_log, settings  
**All indexes created in version 1 migration SQL.**  
**Partial unique index created:** `idx_one_next_action`  
**CHECK constraints added** to all enum columns at table creation time.

---

### Version 2 — Bug Fixes Identified in This Blueprint
**Changes from schema review (Section 2.2):**
- ADD COLUMN `updated_at INTEGER NOT NULL DEFAULT 0` to `assignments`
- ADD COLUMN `updated_at INTEGER NOT NULL DEFAULT 0` to `dsa_topics`
- ADD UNIQUE CONSTRAINT `(profile_id, contest_name, contest_date)` to `cp_contests` (implemented as `CREATE UNIQUE INDEX`)
- ALTER `cp_profiles.current_rating` to allow NULL (Room handles nullable Int with no schema change — this is an entity-level annotation change, no migration SQL needed)

**Note:** Since V1 has not shipped to users yet, these corrections should be incorporated directly into V1 rather than creating a V2 migration. The V2 slot is reserved for the first post-launch schema change.

---

### Version 2 (post-launch) — Reserved for first real change
**Likely candidate:** Add `attendance_cache` column to `subjects` if SnapshotBuilder performance requires pre-aggregation.  
```sql
ALTER TABLE subjects ADD COLUMN cached_attendance_pct REAL;
ALTER TABLE subjects ADD COLUMN cache_updated_at INTEGER;
```
Migration: additive, safe, no data loss.

---

### Version 3+ — Anticipated future changes
| Change | Migration type | Risk |
|---|---|---|
| Add Finance module tables | CREATE TABLE | Safe |
| Add Habit module tables | CREATE TABLE | Safe |
| Add Gemini/OpenAI provider column to recommendation_cache | ALTER TABLE ADD COLUMN with DEFAULT | Safe |
| Add conversation_id to ai_call_log for V2 multi-turn | ALTER TABLE ADD COLUMN | Safe |
| Add on-device LLM flag to settings | INSERT into settings | Safe |
| Rename timetable_slots columns for clarity | Requires data migration | Medium risk — test carefully |

---

### Migration Testing Protocol
1. Keep `v1.db` golden file in `assets/databases/` test directory.
2. Run `MigrationTestHelper.runMigrationsAndValidate()` for every version pair: 1→2, 1→3, 2→3, etc.
3. Block merge if any migration test fails.
4. Never use `fallbackToDestructiveMigration()` in production build. Use it only in debug builds during active development, and remove before first release.

---

## Section 10 — Backup Compatibility Verification

### 10.1 Export Coverage
| Data | Exported? | Method | Notes |
|---|---|---|---|
| subjects | YES | JSON array | All rows including archived |
| timetable_slots | YES | JSON array | |
| class_events | YES | JSON array | All statuses |
| assignments | YES | JSON array | attachment_uri stored as relative path |
| File attachments | YES (≤10 MB) | Base64 embedded in JSON | Larger files: `external_path` reference + warning |
| cp_profiles | YES | JSON array | Does NOT include API key (stored in EncryptedSharedPreferences) |
| cp_contests | YES | JSON array | |
| cp_reflections | YES | JSON array | |
| dsa_categories | YES | JSON array | |
| dsa_topics | YES | JSON array | |
| projects | YES | JSON array | |
| milestones | YES | JSON array | |
| bugs | YES | JSON array | |
| project_tasks | YES | JSON array | |
| project_resources | YES | JSON array | |
| daily_briefs | YES | JSON array | Includes llm_guidance and brief_json blobs |
| recommendation_cache | YES | JSON array | Bounded at 7 rows — small |
| ai_call_log | YES | JSON array | May be large (1,000 rows); export includes all 90-day window |
| settings | YES | JSON object (key-value map) | All keys EXCEPT API key |
| DeepSeek API key | **NO** | Stored in EncryptedSharedPreferences | Intentional — keys must be re-entered after restore |

---

### 10.2 Import Safety
1. **Schema validation first** — payload is validated against expected table structure before any DB write begins.
2. **Referential order** — insert order must respect FKs: subjects → timetable_slots → class_events → assignments; dsa_categories → dsa_topics; cp_profiles → cp_contests → cp_reflections; projects → milestones/bugs/tasks/resources; then intelligence tables.
3. **Conflict on duplicate keys** — `settings` uses INSERT OR REPLACE; all other tables truncate first, so no conflicts.
4. **Attachment restoration** — after DB transaction commits, file bytes are decoded from Base64 and written to `filesDir/attachments/`. If a file write fails, the user sees a per-file warning, but DB data is safe.
5. **Partial payload** — if a section is missing (e.g., backup from an older version that predates `ai_call_log`), the import treats the missing section as empty and skips it. This ensures forward-compatible imports.

---

### 10.3 Version Compatibility Issue Found
**Problem:** If a user exports from V1 and imports on V2 (which added `updated_at` to `assignments`), the backup JSON has no `updated_at` field. The import transaction would fail on NOT NULL constraint.  
**Fix:** In `ImportUseCase`, when deserialising each entity, use `updated_at ?: 0L` as the default for missing fields. This must be documented as a required pattern for every new NOT NULL column added in future migrations.

---

### 10.4 AI Cache and Logs in Backup
- `recommendation_cache` (7 rows) and `ai_call_log` (up to 1,000 rows) are included in the backup.
- This is correct — the diagnostics screen history should survive a device transfer.
- `ai_call_log` export size: 1,000 rows × ~200 B = ~200 KB. Acceptable in the JSON backup.
- `daily_briefs` blobs (`json_snapshot`, `brief_json`, `llm_guidance`): 365 × ~5 KB = ~1.8 MB in the backup. This is the largest single contributor to backup file size. Acceptable for a manual user-initiated export.

---

### 10.5 Verdict
Backup and restore are structurally sound with two required fixes:
1. **Add null-safe defaults in ImportUseCase** for all NOT NULL columns (handles cross-version imports).
2. **Exclude API key from export** — already designed correctly; verify in task 10.4(e).

---

## Section 11 — Production Risks

### R1 — Denormalised `subject_id` on `class_events` can desync
**Risk:** `class_events.subject_id` is a copy of `timetable_slots.subject_id`. If a subject is ever re-linked to a different slot (edge case), the denormalised column would be stale.  
**Severity:** MEDIUM  
**Fix:** In `TimetableRepository.importTimetable()` transaction, always derive `class_events.subject_id` from the slot being processed, never from user input. Add a note in the repository that `subject_id` on `class_events` is write-once and must never be updated independently.

---

### R2 — `is_next_action` flag race condition under rapid UI taps
**Risk:** If the user taps "Complete" and "Set New Next Action" in rapid succession before the first transaction commits (e.g., UI allows the second tap while the coroutine is still running), two transactions could execute against stale state.  
**Severity:** MEDIUM  
**Fix:** In the ViewModel layer (not the repository), disable the action buttons while any project-task coroutine is in flight using a `isLoading: Boolean` state. The partial unique index provides a DB-level last line of defence.

---

### R3 — Duplicate App_Events causing multiple LLM calls
**Risk:** If a repository emits `AppEvent.AttendanceMarked` and the ViewModel also emits it for the same action (double-emit bug), the IntelligenceOrchestrator's 30-second debounce absorbs it — but the snapshot could be built twice within the window.  
**Severity:** LOW  
**Fix:** Enforce the rule that `AppEventBus.emit()` is called exactly once per user action, in the repository layer only, never in the ViewModel or Use Case layer. Document this as a hard architectural rule in CONTRIBUTING.md.

---

### R4 — `recommendation_cache` hash collision (theoretical)
**Risk:** SHA-256 collision between two different snapshots could return a wrong cached response.  
**Severity:** NEGLIGIBLE  
**Fix:** None needed. SHA-256 collision probability is astronomically small. Acceptable risk.

---

### R5 — Orphaned attachment files after assignment deletion
**Risk:** When an assignment is deleted, `AssignmentDao.delete()` removes the DB row. The file at `filesDir/attachments/<uuid>.<ext>` is not automatically deleted because Room has no file system awareness.  
**Severity:** MEDIUM (storage leak, not data corruption)  
**Fix:** In `AssignmentRepository.deleteAssignment()`, after the DB delete, also delete the file at the stored `attachment_uri` path. Wrap both in a try-catch — a missing file is not fatal. Add a maintenance job in `BackupRepository` that scans `filesDir/attachments/` and removes any file not referenced by any `assignments.attachment_uri` (run once on App startup, async).

---

### R6 — `ai_call_log` unbounded growth if purge is not triggered
**Risk:** The 90-day purge is only triggered by `RateLimiter` or explicitly. If the student disables AI entirely, the purge never runs, and old log entries accumulate indefinitely.  
**Severity:** LOW (small rows, ~200 B each)  
**Fix:** Call `AiCallLogRepository.purgeOlderThan()` from `App.onCreate()` on an IO dispatcher, unconditionally. Takes < 5 ms.

---

### R7 — `daily_briefs.date` timezone ambiguity
**Risk:** `date` is stored as "YYYY-MM-DD" string. If the device timezone changes (e.g., student travels internationally), the app could try to create a second brief for the "same" calendar day in the new timezone, violating the UNIQUE constraint.  
**Severity:** LOW (rare case)  
**Fix:** Always derive the `date` key using the device's current timezone at the time of brief generation. In `DailyBriefWorker`, use `LocalDate.now(ZoneId.systemDefault()).toString()`. If a unique constraint violation occurs on insert, do an UPDATE instead (upsert). Document this in `DailyBriefRepository.saveBrief()`.

---

### R8 — Stale `last_activity_at` if `ProjectRepository` update path is missed
**Risk:** `last_activity_at` must be updated on every mutating project operation. If a new write path is added (e.g., a bulk-complete bugs feature in V2) without updating this column, the inactivity worker fires false reminders.  
**Severity:** MEDIUM  
**Fix:** Create a private extension function `updateActivity(projectId)` inside `ProjectRepository` and call it at the end of every mutating method. Write an integration test that calls each mutating method and asserts `last_activity_at` was updated.

---

### R9 — Backup import truncates `recommendation_cache` and `ai_call_log`
**Risk:** A student restores a backup from 6 months ago. Their current `ai_call_log` (cost history) and `recommendation_cache` are wiped and replaced with the 6-month-old data.  
**Severity:** LOW (expected behaviour, but surprising)  
**Fix:** In the import confirmation dialog, explicitly tell the user: "All AI history and cache will also be replaced." No schema change needed — just a UX disclosure. Alternatively, offer an import option that preserves intelligence tables (V2 enhancement).

---

### R10 — `class_events` generation horizon is unbounded in the import
**Risk:** `TimetableRepository.importTimetable(generateUntilEpoch)` generates events up to a caller-specified date. If the caller passes `Long.MAX_VALUE`, millions of rows could be generated, crashing the transaction or filling storage.  
**Severity:** HIGH if not guarded  
**Fix:** Enforce a maximum generation horizon of 365 days from `valid_from` inside `TimetableRepository`, regardless of the caller's `generateUntilEpoch`. Document this limit. Add a validation assertion at the start of the `importTimetable` function.

---

## Section 12 — Final Verdict

### Production Readiness Score: 8.5 / 10

**What earns the 8.5:**
- Architecture is clean and well-separated. No module coupling violations.
- All critical transactions are identified and correctly scoped.
- Offline-first is genuine — LLM is additive, not load-bearing.
- Index coverage is thorough for the expected query patterns.
- Schema is normalised with intentional, documented denormalisations.
- Performance at V1 dataset scale is comfortably within mobile SQLite limits.
- Security posture (API key in EncryptedSharedPreferences, no key in backup) is correct.
- Event-driven architecture is appropriately minimal — no over-engineering.

**What costs the 1.5:**
- Four schema fixes are required before coding begins (updated_at columns, cp_contests uniqueness, cp_profiles nullable rating). These are small but real gaps.
- The class_events generation horizon guard (R10) is a HIGH severity risk that must be fixed in `TimetableRepository` before it is written.
- The brief history screen blob loading issue (P6) requires a DAO projection query — a minor but necessary fix.
- Attachment orphan cleanup (R5) needs to be added to `AssignmentRepository`.

---

### Metrics Summary

| Metric | Value |
|---|---|
| Estimated Room Tables | 18 |
| Estimated DAOs | 18 |
| Estimated Repositories | 12 |
| Required Transactions | 9 (8 write, 1 read-verify) |
| Required Indexes | 13 (10 explicit + 3 UNIQUE constraints) |
| Total estimated DB size (1 year) | ~2.4 MB |
| Largest single table | daily_briefs (~1.8 MB — blobs) |
| Performance bottleneck risk | MEDIUM (SnapshotBuilder, brief history) |
| Migration complexity | LOW (additive only at V1) |
| Backup compatibility | GOOD (1 fix needed: null-safe defaults) |
| Expected maintenance difficulty | LOW–MEDIUM |
| Expected scalability | HIGH for 1–3 years of student data |

---

### Database Complexity

**Overall complexity: MODERATE.** The schema is larger than a typical personal productivity app but well within the range of a single experienced developer. The intelligence module tables (daily_briefs, recommendation_cache, ai_call_log) add surface area but no fundamental complexity — they are append/upsert tables with no join requirements. The most complex data model piece is the attendance module (subjects → slots → events with the denormalised subject_id and the Extra_Class/linked_slot pattern). The project module's `is_next_action` flag with the partial unique index is the second most complex constraint to maintain correctly.

---

### Required Fixes Before Coding Begins (Blocking)

1. **Add `updated_at` to `assignments` and `dsa_topics`** — incorporate into V1 schema, not a V2 migration.
2. **Add `UNIQUE(profile_id, contest_name, contest_date)` to `cp_contests`** — required for safe sync upsert.
3. **Make `cp_profiles.current_rating` nullable** — a new profile has no rating until first sync.
4. **Add generation horizon guard (max 365 days) to `TimetableRepository.importTimetable()`** — HIGH severity production risk if unchecked.
5. **Add DAO projection query for brief history screen** — `DailyBriefSummary` without blob columns — prevents loading 1.8 MB into memory for a list screen.

---

### Required Fixes Before Coding Begins (Non-Blocking but Strongly Recommended)

6. **Add CHECK constraints to all enum TEXT columns** — enforces domain validity at DB level.
7. **Add `UNIQUE(subject_id, day_of_week, start_time, week_parity, valid_from)` to `timetable_slots`** — prevents duplicate slot import.
8. **Add orphaned attachment cleanup in `AssignmentRepository.deleteAssignment()`.**
9. **Add unconditional 90-day `ai_call_log` purge in `App.onCreate()`.**
10. **Use upsert (not insert) in `DailyBriefRepository.saveBrief()`** to handle timezone edge case gracefully.

---

### Is the Backend Architecture Ready for Implementation?

**YES, with the 5 blocking fixes above resolved first.**

The architecture is sound, the module boundaries are correctly drawn, the transaction model is complete, and the index coverage is appropriate. The identified risks are all fixable with targeted one-to-three line changes or simple additional queries — none require architectural changes. Once the blocking fixes are incorporated into the V1 schema definition (task 1.1–1.5 in tasks.md), coding can begin with high confidence that the data layer will not require structural rework.

The most important discipline to maintain during implementation is: **all attendance calculations computed from raw `class_events` rows, all intelligence data derived from Local_Database via SnapshotBuilder only, and all LLM output treated as display text never parsed for structured values.** These three rules, if never violated, keep the data model stable regardless of what the LLM does.
