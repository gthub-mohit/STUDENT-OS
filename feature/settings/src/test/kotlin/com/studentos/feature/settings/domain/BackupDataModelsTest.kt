package com.studentos.feature.settings.domain

import com.studentos.feature.settings.domain.model.BackupProject
import com.studentos.feature.settings.domain.model.BackupProjectTask
import com.studentos.feature.settings.domain.model.BackupSubject
import com.studentos.feature.settings.domain.model.StudentOsBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupDataModelsTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun serializeAndDeserializeBackup_preservesAllFields() {
        val backup = StudentOsBackup(
            version = 1,
            appVersion = "1.0",
            exportedAt = 1700000000000L,
            subjects = listOf(
                BackupSubject(id = 1L, name = "Data Structures", archivedAt = null),
                BackupSubject(id = 2L, name = "Operating Systems", archivedAt = 1700000001000L)
            ),
            projects = listOf(
                BackupProject(
                    id = 10L,
                    title = "Student OS Android",
                    inactivityThresholdDays = 7,
                    lastActivityAt = 1700000002000L
                )
            ),
            projectTasks = listOf(
                BackupProjectTask(
                    id = 100L,
                    projectId = 10L,
                    title = "Build Settings Screen",
                    isNextAction = true,
                    isParallel = false,
                    completedAt = null,
                    sortOrder = 1,
                    dependencyTaskId = null,
                    priority = "HIGH",
                    deadline = 1700000005000L
                )
            )
        )

        val jsonString = json.encodeToString(backup)
        val deserialized = json.decodeFromString<StudentOsBackup>(jsonString)

        assertEquals(backup.version, deserialized.version)
        assertEquals(backup.appVersion, deserialized.appVersion)
        assertEquals(2, deserialized.subjects.size)
        assertEquals("Data Structures", deserialized.subjects[0].name)
        assertEquals(1, deserialized.projects.size)
        assertEquals("Student OS Android", deserialized.projects[0].title)
        assertEquals(1, deserialized.projectTasks.size)
        assertEquals("Build Settings Screen", deserialized.projectTasks[0].title)
        assertEquals("HIGH", deserialized.projectTasks[0].priority)
    }
}
