package com.karinca.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karinca.app.data.local.CategoryKind
import java.time.Instant

@Entity(
    tableName = "categories",
    indices = [Index("userId"), Index("updatedAt")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String?, // null = system default, set = user's custom category
    val kind: CategoryKind,
    val name: String,
    val color: String, // hex, e.g. "#4C6EF5"
    val icon: String, // icon identifier resolved to a drawable/vector by the UI layer
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?, // tombstone — kept so past transactions still resolve
)
