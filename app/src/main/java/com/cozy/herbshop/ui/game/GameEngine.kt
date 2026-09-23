package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.Herb
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
    val showExtraJarOption: Boolean = false,
    val customerOrder: CustomerOrder? = null,
    val isDailyChallenge: Boolean = false,
    val isZenMode: Boolean = false,
    val animatingPour: PourAnimation? = null
) {
    val canUndo: Boolean get() = history.isNotEmpty()
}

class GameEngine {

    fun pour(sourceJar: Jar, targetJar: Jar): PourResult {
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

    fun executePour(jars: List<Jar>, sourceIndex: Int, targetIndex: Int, countToPour: Int): List<Jar> {
        val src = jars[sourceIndex]
        val dst = jars[targetIndex]
        if (src.topHerb == null) return jars

        val layersPoured = src.layers.takeLast(countToPour)
        val newSrcLayers = src.layers.dropLast(countToPour)
        val newDstLayers = dst.layers + layersPoured

        val newJars = jars.toMutableList()
        newJars[sourceIndex] = src.copy(layers = newSrcLayers).revealTopLayer().unfreezeTopLayer()
        newJars[targetIndex] = dst.copy(layers = newDstLayers).revealTopLayer().unfreezeTopLayer()

        return processSealedJars(newJars)
    }

    fun processSealedJars(jars: List<Jar>): List<Jar> {
        val completedFilledJars = jars.count { !it.isEmpty && it.isFull && it.layers.all { l -> !l.isHidden && !l.isFrozen && l.herb == it.layers.first().herb } }
        if (completedFilledJars > 0 && jars.any { it.isSealed }) {
            val mutable = jars.toMutableList()
            var unsealedCount = 0
            for (i in mutable.indices) {
                if (mutable[i].isSealed && unsealedCount < completedFilledJars) {
                    mutable[i] = mutable[i].unseal().revealTopLayer().unfreezeTopLayer()
                    unsealedCount++
                }
            }
            return mutable
        }
        return jars
    }

    fun revealTopLayers(jars: List<Jar>): List<Jar> {
        return jars.map { it.revealTopLayer() }
    }

    fun checkLevelCompleted(jars: List<Jar>, customerOrder: CustomerOrder? = null): Boolean {
        // 1. Victoria por ordenación completa básica
        val basicSortingCompleted = jars.all { jar -> jar.isCompleted }
        if (basicSortingCompleted) {
            return true
        }

        // 2. Victoria por cumplir el pedido de receta del cliente
        if (customerOrder != null) {
            val orderJar = jars.find { jar ->
                jar.layers.size == customerOrder.requiredHerbs.size &&
                jar.layers.map { it.herb } == customerOrder.requiredHerbs
            }
            if (orderJar != null) {
                val remainingJars = jars.filter { it.id != orderJar.id }
                return remainingJars.all { it.isCompleted }
            }
        }
        return false
    }
}
