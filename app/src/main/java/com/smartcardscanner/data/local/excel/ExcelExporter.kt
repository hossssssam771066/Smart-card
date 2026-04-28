package com.smartcardscanner.data.local.excel

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.smartcardscanner.domain.model.ScanRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ExcelExporter"
    }

    suspend fun exportToExcel(records: List<ScanRecord>): File = withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("سجلات المسح")

        // Set RTL
        sheet.isRightToLeft = true

        // Header style
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.ROYAL_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER

            val font = workbook.createFont()
            font.bold = true
            font.color = IndexedColors.WHITE.index
            font.fontHeightInPoints = 12
            setFont(font)
        }

        // Data style
        val dataStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
            borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
            borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
            borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
        }

        // Headers
        val headers = listOf(
            "م",
            "التاريخ",
            "الاسم من الخلفية",
            "الاسم من NFC",
            "الاسم حسب القاعدة",
            "SSN",
            "الرقم الوطني",
            "الرقم العسكري",
            "الرتبة",
            "الوحدة الرئيسية",
            "الوحدة الفرعية",
            "البطاقة العسكرية",
            "نسبة التطابق",
            "نتيجة المطابقة",
            "حالة NFC",
            "حالة الخلفية",
            "NFC UID",
            "رقم الوثيقة",
            "تاريخ الميلاد",
            "الجنس",
            "الجنسية",
            "ملاحظات"
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        // Data rows
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

        records.forEachIndexed { index, record ->
            val row = sheet.createRow(index + 1)

            val values = listOf(
                (index + 1).toString(),
                dateFormat.format(Date(record.scanDate)),
                record.nameFromBack,
                record.nameFromNfc,
                record.nameFromDatabase,
                record.ssn,
                record.nationalId,
                record.militaryNumber,
                record.rank,
                record.mainUnit,
                record.subUnit,
                record.militaryCard,
                "${(record.matchScore * 100).toInt()}%",
                record.matchType,
                record.nfcStatus,
                record.backScanStatus,
                record.nfcUid,
                record.documentNumber,
                record.dateOfBirth,
                record.gender,
                record.nationality,
                record.notes
            )

            values.forEachIndexed { colIndex, value ->
                val cell = row.createCell(colIndex)
                cell.setCellValue(value)
                cell.cellStyle = dataStyle
            }
        }

        // Auto-size columns
        headers.indices.forEach { sheet.setColumnWidth(it, 5000) }

        // Save file
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, "scan_records_$timestamp.xlsx")
        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

        Log.d(TAG, "Exported ${records.size} records to ${file.absolutePath}")
        return@withContext file
    }
}
