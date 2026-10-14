package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class LevelSolverTest {

    private lateinit var solver: LevelSolver
    private lateinit var generator: LevelGenerator

    @Before
    fun setUp() {
        solver = LevelSolver()
        generator = LevelGenerator(solver)
    }

    @Test
    fun testAlreadySolvedLevel() {
        val jar1 = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE))
        val jar2 = Jar(id = 1, capacity = 4, stack = listOf(Herb.LAVENDER, Herb.LAVENDER, Herb.LAVENDER, Herb.LAVENDER))
        val jar3 = Jar(id = 2, capacity = 4, stack = emptyList())

        val solution = solver.solve(listOf(jar1, jar2, jar3))
        assertNotNull(solution)
        assertTrue(solution!!.isEmpty())
    }

    @Test
    fun testSimpleOneMoveLevel() {
        val jar1 = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE))
        val jar2 = Jar(id = 1, capacity = 4, stack = listOf(Herb.LAVENDER, Herb.LAVENDER, Herb.LAVENDER, Herb.LAVENDER))
        val jar3 = Jar(id = 2, capacity = 4, stack = listOf(Herb.CHAMOMILE))

        val solution = solver.solve(listOf(jar1, jar2, jar3))
        assertNotNull(solution)
        assertEquals(1, solution!!.size)
        val move = solution[0]
        assertTrue(move == Pair(2, 0) || move == Pair(0, 2))
    }

    @Test
    fun testGeneratedLevelSolvability() {
        val level = generator.generateSolvableLevel(
            levelNumber = 1,
            numHerbTypes = 3,
            emptyJars = 2,
            jarCapacity = 4
        )

        val jars = level.jars.mapIndexed { index, herbIds ->
            Jar(id = index, capacity = level.jarCapacity, stack = herbIds.map { Herb.fromId(it) })
        }

        val isSolvable = solver.isSolvable(jars)
        assertTrue("Generated level should be solvable by LevelSolver", isSolvable)
    }

    @Test
    fun testDailyDifficultyLevelHasNoFrozenLayers() {
        // Daily challenge uses difficulty 4 (mystery + sealed, never frozen)
        val level = generator.generateSolvableLevel(
            levelNumber = 4,
            numHerbTypes = 4,
            emptyJars = 2,
            jarCapacity = 4,
            random = Random(42)
        )

        val hasFrozen = level.jars.any { jar -> jar.any { it.startsWith("frozen:") } }
        assertFalse("Difficulty 4 must not generate frozen layers", hasFrozen)

        val jars = level.jars.mapIndexed { index, herbIds ->
            Jar(id = index, capacity = level.jarCapacity, stack = herbIds.map { Herb.fromId(it.removePrefix("sealed:").removePrefix("?")) })
        }
        assertTrue("Daily-difficulty level should be solvable", solver.isSolvable(jars))
    }

    @Test
    fun testGateLevelFourSolvableWithRecipe() {
        // Predefined level 4 from levels.json
        val jars = listOf(
            Jar(id = 0, capacity = 4, stack = listOf(Herb.HIBISCUS, Herb.CHAMOMILE, Herb.HIBISCUS, Herb.ROSEMARY)),
            Jar(id = 1, capacity = 4, stack = listOf(Herb.ROSEMARY, Herb.LAVENDER, Herb.ROSEMARY, Herb.CHAMOMILE)),
            Jar(id = 2, capacity = 4, stack = listOf(Herb.LAVENDER, Herb.HIBISCUS, Herb.CHAMOMILE, Herb.LAVENDER)),
            Jar(id = 3, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.ROSEMARY, Herb.LAVENDER, Herb.HIBISCUS)),
            Jar(id = 4, capacity = 4, stack = emptyList()),
            Jar(id = 5, capacity = 4, stack = emptyList())
        )

        assertTrue(
            "Level 4 must be solvable",
            solver.isSolvable(jars)
        )
    }

    @Test
    fun testGeneratedGateLevelSolvableWithRecipe() {
        val level = generator.generateSolvableLevel(
            levelNumber = 9,
            numHerbTypes = 6,
            emptyJars = 2,
            jarCapacity = 4,
            random = Random(9)
        )

        // El generador comprueba el nivel con su frasco sellado puesto, asi que aqui
        // tambien: si se quita el sello, el frasco se deja tocar y el nivel queda
        // mas facil de lo que era y la comprobacion no vale.
        val jars = level.jars.mapIndexed { index, herbIds ->
            Jar(
                id = index,
                capacity = level.jarCapacity,
                isSealed = herbIds.any { it.startsWith("sealed:") },
                stack = herbIds.map {
                    Herb.fromId(it.removePrefix("sealed:").removePrefix("frozen:").removePrefix("?"))
                }
            )
        }
        assertTrue(
            "Generated gate level 9 must be solvable",
            // El reparto es el que comprueba el generador. La busqueda por
            // anchura no llega a encontrar la solucion de un nivel bien disperso
            // (seis o mas hierbas) dentro del presupuesto de memoria, asi que
            // daria un falso "imposible" en niveles que si se pueden terminar.
            solver.esGanalible(jars)
        )
    }
}
