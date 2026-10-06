package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppTheme
import com.example.data.model.ReportSettings
import com.example.data.model.StampPosition
import com.example.data.model.StampSettings
import com.example.data.model.StampTextSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salim_settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val STAMP_SHOW_DATETIME = booleanPreferencesKey("stamp_show_datetime")
        val STAMP_USE_24H = booleanPreferencesKey("stamp_use_24h")
        val STAMP_SHOW_COORDS = booleanPreferencesKey("stamp_show_coords")
        val STAMP_SHOW_ADDRESS = booleanPreferencesKey("stamp_show_address")
        val STAMP_SHOW_PROJECT = booleanPreferencesKey("stamp_show_project")
        val STAMP_SHOW_NOTE = booleanPreferencesKey("stamp_show_note")
        val STAMP_POSITION = stringPreferencesKey("stamp_position")
        val STAMP_TEXT_SIZE = stringPreferencesKey("stamp_text_size")

        val REPORT_COMPANY = stringPreferencesKey("report_company")
        val REPORT_INSPECTOR = stringPreferencesKey("report_inspector")
        val REPORT_LICENSE = stringPreferencesKey("report_license")
        val REPORT_HEADER = stringPreferencesKey("report_header")

        val APP_THEME = stringPreferencesKey("app_theme")
        val ACTIVE_PROJECT_ID = longPreferencesKey("active_project_id")
    }

    val stampSettingsFlow: Flow<StampSettings> = context.dataStore.data.map { prefs ->
        StampSettings(
            showDateTime = prefs[PreferencesKeys.STAMP_SHOW_DATETIME] ?: true,
            use24Hour = prefs[PreferencesKeys.STAMP_USE_24H] ?: false,
            showCoordinates = prefs[PreferencesKeys.STAMP_SHOW_COORDS] ?: true,
            showAddress = prefs[PreferencesKeys.STAMP_SHOW_ADDRESS] ?: true,
            showProjectName = prefs[PreferencesKeys.STAMP_SHOW_PROJECT] ?: true,
            showNote = prefs[PreferencesKeys.STAMP_SHOW_NOTE] ?: true,
            position = try {
                StampPosition.valueOf(prefs[PreferencesKeys.STAMP_POSITION] ?: StampPosition.BOTTOM_LEFT.name)
            } catch (e: Exception) {
                StampPosition.BOTTOM_LEFT
            },
            textSize = try {
                StampTextSize.valueOf(prefs[PreferencesKeys.STAMP_TEXT_SIZE] ?: StampTextSize.MEDIUM.name)
            } catch (e: Exception) {
                StampTextSize.MEDIUM
            }
        )
    }

    val reportSettingsFlow: Flow<ReportSettings> = context.dataStore.data.map { prefs ->
        ReportSettings(
            companyName = prefs[PreferencesKeys.REPORT_COMPANY] ?: "",
            inspectorName = prefs[PreferencesKeys.REPORT_INSPECTOR] ?: "",
            inspectorLicense = prefs[PreferencesKeys.REPORT_LICENSE] ?: "",
            customHeader = prefs[PreferencesKeys.REPORT_HEADER] ?: ""
        )
    }

    val appThemeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        try {
            AppTheme.valueOf(prefs[PreferencesKeys.APP_THEME] ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    val activeProjectIdFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_PROJECT_ID]
    }

    suspend fun updateStampSettings(settings: StampSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.STAMP_SHOW_DATETIME] = settings.showDateTime
            prefs[PreferencesKeys.STAMP_USE_24H] = settings.use24Hour
            prefs[PreferencesKeys.STAMP_SHOW_COORDS] = settings.showCoordinates
            prefs[PreferencesKeys.STAMP_SHOW_ADDRESS] = settings.showAddress
            prefs[PreferencesKeys.STAMP_SHOW_PROJECT] = settings.showProjectName
            prefs[PreferencesKeys.STAMP_SHOW_NOTE] = settings.showNote
            prefs[PreferencesKeys.STAMP_POSITION] = settings.position.name
            prefs[PreferencesKeys.STAMP_TEXT_SIZE] = settings.textSize.name
        }
    }

    suspend fun updateReportSettings(settings: ReportSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REPORT_COMPANY] = settings.companyName
            prefs[PreferencesKeys.REPORT_INSPECTOR] = settings.inspectorName
            prefs[PreferencesKeys.REPORT_LICENSE] = settings.inspectorLicense
            prefs[PreferencesKeys.REPORT_HEADER] = settings.customHeader
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_THEME] = theme.name
        }
    }

    suspend fun setActiveProjectId(projectId: Long?) {
        context.dataStore.edit { prefs ->
            if (projectId != null) {
                prefs[PreferencesKeys.ACTIVE_PROJECT_ID] = projectId
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_PROJECT_ID)
            }
        }
    }
}
