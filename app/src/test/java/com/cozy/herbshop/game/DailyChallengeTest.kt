package com.cozy.herbshop.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El reto diario se identifica por el dia del ano: mismo dia, mismo tablero,
 * misma receta y mismo premio ("daily_<dia>"); dia nuevo, reto nuevo. Y una vez
 * superado no se puede volver a jugar, asi que la partida apunta el dia con el
 * que se jugo (no el dia con el que se acaba: la partida puede cruzar la
 * medianoche).
 */
class DailyChallengeTest {

    private val repository = LevelRepository(context = null)
    private val solver = LevelSolver()

    @Test
    fun elRetoDelDiaLlevaSuFechaYEsElMismoHastaQueCambiaElDia() {
        val manana = repository.getDailyChallengeLevel(seed = 123)
        val tarde = repository.getDailyChallengeLevel(seed = 123)

        assertEquals("el pedido del diario lleva la fecha en su id", "daily_123", manana.customerOrder?.id)
        assertTrue("el reto del dia tiene que ir marcado como diario", manana.isDailyChallenge)
        assertEquals(999, manana.levelNumber)
        assertEquals("el tablero del dia no puede cambiar dentro del dia", manana.jars, tarde.jars)
        assertEquals(
            "la receta del dia no puede cambiar dentro del dia",
            manana.customerOrder?.requiredHerbs,
            tarde.customerOrder?.requiredHerbs
        )
    }

    @Test
    fun elRetoDelDiaSoloSeBloqueaElMismoDia() {
        assertTrue(
            "el reto superado hoy no puede volver a jugarse",
            LevelRepository.dailyAlreadyPlayed(doneDay = 300, today = 300)
        )
        assertFalse(
            "manana el reto vuelve a estar disponible",
            LevelRepository.dailyAlreadyPlayed(doneDay = 300, today = 301)
        )
        assertFalse(
            "sin reto jugado todavia (0), el de hoy esta disponible",
            LevelRepository.dailyAlreadyPlayed(doneDay = 0, today = 301)
        )
    }

    @Test
    fun elRetoDelDiaSePuedeTerminar() {
        val level = repository.getDailyChallengeLevel(seed = 77)
        val jars = repository.convertToJars(level)

        assertTrue(
            "el reto del dia trae receta: tiene que traer tetera",
            jars.any { it.isTeapot }
        )
        assertTrue(
            "reto del dia imposible de terminar con su receta",
            solver.isSolvable(jars, level.customerOrder?.requiredHerbs)
        )
    }
}
