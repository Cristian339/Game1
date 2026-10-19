package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.HelpItem
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.PourResult
import com.cozy.herbshop.game.Mechanic

data class PourAnimation(
    val sourceIndex: Int,
    val targetIndex: Int,
    val herb: Herb,
    val count: Int
)

data class GameUiState(
    val levelNumber: Int = 1,
    val jars: List<Jar> = emptyList(),
    val selectedJarIndex: Int? = null,
    val movesCount: Int = 0,
    val isLevelCompleted: Boolean = false,
    val history: List<List<Jar>> = emptyList(),
    val hintMove: Pair<Int, Int>? = null,
    /** Motivo del ultimo vertido rechazado, para no dejar al jugador a ciegas. */
    val blockedMessage: String? = null,
    val newlyUnlockedHerb: Herb? = null,
    /**
     * Hay algo que celebrar al ganar: una hierba nueva o una receta servida.
     * Si es false, la pantalla pasa directa al resumen sin ventana intermedia.
     */
    val hasCelebration: Boolean = false,
    /**
     * Los premios del nivel ya se han repartido y [hasCelebration] /
     * [newlyUnlockedHerb] son definitivos. La UI espera a esta bandera antes de
     * decidir si celebra o navega: si no, saltaria al resumen antes de tiempo.
     */
    val completionHandled: Boolean = false,
    val showExtraJarOption: Boolean = false,
    val customerOrder: CustomerOrder? = null,
    val isDailyChallenge: Boolean = false,
    val isZenMode: Boolean = false,
    val isGateLevel: Boolean = false,
    val orderFulfilled: Boolean = false,
    val bonusLeaves: Int = 0,
    val showBasicTutorial: Boolean = false,
    val showRecipeTutorial: Boolean = false,
    /**
     * Mecanica que este nivel estrena y que el jugador aun no ha visto. Se
     * explica una vez por mecanica, con opcion de saltar.
     */
    val showMechanicTutorial: Mechanic? = null,
    val animatingPour: PourAnimation? = null,
    val undoTokens: Int = 3,
    /** Usos restantes de cada ayuda. Ver [HelpItem]. */
    val helpStocks: Map<String, Int> = emptyMap(),
    /** Ayuda que se ha agotado y para la que hay que ofrecer anuncio o compra. */
    val depletedHelp: String? = null,
    val showExitConfirmDialog: Boolean = false,
    val restartCount: Int = 0,
    val teapotBrewed: Boolean = false,
    val teapotRuined: Boolean = false,
    val nextRecipeHerb: Herb? = null,
    val recipeRewardNext: Int = 0,
    val baseRewardNext: Int = 0,
    val rewardBase: Int = 0,
    val rewardRecipe: Int = 0,
    val showRecipeCelebration: Boolean = false,
    val pendingActions: List<Int> = emptyList(),
    /** Hierbas secretas ya encontradas en otras partidas. */
    val foundSecrets: Set<String> = emptySet(),
    /** Hierba secreta recien destapada, para celebrarla un momento. */
    val foundSecret: Herb? = null,
    /**
     * Jugadas de la solucion con la que el generador comprobo el nivel. Es el
     * par: la referencia para la chapa de "en par". Null si el nivel vino de
     * levels.json y no se ha generado.
     */
    val parMoves: Int? = null,
    /** Novedad que trae el sector (molinillo, limite de jugadas...). */
    val sectorRule: com.cozy.herbshop.data.model.SectorRule =
        com.cozy.herbshop.data.model.SectorRule.NONE,
    /** Se ha usado la Pista en este intento: anula su chapa. */
    val usedHint: Boolean = false,
    /** Se ha usado el Deshacer en este intento: anula su chapa. */
    val usedUndo: Boolean = false
) {
    val canUndo: Boolean get() = history.isNotEmpty()

    /**
     * En el ultimo sector cada nivel tiene un tope de jugadas: el par con
     * margen. No se pierde nunca: pasado el limite no se dejan hacer mas
     * jugadas, pero el nivel que ya esta resuelto cuenta igual.
     */
    val moveLimit: Int? get() = parMoves?.takeIf {
        sectorRule.limitaJugadas
    }?.let { it + LIMIT_HEADROOM }

    /** Quedan jugadas por gastar antes de tocar el limite del sector. */
    val movesLeft: Int? get() = moveLimit?.let { (it - movesCount).coerceAtLeast(0) }

    val atMoveLimit: Boolean get() = movesLeft == 0 && !isLevelCompleted

    companion object {
        /** Margen sobre el par: el limite es un reto, no una jaula. */
        const val LIMIT_HEADROOM = 4
    }
}

class GameEngine {

    fun pour(sourceJar: Jar, targetJar: Jar, customerOrder: CustomerOrder? = null): PourResult {
        if (sourceJar.id == targetJar.id) {
            return PourResult.Invalid("Es el mismo frasco.")
        }
        if (sourceJar.isSealed) {
            return PourResult.Invalid("Ese frasco está sellado: completa otro para abrirlo.")
        }
        if (targetJar.isSealed) {
            return PourResult.Invalid("Ese frasco está sellado: completa otro para abrirlo.")
        }
        if (sourceJar.isEmpty) {
            return PourResult.Invalid("Ese frasco está vacío.")
        }
        // El molinillo tiene capacidad de una hierba: si esta lleno hay que
        // sacarla antes de moler otra, no basta con decir "frasco lleno".
        if (targetJar.isGrinder && targetJar.isFull) {
            return PourResult.Invalid("El molinillo ya tiene una hierba: vacíalo antes de moler otra.")
        }
        if (targetJar.isTeapot && customerOrder != null && targetJar.isFull) {
            return PourResult.Invalid("La tetera ya está llena.")
        }
        if (targetJar.isFull) {
            return PourResult.Invalid("Ese frasco ya está lleno.")
        }
        // El hielo bloquea el frasco entero: no sale nada de el hasta que se
        // complete otro frasco y el tablero se descongele.
        if (sourceJar.hasFrozen) {
            return PourResult.Invalid("El hielo bloquea el frasco: completa otro frasco para romperlo.")
        }
        // La miel tapa la boca del frasco. Se puede verter encima de ella con su
        // mismo color, pero nunca sacarla.
        if (sourceJar.topLayer?.isSticky == true) {
            return PourResult.Invalid("La miel tapa el frasco: cúbrela con su mismo color para poder sacarla.")
        }

        val herbToPour = sourceJar.topHerb ?: return PourResult.Invalid("Ese frasco está tapado y no se ve qué contiene.")

        // La tetera es el único recipiente donde se mezcla: admite cualquier
        // hierba (equivocarse cuesta un Deshacer) y se vierte de una en una, como
        // si fuera una cuchara. La mezcla, una vez empezada, es definitiva.
        if (targetJar.isTeapot && customerOrder != null) {
            if (sourceJar.isTeapot) {
                return PourResult.Invalid("No puedes mezclar la tetera consigo misma.")
            }
            return PourResult.Success(1)
        }

        if (sourceJar.isTeapot) {
            return PourResult.Invalid("La mezcla de la tetera es definitiva: usa Deshacer si te equivocaste.")
        }

        // El molinillo no guarda, transforma: admite cualquier hierba, de una en
        // una, y la deja convertida en la siguiente del ciclo. Por eso no se le
        // exige que la hierba coincida con lo que tenga dentro.
        if (targetJar.isGrinder) {
            if (sourceJar.isGrinder) {
                return PourResult.Invalid("El molinillo no puede molerse a sí mismo.")
            }
            return PourResult.Success(1)
        }

        if (!targetJar.isEmpty && targetJar.topHerb != herbToPour) {
            return PourResult.Invalid("Solo puedes verter sobre la misma hierba o en un frasco vacío.")
        }

        val availableSpace = targetJar.capacity - targetJar.layers.size
        val sourceTopCount = sourceJar.topCount
        val countToPour = minOf(availableSpace, sourceTopCount)

        if (countToPour <= 0) {
            return PourResult.Invalid("No cabe nada más en ese frasco.")
        }

        return PourResult.Success(countToPour)
    }

    fun executePour(
        jars: List<Jar>,
        sourceIndex: Int,
        targetIndex: Int,
        countToPour: Int,
        customerOrder: CustomerOrder? = null
    ): List<Jar> {
        val src = jars[sourceIndex]
        val dst = jars[targetIndex]
        if (src.topHerb == null) return jars

        val layersPoured = src.layers.takeLast(countToPour)
        val newSrcLayers = src.layers.dropLast(countToPour)
        // Al molinillo no se le echa hierba: se le echa hierba y sale molida.
        val layersToStore = if (dst.isGrinder) {
            layersPoured.map { it.copy(herb = it.herb.molida) }
        } else {
            layersPoured
        }
        val newDstLayers = dst.layers + layersToStore

        val newJars = jars.toMutableList()
        newJars[sourceIndex] = src.copy(layers = newSrcLayers).revealTopLayer()
        newJars[targetIndex] = dst.copy(layers = newDstLayers).revealTopLayer()

        val brewed = if (newJars[targetIndex].isTeapot) sealBrewedTeapot(newJars[targetIndex], customerOrder) else null
        if (brewed != null) {
            newJars[targetIndex] = brewed
        }

        return processSealedJars(newJars)
    }

    /** Sella la tetera cuando la mezcla coincide con la receta activa. */
    fun sealBrewedTeapot(teapot: Jar, customerOrder: CustomerOrder?): Jar? {
        if (customerOrder == null || !teapot.isTeapot) return null
        val required = customerOrder.requiredHerbs
        if (required.isEmpty() || teapot.layers.size != required.size) return null
        if (teapot.layers.map { it.herb } != required) return null
        return teapot.copy(isSealed = true)
    }

    /** Tetera con la receta ya preparada (aún sin sellar). */
    fun isTeapotBrewed(jars: List<Jar>, customerOrder: CustomerOrder?): Boolean {
        if (customerOrder == null) return false
        val teapot = jars.firstOrNull { it.isTeapot } ?: return false
        return teapot.layers.size == customerOrder.requiredHerbs.size &&
            teapot.layers.map { it.herb } == customerOrder.requiredHerbs
    }

    /**
     * La tetera ya no puede llegar a la receta: se vertió una hierba que no sigue
     * el orden. Solo se puede recuperar con Deshacer o reiniciando el nivel.
     */
    fun isTeapotRuined(jars: List<Jar>, customerOrder: CustomerOrder?): Boolean {
        if (customerOrder == null) return false
        val teapot = jars.firstOrNull { it.isTeapot } ?: return false
        val required = customerOrder.requiredHerbs
        val current = teapot.layers.map { it.herb }
        if (current.isEmpty() || current.size > required.size) return false
        return current != required.take(current.size)
    }

    /** Hierba que la tetera necesita ahora mismo, o null si ya no puede completar. */
    fun nextRecipeHerb(teapot: Jar?, customerOrder: CustomerOrder?): Herb? {
        if (teapot == null || customerOrder == null) return null
        val required = customerOrder.requiredHerbs
        val slot = teapot.layers.size
        return if (slot < required.size) required[slot] else null
    }

    fun processSealedJars(jars: List<Jar>): List<Jar> {
        // "Completado" = lleno y de un solo color. Las capas ocultas o
        // congeladas no impiden contar ese frasco aqui: es justo lo que rompe el
        // hielo del tablero y abre los sellos. El molinillo y la tetera no
        // cuentan como frascos de almacen.
        val completedFilledJars = jars.count { jar ->
            !jar.isEmpty && !jar.isTeapot && !jar.isGrinder && jar.isFull &&
                jar.layers.all { layer -> layer.herb == jar.layers.first().herb }
        }
        if (completedFilledJars == 0) return jars

        // Al completar un frasco, el calor recorre la mesa y rompe TODO el hielo.
        // Asi el jugador no se queda con un frasco bloqueado para siempre: el
        // hielo se encadena con el resto del nivel.
        val mutable = jars.map { it.thaw() }.toMutableList()
        var unsealedCount = 0
        for (i in mutable.indices) {
            if (mutable[i].isTeapot) continue
            if (mutable[i].isSealed && unsealedCount < completedFilledJars) {
                mutable[i] = mutable[i].unseal().revealTopLayer()
                unsealedCount++
            }
        }
        return mutable
    }

    fun revealTopLayers(jars: List<Jar>): List<Jar> {
        return jars.map { it.revealTopLayer() }
    }

    /**
     * Hierbas secretas que el jugador tiene a la vista: las capas secretas ya
     * destapadas. La puerta esconde una en un frasco tapado, y hasta que no se
     * destapa no entra al herbario; por eso el nivel tampoco se termina con capas
     * por destapar.
     */
    fun visibleSecrets(jars: List<Jar>): List<Herb> =
        jars.flatMap { jar -> jar.layers }
            .filter { capa -> !capa.isHidden && capa.herb.esSecreta }
            .map { capa -> capa.herb }
            .distinct()

    fun checkLevelCompleted(
        jars: List<Jar>,
        customerOrder: CustomerOrder? = null,
        requireRecipe: Boolean = false
    ): Boolean {
        val fulfilled = isOrderFulfilled(jars, customerOrder)

        // En niveles puerta la receta es obligatoria para completar
        if (requireRecipe) {
            return fulfilled
        }

        // Victoria por cumplir el pedido de receta del cliente (mezcla en la tetera)
        if (fulfilled) {
            return true
        }

        // Con una mezcla empezada en la tetera ya no se puede ganar ordenando:
        // la receta es el camino válido.
        if (customerOrder != null) {
            val teapot = jars.firstOrNull { it.isTeapot }
            if (teapot != null && teapot.layers.isNotEmpty()) {
                return false
            }
        }

        // 1. Victoria por ordenación completa básica
        return jars.all { jar -> jar.isCompleted }
    }

    fun isOrderFulfilled(jars: List<Jar>, customerOrder: CustomerOrder?): Boolean {
        if (customerOrder == null) return false
        val required = customerOrder.requiredHerbs
        if (required.isEmpty()) return false

        val teapot = jars.firstOrNull { it.isTeapot }
        val matches = { jar: Jar ->
            jar.layers.size == required.size && jar.layers.map { it.herb } == required
        }
        // La receta se prepara en la tetera; si el nivel no tiene tetera, se acepta
        // cualquier frasco con la mezcla exacta.
        val orderJar: Jar = (if (teapot != null) teapot.takeIf(matches) else jars.firstOrNull(matches))
            ?: return false

        // Resto: vacío o puro (una sola hierba). Se admite frasco parcial puro
        // porque la receta consume 2+2 de dos hierbas y sobran 2 unidades. El
        // molinillo no almacena: tiene que quedar vacío, igual que en los niveles
        // sin receta. Un frasco helado no se puede dar por bueno sin romperlo.
        //
        // No hace falta pedir que esté todo destapado: la hierba secreta es la
        // única de su color, así que su frasco solo puede terminar vacío o con
        // ella sola, y para eso hay que quitarle lo de encima (que la destapa).
        val remainingJars = jars.filter { it.id != orderJar.id }
        return remainingJars.all { jar ->
            when {
                jar.isGrinder -> jar.isEmpty
                jar.isEmpty -> true
                jar.hasFrozen -> false
                else -> jar.layers.all { it.herb == jar.layers.first().herb }
            }
        }
    }

    /** Índice de la tetera con la receta ya mezclada (para el check en UI), o null. */
    fun findOrderJarIndex(jars: List<Jar>, customerOrder: CustomerOrder?): Int? {
        if (customerOrder == null) return null
        val required = customerOrder.requiredHerbs
        if (required.isEmpty()) return null
        return jars.indexOfFirst { jar ->
            jar.isTeapot && jar.layers.size == required.size && jar.layers.map { it.herb } == required
        }.takeIf { it >= 0 }
    }

    /** Índice de la tetera (para la Pista y la UI), o null si el nivel no tiene. */
    fun findTeapotIndex(jars: List<Jar>): Int? =
        jars.indexOfFirst { it.isTeapot }.takeIf { it >= 0 }
}
