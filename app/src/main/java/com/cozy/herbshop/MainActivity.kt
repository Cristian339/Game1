package com.cozy.herbshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cozy.herbshop.data.ads.AdManager
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.audio.MusicTrack
import com.cozy.herbshop.data.billing.BillingManager
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.ui.game.GameScreen
import com.cozy.herbshop.ui.game.GameViewModel
import com.cozy.herbshop.ui.game.LevelCompleteScreen
import com.cozy.herbshop.ui.herbarium.HerbariumScreen
import com.cozy.herbshop.ui.journal.JournalScreen
import com.cozy.herbshop.ui.main.MainMenuScreen
import com.cozy.herbshop.ui.main.MainViewModel
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

@Composable
fun CozyHerbShopAppNavHost(
    activity: ComponentActivity,
    preferencesRepository: UserPreferencesRepository,
    levelRepository: LevelRepository,
    adManager: AdManager,
    billingManager: BillingManager
) {
    val navController = rememberNavController()
    val mainViewModel = remember { MainViewModel(preferencesRepository) }
    val gameViewModel = remember {
        GameViewModel(levelRepository, preferencesRepository, soundManager = AudioHub.sound)
    }

    val mainUiState by mainViewModel.uiState.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore("/{")

    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            "main_menu", "settings", "shop", "herbarium", "journal" -> {
                AudioHub.music.play(MusicTrack.MENU)
                AudioHub.ambient.stop()
            }
            "game", "daily_challenge", "level_complete" -> {
                AudioHub.music.play(MusicTrack.GAME)
                AudioHub.ambient.stop()
            }
            "zen_mode" -> {
                AudioHub.music.play(MusicTrack.ZEN)
                preferencesRepository.ambientSoundFlow.collect { soundName ->
                    if (AudioHub.ambient.ambientEnabled) AudioHub.ambient.play(soundName)
                }
            }
            else -> Unit
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
                    navController.navigate("daily_challenge")
                },
                onZenModeClicked = {
                    navController.navigate("zen_mode")
                },
                onJournalClicked = {
                    navController.navigate("journal")
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
                onWatchAdForStamina = {
                    adManager.showRewardedAd(activity) {
                        mainViewModel.addStaminaReward()
                        AudioHub.sound.playReward()
                    }
                }
            )
        }

        composable("journal") {
            JournalScreen(
                repository = preferencesRepository,
                onBackClicked = { navController.popBackStack() }
            )
        }

        composable("zen_mode") {
            LaunchedEffect(Unit) {
                gameViewModel.loadZenLevel()
            }
            GameScreen(
                viewModel = gameViewModel,
                levelNumber = 0,
                onBackClicked = { navController.popBackStack() },
                onLevelCompleted = {
                    // El Modo Zen gestiona su propia transición infinita en GameScreen
                },
                onWatchAdForHint = { gameViewModel.requestHint() },
                onWatchAdForExtraJar = { gameViewModel.addExtraJar() }
            )
        }

        composable("daily_challenge") {
            LaunchedEffect(Unit) {
                gameViewModel.loadDailyChallenge()
            }
            GameScreen(
                viewModel = gameViewModel,
                levelNumber = 999,
                onBackClicked = { navController.popBackStack() },
                onLevelCompleted = {
                    navController.navigate("level_complete/999") {
                        popUpTo("daily_challenge") { inclusive = true }
                    }
                },
                onWatchAdForHint = {
                    adManager.showRewardedAd(activity) {
                        gameViewModel.requestHint()
                    }
                },
                onWatchAdForExtraJar = {
                    adManager.showRewardedAd(activity) {
                        gameViewModel.addExtraJar()
                    }
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
                onBackClicked = { navController.popBackStack() },
                onLevelCompleted = {
                    navController.navigate("level_complete/$levelNum") {
                        popUpTo("game/$levelNum") { inclusive = true }
                    }
                },
                onWatchAdForHint = {
                    adManager.showRewardedAd(activity) {
                        gameViewModel.requestHint()
                    }
                },
                onWatchAdForExtraJar = {
                    adManager.showRewardedAd(activity) {
                        gameViewModel.addExtraJar()
                    }
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
                AudioHub.sound.playLevelCompleteSound()
                gameState.newlyUnlockedHerb?.let { AudioHub.sound.playUnlock() }
            }

            BackHandler {
                gameViewModel.resetLevelCompleted()
                navController.navigate("main_menu") {
                    popUpTo("main_menu") { inclusive = true }
                }
            }

            LevelCompleteScreen(
                levelNumber = levelNum,
                unlockedHerb = gameState.newlyUnlockedHerb,
                onNextLevelClicked = {
                    gameViewModel.resetLevelCompleted()
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
                                    navController.navigate("main_menu") {
                                        popUpTo("main_menu") { inclusive = true }
                                    }
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
                                navController.navigate("main_menu") {
                                    popUpTo("main_menu") { inclusive = true }
                                }
                            }
                        )
                    }
                },
                onMenuClicked = {
                    gameViewModel.resetLevelCompleted()
                    navController.navigate("main_menu") {
                        popUpTo("main_menu") { inclusive = true }
                    }
                }
            )
        }

        composable("herbarium") {
            HerbariumScreen(
                repository = preferencesRepository,
                onBackClicked = { navController.popBackStack() }
            )
        }

        composable("shop") {
            ShopScreen(
                billingManager = billingManager,
                repository = preferencesRepository,
                onBackClicked = { navController.popBackStack() },
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
                onBackClicked = { navController.popBackStack() }
            )
        }
    }
}
