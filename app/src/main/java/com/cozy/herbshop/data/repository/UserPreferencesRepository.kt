package com.cozy.herbshop.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val CURRENT_LEVEL = intPreferencesKey("current_level")
        val STAMINA_COUNT = intPreferencesKey("stamina_count")
        val LAST_STAMINA_REGEN_TIME = longPreferencesKey("last_stamina_regen_time")
        val UNLOCKED_HERBS = stringSetPreferencesKey("unlocked_herbs")
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val THEME_PACK_UNLOCKED = booleanPreferencesKey("theme_pack_unlocked")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    val MUSIC_IN_GAME_ENABLED = booleanPreferencesKey("music_in_game_enabled")
    val SELECTED_ASMR_PACK = stringPreferencesKey("selected_asmr_pack")
        val AMBIENT_ENABLED = booleanPreferencesKey("ambient_enabled")
        val COMPLETED_LEVELS_COUNT = intPreferencesKey("completed_levels_count")
        val GOLD_LEAVES = intPreferencesKey("gold_leaves")
        val UNLOCKED_DECORATIONS = stringSetPreferencesKey("unlocked_decorations")
        val UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
        val COLORBLIND_MODE = booleanPreferencesKey("colorblind_mode")
        val AMBIENT_SOUND = stringPreferencesKey("ambient_sound")
        val CUSTOMER_LETTERS = stringSetPreferencesKey("customer_letters")
        val SECRET_INGREDIENTS = stringSetPreferencesKey("secret_ingredients")
        val TUTORIAL_BASIC_DONE = booleanPreferencesKey("tutorial_basic_done")
        val TUTORIAL_RECIPE_DONE = booleanPreferencesKey("tutorial_recipe_done")
        val UNDO_TOKENS = intPreferencesKey("undo_tokens")
        val UNLOCKED_ASMR_PACKS = stringSetPreferencesKey("unlocked_asmr_packs")
        val ASMR_IN_GAME_ENABLED = booleanPreferencesKey("asmr_in_game_enabled")

        /** Contador de usos de cada ayuda en partida (Pista, +Frasco...). */
        fun helpStockKey(helpId: String) = intPreferencesKey("help_stock_$helpId")

        const val MAX_STAMINA = 5
        const val STAMINA_REGEN_INTERVAL_MS = 15 * 60 * 1000L

        // Recompensas por repetir un nivel (ver computeReward).
        const val FIRST_LEVEL_REWARD = 15
        const val FIRST_RECIPE_REWARD = 50
        const val REPEAT_RECIPE_REWARD = 5

        /** Contador de veces completadas un nivel concreto. */
        fun rewardCountKey(levelId: String) = intPreferencesKey("reward_count_$levelId")

        /**
         * Premios al terminar un nivel. Solo la primera partida paga completo; la
         * segunda deja una propina y a partir de la tercera no se repite.
         *
         * | Partida | Base nivel | Receta (si se cumplió) |
         * |---------|-----------|----------------------|
         * | 1ª      | +15       | +50                  |
         * | 2ª      | 0         | +5                   |
         * | 3ª+     | 0         | 0                    |
         */
        fun computeReward(timesCompleted: Int, recipeFulfilled: Boolean): CompletionReward {
            val base = if (timesCompleted <= 0) FIRST_LEVEL_REWARD else 0
            val recipe = when {
                !recipeFulfilled -> 0
                timesCompleted <= 0 -> FIRST_RECIPE_REWARD
                timesCompleted == 1 -> REPEAT_RECIPE_REWARD
                else -> 0
            }
            return CompletionReward(base = base, recipe = recipe)
        }

        /**
         * Usos de cada ayuda con los que arranca una partida nueva. Deshacer
         * mantiene su valor historico; las demas empiezan con menos para que el
         * jugador llegue a ver la ventana de "te has quedado sin" sin aggressive.
         */
        val STARTING_HELP_STOCK = mapOf(
            "help_undo" to 3,
            "help_hint" to 3,
            "help_extra_jar" to 2
        )

        }

    /** Cuantos usos de una ayuda quedan sin gastar. */
    fun helpStockFlow(helpId: String): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[helpStockKey(helpId)] ?: (STARTING_HELP_STOCK[helpId] ?: 0)
    }.distinctUntilChanged()

    /** Premios efectivamente pagados al terminar un nivel. */
    data class CompletionReward(val base: Int, val recipe: Int) {
        val total: Int get() = base + recipe
    }

    /** Cuántas veces se ha completado ya el nivel (para mostrar el premio en juego). */
    suspend fun rewardTimes(levelId: String): Int {
        val key = rewardCountKey(levelId)
        return context.dataStore.data.first()[key] ?: 0
    }

    /**
     * Paga (y consume) el premio de completar un nivel. Devuelve lo efectivamente
     * pagado para poder mostrárselo al jugador.
     */
    suspend fun claimCompletionReward(levelId: String, recipeFulfilled: Boolean): CompletionReward {
        val key = rewardCountKey(levelId)
        var paid = CompletionReward(0, 0)
        context.dataStore.edit { prefs ->
            val times = prefs[key] ?: 0
            paid = computeReward(times, recipeFulfilled)
            if (paid.total > 0) {
                val leaves = prefs[GOLD_LEAVES] ?: 50
                prefs[GOLD_LEAVES] = leaves + paid.total
            }
            prefs[key] = times + 1
        }
        return paid
    }

    val currentLevelFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[CURRENT_LEVEL] ?: 1
    }

    val staminaCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[STAMINA_COUNT] ?: MAX_STAMINA
    }

    val unlockedHerbsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[UNLOCKED_HERBS] ?: setOf("chamomile", "lavender", "mint")
    }

    val adsRemovedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ADS_REMOVED] ?: false
    }

    val themePackUnlockedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[THEME_PACK_UNLOCKED] ?: false
    }

    val selectedThemeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_THEME] ?: "cozy_default"
    }

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SOUND_ENABLED] ?: true
    }.distinctUntilChanged()

    val musicEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[MUSIC_ENABLED] ?: true
    }.distinctUntilChanged()

    val ambientEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AMBIENT_ENABLED] ?: true
    }.distinctUntilChanged()

    val ambientSoundFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AMBIENT_SOUND] ?: "lluvia"
    }

    val completedLevelsCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COMPLETED_LEVELS_COUNT] ?: 0
    }

    val goldLeavesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[GOLD_LEAVES] ?: 50
    }

    val unlockedDecorationsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[UNLOCKED_DECORATIONS] ?: setOf("estante_roble")
    }

    val unlockedAchievementsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[UNLOCKED_ACHIEVEMENTS] ?: emptySet()
    }

    val colorblindModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[COLORBLIND_MODE] ?: true
    }

    val customerLettersFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[CUSTOMER_LETTERS] ?: setOf("letter_clara")
    }

    /** Hierbas secretas que el jugador ya ha destapado en alguna puerta. */
    val secretIngredientsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[SECRET_INGREDIENTS] ?: emptySet()
    }

    val tutorialBasicDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TUTORIAL_BASIC_DONE] ?: false
    }.distinctUntilChanged()

    val tutorialRecipeDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TUTORIAL_RECIPE_DONE] ?: false
    }.distinctUntilChanged()

    val undoTokensFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[UNDO_TOKENS] ?: 3
    }

    val unlockedAsmrPacksFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[UNLOCKED_ASMR_PACKS] ?: setOf("asmr_cristal")
    }

    /** Capa de ambiente ASMR durante las partidas (la del pack que se posea). */
    val asmrInGameEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ASMR_IN_GAME_ENABLED] ?: true
    }

    /** Música de fondo durante las partidas (independiente de la del menú). */
    val musicInGameEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[MUSIC_IN_GAME_ENABLED] ?: true
    }

    /**
     * Ambiente que aporta cada pack ASMR comprado. "asmr_cristal" es el pack
     * inicial (gratuito) y NO aporta capa: es un efecto de selección, así que si
     * el jugador lo elige se queda en silencio en vez de sonar otra cosa.
     *
     * Si el jugador ha elegido un pack concreto se respeta su elección; si no,
     * se prioriza el que pueda sonar.
     */
    fun asmrAmbientFor(unlockedPacks: Set<String>, selectedPack: String? = null): String? {
        val ambientPacks = mapOf(
            "asmr_chimenea" to "lumbre",
            "asmr_lluvia" to "lluvia",
            "asmr_arroyo" to "arroyo",
            "asmr_bosque" to "bosque",
            "asmr_noche" to "noche"
        )
        val ownedAmbient = ambientPacks.filterKeys { unlockedPacks.contains(it) }
        // Cristal (u otro pack sin ambiente) elegido explícitamente = silencio.
        if (selectedPack != null && !ownedAmbient.containsKey(selectedPack)) return null
        val chosen = selectedPack ?: ownedAmbient.keys.firstOrNull()
        return ambientPacks[chosen]
    }

    /** Pack ASMR elegido por el jugador para escuchar durante la partida. */
    val selectedAsmrPackFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_ASMR_PACK]
    }

    suspend fun setSelectedAsmrPack(packId: String?) {
        context.dataStore.edit { prefs ->
            if (packId == null) prefs.remove(SELECTED_ASMR_PACK) else prefs[SELECTED_ASMR_PACK] = packId
        }
    }

    suspend fun consumeUndoToken(): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val current = prefs[UNDO_TOKENS] ?: 3
            if (current > 0) {
                prefs[UNDO_TOKENS] = current - 1
                success = true
            }
        }
        return success
    }

    suspend fun buyUndoTokens(amount: Int = 3, cost: Int = 15): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val leaves = prefs[GOLD_LEAVES] ?: 50
            if (leaves >= cost) {
                prefs[GOLD_LEAVES] = leaves - cost
                val current = prefs[UNDO_TOKENS] ?: 3
                prefs[UNDO_TOKENS] = current + amount
                success = true
            }
        }
        return success
    }

    /**
     * Gasta un uso de la ayuda. Devuelve false si ya no quedaba ninguno, que es
     * lo que hace que la partida abra la ventana de aviso.
     */
    suspend fun consumeHelpStock(helpId: String): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val key = helpStockKey(helpId)
            val current = prefs[key] ?: (STARTING_HELP_STOCK[helpId] ?: 0)
            if (current > 0) {
                prefs[key] = current - 1
                success = true
            }
        }
        return success
    }

    /** Suma usos a una ayuda, pagados con Hojas de Oro. */
    suspend fun grantHelpStock(helpId: String, amount: Int, cost: Int): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val leaves = prefs[GOLD_LEAVES] ?: 50
            if (leaves >= cost) {
                prefs[GOLD_LEAVES] = leaves - cost
                val key = helpStockKey(helpId)
                prefs[key] = (prefs[key] ?: (STARTING_HELP_STOCK[helpId] ?: 0)) + amount
                success = true
            }
        }
        return success
    }

    suspend fun unlockAsmrPack(packId: String, cost: Int = 40): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val leaves = prefs[GOLD_LEAVES] ?: 50
            if (leaves >= cost) {
                prefs[GOLD_LEAVES] = leaves - cost
                val set = prefs[UNLOCKED_ASMR_PACKS] ?: setOf("asmr_cristal")
                prefs[UNLOCKED_ASMR_PACKS] = set + packId
                success = true
            }
        }
        return success
    }

    suspend fun setTutorialBasicDone() {
        context.dataStore.edit { prefs -> prefs[TUTORIAL_BASIC_DONE] = true }
    }

    suspend fun setTutorialRecipeDone() {
        context.dataStore.edit { prefs -> prefs[TUTORIAL_RECIPE_DONE] = true }
    }

    suspend fun setAmbientSound(sound: String) {
        context.dataStore.edit { prefs ->
            prefs[AMBIENT_SOUND] = sound
        }
    }

    suspend fun unlockCustomerLetter(letterId: String) {
        context.dataStore.edit { prefs ->
            val set = prefs[CUSTOMER_LETTERS] ?: setOf("letter_clara")
            prefs[CUSTOMER_LETTERS] = set + letterId
        }
    }

    /** Apunta una hierba secreta encontrada (una sola vez: es un conjunto). */
    suspend fun unlockSecretIngredient(herbId: String) {
        context.dataStore.edit { prefs ->
            val set = prefs[SECRET_INGREDIENTS] ?: emptySet()
            prefs[SECRET_INGREDIENTS] = set + herbId
        }
    }

    suspend fun updateCurrentLevel(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[CURRENT_LEVEL] = level
        }
    }

    suspend fun incrementCompletedLevels() {
        context.dataStore.edit { prefs ->
            val count = prefs[COMPLETED_LEVELS_COUNT] ?: 0
            prefs[COMPLETED_LEVELS_COUNT] = count + 1
        }
    }

    suspend fun addGoldLeaves(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[GOLD_LEAVES] ?: 50
            prefs[GOLD_LEAVES] = current + amount
        }
    }

    suspend fun unlockDecoration(decorationId: String, price: Int) {
        context.dataStore.edit { prefs ->
            val currentLeaves = prefs[GOLD_LEAVES] ?: 50
            if (currentLeaves >= price) {
                prefs[GOLD_LEAVES] = currentLeaves - price
                val set = prefs[UNLOCKED_DECORATIONS] ?: setOf("estante_roble")
                prefs[UNLOCKED_DECORATIONS] = set + decorationId
            }
        }
    }

    suspend fun unlockAchievement(achievementId: String, reward: Int) {
        context.dataStore.edit { prefs ->
            val currentAchievements = prefs[UNLOCKED_ACHIEVEMENTS] ?: emptySet()
            if (achievementId !in currentAchievements) {
                prefs[UNLOCKED_ACHIEVEMENTS] = currentAchievements + achievementId
                val currentLeaves = prefs[GOLD_LEAVES] ?: 50
                prefs[GOLD_LEAVES] = currentLeaves + reward
            }
        }
    }

    suspend fun setColorblindMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[COLORBLIND_MODE] = enabled
        }
    }

    suspend fun consumeStamina(): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val current = prefs[STAMINA_COUNT] ?: MAX_STAMINA
            if (current > 0) {
                prefs[STAMINA_COUNT] = current - 1
                success = true
            } else {
                prefs[STAMINA_COUNT] = MAX_STAMINA - 1
                success = true
            }
        }
        return success
    }

    suspend fun addStamina(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[STAMINA_COUNT] ?: MAX_STAMINA
            prefs[STAMINA_COUNT] = minOf(MAX_STAMINA, current + amount)
        }
    }

    suspend fun unlockHerb(herbId: String) {
        context.dataStore.edit { prefs ->
            val currentSet = prefs[UNLOCKED_HERBS] ?: setOf("chamomile", "lavender", "mint")
            prefs[UNLOCKED_HERBS] = currentSet + herbId
        }
    }

    suspend fun setAdsRemoved(removed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ADS_REMOVED] = removed
        }
    }

    suspend fun setThemePackUnlocked(unlocked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[THEME_PACK_UNLOCKED] = unlocked
        }
    }

    suspend fun setSelectedTheme(themeId: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_THEME] = themeId
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SOUND_ENABLED] = enabled
        }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[MUSIC_ENABLED] = enabled
        }
    }

    suspend fun setAmbientEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AMBIENT_ENABLED] = enabled
        }
    }

    suspend fun setMusicInGameEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[MUSIC_IN_GAME_ENABLED] = enabled
        }
    }

    suspend fun setAsmrInGameEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ASMR_IN_GAME_ENABLED] = enabled
        }
    }
}
