package com.xiaoliumiaomiao.anjizhang.domain

import java.time.LocalDate

enum class AccountKind { ASSET, LIABILITY }
enum class AccountSubtype { CASH, BANK, FUND, CREDIT_CARD, LOAN, OTHER }
enum class AdjustmentReason { INITIAL, CALIBRATION }

data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val subtype: AccountSubtype,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

data class AccountTransfer(
    val id: String,
    val fromAccountId: String,
    val toAccountId: String,
    val amountCents: Long,
    val date: LocalDate,
    val note: String = "",
)

data class AccountAdjustment(
    val id: String,
    val accountId: String,
    val deltaCents: Long,
    val date: LocalDate,
    val targetBalanceCents: Long,
    val note: String = "",
    val reason: AdjustmentReason = AdjustmentReason.CALIBRATION,
)

data class BalanceAdjustmentRequest(
    val id: String,
    val accountId: String,
    val targetBalanceCents: Long,
    val date: LocalDate,
    val note: String = "",
    val reason: AdjustmentReason = AdjustmentReason.CALIBRATION,
)
