package com.xiaoliumiaomiao.anjizhang.domain

import java.math.BigDecimal
import java.math.RoundingMode

object BalanceMoney {
    private val decimal = Regex("-?[0-9]+(?:\\.[0-9]{1,2})?")

    fun parse(raw: String): Long? {
        val value = raw.trim()
        if (!decimal.matches(value)) return null
        return runCatching {
            BigDecimal(value).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact().takeIf { it != Long.MIN_VALUE }
        }.getOrNull()
    }
}
