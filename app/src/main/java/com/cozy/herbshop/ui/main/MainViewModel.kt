package com.cozy.herbshop.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val currentLevel: Int = 1,
    val staminaCount: Int = UserPreferencesRepository.MAX_STAMINA,
    val unlockedHerbsCount: Int = 3,
    val adsRemoved: Boolean = false,
    val completedLevelsCount: Int = 0
)

class MainViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        preferencesRepository.currentLevelFlow,
        preferencesRepository.staminaCountFlow,
        preferencesRepository.unlockedHerbsFlow,
        preferencesRepository.adsRemovedFlow,
        preferencesRepository.completedLevelsCountFlow
    ) { currentLevel, stamina, unlocked, adsRemoved, completed ->
        MainUiState(
            currentLevel = currentLevel,
            staminaCount = stamina,
            unlockedHerbsCount = unlocked.size,
            adsRemoved = adsRemoved,
            completedLevelsCount = completed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    val decorations: StateFlow<Set<String>> = preferencesRepository.unlockedDecorationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    fun consumeStaminaForGame(onSuccess: () -> Unit, onNoStamina: () -> Unit) {
        viewModelScope.launch {
            val success = preferencesRepository.consumeStamina()
            if (success) {
                onSuccess()
            } else {
                onNoStamina()
            }
        }
    }

    fun addStaminaReward() {
        viewModelScope.launch {
            preferencesRepository.addStamina(3)
        }
    }
}
