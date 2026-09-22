package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
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
}
