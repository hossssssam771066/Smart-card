package com.smartcardscanner.data.local.excel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.smartcardscanner.domain.model.Personnel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelImporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ExcelImporter"
        const val BATCH_SIZE = 500
    }

    data class ImportProgress(
        val current: Int = 0,
        val total: Int = 0,
        val isComplete: Boolean = false,
        val error: String? = null
    )

    private val _progress = MutableStateFlow(ImportProgress())
    val progress: StateFlow<ImportProgress> = _progress

    /**
     * Import Excel file with columns:
     * م | SSN | الرقم العسكري | الرتبة | الاسم | الوحدة الرئيسية | الوحدة الفرعية | البطاقة العسكرية
     */
    suspend fun importFromUri(
        uri: Uri,
        onBatch: suspend (List<Personnel>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var totalImported = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("لا يمكن فتح الملف")

            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            val totalRows = sheet.lastRowNum
            _progress.value = ImportProgress(current = 0, total = totalRows)

            // Detect column indices from header row
            val headerRow = sheet.getRow(0)
            val columnMap = detectColumns(headerRow)

            Log.d(TAG, "Detected columns: $columnMap")

            val batch = mutableListOf<Personnel>()
            var rowIndex = 0

            for (row in sheet) {
                rowIndex++
                if (rowIndex == 1) continue // Skip header

                try {
                    val personnel = parseRow(row, columnMap) ?: continue
                    batch.add(personnel)

                    if (batch.size >= BATCH_SIZE) {
                        onBatch(batch.toList())
                        totalImported += batch.size
                        batch.clear()
                        _progress.value = ImportProgress(current = totalImported, total = totalRows)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing row $rowIndex", e)
                }
            }

            // Insert remaining batch
            if (batch.isNotEmpty()) {
                onBatch(batch.toList())
                totalImported += batch.size
            }

            workbook.close()
            inputStream.close()

            _progress.value = ImportProgress(
                current = totalImported,
                total = totalRows,
                isComplete = true
            )

        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            _progress.value = ImportProgress(error = e.message ?: "خطأ في الاستيراد")
            throw e
        }

        return@withContext totalImported
    }

    private fun detectColumns(headerRow: Row?): Map<String, Int> {
        val columnMap = mutableMapOf<String, Int>()
        if (headerRow == null) {
            // Default column order
            columnMap["serial"] = 0
            columnMap["ssn"] = 1
            columnMap["military_number"] = 2
            columnMap["rank"] = 3
            columnMap["name"] = 4
            columnMap["main_unit"] = 5
            columnMap["sub_unit"] = 6
            columnMap["military_card"] = 7
            return columnMap
        }

        for (cell in headerRow) {
            val value = getCellStringValue(cell).trim()
            val index = cell.columnIndex

            when {
                value == "م" || value.contains("مسلسل") || value.contains("serial", ignoreCase = true) ->
                    columnMap["serial"] = index
                value.contains("SSN", ignoreCase = true) || value.contains("ssn") ->
                    columnMap["ssn"] = index
                value.contains("عسكري") && value.contains("رقم") ->
                    columnMap["military_number"] = index
                value.contains("رتب") || value.contains("rank", ignoreCase = true) ->
                    columnMap["rank"] = index
                value.contains("اسم") || value.contains("الاسم") || value.contains("name", ignoreCase = true) ->
                    columnMap["name"] = index
                value.contains("رئيسي") || value.contains("وحد") && value.contains("رئ") ->
                    columnMap["main_unit"] = index
                value.contains("فرعي") || value.contains("وحد") && value.contains("فر") ->
                    columnMap["sub_unit"] = index
                value.contains("بطاق") && value.contains("عسكري") ->
                    columnMap["military_card"] = index
                value.contains("وحد") && !columnMap.containsKey("main_unit") ->
                    columnMap["main_unit"] = index
            }
        }

        // Fallback: if name column not found, use default positions
        if (!columnMap.containsKey("name")) {
            columnMap["serial"] = 0
            columnMap["ssn"] = 1
            columnMap["military_number"] = 2
            columnMap["rank"] = 3
            columnMap["name"] = 4
            columnMap["main_unit"] = 5
            columnMap["sub_unit"] = 6
            columnMap["military_card"] = 7
        }

        return columnMap
    }

    private fun parseRow(row: Row, columnMap: Map<String, Int>): Personnel? {
        val name = getColumnValue(row, columnMap["name"])
        if (name.isBlank()) return null

        return Personnel(
            serialNumber = getColumnValue(row, columnMap["serial"]),
            ssn = getColumnValue(row, columnMap["ssn"]),
            militaryNumber = getColumnValue(row, columnMap["military_number"]),
            rank = getColumnValue(row, columnMap["rank"]),
            name = name,
            mainUnit = getColumnValue(row, columnMap["main_unit"]),
            subUnit = getColumnValue(row, columnMap["sub_unit"]),
            militaryCard = getColumnValue(row, columnMap["military_card"])
        )
    }

    private fun getColumnValue(row: Row, columnIndex: Int?): String {
        if (columnIndex == null) return ""
        val cell = row.getCell(columnIndex) ?: return ""
        return getCellStringValue(cell)
    }

    private fun getCellStringValue(cell: org.apache.poi.ss.usermodel.Cell): String {
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue?.trim() ?: ""
            CellType.NUMERIC -> {
                val num = cell.numericCellValue
                if (num == num.toLong().toDouble()) {
                    num.toLong().toString()
                } else {
                    num.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue?.trim() ?: ""
                } catch (e: Exception) {
                    try {
                        cell.numericCellValue.toLong().toString()
                    } catch (e2: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
    }
}
