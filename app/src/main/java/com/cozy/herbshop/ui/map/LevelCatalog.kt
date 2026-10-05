package com.cozy.herbshop.ui.map

import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.SectorRule
import com.cozy.herbshop.game.HerbUnlocks
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.ui.components.GameScene

/**
 * Resumen de un nivel para el mapa.
 *
 * Solo muestra lo que es estable entre partidas: el escenario, si es puerta de
 * hierba, la receta y cuantas hierbas/jars tiene. El nivel se genera al azar en
 * cada partida, asi que contar los frascos de una muestra seria mentir: en vez
 * de eso se derivan de las mismas formulas que usa el generador, que si son
 * ciertas para cualquier partida.
 */
data class LevelInfo(
    val number: Int,
    val scene: GameScene,
    val subtitle: String,
    val order: CustomerOrder?,
    val jarCount: Int,
    val herbTypes: Int,
    val isGate: Boolean,
    val unlockedHerb: Herb?,
    val hasMystery: Boolean,
    val hasFrozen: Boolean,
    val hasSealed: Boolean,
    /** Novedad que trae el sector del nivel. */
    val sectorRule: SectorRule = SectorRule.NONE
) {
    /** Linea corta de mecanicas: "5 frascos · 3 hierbas · Tetera". */
    val mechanics: String
        get() {
            val parts = mutableListOf("$jarCount frascos", "$herbTypes hierbas")
            if (order != null) parts.add("Tetera")
            if (hasMystery) parts.add("Misterio")
            if (hasFrozen) parts.add("Congeladas")
            if (hasSealed) parts.add("Sellado")
            return parts.joinToString(" · ")
        }

    val recipe: List<Herb> get() = order?.requiredHerbs.orEmpty()
}

/**
 * La hierba que cada puerta desbloquea. Las puertas caen al final de cada
 * sector, así que el orden es el del camino: primero romero, después hibisco,
 * y así hasta completar el herbario.
 *
 * Se lee de [HerbUnlocks], que es la regla que aplica la partida al ganar, para
 * que el mapa no pueda anunciar una hierba distinta de la que llega de verdad.
 */
private val PUERTA_HERBA = HerbUnlocks.herbByGate(LevelRepository.GATE_LEVELS)

/**
 * Fichas de los niveles.
 *
 * No genera ningun nivel: eso cuesta una busqueda del solver y, al pedir las
 * cincuenta, tumbaba la pantalla. La ficha se deduce de las mismas reglas que
 * usa [LevelRepository] para crearlos, de modo que es rapida y sigue siendo
 * cierta para cualquier partida.
 *
 * Los niveles que vienen fijos de assets/levels.json se cuentan desde el json,
 * que es la unica fuente de verdad para ellos.
 */
class LevelCatalog(
    private val repository: LevelRepository,
    private val totalLevels: Int = 50,
    predefinedLevels: List<GameLevel>? = null
) {
    private val cache = mutableMapOf<Int, LevelInfo>()
    private val predefinedLevels = predefinedLevels ?: repository.loadPredefinedLevels()

    operator fun get(level: Int): LevelInfo = cache.getOrPut(level) { buildInfo(level) }

    fun all(): List<LevelInfo> = (1..totalLevels).map { this[it] }

    private fun buildInfo(numero: Int): LevelInfo {
        val order = repository.orderForLevel(numero)
        val sector = GameScene.sectorOf(numero)

        // Los primeros niveles son fijos (assets/levels.json) y no salen del
        // generador, asi que se cuentan desde el propio json: con las formulas de
        // mas abajo el mapa diria 5 frascos donde hay 3.
        val predefined = predefinedLevels.firstOrNull { it.levelNumber == numero }

        val herbTypes: Int
        val jarCount: Int
        val hasMystery: Boolean
        val hasFrozen: Boolean
        val hasSealed: Boolean

        if (predefined != null) {
            val capas = predefined.jars.flatten()
            val ids = capas.map { it.sinPrefijos() }
            herbTypes = ids.toSet().size
            // La tetera reutiliza el ultimo frasco vacio; solo se anade uno si el
            // nivel no trae ninguno, que es lo que hace LevelRepository.
            jarCount = predefined.jars.size +
                if (order != null && predefined.jars.none { it.isEmpty() }) 1 else 0
            hasMystery = capas.any { it.startsWith("?") || it.startsWith("mystery:") }
            hasFrozen = capas.any { it.startsWith("frozen:") }
            hasSealed = capas.any { it.startsWith("sealed:") }
        } else {
            // Mismas formulas que LevelRepository.getLevel: las **[jarCount]** vacios son los
            // que deja el generador, y con receta uno de ellos es la tetera (ya
            // va en la linea de mecanicas como "Tetera").
            herbTypes = minOf(3 + (numero / 5), 6)
            // Con receta el generador anade un frasco mas, que es donde va la
            // tetera; sin ella la tetera reutiliza una de las vacias.
            jarCount = herbTypes + (if (herbTypes >= 4) 4 else 3) + (if (order != null) 1 else 0)
            // A partir del 3 el generador ya empieza a esconder capas, y del 5 en
            // adelante congela y sella un frasco.
            hasMystery = numero >= 3
            hasFrozen = numero >= 5
            hasSealed = numero >= 4
        }

        return LevelInfo(
            number = numero,
            scene = GameScene.forLevel(numero),
            subtitle = sector.name,
            order = order,
            jarCount = jarCount,
            herbTypes = herbTypes,
            isGate = LevelRepository.isGateLevel(numero),
            unlockedHerb = PUERTA_HERBA[numero],
            hasMystery = hasMystery,
            hasFrozen = hasFrozen,
            hasSealed = hasSealed,
            // La regla del sector se deduce con la misma cuenta que usa el
            // generador, para que el mapa y la partida no se contradigan.
            sectorRule = when {
                numero in 1..10 -> SectorRule.NONE
                numero in 11..20 -> SectorRule.GRINDER
                numero in 21..30 -> SectorRule.MOVE_LIMIT
                numero in 31..40 -> SectorRule.STICKY
                else -> SectorRule.ALL
            }
        )
    }
}

/** Quita los marcadores de capa (misterio, congelado, sellado) de un id de hierba. */
private fun String.sinPrefijos(): String = removePrefix("sealed:")
    .removePrefix("frozen:")
    .removePrefix("?")
    .removePrefix("mystery:")