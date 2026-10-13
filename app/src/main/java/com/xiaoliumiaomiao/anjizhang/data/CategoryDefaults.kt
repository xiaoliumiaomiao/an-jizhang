package com.xiaoliumiaomiao.anjizhang.data

import com.xiaoliumiaomiao.anjizhang.domain.Category
import com.xiaoliumiaomiao.anjizhang.domain.Entry
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind
import java.util.UUID

/** The namespace is stable across installations; historical entry text is never rewritten. */
internal object CategoryDefaults {
    private const val NAMESPACE = "an-jizhang:category:v1:"
    private fun stableId(kind: EntryKind, name: String) =
        UUID.nameUUIDFromBytes("$NAMESPACE${kind.name}:$name".toByteArray(Charsets.UTF_8)).toString()

    val canonical: List<Category> = listOf(
        EntryKind.EXPENSE to listOf("餐饮", "交通", "购物", "居家", "娱乐", "医疗", "教育", "其他支出"),
        EntryKind.INCOME to listOf("工资", "奖金", "理财", "其他收入"),
    ).flatMap { (kind, names) -> names.mapIndexed { order, name -> Category(stableId(kind, name), kind, name, order) } }

    fun supplement(existing: List<Category>, entries: List<Entry>): List<Category> {
        val result = existing.toMutableList()
        val names = existing.map { it.kind to it.name.trim() }.toMutableSet()
        val ids = existing.map { it.id }.toMutableSet()
        val nextOrder = existing.groupBy { it.kind }.mapValues { (_, cats) ->
            cats.maxOf { it.sortOrder }.toLong() + 1
        }.toMutableMap()
        entries.forEach { entry ->
            val name = entry.category.trim()
            if (names.add(entry.kind to name)) {
                var id = stableId(entry.kind, name)
                if (id in ids) do { id = UUID.randomUUID().toString() } while (id in ids)
                ids.add(id)
                val order = nextOrder[entry.kind] ?: 0
                // Tied orders are legal. Keep the author's maximum order without renumbering it.
                val storedOrder = order.coerceAtMost(Int.MAX_VALUE.toLong())
                result.add(Category(id, entry.kind, name, storedOrder.toInt()))
                nextOrder[entry.kind] = storedOrder + 1
            }
        }
        return result
    }
}
