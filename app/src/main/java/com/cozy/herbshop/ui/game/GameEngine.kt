package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.PourResult

data class GameUiState(
    val levelNumber: Int = 1,
    val jars: List<Jar> = emptyList(),
    val selectedJarIndex: Int? = null,
    val movesCount: Int = 0,
    val isLevelCompleted: Boolean = false,
    val history: List<List<Jar>> = emptyList(),
    val hintMove: Pair<Int, Int>? = null,
    val newlyUnlockedHerb: Herb? = null,
    val showExtraJarOption: Boolean = false
) {
    val canUndo: Boolean get() = history.isNotEmpty()
}

class GameEngine {

    fun pour(sourceJar: Jar, targetJar: Jar): PourResult {
        if (sourceJar.id == targetJar.id) {
            return PourResult.Invalid("Source and target are the same jar.")
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

        val availableSpace = targetJar.capacity - targetJar.stack.size
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
        val herbToPour = src.topHerb ?: return jars

        val newSrcStack = src.stack.dropLast(countToPour)
        val newDstStack = dst.stack + List(countToPour) { herbToPour }

        val newJars = jars.toMutableList()
        newJars[sourceIndex] = src.copy(stack = newSrcStack)
        newJars[targetIndex] = dst.copy(stack = newDstStack)

        return newJars
    }

    fun checkLevelCompleted(jars: List<Jar>): Boolean {
        return jars.all { jar -> jar.isCompleted }
    }
}
