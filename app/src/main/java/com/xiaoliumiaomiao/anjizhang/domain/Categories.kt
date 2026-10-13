package com.xiaoliumiaomiao.anjizhang.domain

data class Category(
    val id: String,
    val kind: EntryKind,
    val name: String,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)
