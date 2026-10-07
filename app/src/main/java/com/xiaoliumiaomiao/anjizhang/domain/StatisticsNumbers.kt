package com.xiaoliumiaomiao.anjizhang.domain

import java.math.BigDecimal
import java.math.RoundingMode

object StatisticsNumbers {
    fun percentage(numerator: Long, denominator: Long): String? {
        if (denominator <= 0) return null
        return BigDecimal.valueOf(numerator).multiply(BigDecimal(100))
            .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
            .toPlainString() + "%"
    }

    fun averageCents(totalCents: Long, count: Int): Long? {
        require(count >= 0) { "记录笔数不能为负" }
        if (count == 0) return null
        return BigDecimal.valueOf(totalCents)
            .divide(BigDecimal.valueOf(count.toLong()), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}
