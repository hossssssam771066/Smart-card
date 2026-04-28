package com.smartcardscanner.presentation.home

import androidx.lifecycle.ViewModel
import com.smartcardscanner.data.repository.PersonnelRepository
import com.smartcardscanner.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    personnelRepository: PersonnelRepository,
    scanRepository: ScanRepository
) : ViewModel() {

    val personnelCount: Flow<Int> = personnelRepository.getCountFlow()
    val scanCount: Flow<Int> = scanRepository.getCount()
}
