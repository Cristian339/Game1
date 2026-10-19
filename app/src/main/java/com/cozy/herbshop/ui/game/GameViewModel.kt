package com.cozy.herbshop.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozy.herbshop.data.audio.SoundManager
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.HelpItem
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JournalLetters
import com.cozy.herbshop.data.model.PourResult
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.game.HerbUnlocks
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.game.LevelSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val levelRepository: LevelRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val gameEngine: GameEngine = GameEngine(),
    private val solver: LevelSolver = LevelSolver(),
    private val soundManager: SoundManager? = null
) : ViewModel() {

    /**
     * Si el jugador tiene el pack ASMR Cristal Pro, cada selección de frasco
     * añade un timbre cristalino. Lo activa MainActivity según sus packs.
     */
    var crystalSelectionEnabled: Boolean = false

    private companion object {
        /** Cuántos toques se pueden encolar durante una animación de vertido. */
        const val MAX_PENDING_CLICKS = 3
    }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    /** Pulsaciones encoladas durante una animación de vertido. */
    private val pendingClicks = ArrayDeque<Int>()

    /**
     * Semilla del reto diario en juego. Se apunta al cargarlo, no al ganarlo:
     * una partida que cruce la medianoche se cierra con el dia con el que se
     * jugo, no con el que se acabo.
     */
    private var dailyChallengeSeed: Int? = null

    private fun clearPendingClicks() {
        pendingClicks.clear()
        _uiState.update { if (it.pendingActions.isEmpty()) it else it.copy(pendingActions = emptyList()) }
    }

    private fun drainPendingClicks() {
        while (pendingClicks.isNotEmpty() && _uiState.value.animatingPour == null) {
            val next = pendingClicks.removeFirst()
            _uiState.update { it.copy(pendingActions = pendingClicks.toList()) }
            onJarClicked(next)
        }
    }

    init {
        viewModelScope.launch {
            preferencesRepository.soundEnabledFlow.collect { enabled ->
                soundManager?.soundEnabled = enabled
            }
        }
    }

    fun resetLevelCompleted() {
        // isLevelCompleted se apaga para que la UI deje de observar la victoria,
        // pero la hierba nueva y los premios se conservan: LevelCompleteScreen
        // los necesita todavia. La bandera completionHandled es la que se reinicia
        // para que un replay del mismo nivel no reutilice la decision anterior.
        _uiState.update {
            it.copy(
                isLevelCompleted = false,
                selectedJarIndex = null,
                completionHandled = false,
                // Las chapas son por intento: al volver a empezar el nivel se
                // cuentan de nuevo. Si no, con jugar, fallar y reiniciar dos
                // veces no se podria sacar nunca la de "sin Pista".
                usedHint = false,
                usedUndo = false,
                hintMove = null
            )
        }
    }

fun loadLevel(levelNumber: Int) {
        clearPendingClicks()
        viewModelScope.launch {
            // Generar el nivel es una busqueda en memoria: puede tardar y reservar
            // bastante. Hecho en el hilo principal congela la pantalla y en los
            // niveles altos tumbaba la partida, asi que va en su propio hilo.
            val level = withContext(Dispatchers.Default) {
                levelRepository.getLevel(levelNumber)
            }
            val initialJars = gameEngine.processSealedJars(levelRepository.convertToJars(level))
            val basicDone = preferencesRepository.tutorialBasicDoneFlow.first()
            val recipeDone = preferencesRepository.tutorialRecipeDoneFlow.first()
            val preview = previewReward(rewardIdFor(level.levelNumber, level.customerOrder?.id))
            _uiState.value = GameUiState(
                levelNumber = levelNumber,
                jars = initialJars,
                customerOrder = level.customerOrder,
                isDailyChallenge = level.isDailyChallenge,
                isGateLevel = LevelRepository.isGateLevel(levelNumber) && level.customerOrder != null,
                parMoves = level.parMoves,
                sectorRule = level.sectorRule,
                showBasicTutorial = levelNumber == 1 && !basicDone,
                showRecipeTutorial = LevelRepository.isGateLevel(levelNumber) && !recipeDone && level.customerOrder != null,
                undoTokens = preferencesRepository.undoTokensFlow.first(),
                helpStocks = currentHelpStocks(),
                nextRecipeHerb = gameEngine.nextRecipeHerb(
                    initialJars.firstOrNull { jar -> jar.isTeapot },
                    level.customerOrder
                ),
                recipeRewardNext = preview.first,
                baseRewardNext = preview.second
            )
        }
    }

    /**
     * Id con el que se lleva la cuenta de repeticiones. El reto del día usa el id
     * de su pedido (incluye la fecha) para poder premiarlo completo cada día.
     */
    private fun rewardIdFor(levelNumber: Int, orderId: String?): String =
        orderId ?: "level_$levelNumber"

    /** Premio previsto si se completa ahora: (receta, base). */
    private suspend fun previewReward(rewardId: String): Pair<Int, Int> {
        val times = preferencesRepository.rewardTimes(rewardId)
        val first = UserPreferencesRepository.computeReward(times, recipeFulfilled = true)
        return first.recipe to first.base
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
        clearPendingClicks()
        viewModelScope.launch {
            val seed = LevelRepository.dailySeed()
            dailyChallengeSeed = seed
            val level = levelRepository.getDailyChallengeLevel(seed)
            val initialJars = gameEngine.processSealedJars(levelRepository.convertToJars(level))
            val preview = previewReward(rewardIdFor(level.levelNumber, level.customerOrder?.id))
            _uiState.value = GameUiState(
                levelNumber = 999,
                jars = initialJars,
                customerOrder = level.customerOrder,
                isDailyChallenge = true,
                undoTokens = preferencesRepository.undoTokensFlow.first(),
                helpStocks = currentHelpStocks(),
                nextRecipeHerb = gameEngine.nextRecipeHerb(
                    initialJars.firstOrNull { jar -> jar.isTeapot },
                    level.customerOrder
                ),
                recipeRewardNext = preview.first,
                baseRewardNext = preview.second
            )
        }
    }

    fun loadZenLevel() {
        clearPendingClicks()
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
            isZenMode = true,
            helpStocks = currentHelpStocks()
        )
    }

    /**
     * Toca un frasco. Se puede jugar también mientras se vierte: las pulsaciones
     * se encolan (hasta [MAX_PENDING_CLICKS]) y se aplican en cuanto termina la
     * animación, así que no hay que esperar a verter para planear el siguiente paso.
     */
    fun onJarClicked(jarIndex: Int) {
        val currentState = _uiState.value
        if (currentState.isLevelCompleted) return
        if (jarIndex !in currentState.jars.indices) return

        if (currentState.animatingPour != null) {
            if (pendingClicks.size < MAX_PENDING_CLICKS) {
                pendingClicks.addLast(jarIndex)
                _uiState.update { it.copy(pendingActions = pendingClicks.toList()) }
            }
            return
        }

        val selectedIdx = currentState.selectedJarIndex

        if (selectedIdx == null) {
            if (!currentState.jars[jarIndex].isEmpty) {
                soundManager?.playJarClick()
                // Pack ASMR Cristal Pro: timbre cristalino al elegir frasco
                if (crystalSelectionEnabled) soundManager?.playCrystalChime()
                _uiState.update { it.copy(selectedJarIndex = jarIndex) }
            }
        } else if (selectedIdx == jarIndex) {
            _uiState.update { it.copy(selectedJarIndex = null) }
        } else {
            val src = currentState.jars[selectedIdx]
            val dst = currentState.jars[jarIndex]

            // En el sector de limite de jugadas no se puede pasar del tope. No se
            // pierde: lo que ya esta resuelto sigue contando, y el Deshacer sigue
            // disponible para enmendar la ultima.
            if (currentState.atMoveLimit) {
                soundManager?.playInvalidPour()
                _uiState.update { it.copy(selectedJarIndex = null) }
                return
            }

            val pourResult = gameEngine.pour(src, dst, currentState.customerOrder)
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
                    // Con 3 o mas unidades el vertido es un chorro lleno: se usa el
                    // sonido grave y espaciado mas corto, no gotas sueltas.
                    val isBigPour = units >= 3
                    val firstGlugAt = 130L
                    val glugSpacing = if (isBigPour) 95L else 140L
                    var lastGlugAt = 0L
                    for (i in 0 until units) {
                        val glugAt = firstGlugAt + i * glugSpacing
                        delay(glugAt - lastGlugAt)
                        lastGlugAt = glugAt
                        if (isBigPour) {
                            soundManager?.playPourBig(volume = 0.95f - i * 0.04f, rate = 0.62f + i * 0.05f)
                        } else {
                            val rate = when (i) {
                                0 -> 1f
                                1 -> 1.07f
                                else -> 1.11f
                            }
                            soundManager?.playPourSound(volume = 0.9f - i * 0.05f, rate = rate)
                        }
                    }

                    // Fase 1→2: el chorro llega al destino y se aplica el vertido
                    // (nunca antes de 470ms ni antes de 100ms tras el último glug)
                    delay((470L - lastGlugAt).coerceAtLeast(100L))

                    val stateNow = _uiState.value
                    val newJars = gameEngine.executePour(
                        stateNow.jars,
                        selectedIdx,
                        jarIndex,
                        pourResult.countPoured,
                        stateNow.customerOrder
                    )

                    val newHistory = stateNow.history + listOf(stateNow.jars)
                    val requireRecipe = stateNow.isGateLevel
                    val isCompleted = gameEngine.checkLevelCompleted(
                        newJars,
                        stateNow.customerOrder,
                        requireRecipe
                    )
                    val fulfilled = gameEngine.isOrderFulfilled(newJars, stateNow.customerOrder)
                    val brewed = gameEngine.isTeapotBrewed(newJars, stateNow.customerOrder)
                    val ruined = gameEngine.isTeapotRuined(newJars, stateNow.customerOrder)
                    val teapot = newJars.firstOrNull { it.isTeapot }
                    val wasBrewed = stateNow.teapotBrewed

                    // La hierba secreta de la puerta: en cuanto se destapa entra al
                    // herbario. Se mira el tablero entero, que el vertido destapa la
                    // cima del origen y la del destino.
                    val secretoNuevo = anotarSecretos(newJars)

                    // Fase 2: el líquido llega al destino y el relleno aparece
                    _uiState.update {
                        it.copy(
                            jars = newJars,
                            movesCount = it.movesCount + 1,
                            history = newHistory,
                            isLevelCompleted = isCompleted,
                            orderFulfilled = fulfilled,
                            teapotBrewed = brewed,
                            teapotRuined = ruined,
                            nextRecipeHerb = gameEngine.nextRecipeHerb(teapot, stateNow.customerOrder),
                            showRecipeCelebration = brewed && !wasBrewed,
                            foundSecret = it.foundSecret ?: secretoNuevo,
                            hintMove = null
                        )
                    }

                    if (secretoNuevo != null) {
                        soundManager?.playStingRecipe()
                    }

                    if (brewed && !wasBrewed) {
                        soundManager?.playStingRecipe()
                    }

                    // Fase 3: chorro desvanecido y retorno del frasco
                    delay(400)
                    _uiState.update { it.copy(animatingPour = null) }

                    if (isCompleted) {
                        soundManager?.playStingLevelComplete()
                        onLevelCompleted()
                    } else {
                        // El jugador ya ha podido ir encadenando movimientos.
                        drainPendingClicks()
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
        viewModelScope.launch {
            preferencesRepository.secretIngredientsFlow.collect { secretos ->
                _uiState.update { it.copy(foundSecrets = secretos) }
            }
        }
        HelpItem.entries.forEach { help ->
            viewModelScope.launch {
                preferencesRepository.helpStockFlow(help.id).collect { stock ->
                    _uiState.update { it.copy(helpStocks = it.helpStocks + (help.id to stock)) }
                }
            }
        }
    }

    /**
     * Stock actual de las tres ayudas.
     *
     * Al cargar un nivel se reconstruye el estado entero, y los flujos de stock
     * no vuelven a emitir (llegan con distinctUntilChanged). Sin esto, el panel
     * de ayuda mostraria "sin pistas" pese a haber usos.
     */
    private suspend fun currentHelpStocks(): Map<String, Int> =
        HelpItem.entries.associate { help ->
            help.id to preferencesRepository.helpStockFlow(help.id).first()
        }

/** Usos que le quedan de una ayuda concreta. */
    fun stockOf(help: HelpItem): Int =
        _uiState.value.helpStocks[help.id]
            ?: UserPreferencesRepository.STARTING_HELP_STOCK[help.id]
            ?: 0

    /**
     * Una ayuda sin usos no falla en silencio: se guarda cual se agoto y la UI
     * ofrece conseguirla con un anuncio o comprarla con Hojas de Oro.
     */
    private fun offerDepletedHelp(help: HelpItem) {
        soundManager?.playUiError()
        _uiState.update { it.copy(depletedHelp = help.id) }
    }

    fun dismissDepletedHelp() {
        _uiState.update { it.copy(depletedHelp = null) }
    }

    /**
     * El jugador ha visto el anuncio de una ayuda agotada: recibe un uso. Se
     * llama desde el callback del rewarded ad, ya con la ventana cerrada.
     */
    fun grantHelpByAd(help: HelpItem) {
        viewModelScope.launch {
            preferencesRepository.grantHelpStock(help.id, 1, cost = 0)
            soundManager?.playReward()
        }
    }

    /**
     * Recarga la ayuda agotada. [fromAd] solo cambia el sonido: las dos salidas
     * (anuncio o compra) dan los mismos usos, asi que el jugador puede elegir.
     */
    fun replenishDepletedHelp(fromAd: Boolean) {
        val help = HelpItem.fromId(_uiState.value.depletedHelp ?: return) ?: return
        viewModelScope.launch {
            if (fromAd) {
                preferencesRepository.grantHelpStock(help.id, 1, cost = 0)
                soundManager?.playReward()
            } else {
                val paid = preferencesRepository.grantHelpStock(help.id, help.packSize, help.packPrice)
                if (paid) soundManager?.playPurchase() else soundManager?.playUiError()
            }
            _uiState.update { it.copy(depletedHelp = null) }
        }
    }

    fun undoMove() {
        val currentState = _uiState.value
        if (currentState.history.isEmpty() || currentState.isLevelCompleted) return
        if (currentState.animatingPour != null) return

        if (currentState.undoTokens <= 0) {
            offerDepletedHelp(HelpItem.UNDO)
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
                        hintMove = null,
                        usedUndo = true,
                        teapotBrewed = gameEngine.isTeapotBrewed(previousJars, currentState.customerOrder),
                        teapotRuined = gameEngine.isTeapotRuined(previousJars, currentState.customerOrder),
                        nextRecipeHerb = gameEngine.nextRecipeHerb(
                            previousJars.firstOrNull { jar -> jar.isTeapot },
                            currentState.customerOrder
                        ),
                        showRecipeCelebration = false
                    )
                }
            }
        }
    }

    fun requestExitConfirmation() {
        _uiState.update { it.copy(showExitConfirmDialog = true) }
    }

    fun cancelExit() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
    }

    fun restartLevel(onShowAd: () -> Unit) {

        clearPendingClicks()
        val currentState = _uiState.value
        val newRestartCount = currentState.restartCount + 1

        if (newRestartCount % 3 == 0) {
            onShowAd()
        }

        if (currentState.isDailyChallenge) {
            loadDailyChallenge()
        } else if (currentState.isZenMode) {
            loadZenLevel()
        } else {
            loadLevel(currentState.levelNumber)
        }

        _uiState.update { it.copy(restartCount = newRestartCount) }
    }

    /**
     * Pide la pista. Gasta un uso; si ya no quedan, avisa con la ventana en vez
     * de lanzar un anuncio sin avisar.
     */
    fun requestHint() {
        val currentState = _uiState.value
        if (currentState.isLevelCompleted) return

        viewModelScope.launch {
            if (!preferencesRepository.consumeHelpStock(HelpItem.HINT.id)) {
                offerDepletedHelp(HelpItem.HINT)
                return@launch
            }
            // Con tetera activa la receta siempre es el camino: la Pista la respeta.
            val hasTeapot = currentState.jars.any { it.isTeapot }
            val order = if (currentState.isGateLevel || hasTeapot) {
                currentState.customerOrder?.requiredHerbs
            } else {
                null
            }
            // El reparto sin busqueda va primero porque con los niveles bien dispersos la
            // busqueda por anchura se queda corta y la pista se quedaba muda. Si
            // no, se cae a la busqueda como antes.
            val solution = solver.repartirYResolver(currentState.jars, order)
                ?: solver.solve(currentState.jars, order)
            if (solution != null && solution.isNotEmpty()) {
                _uiState.update { it.copy(usedHint = true) }
                soundManager?.playHint()
                _uiState.update { it.copy(hintMove = solution.first()) }
            } else {
                // Sin solucion no se gasta el uso: se devuelve al jugador.
                preferencesRepository.grantHelpStock(HelpItem.HINT.id, 1, cost = 0)
                soundManager?.playInvalidPour()
            }
        }
    }

    fun addExtraJar() {
        val currentState = _uiState.value

        viewModelScope.launch {
            if (!preferencesRepository.consumeHelpStock(HelpItem.EXTRA_JAR.id)) {
                offerDepletedHelp(HelpItem.EXTRA_JAR)
                return@launch
            }
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
    }

    /**
     * Apunta las hierbas secretas que ya esten a la vista y devuelve la primera
     * que el jugador no habia encontrado todavia. La de la puerta entra al
     * herbario en cuanto se destapa, y hasta que no esta a la vista no cuenta.
     */
    private fun anotarSecretos(jars: List<Jar>): Herb? {
        val vista = gameEngine.visibleSecrets(jars)
            .firstOrNull { it.id !in _uiState.value.foundSecrets } ?: return null
        viewModelScope.launch { preferencesRepository.unlockSecretIngredient(vista.id) }
        return vista
    }

    fun dismissSecretFound() {
        _uiState.update { it.copy(foundSecret = null) }
    }

    fun dismissRecipeCelebration() {
        _uiState.update { it.copy(showRecipeCelebration = false) }
    }

    private fun onLevelCompleted() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isZenMode) return@launch
            pendingClicks.clear()

            preferencesRepository.incrementCompletedLevels()
            soundManager?.playReward()

            if (state.isDailyChallenge) {
                preferencesRepository.addStamina(3)
                // El reto se cierra al superarlo: hasta manana no vuelve a
                // ofrecerse, y se apunta con la semilla con la que se cargo la
                // partida y no con la de este instante.
                preferencesRepository.markDailyChallengeDone(
                    dailyChallengeSeed ?: LevelRepository.dailySeed()
                )
            }

            // Premio con tabla de repeticiones: 1ª +15/+50 · 2ª +5 · 3ª+ nada.
            val rewardId = rewardIdFor(state.levelNumber, state.customerOrder?.id)
            val reward = preferencesRepository.claimCompletionReward(rewardId, state.orderFulfilled)
            _uiState.update {
                it.copy(
                    bonusLeaves = reward.recipe,
                    rewardBase = reward.base,
                    rewardRecipe = reward.recipe
                )
            }

            // Hierbas: solo se desbloquean en niveles puerta con receta cumplida
            if (state.isGateLevel && state.orderFulfilled) {
                val currentUnlocked = preferencesRepository.unlockedHerbsFlow.first()
                val nextLockedHerb = HerbUnlocks.nextLocked(currentUnlocked)
                if (nextLockedHerb != null) {
                    preferencesRepository.unlockHerb(nextLockedHerb.id)
                    soundManager?.playUnlock()
                    _uiState.update { it.copy(newlyUnlockedHerb = nextLockedHerb) }
                }
            }

            // El diario: al terminar la puerta que cierra el tramo llega la carta
            // de su sector. La tabla es la misma que lee el diario, asi que la
            // carta que se entrega es exactamente la que aparece despues.
            JournalLetters.alTerminarPuerta(state.levelNumber)?.let { carta ->
                preferencesRepository.unlockCustomerLetter(carta.id)
            }

            if (!state.isDailyChallenge) {
                preferencesRepository.updateCurrentLevel(state.levelNumber + 1)
            }

            // Solo hay receta que celebrar si el cliente se la llevo preparada.
            val brewed = _uiState.value.teapotBrewed
            // completionHandled avisa a la UI de que ya puede decidir: mostrar el
            // recuadro de logro o pasar directo al resumen.
            _uiState.update { it.copy(hasCelebration = brewed, completionHandled = true) }
        }
    }
}
