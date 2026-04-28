package com.smartcardscanner.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "scan_date")
    val scanDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "name_from_back")
    val nameFromBack: String = "",

    @ColumnInfo(name = "name_from_nfc")
    val nameFromNfc: String = "",

    @ColumnInfo(name = "name_from_database")
    val nameFromDatabase: String = "",

    @ColumnInfo(name = "ssn")
    val ssn: String = "",

    @ColumnInfo(name = "national_id")
    val nationalId: String = "",

    @ColumnInfo(name = "military_number")
    val militaryNumber: String = "",

    @ColumnInfo(name = "rank")
    val rank: String = "",

    @ColumnInfo(name = "main_unit")
    val mainUnit: String = "",

    @ColumnInfo(name = "sub_unit")
    val subUnit: String = "",

    @ColumnInfo(name = "military_card")
    val militaryCard: String = "",

    @ColumnInfo(name = "match_score")
    val matchScore: Double = 0.0,

    @ColumnInfo(name = "match_type")
    val matchType: String = "",

    @ColumnInfo(name = "nfc_status")
    val nfcStatus: String = "",

    @ColumnInfo(name = "back_scan_status")
    val backScanStatus: String = "",

    @ColumnInfo(name = "nfc_uid")
    val nfcUid: String = "",

    @ColumnInfo(name = "document_number")
    val documentNumber: String = "",

    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: String = "",

    @ColumnInfo(name = "gender")
    val gender: String = "",

    @ColumnInfo(name = "nationality")
    val nationality: String = "",

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "face_photo_path")
    val facePhotoPath: String = ""
)
