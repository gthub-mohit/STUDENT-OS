# Requirements Document — Student OS

## Introduction

Student OS is an offline-first Android application that serves as a personal productivity operating system for engineering students. Its core mission is to reduce decision fatigue by automating routine tracking, surfacing only what matters, and telling the student what deserves attention next — rather than acting as a passive data dump.

The app ships with six tightly integrated modules: Attendance Engine, Assignment Engine, Coding Engine, Project Engine, Daily Intelligence Engine, and a Notification System. Every module is designed to be feature-isolated so that future modules (ML recommendations, Finance, Habit tracking, Skill XP) can be added without restructuring the existing system.

**Scope of this document:** Version 1 requirements only. Deferred items are noted explicitly.

---

## Glossary

- **App**: The Student OS Android application.
- **Student**: The human user of the App.
- **Timetable**: A recurring weekly schedule of classes uploaded by the Student.
- **Class_Event**: A single scheduled occurrence of a class, generated from the Timetable.
- **Attendance_Engine**: The module responsible for Timetable import, Class_Event lifecycle, and attendance calculations.
- **Assignment_Engine**: The module responsible for assignment tracking, deadlines, and reminders.
- **Coding_Engine**: The module responsible for competitive programming profile sync and DSA knowledge tracking.
- **Project_Engine**: The module responsible for project management, milestones, bugs, and next-action tracking.
- **Daily_Intelligence_Engine**: The module that composes a personalized daily briefing for the Student each morning and responds to App_Events throughout the day.
- **Notification_Manager**: The subsystem responsible for scheduling and delivering all local push notifications.
- **OCR_Processor**: The on-device component that extracts structured text from timetable images.
- **Local_Database**: The on-device SQLite/Room database that is the single source of truth for all App data.
- **Sync_Service**: The background service that fetches data from external APIs (CodeChef, Codeforces) and writes results to the Local_Database.
- **Attendance_Threshold**: The minimum attendance percentage configured by the Student (default 75%).
- **Bunk_Calculator**: The sub-component of Attendance_Engine that computes how many classes the Student can miss while staying above the Attendance_Threshold.
- **Knowledge_Tree**: The hierarchical topic structure within Coding_Engine used to track DSA topic mastery.
- **Next_Immediate_Action**: The single active task designated for a project at any point in time.
- **Daily_Brief**: The structured morning summary produced by Daily_Intelligence_Engine.
- **Confidence_Level**: A Student-assigned integer (1–5) representing mastery of a DSA topic.
- **Free_Slot**: A time block in the Student's schedule with no Class_Event, assignment deadline, or contest.
- **CP_Profile**: The Student's external competitive programming profile on CodeChef or Codeforces.
- **App_Event**: An internal domain event emitted whenever a meaningful state change occurs in any module (e.g., attendance marked, assignment completed, CP sync finished). App_Events are the trigger for snapshot updates and LLM recommendations.
- **Intelligence_Snapshot**: The compressed, summarised JSON representation of the Student's current academic state, derived from the Local_Database. This is the sole input to the LLM and the deterministic fallback engine.
- **LLM_Provider**: The abstraction layer through which the Daily_Intelligence_Engine communicates with an external large language model. The rest of the App never references a specific provider directly.
- **Deterministic_Fallback**: The on-device rule-based recommendation engine that produces a Daily_Brief and intra-day guidance when no internet is available or the LLM call fails.
- **Recommendation_Cache**: The Local_Database record storing the most recent LLM-generated guidance alongside the snapshot hash that produced it, enabling display of stale recommendations when the LLM is unreachable.

---

## Requirements

### Requirement 1: Timetable Import via OCR

**User Story:** As a Student, I want to import my timetable by photographing or uploading a screenshot, so that I do not have to enter every class manually.

#### Acceptance Criteria

1. WHEN the Student provides an image file or camera capture, THE OCR_Processor SHALL extract a structured timetable representation containing day, time slot, subject name, and location fields.
2. WHEN OCR extraction completes, THE Attendance_Engine SHALL display the extracted timetable in an editable preview screen before any data is persisted.
3. WHILE the Student is reviewing the OCR preview, THE Attendance_Engine SHALL allow the Student to edit, add, or delete any extracted row.
4. WHEN the Student confirms the timetable, THE Attendance_Engine SHALL persist the validated timetable to the Local_Database.
5. IF the OCR_Processor cannot extract a recognisable timetable structure from the provided image, THEN THE Attendance_Engine SHALL display a descriptive error message and allow the Student to retry or enter the timetable manually.
6. THE Attendance_Engine SHALL support timetable import for any number of subjects without an upper bound enforced by the App.
7. WHEN a timetable is imported, THE Attendance_Engine SHALL NOT overwrite existing Class_Events unless the Student explicitly confirms a replacement operation.

---

### Requirement 2: Weekly Schedule Generation

**User Story:** As a Student, I want my confirmed timetable to automatically generate a weekly repeating schedule, so that I do not have to log each class individually.

#### Acceptance Criteria

1. WHEN a timetable is confirmed by the Student, THE Attendance_Engine SHALL generate Class_Events for each timetable slot repeating on the corresponding weekday.
2. THE Attendance_Engine SHALL assign each Class_Event a default status of `Unmarked` at creation time.
3. WHEN the Student navigates to the Weekly View, THE Attendance_Engine SHALL display all Class_Events for the current week grouped by day.
4. THE Attendance_Engine SHALL support Calendar View, showing Class_Events on their respective calendar dates.
5. WHEN a new academic period begins, THE Attendance_Engine SHALL allow the Student to define a custom date range for timetable applicability without requiring a full reimport.

---

### Requirement 3: Class Event Status Management

**User Story:** As a Student, I want to mark each class with an accurate status, so that my attendance record reflects reality.

#### Acceptance Criteria

1. WHEN the Student selects a Class_Event, THE Attendance_Engine SHALL allow the Student to mark it as one of: `Present`, `Absent`, `Cancelled`, `Holiday`, or `Extra_Class`.
2. WHEN a Class_Event is marked `Cancelled` or `Holiday`, THE Attendance_Engine SHALL exclude that event from both the numerator and denominator when computing attendance percentage.
3. WHEN a Class_Event is marked `Extra_Class`, THE Attendance_Engine SHALL add that event to the denominator and allow it to be marked `Present` or `Absent`.
4. WHEN the Student adds an Extra_Class for a subject, THE Attendance_Engine SHALL record it as a one-time event without modifying the recurring Timetable.
5. WHEN the Student records a shifted class (original slot moved to a different time), THE Attendance_Engine SHALL allow the Student to log the new time slot as an Extra_Class linked to the original Timetable entry.
6. IF the Student attempts to mark a future Class_Event as `Present` or `Absent`, THEN THE Attendance_Engine SHALL display a confirmation prompt before saving.

---

### Requirement 4: Attendance Calculation and Analytics

**User Story:** As a Student, I want accurate, real-time attendance percentages and analytics, so that I can make informed decisions about attending classes.

#### Acceptance Criteria

1. WHEN any Class_Event status changes, THE Attendance_Engine SHALL recalculate the attendance percentage for the affected subject within 500 ms.
2. THE Attendance_Engine SHALL calculate attendance percentage as: `(Present + Extra_Class_Present) / (Total - Cancelled - Holiday) × 100`, rounded to two decimal places.
3. THE Attendance_Engine SHALL display per-subject attendance percentage in every relevant view.
4. THE Attendance_Engine SHALL allow the Student to configure an Attendance_Threshold value between 1 and 100, defaulting to 75.
5. WHEN a subject's attendance percentage falls below the Attendance_Threshold, THE Attendance_Engine SHALL visually highlight that subject in all list and calendar views.
6. THE Attendance_Engine SHALL provide subject-level analytics showing: total classes held, total present, total absent, total cancelled, and current percentage.

---

### Requirement 5: Bunk Calculator

**User Story:** As a Student, I want to know how many classes I can safely skip, so that I can make informed attendance decisions without manual calculation.

#### Acceptance Criteria

1. WHEN the Student opens the Bunk_Calculator for a subject, THE Bunk_Calculator SHALL display the maximum number of additional absences the Student can take while remaining at or above the Attendance_Threshold.
2. WHEN the Student's current attendance is already below the Attendance_Threshold, THE Bunk_Calculator SHALL display the minimum number of consecutive classes the Student must attend to reach the Attendance_Threshold.
3. THE Bunk_Calculator SHALL recompute its output in real time whenever attendance data or the Attendance_Threshold changes.
4. THE Bunk_Calculator SHALL clearly label the result with the Attendance_Threshold value used in the calculation so the Student can verify the basis.
5. FOR ALL valid combinations of classes held, classes present, and Attendance_Threshold values, the Bunk_Calculator result SHALL be mathematically consistent with the attendance formula defined in Requirement 4, Criterion 2.

---

### Requirement 6: Assignment Lifecycle Management

**User Story:** As a Student, I want to create and track assignments with full context, so that nothing falls through the cracks near deadlines.

#### Acceptance Criteria

1. THE Assignment_Engine SHALL allow the Student to create an assignment record containing: Subject, Deadline (date and time), Priority (High / Medium / Low), Notes (free text), file Attachment, and Status.
2. WHEN the Student creates an assignment, THE Assignment_Engine SHALL set the default Status to `Pending`.
3. THE Assignment_Engine SHALL support the following Status values: `Pending`, `In_Progress`, `Submitted`, `Completed`.
4. THE Assignment_Engine SHALL provide the following filtered views: `Today` (deadline today), `This_Week` (deadline within 7 days), `Overdue` (deadline passed and status is not `Completed` or `Submitted`), and `Completed`.
5. WHEN the current date and time passes an assignment's Deadline and the Status is not `Submitted` or `Completed`, THE Assignment_Engine SHALL automatically move that assignment to the `Overdue` view.
6. THE Assignment_Engine SHALL allow the Student to attach one file per assignment, stored in the Local_Database or device file system reference.
7. IF the Student attempts to delete an assignment with Status `Pending` or `In_Progress`, THEN THE Assignment_Engine SHALL display a confirmation prompt before deletion.

---

### Requirement 7: Assignment Reminders

**User Story:** As a Student, I want timely reminders for upcoming assignment deadlines, so that I am never caught unprepared.

#### Acceptance Criteria

1. WHEN an assignment is created or its Deadline is updated, THE Notification_Manager SHALL schedule a local push notification at a Student-configured lead time before the Deadline (default 24 hours).
2. THE Notification_Manager SHALL allow the Student to configure per-assignment reminder lead times independently of the global default.
3. WHEN an assignment's Status changes to `Submitted` or `Completed`, THE Notification_Manager SHALL cancel any pending reminder notifications for that assignment.
4. IF the device is offline or restarted, THE Notification_Manager SHALL reschedule all pending notifications on the next App launch.

---

### Requirement 8: Competitive Programming Profile Sync

**User Story:** As a Student, I want my CodeChef and Codeforces ratings and contest history to sync automatically, so that I can review my CP progress without switching apps.

#### Acceptance Criteria

1. THE Coding_Engine SHALL allow the Student to configure one CodeChef handle and one Codeforces handle independently.
2. WHEN valid handles are configured, THE Sync_Service SHALL fetch the Student's current rating, contest history, and recent solved problems from the respective platform APIs.
3. THE Sync_Service SHALL attempt to refresh CP_Profile data at a Student-configured interval (minimum 15 minutes, default 6 hours).
4. WHILE the device has no internet connectivity, THE Coding_Engine SHALL display the most recently synced CP_Profile data from the Local_Database with a visible "last synced" timestamp.
5. IF a platform API returns an error or the handle is invalid, THEN THE Coding_Engine SHALL display a descriptive error alongside the last known data without clearing existing records.
6. THE Coding_Engine SHALL display per-contest results including: contest name, date, rank, rating change, and problems solved.
7. WHEN the Student completes a contest, THE Coding_Engine SHALL prompt the Student to submit a Contest Reflection containing: what went wrong, what to revise, and a self-rating (1–5).
8. THE Coding_Engine SHALL store Contest Reflections in the Local_Database and display them alongside their associated contest result.

---

### Requirement 9: DSA Knowledge Tree

**User Story:** As a Student, I want to track my DSA topic mastery in a structured tree, so that I can identify weak areas and plan revision efficiently.

#### Acceptance Criteria

1. THE Coding_Engine SHALL organise DSA topics in a hierarchical Knowledge_Tree with at least two levels: Category (e.g., "Trees") and Topic (e.g., "Segment Tree").
2. THE Coding_Engine SHALL allow the Student to add, rename, and delete categories and topics at any time.
3. WHEN the Student selects a topic, THE Coding_Engine SHALL allow the Student to set a Confidence_Level (integer 1–5), add free-text Notes, and set a Revision_Status of `Not_Started`, `In_Progress`, or `Revised`.
4. THE Coding_Engine SHALL display a visual indicator per topic reflecting the current Confidence_Level so weak topics are immediately visible.
5. THE Coding_Engine SHALL allow the Student to filter the Knowledge_Tree by Revision_Status or Confidence_Level.
6. WHEN a Free_Slot is detected by the Daily_Intelligence_Engine, THE Coding_Engine SHALL provide a suggested topic based on lowest Confidence_Level among topics with Revision_Status `Not_Started` or `In_Progress`.

---

### Requirement 10: Project Engine

**User Story:** As a Student, I want to manage personal and academic projects with milestones, bugs, and a clear next action, so that I maintain momentum without being overwhelmed.

#### Acceptance Criteria

1. THE Project_Engine SHALL allow the Student to create a project record containing: Title, Overview (description), GitHub repository URL, and Notes.
2. THE Project_Engine SHALL allow the Student to add Milestones to a project, each containing: Title, Description, Target_Date, and Status (`Pending`, `In_Progress`, `Done`).
3. THE Project_Engine SHALL allow the Student to log Bugs against a project, each containing: Description, Severity (`Low`, `Medium`, `High`), and Status (`Open`, `Resolved`).
4. THE Project_Engine SHALL allow the Student to add Resource links (URLs with optional labels) to a project.
5. EACH project SHALL have exactly one designated Next_Immediate_Action at any point in time when the project is active.
6. WHEN the Student marks the current Next_Immediate_Action as complete, THE Project_Engine SHALL prompt the Student to designate a new Next_Immediate_Action from the remaining tasks before returning to the project view.
7. THE Project_Engine SHALL display all future tasks for a project in a visible but visually de-emphasised list, distinct from the Next_Immediate_Action.
8. IF a project has had no status change or Next_Immediate_Action update for a Student-configured period (default 7 days), THEN THE Notification_Manager SHALL send an "Inactive Project" reminder notification.
9. WHEN the Student archives a project, THE Project_Engine SHALL retain all project data in the Local_Database in a read-only state.

---

### Requirement 11: Daily Intelligence Engine — Brief Generation

**User Story:** As a Student, I want a personalised daily briefing every morning that understands my context and motivates me, so that I immediately know what deserves my attention without checking every module separately.

#### Acceptance Criteria

1. THE Daily_Intelligence_Engine SHALL generate a Daily_Brief once per day at a Student-configured time (default 07:00 local time).
2. THE Daily_Brief SHALL include: today's Class_Events with times, subjects with attendance below the Attendance_Threshold, assignments due today or overdue, identified Free_Slots in today's schedule, a suggested DSA topic from the Knowledge_Tree, a suggested Next_Immediate_Action from an active project, a daily score target, and personalised guidance text.
3. WHEN generating the Daily_Brief, THE Daily_Intelligence_Engine SHALL build an Intelligence_Snapshot from the Local_Database. The snapshot is the sole input to both the LLM_Provider and the Deterministic_Fallback engine.
4. WHEN the device has internet connectivity and AI is enabled in settings, THE Daily_Intelligence_Engine SHALL send the Intelligence_Snapshot to the configured LLM_Provider and use the response to populate the human-friendly guidance sections of the Daily_Brief.
5. WHEN the device has no internet connectivity, or when the LLM_Provider call fails for any reason, THE Daily_Intelligence_Engine SHALL immediately fall back to the Deterministic_Fallback engine and generate the Daily_Brief fully on-device. The Student SHALL NOT be shown an error or an empty brief in this case.
6. WHEN the Daily_Brief is generated, THE Notification_Manager SHALL deliver a local push notification with a summary line; tapping the notification SHALL open the full Daily_Brief within the App.
7. THE Daily_Intelligence_Engine SHALL persist each Daily_Brief in the Local_Database so the Student can review past briefs.
8. WHEN no active project has a Next_Immediate_Action defined, THE Daily_Brief SHALL indicate that no project task is available rather than omitting the section.
9. WHEN no DSA topic qualifies for suggestion (all topics at Confidence_Level 5 and Revision_Status `Revised`), THE Daily_Brief SHALL indicate mastery and suggest the Student add new topics.
10. THE LLM_Provider SHALL NEVER be the source of truth for any calculated value. Attendance percentages, deadlines, scores, ratings, and all other structured data are always derived from the Local_Database, not from LLM output.
11. THE Daily_Intelligence_Engine SHALL cache the most recent LLM response in the Local_Database (Recommendation_Cache). If the current Intelligence_Snapshot is identical to the cached snapshot (same hash), the engine SHALL reuse the cached response without making a new API call.

---

### Requirement 11a: Event-Driven Intra-Day Recommendations

**User Story:** As a Student, I want the app to update my guidance automatically when something important changes during the day — not just at 07:00 — so that recommendations stay relevant as my situation evolves.

#### Acceptance Criteria

1. WHEN any of the following App_Events occurs, THE Daily_Intelligence_Engine SHALL rebuild the Intelligence_Snapshot and trigger a recommendation update: `AttendanceMarked`, `AssignmentStatusChanged`, `ProjectTaskCompleted`, `CpSyncCompleted`, `ContestReflectionAdded`, `DsaTopicUpdated`, `DailyScoreChanged`.
2. WHEN an App_Event triggers a recommendation update and internet is available, THE Daily_Intelligence_Engine SHALL send only the changed fields of the Intelligence_Snapshot (a delta) to the LLM_Provider rather than the full snapshot, to minimise token cost.
3. WHEN an App_Event triggers a recommendation update and internet is unavailable, THE Deterministic_Fallback engine SHALL produce an updated recommendation immediately without any network call.
4. THE App_Event system SHALL be internal to the App. No external system receives or processes App_Events.
5. THE Daily_Intelligence_Engine SHALL debounce rapid App_Events: if multiple App_Events arrive within a 30-second window, only one LLM call SHALL be made using the final aggregated snapshot delta.
6. WHEN a recommendation update is produced (by LLM or fallback), THE Daily_Intelligence_Engine SHALL update the active Daily_Brief's guidance section and persist the change to the Local_Database.
7. THE Student SHALL be able to disable intra-day LLM updates independently of the morning brief, to further limit API cost.

---

### Requirement 12: Daily Score Target

**User Story:** As a Student, I want a single daily score target that reflects my workload and goals, so that I can measure my day without tracking every metric manually.

#### Acceptance Criteria

1. THE Daily_Intelligence_Engine SHALL compute a daily score target as a weighted sum of: number of classes to attend, number of pending assignments due within 48 hours, presence of an active project Next_Immediate_Action, and a DSA revision task.
2. THE Daily_Intelligence_Engine SHALL expose the score target weights as Student-configurable parameters with documented defaults.
3. WHEN the Student marks tasks complete throughout the day, THE Daily_Intelligence_Engine SHALL update the Student's progress toward the daily score target in real time.
4. THE Daily_Intelligence_Engine SHALL persist daily score targets and actual scores to the Local_Database for historical trend analysis.

---

### Requirement 13: Notification System

**User Story:** As a Student, I want reliable, contextual notifications, so that I am reminded of important events at the right time without notification fatigue.

#### Acceptance Criteria

1. THE Notification_Manager SHALL support the following notification categories: `Class_Reminder`, `Free_Slot_Recommendation`, `Assignment_Reminder`, `Contest_Reminder`, `Inactive_Project_Reminder`, and `Daily_Brief`.
2. THE Notification_Manager SHALL allow the Student to enable or disable each notification category independently.
3. WHEN a contest is detected in the Student's CP_Profile sync within the configured lookahead window (default 24 hours), THE Notification_Manager SHALL schedule a Contest_Reminder notification.
4. WHEN a Free_Slot is identified in the daily schedule, THE Notification_Manager SHALL deliver a Free_Slot_Recommendation notification containing a suggested activity (DSA revision or project task).
5. THE Notification_Manager SHALL use only local scheduled notifications and SHALL NOT require a remote push notification service for any notification category.
6. IF the App is force-stopped and restarted, THE Notification_Manager SHALL reschedule all pending notifications on the next App launch within 30 seconds of startup.
7. THE Notification_Manager SHALL respect Android's Do Not Disturb settings and SHALL NOT use `PRIORITY_MAX` channels for any non-alarm category.

---

### Requirement 14: Offline-First Data Architecture

**User Story:** As a Student, I want the App to work fully without internet access, so that I am never blocked by connectivity issues during my academic day.

#### Acceptance Criteria

1. THE App SHALL store all Student data exclusively in the Local_Database on the device.
2. THE App SHALL provide full read and write functionality across all six modules while the device has no internet connectivity.
3. WHEN the device regains internet connectivity, THE Sync_Service SHALL resume CP_Profile sync automatically without requiring Student intervention.
4. THE App SHALL NOT require account creation, login, or any remote server for core functionality.
5. THE Local_Database SHALL survive App restarts and device reboots without data loss.
6. THE App SHALL provide a manual export of all Local_Database contents to a JSON file so the Student can back up data independently.
7. WHEN the Student imports a previously exported JSON backup, THE App SHALL restore all module data from that backup, prompting the Student to confirm before overwriting existing data.
8. THE Daily Intelligence Engine SHALL function completely without internet access using the Deterministic_Fallback engine. The LLM_Provider is an optional enhancement; its absence SHALL never degrade core App functionality.
9. WHEN the LLM_Provider is unavailable, any cached recommendation from the Recommendation_Cache SHALL be displayed with a visible "last updated" timestamp so the Student knows the guidance may be stale.

---

### Requirement 15: Timetable and Schedule Customisation

**User Story:** As a Student, I want full control over my schedule configuration, so that the App accommodates any university structure without being constrained by hardcoded assumptions.

#### Acceptance Criteria

1. THE App SHALL NOT hardcode semester names, subject names, or class counts anywhere in the codebase or Local_Database schema.
2. THE Attendance_Engine SHALL allow the Student to define custom time slots, including non-standard durations and irregular schedules.
3. THE Attendance_Engine SHALL allow the Student to define custom weeks (e.g., "odd week", "even week" alternating schedules).
4. THE App SHALL allow the Student to add, rename, or remove subjects at any time without invalidating historical Class_Event records.
5. WHEN a subject is removed, THE App SHALL retain all historical Class_Events for that subject in the Local_Database in an archived state.

---

### Requirement 16: Modular Architecture and Extensibility

**User Story:** As a developer extending Student OS, I want each module to be feature-isolated with a clean interface, so that new modules can be added without modifying existing ones.

#### Acceptance Criteria

1. THE App SHALL implement each of the six modules (Attendance, Assignment, Coding, Project, Daily_Intelligence, Notification) as independent feature modules with no direct inter-module class-level dependencies.
2. THE App SHALL define inter-module communication exclusively through shared data contracts (Local_Database queries or a module interface layer), not direct object references.
3. THE Local_Database schema SHALL use explicit foreign-key relationships between module entities and a shared `subjects` table rather than duplicating subject data per module.
4. THE App SHALL expose a documented extension point (e.g., a `ModuleRegistry`) that allows a new module to register its navigation entry, notification categories, and Local_Database tables without modifying core App files.
5. THE App architecture SHALL separate the data layer (Local_Database, Sync_Service), domain layer (business logic), and presentation layer (UI) in a way that allows each layer to be tested independently.

---

### Requirement 17: OCR Accuracy and Fallback

**User Story:** As a Student, I want the OCR import to handle imperfect images gracefully, so that I am not blocked when my timetable screenshot is not perfectly legible.

#### Acceptance Criteria

1. WHEN the OCR_Processor processes an image, THE OCR_Processor SHALL indicate a confidence score for each extracted field.
2. WHEN a field has a confidence score below a configurable threshold (default 80%), THE Attendance_Engine SHALL highlight that field in the review screen to prompt the Student to verify it.
3. THE Attendance_Engine SHALL allow partial imports where some rows are confirmed and others are corrected or deleted before saving.
4. FOR ALL timetable data confirmed by the Student, THE Attendance_Engine SHALL store exactly the Student-verified values regardless of the original OCR output (round-trip correctness: Student edits → stored data → displayed data SHALL match).

---

### Requirement 18: Data Integrity and Consistency

**User Story:** As a Student, I want my data to remain consistent even if the App crashes or the device loses power mid-operation, so that I never lose academic records.

#### Acceptance Criteria

1. THE Local_Database SHALL wrap all multi-step write operations (e.g., timetable import generating multiple Class_Events) in atomic transactions.
2. IF a transaction fails mid-execution, THEN THE Local_Database SHALL roll back all partial writes and THE App SHALL display a descriptive error to the Student.
3. THE Attendance_Engine SHALL recompute all attendance percentages from raw Class_Event records on App startup to detect and correct any cached calculation drift.
4. THE App SHALL enforce referential integrity constraints in the Local_Database schema so that deleting a subject does not silently orphan Class_Events or Assignments.

---

## Deferred Items (Out of Scope for Version 1)

The following capabilities were considered but are explicitly deferred to preserve delivery focus:

- **ML-based task recommendations**: Rule-based suggestions handle V1 deterministic logic. LLM handles narrative and prioritisation guidance from V1 itself.
- **Remote sync / cloud backup**: V1 is fully local. A server-side sync service is a V2 concern.
- **Finance module**: Explicitly deferred.
- **Habit tracking module**: Explicitly deferred.
- **Skill XP / gamification layer**: Deferred pending feedback on daily score target reception.
- **Social or collaboration features**: Out of scope entirely.
- **iOS port**: Android-only for V1.
- **LLM conversation history / multi-turn dialogue**: V1 LLM calls are stateless single-turn (system prompt + snapshot). Persistent conversation memory is a V2 feature.
- **On-device LLM (e.g., Gemini Nano)**: V1 uses cloud API only. On-device model support is a V2 upgrade path once model quality is sufficient.
- **Gemini / OpenAI provider implementations**: V1 ships only `DeepSeekProvider` and `MockProvider`. The `LLMProvider` interface is designed for drop-in replacement.

---

## Architecture Critiques and Honest Gaps

The following items represent decisions the development team should resolve before implementation begins. They are recorded here so nothing is assumed away.

### 1. "Daily Score Target" is underspecified
Resolved in the design document. Default weights are defined there (class = 10 pts, assignment = 20 pts, project action = 15 pts, DSA = 10 pts). All weights are Student-configurable via `settings`.

### 2. CP Contest detection requires a pull model
The App has no push channel from CodeChef/Codeforces. Contest reminders (Requirement 13, Criterion 3) depend on the Sync_Service polling for upcoming contests — this only works if the Student opens the App or the Sync_Service runs in the background. Android background execution restrictions (Doze, App Standby) may prevent timely reminders. Consider WorkManager with a `NETWORK_REQUIRED` constraint and clearly document the best-effort nature of contest reminders.

### 3. Single Next_Immediate_Action per project is a strong constraint
Requirement 10, Criterion 5 enforces exactly one active action. This works for solo projects but may frustrate students running team projects or parallel workstreams. The `is_parallel` escape hatch in the design resolves this for V1.

### 4. OCR library selection is unresolved
The requirements assume on-device OCR (ML Kit Text Recognition is the natural Android choice). The confidence score mechanism in Requirement 17 depends on the chosen library supporting per-field confidence. ML Kit returns bounding-box-level confidence, not semantic field-level confidence — the OCR_Processor will need a post-processing step to map raw text blocks to timetable fields and assign composite confidence. This should be prototyped early.

### 5. "Summarised JSON snapshot" for Daily Intelligence Engine
Resolved in the design document. The Intelligence_Snapshot schema is formally defined there and serves as the contract between the data layer, the deterministic engine, and the LLM_Provider.

### 6. File attachments in Assignment Engine
Resolved in the design document. Files are copied into App-private `filesDir/attachments/`.

### 7. LLM API key storage (new)
The DeepSeek API key must be stored securely. The key SHALL be stored in Android's `EncryptedSharedPreferences` (Jetpack Security library), not in `settings` plain text or hardcoded in the binary. The key is entered by the Student at first-run or in Settings and is never exported in the JSON backup.

### 8. LLM response trust boundary (new)
The LLM response is treated as untrusted human-readable text only. The App SHALL NOT parse structured data (numbers, dates, status values) out of LLM responses for use in business logic. All structured data comes exclusively from the Local_Database.
