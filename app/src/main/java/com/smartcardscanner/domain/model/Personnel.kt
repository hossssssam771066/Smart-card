package com.smartcardscanner.domain.model

data class Personnel(
    val id: Long = 0,
    val serialNumber: String = "",
    val ssn: String = "",
    val militaryNumber: String = "",
    val rank: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val mainUnit: String = "",
    val subUnit: String = "",
    val militaryCard: String = ""
)
