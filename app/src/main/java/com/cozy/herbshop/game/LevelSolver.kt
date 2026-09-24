package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import java.util.ArrayDeque

class LevelSolver {

    data class State(val jars: List<List<Herb>>)

    fun isSolvable(initialJars: List<Jar>, requiredOrder: List<Herb>? = null): Boolean {
        return solve(initialJars, requiredOrder) != null
    }

    fun solve(initialJars: List<Jar>, requiredOrder: List<Herb>? = null): List<Pair<Int, Int>>? {
        val initialCapacity = initialJars.maxOfOrNull { it.capacity } ?: 4
        val startState = canonicalState(initialJars.map { it.stack })

        if (isSolvedState(startState, initialCapacity, requiredOrder)) {
            return emptyList()
        }

        val queue = ArrayDeque<State>()
        val parentMap = mutableMapOf<State, State?>()
        val moveMap = mutableMapOf<State, Pair<Int, Int>>()
        val visited = mutableSetOf<State>()

        queue.add(startState)
        visited.add(startState)
        parentMap[startState] = null

        var solvedState: State? = null

        while (queue.isNotEmpty() && visited.size < 30000) {
            val current = queue.poll() ?: break

            if (isSolvedState(current, initialCapacity, requiredOrder)) {
                solvedState = current
                break
            }

            for (fromIdx in current.jars.indices) {
                val srcStack = current.jars[fromIdx]
                if (srcStack.isEmpty()) continue

                val topHerb = srcStack.last()
                val topCount = getTopCount(srcStack)

                for (toIdx in current.jars.indices) {
                    if (fromIdx == toIdx) continue
                    val dstStack = current.jars[toIdx]

                    if (dstStack.size >= initialCapacity) continue
                    if (dstStack.isNotEmpty() && dstStack.last() != topHerb) continue

                    val availableSpace = initialCapacity - dstStack.size
                    val amountToPour = minOf(topCount, availableSpace)
                    if (amountToPour <= 0) continue

                    val newJars = current.jars.map { it.toMutableList() }
                    for (i in 0 until amountToPour) {
                        newJars[fromIdx].removeAt(newJars[fromIdx].lastIndex)
                        newJars[toIdx].add(topHerb)
                    }

                    val nextState = canonicalState(newJars)
                    if (visited.add(nextState)) {
                        parentMap[nextState] = current
                        moveMap[nextState] = Pair(fromIdx, toIdx)
                        queue.add(nextState)
                    }
                }
            }
        }

        if (solvedState == null) {
            try {
                java.io.File(java.io.File(System.getProperty("java.io.tmpdir"), "solver_debug.txt").absolutePath)
                    .appendText("exhausted=${visited.size} requiredOrder=$requiredOrder\n")
            } catch (_: Exception) {}
            return null
        }

        val moves = mutableListOf<Pair<Int, Int>>()
        var curr: State? = solvedState
        while (curr != null && parentMap[curr] != null) {
            val move = moveMap[curr]
            if (move != null) {
                moves.add(0, move)
            }
            curr = parentMap[curr]
        }

        return moves
    }

    private fun getTopCount(stack: List<Herb>): Int {
        if (stack.isEmpty()) return 0
        val top = stack.last()
        var count = 0
        for (i in stack.indices.reversed()) {
            if (stack[i] == top) count++ else break
        }
        return count
    }

    private fun canonicalState(jars: List<List<Herb>>): State {
        return State(jars)
    }

    private fun isSolvedState(state: State, capacity: Int, requiredOrder: List<Herb>? = null): Boolean {
        if (requiredOrder != null) {
            // Victoria con receta: un frasco igual a la receta, resto vacío o puro
            val orderJarCount = state.jars.count { it == requiredOrder }
            if (orderJarCount != 1) return false
            for (stack in state.jars) {
                if (stack == requiredOrder) continue
                if (stack.isEmpty()) continue
                if (stack.any { it != stack.first() }) return false
            }
            return true
        }

        for (stack in state.jars) {
            if (stack.isEmpty()) continue
            if (stack.size != capacity) return false
            val first = stack.first()
            if (stack.any { it != first }) return false
        }
        return true
    }
}
