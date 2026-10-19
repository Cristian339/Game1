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
    val sectorRule: SectorRule = SectorRule.NONE,
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
            // Mismas formulas que LevelRepository.getLevel: un frasco vacio de
            // almacen, y con receta uno mas que es la tetera (ya va en la linea
            // de mecanicas como "Tetera").
            val numHerbTypes = minOf(3 + (numero / 5), 6)
            // El generador parte de las primeras [numHerbTypes] hierbas y anade
            // las de la receta que falten: el tomillo del nivel 50 no esta entre
            // las seis primeras y ocupa un frasco entero mas.
            val hierbasBase = (Herb.delReparto.take(numHerbTypes) + order?.requiredHerbs.orEmpty())
                .distinct().size
            // Las puertas esconden ademas su hierba secreta en un frasco, tapada: al
            // jugador le aparece una hierba mas en la partida.
            herbTypes = hierbasBase + (if (LevelRepository.isGateLevel(numero)) 1 else 0)
            // Pero la secreta no trae un frasco mas: sustituye una capa, asi que el
            // tablero trae exactamente las mismas capas (y los mismos frascos) que
            // tendria sin ella. Contarla aqui ponia un frasco de mas en las puertas.
            //
            // El frasco vacio de almacen es uno; con receta el generador anade uno
            // mas, que es donde va la tetera. Y en los sectores con molinillo hay
            // otro frasco mas para el. La puerta que suma hierbas de receta a las
            // seis de siempre (la 40 y la 50) lleva un segundo vacio: es la misma
            // regla que LevelRepository.getLevel.
            val vacios = if (hierbasBase > 6) 2 else 1
            jarCount = hierbasBase + vacios +
                (if (order != null) 1 else 0) +
                (if (SectorRule.forLevel(numero).traeMolinillo) 1 else 0)
            // A partir del 3 el generador ya empieza a esconder capas, y del 5 en
            // adelante congela alguna. Sellados no hay: el generador no sella
            // frascos porque con el reparto en rueda el nivel quedaria imposible.
            hasMystery = numero >= 3
            hasFrozen = numero >= 5
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
            exactCounts = predefined != null
        )
    }
}

/** Quita los marcadores de capa (misterio, congelado, sellado) de un id de hierba. */
private fun String.sinPrefijos(): String = removePrefix("sealed:")
    .removePrefix("frozen:")
    .removePrefix("?")
    .removePrefix("mystery:")