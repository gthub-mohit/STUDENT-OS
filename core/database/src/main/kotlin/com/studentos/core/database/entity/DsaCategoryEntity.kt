package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DsaCategoryEntity — Top-level category groupings in the DSA Knowledge Tree (e.g. "Trees", "Graphs").
 *
 * Constraint: UNIQUE(name)
 */
@Entity(
    tableName = "dsa_categories",
    indices = [
        Index(value = ["name"], unique = true, name = "idx_categories_name")
    ]
)
data class DsaCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0
)
