package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.billing.BillingManager
import com.example.data.model.AppTheme
import com.example.data.model.ReportSettings
import com.example.data.model.StampPosition
import com.example.data.model.StampSettings
import com.example.data.model.StampTextSize
import com.example.data.preferences.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    val billingManager: BillingManager
) : ViewModel() {

    val stampSettings: StateFlow<StampSettings> = settingsDataStore.stampSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StampSettings())

    val reportSettings: StateFlow<ReportSettings> = settingsDataStore.reportSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportSettings())

    val appTheme: StateFlow<AppTheme> = settingsDataStore.appThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    fun updateStampSettings(updated: StampSettings) {
        viewModelScope.launch {
            settingsDataStore.updateStampSettings(updated)
        }
    }

    fun updateReportSettings(updated: ReportSettings) {
        viewModelScope.launch {
            settingsDataStore.updateReportSettings(updated)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsDataStore.setAppTheme(theme)
        }
    }

    fun openPaywall() {
        _showPaywall.value = true
    }

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    class Factory(private val app: SalimApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(app.settingsDataStore, app.billingManager) as T
        }
    }
}
