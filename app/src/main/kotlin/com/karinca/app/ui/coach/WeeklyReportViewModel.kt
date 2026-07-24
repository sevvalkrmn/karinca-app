package com.karinca.app.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karinca.app.data.local.entity.WeeklyReportEntity
import com.karinca.app.data.repository.AuthRepository
import com.karinca.app.data.repository.WeeklyReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val weeklyReportRepository: WeeklyReportRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserIdOrNull()

    val latestReport: StateFlow<WeeklyReportEntity?> =
        (userId?.let(weeklyReportRepository::observeLatest) ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Opening the report screen directly (via the coach FAB, without ever seeing the
        // envelope reveal) should still suppress a future envelope pop for this report.
        viewModelScope.launch {
            latestReport.collect { report ->
                report?.let { weeklyReportRepository.markSeen(it.id) }
            }
        }
    }
}
