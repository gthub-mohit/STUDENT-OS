# PROJECT_STATE.md — Student OS

> **Last updated:** 2026-07-30 (Task 2.10 complete)
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

### Task 2.10 — ML Kit Timetable OCR Parser & OcrPreviewScreen ✅
- Enhanced `OcrViewModel` (`:feature:attendance`) with `addSlot`, `updateSlot`, `removeSlot`, `confirmImport`, and `Dispatchers.IO` async image processing
- Updated `OcrPreviewScreen` Compose UI with photo picker launcher (`ActivityResultContracts.GetContent()`), back navigation button, add/edit/delete slot dialogs, amber warning card for low-confidence (<80%) OCR fields, and timetable overwrite confirmation dialog
- Wired `onNavigateToOcrPreview` action button in `WeeklyViewScreen` top bar and `AttendanceNavGraph.kt`
- Created `TimetableFieldMapperTest` unit test suite verifying 12h/24h time normalization, day detection, location extraction, and confidence thresholding
- Created `OcrViewModelTest` unit test suite verifying image processing state transitions, slot CRUD actions, import confirmation, and overwrite replace dialogs

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

## Next Tasks (Group 3 — Assignments & Deadlines Module)

- [ ] **3.1** [M] Create Room Entities and DAOs for Assignment Engine (`AssignmentEntity`, `AssignmentAttachmentEntity`)

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
