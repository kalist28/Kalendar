package io.kalistratov.kalendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.kalistratov.kalendar.HorizontalKalendarConfig.INITIAL_PAGE
import io.kalistratov.kalendar.HorizontalKalendarConfig.TOTAL_PAGES
import io.kalistratov.kalendar.extension.copy
import io.kalistratov.kalendar.extension.dateNow
import io.kalistratov.kalendar.extension.monthLength
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalKalendar(
    startDate: LocalDate,
    modifier: Modifier = Modifier,
    monthHeader: (@Composable (Month) -> Unit)? = {
        Text(
            text = it.name.take(2).uppercase(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
    },
    dayOfWeekLabel: (@Composable (DayOfWeek) -> Unit)? = null,
    state: HorizontalKalendarState = rememberHorizontalKalendar(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    day: @Composable BoxScope.(KalendarDayState) -> Unit
) {
    val pagerState = state.pagerState

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) { monthOffset ->

        val pageState = remember(monthOffset) { state.calculateDayStates(startDate, monthOffset) }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth()
        ) {

            monthHeader?.let {
                item(span = { GridItemSpan(7) }) {
                    it.invoke(pageState.month)
                }
            }

            if (dayOfWeekLabel != null) DayOfWeek.entries.forEach { dayOfWeek ->
                item {
                    Box(Modifier.fillMaxWidth()) {
                        dayOfWeekLabel(dayOfWeek)
                    }
                }
            }

            pageState.days.forEach { dayState ->
                item {
                    Box(Modifier.fillMaxWidth()) {
                        day(dayState)
                    }
                }
            }
        }

    }
}

object HorizontalKalendarConfig {
    const val TOTAL_PAGES: Int = 100_000
    const val INITIAL_PAGE = TOTAL_PAGES / 2
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberHorizontalKalendar(
    pagerState: PagerState = rememberPagerState(INITIAL_PAGE) { TOTAL_PAGES }
) = remember { HorizontalKalendarState(pagerState) }

@Stable
@OptIn(ExperimentalFoundationApi::class)
class HorizontalKalendarState constructor(
    val pagerState: PagerState
) {
    private val cache = mutableMapOf<Pair<Month, Int>, KalendarPageData>()

    fun calculateDayStates(
        startDate: LocalDate,
        offset: Int
    ): KalendarPageData {
        val now = dateNow()
        val monthOffset = INITIAL_PAGE - offset
        val startMonthDay = startDate
            .minus(monthOffset, DateTimeUnit.MonthBased(1))
            .copy(day = 1)
        val days = buildList {
            cache[startMonthDay.run { month to year }]?.let { return it }

            addAll(calculatePreviousMonthDays(startMonthDay, now))
            val currentDays = calculateCurrentMonthDays(startMonthDay, now)
            addAll(currentDays)
            addAll(calculateNextMonthDays(currentDays.last().date, now))

            cache[startMonthDay.run { month to year }] =
                KalendarPageData(this, startMonthDay.month)
        }

        return KalendarPageData(days, startMonthDay.month)
    }

    private fun calculateCurrentMonthDays(
        date: LocalDate,
        now: LocalDate,
    ): List<KalendarDayState> {
        val dayCount = monthLength(date.month, date.year)
        return (0 until dayCount).map {
            val newDate = date.plus(it, DateTimeUnit.DAY)
            KalendarDayState(
                date = newDate,
                isCurrent = newDate == now
            )
        }
    }

    /**
     * @param date первый день страницы месяца перед которым нужно заполнить
     *     дни из предыдущего месяца
     */
    private fun calculatePreviousMonthDays(
        date: LocalDate,
        now: LocalDate,
    ): List<KalendarDayState> = (date.dayOfWeek.ordinal downTo 1).map {
        val newDate = date.minus(it, DateTimeUnit.DAY)
        KalendarDayState(
            date = newDate,
            isPreviousMonth = true,
            isCurrent = newDate == now
        )
    }


    /**
     * @param date последний день страницы месяца перед которым нужно заполнить
     *     дни из следующего месяца
     */
    private fun calculateNextMonthDays(
        date: LocalDate,
        now: LocalDate,
    ): List<KalendarDayState> {
        val lastDayIndex = date.dayOfWeek.isoDayNumber
        return (6 downTo lastDayIndex).map {
            val newDate = date.plus(7 - it, DateTimeUnit.DAY)
            KalendarDayState(
                date = newDate,
                isNextMonth = true,
                isCurrent = newDate == now
            )
        }
    }

}

@Stable
data class KalendarPageData(
    val days: List<KalendarDayState>,
    val month: Month
)

@Immutable
data class KalendarDayState(
    val date: LocalDate,
    val isCurrent: Boolean = false,
    val isPreviousMonth: Boolean = false,
    val isNextMonth: Boolean = false,
) {
    val isCurrentMonth = !isPreviousMonth && !isNextMonth
}