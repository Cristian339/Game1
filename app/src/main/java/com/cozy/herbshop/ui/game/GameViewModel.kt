package com.cozy.herbshop.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozy.herbshop.data.audio.SoundManager
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.PourResult
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.game.LevelSolver
import kotlinx.coroutines.delay
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
    private val solver: LevelSolver = LevelSolver(),
    private val soundManager: SoundManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.soundEnabledFlow.collect { enabled ->
                soundManager?.soundEnabled = enabled
            }
        }
    }

    fun resetLevelCompleted() {
        _uiState.update { it.copy(isLevelCompleted = false, selectedJarIndex = null) }
    }

    fun loadLevel(levelNumber: Int) {
        viewModelScope.launch {
            val level = levelRepository.getLevel(levelNumber)
            val initialJars = levelRepository.convertToJars(level)
            _uiState.value = GameUiState(
                levelNumber = levelNumber,
                jars = initialJars,
                customerOrder = level.customerOrder,
                isDailyChallenge = level.isDailyChallenge
            )
        }
    }

    fun loadDailyChallenge() {
        viewModelScope.launch {
            val level = levelRepository.getDailyChallengeLevel()
            val initialJars = levelRepository.convertToJars(level)
            _uiState.value = GameUiState(
                levelNumber = 999,
                jars = initialJars,
                customerOrder = level.customerOrder,
                isDailyChallenge = true
            )
        }
    }

    fun loadZenLevel() {
        viewModelScope.launch {
            val level = levelRepository.getZenLevel()
            val initialJars = levelRepository.convertToJars(level)
            _uiState.value = GameUiState(
                levelNumber = 0,
                jars = initialJars,
                isZenMode = true
            )
        }
    }

    fun onJarClicked(jarIndex: Int) {
        val currentState = _uiState.value
        if (currentState.isLevelCompleted) return
        if (currentState.animatingPour != null) return

        val selectedIdx = currentState.selectedJarIndex

        if (selectedIdx == null) {
            if (!currentState.jars[jarIndex].isEmpty) {
                soundManager?.playJarClick()
                _uiState.update { it.copy(selectedJarIndex = jarIndex) }
            }
        } else if (selectedIdx == jarIndex) {
            _uiState.update { it.copy(selectedJarIndex = null) }
        } else {
            val src = currentState.jars[selectedIdx]
            val dst = currentState.jars[jarIndex]

            val pourResult = gameEngine.pour(src, dst)
            if (pourResult is PourResult.Success) {
                val herbToPour = src.topHerb ?: return
                val units = pourResult.countPoured.coerceIn(1, 4)

                _uiState.update {
                    it.copy(
                        selectedJarIndex = null,
                        animatingPour = PourAnimation(selectedIdx, jarIndex, herbToPour, pourResult.countPoured)
                    )
                }

                viewModelScope.launch {
                    // Varios "glugs" escalonados: uno por cada unidad de hierba que
                    // pasa del cuello del frasco origen al destino, con tono/velocidad
                    // ligeramente distintos para que suene natural.
                    val firstGlugAt = 130L
                    val glugSpacing = 140L
                    var lastGlugAt = 0L
                    for (i in 0 until units) {
                        val glugAt = firstGlugAt + i * glugSpacing
                        delay(glugAt - lastGlugAt)
                        lastGlugAt = glugAt
                        val rate = when (i) {
                            0 -> 1f
                            1 -> 1.07f
                            2 -> 0.93f
                            else -> 1.11f
                        }
                        soundManager?.playPourSound(volume = 0.9f - i * 0.05f, rate = rate)
                    }

                    // Fase 1→2: el chorro llega al destino y se aplica el vertido
                    // (nunca antes de 470ms ni antes de 100ms tras el último glug)
                    delay((470L - lastGlugAt).coerceAtLeast(100L))

                    val stateNow = _uiState.value
                    val newJars = gameEngine.executePour(
                        stateNow.jars,
                        selectedIdx,
                        jarIndex,
                        pourResult.countPoured
                    )

                    val newHistory = stateNow.history + listOf(stateNow.jars)
                    val isCompleted = gameEngine.checkLevelCompleted(newJars, stateNow.customerOrder)

                    // Fase 2: el líquido llega al destino y el relleno aparece
                    _uiState.update {
                        it.copy(
                            jars = newJars,
                            movesCount = it.movesCount + 1,
                            history = newHistory,
                            isLevelCompleted = isCompleted,
                            hintMove = null
                        )
                    }

                    // Fase 3: chorro desvanecido y retorno del frasco
                    delay(400)
                    _uiState.update { it.copy(animatingPour = null) }

                    if (isCompleted) {
                        soundManager?.playLevelCompleteSound()
                        onLevelCompleted()
                    }
                }
            } else {
                soundManager?.playInvalidPour()
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
        if (currentState.animatingPour != null) return

        soundManager?.playUndo()

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
            soundManager?.playHint()
            _uiState.update { it.copy(hintMove = solution.first()) }
        } else {
            soundManager?.playInvalidPour()
        }
    }

    fun addExtraJar() {
        val currentState = _uiState.value
        val newJarId = currentState.jars.size
        val capacity = currentState.jars.firstOrNull()?.capacity ?: 4
        val extraJar = Jar(id = newJarId, capacity = capacity, stack = emptyList())

        soundManager?.playExtraJar()

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
            soundManager?.playReward()

            if (_uiState.value.isDailyChallenge) {
                preferencesRepository.addStamina(3)
            }

            val currentUnlocked = preferencesRepository.unlockedHerbsFlow.first()
            val nextLockedHerb = Herb.entries.firstOrNull { it.id !in currentUnlocked }
            if (nextLockedHerb != null) {
                preferencesRepository.unlockHerb(nextLockedHerb.id)
                soundManager?.playUnlock()
                _uiState.update { it.copy(newlyUnlockedHerb = nextLockedHerb) }
            }

            val nextLevel = _uiState.value.levelNumber + 1
            preferencesRepository.updateCurrentLevel(nextLevel)
        }
    }
}
