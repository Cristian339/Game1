package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import kotlin.random.Random

class LevelGenerator(
    private val solver: LevelSolver = LevelSolver()
) {

    fun generateSolvableLevel(
        levelNumber: Int,
        numHerbTypes: Int = 4,
        emptyJars: Int = 2,
        jarCapacity: Int = 4,
        random: Random = Random.Default
    ): GameLevel {
        val availableHerbs = Herb.entries.take(numHerbTypes)
        val attemptsLimit = 50

        for (attempt in 1..attemptsLimit) {
            val solvedStacks = mutableListOf<MutableList<Herb>>()
            for (herb in availableHerbs) {
                solvedStacks.add(MutableList(jarCapacity) { herb })
            }
            for (e in 0 until emptyJars) {
                solvedStacks.add(mutableListOf())
            }

            val totalJars = solvedStacks.size
            val shuffleSteps = 20 + levelNumber * 3

            for (step in 0 until shuffleSteps) {
                val srcIdx = random.nextInt(totalJars)
                val dstIdx = random.nextInt(totalJars)
                if (srcIdx == dstIdx) continue

                val srcStack = solvedStacks[srcIdx]
                val dstStack = solvedStacks[dstIdx]

                if (srcStack.isNotEmpty() && dstStack.size < jarCapacity) {
                    val herb = srcStack.removeAt(srcStack.lastIndex)
                    dstStack.add(herb)
                }
            }

            val initialJars = solvedStacks.mapIndexed { index, herbs ->
                Jar(id = index, capacity = jarCapacity, stack = herbs)
            }

            if (solver.isSolvable(initialJars)) {
                val shouldAddMystery = levelNumber >= 3
                val shouldAddSealed = levelNumber >= 4
                var sealedAdded = false

                val jsonJars = initialJars.mapIndexed { jarIndex, jar ->
                    val isSealedJar = shouldAddSealed && !sealedAdded && jarIndex == 0 && jar.layers.isNotEmpty()
                    if (isSealedJar) sealedAdded = true

                    jar.layers.mapIndexed { layerIndex, layer ->
                        val isTop = layerIndex == jar.layers.lastIndex
                        val isMystery = shouldAddMystery && !isTop && random.nextFloat() < 0.25f
                        val isFrozen = levelNumber >= 5 && isTop && random.nextFloat() < 0.20f

                        var idStr = layer.herb.id
                        if (isMystery) idStr = "?$idStr"
                        if (isFrozen) idStr = "frozen:$idStr"
                        if (isSealedJar && layerIndex == 0) idStr = "sealed:$idStr"
                        idStr
                    }
                }
                return GameLevel(
                    levelNumber = levelNumber,
                    jarCapacity = jarCapacity,
                    jars = jsonJars
                )
            }
        }

        return createFallbackLevel(levelNumber, numHerbTypes, jarCapacity)
    }

    private fun createFallbackLevel(levelNumber: Int, numHerbs: Int, jarCapacity: Int): GameLevel {
        val herbs = Herb.entries.take(numHerbs)
        val jarList = mutableListOf<List<String>>()

        for (i in 0 until numHerbs) {
            val jarHerbs = mutableListOf<String>()
            for (j in 0 until jarCapacity) {
                val herbIndex = (i + j) % numHerbs
                jarHerbs.add(herbs[herbIndex].id)
            }
            jarList.add(jarHerbs)
        }
        jarList.add(emptyList())
        jarList.add(emptyList())

        return GameLevel(
            levelNumber = levelNumber,
            jarCapacity = jarCapacity,
            jars = jarList
        )
    }
}
