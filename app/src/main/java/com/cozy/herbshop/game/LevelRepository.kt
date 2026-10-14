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
 * la siguiente hierba. Cae el ultimo nivel de cada sector, asi que en el
 * mapa se ve que el camino avanza y con que te llevas a cambio.
 */
val GATE_LEVELS = setOf(10, 20, 30, 40, 50)

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
        // Con receta el nivel se genera y valida con la tetera incluida, para que
        // el pedido siempre sea preparable (+50 🍃 y puerta de hierba).
        val requiredOrder = order?.requiredHerbs

        val predefined = loadPredefinedLevels()
        val found = predefined.find { it.levelNumber == levelNumber }
        if (found != null) {
            return found.copy(customerOrder = order)
        }

        val numHerbs = minOf(3 + (levelNumber / 5), 6)
        val jarCapacity = 4
        // Tres frascos vacios y no dos. Con el reparto disperso (cuatro hierbas
        // distintas en cada frasco) dos vacios dejan el nivel justo en el limite
        // de lo que se puede comprobar, y el generador caia al nivel de reserva
        // muy a menudo. Con tres, casi todo sale verificable y sigue siendo
        // puzle de verdad.
        // A partir de cuatro colores, tres frascos vacios se quedan cortos para
        // ordenar: el reparto no encontraba una solucion y el nivel caia al de
        // reserva. Con cuatro si sale siempre.
        val emptyJars = if (numHerbs >= 4) 4 else 3

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

        val dailyOrder = CustomerOrder(
            id = "daily_$todaySeed",
            customerName = "Madame Flora",
            dialogue = "¡Hola querido! Preparo el Té Especial del Día.",
            recipeName = "Infusión del Día Royale",
            requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.LAVENDER, Herb.LAVENDER)
        )

        // levelNumber 4 es solo un identificador de navegación; la dificultad
        // real se genera con 4 (misterio + sellado, sin congelados rotos).
        val level = levelGenerator.generateSolvableLevel(
            levelNumber = 4,
            numHerbTypes = 4,
            emptyJars = 2,
            jarCapacity = 4,
            random = random,
            requiredOrder = dailyOrder.requiredHerbs
        ).copy(levelNumber = 999)

        return level.copy(
            customerOrder = dailyOrder,
            isDailyChallenge = true
        )
    }

    /**
     * Receta del cliente en un nivel, sin llegar a generar el nivel. El mapa la
     * usa para enseñar qué hay que servir sin pagar una búsqueda del solver.
     */
    fun orderForLevel(levelNumber: Int): CustomerOrder? = createCustomerOrderForLevel(levelNumber)

    private fun createCustomerOrderForLevel(levelNumber: Int): CustomerOrder? {
        // Niveles de aprendizaje: sin receta
        if (levelNumber in 1..3) return null

        // Puertas de hierba: receta siempre presente, con hierbas disponibles en el nivel.
        // El nivel cierra su sector, asi que cada una es el broche de un tramo.
        if (isGateLevel(levelNumber)) {
            return when (levelNumber) {
                10 -> CustomerOrder(
                    id = "gate_10",
                    customerName = "Vendedora Rosa",
                    dialogue = "¡Prepara manzanilla con romero y te daré romero para tu tienda!",
                    recipeName = "Infusión del Caminante",
                    requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.ROSEMARY, Herb.ROSEMARY)
                )
                20 -> CustomerOrder(
                    id = "gate_20",
                    customerName = "Mercader Zafiro",
                    dialogue = "Trae un té de hibisco y te revelaré mi cofre de hibisco.",
                    recipeName = "Té del Trópico Rojo",
                    requiredHerbs = listOf(Herb.HIBISCUS, Herb.HIBISCUS, Herb.ROSEMARY, Herb.ROSEMARY)
                )
                30 -> CustomerOrder(
                    id = "gate_30",
                    customerName = "Sabia Aurelia",
                    dialogue = "La canela solo se gana con paciencia… y con este té.",
                    recipeName = "Elixir de Canela Antigua",
                    requiredHerbs = listOf(Herb.CINNAMON, Herb.CINNAMON, Herb.HIBISCUS, Herb.HIBISCUS)
                )
                40 -> CustomerOrder(
                    id = "gate_40",
                    customerName = "Jardinera Luna",
                    dialogue = "Mi caléndula es para quien domine la mezcla dorada.",
                    recipeName = "Sérum de Caléndula",
                    requiredHerbs = listOf(Herb.CALENDULA, Herb.CALENDULA, Herb.MINT, Herb.MINT)
                )
                else -> CustomerOrder(
                    id = "gate_50",
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
        val order = level.customerOrder
        val rawJars = level.jars.toMutableList()

        // La tetera es obligatoria en todo nivel con receta: si el nivel no trae
        // ningún frasco vacío se añade uno (si no, la mezcla sería imposible).
        var teapotJarIndex = -1
        if (order != null) {
            teapotJarIndex = rawJars.indexOfLast { it.isEmpty() }
            if (teapotJarIndex < 0) {
                rawJars.add(emptyList())
                teapotJarIndex = rawJars.lastIndex
            }
        }

        val teapotCapacity = order?.requiredHerbs?.size?.coerceAtLeast(1) ?: level.jarCapacity

        return rawJars.mapIndexed { index, herbIds ->
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
                val isSticky = cleanId.startsWith("sticky:")
                if (isSticky) cleanId = cleanId.removePrefix("sticky:")
                val isHidden = cleanId.startsWith("?") || cleanId.startsWith("mystery:")
                val herbId = cleanId.removePrefix("?").removePrefix("mystery:")
                JarLayer(
                    herb = Herb.fromId(herbId),
                    isHidden = isHidden,
                    isFrozen = isFrozen,
                    isSticky = isSticky
                )
            }
            val isTeapot = (index == teapotJarIndex)
            Jar(
                id = index,
                capacity = if (isTeapot) teapotCapacity else level.jarCapacity,
                layers = layers,
                isSealed = jarIsSealed,
                isTeapot = isTeapot
            ).revealTopLayer().unfreezeTopLayer()
        }
    }
}
