package com.smartcardscanner.presentation.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcardscanner.data.local.excel.ExcelExporter
import com.smartcardscanner.data.repository.ScanRepository
import com.smartcardscanner.domain.model.ScanRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecordsUiState(
    val records: List<ScanRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val excelExporter: ExcelExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            scanRepository.getAllRecords().collect { records ->
                _uiState.value = _uiState.value.copy(records = records)
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                scanRepository.getAllRecords().collect { records ->
                    _uiState.value = _uiState.value.copy(records = records)
                }
            } else {
                scanRepository.searchRecords(query).collect { records ->
                    _uiState.value = _uiState.value.copy(records = records)
                }
            }
        }
    }

    fun exportToExcel(onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val records = scanRepository.getAllRecordsList()
                val file = excelExporter.exportToExcel(records)
                _uiState.value = _uiState.value.copy(isExporting = false)
                launch(Dispatchers.Main) {
                    onComplete(file)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "خطأ في التصدير: ${e.message}"
                )
            }
        }
    }
}
