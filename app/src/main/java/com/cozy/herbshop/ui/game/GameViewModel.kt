package com.cozy.herbshop.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.PourResult
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.game.LevelSolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val levelRepository: LevelRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val gameEngine: GameEngine = GameEngine(),
    private val solver: LevelSolver = LevelSolver()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun loadLevel(levelNumber: Int) {
        viewModelScope.launch {
            val level = levelRepository.getLevel(levelNumber)
            val initialJars = levelRepository.convertToJars(level)
            _uiState.value = GameUiState(
                levelNumber = levelNumber,
                jars = initialJars
            )
        }
    }

    fun onJarClicked(jarIndex: Int) {
        val currentState = _uiState.value
        if (currentState.isLevelCompleted) return

        val selectedIdx = currentState.selectedJarIndex

        if (selectedIdx == null) {
            if (!currentState.jars[jarIndex].isEmpty) {
                _uiState.update { it.copy(selectedJarIndex = jarIndex) }
            }
        } else if (selectedIdx == jarIndex) {
            _uiState.update { it.copy(selectedJarIndex = null) }
        } else {
            val src = currentState.jars[selectedIdx]
            val dst = currentState.jars[jarIndex]

            val pourResult = gameEngine.pour(src, dst)
            if (pourResult is PourResult.Success) {
                val newJars = gameEngine.executePour(
                    currentState.jars,
                    selectedIdx,
                    jarIndex,
                    pourResult.countPoured
                )

                val newHistory = currentState.history + listOf(currentState.jars)
                val isCompleted = gameEngine.checkLevelCompleted(newJars)

                _uiState.update {
                    it.copy(
                        jars = newJars,
                        selectedJarIndex = null,
                        movesCount = it.movesCount + 1,
                        history = newHistory,
                        isLevelCompleted = isCompleted,
                        hintMove = null
                    )
                }

                if (isCompleted) {
                    onLevelCompleted()
                }
            } else {
                if (!dst.isEmpty) {
                    _uiState.update { it.copy(selectedJarIndex = jarIndex) }
                } else {
                    _uiState.update { it.copy(selectedJarIndex = null) }
                }
            }
        }
    }

    fun undoMove() {
        val currentState = _uiState.value
        if (currentState.history.isEmpty() || currentState.isLevelCompleted) return

        val previousJars = currentState.history.last()
        val newHistory = currentState.history.dropLast(1)

        _uiState.update {
            it.copy(
                jars = previousJars,
                history = newHistory,
                selectedJarIndex = null,
                hintMove = null
            )
        }
    }

    fun requestHint() {
        val currentState = _uiState.value
        if (currentState.isLevelCompleted) return

        val solution = solver.solve(currentState.jars)
        if (!solution.isNullOrEmpty()) {
            _uiState.update { it.copy(hintMove = solution.first()) }
        }
    }

    fun addExtraJar() {
        val currentState = _uiState.value
        val newJarId = currentState.jars.size
        val capacity = currentState.jars.firstOrNull()?.capacity ?: 4
        val extraJar = Jar(id = newJarId, capacity = capacity, stack = emptyList())

        _uiState.update {
            it.copy(
                jars = it.jars + extraJar,
                selectedJarIndex = null
            )
        }
    }

    private fun onLevelCompleted() {
        viewModelScope.launch {
            preferencesRepository.incrementCompletedLevels()
            val currentUnlocked = preferencesRepository.unlockedHerbsFlow.first()

            val nextLockedHerb = Herb.entries.firstOrNull { it.id !in currentUnlocked }
            if (nextLockedHerb != null) {
                preferencesRepository.unlockHerb(nextLockedHerb.id)
                _uiState.update { it.copy(newlyUnlockedHerb = nextLockedHerb) }
            }

            val nextLevel = _uiState.value.levelNumber + 1
            preferencesRepository.updateCurrentLevel(nextLevel)
        }
    }
}
