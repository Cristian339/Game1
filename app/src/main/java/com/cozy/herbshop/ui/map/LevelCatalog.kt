package com.cozy.herbshop.ui.map

import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.SectorRule
import com.cozy.herbshop.game.HerbUnlocks
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.game.Mechanic
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
    val sectorRule: SectorRule = SectorRule.NONE,
    /**
     * Mecanica que el nivel estrena: la ficha la anuncia para que el jugador
     * sepa que ahi le van a explicar algo nuevo.
     */
    val newMechanic: Mechanic? = null,
    /** Si las cuentas de la ficha salen del nivel tal cual (niveles fijos). */
    val exactCounts: Boolean = false
) {
    /** Linea corta de mecanicas: "5 frascos · 3 hierbas · Tetera". */
    val mechanics: String
        get() {
            // Los frascos y las hierbas son el tope del reparto normal. Cuando el
            // generador no encuentra tablero ganable entrega el de reserva, que
            // trae menos hierbas, asi que en los niveles generados la ficha no
            // puede prometer la cuenta exacta; en los fijos (levels.json) si.
            val parts = mutableListOf<String>()
            if (exactCounts) {
                parts.add("$jarCount frascos")
                parts.add("$herbTypes hierbas")
            } else {
                parts.add("hasta $jarCount frascos")
                parts.add("hasta $herbTypes hierbas")
            }
            // La novedad del sector va primero: es lo que cambia como se juega.
            parts.addAll(sectorRule.novedades)
            if (order != null) parts.add("Tetera")
            if (hasMystery) parts.add("Misterio")
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
            // Mismas formulas que LevelRepository: hierbas, huecos, tetera y
            // molinillo salen de la misma tabla que usa el generador.
            val hierbasBase = (Herb.delReparto.take(repository.herbTypesForLevel(numero)) +
                order?.requiredHerbs.orEmpty()).distinct().size
            // Las puertas esconden ademas su hierba secreta en un frasco, tapada: al
            // jugador le aparece una hierba mas en la partida.
            herbTypes = hierbasBase + (if (LevelRepository.isGateLevel(numero)) 1 else 0)
            jarCount = repository.jarCountForLevel(numero)
            // El generador garantiza una capa misteriosa desde el nivel 3 y
            // frascos helados en los sectores que los anuncian.
            hasMystery = numero >= 3
            hasFrozen = SectorRule.forLevel(numero).traeHelados
            hasSealed = false
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
            // La regla del sector sale de la tabla unica, la misma que lee el
            // generador: el mapa no puede anunciar algo que la partida no traiga.
            sectorRule = SectorRule.forLevel(numero),
            // Y el estreno de mecanica, de la tabla de Mechanic: el nivel que la
            // trae por primera vez es el que da el tutorial.
            newMechanic = Mechanic.entries.firstOrNull { it.introLevel == numero },
            exactCounts = predefined != null
        )
    }
}

/** Quita los marcadores de capa (misterio, congelado, sellado) de un id de hierba. */
private fun String.sinPrefijos(): String = removePrefix("sealed:")
    .removePrefix("frozen:")
    .removePrefix("?")
    .removePrefix("mystery:")