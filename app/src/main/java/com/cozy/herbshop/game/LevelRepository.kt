package com.cozy.herbshop.game

import android.content.Context
import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.util.Calendar
import kotlin.random.Random

class LevelRepository(
    private val context: Context?,
    private val levelGenerator: LevelGenerator = LevelGenerator()
) {
    private val gson = Gson()
    private var jsonLevelsCache: List<GameLevel>? = null

    fun loadPredefinedLevels(): List<GameLevel> {
        if (jsonLevelsCache != null) return jsonLevelsCache!!
        if (context == null) return emptyList()

        return try {
            context.assets.open("levels.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<GameLevel>>() {}.type
                    val levels: List<GameLevel> = gson.fromJson(reader, type)
                    jsonLevelsCache = levels
                    levels
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLevel(levelNumber: Int): GameLevel {
        val predefined = loadPredefinedLevels()
        val found = predefined.find { it.levelNumber == levelNumber }
        if (found != null) {
            val order = createCustomerOrderForLevel(levelNumber)
            return found.copy(customerOrder = order)
        }

        val numHerbs = minOf(3 + (levelNumber / 3), Herb.entries.size)
        val jarCapacity = 4
        val emptyJars = 2

        val generated = levelGenerator.generateSolvableLevel(
            levelNumber = levelNumber,
            numHerbTypes = numHerbs,
            emptyJars = emptyJars,
            jarCapacity = jarCapacity
        )
        return generated.copy(customerOrder = createCustomerOrderForLevel(levelNumber))
    }

    fun getZenLevel(): GameLevel {
        val level = levelGenerator.generateSolvableLevel(
            levelNumber = 1,
            numHerbTypes = 3,
            emptyJars = 2,
            jarCapacity = 4,
            random = Random.Default
        )
        return level.copy(
            levelNumber = 0,
            isZenMode = true
        )
    }

    fun getDailyChallengeLevel(): GameLevel {
        val todaySeed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val random = Random(todaySeed)

        // levelNumber 999 es solo un identificador de navegación; la dificultad
        // real se genera con 4 (misterio + sellado, sin congelados rotos).
        val level = levelGenerator.generateSolvableLevel(
            levelNumber = 4,
            numHerbTypes = 4,
            emptyJars = 2,
            jarCapacity = 4,
            random = random
        ).copy(levelNumber = 999)

        val dailyOrder = CustomerOrder(
            id = "daily_$todaySeed",
            customerName = "Madame Flora",
            dialogue = "¡Hola querido! Preparo el Té Especial del Día.",
            recipeName = "Infusión del Día Royale",
            requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.LAVENDER, Herb.LAVENDER)
        )

        return level.copy(
            customerOrder = dailyOrder,
            isDailyChallenge = true
        )
    }

    private fun createCustomerOrderForLevel(levelNumber: Int): CustomerOrder? {
        return when (levelNumber % 3) {
            0 -> CustomerOrder(
                id = "order_$levelNumber",
                customerName = "Abuela Clara",
                dialogue = "Busco un té suave para descansar esta tarde...",
                recipeName = "Infusión Dulce Calma",
                requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.LAVENDER, Herb.LAVENDER)
            )
            1 -> CustomerOrder(
                id = "order_$levelNumber",
                customerName = "Botánico Arthur",
                dialogue = "Necesito una mezcla estimulante para estudiar.",
                recipeName = "Elixir de Concentración",
                requiredHerbs = listOf(Herb.MINT, Herb.MINT, Herb.ROSEMARY, Herb.ROSEMARY)
            )
            else -> null
        }
    }

    fun convertToJars(level: GameLevel): List<Jar> {
        return level.jars.mapIndexed { index, herbIds ->
            var jarIsSealed = false
            val layers = herbIds.map { rawId ->
                var cleanId = rawId
                if (cleanId.startsWith("sealed:")) {
                    jarIsSealed = true
                    cleanId = cleanId.removePrefix("sealed:")
                }
                var isFrozen = false
                if (cleanId.startsWith("frozen:")) {
                    isFrozen = true
                    cleanId = cleanId.removePrefix("frozen:")
                }
                val isHidden = cleanId.startsWith("?") || cleanId.startsWith("mystery:")
                val herbId = cleanId.removePrefix("?").removePrefix("mystery:")
                JarLayer(herb = Herb.fromId(herbId), isHidden = isHidden, isFrozen = isFrozen)
            }
            Jar(id = index, capacity = level.jarCapacity, layers = layers, isSealed = jarIsSealed).revealTopLayer()
        }
    }
}
