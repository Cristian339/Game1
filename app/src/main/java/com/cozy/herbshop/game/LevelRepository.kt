package com.cozy.herbshop.game

import android.content.Context
import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.lang.reflect.Type
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

        /**
         * Semilla del reto diario: el dia del ano. Fija el tablero del dia, su
         * receta y el id de su premio ("daily_<semilla>"), asi que tambien es
         * lo que distingue el reto de hoy del de ayer.
         */
        fun dailySeed(): Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        /**
         * El reto de un dia no se repite al siguiente: solo vuelve con otro dia
         * del ano. `doneDay` es la semilla apuntada al ganar (0 = nunca jugado;
         * los dias del ano empiezan en 1).
         */
        fun dailyAlreadyPlayed(doneDay: Int, today: Int = dailySeed()): Boolean = doneDay == today

        /**
         * Gson que si pasa por el constructor de [GameLevel].
         *
         * Gson crea los objetos sin pasar por el constructor y solo pisa los
         * campos que vienen en el json: los que no aparecen (`sectorRule`,
         * `grinderJarIndex`, `parMoves`...) se quedaban en null aunque el data
         * class declare valores por defecto. Como levels.json no trae esas
         * claves, el primer `copy()` de un nivel fijo reventaba con
         * NullPointerException: pulsar JUGAR en el nivel 1 cerraba la app.
         * Con un InstanceCreator el constructor corre, asi que el json solo
         * pisa lo que de verdad trae.
         */
        fun gsonDeNiveles(): Gson = GsonBuilder()
            .registerTypeAdapter(
                GameLevel::class.java,
                object : InstanceCreator<GameLevel> {
                    override fun createInstance(type: Type): GameLevel =
                        GameLevel(levelNumber = 0, jars = emptyList())
                }
            )
            .create()
    }

    private val gson = gsonDeNiveles()
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
        // Un frasco vacio de almacen, mas la tetera y el molinillo cuando tocan
        // (que son herramientas, no almacen). Antes se dejaban tres o cinco
        // "para tener maniobra" y el jugador veia la partida llena de frascos
        // esperando: pidio llenarlos y dejar uno. La puerta que suma a las seis
        // especies de siempre las de su receta (la 40 y la 50) lleva un segundo
        // vacio: con siete u ocho colores y un solo hueco el solucionador no da
        // por bueno ningun tablero sin quitarle a la puerta el misterio y los
        // topes de su sector, y una puerta sin su novedad no es una puerta. El
        // generador valida cada reparto con el solver, asi que el nivel sigue
        // siendo terminable desde la posicion en la que empieza el jugador.
        val hierbasDelNivel = (Herb.delReparto.take(numHerbs) + requiredOrder.orEmpty()).distinct()
        val emptyJars = if (hierbasDelNivel.size > 6) 2 else 1

        val generated = levelGenerator.generateSolvableLevel(
            levelNumber = levelNumber,
            numHerbTypes = numHerbs,
            emptyJars = emptyJars,
            jarCapacity = jarCapacity,
            // Semilla por nivel: el mismo nivel reparte el mismo tablero, y el mapa
            // (que promete cuentas) y los tests pueden contar con ello. Sin esto el
            // generador sorteaba en cada partida y un nivel podia caer al tablero
            // de reserva en una partida y no en la siguiente. El reto diario ya
            // tenia su propia semilla por dia.
            random = Random(levelNumber * 7919L + 13L),
            requiredOrder = requiredOrder
        )
        return generated.copy(customerOrder = order)
    }

    fun getZenLevel(): GameLevel {
        val level = levelGenerator.generateSolvableLevel(
            levelNumber = 1,
            numHerbTypes = 3,
            emptyJars = 1,
            jarCapacity = 4,
            random = Random.Default
        )
        return level.copy(
            levelNumber = 0,
            isZenMode = true
        )
    }

    /**
     * Reto del dia. La semilla entra como parametro para que la partida use
     * siempre la misma de la jornada (y la apunte al ganar) y los tests puedan
     * fijar un dia concreto.
     */
    fun getDailyChallengeLevel(seed: Int): GameLevel {
        val random = Random(seed)

        val dailyOrder = CustomerOrder(
            id = "daily_$seed",
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
            emptyJars = 1,
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
        // El molinillo se descarta aqui: tambien es un frasco vacio, y si no se
        // excluyera la tetera acabaria siendo el molinillo.
        val grinderIndex = level.grinderJarIndex?.takeIf { it in rawJars.indices }
        var teapotJarIndex = -1
        if (order != null) {
            teapotJarIndex = rawJars.indices.lastOrNull {
                rawJars[it].isEmpty() && it != grinderIndex
            } ?: -1
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
            val isGrinder = (index == grinderIndex)
            Jar(
                id = index,
                // El molinillo guarda una sola hierba: es una herramienta, no un
                // frasco de almacen.
                capacity = when {
                    isGrinder -> 1
                    isTeapot -> teapotCapacity
                    else -> level.jarCapacity
                },
                layers = layers,
                isSealed = jarIsSealed,
                isTeapot = isTeapot,
                isGrinder = isGrinder
            ).revealTopLayer().unfreezeTopLayer()
        }
    }
}
