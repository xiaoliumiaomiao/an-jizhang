package com.xiaoliumiaomiao.anjizhang.domain

import java.time.LocalDate
import java.math.BigInteger
import java.util.TreeMap

data class AccountTotals(val assetCents: Long, val debtCents: Long, val netCents: Long)

object AccountBalances {
    private val maxCents = BigInteger.valueOf(Long.MAX_VALUE)
    private val minCents = maxCents.negate()

    fun balances(snapshot: LedgerSnapshot, asOf: LocalDate): Map<String, Long> {
        LedgerRules.validateDate(asOf)
        return calculate(snapshot, asOf)
    }

    fun totals(snapshot: LedgerSnapshot, asOf: LocalDate): AccountTotals {
        val amounts = balances(snapshot, asOf).values.map(BigInteger::valueOf)
        val assets = amounts.filter { it.signum() > 0 }.fold(BigInteger.ZERO, BigInteger::add)
        val debt = amounts.filter { it.signum() < 0 }.fold(BigInteger.ZERO, BigInteger::add).negate()
        return AccountTotals(exactCents(assets), exactCents(debt), exactCents(assets - debt))
    }

    fun validateSnapshot(snapshot: LedgerSnapshot) {
        calculate(snapshot, null)
    }

    private fun calculate(snapshot: LedgerSnapshot, asOf: LocalDate?): Map<String, Long> {
        validateEntities(snapshot)
        val amounts = snapshot.accounts.associate { it.id to BigInteger.ZERO }.toMutableMap()
        val selected = amounts.toMutableMap()
        val events = TreeMap<LocalDate, MutableMap<String, BigInteger>>()
        fun add(date: LocalDate, accountId: String, cents: BigInteger) {
            val daily = events.getOrPut(date) { mutableMapOf() }
            daily[accountId] = (daily[accountId] ?: BigInteger.ZERO) + cents
        }
        snapshot.entries.forEach { entry ->
            entry.accountId?.let { accountId ->
                val cents = BigInteger.valueOf(entry.amountCents)
                add(entry.date, accountId, if (entry.kind == EntryKind.INCOME) cents else cents.negate())
            }
        }
        snapshot.transfers.forEach { transfer ->
            val cents = BigInteger.valueOf(transfer.amountCents)
            add(transfer.date, transfer.fromAccountId, cents.negate())
            add(transfer.date, transfer.toAccountId, cents)
        }
        snapshot.adjustments.forEach { add(it.date, it.accountId, BigInteger.valueOf(it.deltaCents)) }

        var assets = BigInteger.ZERO
        var debt = BigInteger.ZERO
        events.forEach { (date, daily) ->
            daily.forEach { (accountId, delta) ->
                val previous = amounts.getValue(accountId)
                val next = previous + delta
                amounts[accountId] = next
                assets = assets - previous.max(BigInteger.ZERO) + next.max(BigInteger.ZERO)
                debt = debt + previous.min(BigInteger.ZERO) - next.min(BigInteger.ZERO)
                if (asOf == null || date <= asOf) selected[accountId] = next
            }
            // A date is one state: validate after both transfer legs and all same-day events.
            daily.keys.forEach { exactCents(amounts.getValue(it)) }
            exactCents(assets)
            exactCents(debt)
            exactCents(assets - debt)
        }
        return selected.mapValues { exactCents(it.value) }
    }

    private fun validateEntities(snapshot: LedgerSnapshot) {
        snapshot.entries.forEach(LedgerRules::validate)
        snapshot.accounts.forEach(LedgerRules::validate)
        snapshot.transfers.forEach(LedgerRules::validate)
        snapshot.adjustments.forEach(LedgerRules::validate)
        snapshot.categories.forEach(LedgerRules::validate)
        uniqueIds(snapshot.entries.map { it.id })
        uniqueIds(snapshot.accounts.map { it.id })
        uniqueIds(snapshot.transfers.map { it.id })
        uniqueIds(snapshot.adjustments.map { it.id })
        uniqueIds(snapshot.categories.map { it.id })
        val categoryNames = snapshot.categories.map { it.kind to it.name.trim() }
        require(categoryNames.toSet().size == categoryNames.size) { "同类收支不能有重名分类" }
        val accountIds = snapshot.accounts.map { it.id }.toSet()
        snapshot.entries.forEach { require(it.accountId == null || it.accountId in accountIds) { "账目关联账户不存在" } }
        snapshot.transfers.forEach {
            require(it.fromAccountId in accountIds && it.toAccountId in accountIds) { "转账关联账户不存在" }
        }
        snapshot.adjustments.forEach { require(it.accountId in accountIds) { "余额调整关联账户不存在" } }
    }

    private fun uniqueIds(ids: List<String>) {
        require(ids.toSet().size == ids.size) { "同类记录存在重复 ID" }
    }

    private fun exactCents(cents: BigInteger): Long {
        require(cents >= minCents && cents <= maxCents) { "账户余额或汇总超出范围" }
        return cents.toLong()
    }
}
