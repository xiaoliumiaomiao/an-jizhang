package com.xiaoliumiaomiao.anjizhang.domain

import java.time.LocalDate
import java.time.YearMonth

data class StatisticsPeriod(val year: Int, val month: Int? = null) {
    init {
        require(year in 1900..9999) { "年份须为 1900 至 9999" }
        require(month == null || month in 1..12) { "月份须为 1 至 12" }
    }

    val startDate: LocalDate = LocalDate.of(year, month ?: 1, 1)
    val endDate: LocalDate = YearMonth.of(year, month ?: 12).atEndOfMonth()

    fun contains(date: LocalDate): Boolean = date >= startDate && date <= endDate

    companion object {
        fun monthly(month: YearMonth) = StatisticsPeriod(month.year, month.monthValue)
        fun annual(year: Int) = StatisticsPeriod(year)
    }
}
