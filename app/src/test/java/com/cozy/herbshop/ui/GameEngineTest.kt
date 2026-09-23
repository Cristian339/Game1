package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.cozy.herbshop.data.model.PourResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        engine = GameEngine()
    }

    @Test
    fun testValidPourIntoEmptyJar() {
        val src = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE))
        val dst = Jar(id = 1, capacity = 4, stack = emptyList())

        val result = engine.pour(src, dst)
        assertTrue(result is PourResult.Success)
        assertEquals(2, (result as PourResult.Success).countPoured)

        val newJars = engine.executePour(listOf(src, dst), 0, 1, result.countPoured)
        assertTrue(newJars[0].isEmpty)
        assertEquals(2, newJars[1].stack.size)
        assertEquals(Herb.CHAMOMILE, newJars[1].topHerb)
    }

    @Test
    fun testInvalidPourDifferentHerb() {
        val src = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE))
        val dst = Jar(id = 1, capacity = 4, stack = listOf(Herb.LAVENDER))

        val result = engine.pour(src, dst)
        assertTrue(result is PourResult.Invalid)
    }

    @Test
    fun testLevelCompletedCheck() {
        val completedJar1 = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE))
        val emptyJar = Jar(id = 1, capacity = 4, stack = emptyList())

        assertTrue(engine.checkLevelCompleted(listOf(completedJar1, emptyJar)))

        val incompleteJar = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.LAVENDER))
        assertFalse(engine.checkLevelCompleted(listOf(incompleteJar, emptyJar)))
    }

    @Test
    fun testMysteryLayerAutoRevealsWhenTop() {
        val hiddenLayer = JarLayer(herb = Herb.MINT, isHidden = true)
        val visibleLayer = JarLayer(herb = Herb.CHAMOMILE, isHidden = false)
        val jarWithHidden = Jar(id = 0, capacity = 4, layers = listOf(hiddenLayer, visibleLayer)).revealTopLayer()

        // Top layer is CHAMOMILE and revealed, bottom layer MINT is hidden
        assertEquals(Herb.CHAMOMILE, jarWithHidden.topHerb)
        assertTrue(jarWithHidden.layers[0].isHidden)

        val emptyDst = Jar(id = 1, capacity = 4, stack = emptyList())
        val pourResult = engine.pour(jarWithHidden, emptyDst)
        assertTrue(pourResult is PourResult.Success)

        val newJars = engine.executePour(listOf(jarWithHidden, emptyDst), 0, 1, (pourResult as PourResult.Success).countPoured)

        // After pouring top layer, bottom MINT becomes top and is automatically revealed
        assertEquals(Herb.MINT, newJars[0].topHerb)
        assertFalse(newJars[0].layers[0].isHidden)
    }

    @Test
    fun testFrozenLayerCannotBePoured() {
        val frozenLayer = JarLayer(herb = Herb.ROSEMARY, isFrozen = true)
        val frozenJar = Jar(id = 0, capacity = 4, layers = listOf(frozenLayer))
        val emptyDst = Jar(id = 1, capacity = 4, stack = emptyList())

        // Cannot pour top herb if frozen
        val result = engine.pour(frozenJar, emptyDst)
        assertTrue(result is PourResult.Invalid)
    }

    @Test
    fun testCustomerOrderSatisfied() {
        val order = CustomerOrder(
            id = "test_order",
            customerName = "Cliente",
            dialogue = "Té especial",
            recipeName = "Receta Test",
            requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.LAVENDER, Herb.LAVENDER)
        )

        val jarWithRecipe = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.LAVENDER, Herb.LAVENDER))
        val otherJar = Jar(id = 1, capacity = 4, stack = emptyList())

        assertTrue(engine.checkLevelCompleted(listOf(jarWithRecipe, otherJar), order))
    }

    @Test
    fun testLevelCompletedWithBasicSortingEvenIfCustomerOrderIsActive() {
        val order = CustomerOrder(
            id = "order_6",
            customerName = "Abuela Clara",
            dialogue = "Té especial",
            recipeName = "Infusión Dulce Calma",
            requiredHerbs = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.LAVENDER, Herb.LAVENDER)
        )

        // Sorted by single colors (basic sorting completion)
        val jar1 = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE))
        val jar2 = Jar(id = 1, capacity = 4, stack = listOf(Herb.LAVENDER, Herb.LAVENDER, Herb.LAVENDER, Herb.LAVENDER))
        val emptyJar = Jar(id = 2, capacity = 4, stack = emptyList())

        // Must complete successfully even if customer order is present!
        assertTrue(engine.checkLevelCompleted(listOf(jar1, jar2, emptyJar), order))
    }

    @Test
    fun testFrozenTopUnfreezesWhenOtherJarCompletes() {
        val completed = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE, Herb.CHAMOMILE))
        val frozenJar = Jar(
            id = 1,
            capacity = 4,
            layers = listOf(
                JarLayer(herb = Herb.LAVENDER),
                JarLayer(herb = Herb.LAVENDER),
                JarLayer(herb = Herb.MINT),
                JarLayer(herb = Herb.MINT, isFrozen = true)
            )
        )

        val processed = engine.processSealedJars(listOf(completed, frozenJar))
        assertFalse(processed[1].topLayer!!.isFrozen)
        assertEquals(Herb.MINT, processed[1].topHerb)
    }

    @Test
    fun testFrozenTopStaysFrozenWithoutProgress() {
        val mixed = Jar(id = 0, capacity = 4, stack = listOf(Herb.CHAMOMILE, Herb.LAVENDER, Herb.CHAMOMILE, Herb.LAVENDER))
        val frozenJar = Jar(
            id = 1,
            capacity = 4,
            layers = listOf(
                JarLayer(herb = Herb.MINT),
                JarLayer(herb = Herb.MINT, isFrozen = true)
            )
        )

        val processed = engine.processSealedJars(listOf(mixed, frozenJar))
        assertTrue(processed[1].topLayer!!.isFrozen)
    }
}
