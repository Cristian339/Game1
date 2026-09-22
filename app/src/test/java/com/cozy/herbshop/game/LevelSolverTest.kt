package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
}
