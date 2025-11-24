package com.ucw.beatu.business.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucw.beatu.business.settings.domain.model.PlaybackQualityPreference
import com.ucw.beatu.business.settings.domain.model.SettingsPreference
import com.ucw.beatu.business.settings.domain.usecase.SettingsUseCases
import com.ucw.beatu.shared.common.logger.AppLogger
import com.ucw.beatu.shared.common.time.Stopwatch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val useCases: SettingsUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val latencyStopwatches = mutableMapOf<SettingField, Stopwatch>()

    init {
        observeSettings()
    }

    fun toggleAiSearch(target: Boolean) {
        updateSetting(SettingField.AI_SEARCH) { useCases.updateAiSearch(target) }
    }

    fun toggleAiComment(target: Boolean) {
        updateSetting(SettingField.AI_COMMENT) { useCases.updateAiComment(target) }
    }

    fun toggleAutoPlay(target: Boolean) {
        updateSetting(SettingField.AUTO_PLAY) { useCases.updateAutoPlay(target) }
    }

    fun updateDefaultSpeed(speed: Float) {
        updateSetting(SettingField.SPEED) { useCases.updateDefaultSpeed(speed) }
    }

    fun updateDefaultQuality(quality: PlaybackQualityPreference) {
        updateSetting(SettingField.QUALITY) { useCases.updateDefaultQuality(quality) }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            useCases.observeSettings()
                .catch { throwable ->
                    AppLogger.e(TAG, "observeSettings failed", throwable)
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _events.emit(SettingsEvent.ShowMessage(throwable.message ?: "加载设置失败"))
                }
                .collect { preference ->
                    val previous = _uiState.value
                    _uiState.value = previous.copy(
                        isLoading = false,
                        aiSearchEnabled = preference.aiSearchEnabled,
                        aiCommentEnabled = preference.aiCommentEnabled,
                        autoPlayEnabled = preference.autoPlayEnabled,
                        defaultSpeed = preference.defaultSpeed,
                        defaultQuality = preference.defaultQuality,
                        errorMessage = null
                    )
                    trackLatency(previous, preference)
                }
        }
    }

    private fun updateSetting(field: SettingField, block: suspend () -> Unit) {
        markLatency(field)
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { throwable ->
                    AppLogger.e(TAG, "updateSetting $field failed", throwable)
                    latencyStopwatches.remove(field)
                    _events.emit(SettingsEvent.ShowMessage(throwable.message ?: "更新失败"))
                }
        }
    }

    private fun trackLatency(previous: SettingsUiState, preference: SettingsPreference) {
        val currentState = _uiState.value
        SettingField.entries.forEach { field ->
            val changed = when (field) {
                SettingField.AI_SEARCH -> previous.aiSearchEnabled != preference.aiSearchEnabled
                SettingField.AI_COMMENT -> previous.aiCommentEnabled != preference.aiCommentEnabled
                SettingField.AUTO_PLAY -> previous.autoPlayEnabled != preference.autoPlayEnabled
                SettingField.SPEED -> previous.defaultSpeed != preference.defaultSpeed
                SettingField.QUALITY -> previous.defaultQuality != preference.defaultQuality
            }
            if (changed) {
                val latency = latencyStopwatches.remove(field)?.elapsedMillis()
                if (latency != null) {
                    AppLogger.d(TAG, "Setting $field updated in ${latency}ms")
                    _uiState.value = currentState.copy(
                        lastLatencyField = field,
                        lastLatencyMs = latency
                    )
                }
            }
        }
    }

    private fun markLatency(field: SettingField) {
        latencyStopwatches[field] = Stopwatch().also { it.start() }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        val SPEED_OPTIONS = listOf(3.0f, 2.0f, 1.5f, 1.25f, 1.0f, 0.75f)
        val QUALITY_OPTIONS = PlaybackQualityPreference.entries
    }
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val aiSearchEnabled: Boolean = false,
    val aiCommentEnabled: Boolean = false,
    val autoPlayEnabled: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val defaultQuality: PlaybackQualityPreference = PlaybackQualityPreference.AUTO,
    val lastLatencyField: SettingField? = null,
    val lastLatencyMs: Long? = null,
    val errorMessage: String? = null
)

sealed interface SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent
}

enum class SettingField {
    AI_SEARCH,
    AI_COMMENT,
    AUTO_PLAY,
    SPEED,
    QUALITY
}


