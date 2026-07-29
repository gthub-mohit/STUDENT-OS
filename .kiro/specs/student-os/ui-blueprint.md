# Student OS — UI Blueprint

> **Version:** 1.0  
> **Scope:** Complete UI specification for all screens, components, flows, and design rules.  
> **This document is self-contained. All UI decisions are defined here.**

---

## Section 1: Information Architecture

### 1.1 Bottom Navigation Tabs

The bottom navigation bar is always visible (except during full-screen OCR preview). It contains 5 tabs.

| Tab Index | Label | Icon (Material Symbols Outlined) | Route |
|-----------|-------|----------------------------------|-------|
| 0 | Daily Brief | `auto_awesome` | `daily_brief` |
| 1 | Attendance | `event_available` | `attendance` |
| 2 | Assignments | `assignment` | `assignments` |
| 3 | Coding | `code` | `coding` |
| 4 | Projects | `folder_open` | `projects` |

Settings is accessed via a `settings` icon button in the top app bar (present on all root screens). It is NOT a bottom nav tab.

---

### 1.2 Full Navigation Graph

All routes are defined in a single NavHost. Nested graphs group related screens.

```
NavHost (startDestination = "daily_brief")
│
├── daily_brief  (root tab)
│   ├── DailyBriefScreen          route: "daily_brief"
│   └── BriefHistoryScreen        route: "brief_history"
│
├── attendance  (root tab)
│   ├── WeeklyViewScreen          route: "attendance/weekly"
│   ├── CalendarViewScreen        route: "attendance/calendar"
│   ├── AttendanceAnalyticsScreen route: "attendance/analytics"
│   ├── OcrPreviewScreen          route: "attendance/ocr_preview"
│   └── EditTimetableScreen       route: "attendance/edit_timetable"
│
├── assignments  (root tab)
│   ├── AssignmentListScreen      route: "assignments"
│   └── AssignmentDetailScreen    route: "assignments/{assignmentId}"
│
├── coding  (root tab)
│   ├── CpDashboardScreen         route: "coding/cp"
│   ├── ContestReflectionScreen   route: "coding/contest/{contestId}/reflection"
│   ├── KnowledgeTreeScreen       route: "coding/knowledge_tree"
│   └── DsaTopicDetailScreen      route: "coding/dsa/{topicId}"
│
├── projects  (root tab)
│   ├── ProjectListScreen         route: "projects"
│   ├── ProjectDetailScreen       route: "projects/{projectId}"
│   ├── MilestoneScreen           route: "projects/{projectId}/milestones/{milestoneId}"
│   └── BugScreen                 route: "projects/{projectId}/bugs/{bugId}"
│
└── settings  (top-bar entry)
    ├── SettingsScreen            route: "settings"
    ├── AiSettingsScreen          route: "settings/ai"
    ├── AiDiagnosticsScreen       route: "settings/ai/diagnostics"
    ├── NotificationSettingsScreen route: "settings/notifications"
    └── BackupScreen              route: "settings/backup"
```

---

### 1.3 Deep Link URI Scheme

Base scheme: `studentos://`

| Deep Link URI | Destination Screen | Notes |
|---|---|---|
| `studentos://daily_brief` | DailyBriefScreen | Opens tab 0 |
| `studentos://assignments/{id}` | AssignmentDetailScreen | Scrolls to assignment |
| `studentos://attendance/weekly` | WeeklyViewScreen | Opens tab 1 |
| `studentos://coding/cp` | CpDashboardScreen | Opens tab 3 |
| `studentos://projects/{projectId}` | ProjectDetailScreen | Opens tab 4 |

---

### 1.4 Screen Hierarchy Tree

```
Student OS
├── [Tab] Daily Brief
│   ├── DailyBriefScreen  ← default landing
│   └── BriefHistoryScreen
├── [Tab] Attendance
│   ├── WeeklyViewScreen  ← default landing
│   ├── CalendarViewScreen
│   ├── AttendanceAnalyticsScreen
│   ├── OcrPreviewScreen
│   └── EditTimetableScreen
├── [Tab] Assignments
│   ├── AssignmentListScreen  ← default landing
│   └── AssignmentDetailScreen
├── [Tab] Coding
│   ├── CpDashboardScreen  ← default landing
│   ├── ContestReflectionScreen
│   ├── KnowledgeTreeScreen
│   └── DsaTopicDetailScreen
├── [Tab] Projects
│   ├── ProjectListScreen  ← default landing
│   ├── ProjectDetailScreen
│   ├── MilestoneScreen
│   └── BugScreen
└── [Settings] (top-bar)
    ├── SettingsScreen
    ├── AiSettingsScreen
    │   └── AiDiagnosticsScreen
    ├── NotificationSettingsScreen
    └── BackupScreen
```

---

## Section 2: Screen Specifications

### 2.1 DailyBriefScreen

**Purpose:** Displays the AI-generated daily guidance card with recommendations, upcoming events, and offline fallback content.

**Entry points:**
- App launch (default tab)
- Notification tap: DAILY_BRIEF, FREE_SLOT_RECOMMENDATION
- Bottom nav tab 0

**Layout:**
- Top bar: `Student OS` title (TitleLarge), `history` icon button (BriefHistoryScreen), `settings` icon button
- Content area: Scrollable vertical list
- No FAB

**Sections:**
1. AiStatusBadge row — shows "AI Online", "AI Offline", or "Quota Reached" with last-updated timestamp
2. GuidanceCard — full-width card with LLM-generated or fallback text, source label
3. Today's Snapshot row — mini summary: classes today, pending assignments count, nearest contest
4. Recommendations list — up to 5 prioritized action chips with one-line descriptions
5. Upcoming deadlines strip — horizontal scroll of DeadlineCountdown chips for next 48 hours

**Key components:** GuidanceCard, AiStatusBadge, DeadlineCountdown, StatusChip, LoadingOverlay

**Actions:**
- Tap recommendation chip → navigate to relevant screen
- Pull to refresh → re-trigger brief generation
- Tap history icon → BriefHistoryScreen
- Long press GuidanceCard → copy text to clipboard (snackbar confirmation)

**Empty state:** "No brief yet. Tap refresh to generate your daily plan." with a refresh button.

**Loading state:** GuidanceCard replaced by shimmer placeholder. AiStatusBadge shows "Generating…"

**Error state:** GuidanceCard shows "Could not load brief. Check API key in Settings." with retry button.

**Offline state:** GuidanceCard displays deterministic fallback content. AiStatusBadge shows "AI Offline — using cached plan". No error shown to user.

---

### 2.2 BriefHistoryScreen

**Purpose:** Shows past daily briefs in reverse chronological order.

**Entry points:** DailyBriefScreen history icon button.

**Layout:**
- Top bar: Back arrow, "Brief History" title
- Content: Lazy vertical list of GuidanceCards, each with a date header
- No FAB

**Sections:**
1. Date-grouped list — each group header is the date (e.g., "Mon 14 Jul"), each item is a collapsed GuidanceCard
2. Tap to expand — expands to full brief text

**Key components:** GuidanceCard, AiStatusBadge (per entry), EmptyStateView

**Actions:** Tap card to expand/collapse. Back arrow to return.

**Empty state:** "No history yet. Your briefs will appear here after your first generation."

**Loading state:** Shimmer list of 3 collapsed cards.

**Error state:** "Could not load history." with retry.

**Offline state:** Shows whatever is in local Room cache. No network needed.

---

### 2.3 WeeklyViewScreen

**Purpose:** Shows the current week's timetable with attendance status per class slot.

**Entry points:**
- Bottom nav tab 1 (default attendance screen)
- Notification tap: CLASS_REMINDER

**Layout:**
- Top bar: "Attendance" title, `calendar_month` icon → CalendarViewScreen, `bar_chart` icon → AttendanceAnalyticsScreen, `settings` icon
- Content: Horizontal day-tab row (Mon–Sat), then vertical list of ClassEventCards for selected day
- FAB: `add` icon → add extra class dialog

**Sections:**
1. Day selector tabs — 6 tabs (Mon–Sat), current day highlighted, absent-heavy days show amber indicator
2. AttendancePercentageRow — overall % across all subjects, color-coded
3. ClassEventCards list — one card per slot, showing subject, time, status chip
4. BunkCalculatorWidget — sticky footer showing canSkip / mustAttend for selected subject (context-sensitive)

**Key components:** ClassEventCard, AttendancePercentageRow, BunkCalculatorWidget, StatusChip, ConfirmDialog

**Actions:**
- Tap ClassEventCard status → cycle Present / Absent / Cancelled (future date requires confirmation dialog)
- Tap FAB → bottom sheet to add extra/makeup class
- Swipe ClassEventCard left → mark absent (with undo snackbar)
- Swipe ClassEventCard right → mark present (with undo snackbar)

**Empty state:** "No classes scheduled for this day. Add classes via Edit Timetable." with link.

**Loading state:** Shimmer for 4 ClassEventCards.

**Error state:** "Could not load timetable." with retry.

**Offline state:** Fully functional. All data is local Room. No degradation.

---

### 2.4 CalendarViewScreen

**Purpose:** Month-level view of attendance patterns to spot trends and streaks.

**Entry points:** WeeklyViewScreen calendar icon.

**Layout:**
- Top bar: Back arrow, "Calendar View" title
- Content: Month calendar grid, followed by selected-day details

**Sections:**
1. Month grid — each day cell color-coded: green (full attendance), amber (partial), red (full absent), grey (no class)
2. Legend — four color labels below grid
3. Selected day detail — list of ClassEventCards for tapped day

**Key components:** ClassEventCard, StatusChip, AttendancePercentageRow

**Actions:** Tap day → show that day's classes below. Swipe left/right → previous/next month.

**Empty state:** "No attendance data for this month."

**Loading state:** Shimmer grid.

**Error state:** "Could not load calendar data." with retry.

**Offline state:** Fully functional from Room cache.

---

### 2.5 AttendanceAnalyticsScreen

**Purpose:** Per-subject analytics with bunk budget and trend charts.

**Entry points:** WeeklyViewScreen analytics icon.

**Layout:**
- Top bar: Back arrow, "Analytics" title
- Content: Vertical scroll of per-subject analytics cards

**Sections:**
1. Overall summary card — total attendance %, classes attended / total
2. Per-subject rows — each shows: subject name, % bar, attended/total, canSkip count, mustAttend count
3. BunkCalculatorWidget expanded — shows threshold, current %, required classes to reach safe zone
4. Trend section — last 4 weeks trend per subject (text-based: "↑ improving", "↓ declining", "→ stable")

**Key components:** BunkCalculatorWidget, ScoreProgressBar, AttendancePercentageRow, StatusChip

**Actions:** Tap subject row → highlight and scroll to that subject's detail. Back to return.

**Empty state:** "No attendance recorded yet. Start marking classes from Weekly View."

**Loading state:** Shimmer for 3 subject cards.

**Error state:** "Could not compute analytics." with retry.

**Offline state:** Fully functional. All computation is local.

---

### 2.6 OcrPreviewScreen

**Purpose:** Preview and correct OCR-extracted timetable data before importing.

**Entry points:** EditTimetableScreen → "Import via Camera" action.

**Layout:**
- Top bar: Cancel (X), "Review Import" title, "Import" action button (enabled only if no blocking errors)
- Content: Scrollable form of extracted fields
- No FAB

**Sections:**
1. Capture preview thumbnail — small image of the scanned document
2. Extracted fields list — each field: label, extracted value text field, ConfidenceLevelIndicator
3. Low-confidence warning banner — amber banner "Some fields need review" if any field < 70% confidence
4. Partial import toggle — "Import only high-confidence fields" switch

**Key components:** ConfidenceLevelIndicator, StatusChip, EmptyStateView

**Actions:**
- Edit any field inline
- Toggle partial import switch
- Tap Cancel → discard and return
- Tap Import → validate and save to Room, navigate back to EditTimetableScreen

**Empty state:** N/A — screen only appears with OCR data.

**Loading state:** Full-screen LoadingOverlay "Processing image…" shown before screen appears.

**Error state:** "OCR failed. Try again with better lighting." with retry and manual entry option.

**Offline state:** OCR is on-device (ML Kit). Fully offline.

---

### 2.7 EditTimetableScreen

**Purpose:** Manually create or edit the weekly timetable slot by slot.

**Entry points:** WeeklyViewScreen via overflow menu → "Edit Timetable". Settings → "Manage Timetable".

**Layout:**
- Top bar: Back arrow, "Edit Timetable" title, Save checkmark action
- Content: Day tabs + slot list
- FAB: `add` to add new slot to selected day

**Sections:**
1. Day selector tabs — Mon–Sat
2. Slot list — each slot shows: time range, subject name, room (optional), edit/delete icons
3. Add/Edit slot form — shown as bottom sheet: start time, end time, subject picker, room field

**Key components:** StatusChip, ConfirmDialog, EmptyStateView

**Actions:**
- Tap slot → open edit bottom sheet
- Tap delete icon on slot → ConfirmDialog ("Delete this class?")
- Tap FAB → add new slot bottom sheet
- Import via Camera → navigate to OcrPreviewScreen
- Save → persist to Room, back navigate

**Empty state:** "No classes added yet. Tap + to add your first class slot."

**Loading state:** Shimmer list.

**Error state:** "Could not save timetable." with retry.

**Offline state:** Fully local. No network needed.

---

### 2.8 AssignmentListScreen

**Purpose:** Shows all assignments grouped by status with filtering and sorting options.

**Entry points:** Bottom nav tab 2.

**Layout:**
- Top bar: "Assignments" title, `filter_list` icon, `sort` icon, `settings` icon
- Content: Vertical list grouped by status: OVERDUE → IN_PROGRESS → PENDING → COMPLETED
- FAB: `add` → create new assignment

**Sections:**
1. Status group headers — collapsible, show count badge
2. AssignmentCard list per group
3. Filter chips row (sticky below top bar) — All / Pending / In Progress / Submitted / Completed / Overdue

**Key components:** AssignmentCard, DeadlineCountdown, StatusChip, PriorityBadge, ConfirmDialog, EmptyStateView

**Actions:**
- Tap card → AssignmentDetailScreen
- Swipe right → mark complete (PENDING/IN_PROGRESS only; snackbar with undo)
- Swipe left → delete (PENDING/IN_PROGRESS requires ConfirmDialog; SUBMITTED/COMPLETED allows direct delete with undo snackbar)
- Long press → multi-select mode (select multiple, bulk delete or bulk complete)
- Tap FAB → new assignment form (bottom sheet)

**Empty state:** "No assignments yet. Tap + to add your first one." with illustration.

**Loading state:** Shimmer list of 3 AssignmentCards.

**Error state:** "Could not load assignments." with retry.

**Offline state:** Fully functional. All data is local.

---

### 2.9 AssignmentDetailScreen

**Purpose:** Full detail view of a single assignment with editing, subtask management, and status updates.

**Entry points:**
- AssignmentListScreen card tap
- Notification: ASSIGNMENT_REMINDER deep link

**Layout:**
- Top bar: Back arrow, assignment title (TitleLarge), edit pencil icon, `more_vert` overflow
- Content: Scrollable detail form
- No FAB

**Sections:**
1. Header — subject chip, priority badge, due date with DeadlineCountdown
2. Description card — full description text, editable inline on tap
3. Status row — StatusChip (tap to cycle), completion percentage
4. Subtasks / checklist — TaskList with add/remove/check subtasks
5. Notes field — freeform notes text area
6. Attachments row — file link chips (future: local file paths)
7. Metadata footer — created date, last modified

**Key components:** DeadlineCountdown, StatusChip, PriorityBadge, TaskList, ConfirmDialog

**Actions:**
- Tap status chip → cycle status (PENDING → IN_PROGRESS → SUBMITTED → COMPLETED). PENDING/IN_PROGRESS → delete requires ConfirmDialog.
- Edit title/description inline
- Add/remove subtasks
- Overflow: Delete assignment (ConfirmDialog for PENDING/IN_PROGRESS), Duplicate

**Empty state:** N/A — screen always has data.

**Loading state:** Shimmer for all sections.

**Error state:** "Could not load assignment." with retry.

**Offline state:** Fully functional.

---

### 2.10 CpDashboardScreen

**Purpose:** Central hub for competitive programming: profile stats, upcoming contests, and DSA progress.

**Entry points:**
- Bottom nav tab 3
- Notification: CONTEST_REMINDER

**Layout:**
- Top bar: "Coding" title, `leaderboard` icon → KnowledgeTreeScreen, `settings` icon
- Content: Scrollable vertical sections
- FAB: `add` → log new contest result

**Sections:**
1. CP Profile card — platform name, handle, current rating, max rating, ScoreProgressBar
2. Upcoming contests strip — horizontal scroll of ContestResultCards (upcoming, not yet played)
3. Recent contests list — last 5 contests with ContestResultCard
4. DSA Progress section — topic category bars (Arrays, Trees, Graphs, etc.) with ScoreProgressBar per category
5. Streak banner — current daily solve streak

**Key components:** ContestResultCard, ScoreProgressBar, StatusChip, EmptyStateView, LoadingOverlay

**Actions:**
- Tap upcoming contest card → set reminder, mark as planned
- Tap recent contest card → ContestReflectionScreen
- Tap DSA category → KnowledgeTreeScreen filtered to that category
- Tap FAB → add contest result bottom sheet

**Empty state:** "No CP profile set up. Add your CodeChef or Codeforces handle in Settings → AI & Coding."

**Loading state:** Shimmer for profile card and 3 contest cards.

**Error state:** "Could not load CP data." with retry.

**Offline state:** Uses last-synced Room data. Shows "Last synced: [timestamp]" label.

---

### 2.11 ContestReflectionScreen

**Purpose:** Post-contest reflection form: what went well, what to improve, problems to revisit.

**Entry points:** CpDashboardScreen → tap recent contest card.

**Layout:**
- Top bar: Back arrow, "Contest Reflection" title, Save checkmark
- Content: Scrollable form
- No FAB

**Sections:**
1. Contest info header — name, date, platform, final rating change badge
2. Performance metrics — problems solved / total, rank, percentile
3. "What went well" text field (multi-line)
4. "What to improve" text field (multi-line)
5. Problems to revisit — list of problem links/names with difficulty chips, add button
6. Key learnings tags — chip group for tagging topics (e.g., "DP", "Segment Tree")

**Key components:** StatusChip, PriorityBadge, ScoreProgressBar, ConfirmDialog (on back with unsaved changes)

**Actions:**
- Edit all fields inline
- Add problem to revisit list
- Remove problem from list
- Save → persist to Room, back navigate
- Back with unsaved changes → ConfirmDialog "Discard changes?"

**Empty state:** N/A — form pre-populated with contest data.

**Loading state:** Shimmer form skeleton.

**Error state:** "Could not load contest data." with retry.

**Offline state:** Fully local.

---

### 2.12 KnowledgeTreeScreen

**Purpose:** Visual tree of DSA topics showing mastery level and progression dependencies.

**Entry points:**
- CpDashboardScreen knowledge tree icon
- CpDashboardScreen DSA category tap

**Layout:**
- Top bar: Back arrow, "Knowledge Tree" title, search icon
- Content: Scrollable hierarchical list (tree rendered as indented list groups)
- No FAB

**Sections:**
1. Category headers — collapsible (Arrays, Strings, Trees, Graphs, DP, etc.)
2. KnowledgeTreeItem per topic — topic name, mastery badge (Not Started / Learning / Practiced / Mastered), problem count
3. Filter row — filter by mastery level

**Key components:** KnowledgeTreeItem, StatusChip, ScoreProgressBar, EmptyStateView

**Actions:**
- Tap topic → DsaTopicDetailScreen
- Toggle category collapse/expand
- Filter chips to narrow by mastery

**Empty state:** "No topics found. Start solving problems to populate your tree."

**Loading state:** Shimmer for 3 category groups.

**Error state:** "Could not load knowledge tree." with retry.

**Offline state:** Fully local.

---

### 2.13 DsaTopicDetailScreen

**Purpose:** Detailed view of a single DSA topic with problems list, notes, and mastery progression.

**Entry points:** KnowledgeTreeScreen topic tap.

**Layout:**
- Top bar: Back arrow, topic name (TitleLarge), `edit` icon
- Content: Scrollable
- No FAB

**Sections:**
1. Mastery badge and ScoreProgressBar
2. Description / notes card — editable freeform notes
3. Problems list — each problem: name, difficulty chip, solved/unsolved toggle, link
4. Related topics chips — horizontal scroll of linked topic chips

**Key components:** KnowledgeTreeItem, StatusChip, ScoreProgressBar, TaskList

**Actions:**
- Toggle problem solved/unsolved (updates mastery score in real time)
- Edit notes inline
- Tap related topic chip → navigate to that topic's detail

**Empty state:** "No problems added to this topic yet."

**Loading state:** Shimmer.

**Error state:** "Could not load topic data." with retry.

**Offline state:** Fully local.

---

### 2.14 ProjectListScreen

**Purpose:** Displays all engineering projects with status, next action, and activity recency.

**Entry points:** Bottom nav tab 4.

**Layout:**
- Top bar: "Projects" title, filter icon, `settings` icon
- Content: Vertical list of ProjectCards
- FAB: `add` → create new project

**Sections:**
1. Filter chips row — All / Active / Paused / Completed
2. ProjectCard list — sorted by last activity (most recent first)
3. NextActionBanner — sticky banner showing most urgent next action across all active projects

**Key components:** ProjectCard, NextActionBanner, StatusChip, PriorityBadge, EmptyStateView

**Actions:**
- Tap card → ProjectDetailScreen
- Swipe left → archive project (ConfirmDialog)
- Tap FAB → new project bottom sheet (name, description, status, tags)
- Long press → multi-select mode

**Empty state:** "No projects yet. Tap + to create your first project."

**Loading state:** Shimmer list of 3 ProjectCards.

**Error state:** "Could not load projects." with retry.

**Offline state:** Fully local.

---

### 2.15 ProjectDetailScreen

**Purpose:** Full project view with milestones, bugs, next action, and progress overview.

**Entry points:**
- ProjectListScreen card tap
- Notification: INACTIVE_PROJECT_REMINDER

**Layout:**
- Top bar: Back arrow, project name (TitleLarge), `edit` icon, `more_vert` overflow
- Content: Tabbed layout within screen: Overview | Milestones | Bugs
- FAB changes per tab: Overview → no FAB; Milestones → `add`; Bugs → `add`

**Sections (Overview tab):**
1. Project header — name, status chip, tags, description
2. NextActionBanner — current next action with "Complete" button
3. Progress bar — milestones completed / total
4. Recent activity log — last 5 changes with timestamps

**Sections (Milestones tab):**
1. Milestone list — each: name, due date, status chip, completion %
2. Tap → MilestoneScreen

**Sections (Bugs tab):**
1. Bug list — each: title, severity chip, status chip
2. Tap → BugScreen

**Key components:** ProjectCard, NextActionBanner, StatusChip, PriorityBadge, ScoreProgressBar, TaskList, NewNextActionSheet, ConfirmDialog

**Actions:**
- Tap "Complete" on NextActionBanner → NewNextActionSheet (pick next action before returning)
- Add/edit milestones from Milestones tab
- Add/edit bugs from Bugs tab
- Overflow: Archive project, Delete project (ConfirmDialog), Export

**Empty state (Milestones):** "No milestones. Tap + to add one."

**Empty state (Bugs):** "No bugs logged. Tap + to add one."

**Loading state:** Shimmer per tab.

**Error state:** "Could not load project." with retry.

**Offline state:** Fully local.

---

### 2.16 MilestoneScreen

**Purpose:** Detailed view of a single project milestone with tasks and status.

**Entry points:** ProjectDetailScreen → Milestones tab → tap milestone.

**Layout:**
- Top bar: Back arrow, milestone name (TitleLarge), `edit` icon
- Content: Scrollable
- No FAB

**Sections:**
1. Milestone header — due date, status chip, progress bar
2. Task list — TaskList component with add/remove/check items
3. Notes — freeform text

**Key components:** TaskList, StatusChip, ScoreProgressBar, DeadlineCountdown

**Actions:** Edit tasks, update status, edit notes, save. Back with unsaved → ConfirmDialog.

**Empty state:** "No tasks in this milestone."

**Loading state:** Shimmer.

**Error state:** "Could not load milestone." with retry.

**Offline state:** Fully local.

---

### 2.17 BugScreen

**Purpose:** Detailed view of a logged bug with reproduction steps, severity, and resolution notes.

**Entry points:** ProjectDetailScreen → Bugs tab → tap bug.

**Layout:**
- Top bar: Back arrow, bug title, `edit` icon
- Content: Scrollable form
- No FAB

**Sections:**
1. Bug header — severity chip, status chip (Open / In Progress / Resolved)
2. Description field
3. Steps to reproduce — numbered list (TaskList variant)
4. Expected / Actual behavior text fields
5. Resolution notes field
6. Linked milestone chip (optional)

**Key components:** StatusChip, PriorityBadge, TaskList, ConfirmDialog

**Actions:** Edit all fields inline, update status, save. Delete → ConfirmDialog.

**Empty state:** N/A.

**Loading state:** Shimmer.

**Error state:** "Could not load bug." with retry.

**Offline state:** Fully local.

---

### 2.18 SettingsScreen

**Purpose:** Root settings hub with links to all settings sub-screens.

**Entry points:** Top bar settings icon from any root screen.

**Layout:**
- Top bar: Back arrow, "Settings" title
- Content: Preference list grouped by category
- No FAB

**Sections:**
1. AI group — "AI Settings", "AI Diagnostics"
2. Notifications group — "Notification Settings"
3. Data group — "Backup & Restore"
4. Appearance group — "Theme" (System / Light / Dark), "Dynamic Color" toggle (Android 12+)
5. About group — app version, open source licenses

**Key components:** StatusChip (for connectivity status), EmptyStateView

**Actions:** Tap any row → navigate to sub-screen. Toggle theme inline.

**Empty state:** N/A.

**Loading state:** N/A (static list).

**Error state:** N/A.

**Offline state:** N/A — fully local.

---

### 2.19 AiSettingsScreen

**Purpose:** Configure DeepSeek API key, model selection, and AI behavior preferences.

**Entry points:** SettingsScreen → AI Settings.

**Layout:**
- Top bar: Back arrow, "AI Settings" title
- Content: Form-style preference list
- No FAB

**Sections:**
1. API Configuration — API key field (masked, show/hide toggle), model selector dropdown
2. Daily Brief settings — generation time picker, auto-generate toggle
3. Recommendation settings — max recommendations count slider (1–10)
4. Cost controls — daily token budget input, quota warning threshold
5. Diagnostics link row — "View AI Call Log" → AiDiagnosticsScreen

**Key components:** AiStatusBadge, LoadingOverlay (for key validation)

**Actions:** Edit API key → validate on save (show AiStatusBadge result). Tap Diagnostics → AiDiagnosticsScreen.

**Empty state:** N/A.

**Loading state:** LoadingOverlay during key validation.

**Error state:** "Invalid API key. Please check and try again." inline below key field.

**Offline state:** Can edit settings offline; validation deferred.

---

### 2.20 AiDiagnosticsScreen

**Purpose:** Shows ai_call_log with per-call details and daily cost estimate.

**Entry points:** AiSettingsScreen → View AI Call Log.

**Layout:**
- Top bar: Back arrow, "AI Diagnostics" title, `delete_sweep` → clear log (ConfirmDialog)
- Content: Scrollable log list + summary header
- No FAB

**Sections:**
1. Summary card — today's call count, estimated cost, tokens used
2. Log list — each entry: timestamp, call type, tokens in/out, cost estimate, status (success/error), latency
3. Error entries — highlighted in error container color

**Key components:** AiStatusBadge, StatusChip, EmptyStateView

**Actions:** Scroll log, clear log (ConfirmDialog), tap entry to expand full request/response preview (truncated).

**Empty state:** "No AI calls logged yet."

**Loading state:** Shimmer list.

**Error state:** "Could not load call log." with retry.

**Offline state:** Fully local.

---

### 2.21 NotificationSettingsScreen

**Purpose:** Per-channel notification enable/disable and timing configuration.

**Entry points:** SettingsScreen → Notification Settings.

**Layout:**
- Top bar: Back arrow, "Notifications" title
- Content: Grouped preference list
- No FAB

**Sections:**
1. Daily Brief channel — enable toggle, delivery time picker
2. Assignment Reminders — enable toggle, lead time selector (1h / 3h / 1d / 2d before deadline)
3. Class Reminders — enable toggle, lead time selector (5 / 10 / 15 / 30 min before class)
4. Contest Reminders — enable toggle, lead time selector (1h / 3h / 1d)
5. Free Slot Recommendation — enable toggle
6. Inactive Project Reminder — enable toggle, inactivity threshold (3d / 7d / 14d)

**Actions:** Toggle each channel, adjust timing inline.

**Empty state:** N/A. **Loading state:** N/A. **Error state:** N/A. **Offline state:** Fully local.

---

### 2.22 BackupScreen

**Purpose:** Export and restore the full Room database as a local file.

**Entry points:** SettingsScreen → Backup & Restore.

**Layout:**
- Top bar: Back arrow, "Backup & Restore" title
- Content: Two action cards + backup history list
- No FAB

**Sections:**
1. Export card — "Export Backup" button, last export timestamp
2. Restore card — "Restore from File" button, warning text about data overwrite
3. Backup history list — last 5 exports with filename, size, date

**Key components:** ConfirmDialog (for restore), LoadingOverlay (during export/import), StatusChip

**Actions:**
- Tap Export → generate backup file, save to Downloads, snackbar "Backup saved to Downloads/studentos_backup_[date].zip"
- Tap Restore → file picker → ConfirmDialog "This will overwrite all current data. Proceed?" → LoadingOverlay → success/failure snackbar
- Tap history item → share/re-export that backup

**Empty state (history):** "No backups created yet."

**Loading state:** LoadingOverlay "Exporting…" / "Restoring…"

**Error state:** "Backup failed. Storage permission may be required." with settings deep link.

**Offline state:** Fully local.

---

---

## Section 3: Reusable Components

Each component below is defined with its display props, behavior, and where it appears in the app.

---

### 3.1 ClassEventCard

**Description:** Represents a single timetable slot (class period) with attendance status.

**Props:**
- `subjectName: String` — displayed as TitleMedium
- `timeRange: String` — e.g., "09:00–10:00", displayed as BodySmall
- `room: String?` — optional room identifier, LabelSmall
- `status: AttendanceStatus` — PRESENT / ABSENT / CANCELLED / NOT_MARKED
- `isFuture: Boolean` — controls whether status change requires confirmation
- `onStatusChange: (AttendanceStatus) -> Unit`

**Layout:** Horizontal card. Left: colored status strip (4dp wide). Center: subject + time. Right: StatusChip.

**Behavior:** Tap status chip cycles status. If `isFuture = true`, cycling triggers ConfirmDialog before committing. Swipe left = absent, swipe right = present.

**Reuse locations:** WeeklyViewScreen, CalendarViewScreen (selected day), OcrPreviewScreen (preview)

---

### 3.2 AttendancePercentageRow

**Description:** Shows overall attendance percentage as a horizontal bar with color coding.

**Props:**
- `percentage: Float` — 0.0–100.0
- `threshold: Float` — default 75.0, configurable in settings
- `label: String` — e.g., "Overall" or subject name
- `attended: Int`, `total: Int`

**Layout:** Row with label on left, "X/Y classes" in center, percentage on right with color-coded ScoreProgressBar below.

**Behavior:** Color green if `percentage >= threshold`, amber if within 5% below, red if significantly below. Recalculates within 500ms of any status change (debounced recomposition).

**Reuse locations:** WeeklyViewScreen (footer), AttendanceAnalyticsScreen (per subject row), CalendarViewScreen

---

### 3.3 BunkCalculatorWidget

**Description:** Tells the student how many classes they can safely miss or must attend to stay above threshold.

**Props:**
- `currentPercentage: Float`
- `attended: Int`, `total: Int`
- `threshold: Float`
- `subjectName: String`

**Layout:** Card with two columns: "Can Skip: N classes" and "Must Attend: N classes". Threshold label shown below.

**Behavior:** If already below threshold, shows "Must Attend" in error color. If above threshold, shows "Can Skip" in green. Calculation: canSkip = floor((attended - threshold/100 * (total + future)) / 1). Updates in real time.

**Reuse locations:** WeeklyViewScreen (sticky footer, context-sensitive to selected subject), AttendanceAnalyticsScreen

---

### 3.4 AssignmentCard

**Description:** Summary card for an assignment in a list.

**Props:**
- `title: String`
- `subject: String`
- `dueDate: LocalDate`
- `status: AssignmentStatus` — PENDING / IN_PROGRESS / SUBMITTED / COMPLETED / OVERDUE
- `priority: Priority` — LOW / MEDIUM / HIGH
- `onClick: () -> Unit`

**Layout:** Card. Top row: title (TitleSmall) + PriorityBadge. Middle: subject chip + DeadlineCountdown. Bottom: StatusChip.

**Reuse locations:** AssignmentListScreen

---

### 3.5 DeadlineCountdown

**Description:** Displays time remaining until a deadline in human-readable form.

**Props:**
- `dueDate: LocalDateTime`
- `style: CountdownStyle` — CHIP (small) / FULL (expanded)

**Layout:** Chip variant: colored chip "Due in 2d 4h". Full variant: larger text block with date. Color: green > 3 days, amber 1–3 days, red < 1 day, grey if past.

**Behavior:** Updates dynamically. Never the only indicator — always paired with date text.

**Reuse locations:** AssignmentCard, AssignmentDetailScreen, DailyBriefScreen (deadlines strip), MilestoneScreen

---

### 3.6 ContestResultCard

**Description:** Card showing a competitive programming contest entry.

**Props:**
- `contestName: String`
- `platform: String` — `CODECHEF` or `CODEFORCES`
- `date: LocalDate`
- `rank: Int?`
- `ratingChange: Int?` — positive or negative
- `problemsSolved: Int`, `totalProblems: Int`
- `isUpcoming: Boolean`
- `onClick: () -> Unit`

**Layout:** Card. Left: platform icon chip. Center: contest name, date. Right: rating change badge (green/red) or "Upcoming" chip.

**Reuse locations:** CpDashboardScreen (both upcoming and recent lists)

---

### 3.7 ConfidenceLevelIndicator

**Description:** Visual indicator for OCR field confidence score.

**Props:**
- `confidence: Float` — 0.0–1.0
- `label: String` — field name

**Layout:** Small horizontal bar (40dp wide) with color: green ≥ 0.85, amber 0.70–0.84, red < 0.70. Paired with percentage label text.

**Behavior:** Fields with amber/red confidence have their text field highlighted with amber/error border. Never color-only: icon (check / warning / error) always paired.

**Reuse locations:** OcrPreviewScreen

---

### 3.8 KnowledgeTreeItem

**Description:** A single DSA topic row in the knowledge tree.

**Props:**
- `topicName: String`
- `mastery: MasteryLevel` — NOT_STARTED / LEARNING / PRACTICED / MASTERED
- `problemCount: Int`
- `depth: Int` — 0 for category, 1 for topic, for visual indentation
- `onClick: () -> Unit`

**Layout:** Row. Indented by `depth * 16dp`. Left: expand/collapse icon (if has children). Center: topic name. Right: mastery badge chip + problem count.

**Reuse locations:** KnowledgeTreeScreen

---

### 3.9 ProjectCard

**Description:** Summary card for a project in the project list.

**Props:**
- `name: String`
- `status: ProjectStatus` — ACTIVE / PAUSED / COMPLETED / ARCHIVED
- `nextAction: String?`
- `milestoneProgress: Float` — 0.0–1.0
- `lastActivityDate: LocalDate`
- `tags: List<String>`
- `onClick: () -> Unit`

**Layout:** Card. Top: project name + StatusChip. Middle: NextAction text (truncated 1 line). Bottom: ScoreProgressBar for milestones + last activity label.

**Reuse locations:** ProjectListScreen

---

### 3.10 NextActionBanner

**Description:** Prominent banner showing the current next action for a project.

**Props:**
- `actionText: String`
- `projectName: String`
- `onComplete: () -> Unit`

**Layout:** Surface-variant colored banner. Left: "NEXT:" label (LabelSmall) + action text (BodyMedium). Right: "Done" filled button (small).

**Behavior:** Tapping "Done" triggers NewNextActionSheet. Only disappears after new next action is set.

**Reuse locations:** ProjectDetailScreen (top of Overview tab), ProjectListScreen (sticky top banner for most urgent project)

---

### 3.11 TaskList

**Description:** Checklist component with add/remove/check functionality.

**Props:**
- `items: List<TaskItem>` — each has `id`, `text`, `isDone`
- `onItemToggle: (id) -> Unit`
- `onItemAdd: (text) -> Unit`
- `onItemDelete: (id) -> Unit`
- `isEditable: Boolean`

**Layout:** Vertical list. Each row: checkbox + text + (if editable) delete icon. Footer: "Add item" text field (shown when editable).

**Reuse locations:** AssignmentDetailScreen (subtasks), MilestoneScreen (tasks), BugScreen (steps to reproduce), DsaTopicDetailScreen (problems)

---

### 3.12 GuidanceCard

**Description:** Full-width card displaying LLM-generated or fallback daily guidance.

**Props:**
- `content: String` — the AI or fallback text
- `source: BriefSource` — LLM / FALLBACK / CACHED
- `generatedAt: LocalDateTime`

**Layout:** Card (12dp radius, 0dp elevation). Top: source label chip ("AI Generated" / "Offline Mode"). Body: content text (BodyMedium), max 5 lines with "Read more" expand. Footer: timestamp (LabelSmall).

**Reuse locations:** DailyBriefScreen (primary), BriefHistoryScreen (per entry)

---

### 3.13 AiStatusBadge

**Description:** Small badge indicating current AI connectivity and quota state.

**Props:**
- `status: AiStatus` — ONLINE / OFFLINE / QUOTA_REACHED / VALIDATING
- `lastUpdated: LocalDateTime?`

**Layout:** Row with icon + short text label. Icon: wifi / wifi_off / block / hourglass. Color matches status: green/grey/amber/blue.

**Behavior:** Never color-only — always has icon AND text label. Accessible contentDescription reads full status.

**Reuse locations:** DailyBriefScreen, AiSettingsScreen, AiDiagnosticsScreen

---

### 3.14 ScoreProgressBar

**Description:** Horizontal progress bar with percentage label.

**Props:**
- `value: Float` — 0.0–1.0
- `label: String?`
- `color: Color?` — if null, uses theme primary
- `showPercentage: Boolean` — default true
- `animateChanges: Boolean` — default true

**Layout:** Full-width bar (8dp height, 4dp corner radius) with optional label above and percentage text to the right. Animates value changes with 300ms tween.

**Reuse locations:** AttendanceAnalyticsScreen, CpDashboardScreen (rating progress), ProjectCard, MilestoneScreen, KnowledgeTreeItem (mastery), DsaTopicDetailScreen

---

### 3.15 ConfirmDialog

**Description:** Standard Material 3 AlertDialog for destructive or irreversible actions.

**Props:**
- `title: String`
- `message: String`
- `confirmLabel: String` — e.g., "Delete", "Proceed"
- `dismissLabel: String` — default "Cancel"
- `isDestructive: Boolean` — if true, confirm button uses error color
- `onConfirm: () -> Unit`
- `onDismiss: () -> Unit`

**Layout:** Dialog (28dp corner radius, 6dp elevation). Title (TitleLarge), message (BodyMedium), action buttons row.

**Reuse locations:** Assignment delete (PENDING/IN_PROGRESS), Attendance future marking, Project archive/delete, OCR cancel, Backup restore, ContestReflection back, AiDiagnostics clear log, EditTimetable slot delete

---

### 3.16 NewNextActionSheet

**Description:** Bottom sheet that forces user to define the next action before dismissing a completed action.

**Props:**
- `projectName: String`
- `completedAction: String`
- `onConfirm: (newAction: String) -> Unit`
- `onDismiss: () -> Unit`

**Layout:** Bottom sheet (16dp top corner radius). Header "What's next for [project]?". Text field for new action. Optional quick-pick chips (common next actions). "Set Next Action" filled button.

**Behavior:** Cannot be dismissed by back press without entering a next action (enforced). Dismissing without entry shows inline error.

**Reuse locations:** ProjectDetailScreen → NextActionBanner "Done" tap

---

### 3.17 StatusChip

**Description:** Color-coded chip for displaying status values.

**Props:**
- `status: String` — display text
- `variant: StatusVariant` — determines color mapping (POSITIVE / NEGATIVE / NEUTRAL / WARNING / INFO)

**Layout:** Small filled chip (4dp radius). Icon optional (leading). Text LabelMedium.

**Behavior:** Color is never the only indicator — variant also maps to icon (check / close / info / warning / schedule).

**Reuse locations:** Nearly all screens — assignments, attendance, projects, coding, settings

---

### 3.18 PriorityBadge

**Description:** Small badge indicating task/assignment priority.

**Props:**
- `priority: Priority` — LOW / MEDIUM / HIGH / CRITICAL

**Layout:** Mini chip with icon: `keyboard_arrow_down` (LOW), `remove` (MEDIUM), `keyboard_arrow_up` (HIGH), `priority_high` (CRITICAL). Color from priority severity.

**Reuse locations:** AssignmentCard, AssignmentDetailScreen, BugScreen

---

### 3.19 EmptyStateView

**Description:** Standardized empty state with illustration placeholder, title, message, and optional action.

**Props:**
- `icon: ImageVector` — Material Symbol icon (large, 64dp)
- `title: String`
- `message: String`
- `actionLabel: String?`
- `onAction: (() -> Unit)?`

**Layout:** Centered column. Icon (64dp, tinted onSurface at 38%). Title (TitleMedium). Message (BodyMedium, center-aligned). Optional FilledTonalButton.

**Reuse locations:** All screens with list content

---

### 3.20 LoadingOverlay

**Description:** Full-screen semi-transparent overlay with centered CircularProgressIndicator and optional label.

**Props:**
- `isVisible: Boolean`
- `label: String?` — e.g., "Generating brief…"
- `isDimmed: Boolean` — default true (scrim behind indicator)

**Layout:** Box overlay. Scrim at 32% black. Center: CircularProgressIndicator + label below (BodyMedium).

**Behavior:** Prevents interaction with content beneath while visible. Announced to TalkBack as "Loading, please wait."

**Reuse locations:** OcrPreviewScreen (processing), BackupScreen (export/restore), AiSettingsScreen (key validation), DailyBriefScreen (initial generation)

---

---

## Section 4: User Flows

### Flow 1: First Launch / Onboarding

1. User installs and opens app for the first time.
2. App checks Room database — empty, no timetable, no profile.
3. App navigates to DailyBriefScreen (default tab).
4. GuidanceCard shows fallback "Welcome" message: "Set up your timetable to get started."
5. AiStatusBadge shows "AI Offline — API key not configured."
6. App shows an onboarding banner below GuidanceCard with three setup steps: "1. Add your timetable → 2. Set up CP profile → 3. Configure AI key."
7. User taps "Add Timetable" chip → navigates to EditTimetableScreen.
8. User adds class slots manually or taps "Import via Camera."
9. After at least one day is set up, user returns to Attendance tab → WeeklyViewScreen shows the timetable.
10. User navigates to Settings → AI Settings, enters DeepSeek API key, saves.
11. App validates key (LoadingOverlay "Validating…"), AiStatusBadge updates to "AI Online."
12. On next morning trigger, DailyBriefScreen generates first real brief.
13. Onboarding banner dismisses after all three steps are completed.

---

### Flow 2: OCR Timetable Import

1. User is on EditTimetableScreen.
2. User taps overflow menu → "Import via Camera."
3. App requests camera permission if not granted; if denied, shows rationale dialog.
4. Camera viewfinder opens (system camera intent or CameraX).
5. User photographs the printed timetable or screenshot.
6. App shows LoadingOverlay "Processing image…" on OcrPreviewScreen while ML Kit processes.
7. OcrPreviewScreen displays extracted fields. Low-confidence fields (< 70%) have amber borders and ConfidenceLevelIndicator in amber/red.
8. Low-confidence warning banner appears at top if any field < 70%.
9. User edits incorrect fields inline.
10. User optionally toggles "Import only high-confidence fields" switch to skip uncertain entries.
11. User taps "Import" button (top bar action, enabled when no blocking errors).
12. App saves parsed slots to Room, navigates back to EditTimetableScreen.
13. EditTimetableScreen shows updated slot list. Snackbar: "Timetable imported — 12 slots added."
14. If user taps Cancel at step 11 → returns to EditTimetableScreen with no changes.

---

### Flow 3: Attendance Marking

**Present / Absent (past or today):**
1. User opens WeeklyViewScreen, selects the day tab.
2. User sees ClassEventCards for that day.
3. User taps the StatusChip on a ClassEventCard.
4. Status cycles: NOT_MARKED → PRESENT → ABSENT → NOT_MARKED.
5. AttendancePercentageRow updates within 500ms (debounced recomposition).
6. BunkCalculatorWidget footer updates with new canSkip / mustAttend count.
7. Change persists to Room immediately.
8. Swipe right on card = mark Present, swipe left = mark Absent. Undo snackbar shown for 4 seconds.

**Future class marking:**
1. User taps StatusChip on a ClassEventCard where `isFuture = true`.
2. ConfirmDialog appears: "You are marking a future class. This will affect your forecast. Continue?"
3. User confirms → status changes. User cancels → no change.

**Extra / makeup class:**
1. User taps FAB on WeeklyViewScreen.
2. Bottom sheet opens: date picker, time range pickers, subject selector.
3. User fills in fields and taps "Add."
4. New ClassEventCard appears in the list. AttendancePercentageRow recalculates.

---

### Flow 4: Assignment Creation

1. User is on AssignmentListScreen.
2. User taps FAB (+).
3. Bottom sheet appears with form: Title (required), Subject (required), Due Date (required), Priority (default MEDIUM), Description (optional).
4. User fills in fields.
5. Taps "Create."
6. New AssignmentCard appears in PENDING group. List re-sorts by due date.
7. If due date is within 24 hours, DeadlineCountdown chip shows in red.
8. Snackbar: "Assignment created."

---

### Flow 5: Project Creation and Next Action

1. User is on ProjectListScreen.
2. User taps FAB (+).
3. Bottom sheet: Project name (required), Description, Status (default ACTIVE), Tags (optional chips).
4. User fills in and taps "Create."
5. ProjectDetailScreen opens for the new project (auto-navigate).
6. Overview tab shows NextActionBanner with placeholder "No next action set."
7. User taps "Set Next Action" → inline text field or NewNextActionSheet appears.
8. User types next action text, taps "Set."
9. NextActionBanner updates with the new next action text.
10. ProjectListScreen shows this project's next action on its ProjectCard.
11. When user completes the next action (taps "Done" on NextActionBanner), NewNextActionSheet appears.
12. User must enter the new next action before the sheet can be dismissed.
13. New action is saved; banner updates. Project "last activity" timestamp refreshes.

---

### Flow 6: CP Profile Setup and Sync

1. User navigates to CpDashboardScreen (Coding tab).
2. EmptyStateView shown: "No CP profile. Set up in Settings."
3. User navigates to Settings → AI Settings (or a direct "Set up profile" button).
4. User enters platform (CodeChef or Codeforces), handle/username.
5. Taps "Sync" → app fetches profile data (if online) with LoadingOverlay.
6. On success: rating, rank, max rating stored in Room. Snackbar "Profile synced."
7. CpDashboardScreen now shows CP Profile card with ScoreProgressBar for rating.
8. If offline during sync: "Sync failed — will retry when online." Snackbar. Profile not updated.
9. Subsequent sync runs in background on next app open if last sync > 24 hours ago.

---

### Flow 7: Contest Reflection

1. User completes a contest (external to app).
2. User opens CpDashboardScreen. If sync finds a new contest result, it appears in "Recent" list.
3. Alternatively, user taps FAB on CpDashboardScreen → "Add Contest Result" bottom sheet (name, date, rank, rating change, problems solved).
4. User taps the new contest card → ContestReflectionScreen.
5. Contest info is pre-populated (name, date, platform, metrics).
6. User fills in "What went well," "What to improve," adds problems to revisit, tags topics.
7. Taps Save (top bar checkmark) → saved to Room.
8. Back navigation to CpDashboardScreen. DSA progress bars update if topics were tagged.
9. If user presses Back without saving → ConfirmDialog "Discard changes?"

---

### Flow 8: Daily Brief Generation

**Morning path (LLM online):**
1. WorkManager job triggers at user-configured time (default 7:00 AM).
2. App checks: is recommendation cache fresh (< 6 hours)? → No.
3. App checks AiStatus: Online, quota not exceeded.
4. App queries Room for today's context: classes, deadlines, upcoming contests, project next actions.
5. Builds prompt, calls DeepSeek API.
6. Response stored in Room (ai_call_log + brief_cache table).
7. Notification sent on channel DAILY_BRIEF: "Your Daily Brief is ready."
8. User taps notification → DailyBriefScreen opens.
9. GuidanceCard shows AI text. AiStatusBadge shows "AI Online, updated at 7:03 AM."

**Offline / quota exceeded fallback path:**
1. Same trigger as above.
2. App checks: AiStatus = OFFLINE or QUOTA_REACHED.
3. App runs deterministic fallback generator: reads Room data and builds structured text summary.
4. Fallback brief stored in cache with source = FALLBACK.
5. DailyBriefScreen shows fallback text. AiStatusBadge shows "AI Offline — using local plan."
6. No API call made. No cost incurred.

---

### Flow 9: Intra-Day AI Recommendation Update

1. An AppEvent fires during the day (e.g., user marks assignment complete, marks all classes for the day).
2. EventBus receives the event, emits to IntelligenceRepository.
3. Repository checks: is cached brief older than 2 hours OR does event type warrant immediate update?
4. If cache is still fresh → silent reuse, no API call. AiStatusBadge stays unchanged.
5. If update warranted: checks AI availability. If online → calls DeepSeek with updated context.
6. Response replaces recommendation section of GuidanceCard.
7. If user is currently on DailyBriefScreen → GuidanceCard animates to new content (fade transition).
8. If user is on another screen → update is silent. New content available on next visit.
9. If offline → deterministic recalculation only. No API call.

---

### Flow 10: Backup Export

1. User navigates to Settings → Backup & Restore → BackupScreen.
2. User taps "Export Backup."
3. App exports Room database + media attachments (if any) as a `.zip` file.
4. LoadingOverlay "Exporting…" shown.
5. File saved to `Downloads/studentos_backup_YYYY-MM-DD.zip`.
6. LoadingOverlay dismisses. Snackbar: "Backup saved to Downloads."
7. Backup history list updates with new entry (filename, size, timestamp).

---

### Flow 11: Backup Restore

1. User navigates to BackupScreen.
2. User taps "Restore from File."
3. System file picker opens, filtered to `.zip`.
4. User selects backup file.
5. ConfirmDialog: "Restoring will overwrite ALL current data. This cannot be undone. Proceed?"
6. User confirms → LoadingOverlay "Restoring…" shown.
7. App validates backup zip structure. If invalid: error snackbar "Invalid backup file." — no data changed.
8. If valid: Room database is replaced with backup data. App restarts or re-initializes repositories.
9. Snackbar: "Restore complete. Data updated to [backup date]."
10. If user cancels at ConfirmDialog → returns to BackupScreen, no change.

---

---

## Section 5: Material 3 Design Guidelines

### 5.1 Typography Scale

| Token | Style | Font Size | Line Height | Weight | Use |
|---|---|---|---|---|---|
| `displaySmall` | DisplaySmall | 36sp | 44sp | Regular | Major numeric stats (e.g., rating) |
| `headlineLarge` | HeadlineLarge | 32sp | 40sp | Regular | Rarely used; fullscreen headings |
| `headlineMedium` | HeadlineMedium | 28sp | 36sp | Regular | Section headings in analytics |
| `headlineSmall` | HeadlineSmall | 24sp | 32sp | Regular | Card headings |
| `titleLarge` | TitleLarge | 22sp | 28sp | Regular | Screen titles in top bars |
| `titleMedium` | TitleMedium | 16sp | 24sp | Medium | Card titles, assignment titles |
| `titleSmall` | TitleSmall | 14sp | 20sp | Medium | Sub-section titles, list item primary |
| `bodyLarge` | BodyLarge | 16sp | 24sp | Regular | Primary body text |
| `bodyMedium` | BodyMedium | 14sp | 20sp | Regular | Standard content text |
| `bodySmall` | BodySmall | 12sp | 16sp | Regular | Secondary content, card subtitles |
| `labelLarge` | LabelLarge | 14sp | 20sp | Medium | Button labels |
| `labelMedium` | LabelMedium | 12sp | 16sp | Medium | Chip labels, tabs |
| `labelSmall` | LabelSmall | 11sp | 16sp | Medium | Timestamps, metadata, badges |

All sizes are sp (scale-independent pixels) and scale with system font size up to 200%.

---

### 5.2 Spacing System

Base unit: 4dp. All spacing values are multiples of 4.

| Token | Value | Use |
|---|---|---|
| `space2` | 2dp | Icon-to-text micro gaps |
| `space4` | 4dp | Intra-component gaps (icon + label) |
| `space8` | 8dp | Between related elements within a card |
| `space12` | 12dp | Card internal padding (vertical) |
| `space16` | 16dp | Standard horizontal screen margin, card padding |
| `space24` | 24dp | Between sections, card vertical gaps |
| `space32` | 32dp | Large section breaks |
| `space48` | 48dp | Minimum touch target height |

Screen edge margins: 16dp horizontal on phones. 24dp on tablets (600dp+).

---

### 5.3 Color Tokens

Material 3 uses roles, not fixed hex values. Dynamic color (Android 12+) seeds the palette from the wallpaper. Below are the semantic roles used in the app.

| Token | Light Role | Dark Role | Use |
|---|---|---|---|
| `primary` | Brand blue-purple | Lighter brand | FABs, filled buttons, active nav |
| `onPrimary` | White | Dark | Text/icons on primary |
| `primaryContainer` | Light tinted | Dark tinted | Chip backgrounds, selected states |
| `onPrimaryContainer` | Dark | Light | Text on primaryContainer |
| `secondary` | Muted teal | Lighter teal | Secondary chips, less-emphasis elements |
| `onSecondary` | White | Dark | Text on secondary |
| `secondaryContainer` | Light teal tint | Dark teal | Supporting containers |
| `tertiary` | Warm orange | Light warm | Accent highlights (contests, streaks) |
| `tertiaryContainer` | Light warm tint | Dark warm | Contest cards background |
| `error` | Red-600 | Red-300 | Errors, destructive actions, overdue |
| `errorContainer` | Red-100 | Red-900 | Error surface backgrounds |
| `onError` | White | Dark | Text on error |
| `surface` | White-98 | Dark-6 | Card backgrounds |
| `surfaceVariant` | Grey-90 | Dark-12 | Input fields, tonal chip backgrounds |
| `onSurface` | Dark | Light | Primary text on surface |
| `onSurfaceVariant` | Dark-60 | Light-60 | Secondary text, icons |
| `outline` | Grey-50 | Light-30 | Borders, dividers |
| `outlineVariant` | Grey-80 | Dark-20 | Subtle dividers |
| `background` | White-99 | Dark-4 | Screen background |
| `onBackground` | Dark | Light | Text on background |
| `inverseSurface` | Dark | Light | Snackbar background |
| `inverseOnSurface` | Light | Dark | Snackbar text |

**Attendance-specific semantic colors (applied on top of role system):**
- Present → `tertiary` (green tint)
- Absent → `error` (red)
- Cancelled → `onSurfaceVariant` (grey)
- Not Marked → `outline`

---

### 5.4 Elevation Levels

| Level | Value | Use |
|---|---|---|
| Flat | 0dp | Cards (standard content cards, list items) |
| Nav bar | 1dp | Bottom navigation bar |
| Top bar | 2dp | Top app bar (adds tonal tint on scroll) |
| FAB | 3dp | Floating action button |
| Dialog | 6dp | Modal dialogs, bottom sheets |
| Tooltip | 8dp | Tooltips (if used) |

Cards in this app are predominantly flat (0dp) per Material 3 best practice. Tonal elevation (color tint) distinguishes hierarchy instead of shadow.

---

### 5.5 Corner Radius Per Component Type

| Component | Corner Radius |
|---|---|
| Filled / Outlined Buttons | 4dp (slightly rounded) |
| Text Buttons | 4dp |
| Small chips | 8dp (fully rounded for small chips) |
| Cards (standard) | 12dp |
| List item cards | 8dp |
| Bottom Sheets | 16dp top corners only |
| Dialogs | 28dp |
| FABs (regular) | 16dp |
| FABs (extended) | 16dp |
| FABs (large) | 28dp |
| Text fields | 4dp top corners |
| Progress bars | 4dp |
| Navigation bar | 0dp (full bleed) |

---

### 5.6 Icon Style

- Library: Material Symbols (variable font, replaces older Material Icons)
- Style: **Outlined** for all navigation and UI icons
- Fill: Filled variant used for the **active** bottom nav tab icon only
- Weight: 400 (default)
- Grade: 0
- Optical size: 24dp for standard icons, 20dp for inline/badge icons, 48dp for empty state icons
- All icon-only buttons must have `contentDescription` set

---

### 5.7 Animation Durations

| Animation Type | Duration | Easing |
|---|---|---|
| Screen enter transition | 300ms | EmphasizedDecelerate |
| Screen exit transition | 200ms | EmphasizedAccelerate |
| Shared element transition | 500ms | Emphasized |
| Bottom sheet expand | 300ms | EmphasizedDecelerate |
| Bottom sheet collapse | 250ms | EmphasizedAccelerate |
| Dialog enter | 300ms | EmphasizedDecelerate |
| Dialog exit | 200ms | EmphasizedAccelerate |
| FAB expand (to extended) | 200ms | Standard |
| Progress bar update | 300ms | Linear |
| Chip selection | 100ms | Standard |
| List item appear | 150ms staggered | EmphasizedDecelerate |
| Snackbar slide up | 200ms | Decelerate |
| Snackbar slide down | 150ms | Accelerate |
| AttendancePercentageRow recalc | 300ms tween | Linear |
| ScoreProgressBar value change | 300ms tween | FastOutSlowIn |

Reduce motion: All transitions respect `AccessibilityManager.isAnimationEnabled`. If animations disabled, transitions are instant.

---

### 5.8 Dark Mode Behavior

- App supports System (follows OS), Light, and Dark modes via Settings → Theme.
- Dark mode uses Material 3 dark color scheme (dark backgrounds, lighter content).
- All custom semantic colors (attendance status, priority, mastery) have dark variants defined.
- Images and illustrations use `ColorFilter` to adapt in dark mode.
- Elevation in dark mode shows tonal color overlay instead of shadow.
- Bottom nav and top bars use `surfaceColorAtElevation` to show proper dark tonal values.
- ConfidenceLevelIndicator amber: adapts to `tertiaryContainer` in dark mode.

---

### 5.9 Dynamic Color Behavior

- On Android 12+ (API 31+): `DynamicColors.applyToActivityIfAvailable()` is called.
- The wallpaper seeds a palette; Material 3 generates all color roles from it automatically.
- App does not override seed color on Android 12+.
- On Android < 12: A default seed color (blue-purple, matching Student OS brand) is used.
- Dynamic color is user-toggleable in Settings → Appearance → "Dynamic Color" (shown only on API 31+).
- When disabled: falls back to default seed color palette.

---

### 5.10 Tablet (600dp+) Layout Changes

- Bottom navigation bar converts to a **Navigation Rail** on the left side.
- Settings icon moves from top bar to the bottom of the Navigation Rail.
- Screen content uses a two-pane layout where applicable:
  - AssignmentListScreen (left) + AssignmentDetailScreen (right)
  - ProjectListScreen (left) + ProjectDetailScreen (right)
  - KnowledgeTreeScreen (left) + DsaTopicDetailScreen (right)
  - SettingsScreen (left) + Sub-setting screen (right)
- Single-pane screens (DailyBrief, Attendance) center content with max width 840dp.
- Horizontal screen margins increase to 24dp on 600dp and 32dp on 840dp+.

---

### 5.11 Landscape Behavior

- Phone landscape: Bottom nav remains visible. Content area shrinks vertically.
- Screens with scrollable content (most screens) handle well.
- Screens with tall forms (EditTimetable, ContestReflection) add keyboard scroll handling.
- OcrPreviewScreen: Camera viewfinder adapts to landscape aspect ratio.
- Bottom sheets: max height 90% of screen in landscape.
- Dialogs: constrained to 480dp max width to prevent spanning the full screen.

---

---

## Section 6: State Management Per Screen

Each screen's UI state is described across five conditions.

| Screen | Loading | Success | Empty | Error | Offline |
|---|---|---|---|---|---|
| **DailyBriefScreen** | GuidanceCard shimmer, AiStatusBadge "Generating…" | Full guidance card, recommendations, deadlines strip | "No brief yet. Tap refresh." | "Could not load brief. Check API key." with retry | Fallback brief shown, AiStatusBadge "AI Offline" |
| **BriefHistoryScreen** | Shimmer list of 3 collapsed cards | Date-grouped list of GuidanceCards | "No history yet." | "Could not load history." with retry | Shows all locally cached briefs |
| **WeeklyViewScreen** | Shimmer for 4 ClassEventCards | Day tabs + ClassEventCards + BunkCalculator | "No classes scheduled for this day." | "Could not load timetable." with retry | Fully functional (all local) |
| **CalendarViewScreen** | Shimmer grid | Month grid + selected day detail | "No attendance data for this month." | "Could not load calendar data." with retry | Fully functional |
| **AttendanceAnalyticsScreen** | Shimmer for 3 subject cards | Summary card + per-subject rows + trends | "No attendance recorded yet." | "Could not compute analytics." with retry | Fully functional |
| **OcrPreviewScreen** | Full LoadingOverlay "Processing image…" before screen appears | Extracted fields list, confidence indicators | N/A (screen always has data) | "OCR failed. Try again." with retry | Fully offline (ML Kit) |
| **EditTimetableScreen** | Shimmer list | Day tabs + slot list | "No classes added yet. Tap + to add." | "Could not save timetable." with retry | Fully functional |
| **AssignmentListScreen** | Shimmer 3 AssignmentCards | Status-grouped list | "No assignments yet. Tap + to add." | "Could not load assignments." with retry | Fully functional |
| **AssignmentDetailScreen** | Full shimmer skeleton | All sections populated | N/A | "Could not load assignment." with retry | Fully functional |
| **CpDashboardScreen** | Shimmer profile card + 3 contest cards | Profile card + contests + DSA progress | "No CP profile set up. Add handle in Settings." | "Could not load CP data." with retry | Cached data shown with "Last synced: X" label |
| **ContestReflectionScreen** | Shimmer form skeleton | Pre-populated form fields | N/A | "Could not load contest data." with retry | Fully functional |
| **KnowledgeTreeScreen** | Shimmer 3 category groups | Hierarchical topic list | "No topics found. Start solving problems." | "Could not load knowledge tree." with retry | Fully functional |
| **DsaTopicDetailScreen** | Shimmer | All sections | "No problems added to this topic yet." | "Could not load topic data." with retry | Fully functional |
| **ProjectListScreen** | Shimmer 3 ProjectCards | ProjectCard list + NextActionBanner | "No projects yet. Tap + to create one." | "Could not load projects." with retry | Fully functional |
| **ProjectDetailScreen** | Shimmer per tab | Tabbed content (Overview/Milestones/Bugs) | Per tab: "No milestones." / "No bugs." | "Could not load project." with retry | Fully functional |
| **MilestoneScreen** | Shimmer | Task list + notes | "No tasks in this milestone." | "Could not load milestone." with retry | Fully functional |
| **BugScreen** | Shimmer | All form sections | N/A | "Could not load bug." with retry | Fully functional |
| **SettingsScreen** | N/A (static list) | Preference groups | N/A | N/A | N/A (fully local) |
| **AiSettingsScreen** | LoadingOverlay during key validation | Settings form | N/A | "Invalid API key." inline error | Can edit offline; validation deferred |
| **AiDiagnosticsScreen** | Shimmer list | Summary card + log entries | "No AI calls logged yet." | "Could not load call log." with retry | Shows cached log |
| **NotificationSettingsScreen** | N/A | Toggle list | N/A | N/A | Fully local |
| **BackupScreen** | LoadingOverlay during export/restore | Action cards + history list | "No backups created yet." (history section) | "Backup failed. Storage permission may be required." | Fully local |

---

### 6.1 State Handling Rules

- **Loading:** Always show skeleton/shimmer that matches the shape of real content. Never show a blank screen.
- **Error:** Always provide a retry action. Never show a dead end. Error message uses `onSurface` color with `error` icon.
- **Empty:** Always explain why it's empty and how to fix it. Provide an action button where applicable.
- **Offline:** This app is offline-first. "Offline" is the normal operating state. Network is only needed for AI calls and CP profile sync. The UI must never degrade significantly when offline.
- **Loading + Error overlay rule:** Error states replace content, not overlay it. LoadingOverlay is only for blocking operations (OCR, export, restore, key validation).

---

---

## Section 7: Accessibility Specification

### 7.1 contentDescription Rules

Every icon-only button (no visible text label) must have a `contentDescription`. The following table covers all icon-only interactive elements.

| Screen | Icon | contentDescription |
|---|---|---|
| DailyBriefScreen | history icon | "View brief history" |
| DailyBriefScreen | settings icon | "Open settings" |
| WeeklyViewScreen | calendar_month icon | "Switch to calendar view" |
| WeeklyViewScreen | bar_chart icon | "View attendance analytics" |
| WeeklyViewScreen | FAB add icon | "Add extra class" |
| WeeklyViewScreen | ClassEventCard status chip | "Attendance status: [value]. Tap to change." |
| CalendarViewScreen | day cell (color-coded) | "Day [date], attendance: [green/amber/red/no class]" |
| AttendanceAnalyticsScreen | subject expand icon | "Expand [subject name] analytics" |
| EditTimetableScreen | slot edit icon | "Edit [subject] slot at [time]" |
| EditTimetableScreen | slot delete icon | "Delete [subject] slot" |
| EditTimetableScreen | FAB | "Add new class slot" |
| AssignmentListScreen | filter_list icon | "Filter assignments" |
| AssignmentListScreen | sort icon | "Sort assignments" |
| AssignmentListScreen | FAB | "Create new assignment" |
| AssignmentDetailScreen | edit icon | "Edit assignment" |
| AssignmentDetailScreen | more_vert icon | "More options" |
| CpDashboardScreen | leaderboard icon | "View knowledge tree" |
| CpDashboardScreen | FAB | "Add contest result" |
| ContestReflectionScreen | save checkmark | "Save reflection" |
| KnowledgeTreeScreen | search icon | "Search topics" |
| ProjectListScreen | filter icon | "Filter projects" |
| ProjectListScreen | FAB | "Create new project" |
| ProjectDetailScreen | edit icon | "Edit project" |
| ProjectDetailScreen | more_vert | "More options" |
| AiDiagnosticsScreen | delete_sweep | "Clear all AI call logs" |
| OcrPreviewScreen | confidence bar | "Confidence: [X]%. [High/Medium/Low] confidence." |
| All screens | back arrow | "Navigate back" (system default, but verify) |

---

### 7.2 Touch Target Rules

- Minimum touch target: 48dp × 48dp for all interactive elements.
- If the visual size of an element is smaller than 48dp (e.g., a 24dp icon), add invisible padding around it to meet the 48dp minimum.
- Chips with short labels: ensure minimum 48dp height.
- TaskList checkboxes: 48dp touch target even though visual size is 24dp.
- StatusChip on ClassEventCard: must be at least 48dp tall.
- ConfidenceLevelIndicator bar: if tappable, 48dp touch target.
- Bottom nav items: system-managed, but verify each tab hit area is ≥ 48dp.

---

### 7.3 TalkBack Traversal Order

TalkBack reads elements top-to-bottom, left-to-right, matching visual order. For each screen, the correct traversal order is:

**DailyBriefScreen:** Top bar title → history button → settings button → AiStatusBadge + timestamp → GuidanceCard (read as single block) → Today's Snapshot → Recommendations (each chip) → Deadlines strip (each chip)

**WeeklyViewScreen:** Top bar title → analytics button → calendar button → settings button → Day tabs (Monday through Saturday) → AttendancePercentageRow → ClassEventCards (in time order, each card: subject → time → room → status) → BunkCalculatorWidget

**AssignmentListScreen:** Top bar → filter chip row → OVERDUE group header → OVERDUE cards (each: title → subject → deadline → status → priority) → IN_PROGRESS group → IN_PROGRESS cards → PENDING group → PENDING cards → COMPLETED group → FAB

**ProjectDetailScreen:** Back button → project name → edit button → more options → tab bar (Overview/Milestones/Bugs) → [tab content in visual order] → FAB (if present)

**OcrPreviewScreen:** Cancel button → "Review Import" title → Import button → Capture thumbnail → Warning banner (if present) → Each extracted field in order: label → text field → confidence indicator → Partial import toggle

**General rule:** Decorative elements (dividers, background shapes, illustration shapes) must have `contentDescription = null` and `semantics { clearAndSetSemantics {} }` to be excluded from traversal.

---

### 7.4 Heading Semantics

Use `semantics { heading() }` modifier on section titles to allow TalkBack users to navigate by headings.

| Screen | Element | Heading level |
|---|---|---|
| DailyBriefScreen | "Today's Snapshot" | Section heading |
| DailyBriefScreen | "Recommendations" | Section heading |
| AttendanceAnalyticsScreen | Each subject name in analytics | Section heading |
| CpDashboardScreen | "Upcoming Contests" | Section heading |
| CpDashboardScreen | "Recent Contests" | Section heading |
| CpDashboardScreen | "DSA Progress" | Section heading |
| ProjectDetailScreen | "Overview" / "Milestones" / "Bugs" tab labels | Navigation heading |
| KnowledgeTreeScreen | Each category name | Section heading |
| SettingsScreen | Each settings group title | Section heading |
| BriefHistoryScreen | Each date group header | Section heading |

---

### 7.5 Color Contrast Requirements

- Body text (`bodyMedium`, `bodySmall`, `labelSmall`) on any background: minimum **4.5:1** contrast ratio.
- Large text (`titleLarge`, `headlineMedium` and above) on any background: minimum **3:1** contrast ratio.
- UI components (buttons, chips, focus indicators): minimum **3:1** against adjacent colors.
- Error text and error icons: must meet 4.5:1 ratio.
- Attendance status colors (present/absent/cancelled): must not rely solely on color. Each status has an icon AND a text label.
- AiStatusBadge: icon + text, never just colored dot.
- PriorityBadge: icon + optional text, never just color.
- DeadlineCountdown chip: color changes (green/amber/red) always paired with time-remaining text.
- All color combinations are validated in both light and dark mode.

---

### 7.6 Font Scaling Behavior

- All text uses `sp` (scale-independent pixels) for font size.
- App supports font scaling up to 200% (as required by Android accessibility).
- At 200% scale:
  - Cards may wrap to more lines. Card `minHeight` is used, not fixed height.
  - Chips with short labels remain fully visible (no truncation).
  - Bottom nav labels must remain visible (not truncated). If label doesn't fit, use a slightly smaller `labelSmall` variant but never hide the label.
  - Top bar title may ellipsize at 2 lines max.
  - BunkCalculatorWidget: two-column layout switches to stacked single-column layout at > 150% scale.
  - LoadingOverlay label: wraps to 2 lines if needed.
- Fixed-pixel heights must not be used for text-containing components. Use `wrapContentHeight` with `padding`.

---

### 7.7 Focus Management After Dialogs

When a dialog or bottom sheet is dismissed, focus must return to a logical element:

| Trigger | Dialog / Sheet | Focus returns to |
|---|---|---|
| Delete assignment (confirmed) | ConfirmDialog | First AssignmentCard in list, or EmptyStateView if list now empty |
| Delete assignment (cancelled) | ConfirmDialog | The assignment card that was being deleted |
| Future class marking (confirmed) | ConfirmDialog | The ClassEventCard whose status was changed |
| Future class marking (cancelled) | ConfirmDialog | The ClassEventCard that was tapped |
| Project archive (confirmed) | ConfirmDialog | Next ProjectCard in list, or EmptyStateView |
| Backup restore (confirmed) | ConfirmDialog | "Restore from File" button on BackupScreen |
| Backup restore (cancelled) | ConfirmDialog | "Restore from File" button |
| NewNextActionSheet (confirmed) | Bottom sheet | NextActionBanner (now showing new action) |
| Add class slot (confirmed) | Bottom sheet | Newly added ClassEventCard in EditTimetableScreen |
| Add assignment (confirmed) | Bottom sheet | New AssignmentCard in list |
| OCR Import (confirmed) | Navigates back | First field in EditTimetableScreen |
| Contest reflection back with unsaved | ConfirmDialog (discard) | ContestReflectionScreen title or CpDashboardScreen after pop |

---

---

## Section 8: UX Rules

### 8.1 Back Navigation Behavior Per Screen

| Screen | Back action | Behavior |
|---|---|---|
| DailyBriefScreen | System back | App minimizes (not killed). DailyBrief is the root. |
| BriefHistoryScreen | Back arrow | Pop back to DailyBriefScreen |
| WeeklyViewScreen | System back | App minimizes |
| CalendarViewScreen | Back arrow | Pop back to WeeklyViewScreen |
| AttendanceAnalyticsScreen | Back arrow | Pop back to WeeklyViewScreen |
| OcrPreviewScreen | Back arrow / Cancel | ConfirmDialog only if user has made edits. Then pop to EditTimetableScreen. |
| EditTimetableScreen | Back arrow | ConfirmDialog "Discard unsaved changes?" if dirty. |
| AssignmentListScreen | System back | App minimizes |
| AssignmentDetailScreen | Back arrow | Pop to AssignmentListScreen. Auto-save changes inline. |
| CpDashboardScreen | System back | App minimizes |
| ContestReflectionScreen | Back arrow | ConfirmDialog if unsaved. Pop to CpDashboardScreen. |
| KnowledgeTreeScreen | Back arrow | Pop to CpDashboardScreen |
| DsaTopicDetailScreen | Back arrow | Pop to KnowledgeTreeScreen |
| ProjectListScreen | System back | App minimizes |
| ProjectDetailScreen | Back arrow | Pop to ProjectListScreen. Auto-save. |
| MilestoneScreen | Back arrow | Pop to ProjectDetailScreen (Milestones tab). Auto-save. |
| BugScreen | Back arrow | Pop to ProjectDetailScreen (Bugs tab). Auto-save. |
| SettingsScreen | Back arrow | Pop to previous screen (wherever settings was opened from) |
| AiSettingsScreen | Back arrow | Pop to SettingsScreen. Prompt to save unsaved key. |
| AiDiagnosticsScreen | Back arrow | Pop to AiSettingsScreen |
| NotificationSettingsScreen | Back arrow | Pop to SettingsScreen. Auto-saves toggles immediately. |
| BackupScreen | Back arrow | Pop to SettingsScreen |

---

### 8.2 Snackbar Rules

**When to use a snackbar:**
- Confirming a non-destructive action completed: "Assignment created.", "Backup saved."
- Undo opportunity: "Marked absent. [Undo]", "Assignment deleted. [Undo]"
- Non-blocking informational update: "Brief updated.", "Synced successfully."
- Transient warnings that don't need user decision: "Sync failed. Will retry online."

**When NOT to use a snackbar:**
- When user input is required (use a Dialog or bottom sheet instead)
- For errors that block the user from proceeding (use inline error state)
- For confirmations before destructive actions (use ConfirmDialog)

**Duration:**
- Short (4 seconds): Simple confirmations with no action ("Assignment created.")
- Long (6–8 seconds): When an action button ("Undo") is present

**Action button rules:**
- Only one action button per snackbar.
- Label is 1–2 words: "Undo", "Retry", "View", "Dismiss".
- Action label uses `inversePrimary` color.

**Snackbar queue:** If multiple snackbars fire in quick succession, queue them. Do not stack.

---

### 8.3 Dialog Rules

Use `ConfirmDialog` (AlertDialog) when:
- An action is irreversible or hard to undo (delete, restore from backup)
- The action has significant consequences (overwrite all data)
- The action affects data the user didn't explicitly target (marking a future class)
- The user has unsaved changes and is about to leave the screen

Do NOT use a dialog when:
- The action is reversible with Undo snackbar (swipe to dismiss, simple status change)
- The notification is purely informational (use snackbar instead)
- Asking for input (use bottom sheet instead)

Dialog anatomy:
- Title: Brief noun phrase, not a question. "Delete Assignment" not "Are you sure?"
- Message: One clear sentence about the consequence. "This assignment will be permanently deleted."
- Dismiss button: Always "Cancel" (secondary text button)
- Confirm button: Action verb matching the action. "Delete", "Restore", "Proceed". Destructive actions use `error` color.

---

### 8.4 Confirmation Prompts (Required Cases)

| Action | Prompt Required | Message |
|---|---|---|
| Delete PENDING or IN_PROGRESS assignment | Yes | "Delete this assignment? It cannot be undone." |
| Delete DONE assignment | No — snackbar with Undo instead | "Assignment deleted. [Undo]" |
| Mark a future class as Present/Absent | Yes | "Marking future classes affects your attendance forecast. Continue?" |
| Archive a project | Yes | "Archive this project? It will be hidden from your active list." |
| Delete a project permanently | Yes | "Delete this project and all its milestones and bugs? This cannot be undone." |
| Clear AI call log | Yes | "Clear all AI call history? This cannot be undone." |
| Restore from backup | Yes | "Restoring will overwrite ALL current data. This cannot be undone. Proceed?" |
| Cancel OCR import with edits | Yes | "Discard changes to this import?" |
| Leave ContestReflection with unsaved changes | Yes | "Discard unsaved changes?" |
| Leave EditTimetable with unsaved changes | Yes | "Discard unsaved changes?" |
| Delete a timetable slot | Yes | "Delete this class slot?" |

---

### 8.5 Delete Behavior

| Item | Delete trigger | Confirmation | Undo available |
|---|---|---|---|
| PENDING / IN_PROGRESS Assignment | Overflow menu or swipe | ConfirmDialog | No |
| DONE Assignment | Swipe left | No dialog | Yes (4s snackbar) |
| Timetable slot | Delete icon in EditTimetableScreen | ConfirmDialog | No |
| Project | Overflow menu → Delete | ConfirmDialog | No |
| Project (archive) | Overflow menu → Archive | ConfirmDialog | No |
| Milestone | Swipe left in Milestones tab | ConfirmDialog | No |
| Bug | Swipe left in Bugs tab | ConfirmDialog | No |
| Subtask / task item | Delete icon in TaskList | No dialog | Yes (inline undo within same session) |
| Problem in ContestReflection | Delete icon | No dialog | Inline: item reappears if tapping undo chip |
| AI Call Log (all) | Clear button in AiDiagnosticsScreen | ConfirmDialog | No |
| Backup history entry | N/A (backup history read-only in v1) | — | — |

---

### 8.6 Long Press Actions Per Screen

| Screen | Long press target | Action |
|---|---|---|
| AssignmentListScreen | AssignmentCard | Enter multi-select mode (checkbox appears on all cards) |
| ProjectListScreen | ProjectCard | Enter multi-select mode |
| DailyBriefScreen | GuidanceCard | Context menu: "Copy text" |
| WeeklyViewScreen | ClassEventCard | Context menu: "Edit slot", "Mark all [Subject] as Present today" |
| KnowledgeTreeScreen | KnowledgeTreeItem | Context menu: "Copy topic name", "Reset mastery" |
| AiDiagnosticsScreen | Log entry | Context menu: "Copy request", "Copy response" |
| BriefHistoryScreen | GuidanceCard | Context menu: "Copy text", "Delete this entry" |

Multi-select mode (AssignmentListScreen, ProjectListScreen):
- Top bar transforms: close icon (exit multi-select), count badge ("2 selected"), delete icon.
- All cards show checkbox overlay.
- Tapping another card toggles its selection.
- Exiting multi-select: X icon or back press.

---

### 8.7 Swipe Actions

| Screen | Swipe direction | Target | Action |
|---|---|---|---|
| WeeklyViewScreen | Left | ClassEventCard | Mark Absent (snackbar Undo) |
| WeeklyViewScreen | Right | ClassEventCard | Mark Present (snackbar Undo) |
| AssignmentListScreen | Left | AssignmentCard | Delete (PENDING/IN_PROGRESS: ConfirmDialog; DONE: snackbar Undo) |
| AssignmentListScreen | Right | AssignmentCard | Mark Complete (snackbar Undo if was PENDING/IN_PROGRESS) |
| ProjectListScreen | Left | ProjectCard | Archive (ConfirmDialog) |
| BriefHistoryScreen | Left | GuidanceCard | Delete that brief entry (ConfirmDialog) |
| ProjectDetailScreen (Milestones tab) | Left | Milestone row | Delete milestone (ConfirmDialog) |
| ProjectDetailScreen (Bugs tab) | Left | Bug row | Delete bug (ConfirmDialog) |

Swipe reveals a colored action background:
- Left swipe → error/red background with trash icon
- Right swipe → green background with check icon

---

### 8.8 Pull-to-Refresh Behavior

| Screen | Pull-to-refresh effect |
|---|---|
| DailyBriefScreen | Triggers new brief generation (respects AI availability and cache rules) |
| CpDashboardScreen | Triggers CP profile sync (online only; shows "Sync failed" snackbar if offline) |
| BriefHistoryScreen | Refreshes history list from Room (no network, instant) |
| AttendanceAnalyticsScreen | Recomputes analytics from Room (instant) |
| AiDiagnosticsScreen | Refreshes log from Room (instant) |
| All other screens | No pull-to-refresh (data is push-driven via Room Flow / StateFlow) |

Pull-to-refresh uses Material 3 `PullRefreshIndicator` style. Indicator uses `primary` color.

---

---

## Section 9: Notification Navigation

### 9.1 Notification Channel and Deep Link Table

| Notification Type | Channel ID | Channel Name | Importance | Action on Tap | Destination Screen | Parameters Passed |
|---|---|---|---|---|---|---|
| `DAILY_BRIEF` | `daily_brief_channel` | Daily Brief | DEFAULT | Open app to brief | DailyBriefScreen | None |
| `ASSIGNMENT_REMINDER` | `assignment_reminders` | Assignment Reminders | HIGH | Open specific assignment | AssignmentDetailScreen | `assignmentId: String` |
| `CLASS_REMINDER` | `class_reminders` | Class Reminders | HIGH | Open weekly view | WeeklyViewScreen | None |
| `CONTEST_REMINDER` | `contest_reminders` | Contest Reminders | DEFAULT | Open CP dashboard | CpDashboardScreen | None |
| `FREE_SLOT_RECOMMENDATION` | `daily_brief_channel` | Daily Brief | DEFAULT | Open brief (updated) | DailyBriefScreen | None (reuses daily brief channel) |
| `INACTIVE_PROJECT_REMINDER` | `project_reminders` | Project Reminders | DEFAULT | Open specific project | ProjectDetailScreen | `projectId: String` |

---

### 9.2 Deep Link URI per Notification

| Notification Type | Deep Link URI | Notes |
|---|---|---|
| `DAILY_BRIEF` | `studentos://daily_brief` | Opens tab 0 |
| `ASSIGNMENT_REMINDER` | `studentos://assignments/{assignmentId}` | assignmentId from notification data payload |
| `CLASS_REMINDER` | `studentos://attendance/weekly` | Opens tab 1, default day = today |
| `CONTEST_REMINDER` | `studentos://coding/cp` | Opens tab 3 |
| `FREE_SLOT_RECOMMENDATION` | `studentos://daily_brief` | Same as DAILY_BRIEF |
| `INACTIVE_PROJECT_REMINDER` | `studentos://projects/{projectId}` | projectId from notification data payload |

---

### 9.3 Navigation Behavior Details

**DAILY_BRIEF / FREE_SLOT_RECOMMENDATION:**
- If app is in background: bring to foreground, switch to tab 0.
- If app is killed: launch app, land on DailyBriefScreen.
- If already on DailyBriefScreen: scroll to top, refresh content.

**ASSIGNMENT_REMINDER:**
- Navigates directly to AssignmentDetailScreen with the given `assignmentId`.
- If the assignment no longer exists (deleted): land on AssignmentListScreen with snackbar "Assignment not found."
- Back navigation from AssignmentDetailScreen returns to AssignmentListScreen (not notification).

**CLASS_REMINDER:**
- Navigates to WeeklyViewScreen.
- Auto-selects today's day tab.
- Scrolls to the upcoming class slot that triggered the reminder.

**CONTEST_REMINDER:**
- Navigates to CpDashboardScreen.
- Scrolls to the contest card that is upcoming (highlighted with a pulsing border for 3 seconds then normalizes).

**INACTIVE_PROJECT_REMINDER:**
- Navigates to ProjectDetailScreen for the given `projectId`.
- If project no longer exists or is archived: land on ProjectListScreen with snackbar "Project not found."

---

### 9.4 Notification Content Templates

| Type | Title template | Body template |
|---|---|---|
| `DAILY_BRIEF` | "Your Daily Brief is ready ✨" | "[Day], [Date] — [brief opening line, max 80 chars]" |
| `ASSIGNMENT_REMINDER` | "Due soon: [Assignment Title]" | "[Subject] · Due in [N hours/days]" |
| `CLASS_REMINDER` | "[Subject] in [N min]" | "[Time] · [Room if available]" |
| `CONTEST_REMINDER` | "[Contest Name] starting soon" | "[Platform] · Starts in [N hours]" |
| `FREE_SLOT_RECOMMENDATION` | "You have a free slot 💡" | "Use the next [N min] to: [recommendation]" |
| `INACTIVE_PROJECT_REMINDER` | "[Project Name] needs attention" | "No activity for [N days]. Last action: [action text]" |

---

---

## Section 10: Future Expansion Hooks

### 10.1 Adding a 6th Bottom Navigation Tab

The bottom navigation bar currently has 5 tabs. Material 3 NavigationBar supports up to 5 items. To add a 6th tab without breaking existing navigation:

**Option A: NavigationDrawer upgrade**
- Replace the bottom NavigationBar with a NavigationDrawer (side menu) when the tab count exceeds 5.
- The bottom NavigationBar remains for the 5 most-used tabs. Less-used modules (Finance, Habits, Skill XP) live in the NavigationDrawer.
- Implement: Add a `menu` icon to the top bar. NavigationDrawer lists all modules. Transition: drawer slides in from left.
- No changes to existing routes, ViewModels, or screens.

**Option B: NavigationRail on all phones**
- Migrate from bottom NavigationBar to NavigationRail (left-side rail) on all screen sizes.
- NavigationRail supports 3–7 items.
- Trade-off: rail takes ~80dp of horizontal space. Acceptable on phones ≥ 360dp width.
- Implement: Replace `NavigationBar` composable with `NavigationRail`. All route references remain unchanged.

**Preferred approach for Student OS:** Option A. Keep bottom nav for the 5 primary tabs; drawer for additional modules. This preserves thumb-reach UX for the most-used screens.

To add a tab:
1. Add a new entry in the bottom nav tab definition table (Section 1.1).
2. Add a new nested graph in NavHost.
3. The new tab appears in the NavigationBar if total ≤ 5, or in the NavigationDrawer if total > 5.
4. No changes to existing tab indices — new tab appends at the end.

---

### 10.2 Reserved Route Namespace for Future Modules

The following route prefixes are reserved and must not be used by current screens:

| Module | Reserved Route Prefix | Status |
|---|---|---|
| Finance | `finance/` | Reserved, not implemented |
| Habits | `habits/` | Reserved, not implemented |
| Skill XP | `skill_xp/` | Reserved, not implemented |
| Social / Study Groups | `social/` | Reserved, not implemented |
| Timetable Sharing | `share/` | Reserved, not implemented |
| AI Chat | `ai_chat/` | Reserved, not implemented |

Any deep link or navigation call to a reserved route will route to a `PlaceholderScreen` (see Section 10.4) until that module is implemented. No crash, no 404 — always a graceful placeholder.

---

### 10.3 ModuleRegistry Extension Point UI Impact

The app has a `ModuleRegistry` (architectural concept) that declares which modules are active at runtime. The UI responds to this registry in the following ways:

**Bottom navigation:** The navigation bar renders tabs only for modules listed as `ENABLED` in `ModuleRegistry`. Disabled modules are hidden from the nav bar automatically. No hardcoded tab list — the bar is built from the registry at runtime.

**Settings screen:** A "Modules" section in SettingsScreen lists all registered modules with enable/disable toggles. Disabling a module hides its nav tab and disables all related notifications. Enabling a module re-shows the tab and reactivates notifications.

**Notification channels:** Each module registers its notification channels on install. Disabled modules do not receive or display notifications.

**Deep links:** Deep links to disabled module screens route to PlaceholderScreen with message: "[Module Name] is not enabled. Enable it in Settings → Modules."

**Widget surface:** Future home screen widgets are registered per module. ModuleRegistry tracks which widget slots are claimed. A new module adding a widget will not conflict with existing widgets.

---

### 10.4 Placeholder Screens for Future Modules

Three placeholder screens are defined in the nav graph now (route exists, screen shows placeholder UI) to allow testing deep links and navigation before implementation:

**FinancePlaceholderScreen** (`finance/`)
- Route: `finance/home`
- Content: EmptyStateView with icon `account_balance_wallet`, title "Finance Module — Coming Soon", message "Track your monthly expenses, budgets, and financial goals. Available in a future update."
- CTA button: "Go to Daily Brief" → navigates to DailyBriefScreen

**HabitsPlaceholderScreen** (`habits/`)
- Route: `habits/home`
- Content: EmptyStateView with icon `loop`, title "Habits Module — Coming Soon", message "Build and track daily habits aligned with your academic goals."
- CTA button: "Go to Projects" → navigates to ProjectListScreen

**SkillXpPlaceholderScreen** (`skill_xp/`)
- Route: `skill_xp/home`
- Content: EmptyStateView with icon `military_tech`, title "Skill XP — Coming Soon", message "Earn XP for completing assignments, attending classes, and solving problems. Level up your engineering skills."
- CTA button: "Go to Coding" → navigates to CpDashboardScreen

All three placeholders:
- Use the standard bottom nav bar (whichever tab last active is highlighted, none of the placeholder routes correspond to a tab).
- Are accessible via Settings → Modules → tap the disabled module → "Preview" link.
- Do not appear in the nav bar until module is fully implemented and enabled.

---

### 10.5 AI Provider Switch Impact on Settings UI

Currently AiSettingsScreen is built for DeepSeek API. The settings UI must accommodate switching providers (Gemini, OpenAI, custom endpoint) with minimal screen changes:

**Provider selector:** Add a "Provider" dropdown at the top of AiSettingsScreen: DeepSeek (default) / Gemini / OpenAI / Custom.

**Dynamic fields per provider:**
- **DeepSeek:** API Key field, Model selector (deepseek-chat, deepseek-reasoner), Base URL (pre-filled, editable)
- **Gemini:** API Key field, Model selector (gemini-1.5-flash, gemini-1.5-pro), Safety settings toggle
- **OpenAI:** API Key field, Model selector (gpt-4o, gpt-4o-mini), Organization ID (optional)
- **Custom:** Base URL field (required), API Key field (optional), Model name field (freeform), Header editor (key-value list)

**Fields that do not change per provider:** Daily Brief generation time, auto-generate toggle, max recommendations slider, daily token budget, quota warning threshold.

**AiStatusBadge** label updates to show the active provider: "DeepSeek Online", "Gemini Online", etc.

**AiDiagnosticsScreen** adds a "Provider" column to the call log table to distinguish which provider handled each call.

**Migration behavior:** When user switches provider, existing API key for the old provider is saved (not cleared) so user can switch back. Each provider's key is stored under its own Room key in the settings table.

**UI layout change:** The "AI Settings" section expands by ~1–3 rows per provider. No structural layout change. The form scrolls as needed. Provider-specific fields are wrapped in an `AnimatedVisibility` block (slides in/out when provider changes, 200ms).

---

### 10.6 Summary of Reserved Hooks

| Hook | What it enables |
|---|---|
| Reserved route namespaces | New modules slot in without route conflicts |
| Placeholder screens | Deep links work pre-implementation |
| ModuleRegistry | Runtime enable/disable of features, no code changes |
| Bottom nav drawer migration path | 6th+ tab without UX regression |
| Provider-agnostic AI settings | Switch LLM provider with UI-only change |
| Per-module notification channels | New modules own their notification UX |
| Two-pane layout on 600dp+ | New list-detail screens get tablet layout free |

---

*End of Student OS UI Blueprint v1.0*
