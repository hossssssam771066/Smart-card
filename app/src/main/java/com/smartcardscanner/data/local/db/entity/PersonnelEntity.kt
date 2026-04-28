package com.smartcardscanner.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personnel",
    indices = [
        Index(value = ["ssn"]),
        Index(value = ["military_number"]),
        Index(value = ["normalized_name"]),
        Index(value = ["name"])
    ]
)
data class PersonnelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "serial_number")
    val serialNumber: String = "",

    @ColumnInfo(name = "ssn")
    val ssn: String = "",

    @ColumnInfo(name = "military_number")
    val militaryNumber: String = "",

    @ColumnInfo(name = "rank")
    val rank: String = "",

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "normalized_name")
    val normalizedName: String = "",

    @ColumnInfo(name = "name_tokens")
    val nameTokens: String = "",

    @ColumnInfo(name = "main_unit")
    val mainUnit: String = "",

    @ColumnInfo(name = "sub_unit")
    val subUnit: String = "",

    @ColumnInfo(name = "military_card")
    val militaryCard: String = ""
)
