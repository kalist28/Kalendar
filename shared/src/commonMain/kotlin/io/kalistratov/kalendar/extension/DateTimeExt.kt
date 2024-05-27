package io.kalistratov.kalendar.extension

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

fun instantNow() = Clock.System.now()

fun dateNow() = dateTimeNow().date

fun dateTimeNow(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime = instantNow().toLocalDateTime(timeZone)

fun isLeapYear(year: Int): Boolean = try {
    LocalDate(year, 2, 29)
    true
} catch (exception: IllegalArgumentException) {
    false
}

fun monthLength(
    month: Month,
    year: Int,
): Int = when (month) {
    Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
    Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
    else -> 31
}

fun LocalDate.copy(
    year: Int = this.year,
    month: Month = this.month,
    day: Int = this.dayOfMonth,
): LocalDate = safetyCopy(year, month, day)

private fun safetyCopy(
    year: Int,
    month: Month,
    day: Int
): LocalDate = try {
    LocalDate(year, month, day)
} catch (e: IllegalArgumentException) {
    LocalDate(year, month, monthLength(month, year))
}