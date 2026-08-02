# PROJECT_STATE.md — Student OS

> **Last updated:** 2026-08-02 (Group 5a complete - Task 5a.5)
> **Purpose:** Snapshot for AI continuity. Read this file first when resuming work.

---

## Completed Tasks

### Task 0.1 — Project Initialisation ✅
- Android project initialised with Kotlin, Jetpack Compose, Gradle Kotlin DSL
- `gradle/libs.versions.toml` version catalog created (single source of truth)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9, KSP 2.0.21-1.0.28
- Hilt 2.56.1 configured (plugin + dependencies) across all modules
- Room 2.6.1, Compose BOM 2024.12.01, WorkManager 2.10.0 in catalog
- `:app` module: `StudentOsApp.kt` (@HiltAndroidApp), `MainActivity.kt` (@AndroidEntryPoint)
- `AndroidManifest.xml`: permissions, deep-link intent filter, WorkManager init disabled
- All 13 modules declared in `settings.gradle.kts` with stub `build.gradle.kts` files

### Task 0.2 — Multi-Module Structure ✅
- Source directory trees (`src/main/kotlin/com/studentos/...`) created for all 12 submodules
- `.gitkeep` files preserve empty directories in Git
- App stubs: `AppNavHost.kt`, `BottomNavBar.kt`, `AppModule.kt`
- `:lint-checks` module: `FeatureToFeatureDependencyDetector` (Android Lint)
  - Prevents any `:feature:*` module from depending on another `:feature:*`
  - Registered via `lintChecks(project(":lint-checks"))` in all 6 feature modules
- `kotlin-jvm` plugin and Lint API (31.7.3) added to version catalog

### Task 0.3 — Hilt Dependency Injection Wiring ✅
- `@Inject lateinit var workerFactory: HiltWorkerFactory` added to `StudentOsApp`
- `StudentOsApp` implements `Configuration.Provider` and passes `workerFactory` to `Configuration.Builder().setWorkerFactory(...)`
- Enables Hilt dependency injection into all `@HiltWorker` annotated `ListenableWorker` instances across all feature and core modules
- Verified `@AndroidEntryPoint` on `MainActivity`

### Task 0.4 — Modular Navigation Architecture ✅
- Created `ModuleRegistry.kt` defining `ModuleNavGraph` interface and `NavigationItem` data class
- `AppNavHost` dynamically registers all `ModuleNavGraph` entries via `ModuleRegistry`
- `BottomNavBar` implements Material 3 `NavigationBar` for the 5 root tabs (`Daily Brief`, `Attendance`, `Assignments`, `Coding`, `Projects`)
- Jetpack Navigation backstack preservation (`saveState = true`, `restoreState = true`, `launchSingleTop = true`)
- `MainActivity` hosts `Scaffold` with `TopAppBar` (settings action), `BottomNavBar`, and `AppNavHost`
- Placeholder screens set up for all module routes to verify navigation without premature business logic

### Task 0.5 — Room Database & Settings Infrastructure ✅
- Room database `AppDatabase` created in `:core:database` (`version = 1`, `exportSchema = true`)
- Write-Ahead Logging (`JournalMode.WRITE_AHEAD_LOGGING`) explicitly enabled in `DatabaseModule`
- `SettingEntity` and `SettingsDao` created for key-value persistence
- `SettingsRepository` interface & `SettingsRepositoryImpl` created with typed accessors for all 17 default settings keys
- Hilt `DatabaseModule` and `SettingsModule` providing `@Singleton AppDatabase`, `@Singleton SettingsDao`, and `@Singleton SettingsRepository`
- Verified schema compilation and KSP code generation

### Task 0.6 — GitHub Actions CI Workflow ✅
- Created `.github/workflows/ci.yml` workflow for GitHub Actions CI
- Triggered on `push` and `pull_request` to `main`/`master`
- Configured JDK 17 (Temurin) setup with Gradle dependency caching
- Runs `./gradlew build lint test --no-daemon` to compile all 13 submodules, run Android Lint (including architectural guard `:lint-checks`), and execute unit tests

### Task 1.1 — Core Database Schema for Attendance Engine ✅
- Created Room entities: `SubjectEntity` (`subjects`), `TimetableSlotEntity` (`timetable_slots`), `ClassEventEntity` (`class_events`)
- Preserved all foreign keys (`ON DELETE RESTRICT`), indexes, check constraints, default values, and compound unique constraints specified in `backend-blueprint.md`
- Created projection data class `SubjectAttendanceSummary` for single-query attendance aggregation
- Created Room DAOs: `SubjectDao`, `TimetableSlotDao`, `ClassEventDao`
- Registered entities and DAOs in `AppDatabase` and provided `@Singleton` instances in `DatabaseModule`
- Generated Room version 1 baseline schema JSON export (`core/database/schemas/com.studentos.core.database.AppDatabase/1.json`)

### Task 1.2 — Core Database Schema for Assignment Module ✅
- Created Room entity `AssignmentEntity` (`assignments` table) with `attachment_uri` and `updated_at` (default 0) columns
- Preserved foreign key `subject_id → subjects(id) ON DELETE RESTRICT`, indexes (`idx_assignments_deadline`, `idx_assignments_subject`), default values, and status/priority enum constants
- Created `AssignmentDao` with `insert`, `update`, `updateStatus`, `updateDeadline`, `updateReminderLeadMs`, `deleteById`, status/today/this-week/overdue/urgent queries
- Registered `AssignmentEntity` and `AssignmentDao` in `AppDatabase` and provided `@Singleton` instance in `DatabaseModule`
- Verified schema compilation and KSP code generation

### Task 1.4 — Core Database Schema for Project Module ✅
- Created Room entities: `ProjectEntity` (`projects`), `MilestoneEntity` (`milestones`), `BugEntity` (`bugs`), `ProjectTaskEntity` (`project_tasks`), `ProjectResourceEntity` (`project_resources`)
- Created `ProjectWithNextAction` projection relation data class
- Preserved all foreign keys (`project_id -> projects CASCADE`) and indexes (`idx_projects_active`, `idx_milestones_project`, `idx_bugs_project_status`, `idx_tasks_project_next`, `idx_resources_project`)
- Executed Partial UNIQUE Index `idx_one_next_action` on `project_tasks(project_id) WHERE is_next_action = 1 AND is_parallel = 0` via database callback DDL
- Created DAOs: `ProjectDao`, `ProjectTaskDao`, `MilestoneDao`, `BugDao`, `ProjectResourceDao`
- Registered entities and DAOs in `AppDatabase` and provided `@Singleton` instances in `DatabaseModule`
- Verified schema compilation and KSP code generation

### Task 1.6 — Database Unit & Instrumented Test Suite ✅
- Created `AttendanceCalculator.kt` domain calculator in `:feature:attendance`
- Created `AttendanceCalculatorTest.kt` verifying all 10 required test combinations (100%, partial, cancelled classes, holidays, extra classes, all cancelled, zero classes held, 100% via extra classes, exact threshold boundary, low attendance)
- Created `ForeignKeyConstraintTest.kt` instrumented test suite verifying `ON DELETE RESTRICT` foreign key enforcement across subjects, slots, class events, assignments, and DSA categories
- Created `PartialUniqueIndexTest.kt` instrumented test suite verifying the `idx_one_next_action` partial unique index invariant under sequential vs. parallel modes
- All tests passed cleanly (`BUILD SUCCESSFUL`)

---

## Current Module Structure

```
StudentOS/
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions CI (build + lint + test)
│
├── app/                          ← :app (shell, navigation, DI wiring)
│   └── src/main/kotlin/com/studentos/app/
│       ├── StudentOsApp.kt       ← @HiltAndroidApp, Configuration.Provider
│       ├── MainActivity.kt       ← @AndroidEntryPoint, Scaffold, TopAppBar
│       ├── navigation/
│       │   ├── ModuleRegistry.kt ← ModuleNavGraph, NavigationItem, ModuleRegistry
│       │   ├── AppNavHost.kt     ← Dynamic NavHost builder with placeholder routes
│       │   └── BottomNavBar.kt   ← Material 3 NavigationBar with backstack state handling
│       ├── di/                   ← AppModule.kt (stub)
│       └── receiver/             ← BootReceiver.kt
│
├── lint-checks/                  ← :lint-checks (JVM, custom lint rules)
│   └── src/main/kotlin/com/studentos/lint/
│       ├── FeatureToFeatureDependencyDetector.kt
│       └── StudentOsIssueRegistry.kt
│
├── core/
│   ├── database/                 ← :core:database (Room, KSP, Hilt, AppDatabase, WAL mode, 18 Entities, 18 DAOs)
│   ├── events/                   ← :core:events (AppEventBus, Hilt)
│   ├── intelligence/             ← :core:intelligence (LLM, serialization, Hilt)
│   ├── notifications/            ← :core:notifications (Hilt)
│   ├── sync/                     ← :core:sync (Retrofit, serialization, Hilt)
│   └── ui/                       ← :core:ui (Compose, no Hilt)
│
└── feature/
    ├── attendance/               ← :feature:attendance (Compose, Hilt, KSP, AttendanceCalculator)
    ├── assignments/              ← :feature:assignments
    ├── coding/                   ← :feature:coding (+ kotlin-serialization)
    ├── projects/                 ← :feature:projects
    ├── intelligence/             ← :feature:intelligence
    └── settings/                 ← :feature:settings (SettingsRepository, SettingsRepositoryImpl, SettingsModule)
```

---

## Key Version Pins

| Dependency | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.0.21 | KSP must match: 2.0.21-1.0.28 |
| AGP | 8.7.3 | Lint API = 31.7.3 (AGP+23) |
| Gradle | 8.9 | Wrapper JAR committed |
| Hilt | 2.56.1 | Avoid 2.56.2 (ZipException), 2.57 (test break) |
| Compose BOM | 2024.12.01 | Aligns all Compose library versions |
| Room | 2.6.1 | WAL mode enabled in DatabaseModule |

---

### Task 2.1 — ML Kit OCR Timetable Parsing (`OcrProcessor`) ✅
- Added `libs.mlkit.text.recognition` dependency to `feature/attendance/build.gradle.kts`
- Created domain models `ParsedTimetableSlot` (day 1..7, start/end time, subject, location, confidence, low-confidence flag) and `OcrResult` (slots, hasWarnings, rawText)
- Created `TimetableFieldMapper` converting ML Kit text blocks / raw text lines into structured timetable slots with 24-hour time normalization and day parsing
- Created `OcrProcessor` wrapping ML Kit Text Recognition with `suspendCancellableCoroutine` for async bitmap extraction
- Created `OcrModule` Hilt DI module providing `@Singleton` instances of `TimetableFieldMapper` and `OcrProcessor`
- Created `TimetableFieldMapperTest` unit test suite verifying 5 distinct timetable layouts (standard grid, abbreviated day names, 12-hour AM/PM format, low-confidence warning threshold, noisy text)

---

## Current Module Structure

```
StudentOS/
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions CI (build + lint + test)
│
├── app/                          ← :app (shell, navigation, DI wiring)
│   └── src/main/kotlin/com/studentos/app/
│       ├── StudentOsApp.kt       ← @HiltAndroidApp, Configuration.Provider
│       ├── MainActivity.kt       ← @AndroidEntryPoint, Scaffold, TopAppBar
│       ├── navigation/
│       │   ├── ModuleRegistry.kt ← ModuleNavGraph, NavigationItem, ModuleRegistry
│       │   ├── AppNavHost.kt     ← Dynamic NavHost builder with placeholder routes
│       │   └── BottomNavBar.kt   ← Material 3 NavigationBar with backstack state handling
│       ├── di/                   ← AppModule.kt (stub)
│       └── receiver/             ← BootReceiver.kt
│
├── lint-checks/                  ← :lint-checks (JVM, custom lint rules)
│   └── src/main/kotlin/com/studentos/lint/
│       ├── FeatureToFeatureDependencyDetector.kt
│       └── StudentOsIssueRegistry.kt
│
├── core/
│   ├── database/                 ← :core:database (Room, KSP, Hilt, AppDatabase, WAL mode, 18 Entities, 18 DAOs)
│   ├── events/                   ← :core:events (AppEventBus, Hilt)
│   ├── intelligence/             ← :core:intelligence (LLM, serialization, Hilt)
│   ├── notifications/            ← :core:notifications (Hilt)
│   ├── sync/                     ← :core:sync (Retrofit, serialization, Hilt)
│   └── ui/                       ← :core:ui (Compose, no Hilt)
│
└── feature/
    ├── attendance/               ← :feature:attendance (Compose, Hilt, KSP, ML Kit OCR, OcrProcessor, TimetableFieldMapper)
    ├── assignments/              ← :feature:assignments
    ├── coding/                   ← :feature:coding (+ kotlin-serialization)
    ├── projects/                 ← :feature:projects
    ├── intelligence/             ← :feature:intelligence
    └── settings/                 ← :feature:settings (SettingsRepository, SettingsRepositoryImpl, SettingsModule)
```

---

## Key Version Pins

| Dependency | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.0.21 | KSP must match: 2.0.21-1.0.28 |
| AGP | 8.7.3 | Lint API = 31.7.3 (AGP+23) |
| Gradle | 8.9 | Wrapper JAR committed |
| Hilt | 2.56.1 | Avoid 2.56.2 (ZipException), 2.57 (test break) |
| Compose BOM | 2024.12.01 | Aligns all Compose library versions |
| Room | 2.6.1 | WAL mode enabled in DatabaseModule |
| ML Kit OCR | 16.0.1 | Text Recognition Latin |

---

### Task 2.2 — ImportTimetableUseCase & OcrPreviewScreen UI ✅
- Created `AppResult` sealed class and `AppError` sealed hierarchy in `:core:events` (`AppResult.kt`)
- Created `TimetableRepository` domain interface and `TimetableRepositoryImpl` data repository executing single Room database transaction (`database.withTransaction`) to insert subjects, slots, and generate `class_events` up to 365 days max
- Implemented `ImportTimetableUseCase` enforcing mandatory **365-day generation horizon guard** (`minOf(horizonDays, 365)`)
- Created `OcrViewModel` managing OCR state, editable slot updates, replacement confirmation dialog, and import execution
- Created `OcrPreviewScreen` Jetpack Compose UI with editable slot cards, amber background highlighting for low-confidence fields (< 0.80f threshold), and replace confirmation dialog
- Created `AttendanceRepositoryModule` Hilt DI module binding `TimetableRepositoryImpl` to `TimetableRepository`
- Created `ImportTimetableUseCaseTest` unit test suite verifying the 365-day horizon clamp guard

---

## Current Module Structure

```
StudentOS/
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions CI (build + lint + test)
│
├── app/                          ← :app (shell, navigation, DI wiring)
│   └── src/main/kotlin/com/studentos/app/
│       ├── StudentOsApp.kt       ← @HiltAndroidApp, Configuration.Provider
│       ├── MainActivity.kt       ← @AndroidEntryPoint, Scaffold, TopAppBar
│       ├── navigation/
│       │   ├── ModuleRegistry.kt ← ModuleNavGraph, NavigationItem, ModuleRegistry
│       │   ├── AppNavHost.kt     ← Dynamic NavHost builder with placeholder routes
│       │   └── BottomNavBar.kt   ← Material 3 NavigationBar with backstack state handling
│       ├── di/                   ← AppModule.kt (stub)
│       └── receiver/             ← BootReceiver.kt
│
├── lint-checks/                  ← :lint-checks (JVM, custom lint rules)
│   └── src/main/kotlin/com/studentos/lint/
│       ├── FeatureToFeatureDependencyDetector.kt
│       └── StudentOsIssueRegistry.kt
│
├── core/
│   ├── database/                 ← :core:database (Room, KSP, Hilt, AppDatabase, WAL mode, 18 Entities, 18 DAOs)
│   ├── events/                   ← :core:events (AppResult, AppError, AppEventBus, Hilt)
│   ├── intelligence/             ← :core:intelligence (LLM, serialization, Hilt)
│   ├── notifications/            ← :core:notifications (Hilt)
│   ├── sync/                     ← :core:sync (Retrofit, serialization, Hilt)
│   └── ui/                       ← :core:ui (Compose, no Hilt)
│
└── feature/
    ├── attendance/               ← :feature:attendance (Compose, Hilt, KSP, ML Kit OCR, ImportTimetableUseCase, OcrPreviewScreen, TimetableRepository)
    ├── assignments/              ← :feature:assignments
    ├── coding/                   ← :feature:coding (+ kotlin-serialization)
    ├── projects/                 ← :feature:projects
    ├── intelligence/             ← :feature:intelligence
    └── settings/                 ← :feature:settings (SettingsRepository, SettingsRepositoryImpl, SettingsModule)
```

---

## Key Version Pins

| Dependency | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.0.21 | KSP must match: 2.0.21-1.0.28 |
| AGP | 8.7.3 | Lint API = 31.7.3 (AGP+23) |
| Gradle | 8.9 | Wrapper JAR committed |
| Hilt | 2.56.1 | Avoid 2.56.2 (ZipException), 2.57 (test break) |
| Compose BOM | 2024.12.01 | Aligns all Compose library versions |
| Room | 2.6.1 | WAL mode enabled in DatabaseModule |
| ML Kit OCR | 16.0.1 | Text Recognition Latin |

---

### Task 2.3 — SubjectRepository, ClassEventRepository & AppEventBus Integration ✅
- Created `AppEvent` sealed class and `AppEventBus` interface with `AppEventBusImpl` (`SharedFlow<AppEvent>` with `replay = 0`) in `:core:events` (`AppEvent.kt`, `AppEventBus.kt`, `AppEventBusImpl.kt`)
- Created `EventsModule` Hilt `@Module` binding `@Singleton AppEventBus`
- Created `SubjectRepository` domain interface and `SubjectRepositoryImpl` data repository
- Created `ClassEventRepository` domain interface and `ClassEventRepositoryImpl` data repository emitting `AppEvent.AttendanceMarked` after database writes succeed
- Implemented `UpdateClassEventStatusUseCase`, `AddExtraClassUseCase`, and `ArchiveSubjectUseCase` (with active timetable slot validation)
- Updated `AttendanceRepositoryModule` Hilt DI module binding `SubjectRepository` and `ClassEventRepository`
- Created `UpdateClassEventStatusUseCaseTest` unit test suite verifying status write and `AppEventBus` event emission

---

## Current Module Structure

```
StudentOS/
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions CI (build + lint + test)
│
├── app/                          ← :app (shell, navigation, DI wiring)
│   └── src/main/kotlin/com/studentos/app/
│       ├── StudentOsApp.kt       ← @HiltAndroidApp, Configuration.Provider
│       ├── MainActivity.kt       ← @AndroidEntryPoint, Scaffold, TopAppBar
│       ├── navigation/
│       │   ├── ModuleRegistry.kt ← ModuleNavGraph, NavigationItem, ModuleRegistry
│       │   ├── AppNavHost.kt     ← Dynamic NavHost builder with placeholder routes
│       │   └── BottomNavBar.kt   ← Material 3 NavigationBar with backstack state handling
│       ├── di/                   ← AppModule.kt (stub)
│       └── receiver/             ← BootReceiver.kt
│
├── lint-checks/                  ← :lint-checks (JVM, custom lint rules)
│   └── src/main/kotlin/com/studentos/lint/
│       ├── FeatureToFeatureDependencyDetector.kt
│       └── StudentOsIssueRegistry.kt
│
├── core/
│   ├── database/                 ← :core:database (Room, KSP, Hilt, AppDatabase, WAL mode, 18 Entities, 18 DAOs)
│   ├── events/                   ← :core:events (AppResult, AppError, AppEvent, AppEventBus, AppEventBusImpl, EventsModule)
│   ├── intelligence/             ← :core:intelligence (LLM, serialization, Hilt)
│   ├── notifications/            ← :core:notifications (Hilt)
│   ├── sync/                     ← :core:sync (Retrofit, serialization, Hilt)
│   └── ui/                       ← :core:ui (Compose, no Hilt)
│
└── feature/
    ├── attendance/               ← :feature:attendance (Compose, Hilt, KSP, ML Kit OCR, ImportTimetableUseCase, OcrPreviewScreen, SubjectRepository, ClassEventRepository, UpdateClassEventStatusUseCase, AddExtraClassUseCase, ArchiveSubjectUseCase)
    ├── assignments/              ← :feature:assignments
    ├── coding/                   ← :feature:coding (+ kotlin-serialization)
    ├── projects/                 ← :feature:projects
    ├── intelligence/             ← :feature:intelligence
    └── settings/                 ← :feature:settings (SettingsRepository, SettingsRepositoryImpl, SettingsModule)
```

---

## Key Version Pins

| Dependency | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.0.21 | KSP must match: 2.0.21-1.0.28 |
| AGP | 8.7.3 | Lint API = 31.7.3 (AGP+23) |
| Gradle | 8.9 | Wrapper JAR committed |
| Hilt | 2.56.1 | Avoid 2.56.2 (ZipException), 2.57 (test break) |
| Compose BOM | 2024.12.01 | Aligns all Compose library versions |
| Room | 2.6.1 | WAL mode enabled in DatabaseModule |
| ML Kit OCR | 16.0.1 | Text Recognition Latin |

---

### Task 2.4 — BunkCalculator & Property-Based Testing ✅
- Created pure Kotlin domain calculator `BunkCalculator` (`BunkCalculator.kt`) with zero Android dependencies implementing `canSkip` and `mustAttend` formulas
- Added `testImplementation(libs.kotest.property)` to `feature/attendance/build.gradle.kts`
- Created `BunkCalculatorTest` test suite (`BunkCalculatorTest.kt`) with Kotest property-based tests (`forAll`) verifying Skip Boundary, Attend Boundary, and Mutual Exclusivity invariants, as well as 8 deterministic unit test vectors (100% attendance, 75% threshold, below threshold, 0 held, all cancelled, extra classes, 0% threshold, 100% threshold)

---

## Current Module Structure

```
StudentOS/
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions CI (build + lint + test)
│
├── app/                          ← :app (shell, navigation, DI wiring)
│   └── src/main/kotlin/com/studentos/app/
│       ├── StudentOsApp.kt       ← @HiltAndroidApp, Configuration.Provider
│       ├── MainActivity.kt       ← @AndroidEntryPoint, Scaffold, TopAppBar
│       ├── navigation/
│       │   ├── ModuleRegistry.kt ← ModuleNavGraph, NavigationItem, ModuleRegistry
│       │   ├── AppNavHost.kt     ← Dynamic NavHost builder with placeholder routes
│       │   └── BottomNavBar.kt   ← Material 3 NavigationBar with backstack state handling
│       ├── di/                   ← AppModule.kt (stub)
│       └── receiver/             ← BootReceiver.kt
│
├── lint-checks/                  ← :lint-checks (JVM, custom lint rules)
│   └── src/main/kotlin/com/studentos/lint/
│       ├── FeatureToFeatureDependencyDetector.kt
│       └── StudentOsIssueRegistry.kt
│
├── core/
│   ├── database/                 ← :core:database (Room, KSP, Hilt, AppDatabase, WAL mode, 18 Entities, 18 DAOs)
│   ├── events/                   ← :core:events (AppResult, AppError, AppEvent, AppEventBus, AppEventBusImpl, EventsModule)
│   ├── intelligence/             ← :core:intelligence (LLM, serialization, Hilt)
│   ├── notifications/            ← :core:notifications (Hilt)
│   ├── sync/                     ← :core:sync (Retrofit, serialization, Hilt)
│   └── ui/                       ← :core:ui (Compose, no Hilt)
│
└── feature/
    ├── attendance/               ← :feature:attendance (Compose, Hilt, KSP, ML Kit OCR, ImportTimetableUseCase, OcrPreviewScreen, SubjectRepository, ClassEventRepository, UpdateClassEventStatusUseCase, AttendanceCalculator, BunkCalculator, Kotest)
    ├── assignments/              ← :feature:assignments
    ├── coding/                   ← :feature:coding (+ kotlin-serialization)
    ├── projects/                 ← :feature:projects
    ├── intelligence/             ← :feature:intelligence
    └── settings/                 ← :feature:settings (SettingsRepository, SettingsRepositoryImpl, SettingsModule)
```

---

## Key Version Pins

| Dependency | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.0.21 | KSP must match: 2.0.21-1.0.28 |
| AGP | 8.7.3 | Lint API = 31.7.3 (AGP+23) |
| Gradle | 8.9 | Wrapper JAR committed |
| Hilt | 2.56.1 | Avoid 2.56.2 (ZipException), 2.57 (test break) |
| Compose BOM | 2024.12.01 | Aligns all Compose library versions |
| Room | 2.6.1 | WAL mode enabled in DatabaseModule |
| ML Kit OCR | 16.0.1 | Text Recognition Latin |
| Kotest | 5.9.1 | Property-based testing |

---

### Task 2.5 — WeeklyView and CalendarView Screens ✅
- Added `libs.navigation.compose` and `libs.hilt.navigation.compose` to `feature/attendance/build.gradle.kts`
- Created `WeeklyUiState` and `CalendarUiState` sealed interfaces defining reactive UI state
- Created `@HiltViewModel`s `WeeklyViewModel` and `CalendarViewModel` combining reactive state flows from `ClassEventRepository`, `SubjectRepository`, and `SettingsDao`
- Created Compose components `ClassEventCard` (with future event status confirmation dialog), `AttendancePercentageRow` (with red tint when below threshold), `BunkCalculatorWidget`, and `DayColumn`
- Created root Compose screens `WeeklyViewScreen` and `CalendarViewScreen`
- Created `AttendanceNavGraph.kt` registering navigation routes (`weekly`, `calendar`, `ocr-preview`)
- Created `WeeklyViewModelTest` unit test suite verifying reactive state emission and day selection

---

## Current Module Structure

```
StudentOS/
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions CI (build + lint + test)
│
├── app/                          ← :app (shell, navigation, DI wiring)
│   └── src/main/kotlin/com/studentos/app/
│       ├── StudentOsApp.kt       ← @HiltAndroidApp, Configuration.Provider
│       ├── MainActivity.kt       ← @AndroidEntryPoint, Scaffold, TopAppBar
│       ├── navigation/
│       │   ├── ModuleRegistry.kt ← ModuleNavGraph, NavigationItem, ModuleRegistry
│       │   ├── AppNavHost.kt     ← Dynamic NavHost builder with placeholder routes
│       │   └── BottomNavBar.kt   ← Material 3 NavigationBar with backstack state handling
│       ├── di/                   ← AppModule.kt (stub)
│       └── receiver/             ← BootReceiver.kt
│
├── lint-checks/                  ← :lint-checks (JVM, custom lint rules)
│   └── src/main/kotlin/com/studentos/lint/
│       ├── FeatureToFeatureDependencyDetector.kt
│       └── StudentOsIssueRegistry.kt
│
├── core/
│   ├── database/                 ← :core:database (Room, KSP, Hilt, AppDatabase, WAL mode, 18 Entities, 18 DAOs)
│   ├── events/                   ← :core:events (AppResult, AppError, AppEvent, AppEventBus, AppEventBusImpl, EventsModule)
│   ├── intelligence/             ← :core:intelligence (LLM, serialization, Hilt)
│   ├── notifications/            ← :core:notifications (Hilt)
│   ├── sync/                     ← :core:sync (Retrofit, serialization, Hilt)
│   └── ui/                       ← :core:ui (Compose, no Hilt)
│
└── feature/
    ├── attendance/               ← :feature:attendance (Compose, Hilt, KSP, ML Kit OCR, WeeklyViewScreen, CalendarViewScreen, AttendanceNavGraph, WeeklyViewModel, CalendarViewModel, BunkCalculator)
    ├── assignments/              ← :feature:assignments
    ├── coding/                   ← :feature:coding (+ kotlin-serialization)
    ├── projects/                 ← :feature:projects
    ├── intelligence/             ← :feature:intelligence
    └── settings/                 ← :feature:settings (SettingsRepository, SettingsRepositoryImpl, SettingsModule)
```

---

## Key Version Pins

| Dependency | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.0.21 | KSP must match: 2.0.21-1.0.28 |
| AGP | 8.7.3 | Lint API = 31.7.3 (AGP+23) |
| Gradle | 8.9 | Wrapper JAR committed |
| Hilt | 2.56.1 | Avoid 2.56.2 (ZipException), 2.57 (test break) |
| Compose BOM | 2024.12.01 | Aligns all Compose library versions |
| Room | 2.6.1 | WAL mode enabled in DatabaseModule |
| ML Kit OCR | 16.0.1 | Text Recognition Latin |
| Kotest | 5.9.1 | Property-based testing |

---

### Task 2.6 — AttendanceAnalyticsScreen & BunkCalculatorWidget Integration ✅
- Added `getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>>` Room `@Query` to `ClassEventDao` (`:core:database`)
- Updated `ClassEventRepository` interface and `ClassEventRepositoryImpl` data repository exposing `getAllAttendanceSummaries()`
- Created `AnalyticsUiState` sealed interface (`Loading`, `Success`, `Error`)
- Created `@HiltViewModel` `AttendanceAnalyticsViewModel` combining reactive attendance summaries and `SettingsDao` threshold
- Created `AttendanceAnalyticsScreen` Compose root screen displaying overall attendance summary card, per-subject breakdown progress bars, and inline `BunkCalculatorWidget`
- Registered `analytics` route in `AttendanceNavGraph.kt` and wired `WeeklyViewScreen` top bar navigation action
- Created `AttendanceAnalyticsViewModelTest` unit test suite

---

### Task 2.7 — RecalibrationUseCase ✅
- Created `RecalibrationUseCase` domain use case executing startup verification and recalibration pass directly from raw `ClassEvent` records (`backend-blueprint.md` §T6)
- Created `RecalibrationUseCaseTest` unit test suite verifying startup recalculation success and database error handling via `AppResult.Failure`

---

### Task 2.8 — Timetable Customisation & EditTimetableScreen ✅
- Added `getAllSlots(): Flow<List<TimetableSlotEntity>>` Room `@Query` to `TimetableSlotDao` (`:core:database`)
- Created `EditTimetableUiState` sealed interface (`Loading`, `Success`, `Error`)
- Created `@HiltViewModel` `EditTimetableViewModel` managing timetable slot CRUD operations and reactive state flow
- Created `EditTimetableScreen` Compose root screen with day selector tabs (Mon–Sun), slot list, edit/delete actions, delete confirmation dialog, and modal bottom sheet form for adding/editing slots (subject, start/end time, location, week parity)
- Registered `edit-timetable` route in `AttendanceNavGraph.kt` and wired `WeeklyViewScreen` top bar edit action
- Created `EditTimetableViewModelTest` unit test suite verifying slot CRUD operations and day selection

---

### Task 2.9 — Subject Management (Add, Rename, Archive) ✅
- Added `@Query("UPDATE subjects SET name = :newName WHERE id = :id") suspend fun rename(id: Long, newName: String)` to `SubjectDao` (`:core:database`)
- Added `addSubject(name: String)` and `renameSubject(id: Long, newName: String)` to `SubjectRepository` & `SubjectRepositoryImpl` (`:feature:attendance`)
- Created `ManageSubjectsUiState` sealed interface (`Loading`, `Success`, `Error`)
- Created `@HiltViewModel` `ManageSubjectsViewModel` managing subject addition, renaming, archiving, and reactive state flow
- Created `ManageSubjectsScreen` Compose root screen displaying active and archived subject lists, add/rename dialogs, and archive confirmation dialog
- Registered `manage-subjects` route in `AttendanceNavGraph.kt`
- Created `ManageSubjectsViewModelTest` unit test suite verifying subject creation, renaming, archiving, and reactive state updates

---

### Task 3.2 — Filtered Assignment Views (`Today`, `This_Week`, `Overdue`, `Completed`) ✅
- Created `AssignmentFilter` domain enum in `:feature:assignments` (`TODAY`, `THIS_WEEK`, `OVERDUE`, `COMPLETED`)
- Created `GetFilteredAssignmentsUseCase` domain use case calculating system local timezone epoch-ms boundaries (start of day, end of day, end of 7-day week, current epoch-ms for overdue) and executing Room DAO filter queries via `AssignmentRepository`
- Confirmed `OVERDUE` remains a view-level query filter strictly without ever writing `OVERDUE` to the database `status` column
- Created `GetFilteredAssignmentsUseCaseTest` unit test suite verifying date calculations, boundary timestamps, status query filters, and empty result flows

### Task 3.3 — Assignment UI (List & Detail Screens) ✅
- Created `AssignmentListUiState` and `AssignmentDetailUiState` sealed interfaces (`:feature:assignments`).
- Created reusable Jetpack Compose components: `PriorityBadge`, `StatusChip`, `DeadlineCountdown`, `AssignmentFilterTabs`, `AssignmentCard`, `AttachmentRow`.
- Created `AssignmentListViewModel` managing list state, filter selection, status cycling, and delete confirmation prompt.
- Created `AssignmentDetailViewModel` managing detail view, status cycling, attachment removal, and delete confirmation prompt.
- Created `AssignmentListScreen` and `AssignmentDetailScreen` Compose screens.
- Created `AssignmentsNavGraph.kt` navigation builder for `assignments/list` and `assignments/detail/{id}` routes.
- Implemented `ConfirmDialog` confirmation prompt for deleting `PENDING` or `IN_PROGRESS` assignments (direct delete for `COMPLETED`/`SUBMITTED`).
- Created unit test suites (`AssignmentListViewModelTest` & `AssignmentDetailViewModelTest`) with custom `FakeSubjectDao` and main coroutine dispatcher rule.

### Task 3.4 — File Attachment Support ✅
- Integrated `ActivityResultContracts.GetContent` for file selection in `AssignmentDetailScreen`.
- Implemented `attachFile(id, sourceUriString)` in `AssignmentRepositoryImpl` running on `Dispatchers.IO`.
- Copied picked files to `filesDir/attachments/<uuid>.<ext>` and stored ONLY relative path in `attachment_uri`.
- Implemented automatic file deletion when an attachment is replaced, removed, or when its assignment is deleted via `deleteAssignment(id)`.
- Updated `AttachmentRow` and `AssignmentDetailViewModel` to handle file adding, replacing, and removal seamlessly.
- Created unit tests verifying file import, replacement, removal, orphaned file deletion, and stream copy failure error handling.

### Task 3.5 — Assignment Reminder Worker (WorkManager) ✅
- Created `@HiltWorker` `AssignmentReminderWorker` executing background notification dispatch when assignment reminder triggers.
- Created `AssignmentReminderScheduler` interface and `AssignmentReminderSchedulerImpl` scheduling `OneTimeWorkRequest` at `deadline - reminder_lead_ms`.
- Tagged every work request with `assignment_<id>`.
- Replaced existing reminders when rescheduling via `ExistingWorkPolicy.REPLACE`.
- Handled per-assignment `reminderLeadMs` with fallback to global settings `default_assignment_reminder_lead_ms` (1 hour default).
- Implemented automatic reminder cancellation when assignment status transitions to `SUBMITTED` or `COMPLETED`.
- Prevented scheduling of reminders whose trigger time has already passed (`triggerEpoch <= nowEpoch`).
- Bound `AssignmentReminderScheduler` in `AssignmentsModule` and injected into `AssignmentRepositoryImpl`.
- Created comprehensive unit test suite `AssignmentReminderSchedulerTest` covering all 7 required scheduling scenarios.

### Task 4.1 — CodeChef & Codeforces API Clients (`:core:sync`) ✅
- Created Retrofit API service interfaces `CodeChefApiService` and `CodeforcesApiService`.
- Created Kotlinx Serializable DTOs: `CodeChefProfileDto`, `CodeChefProfileResponseDto`, `CodeChefContestDto`, `CodeforcesProfileDto`, `CodeforcesUserResponseDto`, `CodeforcesContestDto`, and `CodeforcesRatingResponseDto`.
- Created `CodeChefMapper` and `CodeforcesMapper` mapping API responses to `CpProfileEntity` and `CpContestEntity` with safe fallback handling for null fields, invalid ratings, and missing optional data.
- Created Hilt module `SyncModule` in `:core:sync` providing `@Named("cp") OkHttpClient`, `@Named("cp") Retrofit`, `CodeChefApiService`, and `CodeforcesApiService`.
- Created unit test suites `CodeChefMapperTest` and `CodeforcesMapperTest` covering normal mapping, null fields, empty contest history, invalid DTO values, and missing optional fields.

### Task 4.2 — CpSyncWorker & ContestReminderWorker (`:core:sync`) ✅
- Created `@HiltWorker` `CpSyncWorker` executing periodic CP data refresh, database upserting, lookahead contest reminder scheduling, and `AppEvent.CpSyncCompleted` event emission.
- Created `@HiltWorker` `ContestReminderWorker` posting high-priority notifications on channel `"CONTEST_REMINDER"`.
- Implemented contest reminder lookahead query with duplicate prevention (`ExistingWorkPolicy.REPLACE`).
- Documented best-effort Doze-mode execution behavior in code comment.
- Created unit test suite `CpSyncWorkerTest` covering successful sync, empty profiles, API failure, partial platform failure, room data preservation, and reminder scheduling.

### Task 4.3 — Coding Dashboard UI (`:feature:coding`) ✅
- Created domain models `CpProfile` and `CpContest`.
- Created domain contract `CpRepository` and data implementation `CpRepositoryImpl` mapping Room entities to domain models over reactive Flows.
- Created `CpDashboardUiState` and `CpDashboardViewModel` exposing reactive `StateFlow<CpDashboardUiState>`.
- Created UI components: `RatingBadge`, `ContestResultCard`, and `LastSyncedBanner`.
- Created `CpDashboardScreen` handling loading state, empty state (`"No CP profile set up. Add your CodeChef or Codeforces handle in Settings → AI & Coding."`), profile stats, contest history, and last-synced banner.
- Created `CodingNavGraph` declaring route `"coding/cp"`.
- Created Hilt module `CodingModule` binding `CpRepository` to `CpRepositoryImpl`.
- Created unit test suite `CpDashboardViewModelTest` covering loading, empty state, profile mapping, contest mapping, lastSynced timestamp, and Room Flow updates.

### Task 4.4 — Contest Reflection System (`:feature:coding`) ✅
- Created domain model `CpReflection`.
- Extended `CpRepository` and `CpRepositoryImpl` with `getReflection(contestId)` and `saveReflection(reflection)`.
- Created `SaveContestReflectionUseCase` persisting reflections to Room and emitting `AppEvent.ContestReflectionAdded(contestId)` to `AppEventBus`.
- Created `ContestReflectionUiState` and `ContestReflectionViewModel` handling form pre-filling, input validation (self-rating 1–5), save operations, and unsaved changes detection.
- Created `ContestReflectionScreen` form composable with TopAppBar, `wentWrong` and `toRevise` multiline fields, 5-star rating selector, save action, and back press `ConfirmDialog` ("Discard changes?").
- Registered route `coding/reflection/{contestId}` in `CodingNavGraph`.
- Created unit test suites `SaveContestReflectionUseCaseTest` and `ContestReflectionViewModelTest` covering save, update, validation, event emission, pre-filling, and unsaved changes confirmation.

### Task 4.5 — DSA Knowledge Tree (`:feature:coding`) ✅
- Created domain models `DsaCategory` and `DsaTopic`.
- Created domain repository contract `DsaRepository` and implementation `DsaRepositoryImpl` interfacing with `DsaCategoryDao` and `DsaTopicDao`.
- Created UseCases: `AddDsaCategoryUseCase`, `DeleteDsaCategoryUseCase`, and `UpdateDsaTopicUseCase` (emits `AppEvent.DsaTopicUpdated` to `AppEventBus`).
- Created `DsaKnowledgeUiState` and `KnowledgeTreeViewModel` handling loading state, empty state, category tree expansion, add/delete category dialogs, topic confidence updates (1–5), and solved status toggles.
- Created `KnowledgeTreeScreen` composable displaying overall tree progress percentage, expandable category cards, category completion percentages, topic rows with star ratings and revised chips, and dialogs.
- Updated `CodingModule` binding `DsaRepository` -> `DsaRepositoryImpl`.
- Registered route `coding/knowledge-tree` in `CodingNavGraph`.
- Created unit test suites `DsaRepositoryTest`, `AddDsaCategoryUseCaseTest`, `DeleteDsaCategoryUseCaseTest`, `UpdateDsaTopicUseCaseTest`, and `KnowledgeTreeViewModelTest`.

### Task N1 — Navigation Integration Patch (Completed Features) ✅
- Enabled project dependencies `:feature:attendance`, `:feature:assignments`, and `:feature:coding` in `app/build.gradle.kts`.
- Connected `attendanceNavGraph` to `ModuleRegistry` in `AppNavHost.kt` (registered route `"weekly"`).
- Connected `assignmentsNavGraph` to `ModuleRegistry` in `AppNavHost.kt` (registered route `"assignments/list"`).
- Connected `codingNavGraph` to `ModuleRegistry` in `AppNavHost.kt` (registered route `CodingNavGraph.ROUTE_CP_DASHBOARD`).
- Kept temporary architectural `PlaceholderScreen` stubs for unbuilt modules: `intelligence` (Group 5/6), `projects` (Group 7), and `settings` (Group 8).
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 5.1 — Daily Brief Models & Repository (`:feature:intelligence`) ✅
- Created domain models `DailyBrief`, `DailyBriefSummaryDomain`, and `RecommendationCard`.
- Created domain repository contract `DailyBriefRepository` and implementation `DailyBriefRepositoryImpl` mapping Room `DailyBriefEntity` to domain objects.
- Created UseCases `GetDailyBriefUseCase` and `SaveDailyBriefUseCase`.
- Created `IntelligenceModule` Hilt module binding `DailyBriefRepository` -> `DailyBriefRepositoryImpl`.
- Created unit test suite `DailyBriefRepositoryTest` verifying save, flow mapping, and guidance update operations.
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 5.2 — Modular Pipeline & Daily Intelligence Generator (`:feature:intelligence`) ✅
- Implemented modular, open-closed pipeline architecture with `IntelligenceAnalyzer` interface.
- Created immutable fact containers: `AttendanceFact`, `AssignmentFact`, `CodingFact`, `ProjectFact`, and `IntelligenceFacts`.
- Created analyzers returning pure facts without UI strings or business logic: `AttendanceAnalyzer`, `AssignmentAnalyzer`, `CodingAnalyzer`, and `ProjectAnalyzerStub`.
- Created pure domain services with zero Android framework dependencies: `PriorityScoringEngine` (computes scores 0–100 and card priorities 1–5) and `RecommendationEngine` (generates ordered `RecommendationCard` items).
- Created `DailyBriefGenerator` orchestrating parallel analyzer execution (`async`), generating SHA-256 `snapshotHash`, embedding `schemaVersion = "1"`, and injecting `java.time.Clock`.
- Created `IntelligencePipelineModule` providing `java.time.Clock`.
- Created unit test suite `DailyBriefGeneratorTest` verifying pipeline orchestration, priority sorting, target score calculation, and schema version inclusion.
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 5.3 — Daily Brief UI & Presentation Layer (`:feature:intelligence`) ✅
- Created `GenerateDailyBriefUseCase` invoking pipeline generation and saving entry.
- Created `DailyBriefUiState` managing `isLoading`, `isGenerating`, `dailyBrief`, `recommendations`, `errorMessage`, `isEmpty`, and `todayDate`.
- Created `DailyBriefViewModel` managing state flow, today's brief loading, and generation triggers.
- Created Material3 UI components: `ScoreSummaryCard` (progress bar, score target vs actual, engine badge) and `RecommendationCardItem` (visually distinct priority badges, category tag, title, description, action button).
- Created `DailyBriefScreen` composable with TopAppBar, loading placeholders, empty state ("No Daily Brief generated yet." with "Generate Today's Brief" button), error state with Retry, and recommendation list.
- Created `DailyBriefRoute` using `collectAsStateWithLifecycle()`.
- Created `IntelligenceNavGraph` registering route `intelligence/daily-brief`.
- Updated `:app` `build.gradle.kts` and `AppNavHost.kt` connecting `intelligenceNavGraph`.
- Created unit test suite `DailyBriefViewModelTest` verifying empty state loading, generation trigger, recommendation parsing, and state updates.
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 5.4 — Daily Brief History & Automatic Generation Integration (`:feature:intelligence`) ✅
- Extended `DailyBriefRepository` and `DailyBriefRepositoryImpl` with `getBriefById(id: Long)`.
- Created `GetBriefHistoryUseCase` returning `Flow<List<DailyBriefSummaryDomain>>`.
- Created `DailyBriefHistoryUiState` and `DailyBriefHistoryViewModel` sorting history entries newest $\rightarrow$ oldest.
- Created `DailyBriefHistoryItem` composable (date, score target vs actual, guidance source badge, generated time, item click listener).
- Created `DailyBriefHistoryScreen` and `DailyBriefHistoryRoute` composables with top bar back arrow, loading indicator, empty state, and error handling.
- Updated `DailyBriefViewModel` to implement **Automatic Daily Generation**: auto-generates today's brief if missing on launch (at most once per calendar day) and supports loading specific brief dates via `SavedStateHandle`.
- Updated `DailyBriefScreen` TopAppBar with History icon button.
- Updated `IntelligenceNavGraph` registering `intelligence/history` and `intelligence/daily-brief?date={date}` routes with popBackStack navigation.
- Created unit test suites `DailyBriefHistoryViewModelTest` and updated `DailyBriefViewModelTest` verifying automatic generation rules, history sorting, empty/error state handling.
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 5.5 — Daily Brief Background Generation (WorkManager) (`:feature:intelligence`) ✅
- Created `@HiltWorker` implementation `DailyBriefWorker` extending `CoroutineWorker`.
- Injected `GenerateDailyBriefUseCase`, `DailyBriefRepository`, and `Clock`.
- Implemented background generation checks ensuring generation occurs at most once per calendar day on `Dispatchers.IO`.
- Configured WorkManager scheduling helper `enqueue(context: Context)` with `enqueueUniquePeriodicWork` using `ExistingPeriodicWorkPolicy.KEEP` and 24-hour repeat interval.
- Handled error retry policy (`Result.retry()` up to 3 retries, then `Result.failure()`).
- Created unit test suite `DailyBriefWorkerTest` verifying skip on existing brief, generation on missing brief, and retry policy on failure.
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 6.1 — AppEventBus Foundation (`:core:events`) ✅
- Created sealed class hierarchy `AppEvent`: `AttendanceMarked`, `AttendanceUpdated`, `AssignmentStatusChanged`, `AssignmentCreated`, `AssignmentDeleted`, `ProjectTaskCompleted`, `ProjectUpdated`, `CpSyncCompleted`, `ContestReflectionAdded`, `DsaTopicUpdated`, `DailyScoreChanged`.
- Created thread-safe, non-blocking asynchronous `AppEventBus` interface and `AppEventBusImpl` backed by `MutableSharedFlow(replay = 0, extraBufferCapacity = 64, onBufferOverflow = DROP_OLDEST)`.
- Created Hilt module `EventsModule` binding `AppEventBusImpl` as `@Singleton` `AppEventBus`.
- Created unit test suite `AppEventBusTest` verifying thread-safety, event dispatching, multiple collectors support, in-order delivery, and zero replay behavior on new subscriptions.
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 6.2 — LLMProvider Interface & MockProvider (`:core:intelligence`) ✅
- Created pure domain interface `LLMProvider` with `generateBrief`, `updateGuidance`, and `isAvailable`.
- Created domain model `LLMResult` (`Success` / `Failure`) and `FailureReason` enum (`OFFLINE`, `API_ERROR`, `RATE_LIMITED`, `INVALID_KEY`, `TIMEOUT`).
- Implemented `MockProvider` returning fast, deterministic responses without randomness or network dependencies.
- Implemented `LLMProviderFactory` reading `ai_provider` from `SettingsDao` and returning provider implementation.
- Configured Hilt module `IntelligenceCoreModule` binding `MockProvider` as `@Singleton` `LLMProvider`.
- Created unit test suites `MockProviderTest` and `LLMProviderFactoryTest`.
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 6.3 — DeepSeekProvider via Retrofit (`:core:intelligence`) ✅
- Created DTO request/response models `DeepSeekRequest`, `DeepSeekMessage`, `DeepSeekResponse`, `DeepSeekChoice`, `DeepSeekUsage`.
- Created Retrofit API interface `DeepSeekApiService` targeting POST `chat/completions`.
- Created `DeepSeekKeyProvider` for secure API key retrieval from `SettingsDao`.
- Created `DeepSeekProvider` implementing `LLMProvider` with 30s timeout, response parsing, and comprehensive error mapping (`OFFLINE`, `API_ERROR`, `INVALID_KEY`, `TIMEOUT`, `RATE_LIMITED`).
- Updated `LLMProviderFactory` to support dynamic switching between `MockProvider` and `DeepSeekProvider`.
- Created Hilt module `IntelligenceNetworkModule` for OkHttp and Retrofit dependency injection.
- Created unit test suite `DeepSeekProviderTest` using `MockWebServer` and updated `LLMProviderFactoryTest`.
- Verified unit test suite (`.\gradlew.bat test --no-daemon`) and debug build (`.\gradlew.bat assembleDebug --no-daemon`).

### Task 6.4 — SnapshotBuilder (`:core:intelligence`) ✅
- Created immutable `IntelligenceSnapshot` and nested DTO models (`StudentContextSnapshot`, `ClassTodaySnapshot`, `AttendanceWarningSnapshot`, `AssignmentUrgentSnapshot`, `FreeSlotSnapshot`, `SuggestedDsaTopicSnapshot`, `SuggestedProjectActionSnapshot`, `ScoreSnapshot`, `CpSummarySnapshot`).
- Created `@Singleton` `SnapshotBuilder` in `:core:intelligence` gathering real-time facts from Room DAOs concurrently (`coroutineScope`, `async`).
- Evaluated `score_target` dynamically based on active timetable slots, urgent assignments, project next actions, and DSA suggestions.
- Preserved purity: no UI text, no recommendation strings, no prompt formatting, no markdown, no JSON logic.
- Created unit test suite `SnapshotBuilderTest` verifying concurrent snapshot generation under 200ms, empty repository handling, score target calculation, and attendance warning calculations.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 2m 11s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 3s`).

### Task 6.5 — SnapshotDiffer (`:core:intelligence`) ✅
- Created immutable `SnapshotDelta` data models (`ClassesDelta`, `AttendanceDelta`, `AssignmentsDelta`, `FreeSlotsDelta`, `DsaTopicDelta`, `ProjectActionDelta`, `ScoreDelta`, `CpSummaryDelta`).
- Implemented pure, stateless, thread-safe domain component `SnapshotDiffer` in `:core:intelligence` evaluating differences between old and new `IntelligenceSnapshot` instances.
- Computed `SnapshotDelta.isEmpty` property evaluating to `true` when no student-state changes occur.
- Implemented delta filtering rules ignoring insignificant timestamp changes (e.g. `lastSynced` CP timestamp changes when ratings are identical).
- Created comprehensive unit test suite `SnapshotDifferTest` covering empty delta detection, null old snapshot, timestamp ignoring, attendance/assignment additions/removals/updates, free slots, DSA topic, project action, score changes, and deterministic output.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 2m 16s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 46s`).

### Task 6.6 — PromptBuilder (`:core:intelligence`) ✅
- Created pure domain service `PromptBuilder` in `:core:intelligence` annotated with Hilt `@Singleton`.
- Implemented `buildMorningPrompt(snapshot)` generating structured compact morning prompts ($\le 400$ tokens / ~1600 chars).
- Implemented `buildDeltaPrompt(previous, current, delta)` generating incremental prompts ($\le 150$ tokens / ~600 chars) including only modified sections and returning `"DELTA: NO_CHANGES"` when `delta.isEmpty` is true.
- Applied token optimization strategies: machine-oriented compact formatting, omission of empty sections, and abbreviation of redundant labels.
- Created unit test suite `PromptBuilderTest` verifying full snapshot formatting, empty section omission, delta section isolation, token budget bounds, and output determinism.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 2m 20s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 47s`).

### Task 6.7 — DeterministicFallback (`:core:intelligence`) ✅
- Created structured domain models `GuidanceResult`, `GuidanceItem`, and `GuidanceSource` in `:core:intelligence` (`com.studentos.core.intelligence.fallback`).
- Created pure offline rule engine `DeterministicFallback` annotated with `@Singleton` in `:core:intelligence`.
- Implemented strict priority order rules: (1) Critical attendance $\rightarrow$ (2) Overdue assignments $\rightarrow$ (3) Urgent assignments $\rightarrow$ (4) Upcoming classes $\rightarrow$ (5) CP contest $\rightarrow$ (6) Weak DSA topic $\rightarrow$ (7) Project next action $\rightarrow$ (8) Free slot utilization.
- Maintained strict purity: zero LLM calls, zero Retrofit, zero PromptBuilder, zero Room, zero Android SDK dependencies.
- Created comprehensive unit test suite `DeterministicFallbackTest` verifying priority hierarchy, overdue vs urgent ordering, contest/DSA/project recommendations, empty snapshot handling, and deterministic output.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 1m 42s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 44s`).

### Task 6.8 — RecommendationCache (`:core:intelligence`) ✅
- Reused existing `RecommendationCacheEntity` and `RecommendationCacheDao` in `:core:database`.
- Extended `RecommendationCacheDao` with `deleteExpired(thresholdEpochMs)` and `getCount()` queries.
- Created `CachedRecommendation` domain model and `@Singleton` service `RecommendationCache` in `:core:intelligence`.
- Implemented 24-hour TTL expiration logic utilizing injected `Clock`.
- Implemented 7-entry retention limit (`deleteOldestBeyondLimit(7)`).
- Implemented duplicate hash overwrite handling refreshing timestamp and replacing existing entry.
- Added `putGuidance` helper serializing `GuidanceResult` domain objects via Kotlinx Serialization `Json`.
- Created comprehensive unit test suite `RecommendationCacheTest` verifying cache hits, cache misses, 24h TTL expiration, duplicate overwriting, 7-entry retention limit, `clearExpired()`, and `Clock` dependency injection.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 2m 52s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 52s`).

### Task 6.9 — RateLimiter (`:core:intelligence`) ✅
- Reused existing `AiCallLogEntity` and `AiCallLogDao` in `:core:database`.
- Created `@Singleton` service `RateLimiter` in `:core:intelligence` (`com.studentos.core.intelligence.limiter`).
- Integrated dynamic daily limit lookup via `SettingsDao` key `"ai_daily_limit"` (defaulting to 10 if unconfigured).
- Implemented dynamic start-of-day epoch calculation using injected `Clock` and `LocalDate.now(clock)`.
- Implemented `canCall()` (`callsToday < dailyLimit`), `remainingCalls()` (`(dailyLimit - callsToday).coerceAtLeast(0)`), `callsToday()`, and `recordCall()` appending audit log entries with clock timestamp.
- Created comprehensive unit test suite `RateLimiterTest` verifying default limits, custom limits, call recording, limit bounds, automatic next-day reset, and coroutine-safe concurrent calls.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 1m 51s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 47s`).

### Task 6a.1 — Morning Generation Orchestration Pipeline (`:feature:intelligence`) ✅
- Built `@Singleton` `IntelligenceOrchestrator` in `:feature:intelligence` (`com.studentos.feature.intelligence.orchestrator`).
- Implemented `generateMorningBrief(today: LocalDate)` strictly following 5-step morning pipeline:
  1. Build `IntelligenceSnapshot` using `SnapshotBuilder`.
  2. Compute SHA-256 snapshot hash.
  3. Check `RecommendationCache` (return cached brief immediately on hit).
  4. If miss, check `RateLimiter.canCall()`. If available, call `LLMProviderFactory.getProvider().generateBrief(prompt)`. On success: cache response, record AI call, persist `DailyBrief` via `DailyBriefRepository`, and return AI guidance brief. On failure: record call failure and fall back to `DeterministicFallback`.
  5. If rate limit exceeded: skip LLM, generate offline guidance via `DeterministicFallback`, persist `DailyBrief` via `DailyBriefRepository`, and return offline brief.
- Preserved strict scoping: zero `AppEventBus` subscriptions, zero WorkManager changes, zero Compose/ViewModel code.
- Created comprehensive unit test suite `IntelligenceOrchestratorTest` verifying cache hits/skips, cache misses, rate limit fallback, LLM failure fallback, audit call logging, pipeline execution order, and determinism.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 3m 51s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 31s`).

### Task 6a.2 — Live Event Subscription & Intra-Day Refresh (`:feature:intelligence`) ✅
- Extended `IntelligenceOrchestrator` with `startEventSubscription(scope, debounceMillis)` subscribing to `AppEventBus.events`.
- Applied 30-second `debounce()` (`debounceMillis`) preventing duplicate refreshes during event bursts.
- Implemented `processIntraDayRefresh(today: LocalDate)` intra-day refresh pipeline:
  1. Build current snapshot with `SnapshotBuilder`.
  2. Diff previous vs current snapshot with `SnapshotDiffer`.
  3. If `delta.isEmpty == true`: stop immediately (no LLM, no PromptBuilder, no DB updates).
  4. Compute snapshot hash and check `RecommendationCache`. On hit: update repository guidance from cache and update `previousSnapshot`.
  5. On miss: check `RateLimiter.canCall()`. If available, build `PromptBuilder.buildDeltaPrompt(...)` and invoke `LLMProviderFactory.getProvider().generateBrief(...)`. On success: cache response, record call, update repository guidance via `DailyBriefRepository.updateGuidance(...)`, and replace `previousSnapshot`.
  6. On rate limit exceed or LLM failure: fall back to `DeterministicFallback.generateGuidance(...)`, update repository, and replace `previousSnapshot`.
- Maintained UI independence: zero Compose, zero ViewModels, zero direct Room DAO calls, single coroutine subscription scope.
- Extended unit test suite `IntelligenceOrchestratorTest` covering debounced event handling, empty delta short-circuiting, cache hits, rate limit fallbacks, delta prompt generation, and snapshot state replacement.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 2m 2s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 15s`).

### Task 6a.3 — DailyBriefWorker Integration with IntelligenceOrchestrator (`:feature:intelligence`) ✅
- Refactored `DailyBriefWorker` to delegate 100% of brief generation to `IntelligenceOrchestrator.generateMorningBrief(today)`.
- Stripped all direct business logic, repository calls, and use case invocations from `DailyBriefWorker`.
- Preserved existing WorkManager scheduling: `enqueueUniquePeriodicWork("daily_brief_worker", ExistingPeriodicWorkPolicy.KEEP, ...)` repeating every 24 hours.
- Preserved existing 3-attempt retry policy: returning `Result.retry()` for attempt counts $<3$, and `Result.failure()` thereafter.
- Refactored `DailyBriefWorkerTest` to verify orchestrator invocation, retry policy, and failure paths.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 2m 54s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 26s`).

### Task 6a.4 — DailyBriefScreen Live Dashboard Integration (`:feature:intelligence`) ✅
- Transformed `DailyBriefScreen` into a live dashboard rendering the latest `DailyBrief` generated by `IntelligenceOrchestrator`.
- Implemented all 10 required UI sections in exact order:
  1. **Header**: Date, source badge ("AI" / "Offline"), and last updated timestamp string.
  2. **Daily Score Card**: Bound to ViewModel state (`scoreTarget`, `scoreActual`, progress bar).
  3. **AI / Offline Guidance Card**: Renders guidance summary with "AI Guidance" / "Offline Guidance" badge.
  4. **Recommendation Cards**: Displays recommendation list with priority badges, categories, titles, descriptions, and action buttons navigating via `actionRoute`.
  5. **Attendance Warning Section**: Renders warnings for low attendance subjects.
  6. **Urgent Assignment Section**: Displays urgent assignments with deadlines.
  7. **Free Slot Suggestions**: Displays recommended free slot activities.
  8. **Empty State**: Displays "No Daily Brief available." with "Generate Today's Brief" button delegating to ViewModel.
  9. **Loading State**: Displays `CircularProgressIndicator()` when loading/generating.
  10. **Error State**: Displays error message with "Retry" button delegating to ViewModel.
- Maintained strict Clean Architecture: zero business logic in composables, ViewModel owns all state, deep links navigate without hardcoded routing inside UI.
- Updated `DailyBriefViewModel` to inject `IntelligenceOrchestrator` & `DailyBriefRepository`, and updated `DailyBriefViewModelTest`.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 2m 57s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 19s`).

### Task 6a.5 — BriefHistoryScreen Implementation (`:feature:intelligence`) ✅
- Implemented `BriefHistoryScreen` displaying all historical daily briefs sorted newest $\rightarrow$ oldest (`LazyColumn`).
- Created `DailyBriefHistoryItem` card displaying date, guidance source ("AI Engine" / "Offline Engine"), daily score (`scoreActual` / `scoreTarget`), guidance summary (`llmGuidance`), and generation timestamp (`HH:mm`).
- Configured card click callback to navigate to `intelligence/daily-brief?date={date}`.
- Handled loading state, empty state ("No history yet."), and error state with retry support (`DailyBriefHistoryViewModel.loadHistory()`).
- Updated `DailyBriefDao`, `DailyBriefSummary`, `DailyBriefSummaryDomain`, and `DailyBriefRepositoryImpl` to include brief summary projections.
- Updated `DailyBriefHistoryViewModelTest` to cover sorting, empty state, and error handling.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 4m 40s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 19s`).

### Task 6a.6 — DailyScoreViewModel & Real-Time Daily Score Updates (`:feature:intelligence`) ✅
- Implemented `@HiltViewModel` `DailyScoreViewModel` in `:feature:intelligence` (`com.studentos.feature.intelligence.presentation.viewmodel`).
- Created `DailyScoreUiState` representing `targetScore`, `currentScore`, `progressPercentage`, `progressBarValue`, `remainingScore`, `isLoading`, and `errorMessage`.
- Subscribed to `AppEventBus.events` with `debounce(30.seconds)` (default 30_000ms), reacting to all 11 system events (`AttendanceMarked`, `AttendanceUpdated`, `AssignmentStatusChanged`, `AssignmentCreated`, `AssignmentDeleted`, `ProjectTaskCompleted`, `ProjectUpdated`, `CpSyncCompleted`, `ContestReflectionAdded`, `DsaTopicUpdated`, `DailyScoreChanged`).
- On debounced event trigger: reloads today's `DailyBrief` via `DailyBriefRepository.getBriefForDate(today)` and updates ONLY score UI state without re-triggering brief generation.
- Prevents redundant UI updates if score parameters remain unchanged.
- Created unit test suite `DailyScoreViewModelTest` verifying initial load, event score updates, 30s debouncing, event collapsing, and StateFlow emissions.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 3m 36s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 1s`). **Completed Group 6a!**

### Task 5a.1 — Project Creation & Dashboard (`:feature:projects`) ✅
- Implemented `ProjectDomain` domain model, `ProjectRepository` interface, `ProjectRepositoryImpl`, and `ProjectRepositoryModule` Hilt DI binding.
- Implemented `ProjectsViewModel` (@HiltViewModel) managing active and archived project lists, project creation, edit, archiving/unarchiving, and UI states.
- Created `ProjectCard` composable component displaying title, status badge ("Active", "Inactive Alert", "Archived"), active next action task, progress % and progress bar, last updated timestamp, and action buttons.
- Created `CreateProjectDialog` composable dialog for project title input and configurable inactivity threshold days (default 7 days).
- Implemented `ProjectsScreen` and `ProjectsRoute` composable views supporting loading, active/archived tab filtering, empty state, error state with retry, and project selection callbacks.
- Connected `:feature:projects` dependency in `app/build.gradle.kts` and registered `ProjectsRoute` in `AppNavHost.kt` navigation graph.
- Added comprehensive unit test suites `ProjectsViewModelTest` and `ProjectRepositoryImplTest`.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 1m 52s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 51s`).

### Task 5a.2 — Sequential & Parallel Task Management (`:feature:projects`) ✅
- Implemented `ProjectTaskDomain` domain model and task management methods in `ProjectRepository` / `ProjectRepositoryImpl`.
- Implemented `@HiltViewModel` `ProjectTaskViewModel` managing tasks, sequential/parallel mode toggling, creation, editing, deletion, completion/reopening, and manual Next Action selection.
- Enforced Room partial unique index invariant `idx_one_next_action` (`WHERE is_next_action = 1 AND is_parallel = 0`). In sequential mode, completing the current next action automatically promotes the next unfinished task. Setting a next action explicitly clears existing next action flags for the project first.
- Emitted `AppEvent.ProjectTaskCompleted` and `AppEvent.ProjectUpdated` through `AppEventBus` on task updates.
- Created `TaskItem`, `CreateTaskDialog`, `ProjectTaskScreen`, and `ProjectTaskRoute` composable components using Material 3. Registered `projects/detail/{projectId}` route in `AppNavHost.kt`.
- Created comprehensive unit test suites `ProjectTaskViewModelTest` and updated `ProjectRepositoryImplTest`.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 1m 44s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 32s`).

### Task 5a.3 — Milestone & Sub-Goal Tracking (`:feature:projects`) ✅
- Implemented `MilestoneDomain` domain model and milestone operations (`createMilestone`, `updateMilestone`, `deleteMilestone`, `completeMilestone`, `reopenMilestone`) in `ProjectRepository` / `ProjectRepositoryImpl`.
- Reused existing `MilestoneEntity` and `MilestoneDao` from `:core:database`.
- Implemented `@HiltViewModel` `MilestoneViewModel` managing milestone state, creation, editing, deletion, and completion toggles.
- Dynamic milestone progress calculated dynamically as `(completedCount / totalCount) * 100%`.
- Emitted `AppEvent.ProjectUpdated(projectId)` through `AppEventBus` on milestone changes to trigger live system updates.
- Created `MilestoneCard`, `CreateMilestoneDialog`, `MilestoneScreen`, and `MilestoneRoute` composable components using Material 3. Registered `projects/milestones/{projectId}` route in `AppNavHost.kt`.
- Created unit test suite `MilestoneViewModelTest` and updated `ProjectRepositoryImplTest`.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 4m 19s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 21s`).

### Task 5a.4 — Project Bug / Issue Tracker (`:feature:projects`) ✅
- Implemented `BugDomain` domain model and bug operations (`createBug`, `updateBug`, `deleteBug`, `resolveBug`, `reopenBug`) in `ProjectRepository` / `ProjectRepositoryImpl`.
- Reused existing `BugEntity` and `BugDao` from `:core:database`.
- Implemented `@HiltViewModel` `BugTrackerViewModel` managing bug state, status filters (`OPEN`, `RESOLVED`, `ALL`), severity filters (`HIGH`, `MEDIUM`, `LOW`, `ALL`), and sort orders (`SEVERITY_DESC`, `SEVERITY_ASC`, `NEWEST`).
- Emitted `AppEvent.ProjectUpdated(projectId)` through `AppEventBus` on bug mutations.
- Created `BugCard`, `CreateBugDialog`, `BugTrackerScreen`, and `BugTrackerRoute` composable components using Material 3. Registered `projects/bugs/{projectId}` route in `AppNavHost.kt`.
- Created unit test suite `BugTrackerViewModelTest` and updated `ProjectRepositoryImplTest`.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 4m 12s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 13s`).

### Task 5a.5 — Project Resource Vault & Notes (`:feature:projects`) ✅
- Implemented `ProjectResourceDomain` domain model and resource operations (`createResource`, `updateResource`, `deleteResource`) in `ProjectRepository` / `ProjectRepositoryImpl`.
- Reused existing `ProjectResourceEntity` and `ProjectResourceDao` from `:core:database`.
- Implemented `@HiltViewModel` `ProjectResourcesViewModel` managing resource vault state, creation, editing, and deletion.
- Supported resource types (`LINK`, `NOTE`, `DOCUMENTATION`, `FILE`).
- Emitted `AppEvent.ProjectUpdated(projectId)` through `AppEventBus` on resource mutations.
- Created `ResourceCard`, `CreateResourceDialog`, `ProjectResourcesScreen`, and `ProjectResourcesRoute` composable components using Material 3. Registered `projects/resources/{projectId}` route in `AppNavHost.kt`.
- Created unit test suite `ProjectResourcesViewModelTest` and updated `ProjectRepositoryImplTest`.
- Verified unit test suite (`.\gradlew.bat test --no-daemon` -> `BUILD SUCCESSFUL in 4m 57s`) and debug assembly (`.\gradlew.bat assembleDebug --no-daemon` -> `BUILD SUCCESSFUL in 1m 20s`). (Group 5a fully complete!)

---

## Current Module Structure

```
StudentOS/
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions CI (build + lint + test)
│
├── app/                          ← :app (shell, navigation, DI wiring)
│   └── src/main/kotlin/com/studentos/app/
│       ├── StudentOsApp.kt       ← @HiltAndroidApp, Configuration.Provider
│       ├── MainActivity.kt       ← @AndroidEntryPoint, Scaffold, TopAppBar
│       ├── navigation/
│       │   ├── ModuleRegistry.kt ← ModuleNavGraph, NavigationItem, ModuleRegistry
│       │   ├── AppNavHost.kt     ← Dynamic NavHost builder with placeholder routes
│       │   └── BottomNavBar.kt   ← Material 3 NavigationBar with backstack state handling
│       ├── di/                   ← AppModule.kt (stub)
│       └── receiver/             ← BootReceiver.kt
│
├── lint-checks/                  ← :lint-checks (JVM, custom lint rules)
│   └── src/main/kotlin/com/studentos/lint/
│       ├── FeatureToFeatureDependencyDetector.kt
│       └── StudentOsIssueRegistry.kt
│
├── core/
│   ├── database/                 ← :core:database (Room, KSP, Hilt, AppDatabase, WAL mode, 18 Entities, 18 DAOs, SubjectAttendanceSummary)
│   ├── events/                   ← :core:events (AppResult, AppError, AppEvent, AppEventBus, AppEventBusImpl, EventsModule)
│   ├── intelligence/             ← :core:intelligence (LLM, serialization, Hilt)
│   ├── notifications/            ← :core:notifications (Hilt)
│   ├── sync/                     ← :core:sync (Retrofit, serialization, Hilt)
│   └── ui/                       ← :core:ui (Compose, no Hilt)
│
└── feature/
    ├── attendance/               ← :feature:attendance (Compose, Hilt, KSP, ML Kit OCR, WeeklyViewScreen, CalendarViewScreen, AttendanceAnalyticsScreen, EditTimetableScreen, ManageSubjectsScreen, OcrPreviewScreen, AttendanceNavGraph, ViewModels, BunkCalculator, RecalibrationUseCase, ImportTimetableUseCase, OcrProcessor, TimetableFieldMapper)
    ├── assignments/              ← :feature:assignments (Compose, Hilt, KSP, AssignmentRepository, AssignmentRepositoryImpl, CreateAssignmentUseCase, UpdateAssignmentStatusUseCase, GetFilteredAssignmentsUseCase, AssignmentFilter, AssignmentsModule)
    ├── coding/                   ← :feature:coding (+ kotlin-serialization)
    ├── projects/                 ← :feature:projects
    ├── intelligence/             ← :feature:intelligence
    └── settings/                 ← :feature:settings (SettingsRepository, SettingsRepositoryImpl, SettingsModule)
```

---

## Key Version Pins

| Dependency | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.0.21 | KSP must match: 2.0.21-1.0.28 |
| AGP | 8.7.3 | Lint API = 31.7.3 (AGP+23) |
| Gradle | 8.9 | Wrapper JAR committed |
| Hilt | 2.56.1 | Avoid 2.56.2 (ZipException), 2.57 (test break) |
| Compose BOM | 2024.12.01 | Aligns all Compose library versions |
| Room | 2.6.1 | WAL mode enabled in DatabaseModule |
| ML Kit OCR | 16.0.1 | Text Recognition Latin |
| Kotest | 5.9.1 | Property-based testing |

---

## Next Tasks (Group 3 — Assignments & Deadlines Module)

- [ ] **3.3** [M] Build `AssignmentListScreen` and `AssignmentDetailScreen` (Compose). Show priority badge, deadline countdown, status chip. Confirmation prompt on delete of `PENDING` or `IN_PROGRESS` assignments.

---

## Architecture Rules

1. **No feature-to-feature dependencies** — enforced by `FeatureToFeatureDependencyDetector` lint rule
2. **`:core:sync`** — only `:feature:coding` may depend on it
3. **`:core:intelligence`** — only `:feature:intelligence` and `:feature:settings` may depend on it
4. **All versions** live in `gradle/libs.versions.toml` — no version literals in build files
5. **All Workers** use `@HiltWorker` + `@AssistedInject` (wired in task 0.3)
6. **All ViewModels** use `@HiltViewModel` (ViewModelScoped by default)

---

## Known Issues

- Deprecation warning: `Hilt_StudentOsApp.java uses or overrides a deprecated API` — harmless, comes from Hilt code generation. Will resolve with a future Hilt update.
