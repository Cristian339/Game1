package com.cozy.herbshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cozy.herbshop.data.ads.AdManager
import com.cozy.herbshop.data.billing.BillingManager
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.ui.game.GameScreen
import com.cozy.herbshop.ui.game.GameViewModel
import com.cozy.herbshop.ui.game.LevelCompleteScreen
import com.cozy.herbshop.ui.herbarium.HerbariumScreen
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
    val gameViewModel = remember { GameViewModel(levelRepository, preferencesRepository) }

    val mainUiState by mainViewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(
                viewModel = mainViewModel,
                onPlayClicked = {
                    navController.navigate("game/${mainUiState.currentLevel}")
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

            LevelCompleteScreen(
                levelNumber = levelNum,
                unlockedHerb = gameState.newlyUnlockedHerb,
                onNextLevelClicked = {
                    val nextLevel = levelNum + 1

                    val isAdsRemoved = mainUiState.adsRemoved
                    if (!isAdsRemoved && nextLevel % AdManager.INTERSTITIAL_FREQUENCY_LEVELS == 0) {
                        adManager.showInterstitialAd(activity) {
                            mainViewModel.consumeStaminaForGame(
                                onSuccess = { navController.navigate("game/$nextLevel") },
                                onNoStamina = { navController.navigate("main_menu") }
                            )
                        }
                    } else {
                        mainViewModel.consumeStaminaForGame(
                            onSuccess = { navController.navigate("game/$nextLevel") },
                            onNoStamina = { navController.navigate("main_menu") }
                        )
                    }
                },
                onMenuClicked = {
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
                onBackClicked = { navController.popBackStack() },
                onWatchAdForStamina = {
                    adManager.showRewardedAd(activity) {
                        mainViewModel.addStaminaReward()
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
