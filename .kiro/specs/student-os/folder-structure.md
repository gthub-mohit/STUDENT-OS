
# Folder Structure & Implementation Reference — Student OS
**Generated from:** requirements.md · design.md · tasks.md · backend-blueprint.md  
**Purpose:** Eliminate all architectural decisions during coding. Every file has exactly one correct location.

---

## 1. Gradle Module Hierarchy

```
StudentOS/                                    ← root project
│
├── app/                                      ← :app  (shell, navigation, DI wiring)
│
├── core/
│   ├── database/                             ← :core:database
│   ├── ui/                                   ← :core:ui
│   ├── events/                               ← :core:events
│   ├── intelligence/                         ← :core:intelligence
│   ├── notifications/                        ← :core:notifications
│   └── sync/                                 ← :core:sync
│
└── feature/
    ├── attendance/                           ← :feature:attendance
    ├── assignments/                          ← :feature:assignments
    ├── coding/                               ← :feature:coding
    ├── projects/                             ← :feature:projects
    ├── intelligence/                         ← :feature:intelligence
    └── settings/                             ← :feature:settings
```

### Module dependency rules (enforced by lint)
```
:app
  → all :feature:* modules
  → all :core:* modules

:feature:attendance
  → :core:database
  → :core:events
  → :core:ui
  → :core:notifications    (to schedule reminders)

:feature:assignments
  → :core:database
  → :core:events
  → :core:ui
  → :core:notifications

:feature:coding
  → :core:database
  → :core:events
  → :core:ui
  → :core:sync

:feature:projects
  → :core:database
  → :core:events
  → :core:ui
  → :core:notifications

:feature:intelligence
  → :core:database
  → :core:events
  → :core:intelligence
  → :core:ui
  → :core:notifications

:feature:settings
  → :core:database
  → :core:intelligence     (AI settings + diagnostics)
  → :core:ui

:core:intelligence
  → :core:database
  → :core:events

:core:notifications
  → :core:database

:core:sync
  → :core:database
  → :core:events

:core:events
  → (no internal dependencies)

:core:database
  → (no internal dependencies)

:core:ui
  → (no internal dependencies)

FORBIDDEN for every :feature:* module:
  - importing any other :feature:* module
  - importing :core:sync directly (except :feature:coding)
  - importing :core:intelligence directly (except :feature:intelligence and :feature:settings)
```

---

## 2. Root-Level Files

```
StudentOS/
├── build.gradle.kts              ← root build config
├── settings.gradle.kts           ← module declarations
├── gradle.properties
├── libs.versions.toml            ← version catalog (single source for all deps)
├── .github/
│   └── workflows/
│       └── ci.yml                ← GitHub Actions: build + lint + test
└── app/
    ├── build.gradle.kts
    ├── src/main/
    │   ├── AndroidManifest.xml
    │   └── kotlin/com/studentos/app/
    │       ├── StudentOsApp.kt           ← @HiltAndroidApp
    │       ├── MainActivity.kt           ← @AndroidEntryPoint, single activity
    │       ├── navigation/
    │       │   ├── AppNavHost.kt         ← root NavHost, reads ModuleRegistry
    │       │   └── BottomNavBar.kt
    │       └── di/
    │           └── AppModule.kt          ← top-level Hilt bindings
    └── src/test/
```

---

## 3. Core Module Package Structures

### 3.1 :core:database
```
core/database/
├── build.gradle.kts
└── src/main/kotlin/com/studentos/core/database/
    │
    ├── AppDatabase.kt                    ← @Database(entities=[...], version=1)
    ├── DatabaseMigrations.kt             ← Migration(1,2), Migration(2,3) ...
    │
    ├── entity/                           ← Room @Entity classes only
    │   ├── SubjectEntity.kt
    │   ├── TimetableSlotEntity.kt
    │   ├── ClassEventEntity.kt
    │   ├── AssignmentEntity.kt
    │   ├── CpProfileEntity.kt
    │   ├── CpContestEntity.kt
    │   ├── CpReflectionEntity.kt
    │   ├── DsaCategoryEntity.kt
    │   ├── DsaTopicEntity.kt
    │   ├── ProjectEntity.kt
    │   ├── MilestoneEntity.kt
    │   ├── BugEntity.kt
    │   ├── ProjectTaskEntity.kt
    │   ├── ProjectResourceEntity.kt
    │   ├── DailyBriefEntity.kt
    │   ├── RecommendationCacheEntity.kt
    │   ├── AiCallLogEntity.kt
    │   └── SettingEntity.kt
    │
    ├── dao/                              ← Room @Dao interfaces only
    │   ├── SubjectDao.kt
    │   ├── TimetableSlotDao.kt
    │   ├── ClassEventDao.kt
    │   ├── AssignmentDao.kt
    │   ├── CpProfileDao.kt
    │   ├── CpContestDao.kt
    │   ├── CpReflectionDao.kt
    │   ├── DsaCategoryDao.kt
    │   ├── DsaTopicDao.kt
    │   ├── ProjectDao.kt
    │   ├── MilestoneDao.kt
    │   ├── BugDao.kt
    │   ├── ProjectTaskDao.kt
    │   ├── ProjectResourceDao.kt
    │   ├── DailyBriefDao.kt
    │   ├── RecommendationCacheDao.kt
    │   ├── AiCallLogDao.kt
    │   └── SettingsDao.kt
    │
    ├── relation/                         ← Room @Relation / multi-table query results
    │   ├── SubjectAttendanceSummary.kt
    │   ├── ProjectWithNextAction.kt
    │   └── DailyBriefSummary.kt         ← projection (no blob columns) — see blueprint §8/P6
    │
    ├── converter/                        ← Room @TypeConverter classes
    │   └── Converters.kt
    │
    └── di/
        └── DatabaseModule.kt            ← @Module providing AppDatabase + all DAOs
```

**Rules for :core:database:**
- Contains ONLY `@Entity`, `@Dao`, `@Database`, `@TypeConverter`, and Hilt `@Module` for DB provision.
- No domain models. No business logic. No repositories.
- `@Entity` classes are data classes with no methods beyond what Room requires.

---

### 3.2 :core:events
```
core/events/
├── build.gradle.kts
└── src/main/kotlin/com/studentos/core/events/
    │
    ├── AppEvent.kt                       ← sealed class AppEvent { ... }
    ├── AppEventBus.kt                    ← interface AppEventBus
    ├── AppEventBusImpl.kt                ← SharedFlow implementation
    └── di/
        └── EventsModule.kt              ← @Singleton binding AppEventBus → AppEventBusImpl
```

**AppEvent sealed class members (all defined here, nowhere else):**
```
AppEvent
├── AttendanceMarked(subjectId: Long, status: String)
├── AssignmentStatusChanged(assignmentId: Long, newStatus: String)
├── ProjectTaskCompleted(taskId: Long, projectId: Long)
├── CpSyncCompleted
├── ContestReflectionAdded(contestId: Long)
├── DsaTopicUpdated(topicId: Long)
└── DailyScoreChanged(newScore: Int)
```

---

### 3.3 :core:intelligence
```
core/intelligence/
├── build.gradle.kts
└── src/main/kotlin/com/studentos/core/intelligence/
    │
    ├── model/                            ← Pure Kotlin data classes (no Android, no Room)
    │   ├── IntelligenceSnapshot.kt
    │   ├── SnapshotDelta.kt
    │   ├── GuidanceText.kt
    │   ├── LLMResult.kt                 ← sealed class: Success / Failure
    │   ├── FailureReason.kt             ← enum
    │   └── CachedRecommendation.kt
    │
    ├── provider/                         ← LLM provider abstraction
    │   ├── LLMProvider.kt               ← interface
    │   ├── DeepSeekProvider.kt          ← Retrofit implementation
    │   ├── MockProvider.kt              ← test/no-key fallback
    │   └── LLMProviderFactory.kt        ← reads aiProvider setting, returns impl
    │
    ├── snapshot/
    │   ├── SnapshotBuilder.kt           ← suspend fun build(): IntelligenceSnapshot
    │   └── SnapshotDiffer.kt            ← fun diff(old, new): SnapshotDelta
    │
    ├── prompt/
    │   └── PromptBuilder.kt             ← buildFullPrompt() / buildDeltaPrompt()
    │
    ├── fallback/
    │   └── DeterministicFallback.kt     ← fun compose(snapshot): GuidanceText
    │
    ├── cache/
    │   ├── RecommendationCache.kt       ← interface
    │   └── RecommendationCacheImpl.kt   ← wraps RecommendationCacheDao
    │
    ├── ratelimit/
    │   └── RateLimiter.kt               ← reads ai_call_log, enforces daily cap
    │
    ├── api/                              ← Retrofit service interface + DTOs
    │   ├── DeepSeekApiService.kt
    │   └── dto/
    │       ├── ChatRequest.kt
    │       └── ChatResponse.kt
    │
    └── di/
        └── IntelligenceModule.kt        ← binds LLMProvider, provides Retrofit client,
                                            provides SnapshotBuilder, RateLimiter, etc.
```

---

### 3.4 :core:notifications
```
core/notifications/
├── build.gradle.kts
└── src/main/kotlin/com/studentos/core/notifications/
    │
    ├── NotificationChannelRegistry.kt   ← creates all 6 channels at app startup
    ├── NotificationCategory.kt          ← enum of all categories
    ├── NotificationScheduler.kt         ← interface: scheduleAssignmentReminder(), etc.
    ├── NotificationSchedulerImpl.kt     ← WorkManager-backed implementation
    ├── NotificationRescheduler.kt       ← called at App.onCreate() to re-enqueue dropped workers
    └── di/
        └── NotificationsModule.kt
```

---

### 3.5 :core:sync
```
core/sync/
├── build.gradle.kts
└── src/main/kotlin/com/studentos/core/sync/
    │
    ├── api/                              ← Retrofit service interfaces + DTOs
    │   ├── CodeChefApiService.kt
    │   ├── CodeforcesApiService.kt
    │   └── dto/
    │       ├── CodeChefContestDto.kt
    │       ├── CodeforcesContestDto.kt
    │       ├── CodeChefProfileDto.kt
    │       └── CodeforcesProfileDto.kt
    │
    ├── worker/
    │   ├── CpSyncWorker.kt              ← CoroutineWorker, periodic
    │   └── ContestReminderWorker.kt     ← CoroutineWorker, one-time
    │
    ├── mapper/
    │   ├── CodeChefMapper.kt
    │   └── CodeforcesMapper.kt
    │
    └── di/
        └── SyncModule.kt               ← provides Retrofit, OkHttpClient for CP APIs
```

---

### 3.6 :core:ui
```
core/ui/
├── build.gradle.kts
└── src/main/kotlin/com/studentos/core/ui/
    │
    ├── theme/
    │   ├── StudentOsTheme.kt
    │   ├── Color.kt
    │   ├── Typography.kt
    │   └── Shape.kt
    │
    ├── component/                        ← shared reusable Composables only
    │   ├── StudentOsTopBar.kt
    │   ├── StatusChip.kt
    │   ├── PriorityBadge.kt
    │   ├── ProgressBar.kt
    │   ├── EmptyStateView.kt
    │   ├── ConfirmDialog.kt
    │   └── LoadingOverlay.kt
    │
    └── util/
        ├── DateFormatter.kt
        └── EpochExtensions.kt
```

---

## 4. Feature Module Package Structures

### 4.1 :feature:attendance
```
feature/attendance/
├── build.gradle.kts
└── src/main/kotlin/com/studentos/feature/attendance/
    │
    ├── data/
    │   ├── repository/
    │   │   ├── SubjectRepositoryImpl.kt
    │   │   ├── TimetableRepositoryImpl.kt
    │   │   └── ClassEventRepositoryImpl.kt
    │   └── ocr/
    │       ├── OcrProcessor.kt
    │       └── TimetableFieldMapper.kt
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Subject.kt
    │   │   ├── TimetableSlot.kt
    │   │   ├── ClassEvent.kt
    │   │   ├── AttendanceSummary.kt
    │   │   ├── SubjectAttendanceSummary.kt
    │   │   └── OcrResult.kt
    │   ├── repository/
    │   │   ├── SubjectRepository.kt           ← interface
    │   │   ├── TimetableRepository.kt         ← interface
    │   │   └── ClassEventRepository.kt        ← interface
    │   ├── usecase/
    │   │   ├── ImportTimetableUseCase.kt
    │   │   ├── UpdateClassEventStatusUseCase.kt
    │   │   ├── AddExtraClassUseCase.kt
    │   │   ├── ArchiveSubjectUseCase.kt
    │   │   └── RecalibrationUseCase.kt
    │   └── calculator/
    │       ├── AttendanceCalculator.kt        ← pure Kotlin, zero Android deps
    │       └── BunkCalculator.kt              ← pure Kotlin, zero Android deps
    │
    ├── presentation/
    │   ├── screen/
    │   │   ├── WeeklyViewScreen.kt
    │   │   ├── CalendarViewScreen.kt
    │   │   ├── AttendanceAnalyticsScreen.kt
    │   │   ├── OcrPreviewScreen.kt
    │   │   └── EditTimetableScreen.kt
    │   ├── component/
    │   │   ├── ClassEventCard.kt
    │   │   ├── AttendancePercentageRow.kt
    │   │   ├── BunkCalculatorWidget.kt
    │   │   ├── DayColumn.kt
    │   │   └── OcrFieldRow.kt
    │   ├── viewmodel/
    │   │   ├── WeeklyViewModel.kt
    │   │   ├── CalendarViewModel.kt
    │   │   ├── AttendanceAnalyticsViewModel.kt
    │   │   └── OcrPreviewViewModel.kt
    │   └── state/
    │       ├── WeeklyUiState.kt
    │       ├── CalendarUiState.kt
    │       ├── AnalyticsUiState.kt
    │       └── OcrPreviewUiState.kt
    │
    ├── navigation/
    │   └── AttendanceNavGraph.kt             ← NavGraphBuilder extension
    │
    └── di/
        └── AttendanceModule.kt               ← binds repository interfaces to impls
```

---

### 4.2 :feature:assignments
```
feature/assignments/
└── src/main/kotlin/com/studentos/feature/assignments/
    │
    ├── data/
    │   └── repository/
    │       └── AssignmentRepositoryImpl.kt
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Assignment.kt
    │   │   └── AssignmentInput.kt
    │   ├── repository/
    │   │   └── AssignmentRepository.kt        ← interface
    │   └── usecase/
    │       ├── CreateAssignmentUseCase.kt
    │       ├── UpdateAssignmentStatusUseCase.kt
    │       ├── UpdateAssignmentDeadlineUseCase.kt
    │       ├── DeleteAssignmentUseCase.kt
    │       └── SetAttachmentUseCase.kt
    │
    ├── presentation/
    │   ├── screen/
    │   │   ├── AssignmentListScreen.kt
    │   │   └── AssignmentDetailScreen.kt
    │   ├── component/
    │   │   ├── AssignmentCard.kt
    │   │   ├── DeadlineCountdown.kt
    │   │   ├── AttachmentRow.kt
    │   │   └── AssignmentFilterTabs.kt
    │   ├── viewmodel/
    │   │   ├── AssignmentListViewModel.kt
    │   │   └── AssignmentDetailViewModel.kt
    │   └── state/
    │       ├── AssignmentListUiState.kt
    │       └── AssignmentDetailUiState.kt
    │
    ├── worker/
    │   └── AssignmentReminderWorker.kt
    │
    ├── navigation/
    │   └── AssignmentsNavGraph.kt
    │
    └── di/
        └── AssignmentsModule.kt
```

---

### 4.3 :feature:coding
```
feature/coding/
└── src/main/kotlin/com/studentos/feature/coding/
    │
    ├── data/
    │   └── repository/
    │       ├── CpRepositoryImpl.kt
    │       └── DsaRepositoryImpl.kt
    │
    ├── domain/
    │   ├── model/
    │   │   ├── CpProfile.kt
    │   │   ├── CpContest.kt
    │   │   ├── CpReflection.kt
    │   │   ├── DsaCategory.kt
    │   │   └── DsaTopic.kt
    │   ├── repository/
    │   │   ├── CpRepository.kt               ← interface
    │   │   └── DsaRepository.kt              ← interface
    │   └── usecase/
    │       ├── SaveContestReflectionUseCase.kt
    │       ├── UpdateDsaTopicUseCase.kt
    │       ├── AddDsaCategoryUseCase.kt
    │       ├── DeleteDsaCategoryUseCase.kt
    │       └── DsaTopicSuggester.kt          ← pure logic, no Android deps
    │
    ├── presentation/
    │   ├── screen/
    │   │   ├── CpDashboardScreen.kt
    │   │   ├── ContestReflectionScreen.kt
    │   │   ├── KnowledgeTreeScreen.kt
    │   │   └── DsaTopicDetailScreen.kt
    │   ├── component/
    │   │   ├── ContestResultCard.kt
    │   │   ├── RatingBadge.kt
    │   │   ├── LastSyncedBanner.kt
    │   │   ├── KnowledgeTreeItem.kt
    │   │   ├── ConfidenceLevelIndicator.kt
    │   │   └── RevisionStatusChip.kt
    │   ├── viewmodel/
    │   │   ├── CpDashboardViewModel.kt
    │   │   ├── ContestReflectionViewModel.kt
    │   │   └── KnowledgeTreeViewModel.kt
    │   └── state/
    │       ├── CpDashboardUiState.kt
    │       ├── ContestReflectionUiState.kt
    │       └── KnowledgeTreeUiState.kt
    │
    ├── navigation/
    │   └── CodingNavGraph.kt
    │
    └── di/
        └── CodingModule.kt
```

---

### 4.4 :feature:projects
```
feature/projects/
└── src/main/kotlin/com/studentos/feature/projects/
    │
    ├── data/
    │   └── repository/
    │       └── ProjectRepositoryImpl.kt
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Project.kt
    │   │   ├── ProjectInput.kt
    │   │   ├── Milestone.kt
    │   │   ├── Bug.kt
    │   │   ├── ProjectTask.kt
    │   │   └── ProjectResource.kt
    │   ├── repository/
    │   │   └── ProjectRepository.kt          ← interface
    │   └── usecase/
    │       ├── CreateProjectUseCase.kt
    │       ├── CompleteNextActionUseCase.kt   ← wraps the critical transaction
    │       ├── SetNextActionUseCase.kt
    │       ├── ArchiveProjectUseCase.kt
    │       ├── UpsertMilestoneUseCase.kt
    │       └── UpsertBugUseCase.kt
    │
    ├── presentation/
    │   ├── screen/
    │   │   ├── ProjectListScreen.kt
    │   │   ├── ProjectDetailScreen.kt
    │   │   ├── MilestoneScreen.kt
    │   │   └── BugScreen.kt
    │   ├── component/
    │   │   ├── ProjectCard.kt
    │   │   ├── NextActionBanner.kt
    │   │   ├── TaskList.kt
    │   │   ├── MilestoneRow.kt
    │   │   ├── BugRow.kt
    │   │   ├── ResourceRow.kt
    │   │   └── NewNextActionSheet.kt         ← BottomSheet for next action selection
    │   ├── viewmodel/
    │   │   ├── ProjectListViewModel.kt
    │   │   └── ProjectDetailViewModel.kt
    │   └── state/
    │       ├── ProjectListUiState.kt
    │       └── ProjectDetailUiState.kt
    │
    ├── worker/
    │   └── ProjectInactivityWorker.kt
    │
    ├── navigation/
    │   └── ProjectsNavGraph.kt
    │
    └── di/
        └── ProjectsModule.kt
```

---

### 4.5 :feature:intelligence
```
feature/intelligence/
└── src/main/kotlin/com/studentos/feature/intelligence/
    │
    ├── data/
    │   └── repository/
    │       ├── DailyBriefRepositoryImpl.kt
    │       ├── RecommendationCacheRepositoryImpl.kt
    │       └── AiCallLogRepositoryImpl.kt
    │
    ├── domain/
    │   ├── model/
    │   │   ├── DailyBrief.kt
    │   │   └── AiCallLog.kt
    │   ├── repository/
    │   │   ├── DailyBriefRepository.kt       ← interface
    │   │   ├── RecommendationCacheRepository.kt ← interface
    │   │   └── AiCallLogRepository.kt        ← interface
    │   └── usecase/
    │       └── UpdateDailyScoreUseCase.kt
    │
    ├── orchestrator/
    │   └── IntelligenceOrchestrator.kt       ← @Singleton, wires all sub-components
    │
    ├── presentation/
    │   ├── screen/
    │   │   ├── DailyBriefScreen.kt
    │   │   └── BriefHistoryScreen.kt
    │   ├── component/
    │   │   ├── GuidanceCard.kt               ← displays llm_guidance text
    │   │   ├── AiStatusBadge.kt              ← "AI offline" / "AI quota reached"
    │   │   ├── ScoreProgressBar.kt
    │   │   ├── ClassesSummarySection.kt
    │   │   ├── AttendanceWarningSection.kt
    │   │   ├── UrgentAssignmentsSection.kt
    │   │   ├── FreeSlotsSection.kt
    │   │   ├── DsaSuggestionSection.kt
    │   │   └── ProjectActionSection.kt
    │   ├── viewmodel/
    │   │   ├── DailyBriefViewModel.kt
    │   │   ├── BriefHistoryViewModel.kt
    │   │   └── DailyScoreViewModel.kt
    │   └── state/
    │       ├── DailyBriefUiState.kt
    │       └── BriefHistoryUiState.kt
    │
    ├── worker/
    │   ├── DailyBriefWorker.kt
    │   └── FreeSlotWorker.kt
    │
    ├── navigation/
    │   └── IntelligenceNavGraph.kt
    │
    └── di/
        └── IntelligenceFeatureModule.kt
```

---

### 4.6 :feature:settings
```
feature/settings/
└── src/main/kotlin/com/studentos/feature/settings/
    │
    ├── data/
    │   └── repository/
    │       ├── SettingsRepositoryImpl.kt
    │       └── BackupRepositoryImpl.kt
    │
    ├── domain/
    │   ├── model/
    │   │   └── BackupPayload.kt
    │   ├── repository/
    │   │   ├── SettingsRepository.kt         ← interface
    │   │   └── BackupRepository.kt           ← interface
    │   └── usecase/
    │       ├── ExportUseCase.kt
    │       └── ImportUseCase.kt
    │
    ├── presentation/
    │   ├── screen/
    │   │   ├── SettingsScreen.kt
    │   │   ├── AiSettingsScreen.kt
    │   │   ├── AiDiagnosticsScreen.kt
    │   │   ├── NotificationSettingsScreen.kt
    │   │   └── BackupScreen.kt
    │   ├── component/
    │   │   ├── SettingsSectionHeader.kt
    │   │   ├── SliderSettingRow.kt
    │   │   ├── ToggleSettingRow.kt
    │   │   ├── DropdownSettingRow.kt
    │   │   ├── ApiKeyField.kt               ← masked input, writes to EncryptedSharedPrefs
    │   │   ├── AiCallLogRow.kt
    │   │   └── DailyCostEstimate.kt
    │   ├── viewmodel/
    │   │   ├── SettingsViewModel.kt
    │   │   ├── AiSettingsViewModel.kt
    │   │   ├── AiDiagnosticsViewModel.kt
    │   │   └── BackupViewModel.kt
    │   └── state/
    │       ├── SettingsUiState.kt
    │       ├── AiSettingsUiState.kt
    │       └── BackupUiState.kt
    │
    ├── navigation/
    │   └── SettingsNavGraph.kt
    │
    └── di/
        └── SettingsModule.kt
```

---

## 5. Hilt Dependency Graph

### 5.1 Provision map (what provides what)

```
@HiltAndroidApp
  StudentOsApp
      │
      ├── DatabaseModule                    (:core:database)
      │     provides:
      │       @Singleton AppDatabase
      │       @Singleton SubjectDao
      │       @Singleton TimetableSlotDao
      │       @Singleton ClassEventDao
      │       @Singleton AssignmentDao
      │       @Singleton CpProfileDao
      │       @Singleton CpContestDao
      │       @Singleton CpReflectionDao
      │       @Singleton DsaCategoryDao
      │       @Singleton DsaTopicDao
      │       @Singleton ProjectDao
      │       @Singleton MilestoneDao
      │       @Singleton BugDao
      │       @Singleton ProjectTaskDao
      │       @Singleton ProjectResourceDao
      │       @Singleton DailyBriefDao
      │       @Singleton RecommendationCacheDao
      │       @Singleton AiCallLogDao
      │       @Singleton SettingsDao
      │
      ├── EventsModule                      (:core:events)
      │     provides:
      │       @Singleton AppEventBus → AppEventBusImpl
      │
      ├── SyncModule                        (:core:sync)
      │     provides:
      │       @Singleton @Named("cp") OkHttpClient
      │       @Singleton @Named("cp") Retrofit
      │       @Singleton CodeChefApiService
      │       @Singleton CodeforcesApiService
      │
      ├── IntelligenceModule                (:core:intelligence)
      │     provides:
      │       @Singleton @Named("llm") OkHttpClient  (30s timeout, retry interceptor)
      │       @Singleton @Named("llm") Retrofit
      │       @Singleton DeepSeekApiService
      │       @Singleton LLMProvider → LLMProviderFactory.create()
      │       @Singleton SnapshotBuilder
      │       @Singleton SnapshotDiffer
      │       @Singleton PromptBuilder
      │       @Singleton DeterministicFallback
      │       @Singleton RecommendationCache → RecommendationCacheImpl
      │       @Singleton RateLimiter
      │
      ├── NotificationsModule               (:core:notifications)
      │     provides:
      │       @Singleton NotificationScheduler → NotificationSchedulerImpl
      │       @Singleton NotificationRescheduler
      │
      ├── AttendanceModule                  (:feature:attendance)
      │     binds:
      │       SubjectRepository → SubjectRepositoryImpl
      │       TimetableRepository → TimetableRepositoryImpl
      │       ClassEventRepository → ClassEventRepositoryImpl
      │
      ├── AssignmentsModule                 (:feature:assignments)
      │     binds:
      │       AssignmentRepository → AssignmentRepositoryImpl
      │
      ├── CodingModule                      (:feature:coding)
      │     binds:
      │       CpRepository → CpRepositoryImpl
      │       DsaRepository → DsaRepositoryImpl
      │
      ├── ProjectsModule                    (:feature:projects)
      │     binds:
      │       ProjectRepository → ProjectRepositoryImpl
      │
      ├── IntelligenceFeatureModule         (:feature:intelligence)
      │     provides:
      │       @Singleton IntelligenceOrchestrator
      │     binds:
      │       DailyBriefRepository → DailyBriefRepositoryImpl
      │       RecommendationCacheRepository → RecommendationCacheRepositoryImpl
      │       AiCallLogRepository → AiCallLogRepositoryImpl
      │
      └── SettingsModule                    (:feature:settings)
            binds:
              SettingsRepository → SettingsRepositoryImpl
              BackupRepository → BackupRepositoryImpl
```

### 5.2 ViewModel injection scope

All ViewModels use `@HiltViewModel`. They are `@ViewModelScoped` by default — created per screen, destroyed when the screen leaves the back stack.

`IntelligenceOrchestrator` is `@Singleton` — it must survive configuration changes and back stack movements because it holds the `AppEventBus` subscription and the 30-second debounce state.

### 5.3 Worker injection

All Workers use `@HiltWorker` + `@AssistedInject`. WorkManager is initialised with `HiltWorkerFactory` via a custom `Configuration.Provider` in `StudentOsApp`.

Workers requiring injection:
```
CpSyncWorker                 ← @HiltWorker
ContestReminderWorker        ← @HiltWorker
AssignmentReminderWorker     ← @HiltWorker
DailyBriefWorker             ← @HiltWorker
FreeSlotWorker               ← @HiltWorker
ProjectInactivityWorker      ← @HiltWorker
ClassReminderWorker          ← @HiltWorker
```

---

## 6. Navigation Graph

### 6.1 Screen inventory and routes

```
Route constant location: each NavGraph file defines its own routes as a sealed class or object.

ATTENDANCE module
  attendance/weekly              ← WeeklyViewScreen (default tab)
  attendance/calendar            ← CalendarViewScreen
  attendance/analytics/{subjectId} ← AttendanceAnalyticsScreen
  attendance/ocr-preview         ← OcrPreviewScreen
  attendance/edit-timetable      ← EditTimetableScreen

ASSIGNMENTS module
  assignments/list               ← AssignmentListScreen (default tab)
  assignments/detail/{id}        ← AssignmentDetailScreen

CODING module
  coding/dashboard               ← CpDashboardScreen (default tab)
  coding/reflection/{contestId}  ← ContestReflectionScreen
  coding/knowledge-tree          ← KnowledgeTreeScreen
  coding/topic-detail/{topicId}  ← DsaTopicDetailScreen

PROJECTS module
  projects/list                  ← ProjectListScreen (default tab)
  projects/detail/{projectId}    ← ProjectDetailScreen
  projects/milestone/{projectId} ← MilestoneScreen
  projects/bug/{projectId}       ← BugScreen

INTELLIGENCE module
  intelligence/daily-brief       ← DailyBriefScreen (default tab / home)
  intelligence/history           ← BriefHistoryScreen

SETTINGS module
  settings/main                  ← SettingsScreen
  settings/ai                    ← AiSettingsScreen
  settings/ai-diagnostics        ← AiDiagnosticsScreen
  settings/notifications         ← NotificationSettingsScreen
  settings/backup                ← BackupScreen
```

### 6.2 Navigation paths

```
Bottom navigation tabs (always visible):
  [Daily Brief] [Attendance] [Assignments] [Coding] [Projects]

  Each tab maintains its own back stack.
  Settings is accessible via a top-bar icon from any screen.

Deep-link paths triggered by notifications:
  DAILY_BRIEF notification         → intelligence/daily-brief
  ASSIGNMENT_REMINDER              → assignments/detail/{id}
  CLASS_REMINDER                   → attendance/weekly
  CONTEST_REMINDER                 → coding/dashboard
  FREE_SLOT_RECOMMENDATION         → intelligence/daily-brief
  INACTIVE_PROJECT_REMINDER        → projects/detail/{projectId}

Intra-module navigation:
  WeeklyViewScreen
    → AttendanceAnalyticsScreen (tap subject row)
    → OcrPreviewScreen (import FAB)
    → EditTimetableScreen (edit icon)

  AssignmentListScreen
    → AssignmentDetailScreen (tap card)

  CpDashboardScreen
    → ContestReflectionScreen (tap contest card with no reflection)
    → KnowledgeTreeScreen (tab)

  KnowledgeTreeScreen
    → DsaTopicDetailScreen (tap topic)

  ProjectListScreen
    → ProjectDetailScreen (tap project card)

  ProjectDetailScreen
    → MilestoneScreen (milestones tab)
    → BugScreen (bugs tab)

  DailyBriefScreen (each section deep-links into its module):
    → attendance/weekly            (classes section)
    → attendance/analytics/{id}    (attendance warning section)
    → assignments/detail/{id}      (assignment section)
    → coding/knowledge-tree        (DSA section)
    → projects/detail/{id}         (project action section)

  SettingsScreen
    → AiSettingsScreen
    → AiDiagnosticsScreen
    → NotificationSettingsScreen
    → BackupScreen
```

### 6.3 NavGraph registration

Each module's `*NavGraph.kt` file provides a `fun NavGraphBuilder.attendanceNavGraph(navController)` extension. `AppNavHost.kt` in `:app` calls all of them. `ModuleRegistry` is the registry that `:app` reads to discover which extensions to call — this is the extension point for adding new modules.

---

## 7. File Naming Conventions

Every file has exactly one correct name. No deviation.

### Entities (in :core:database)
```
Pattern:  <Domain>Entity.kt
Examples: SubjectEntity.kt
          TimetableSlotEntity.kt
          ClassEventEntity.kt
          AssignmentEntity.kt
          DailyBriefEntity.kt
          AiCallLogEntity.kt
```

### DAOs (in :core:database)
```
Pattern:  <Domain>Dao.kt
Examples: SubjectDao.kt
          ClassEventDao.kt
          DailyBriefDao.kt
```

### Repository interfaces (in domain/repository/ of each feature)
```
Pattern:  <Domain>Repository.kt
Examples: SubjectRepository.kt
          ClassEventRepository.kt
          AssignmentRepository.kt
          ProjectRepository.kt
```

### Repository implementations (in data/repository/ of each feature)
```
Pattern:  <Domain>RepositoryImpl.kt
Examples: SubjectRepositoryImpl.kt
          ClassEventRepositoryImpl.kt
          AssignmentRepositoryImpl.kt
```

### Use Cases (in domain/usecase/ of each feature)
```
Pattern:  <Verb><Noun>UseCase.kt
Examples: ImportTimetableUseCase.kt
          UpdateClassEventStatusUseCase.kt
          CompleteNextActionUseCase.kt
          CreateAssignmentUseCase.kt
          ExportUseCase.kt
          ImportUseCase.kt
```

### Domain Models (in domain/model/ of each feature)
```
Pattern:  <Domain>.kt   (no suffix — these are the clean domain objects)
Examples: Subject.kt
          ClassEvent.kt
          Assignment.kt
          DsaTopic.kt
          Project.kt
```

### ViewModels
```
Pattern:  <Screen>ViewModel.kt
Examples: WeeklyViewModel.kt
          AssignmentListViewModel.kt
          ProjectDetailViewModel.kt
          DailyBriefViewModel.kt
          DailyScoreViewModel.kt
```

### UI State
```
Pattern:  <Screen>UiState.kt    (sealed class or data class)
Examples: WeeklyUiState.kt
          AssignmentListUiState.kt
          DailyBriefUiState.kt
```

### Screens (Composable root functions)
```
Pattern:  <Screen>Screen.kt     (file name matches the @Composable function name)
Examples: WeeklyViewScreen.kt       → @Composable fun WeeklyViewScreen(...)
          AssignmentListScreen.kt   → @Composable fun AssignmentListScreen(...)
          DailyBriefScreen.kt       → @Composable fun DailyBriefScreen(...)
```

### Composable Components
```
Pattern:  <Purpose>.kt   (descriptive noun or noun-phrase, no "Screen" suffix)
Examples: ClassEventCard.kt
          BunkCalculatorWidget.kt
          GuidanceCard.kt
          AiStatusBadge.kt
          NextActionBanner.kt
```

### Workers
```
Pattern:  <Purpose>Worker.kt
Examples: AssignmentReminderWorker.kt
          DailyBriefWorker.kt
          CpSyncWorker.kt
          ProjectInactivityWorker.kt
          ClassReminderWorker.kt
```

### Navigation
```
Pattern:  <Module>NavGraph.kt
Examples: AttendanceNavGraph.kt
          AssignmentsNavGraph.kt
          IntelligenceNavGraph.kt
```

### Hilt Modules
```
Pattern:  <Module>Module.kt
Examples: DatabaseModule.kt
          AttendanceModule.kt
          IntelligenceModule.kt
          IntelligenceFeatureModule.kt    ← when two modules in different Gradle modules have similar names
```

### Events
```
AppEvent.kt          ← sealed class (single file, in :core:events)
AppEventBus.kt       ← interface
AppEventBusImpl.kt   ← implementation
```

### LLM Provider
```
LLMProvider.kt          ← interface
LLMResult.kt            ← sealed class
FailureReason.kt        ← enum
DeepSeekProvider.kt     ← implementation
MockProvider.kt         ← test/no-key implementation
LLMProviderFactory.kt   ← factory
```

---

## 8. Dependency Rules

### 8.1 Layer dependency direction

```
Presentation  →  Domain  →  Data  →  :core:database
     ↓                ↑
  (ViewModels     (pure Kotlin,
   import         no Android,
   Use Cases)     no Room)
```

The arrow points in the direction of the import. The domain layer must never import from the presentation or data layers.

### 8.2 What each layer may import

#### Presentation layer (`presentation/`)
```
MAY import:
  - domain/model/*           (to display data)
  - domain/usecase/*         (to trigger actions)
  - domain/repository/*      (interfaces only, for constructor injection)
  - :core:ui/*               (shared Composables and theme)
  - :core:events/AppEvent    (to observe events if needed in ViewModel)
  - Jetpack Compose
  - Hilt (@HiltViewModel)
  - Kotlin coroutines (viewModelScope)
  - Android lifecycle (ViewModel, StateFlow, collectAsState)

FORBIDDEN:
  - data/repository/*Impl    (never import the implementation)
  - :core:database/entity/*  (never use Room entities in UI)
  - :core:database/dao/*     (never call DAOs from ViewModels)
  - Any other :feature:* module
  - Retrofit / OkHttp
  - Room annotations
```

#### Domain layer (`domain/`)
```
MAY import:
  - domain/model/*           (own models only)
  - domain/repository/*      (own interfaces only)
  - :core:events/AppEvent    (to read event types in use cases if needed)
  - Pure Kotlin stdlib
  - Kotlin coroutines

FORBIDDEN:
  - data/*                   (no implementation details)
  - :core:database/*         (no Room, no DAOs, no entities)
  - Android SDK classes      (no Context, no Uri, no Log)
  - :core:intelligence/*     (except in :feature:intelligence domain layer)
  - Any Jetpack library
```

#### Data layer (`data/`)
```
MAY import:
  - domain/model/*           (to map entities → domain models)
  - domain/repository/*      (to implement interfaces)
  - :core:database/dao/*     (to call queries)
  - :core:database/entity/*  (to map domain models → entities)
  - :core:events/AppEventBus (to emit events after writes)
  - Kotlin coroutines
  - Android Uri / File       (for attachment handling only, in AssignmentRepositoryImpl)

FORBIDDEN:
  - presentation/*
  - :core:intelligence/*     (except in :feature:intelligence data layer)
  - Retrofit (except in :core:sync and :core:intelligence)
  - Compose
```

#### :core:database
```
MAY import:
  - Room
  - Kotlin stdlib

FORBIDDEN:
  - Everything else. Zero business logic. Zero feature code.
```

#### :core:events
```
MAY import:
  - Kotlin coroutines (SharedFlow, MutableSharedFlow)
  - Hilt (for @Singleton)

FORBIDDEN:
  - Everything else. No Room. No Android SDK. No feature code.
```

#### :core:intelligence
```
MAY import:
  - :core:database/dao/*     (SnapshotBuilder reads DAOs)
  - :core:database/entity/*  (for DAO return types)
  - :core:events/*           (to declare EventBus dependency)
  - :core:intelligence/model/* (own models)
  - Retrofit / OkHttp        (for DeepSeekProvider)
  - Kotlin coroutines
  - Android context          (only for connectivity check in LLMProvider.isAvailable())

FORBIDDEN:
  - Any :feature:* module
  - Compose
  - ViewModel
  - Room @Transaction (transactions belong in repositories in feature modules)
```

### 8.3 Forbidden patterns (anywhere in codebase)
```
1. ViewModel calling a DAO directly
2. Composable calling a Repository or UseCase directly (must go through ViewModel)
3. Repository importing another Repository
4. UseCase importing a DAO
5. Domain model extending a Room @Entity
6. @Entity class containing business logic methods
7. LLM response text being parsed for structured data (numbers, dates, status values)
8. AppEventBus.emit() called from ViewModel or UseCase (only from Repository layer)
9. Any :feature:* module depending on another :feature:* module
10. Any business rule (attendance formula, score weights, bunk calc) inside a DAO or @Entity
```

---

## 9. Coding Conventions

### 9.1 Flow vs StateFlow

```
Rule 1: DAOs return Flow<T> — reactive DB queries.
Rule 2: Repositories expose Flow<T> for stream data, and suspend fun for one-shot reads.
Rule 3: ViewModels convert Flow<T> to StateFlow<UiState> using stateIn(viewModelScope).
Rule 4: Composables collect StateFlow using collectAsStateWithLifecycle().
Rule 5: Never use LiveData — this project uses Flow throughout.
Rule 6: One-shot actions (create, update, delete) are suspend functions, not Flows.

Repository signature pattern:
  fun getX(): Flow<X>              ← stream
  suspend fun getXById(id): X?     ← one-shot read
  suspend fun createX(input): Long ← write, returns new id
  suspend fun updateX(x: X)        ← write, no return
  suspend fun deleteX(id: Long)    ← write

ViewModel pattern:
  val uiState: StateFlow<XUiState> = repository.getX()
      .map { data -> XUiState.Success(data) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XUiState.Loading)
```

### 9.2 UiState sealed class pattern

Every screen has exactly one UiState. Never use multiple separate state variables.

```
Sealed class structure:
  sealed class XUiState {
      object Loading : XUiState()
      data class Success(val data: XData) : XUiState()
      data class Error(val message: String) : XUiState()
  }

Composable consumes:
  when (uiState) {
      is Loading → LoadingOverlay()
      is Success → XContent(uiState.data)
      is Error   → ErrorView(uiState.message)
  }
```

### 9.3 suspend functions

```
Rule: Every write operation is a suspend function.
Rule: Every one-shot read is a suspend function.
Rule: Use cases are suspend functions called from viewModelScope.launch { }.
Rule: Repository suspend functions run on Dispatchers.IO — callers do not need to specify dispatcher.
      Repositories use withContext(Dispatchers.IO) { } internally.
Rule: SnapshotBuilder.build() is a suspend function running on Dispatchers.IO.
Rule: Workers use CoroutineWorker — already on a background thread.
```

### 9.4 Transactions

```
Rule: All Room @Transaction functions live in DAOs or are called via RepositoryImpl.
Rule: @Transaction functions in DAOs are suspend functions.
Rule: RepositoryImpl methods that span multiple DAO calls use withTransaction { }.
Rule: AppEventBus.emit() is called AFTER the transaction commits, never inside it.
      Reason: if the transaction rolls back, the event must not be delivered.
Rule: Use cases never call @Transaction directly — they call repository methods.
```

### 9.5 Result wrapper

Use a simple sealed class for use-case outcomes. Do not use Kotlin's built-in `Result<T>` — it wraps exceptions, which hides intent.

```
Sealed class:
  sealed class AppResult<out T> {
      data class Success<T>(val data: T) : AppResult<T>()
      data class Failure(val reason: AppError) : AppResult<Nothing>()
  }

  sealed class AppError {
      data class DatabaseError(val message: String) : AppError()
      data class NetworkError(val message: String) : AppError()
      data class ValidationError(val message: String) : AppError()
      object Offline : AppError()
      object RateLimited : AppError()
  }

Location: core/events/ or a new :core:common/ module if it grows.
          For V1, define in :core:events.

Use cases that can fail return AppResult<T>.
Use cases that cannot fail (e.g., pure local writes) return Unit or the new entity id.
ViewModels map AppResult to UiState.
```

### 9.6 Error handling rules

```
Rule 1: DAOs do not catch exceptions — they propagate up.
Rule 2: RepositoryImpl catches SQLiteException and wraps in AppResult.Failure(DatabaseError).
Rule 3: LLM call failures are caught in DeepSeekProvider and returned as LLMResult.Failure.
        They never propagate as exceptions to IntelligenceOrchestrator.
Rule 4: IntelligenceOrchestrator never throws — it always returns either LLM guidance or
        deterministic fallback. The caller never sees an exception from the intelligence layer.
Rule 5: Workers catch all exceptions in doWork() and return Result.failure(). They do not crash.
Rule 6: ViewModels catch exceptions from use case calls and emit UiState.Error.
Rule 7: The UI never shows raw exception messages — always a user-friendly string resource.
```

### 9.7 Immutability

```
Rule: All domain models are data classes with val properties only.
Rule: All UiState classes are data classes or sealed classes with val properties only.
Rule: Room @Entity classes are data classes with var properties (required by Room for copy()).
Rule: Never mutate a domain model — use data class copy() instead.
```

### 9.8 Coroutine scope rules

```
viewModelScope     ← ViewModel coroutines (UI-tied, auto-cancelled)
lifecycleScope     ← Composable/Activity coroutines (use sparingly)
applicationScope   ← Long-lived operations in IntelligenceOrchestrator
                     (Provided by Hilt as @Singleton CoroutineScope)
WorkManager        ← Background tasks (CpSyncWorker, DailyBriefWorker, etc.)

IntelligenceOrchestrator uses applicationScope, not viewModelScope.
Reason: it must survive screen navigation and collect AppEventBus continuously.
```

---

## 10. Complete Layout for :feature:attendance (Reference Implementation)

This section shows every class, its exact file location, and its single responsibility.

```
feature/attendance/
└── src/
    ├── main/kotlin/com/studentos/feature/attendance/
    │
    │   ── data/ ─────────────────────────────────────────────────────────
    │   ├── data/
    │   │   ├── repository/
    │   │   │   ├── SubjectRepositoryImpl.kt
    │   │   │   │     Responsibility: implements SubjectRepository.
    │   │   │   │     Calls: SubjectDao.
    │   │   │   │     Emits: no events (subjects are not intelligence triggers).
    │   │   │   │     Transactions: none (single-row writes).
    │   │   │   │
    │   │   │   ├── TimetableRepositoryImpl.kt
    │   │   │   │     Responsibility: implements TimetableRepository.
    │   │   │   │     Calls: TimetableSlotDao, ClassEventDao.
    │   │   │   │     Emits: no events.
    │   │   │   │     Transactions: importTimetable() — T1 from blueprint.
    │   │   │   │     Contains: 365-day generation horizon guard.
    │   │   │   │
    │   │   │   └── ClassEventRepositoryImpl.kt
    │   │   │         Responsibility: implements ClassEventRepository.
    │   │   │         Calls: ClassEventDao, AppEventBus.
    │   │   │         Emits: AppEvent.AttendanceMarked after updateStatus().
    │   │   │         Transactions: none.
    │   │   │
    │   │   └── ocr/
    │   │       ├── OcrProcessor.kt
    │   │       │     Responsibility: ML Kit integration.
    │   │       │     Input: Bitmap. Output: OcrResult (raw text blocks + confidence).
    │   │       │     No DB access.
    │   │       │
    │   │       └── TimetableFieldMapper.kt
    │   │             Responsibility: maps OcrResult → List<ParsedTimetableRow>.
    │   │             Each row has: day, start_time, end_time, subject, location, confidence.
    │   │             Pure Kotlin. No Android. No DB.
    │   │
    │   ── domain/ ──────────────────────────────────────────────────────
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Subject.kt
    │   │   │   │     Fields: id, name, isArchived.
    │   │   │   │     data class, val only.
    │   │   │   │
    │   │   │   ├── TimetableSlot.kt
    │   │   │   │     Fields: id, subjectId, dayOfWeek, startTime, endTime,
    │   │   │   │             location, weekParity, validFrom, validUntil.
    │   │   │   │
    │   │   │   ├── ClassEvent.kt
    │   │   │   │     Fields: id, timetableSlotId, subjectId, scheduledAt,
    │   │   │   │             endAt, status, isExtra, linkedSlotId, updatedAt.
    │   │   │   │     status is a String — use ClassEventStatus enum for validation.
    │   │   │   │
    │   │   │   ├── ClassEventStatus.kt
    │   │   │   │     enum: UNMARKED, PRESENT, ABSENT, CANCELLED, HOLIDAY, EXTRA_CLASS
    │   │   │   │
    │   │   │   ├── AttendanceSummary.kt
    │   │   │   │     Fields: present, absent, cancelled, holiday,
    │   │   │   │             extraPresent, total, percentage.
    │   │   │   │     data class. Computed by AttendanceCalculator, not DB.
    │   │   │   │
    │   │   │   ├── SubjectAttendanceSummary.kt
    │   │   │   │     Fields: subject, summary.
    │   │   │   │     Used by RecalibrationUseCase and SnapshotBuilder.
    │   │   │   │
    │   │   │   └── OcrResult.kt
    │   │   │         Fields: rows: List<ParsedTimetableRow>, rawText: String.
    │   │   │         ParsedTimetableRow: day, startTime, endTime, subject,
    │   │   │                             location, confidence.
    │   │   │
    │   │   ├── repository/
    │   │   │   ├── SubjectRepository.kt          ← interface
    │   │   │   ├── TimetableRepository.kt         ← interface
    │   │   │   └── ClassEventRepository.kt        ← interface
    │   │   │
    │   │   ├── usecase/
    │   │   │   ├── ImportTimetableUseCase.kt
    │   │   │   │     Input: List<ParsedTimetableRow>, replaceExisting: Boolean.
    │   │   │   │     Calls: TimetableRepository.importTimetable().
    │   │   │   │     Returns: AppResult<Unit>.
    │   │   │   │
    │   │   │   ├── UpdateClassEventStatusUseCase.kt
    │   │   │   │     Input: eventId: Long, status: ClassEventStatus.
    │   │   │   │     Validates future-event confirmation flag.
    │   │   │   │     Calls: ClassEventRepository.updateStatus().
    │   │   │   │     Returns: AppResult<Unit>.
    │   │   │   │
    │   │   │   ├── AddExtraClassUseCase.kt
    │   │   │   │     Input: subjectId, scheduledAt, endAt, linkedSlotId?.
    │   │   │   │     Returns: AppResult<Long> (new event id).
    │   │   │   │
    │   │   │   ├── ArchiveSubjectUseCase.kt
    │   │   │   │     Input: subjectId: Long.
    │   │   │   │     Validates no active timetable slots (ask user to confirm).
    │   │   │   │     Calls: SubjectRepository.archiveSubject().
    │   │   │   │
    │   │   │   └── RecalibrationUseCase.kt
    │   │   │         Input: none.
    │   │   │         Reads all ClassEvents for all subjects.
    │   │   │         Recomputes AttendanceSummary for each subject.
    │   │   │         Pure read — no writes in V1.
    │   │   │         Called at App startup from App.onCreate() on IO dispatcher.
    │   │   │
    │   │   └── calculator/
    │   │       ├── AttendanceCalculator.kt
    │   │       │     Input: present, absent, cancelled, holiday, extraPresent counts.
    │   │       │     Output: percentage: Double (rounded to 2 decimal places).
    │   │       │     Formula: (present + extraPresent) / (total - cancelled - holiday) × 100.
    │   │       │     Pure Kotlin object. No constructor. No state. No Android deps.
    │   │       │     100% unit-testable without any mocking.
    │   │       │
    │   │       └── BunkCalculator.kt
    │   │             Input: AttendanceSummary, threshold: Int.
    │   │             Output: canSkip: Int, mustAttend: Int.
    │   │             Pure Kotlin object. 100% unit-testable.
    │   │             Property-based tests verify consistency with AttendanceCalculator formula.
    │   │
    │   ── presentation/ ─────────────────────────────────────────────────
    │   ├── presentation/
    │   │   ├── screen/
    │   │   │   ├── WeeklyViewScreen.kt
    │   │   │   │     Root @Composable. Receives WeeklyViewModel via hiltViewModel().
    │   │   │   │     Collects uiState: WeeklyUiState.
    │   │   │   │     Displays 7-column day grid.
    │   │   │   │
    │   │   │   ├── CalendarViewScreen.kt
    │   │   │   │     Root @Composable. Calendar grid with event dots.
    │   │   │   │
    │   │   │   ├── AttendanceAnalyticsScreen.kt
    │   │   │   │     Root @Composable. Per-subject breakdown + BunkCalculatorWidget.
    │   │   │   │
    │   │   │   ├── OcrPreviewScreen.kt
    │   │   │   │     Root @Composable. Editable table of OCR-extracted rows.
    │   │   │   │     Low-confidence fields shown in amber.
    │   │   │   │
    │   │   │   └── EditTimetableScreen.kt
    │   │   │         Root @Composable. Manual slot editor.
    │   │   │
    │   │   ├── component/
    │   │   │   ├── ClassEventCard.kt
    │   │   │   │     Input: ClassEvent, onStatusSelected callback.
    │   │   │   │     Displays status badge, time, subject name.
    │   │   │   │     Shows confirmation dialog for future events.
    │   │   │   │
    │   │   │   ├── AttendancePercentageRow.kt
    │   │   │   │     Input: SubjectAttendanceSummary, isBelow: Boolean.
    │   │   │   │     Red tint when isBelow is true.
    │   │   │   │
    │   │   │   ├── BunkCalculatorWidget.kt
    │   │   │   │     Input: AttendanceSummary, threshold: Int.
    │   │   │   │     Displays canSkip / mustAttend with threshold label.
    │   │   │   │
    │   │   │   ├── DayColumn.kt
    │   │   │   │     Input: dayLabel, List<ClassEvent>.
    │   │   │   │     Used in WeeklyViewScreen.
    │   │   │   │
    │   │   │   └── OcrFieldRow.kt
    │   │   │         Input: ParsedTimetableRow, onEdit callback.
    │   │   │         Amber highlight on low-confidence fields.
    │   │   │
    │   │   ├── viewmodel/
    │   │   │   ├── WeeklyViewModel.kt
    │   │   │   │     @HiltViewModel.
    │   │   │   │     Depends on: ClassEventRepository, SubjectRepository, SettingsRepository.
    │   │   │   │     Exposes: uiState: StateFlow<WeeklyUiState>.
    │   │   │   │     Actions: onStatusSelected(eventId, status), onWeekChanged(offset).
    │   │   │   │
    │   │   │   ├── CalendarViewModel.kt
    │   │   │   │     @HiltViewModel.
    │   │   │   │     Exposes: uiState: StateFlow<CalendarUiState>.
    │   │   │   │     Actions: onMonthChanged(offset).
    │   │   │   │
    │   │   │   ├── AttendanceAnalyticsViewModel.kt
    │   │   │   │     @HiltViewModel.
    │   │   │   │     Depends on: ClassEventRepository, SettingsRepository.
    │   │   │   │     Exposes: uiState: StateFlow<AnalyticsUiState>.
    │   │   │   │
    │   │   │   └── OcrPreviewViewModel.kt
    │   │   │         @HiltViewModel.
    │   │   │         Depends on: ImportTimetableUseCase, OcrProcessor, TimetableFieldMapper.
    │   │   │         Exposes: uiState: StateFlow<OcrPreviewUiState>.
    │   │   │         Actions: onImageSelected(uri), onRowEdited(...), onConfirm().
    │   │   │
    │   │   └── state/
    │   │       ├── WeeklyUiState.kt
    │   │       │     sealed class: Loading | Success(days: List<DayEvents>) | Error
    │   │       │
    │   │       ├── CalendarUiState.kt
    │   │       │     sealed class: Loading | Success(events: Map<LocalDate, List<ClassEvent>>)
    │   │       │
    │   │       ├── AnalyticsUiState.kt
    │   │       │     sealed class: Loading | Success(summaries: List<SubjectAttendanceSummary>)
    │   │       │
    │   │       └── OcrPreviewUiState.kt
    │   │             sealed class: Idle | Processing | Review(rows, canConfirm) | Saving | Error
    │   │
    │   ── navigation/ ───────────────────────────────────────────────────
    │   ├── navigation/
    │   │   └── AttendanceNavGraph.kt
    │   │         fun NavGraphBuilder.attendanceNavGraph(navController: NavHostController)
    │   │         Registers: weekly, calendar, analytics/{subjectId},
    │   │                    ocr-preview, edit-timetable routes.
    │   │
    │   ── di/ ───────────────────────────────────────────────────────────
    │   └── di/
    │       └── AttendanceModule.kt
    │             @InstallIn(SingletonComponent)
    │             @Binds SubjectRepository → SubjectRepositoryImpl
    │             @Binds TimetableRepository → TimetableRepositoryImpl
    │             @Binds ClassEventRepository → ClassEventRepositoryImpl
    │
    └── test/kotlin/com/studentos/feature/attendance/
        ├── calculator/
        │   ├── AttendanceCalculatorTest.kt   ← unit tests for 10 known inputs
        │   └── BunkCalculatorTest.kt         ← property-based tests (Kotest forAll)
        ├── usecase/
        │   └── ImportTimetableUseCaseTest.kt ← integration test with in-memory Room
        └── viewmodel/
            └── WeeklyViewModelTest.kt        ← turbine + fake repository
```

---

## 11. Test Folder Structure

Every Gradle module follows the same test layout:

```
module/
└── src/
    ├── main/...
    ├── test/kotlin/...          ← JVM unit tests (pure Kotlin, Kotest, MockK)
    └── androidTest/kotlin/...   ← Instrumented tests (Room in-memory, Espresso, Compose)
```

Test file naming:
```
Pattern:  <ClassName>Test.kt
Examples: AttendanceCalculatorTest.kt
          BunkCalculatorTest.kt
          SubjectRepositoryImplTest.kt   ← uses in-memory Room
          WeeklyViewModelTest.kt         ← uses Turbine + fake repository
          AppDatabaseMigrationTest.kt    ← in androidTest/
```

---

## 12. Quick Reference: "Where does X go?"

| Class type | Location |
|---|---|
| Room `@Entity` | `:core:database/entity/` |
| Room `@Dao` | `:core:database/dao/` |
| Room `@TypeConverter` | `:core:database/converter/` |
| Multi-table query result (`@Relation`) | `:core:database/relation/` |
| `AppDatabase` | `:core:database/` root |
| Database migrations | `:core:database/DatabaseMigrations.kt` |
| Hilt `@Module` for DAOs | `:core:database/di/DatabaseModule.kt` |
| `AppEvent` sealed class | `:core:events/AppEvent.kt` |
| `AppEventBus` interface | `:core:events/AppEventBus.kt` |
| `LLMProvider` interface | `:core:intelligence/provider/LLMProvider.kt` |
| `DeepSeekProvider` | `:core:intelligence/provider/DeepSeekProvider.kt` |
| `SnapshotBuilder` | `:core:intelligence/snapshot/SnapshotBuilder.kt` |
| `DeterministicFallback` | `:core:intelligence/fallback/DeterministicFallback.kt` |
| `IntelligenceOrchestrator` | `:feature:intelligence/orchestrator/` |
| Domain model | `feature/<module>/domain/model/` |
| Repository interface | `feature/<module>/domain/repository/` |
| Repository implementation | `feature/<module>/data/repository/` |
| Use case | `feature/<module>/domain/usecase/` |
| Pure calculator / logic | `feature/<module>/domain/calculator/` |
| `@HiltViewModel` | `feature/<module>/presentation/viewmodel/` |
| Screen root `@Composable` | `feature/<module>/presentation/screen/` |
| Reusable component `@Composable` | `feature/<module>/presentation/component/` |
| UiState sealed class | `feature/<module>/presentation/state/` |
| `NavGraphBuilder` extension | `feature/<module>/navigation/` |
| Feature Hilt `@Module` (binds) | `feature/<module>/di/` |
| `CoroutineWorker` | `feature/<module>/worker/` |
| Shared UI theme / colors | `:core:ui/theme/` |
| Shared reusable Composable | `:core:ui/component/` |
| Retrofit API service | `:core:sync/api/` or `:core:intelligence/api/` |
| DTO (API response) | `api/dto/` subfolder of the relevant core module |
| API key storage | `EncryptedSharedPreferences` — NOT in Room, NOT in any file |
| Settings values | `:core:database` via `SettingsDao` + `SettingsRepositoryImpl` in `:feature:settings` |
