package com.smartcardscanner.presentation.database

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcardscanner.data.local.excel.ExcelImporter
import com.smartcardscanner.data.repository.PersonnelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val totalRecords: Int = 0,
    val isImporting: Boolean = false,
    val importProgress: Int = 0,
    val importTotal: Int = 0,
    val importedCount: Int = 0,
    val importSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val excelImporter: ExcelImporter,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    init {
        loadCurrentCount()
    }

    private fun loadCurrentCount() {
        viewModelScope.launch {
            val count = personnelRepository.getCount()
            _uiState.value = _uiState.value.copy(totalRecords = count)
        }
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                importSuccess = false,
                error = null,
                importProgress = 0,
                importTotal = 0
            )

            try {
                // Collect progress updates
                val progressJob = launch {
                    excelImporter.progress.collect { progress ->
                        _uiState.value = _uiState.value.copy(
                            importProgress = progress.current,
                            importTotal = progress.total
                        )
                    }
                }

                val count = excelImporter.importFromUri(uri) { batch ->
                    personnelRepository.insertBatch(batch)
                }

                progressJob.cancel()

                val totalCount = personnelRepository.getCount()

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importSuccess = true,
                    importedCount = count,
                    totalRecords = totalCount
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "خطأ في الاستيراد: ${e.message}"
                )
            }
        }
    }

    fun deleteDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            personnelRepository.deleteAll()
            _uiState.value = _uiState.value.copy(
                totalRecords = 0,
                importSuccess = false
            )
        }
    }
}
