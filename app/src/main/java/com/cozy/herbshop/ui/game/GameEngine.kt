package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.HelpItem
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.PourResult

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
     * Cuando el sector es de limite de jugadas, el tope es el par con margen.
     * No se pierde nunca: pasado el limite no se dejan hacer mas jugadas, pero
     * el nivel que ya esta resuelto cuenta igual.
     */
    val moveLimit: Int? get() = when (sectorRule) {
        com.cozy.herbshop.data.model.SectorRule.MOVE_LIMIT ->
            parMoves?.let { it + LIMIT_HEADROOM }
        else -> null
    }

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
            return PourResult.Invalid("Source and target are the same jar.")
        }
        if (sourceJar.isSealed) {
            return PourResult.Invalid("Source jar is sealed.")
        }
        if (targetJar.isSealed) {
            return PourResult.Invalid("Target jar is sealed.")
        }
        if (sourceJar.isEmpty) {
            return PourResult.Invalid("Source jar is empty.")
        }
        if (targetJar.isFull) {
            return PourResult.Invalid("Target jar is full.")
        }

        val herbToPour = sourceJar.topHerb ?: return PourResult.Invalid("No herb to pour.")

        // La tetera es el único recipiente donde se mezcla: admite cualquier
        // hierba (equivocarse cuesta un Deshacer) y se vierte de una en una, como
        // si fuera una cuchara. La mezcla, una vez empezada, es definitiva.
        if (targetJar.isTeapot && customerOrder != null) {
            if (sourceJar.isTeapot) {
                return PourResult.Invalid("No se puede mezclar la tetera con sí misma.")
            }
            if (targetJar.capacity - targetJar.layers.size <= 0) {
                return PourResult.Invalid("La Tetera ya está llena.")
            }
            return PourResult.Success(1)
        }

        if (sourceJar.isTeapot) {
            return PourResult.Invalid("La mezcla de la Tetera es definitiva: usa Deshacer si te equivocaste.")
        }

        // El molinillo no guarda, transforma: admite cualquier hierba, de una en
        // una, y la deja convertida en la siguiente del ciclo. Por eso no se le
        // exige que la hierba coincida con lo que tenga dentro.
        if (targetJar.isGrinder) {
            if (sourceJar.isGrinder) {
                return PourResult.Invalid("El molinillo no se muele a sí mismo.")
            }
            if (targetJar.capacity - targetJar.layers.size <= 0) {
                return PourResult.Invalid("El molinillo ya tiene una hierba dentro.")
            }
            return PourResult.Success(1)
        }

        if (!targetJar.isEmpty && targetJar.topHerb != herbToPour) {
            return PourResult.Invalid("Target jar contains a different herb type.")
        }

        val availableSpace = targetJar.capacity - targetJar.layers.size
        val sourceTopCount = sourceJar.topCount
        val countToPour = minOf(availableSpace, sourceTopCount)

        if (countToPour <= 0) {
            return PourResult.Invalid("No space to pour.")
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
        newJars[sourceIndex] = src.copy(layers = newSrcLayers).revealTopLayer().unfreezeTopLayer()
        newJars[targetIndex] = dst.copy(layers = newDstLayers).revealTopLayer().unfreezeTopLayer()

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
        // congeladas son solo una puerta visual: seVAN revelando al verter, así
        // que no cuentan aquí (importante para que la Pista cuadre con el juego).
        val completedFilledJars = jars.count { jar ->
            !jar.isEmpty && !jar.isTeapot && jar.isFull &&
                jar.layers.all { layer -> layer.herb == jar.layers.first().herb }
        }
        if (completedFilledJars == 0) return jars

        val mutable = jars.toMutableList()
        var unsealedCount = 0
        var changed = false
        for (i in mutable.indices) {
            if (mutable[i].isTeapot) continue
            if (mutable[i].isSealed && unsealedCount < completedFilledJars) {
                mutable[i] = mutable[i].unseal().revealTopLayer().unfreezeTopLayer()
                unsealedCount++
                changed = true
            } else if (!mutable[i].isSealed && mutable[i].topLayer?.isFrozen == true) {
                mutable[i] = mutable[i].unfreezeTopLayer()
                changed = true
            }
        }
        return if (changed) mutable else jars
    }

    fun revealTopLayers(jars: List<Jar>): List<Jar> {
        return jars.map { it.revealTopLayer() }
    }

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
        // porque la receta consume 2+2 de dos hierbas y sobran 2 unidades.
        val remainingJars = jars.filter { it.id != orderJar.id }
        return remainingJars.all { jar ->
            jar.isEmpty || jar.layers.all { it.herb == jar.layers.first().herb }
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
