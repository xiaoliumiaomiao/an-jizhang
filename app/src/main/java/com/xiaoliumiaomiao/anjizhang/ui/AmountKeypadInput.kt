package com.xiaoliumiaomiao.anjizhang.ui

object AmountKeypadInput {
    fun append(amount: String, key: Char): String {
        if (key == '.') return if ('.' in amount) amount else amount.ifEmpty { "0" } + "."
        if (key !in '0'..'9') return amount
        val decimal = amount.indexOf('.')
        if (decimal >= 0) return if (amount.length - decimal - 1 < 2) amount + key else amount
        if (amount == "0") return key.toString()
        return if (amount.length < 9) amount + key else amount
    }
    fun backspace(amount: String): String = amount.dropLast(1)
}
