package com.sortoapps.chorepilot.features.chores.domain

import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun proofReviewSectionTitle(
    createdAtEpochMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val date = Instant.fromEpochMilliseconds(createdAtEpochMs).toLocalDateTime(timeZone).date
    return "${date.month.shortDisplayName()} ${date.day}, ${date.year}"
}

fun proofReviewSubmittedLabel(
    createdAtEpochMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = Instant.fromEpochMilliseconds(createdAtEpochMs).toLocalDateTime(timeZone)
    val hour = local.hour.let { if (it % 12 == 0) 12 else it % 12 }
    val minute = local.minute.toString().padStart(2, '0')
    val suffix = if (local.hour >= 12) "PM" else "AM"
    return "Submitted ${local.date.month.shortDisplayName()} ${local.date.day} at $hour:$minute $suffix"
}

private fun Month.shortDisplayName(): String = when (this) {
    Month.JANUARY -> "Jan"
    Month.FEBRUARY -> "Feb"
    Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"
    Month.MAY -> "May"
    Month.JUNE -> "Jun"
    Month.JULY -> "Jul"
    Month.AUGUST -> "Aug"
    Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"
    Month.NOVEMBER -> "Nov"
    Month.DECEMBER -> "Dec"
}
