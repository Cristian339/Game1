package com.cozy.herbshop

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.cozy.herbshop.ui.collection.CollectionShowcaseScreen
import com.cozy.herbshop.ui.herbarium.HerbDetailDialog
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cozy.herbshop.data.ads.AdManager
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.audio.MusicTrack
import com.cozy.herbshop.data.model.HelpItem
import com.cozy.herbshop.ui.components.GeneratedArt
import com.cozy.herbshop.ui.components.GameScene
import com.cozy.herbshop.data.billing.BillingManager
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.ui.game.GameScreen
import com.cozy.herbshop.ui.game.GameViewModel
import com.cozy.herbshop.ui.game.LevelCompleteScreen
import com.cozy.herbshop.ui.herbarium.HerbariumScreen
import com.cozy.herbshop.ui.album.AlbumScreen
import com.cozy.herbshop.ui.main.MainMenuScreen
import com.cozy.herbshop.ui.main.MainViewModel
import com.cozy.herbshop.ui.map.LevelMapScreen
import com.cozy.herbshop.ui.settings.SettingsScreen
import com.cozy.herbshop.ui.shop.ShopScreen
import com.cozy.herbshop.ui.theme.CozyHerbShopTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var levelRepository: LevelRepository
    private lateinit var adManager: AdManager
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AudioHub.init(applicationContext)
        GeneratedArt.init(applicationContext)

        preferencesRepository = UserPreferencesRepository(applicationContext)
        levelRepository = LevelRepository(applicationContext)

        adManager = AdManager(this)
        adManager.initialize()

        billingManager = BillingManager(
            context = applicationContext,
            onAdRemovalPurchased = {},
            onThemePackPurchased = {}
        )
        billingManager.startConnection()

        setContent {
            CozyHerbShopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE -> AudioHub.pauseAll()
                                Lifecycle.Event.ON_RESUME -> AudioHub.resumeAll()
                                Lifecycle.Event.ON_STOP -> AudioHub.pauseAll()
                                Lifecycle.Event.ON_START -> AudioHub.resumeAll()
                                else -> Unit
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    LaunchedEffect(Unit) {
                        preferencesRepository.soundEnabledFlow.collect { AudioHub.sound.soundEnabled = it }
                    }
                    LaunchedEffect(Unit) {
                        preferencesRepository.musicEnabledFlow.collect { AudioHub.music.musicEnabled = it }
                    }
                    LaunchedEffect(Unit) {
                        preferencesRepository.ambientEnabledFlow.collect { AudioHub.ambient.ambientEnabled = it }
                    }

                    CozyHerbShopAppNavHost(
                        activity = this,
                        preferencesRepository = preferencesRepository,
                        levelRepository = levelRepository,
                        adManager = adManager,
                        billingManager = billingManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AudioHub.release()
        }
    }
}

/**
 * Evita que machacar el botón "atrás" rompa la pila de navegación (pantalla en
 * blanco): ignora los toques repetidos y siempre vuelve al menú de forma segura.
 */
private class NavGuard {
    private var lastPopAt = 0L
    private var lastMenuAt = 0L

    fun pop(navController: NavHostController) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPopAt < 250L) return
        lastPopAt = now
        navController.popBackStack()
    }

    fun goToMenu(navController: NavHostController) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastMenuAt < 400L) return
        lastMenuAt = now
        navController.navigate("main_menu") {
            popUpTo("main_menu") { inclusive = true }
            launchSingleTop = true
        }
    }

    fun reset() {
        lastPopAt = 0L
        lastMenuAt = 0L
    }
}

@Composable
fun CozyHerbShopAppNavHost(
    activity: ComponentActivity,
    preferencesRepository: UserPreferencesRepository,
    levelRepository: LevelRepository,
    adManager: AdManager,
    billingManager: BillingManager
) {
    val navController = rememberNavController()
    val navGuard = remember { NavGuard() }
    val asmrScope = rememberCoroutineScope()
    val mainViewModel = remember { MainViewModel(preferencesRepository) }
    val gameViewModel = remember {
        GameViewModel(levelRepository, preferencesRepository, soundManager = AudioHub.sound)
    }

    val mainUiState by mainViewModel.uiState.collectAsState()
    val gameUiState by gameViewModel.uiState.collectAsState()
    val unlockedAsmrPacks by preferencesRepository.unlockedAsmrPacksFlow.collectAsState(initial = setOf("asmr_cristal"))
    val asmrInGameEnabled by preferencesRepository.asmrInGameEnabledFlow.collectAsState(initial = true)

    // Ambiente ASMR disponible según los packs comprados (null si solo tiene el gratis)
    val selectedAsmr by preferencesRepository.selectedAsmrPackFlow.collectAsState(initial = null)
    val asmrChoices = remember(unlockedAsmrPacks) {
        com.cozy.herbshop.ui.collection.asmrSlots
            .filter { it.id != "asmr_cristal" && unlockedAsmrPacks.contains(it.id) }
            .map { com.cozy.herbshop.ui.game.AsmrChoice(it.id, it.name) }
    }
    val asmrAmbientName = remember(unlockedAsmrPacks, selectedAsmr) {
        preferencesRepository.asmrAmbientFor(unlockedAsmrPacks, selectedAsmr)
    }
    LaunchedEffect(unlockedAsmrPacks) {
        gameViewModel.crystalSelectionEnabled = unlockedAsmrPacks.contains("asmr_cristal")
    }

    val toggleAsmrInGame = {
        val next = !asmrInGameEnabled
        asmrScope.launch { preferencesRepository.setAsmrInGameEnabled(next) }
        if (next && asmrAmbientName != null) {
            AudioHub.ambient.play(asmrAmbientName)
        } else {
            AudioHub.ambient.stop()
        }
    }



    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore("/{")

    // La música de partida sigue al escenario (Amanecer/Invernadero, Noche,
    // Bosque, Otoño) usando la misma función que el fondo de la partida.
    val gameMusicTrack = remember(
        gameUiState.levelNumber,
        gameUiState.isDailyChallenge,
        gameUiState.isZenMode
    ) {
        MusicTrack.forScene(
            GameScene.forMode(gameUiState.levelNumber, gameUiState.isDailyChallenge, gameUiState.isZenMode)
        )
    }
    val musicInGameEnabled by preferencesRepository.musicInGameEnabledFlow.collectAsState(initial = true)

    val setMusicInGame: (Boolean) -> Unit = { enabled ->
        asmrScope.launch { preferencesRepository.setMusicInGameEnabled(enabled) }
        // Se usa la propiedad del gestor (con fade) en vez de pause(), que marca la
        // app como pausada y hace que play() no reinicie la pista.
        AudioHub.music.musicEnabled = enabled
    }

    val setAsmrInGame: (Boolean) -> Unit = { enabled ->
        asmrScope.launch { preferencesRepository.setAsmrInGameEnabled(enabled) }
        if (enabled && asmrAmbientName != null) {
            AudioHub.ambient.play(asmrAmbientName)
        } else {
            AudioHub.ambient.stop()
        }
    }

    LaunchedEffect(currentRoute) {
        // La musica de partida va solo en las pantallas donde se juega. Antes se
        // hacia con una lista de rutas y lo que no estuviera en ella (el mapa de
        // niveles, la coleccion) se caia en el "else" sin hacer nada: al salir de
        // un nivel al mapa se seguia oyendo la pista de la partida. Ahora es al
        // reves: cualquier pantalla que no sea de juego devuelve la del menu.
        val enPartida = currentRoute in setOf("game", "daily_challenge", "level_complete")
        when {
            currentRoute == "zen_mode" -> {
                AudioHub.music.play(MusicTrack.ZEN)
                preferencesRepository.ambientSoundFlow.collect { soundName ->
                    if (AudioHub.ambient.ambientEnabled) AudioHub.ambient.play(soundName)
                }
            }
            enPartida -> {
                if (musicInGameEnabled) {
                    AudioHub.music.play(if (gameUiState.isZenMode) MusicTrack.ZEN else gameMusicTrack)
                } else {
                    AudioHub.music.pause()
                }
                // Capa ASMR en partida si el jugador tiene un pack y la ha activado
                if (asmrInGameEnabled && asmrAmbientName != null && !gameUiState.isZenMode) {
                    AudioHub.ambient.play(asmrAmbientName)
                } else {
                    AudioHub.ambient.stop()
                }
            }
            else -> {
                AudioHub.music.play(MusicTrack.MENU)
                AudioHub.ambient.stop()
            }
        }
    }

    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(
                viewModel = mainViewModel,
                onPlayClicked = {
                    navController.navigate("game/${mainUiState.currentLevel}")
                },
                onDailyChallengeClicked = {
                    // Con el reto de hoy ya superado el boton va apagado; la
                    // guarda evita que un toque tardio lo vuelva a abrir.
                    if (!mainUiState.dailyChallengeDoneToday) {
                        navController.navigate("daily_challenge")
                    }
                },
                onZenModeClicked = {
                    navController.navigate("zen_mode")
                },
                onAlbumClicked = {
                    navController.navigate("album")
                },
                onLevelMapClicked = {
                    navController.navigate("level_map")
                },
                onHerbariumClicked = {
                    navController.navigate("herbarium")
                },
                onShopClicked = {
                    navController.navigate("shop")
                },
                onSettingsClicked = {
                    navController.navigate("settings")
                },
                onCollectionClicked = {
                    navController.navigate("collection")
                },
                onWatchAdForStamina = {
                    adManager.showRewardedAd(activity) {
                        mainViewModel.addStaminaReward()
                        AudioHub.sound.playReward()
                    }
                }
            )
        }

        composable("album") {
            AlbumScreen(
                repository = preferencesRepository,
                onBackClicked = { navGuard.pop(navController) }
            )
        }

        composable("level_map") {
            LevelMapScreen(
                repository = preferencesRepository,
                levelRepository = levelRepository,
                onLevelSelected = { selectedLevel ->
                    mainViewModel.consumeStaminaForGame(
                        onSuccess = {
                            navController.navigate("game/$selectedLevel")
                        },
                        onNoStamina = {
                            AudioHub.sound.playNoStamina()
                            adManager.showRewardedAd(activity) {
                                mainViewModel.addStaminaReward()
                                AudioHub.sound.playReward()
                            }
                        }
                    )
                },
                onBackClicked = { navGuard.pop(navController) }
            )
        }

        composable("zen_mode") {
            LaunchedEffect(Unit) {
                gameViewModel.loadZenLevel()
            }
            GameScreen(
                viewModel = gameViewModel,
                levelNumber = 0,
                onBackClicked = { navGuard.pop(navController) },
                onLevelCompleted = {
                    // El Modo Zen gestiona su propia transición infinita en GameScreen
                },
                onWatchAdForHelp = { help ->
                    adManager.showRewardedAd(activity) {
                        gameViewModel.grantHelpByAd(help)
                    }
                }
            )
        }

        composable("daily_challenge") {
            LaunchedEffect(Unit) {
                gameViewModel.loadDailyChallenge()
            }
            GameScreen(
                viewModel = gameViewModel,
                levelNumber = 999,
                onBackClicked = { navGuard.pop(navController) },
                onLevelCompleted = {
                    navController.navigate("level_complete/999") {
                        popUpTo("daily_challenge") { inclusive = true }
                    }
                },
                onWatchAdForHelp = { help ->
                    adManager.showRewardedAd(activity) {
                        gameViewModel.grantHelpByAd(help)
                    }
                },
                asmrAmbientName = asmrAmbientName,
                asmrEnabled = asmrInGameEnabled,
                onToggleAsmr = toggleAsmrInGame,
                musicInGameEnabled = musicInGameEnabled,
                onToggleMusic = { setMusicInGame(!musicInGameEnabled) },
                onSetMusic = setMusicInGame,
                onSetAsmr = setAsmrInGame,
                asmrChoices = asmrChoices,
                selectedAsmrPack = selectedAsmr,
                onSelectAsmr = { packId ->
                    asmrScope.launch { preferencesRepository.setSelectedAsmrPack(packId) }
                    val name = preferencesRepository.asmrAmbientFor(unlockedAsmrPacks, packId)
                    if (name == null) AudioHub.ambient.stop() else AudioHub.ambient.play(name)
                    // Elegir un ambiente lo enciende. Antes no se tocaba este
                    // estado, asi que el interruptor de la partida podia marcar
                    // "apagado" mientras el ambiente estaba sonando.
                    asmrScope.launch { preferencesRepository.setAsmrInGameEnabled(name != null) }
                },
                onOpenShop = {
                    navGuard.reset()
                    navController.navigate("shop") { launchSingleTop = true }
                }
            )
        }

        composable(
            route = "game/{levelNumber}",
            arguments = listOf(navArgument("levelNumber") { type = NavType.IntType })
        ) { backStackEntry ->
            val levelNum = backStackEntry.arguments?.getInt("levelNumber") ?: 1

            GameScreen(
                viewModel = gameViewModel,
                levelNumber = levelNum,
                onBackClicked = { navGuard.pop(navController) },
                onLevelCompleted = {
                    navController.navigate("level_complete/$levelNum") {
                        popUpTo("game/$levelNum") { inclusive = true }
                    }
                },
                onWatchAdForHelp = { help ->
                    adManager.showRewardedAd(activity) {
                        gameViewModel.grantHelpByAd(help)
                    }
                },
                asmrAmbientName = asmrAmbientName,
                asmrEnabled = asmrInGameEnabled,
                onToggleAsmr = toggleAsmrInGame,
                musicInGameEnabled = musicInGameEnabled,
                onToggleMusic = { setMusicInGame(!musicInGameEnabled) },
                onSetMusic = setMusicInGame,
                onSetAsmr = setAsmrInGame,
                asmrChoices = asmrChoices,
                selectedAsmrPack = selectedAsmr,
                onSelectAsmr = { packId ->
                    asmrScope.launch { preferencesRepository.setSelectedAsmrPack(packId) }
                    val name = preferencesRepository.asmrAmbientFor(unlockedAsmrPacks, packId)
                    if (name == null) AudioHub.ambient.stop() else AudioHub.ambient.play(name)
                    // Elegir un ambiente lo enciende. Antes no se tocaba este
                    // estado, asi que el interruptor de la partida podia marcar
                    // "apagado" mientras el ambiente estaba sonando.
                    asmrScope.launch { preferencesRepository.setAsmrInGameEnabled(name != null) }
                },
                onOpenShop = {
                    navGuard.reset()
                    navController.navigate("shop") { launchSingleTop = true }
                }
            )
        }

        composable(
            route = "level_complete/{levelNumber}",
            arguments = listOf(navArgument("levelNumber") { type = NavType.IntType })
        ) { backStackEntry ->
            val levelNum = backStackEntry.arguments?.getInt("levelNumber") ?: 1
            val gameState by gameViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                // La fanfarria de victoria ya suena al completar el nivel;
                // aquí solo el sonido de "hierba nueva desbloqueada".
                gameState.newlyUnlockedHerb?.let { AudioHub.sound.playUnlock() }
            }

            BackHandler {
                gameViewModel.resetLevelCompleted()
                navGuard.goToMenu(navController)
            }

            LevelCompleteScreen(
                levelNumber = levelNum,
                unlockedHerb = gameState.newlyUnlockedHerb,
                orderFulfilled = gameState.orderFulfilled,
                bonusLeaves = gameState.bonusLeaves,
                baseReward = gameState.rewardBase,
                recipeName = gameState.customerOrder?.recipeName,
                movesCount = gameState.movesCount,
                parMoves = gameState.parMoves,
                usedHint = gameState.usedHint,
                usedUndo = gameState.usedUndo,
                isDailyChallenge = gameState.isDailyChallenge,
                onNextLevelClicked = {
                    gameViewModel.resetLevelCompleted()
                    if (gameState.isDailyChallenge) {
                        // El reto diario se cierra al superarlo: no hay siguiente
                        // nivel ni se puede volver a jugar hoy. La pantalla ya
                        // esconde el boton, pero este guardia evita que una
                        // pulsacion tardia o una pila de navegacion vieja abran
                        // otro nivel.
                        navGuard.goToMenu(navController)
                    } else {
                        val nextLevel = levelNum + 1

                        val isAdsRemoved = mainUiState.adsRemoved
                        if (!isAdsRemoved && nextLevel % AdManager.INTERSTITIAL_FREQUENCY_LEVELS == 0) {
                            adManager.showInterstitialAd(activity) {
                                mainViewModel.consumeStaminaForGame(
                                    onSuccess = {
                                        navController.navigate("game/$nextLevel") {
                                            popUpTo("main_menu") { inclusive = false }
                                        }
                                    },
                                    onNoStamina = {
                                        AudioHub.sound.playNoStamina()
                                        navGuard.goToMenu(navController)
                                    }
                                )
                            }
                        } else {
                            mainViewModel.consumeStaminaForGame(
                                onSuccess = {
                                    navController.navigate("game/$nextLevel") {
                                        popUpTo("main_menu") { inclusive = false }
                                    }
                                },
                                onNoStamina = {
                                    AudioHub.sound.playNoStamina()
                                    navGuard.goToMenu(navController)
                                }
                            )
                        }
                    }
                },
                onMenuClicked = {
                    gameViewModel.resetLevelCompleted()
                    navGuard.goToMenu(navController)
                }
            )
        }

        composable("herbarium") {
            HerbariumScreen(
                repository = preferencesRepository,
                onBackClicked = { navGuard.pop(navController) }
            )
        }

        composable("collection") {
            val unlockedHerbs by preferencesRepository.unlockedHerbsFlow.collectAsState(initial = emptySet())
            val unlockedDecorations by preferencesRepository.unlockedDecorationsFlow.collectAsState(initial = emptySet())
            val unlockedAsmr by preferencesRepository.unlockedAsmrPacksFlow.collectAsState(initial = setOf("asmr_cristal"))
            val leaves by preferencesRepository.goldLeavesFlow.collectAsState(initial = 0)
            var herbDialog by remember { mutableStateOf<com.cozy.herbshop.data.model.Herb?>(null) }
            herbDialog?.let { herb ->
                HerbDetailDialog(herb = herb, onDismiss = { herbDialog = null })
            }
            val selectedAsmr by preferencesRepository.selectedAsmrPackFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            CollectionShowcaseScreen(
                unlockedHerbs = unlockedHerbs,
                unlockedDecorations = unlockedDecorations,
                unlockedAsmrPacks = unlockedAsmr,
                selectedAsmrPack = selectedAsmr,
                goldLeaves = leaves,
                onBackClicked = { navGuard.pop(navController) },
                onHerbClick = { herbDialog = it },
                onAsmrSelected = { packId ->
                    AudioHub.sound.playUiToggle()
                    scope.launch { preferencesRepository.setSelectedAsmrPack(packId) }
                    val name = if (packId == null) null else preferencesRepository.asmrAmbientFor(unlockedAsmr, packId)
                    if (packId == null || name == null) AudioHub.ambient.stop() else AudioHub.ambient.play(name)
                },
                onSlotClick = {
                    navGuard.reset()
                    navController.navigate("shop") { launchSingleTop = true }
                }
            )
        }

        composable("shop") {
            ShopScreen(
                billingManager = billingManager,
                repository = preferencesRepository,
                onBackClicked = { navGuard.pop(navController) },
                onWatchAdForStamina = {
                    adManager.showRewardedAd(activity) {
                        mainViewModel.addStaminaReward()
                        AudioHub.sound.playReward()
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                repository = preferencesRepository,
                onBackClicked = { navGuard.pop(navController) }
            )
        }
    }
}
