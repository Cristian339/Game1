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
    companion object {
        /**
         * Niveles puerta: la receta es obligatoria y al cumplirla se desbloquea
         * la siguiente hierba. Intervalos crecientes para alargar la campaña.
         */
        val GATE_LEVELS = setOf(4, 9, 15, 21, 28)

        fun isGateLevel(levelNumber: Int): Boolean = levelNumber in GATE_LEVELS
    }

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
        val order = createCustomerOrderForLevel(levelNumber)
        val requiredOrder = if (isGateLevel(levelNumber)) order?.requiredHerbs else null

        val predefined = loadPredefinedLevels()
        val found = predefined.find { it.levelNumber == levelNumber }
        if (found != null) {
            return found.copy(customerOrder = order)
        }

        val numHerbs = minOf(3 + (levelNumber / 3), Herb.entries.size)
        val jarCapacity = 4
        val emptyJars = 2

        val generated = levelGenerator.generateSolvableLevel(
            levelNumber = levelNumber,
            numHerbTypes = numHerbs,
            emptyJars = emptyJars,
            jarCapacity = jarCapacity,
            requiredOrder = requiredOrder
        )
        return generated.copy(customerOrder = order)
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
        // Niveles de aprendizaje: sin receta
        if (levelNumber in 1..3) return null

        // Puertas de hierba: receta siempre presente, con hierbas disponibles en el nivel
        if (isGateLevel(levelNumber)) {
            return when (levelNumber) {
                4 -> CustomerOrder(
                    id = "gate_4",
                    customerName = "Vendedora Rosa",
                    dialogue = "¡Prepara manzanilla con romero y te daré romero para tu tienda!",
                    recipeName = "Infusión del Caminante",
                    requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.ROSEMARY, Herb.ROSEMARY)
                )
                9 -> CustomerOrder(
                    id = "gate_9",
                    customerName = "Mercader Zafiro",
                    dialogue = "Trae un té de hibisco y te revelaré mi cofre de hibisco.",
                    recipeName = "Té del Trópico Rojo",
                    requiredHerbs = listOf(Herb.HIBISCUS, Herb.HIBISCUS, Herb.ROSEMARY, Herb.ROSEMARY)
                )
                15 -> CustomerOrder(
                    id = "gate_15",
                    customerName = "Sabia Aurelia",
                    dialogue = "La canela solo se gana con paciencia… y con este té.",
                    recipeName = "Elixir de Canela Antigua",
                    requiredHerbs = listOf(Herb.CINNAMON, Herb.CINNAMON, Herb.HIBISCUS, Herb.HIBISCUS)
                )
                21 -> CustomerOrder(
                    id = "gate_21",
                    customerName = "Jardinera Luna",
                    dialogue = "Mi caléndula es para quien domine la mezcla dorada.",
                    recipeName = "Sérum de Caléndula",
                    requiredHerbs = listOf(Herb.CALENDULA, Herb.CALENDULA, Herb.MINT, Herb.MINT)
                )
                else -> CustomerOrder(
                    id = "gate_28",
                    customerName = "Ermitaño Verde",
                    dialogue = "El tomillo silvestre premia al último maestro té.",
                    recipeName = "Bálsamo del Tomillo Silvestre",
                    requiredHerbs = listOf(Herb.THYME, Herb.THYME, Herb.ROSEMARY, Herb.ROSEMARY)
                )
            }
        }

        // Entre puertas: receta opcional en los niveles pares (a veces sí, a veces no)
        if (levelNumber % 2 != 0) return null

        return when (levelNumber % 4) {
            0 -> CustomerOrder(
                id = "order_$levelNumber",
                customerName = "Abuela Clara",
                dialogue = "Busco un té suave para descansar esta tarde...",
                recipeName = "Infusión Dulce Calma",
                requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.LAVENDER, Herb.LAVENDER)
            )
            2 -> CustomerOrder(
                id = "order_$levelNumber",
                customerName = "Botánico Arthur",
                dialogue = "Necesito una mezcla estimulante para estudiar.",
                recipeName = "Elixir de Concentración",
                requiredHerbs = listOf(Herb.MINT, Herb.MINT, Herb.LAVENDER, Herb.LAVENDER)
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
