package com.xiaoliumiaomiao.anjizhang.domain

import java.math.BigDecimal
import java.math.RoundingMode

object Money {
    const val MAX_CENTS: Long = 99999999999L
    private val decimal = Regex("[0-9]+(?:\\.[0-9]{1,2})?")

    // Adapted from MoneyBook String.toCents; see third_party/NOTICE.txt and MIT license.
    fun parse(text: String): Long? {
        val value = text.trim()
        if (!decimal.matches(value)) return null
        return runCatching {
            val bd = BigDecimal(value)
            if (bd.signum() <= 0) return null
            bd.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
                .takeIf { it <= MAX_CENTS }
        }.getOrNull()
    }

    fun format(cents: Long): String = "¥${plain(cents)}"
    fun plain(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
}
