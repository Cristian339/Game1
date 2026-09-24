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
            val initialJars = gameEngine.processSealedJars(levelRepository.convertToJars(level))
            val basicDone = preferencesRepository.tutorialBasicDoneFlow.first()
            val recipeDone = preferencesRepository.tutorialRecipeDoneFlow.first()
            _uiState.value = GameUiState(
                levelNumber = levelNumber,
                jars = initialJars,
                customerOrder = level.customerOrder,
                isDailyChallenge = level.isDailyChallenge,
                isGateLevel = LevelRepository.isGateLevel(levelNumber) && level.customerOrder != null,
                showBasicTutorial = levelNumber == 1 && !basicDone,
                showRecipeTutorial = levelNumber == 4 && !recipeDone && level.customerOrder != null
            )
        }
    }

    fun dismissTutorial() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.showBasicTutorial) {
                preferencesRepository.setTutorialBasicDone()
                _uiState.update { it.copy(showBasicTutorial = false) }
            }
            if (state.showRecipeTutorial) {
                preferencesRepository.setTutorialRecipeDone()
                _uiState.update { it.copy(showRecipeTutorial = false) }
            }
        }
    }

    fun loadDailyChallenge() {
        viewModelScope.launch {
            val level = levelRepository.getDailyChallengeLevel()
            val initialJars = gameEngine.processSealedJars(levelRepository.convertToJars(level))
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
            _uiState.value = buildZenState()
        }
    }

    suspend fun loadNextZenLevel() {
        _uiState.value = buildZenState()
    }

    private suspend fun buildZenState(): GameUiState {
        val level = levelRepository.getZenLevel()
        val initialJars = gameEngine.processSealedJars(levelRepository.convertToJars(level))
        return GameUiState(
            levelNumber = 0,
            jars = initialJars,
            customerOrder = level.customerOrder,
            isZenMode = true
        )
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
                    val requireRecipe = stateNow.isGateLevel
                    val isCompleted = gameEngine.checkLevelCompleted(
                        newJars,
                        stateNow.customerOrder,
                        requireRecipe
                    )
                    val fulfilled = gameEngine.isOrderFulfilled(newJars, stateNow.customerOrder)
                    val bonus = when {
                        stateNow.isGateLevel && fulfilled -> 50
                        !stateNow.isGateLevel && fulfilled && stateNow.customerOrder != null -> 15
                        else -> 0
                    }

                    // Fase 2: el líquido llega al destino y el relleno aparece
                    _uiState.update {
                        it.copy(
                            jars = newJars,
                            movesCount = it.movesCount + 1,
                            history = newHistory,
                            isLevelCompleted = isCompleted,
                            orderFulfilled = fulfilled,
                            bonusLeaves = bonus,
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

    init {
        viewModelScope.launch {
            preferencesRepository.soundEnabledFlow.collect { enabled ->
                soundManager?.soundEnabled = enabled
            }
        }
        viewModelScope.launch {
            preferencesRepository.undoTokensFlow.collect { tokens ->
                _uiState.update { it.copy(undoTokens = tokens) }
            }
        }
    }

    fun undoMove() {
        val currentState = _uiState.value
        if (currentState.history.isEmpty() || currentState.isLevelCompleted) return
        if (currentState.animatingPour != null) return

        if (currentState.undoTokens <= 0) {
            _uiState.update { it.copy(showBuyUndoDialog = true) }
            return
        }

        viewModelScope.launch {
            val success = preferencesRepository.consumeUndoToken()
            if (success) {
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
        }
    }

    fun buyUndoTokens() {
        viewModelScope.launch {
            val success = preferencesRepository.buyUndoTokens(3, 15)
            if (success) {
                _uiState.update { it.copy(showBuyUndoDialog = false) }
            }
        }
    }

    fun dismissBuyUndoDialog() {
        _uiState.update { it.copy(showBuyUndoDialog = false) }
    }

    fun requestHint() {
        val currentState = _uiState.value
        if (currentState.isLevelCompleted) return

        val order = if (currentState.isGateLevel) currentState.customerOrder?.requiredHerbs else null
        val solution = solver.solve(currentState.jars, order)
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
            val state = _uiState.value
            if (state.isZenMode) return@launch

            preferencesRepository.incrementCompletedLevels()
            soundManager?.playReward()

            if (state.isDailyChallenge) {
                preferencesRepository.addStamina(3)
            }

            // Bonus de hojas por receta cumplida (opcional o de puerta)
            if (state.bonusLeaves > 0) {
                preferencesRepository.addGoldLeaves(state.bonusLeaves)
            }

            // Hierbas: solo se desbloquean en niveles puerta con receta cumplida
            if (state.isGateLevel && state.orderFulfilled) {
                val currentUnlocked = preferencesRepository.unlockedHerbsFlow.first()
                val nextLockedHerb = Herb.entries.firstOrNull { it.id !in currentUnlocked }
                if (nextLockedHerb != null) {
                    preferencesRepository.unlockHerb(nextLockedHerb.id)
                    soundManager?.playUnlock()
                    _uiState.update { it.copy(newlyUnlockedHerb = nextLockedHerb) }
                }
            }

            if (!state.isDailyChallenge) {
                preferencesRepository.updateCurrentLevel(state.levelNumber + 1)
            }
        }
    }
}
