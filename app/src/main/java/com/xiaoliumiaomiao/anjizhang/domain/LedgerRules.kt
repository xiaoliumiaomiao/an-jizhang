package com.xiaoliumiaomiao.anjizhang.domain

import java.util.UUID
import java.time.LocalDate

object LedgerRules {
    fun validate(entry: Entry) {
        validateId(entry.id)
        require(entry.amountCents in 1..Money.MAX_CENTS) { "金额超出范围" }
        require(entry.category.trim().length in 1..20) { "分类须为 1 至 20 字符" }
        validateNoteAndDate(entry.note, entry.date)
        entry.accountId?.let(::validateId)
    }

    fun validate(account: Account) {
        validateId(account.id)
        require(account.name.trim().length in 1..30) { "账户名须为 1 至 30 字符" }
        require(account.sortOrder >= 0) { "排序不能为负数" }
    }

    fun validate(transfer: AccountTransfer) {
        validateId(transfer.id)
        validateId(transfer.fromAccountId)
        validateId(transfer.toAccountId)
        require(transfer.fromAccountId != transfer.toAccountId) { "转出与转入账户不能相同" }
        require(transfer.amountCents in 1..Money.MAX_CENTS) { "转账金额超出范围" }
        validateNoteAndDate(transfer.note, transfer.date)
    }

    fun validate(adjustment: AccountAdjustment) {
        validateId(adjustment.id)
        validateId(adjustment.accountId)
        validateBalance(adjustment.deltaCents)
        validateBalance(adjustment.targetBalanceCents)
        validateNoteAndDate(adjustment.note, adjustment.date)
    }

    fun validate(request: BalanceAdjustmentRequest) {
        validateId(request.id)
        validateId(request.accountId)
        validateBalance(request.targetBalanceCents)
        validateNoteAndDate(request.note, request.date)
    }

    fun validate(category: Category) {
        validateId(category.id)
        require(category.name.trim().length in 1..20) { "分类须为 1 至 20 字符" }
        require(category.sortOrder >= 0) { "排序不能为负数" }
    }

    internal fun validateDate(date: LocalDate) {
        require(date.year in 1900..9999) { "年份须为 1900 至 9999" }
    }

    private fun validateId(id: String) {
        require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) { "记录 ID 必须为规范 UUID" }
    }

    private fun validateBalance(cents: Long) {
        require(cents != Long.MIN_VALUE) { "余额或调整金额超出范围" }
    }

    private fun validateNoteAndDate(note: String, date: LocalDate) {
        require(note.length <= 200) { "备注最多 200 字符" }
        validateDate(date)
    }
}
