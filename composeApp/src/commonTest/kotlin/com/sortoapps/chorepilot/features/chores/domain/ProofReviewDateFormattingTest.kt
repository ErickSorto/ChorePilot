package com.sortoapps.chorepilot.features.chores.domain

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class ProofReviewDateFormattingTest {

    @Test
    fun sectionTitleUsesExactCalendarDate() {
        val title = proofReviewSectionTitle(
            createdAtEpochMs = 1775424180000L,
            timeZone = TimeZone.of("America/New_York"),
        )

        assertEquals("Apr 5, 2026", title)
    }

    @Test
    fun submittedLabelUsesExactCalendarDateAndClockTime() {
        val label = proofReviewSubmittedLabel(
            createdAtEpochMs = 1775424180000L,
            timeZone = TimeZone.of("America/New_York"),
        )

        assertEquals("Submitted Apr 5 at 5:23 PM", label)
    }
}
