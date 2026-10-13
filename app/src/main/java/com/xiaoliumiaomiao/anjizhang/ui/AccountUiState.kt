package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.runtime.saveable.listSaver
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.LocalDate
import java.util.UUID

data class AccountEditorState(val id: String, val name: String, val kind: AccountKind,
    val subtype: AccountSubtype, val archived: Boolean, val sortOrder: Int,
    val initialRequestId: String, val initialBalance: String, val date: String, val note: String) {
    fun toAccount() = Account(id, name, kind, subtype, archived, sortOrder)
    fun initialRequest(): BalanceAdjustmentRequest? {
        val target = BalanceMoney.parse(initialBalance) ?: return null
        val parsedDate = draftDate(date) ?: return null
        return BalanceAdjustmentRequest(initialRequestId, id, target, parsedDate, note, AdjustmentReason.INITIAL)
    }
    companion object {
        fun from(account: Account?, today: LocalDate) = AccountEditorState(account?.id ?: UUID.randomUUID().toString(),
            account?.name.orEmpty(), account?.kind ?: AccountKind.ASSET, account?.subtype ?: AccountSubtype.CASH,
            account?.archived ?: false, account?.sortOrder ?: 0, UUID.randomUUID().toString(), "", today.toString(), "")
    }
}
data class BalanceEditorState(val requestId: String, val accountId: String, val targetBalance: String,
    val date: String, val note: String) {
    fun request(): BalanceAdjustmentRequest? {
        val target = BalanceMoney.parse(targetBalance) ?: return null
        val parsedDate = draftDate(date) ?: return null
        return BalanceAdjustmentRequest(requestId, accountId, target, parsedDate, note)
    }
    companion object {
        fun create(accountId: String, today: LocalDate) = BalanceEditorState(UUID.randomUUID().toString(), accountId, "", today.toString(), "")
    }
}
data class TransferEditorState(val id: String, val fromAccountId: String, val toAccountId: String,
    val amount: String, val date: String, val note: String) {
    fun transfer(): AccountTransfer? {
        val cents = Money.parse(amount) ?: return null
        val parsedDate = draftDate(date) ?: return null
        return AccountTransfer(id, fromAccountId, toAccountId, cents, parsedDate, note)
    }
    companion object {
        fun from(transfer: AccountTransfer?, today: LocalDate) = TransferEditorState(transfer?.id ?: UUID.randomUUID().toString(),
            transfer?.fromAccountId.orEmpty(), transfer?.toAccountId.orEmpty(), transfer?.amountCents?.let(Money::plain).orEmpty(),
            transfer?.date?.toString() ?: today.toString(), transfer?.note.orEmpty())
    }
}
private fun draftDate(raw: String): LocalDate? =
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(raw)) null
    else runCatching { LocalDate.parse(raw).takeIf { it.year in 1..9999 } }.getOrNull()

internal fun AccountEditorState.restartForToday(permit: () -> Boolean, today: () -> LocalDate): AccountEditorState {
    if (!permit()) return this
    return copy(id = UUID.randomUUID().toString(), initialRequestId = UUID.randomUUID().toString(), date = today().toString())
}

internal fun BalanceEditorState.restartForToday(permit: () -> Boolean, today: () -> LocalDate): BalanceEditorState {
    if (!permit()) return this
    return copy(requestId = UUID.randomUUID().toString(), date = today().toString())
}

val AccountEditorStateSaver = listSaver<AccountEditorState, String>(
    save = { listOf(it.id, it.name, it.kind.name, it.subtype.name, it.archived.toString(), it.sortOrder.toString(),
        it.initialRequestId, it.initialBalance, it.date, it.note) },
    restore = { AccountEditorState(it[0], it[1], AccountKind.valueOf(it[2]), AccountSubtype.valueOf(it[3]),
        it[4].toBooleanStrict(), it[5].toInt(), it[6], it[7], it[8], it[9]) }
)
val BalanceEditorStateSaver = listSaver<BalanceEditorState, String>(
    save = { listOf(it.requestId, it.accountId, it.targetBalance, it.date, it.note) },
    restore = { BalanceEditorState(it[0], it[1], it[2], it[3], it[4]) }
)
val TransferEditorStateSaver = listSaver<TransferEditorState, String>(
    save = { listOf(it.id, it.fromAccountId, it.toAccountId, it.amount, it.date, it.note) },
    restore = { TransferEditorState(it[0], it[1], it[2], it[3], it[4], it[5]) }
)
