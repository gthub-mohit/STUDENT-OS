package com.studentos.feature.intelligence.domain

import com.studentos.feature.intelligence.domain.parser.JournalExtractionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalExtractionParserTest {

    @Test
    fun `parse valid clean JSON response extracts activities and commitments`() {
        val json = """
            {
              "summary": "Solved 3 dynamic programming problems and noted compiler lab deadline.",
              "activities": [
                {
                  "title": "Solved DP problems",
                  "activityType": "DSA",
                  "metricDelta": 3.0,
                  "durationMinutes": 45,
                  "matchedGoalId": 10,
                  "confidence": 0.95
                }
              ],
              "commitments": [
                {
                  "title": "Submit compiler lab",
                  "category": "ACADEMIC",
                  "targetDate": "2026-10-24",
                  "targetTime": "17:00",
                  "priority": "HIGH",
                  "estimatedMinutes": 60
                }
              ],
              "clarifications": []
            }
        """.trimIndent()

        val result = JournalExtractionParser.parse(json)
        assertTrue(result.isSuccess)

        val response = result.getOrThrow()
        assertEquals("Solved 3 dynamic programming problems and noted compiler lab deadline.", response.summary)
        assertEquals(1, response.activities.size)
        assertEquals("Solved DP problems", response.activities[0].title)
        assertEquals("DSA", response.activities[0].activityType)
        assertEquals(3.0, response.activities[0].metricDelta)
        assertEquals(45, response.activities[0].durationMinutes)
        assertEquals(10L, response.activities[0].matchedGoalId)

        assertEquals(1, response.commitments.size)
        assertEquals("Submit compiler lab", response.commitments[0].title)
        assertEquals("2026-10-24", response.commitments[0].targetDate)
        assertEquals("17:00", response.commitments[0].targetTime)
        assertEquals("HIGH", response.commitments[0].priority)
    }

    @Test
    fun `parse markdown wrapped JSON strips code fences properly`() {
        val markdown = """
            Here is your extracted structured summary:
            ```json
            {
              "summary": "Quick review",
              "activities": [
                {
                  "title": "Reviewed OS notes",
                  "activityType": "LEARNING",
                  "metricDelta": 1.0,
                  "durationMinutes": null,
                  "matchedGoalId": null
                }
              ],
              "commitments": []
            }
            ```
            Hope this helps!
        """.trimIndent()

        val result = JournalExtractionParser.parse(markdown)
        assertTrue(result.isSuccess)

        val response = result.getOrThrow()
        assertEquals("Quick review", response.summary)
        assertEquals(1, response.activities.size)
        assertEquals("Reviewed OS notes", response.activities[0].title)
        assertNull(response.activities[0].durationMinutes)
        assertNull(response.activities[0].matchedGoalId)
    }

    @Test
    fun `parse unestablished duration and count does not invent values`() {
        val json = """
            {
              "summary": "Studied algorithms without mentioning time or problem count.",
              "activities": [
                {
                  "title": "Studied graph algorithms",
                  "activityType": "LEARNING",
                  "metricDelta": null,
                  "durationMinutes": null,
                  "matchedGoalId": null
                }
              ],
              "commitments": [
                {
                  "title": "Review graph homework",
                  "category": "ACADEMIC",
                  "targetDate": null,
                  "targetTime": null,
                  "priority": "MEDIUM",
                  "estimatedMinutes": null
                }
              ],
              "clarifications": ["How many minutes did you study graph algorithms?"]
            }
        """.trimIndent()

        val result = JournalExtractionParser.parse(json)
        assertTrue(result.isSuccess)

        val response = result.getOrThrow()
        val activity = response.activities[0]
        assertNull("Duration must remain null when unestablished", activity.durationMinutes)
        assertNull("Metric delta must remain null when unestablished", activity.metricDelta)

        val commitment = response.commitments[0]
        assertNull("Target date must remain null when unestablished", commitment.targetDate)
        assertNull("Target time must remain null when unestablished", commitment.targetTime)
        assertNull("Estimated minutes must remain null when unestablished", commitment.estimatedMinutes)
        assertEquals(1, response.clarifications.size)
    }

    @Test
    fun `parse malformed JSON returns failure without throwing unhandled exception`() {
        val malformed = """
            ```json
            {
              "summary": "Broken json missing closing brace
              "activities": [
            ```
        """.trimIndent()

        val result = JournalExtractionParser.parse(malformed)
        assertTrue("Malformed JSON should return Result.failure", result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `parse blank or empty input returns failure`() {
        val result = JournalExtractionParser.parse("")
        assertTrue(result.isFailure)
    }
}
