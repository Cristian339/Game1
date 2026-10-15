package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.cozy.herbshop.data.model.PourResult
import com.cozy.herbshop.game.LevelGenerator
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.game.LevelSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Flujo del "Frasco Tetera": único recipiente donde se mezcla siguiendo la receta.
 */
class TeapotFlowTest {

    private val engine = GameEngine()
    private val solver = LevelSolver()

    private val order = CustomerOrder(
        id = "t",
        customerName = "Test",
        dialogue = "d",
        recipeName = "Infusión de Prueba",
        requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.LAVENDER, Herb.LAVENDER)
    )

    private fun jar(id: Int, vararg herbs: Herb, isTeapot: Boolean = false) = Jar(
        id = id,
        capacity = 4,
        layers = herbs.map { JarLayer(herb = it) },
        isTeapot = isTeapot
    )

    @Test
    fun teapotTakesTheTopRunAndKeepsItOpen() {
        val src = jar(0, Herb.CHAMOMILE, Herb.CHAMOMILE)
        val teapot = jar(1, isTeapot = true)

        val result = engine.pour(src, teapot, order)
        assertTrue(result is PourResult.Success)
        assertEquals("en la tetera se vierte de una en una", 1, (result as PourResult.Success).countPoured)

        val afterOne = engine.executePour(listOf(src, teapot), 0, 1, result.countPoured, order)
        assertEquals(listOf(Herb.CHAMOMILE), afterOne[1].stack)
        assertFalse(afterOne[1].isSealed)
        assertFalse(engine.isTeapotRuined(afterOne, order))
    }

    @Test
    fun teapotAcceptsAnyHerbSoMistakesCostAnUndo() {
        val src = jar(0, Herb.LAVENDER)
        val teapot = jar(1, Herb.CHAMOMILE, isTeapot = true)

        // La tetera ya NO valida el orden: el jugador puede equivocarse
        val result = engine.pour(src, teapot, order)
        assertTrue(result is PourResult.Success)

        val after = engine.executePour(listOf(src, teapot), 0, 1, (result as PourResult.Success).countPoured, order)
        assertEquals(listOf(Herb.CHAMOMILE, Herb.LAVENDER), after[1].stack)
        assertFalse("una mezcla errónea no está servida", engine.isTeapotBrewed(after, order))
        assertTrue("el desajuste debe detectarse como mezcla perdida", engine.isTeapotRuined(after, order))
    }

    @Test
    fun teapotNeverOverflowsEvenWithABigRun() {
        val src = jar(0, Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE)
        val teapot = jar(1, isTeapot = true)

        // Aunque haya 4 iguales arriba, la tetera solo coge de una en una:
        // así una receta 2+2 siempre es alcanzable.
        var current = listOf(src, teapot)
        repeat(4) {
            val result = engine.pour(current[0], current[1], order) as PourResult.Success
            current = engine.executePour(current, 0, 1, result.countPoured, order)
        }
        assertEquals(listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE), current[1].stack)
        assertTrue("4 manzanillas no son la receta 2+2", engine.isTeapotRuined(current, order))
    }

    @Test
    fun teapotMixCompletesSealsAndFulfilsOrder() {
        val jars = listOf(
            jar(0, Herb.CHAMOMILE, Herb.CHAMOMILE),
            jar(1, Herb.LAVENDER, Herb.LAVENDER),
            jar(2, isTeapot = true)
        )
        var current = jars
        // 2+2: de una en una, como en el juego
        repeat(4) { slot ->
            val srcIndex = if (slot < 2) 0 else 1
            val result = engine.pour(current[srcIndex], current[2], order)
            assertTrue(result is PourResult.Success)
            current = engine.executePour(current, srcIndex, 2, (result as PourResult.Success).countPoured, order)
        }

        assertEquals(order.requiredHerbs, current[2].stack)
        assertTrue("la tetera debe quedar sellada al servir la receta", current[2].isSealed)
        assertTrue(engine.isOrderFulfilled(current, order))
        assertTrue(engine.isTeapotBrewed(current, order))
        assertFalse(engine.isTeapotRuined(current, order))
        assertTrue(engine.checkLevelCompleted(current, order, requireRecipe = true))
    }

    @Test
    fun teapotCannotBeEmptiedOnceMixed() {
        val mixed = jar(2, Herb.CHAMOMILE, isTeapot = true)
        val target = jar(3, isTeapot = false)
        assertTrue(engine.pour(mixed, target, order) is PourResult.Invalid)
    }

    @Test
    fun levelWithRecipeAlwaysGetsTeapot() {
        val level = GameLevel(
            levelNumber = 9,
            jarCapacity = 4,
            jars = listOf(
                listOf("chamomile", "chamomile", "rosemary", "rosemary"),
                listOf("hibiscus", "hibiscus", "rosemary", "rosemary")
            ),
            customerOrder = order
        )
        val jars = LevelRepository(null).convertToJars(level)

        val teapots = jars.filter { it.isTeapot }
        assertEquals("debe haber exactamente una tetera", 1, teapots.size)
        assertTrue("la tetera empieza vacía", teapots.first().isEmpty)
        assertEquals(order.requiredHerbs.size, teapots.first().capacity)
    }

    @Test
    fun levelWithoutRecipeHasNoTeapot() {
        val level = GameLevel(
            levelNumber = 1,
            jarCapacity = 4,
            jars = listOf(listOf("chamomile"), listOf("mint"), emptyList())
        )
        val jars = LevelRepository(null).convertToJars(level)
        assertTrue(jars.none { it.isTeapot })
    }

    @Test
    fun generatedGateLevelIsSolvableWithTeapot() {
        val repository = LevelRepository(null)
        val level = repository.getLevel(20)
        val recipe = level.customerOrder!!.requiredHerbs

        val jars = repository.convertToJars(level)
        val teapotIndex = engine.findTeapotIndex(jars)
        assertNotNull("la tetera debe existir en niveles con receta", teapotIndex)

        val solution = solver.solve(jars, recipe)
        assertNotNull("el solver debe resolver el nivel usando la tetera", solution)
        assertTrue(solution!!.isNotEmpty())
    }

    @Test
    fun gateLevelsAreSolvableAndAlwaysShipATeapot() {
        val repository = LevelRepository(null)
        listOf(10, 20, 30, 40, 50).forEach { levelNumber ->
            val level = repository.getLevel(levelNumber)
            val jars = repository.convertToJars(level)
            assertNotNull("nivel $levelNumber sin tetera", engine.findTeapotIndex(jars))
            assertTrue(
                "nivel $levelNumber no tiene receta",
                level.customerOrder != null && level.customerOrder.requiredHerbs.isNotEmpty()
            )
            val solution = solver.solve(jars, level.customerOrder?.requiredHerbs)
            assertNotNull("nivel $levelNumber irresoluble con tetera", solution)
        }
    }

    @Test
    fun optionalOrderLevelsAlsoGetTeapot() {
        val repository = LevelRepository(null)
        val level = repository.getLevel(6)
        assertNotNull("nivel 6 (par con pedido) debe tener tetera", engine.findTeapotIndex(repository.convertToJars(level)))
    }

    @Test
    fun dailyChallengeIsSolvableWithItsTeapot() {
        val repository = LevelRepository(null)
        val level = repository.getDailyChallengeLevel()
        val jars = repository.convertToJars(level)

        assertNotNull("el reto del día debe tener tetera", engine.findTeapotIndex(jars))
        val recipe = level.customerOrder!!.requiredHerbs
        assertNotNull(
            "el reto del día debe poder servirse en la tetera",
            solver.solve(jars, recipe)
        )
    }

    /**
     * Reproduce en el motor real la solución que devuelve el solver: si el solver
     * se desincroniza del juego, la Pista sugeriría jugadas ilegales.
     */
    @Test
    fun solverSolutionIsExecutableInTheEngine() {
        val repository = LevelRepository(null)
        // Solo niveles con receta: los primeros son de aprendizaje y no llevan
        // tetera, así que no hay solución que reproducir.
        val conReceta = listOf(6, 8, 10, 20, 30, 40, 50)
        conReceta.forEach { levelNumber ->
            val level = repository.getLevel(levelNumber)
            val order = level.customerOrder ?: return@forEach
            val recipe = order.requiredHerbs
            var jars = repository.convertToJars(level)

            val solution = solver.solve(jars, recipe)
            assertNotNull("nivel $levelNumber sin solución", solution)
            assertTrue("nivel $levelNumber con solución vacía", solution!!.isNotEmpty())

            solution.forEach { (from, to) ->
                val result = engine.pour(jars[from], jars[to], order)
                assertTrue(
                    "nivel $levelNumber: movimiento ilegal $from -> $to (${result as? PourResult.Invalid})",
                    result is PourResult.Success
                )
                jars = engine.executePour(
                    jars, from, to,
                    (result as PourResult.Success).countPoured,
                    order
                )
            }

            assertTrue(
                "nivel $levelNumber: la solución no completa el nivel",
                engine.checkLevelCompleted(jars, order, LevelRepository.isGateLevel(levelNumber))
            )
        }
    }

    /**
     * El generador usa azar, así que un nivel puede salir "difícil" en un intento
     * y fácil en otro: repetimos para detectar niveles irresolubles.
     */
    @Test
    fun generatedLevelsAreAlwaysSolvable() {
        val generator = LevelGenerator()
        val recipes = mapOf(
            4 to listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.ROSEMARY, Herb.ROSEMARY),
            9 to listOf(Herb.HIBISCUS, Herb.HIBISCUS, Herb.ROSEMARY, Herb.ROSEMARY),
            15 to listOf(Herb.CINNAMON, Herb.CINNAMON, Herb.HIBISCUS, Herb.HIBISCUS),
            21 to listOf(Herb.CALENDULA, Herb.CALENDULA, Herb.MINT, Herb.MINT),
            28 to listOf(Herb.THYME, Herb.THYME, Herb.ROSEMARY, Herb.ROSEMARY)
        )
        repeat(6) { round ->
            recipes.forEach { (levelNumber, recipe) ->
                val level = generator.generateSolvableLevel(
                    levelNumber = levelNumber,
                    numHerbTypes = minOf(3 + levelNumber / 3, Herb.entries.size),
                    emptyJars = 2,
                    jarCapacity = 4,
                    random = kotlin.random.Random(round * 100L + levelNumber),
                    requiredOrder = recipe
                )
                val jars = LevelRepository(null).convertToJars(level.copy(customerOrder = order.copy(requiredHerbs = recipe)))
                assertNotNull("ronda $round, nivel $levelNumber sin tetera", engine.findTeapotIndex(jars))
                assertNotNull(
                    "ronda $round, nivel $levelNumber irresoluble",
                    solver.solve(jars, recipe)
                )
            }
        }
    }

    @Test
    fun everyLevelWithAnOrderCanServeIt() {        val repository = LevelRepository(null)
        (1..30).forEach { levelNumber ->
            val level = repository.getLevel(levelNumber)
            val recipe = level.customerOrder?.requiredHerbs
            if (recipe.isNullOrEmpty()) return@forEach

            val jars = repository.convertToJars(level)
            assertNotNull("nivel $levelNumber sin tetera", engine.findTeapotIndex(jars))
            assertNotNull("nivel $levelNumber con receta imposible", solver.solve(jars, recipe))
        }
    }
}
