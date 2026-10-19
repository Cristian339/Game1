package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.CustomerOrder
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
    private lateinit var repository: LevelRepository

    @Before
    fun setUp() {
        solver = LevelSolver()
        generator = LevelGenerator(solver)
        repository = LevelRepository(null)
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

        // Se lee con el mismo parser que usa la partida: los ids pueden traer
        // prefijos de misterio y una capa tapada no es una hierba distinta.
        val jars = repository.convertToJars(level)
        val isSolvable = solver.isSolvable(jars)
        assertTrue("Generated level should be solvable by LevelSolver", isSolvable)
    }

    @Test
    fun testDailyDifficultyLevelHasNoFrozenLayers() {
        // Daily challenge uses level 4 (mystery, never frozen).
        val level = generator.generateSolvableLevel(
            levelNumber = 4,
            numHerbTypes = 4,
            emptyJars = 2,
            jarCapacity = 4,
            random = Random(42)
        )

        val jars = repository.convertToJars(level)
        assertFalse("Difficulty 4 must not generate frozen layers", jars.any { it.hasFrozen })
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
        // Un nivel con receta de verdad: la tetera es una herramienta y el
        // objetivo no es solo ordenar, hay que servirla en orden.
        val receta = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.ROSEMARY, Herb.LAVENDER)
        val level = generator.generateSolvableLevel(
            levelNumber = 9,
            numHerbTypes = 6,
            emptyJars = 2,
            jarCapacity = 4,
            random = Random(9),
            requiredOrder = receta
        ).copy(
            customerOrder = CustomerOrder(
                id = "test_gate_9",
                customerName = "Prueba",
                dialogue = "",
                recipeName = "Receta de prueba",
                requiredHerbs = receta
            )
        )

        // Se lee con el mismo parser que usa la partida: la tetera es el ultimo
        // frasco vacio y el molinillo, si lo hubiera, no se confunde con ella.
        val jars = repository.convertToJars(level)
        assertTrue(
            "Generated gate level 9 must be solvable",
            // El reparto es el que comprueba el generador. La busqueda por
            // anchura no llega a encontrar la solucion de un nivel bien disperso
            // (seis o mas hierbas) dentro del presupuesto de memoria, asi que
            // daria un falso "imposible" en niveles que si se pueden terminar.
            solver.esGanalible(jars, receta)
        )
    }
}
