package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.repository.UserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tabla de premios por repetir un nivel:
 *
 * | Partida | Base nivel | Receta |
 * |---------|-----------|--------|
 * | 1ª      | +15       | +50    |
 * | 2ª      | 0         | +5     |
 * | 3ª+     | 0         | 0      |
 */
class RewardTableTest {

    private fun reward(times: Int, recipe: Boolean) =
        UserPreferencesRepository.computeReward(times, recipeFulfilled = recipe)

    @Test
    fun firstCompletionPaysFull() {
        val r = reward(times = 0, recipe = true)
        assertEquals(15, r.base)
        assertEquals(50, r.recipe)
        assertEquals(65, r.total)
    }

    @Test
    fun secondCompletionOnlyPaysATip() {
        val r = reward(times = 1, recipe = true)
        assertEquals(0, r.base)
        assertEquals(5, r.recipe)
        assertEquals(5, r.total)
    }

    @Test
    fun thirdAndLaterCompletionsPayNothing() {
        listOf(2, 3, 10).forEach { times ->
            val r = reward(times = times, recipe = true)
            assertEquals("partida $times no debe base", 0, r.base)
            assertEquals("partida $times no debe receta", 0, r.recipe)
            assertEquals(0, r.total)
        }
    }

    @Test
    fun levelWithoutRecipeOnlyPaysTheFirstBase() {
        assertEquals(15, reward(times = 0, recipe = false).total)
        assertEquals(0, reward(times = 1, recipe = false).total)
        assertEquals(0, reward(times = 2, recipe = false).total)
    }
}
