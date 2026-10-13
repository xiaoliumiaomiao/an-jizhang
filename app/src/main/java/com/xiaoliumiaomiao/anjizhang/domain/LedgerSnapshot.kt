package com.xiaoliumiaomiao.anjizhang.domain

data class LedgerSnapshot(
    val entries: List<Entry> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val transfers: List<AccountTransfer> = emptyList(),
    val adjustments: List<AccountAdjustment> = emptyList(),
    val categories: List<Category> = emptyList(),
)
