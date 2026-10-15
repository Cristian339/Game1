package com.cozy.herbshop.data.model

import androidx.compose.ui.graphics.Color

enum class Herb(
    val id: String,
    val displayName: String,
    val scientificName: String,
    val description: String,
    val aromaProfile: String,
    val origin: String,
    val symbol: String,
    val colorHex: String,
    val defaultColor: Color,
    /**
     * Puerta en la que se esconde esta hierba, o null si es una hierba del
     * reparto. Las secretas no entran en el reparto ni en el ciclo del molinillo:
     * van tapadas en un frasco de su puerta y cuentan como encontradas en cuanto
     * el jugador las destapa.
     */
    val gate: Int? = null
) {
    CHAMOMILE("chamomile", "Manzanilla", "Matricaria chamomilla", "Relajante e infusión dorada con suaves flores blancas.", "Dulce, floral y manzana", "Europa y Asia Central", "🌼", "#E6C200", Color(0xFFFBC02D)),
    LAVENDER("lavender", "Lavanda", "Lavandula angustifolia", "Flores violetas aromáticas para la calma profunda.", "Fresco, floral y herbáceo", "Región Mediterránea", "🪻", "#8E24AA", Color(0xFFAB47BC)),
    MINT("mint", "Menta", "Mentha piperita", "Hojas frescas de verde vibrante con fresco aroma mentolado.", "Mentolado fresco e intenso", "Europa y Medio Oriente", "🍃", "#388E3C", Color(0xFF66BB6A)),
    ROSEMARY("rosemary", "Romero", "Salvia rosmarinus", "Especia leñosa y estimulante para la concentración.", "Pino leñoso y alcanforado", "Cuenca del Mediterráneo", "🌿", "#2E7D32", Color(0xFF388E3C)),
    HIBISCUS("hibiscus", "Flor de Jamaica", "Hibiscus sabdariffa", "Pétalos rojizos intensos de sabor cítrico y refrescante.", "Cítrico, afrutado y ácido", "África Tropical", "🌺", "#C2185B", Color(0xFFE91E63)),
    CINNAMON("cinnamon", "Canela", "Cinnamomum verum", "Corteza aromática cálida con tonos terrosos y especiados.", "Cálido, especiado y dulce", "Sri Lanka", "🪵", "#8D6E63", Color(0xFFA1887F)),
    CALENDULA("calendula", "Caléndula", "Calendula officinalis", "Flores anaranjadas luminosas llena de vitalidad.", "Suave, ligeramente amargo", "Sur de Europa", "🌸", "#F57C00", Color(0xFFFF9800)),
    THYME("thyme", "Tomillo", "Thymus vulgaris", "Pequeñas hojas herbales y aromáticas de monte.", "Herbal silvestre y picante", "Mediterráneo Occidental", "🌱", "#558B2F", Color(0xFF8BC34A)),

    // Secretas: una por puerta, al final del enum porque el generador y el mapa
    // toman las primeras (las del reparto) y asi no hay que excluirlas a mano.
    DAWN_FLOWER(
        "dawnflower", "Flor de Alba", "Ipomoea matutina",
        "Abre con los primeros rayos y huele a hierba mojada.", "Fresco, verde y florido",
        "La huerta al amanecer", "🌅", "#F06292", Color(0xFFF06292), gate = 10
    ),
    CRYSTAL_ORCHID(
        "orchid", "Orquídea de Cristal", "Orchis vitrea",
        "Sus pétalos parecen vidrio empañado.", "Limpio, mineral y dulce",
        "El invernadero", "🔮", "#4DD0E1", Color(0xFF4DD0E1), gate = 20
    ),
    MIDNIGHT_JASMINE(
        "jasmine", "Jazmín de Medianoche", "Jasminum nocturnum",
        "Perfuma la tienda entera cuando cae la noche.", "Dulce, intenso y embriagador",
        "Los tejados del pueblo", "🌙", "#5C6BC0", Color(0xFF5C6BC0), gate = 30
    ),
    GLOWING_MOSS(
        "glowingmoss", "Musgo Luminoso", "Bryum lucens",
        "Brilla suave entre la hojarasca del bosque viejo.", "Húmedo, terroso y fresco",
        "El bosque", "🍄", "#C0CA33", Color(0xFFC0CA33), gate = 40
    ),
    AUTUMN_SAFFRON(
        "saffron", "Azafrán de Otoño", "Crocus autumnale",
        "Se recoge en una sola semana del año, al caer la hoja.", "Cálido, dorado y especiado",
        "Los prados de otoño", "🍁", "#D84315", Color(0xFFD84315), gate = 50
    );

    /** Si es una hierba del reparto o una de las secretas que esconden las puertas. */
    val esSecreta: Boolean get() = gate != null

    companion object {
        fun fromId(id: String): Herb {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CHAMOMILE
        }

        /** Las hierbas del reparto, sin las secretas. */
        val delReparto: List<Herb> by lazy { entries.filter { !it.esSecreta } }

        /** Las secretas que esconden las puertas, en orden de sector. */
        val secretas: List<Herb> by lazy { entries.filter { it.esSecreta } }

        /** La hierba secreta que esconde una puerta, o null si esa puerta no trae. */
        fun secretaDePuerta(gate: Int): Herb? = entries.firstOrNull { it.gate == gate }
    }

    /**
     * Lo que sale al pasar esta hierba por el molinillo: la siguiente del ciclo,
     * dando la vuelta al final. Es la unica regla de conversion del juego, y esta
     * aqui para que el motor, el solver y el generador no puedan discrepar.
     *
     * El ciclo solo da vueltas entre las hierbas del reparto: una secreta muele a
     * la primera del ciclo, que es lo que sale si se cuela en el mortero.
     */
    val molida: Herb
        get() {
            val ciclo = delReparto
            val puesto = ciclo.indexOf(this)
            return ciclo[if (puesto < 0) 0 else (puesto + 1) % ciclo.size]
        }
}

data class JarLayer(
    val herb: Herb,
    val isHidden: Boolean = false,
    val isFrozen: Boolean = false,
    /**
     * Capa pegajosa (sector del bosque): esta pegada al frasco y no se deja
     * verter por encima. Es un tope, asi que obliga a gastar una jugada de mas
     * para sacar el bloque de la vez.
     */
    val isSticky: Boolean = false
)

data class CustomerOrder(
    val id: String,
    val customerName: String,
    val dialogue: String,
    val recipeName: String,
    val requiredHerbs: List<Herb>
)

data class Jar(
    val id: Int,
    val capacity: Int = 4,
    val layers: List<JarLayer> = emptyList(),
    val isSealed: Boolean = false,
    val isTeapot: Boolean = false,
    /**
     * Molinillo: acepta cualquier hierba y la transforma en la siguiente del
     * ciclo ([Herb.molida]). No es un frasco de almacen, es una herramienta, asi
     * que para terminar el nivel tiene que quedar vacio.
     */
    val isGrinder: Boolean = false
) {
    val stack: List<Herb> get() = layers.map { it.herb }
    val isFull: Boolean get() = layers.size >= capacity
    val isEmpty: Boolean get() = layers.isEmpty()
    val topLayer: JarLayer? get() = layers.lastOrNull()

    // Solo se obtiene la hierba superior si el frasco no está sellado y la capa
    // está revelada. Las capas congeladas son un efecto visual: se descongelan al
    // aflorar y nunca bloquean un vertido (así la Pista siempre es jugable).
    val topHerb: Herb? get() = if (isSealed) null else topLayer?.takeIf { !it.isHidden }?.herb

    val topCount: Int
        get() {
            if (isEmpty || isSealed) return 0
            val top = topHerb ?: return 0
            var count = 0
            for (i in layers.indices.reversed()) {
                val layer = layers[i]
                // Una capa pegajosa que tenga algo encima hace de tope: el
                // vertido se para ahi y hay que gastarla en otra jugada.
                if (i != layers.lastIndex && layer.isSticky) break
                if (!layer.isHidden && !layer.isFrozen && layer.herb == top) count++ else break
            }
            return count
        }

    /** Si el frasco tiene alguna capa pegajosa. Lo usa el aviso del nivel. */
    val hasSticky: Boolean get() = layers.any { it.isSticky }

    val isCompleted: Boolean
        get() = if (isGrinder) isEmpty
        else isEmpty || (isFull && !isSealed && layers.all { !it.isHidden && !it.isFrozen && it.herb == layers.first().herb })

    fun unseal(): Jar = copy(isSealed = false)

    fun unfreezeTopLayer(): Jar {
        if (layers.isEmpty() || !layers.last().isFrozen) return this
        val newLayers = layers.toMutableList()
        newLayers[newLayers.lastIndex] = newLayers.last().copy(isFrozen = false)
        return copy(layers = newLayers)
    }

    /**
     * Revela automáticamente la capa superior si estaba oculta.
     */
    fun revealTopLayer(): Jar {
        if (layers.isEmpty() || !layers.last().isHidden) return this
        val newLayers = layers.toMutableList()
        newLayers[newLayers.lastIndex] = newLayers.last().copy(isHidden = false)
        return copy(layers = newLayers)
    }
}

fun Jar(id: Int, capacity: Int = 4, stack: List<Herb>, isSealed: Boolean = false): Jar {
    return Jar(
        id = id,
        capacity = capacity,
        layers = stack.map { JarLayer(herb = it, isHidden = false) },
        isSealed = isSealed
    )
}

data class GameLevel(
    val levelNumber: Int,
    val jarCapacity: Int = 4,
    val jars: List<List<String>>,
    val customerOrder: CustomerOrder? = null,
    val isDailyChallenge: Boolean = false,
    val isZenMode: Boolean = false,
    /**
     * Jugadas de la solucion que el generador comprobo al crear el nivel. Es el
     * "par": lo que cuesta el nivel bien hecho. Sale gratis porque el generador
     * ya resuelve el nivel para asegurarse de que se puede terminar.
     */
    val parMoves: Int? = null,
    /**
     * Regla especial del sector. Va en el nivel y no se deduce del numero para
     * que el mapa y la partida no puedan contradecirse.
     */
    val sectorRule: SectorRule = SectorRule.NONE,
    /**
     * Frasco que hace de molinillo, si el sector lo trae. Va explicito y no se
     * deduce (como la tetera se deducia del ultimo frasco vacio) porque los dos
     * son frascos vacios y el nivel puede traer los dos a la vez.
     */
    val grinderJarIndex: Int? = null
)

/**
 * La novedad que trae cada sector. El camino se va haciendo mas dificil porque
 * cada tramo anade una regla, no solo mas frascos.
 */
enum class SectorRule(val displayName: String) {
    NONE(""),
    GRINDER("Molinillo"),
    MOVE_LIMIT("Te despues de la cena"),
    STICKY("Capa pegajosa"),
    ALL("Todo a la vez");

    /** Si este sector trae molinillo. */
    val traeMolinillo: Boolean get() = this == GRINDER || this == ALL

    /**
     * Lo que la ficha del nivel anuncia de este sector. [displayName] vale para
     * nombrar el sector, pero "Todo a la vez" no le dice nada al jugador: en la
     * ficha van las reglas nuevas una a una.
     */
    val novedades: List<String>
        get() = when (this) {
            NONE -> emptyList()
            ALL -> listOf("Molinillo", "Capa pegajosa")
            else -> listOf(displayName)
        }

    companion object {
        /**
         * El sector de un nivel. Vive aqui, y no en el generador ni en el mapa,
         * para que los dos lean la misma tabla: antes estaba escrita dos veces y
         * el mapa podia anunciar una regla que la partida no traia.
         */
        fun forLevel(levelNumber: Int): SectorRule = when (levelNumber) {
            in 1..10 -> NONE
            in 11..20 -> GRINDER
            in 21..30 -> MOVE_LIMIT
            in 31..40 -> STICKY
            else -> ALL
        }
    }
}

sealed class PourResult {
    data class Success(val countPoured: Int) : PourResult()
    data class Invalid(val reason: String) : PourResult()
}

data class TeaRecipe(
    val id: String,
    val name: String,
    val description: String,
    val ingredients: List<Herb>,
    val benefits: String,
    val requiredHerbIds: Set<String>
)

data class DecorationItem(
    val id: String,
    val name: String,
    val category: String,
    val priceInLeaves: Int,
    val iconEmoji: String,
    val description: String
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val targetCount: Int,
    val rewardLeaves: Int,
    val iconEmoji: String
)

data class CustomerLetter(
    val id: String,
    val senderName: String,
    val senderAvatar: String,
    val letterTitle: String,
    val content: String
)
