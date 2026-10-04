package com.xiaoliumiaomiao.anjizhang.domain

import java.util.UUID

object LedgerRules {
    fun validate(entry: Entry) {
        require(runCatching { UUID.fromString(entry.id).toString() == entry.id }.getOrDefault(false)) { "记录 ID 必须为规范 UUID" }
        require(entry.amountCents in 1..Money.MAX_CENTS) { "金额超出范围" }
        require(entry.category.trim().length in 1..20) { "分类须为 1 至 20 字符" }
        require(entry.note.length <= 200) { "备注最多 200 字符" }
        require(entry.date.year in 1900..9999) { "年份须为 1900 至 9999" }
    }
}
