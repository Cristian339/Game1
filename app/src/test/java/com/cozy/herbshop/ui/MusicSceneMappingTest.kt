package com.cozy.herbshop.ui.game

import com.cozy.herbshop.data.audio.MusicTrack
import com.cozy.herbshop.ui.components.GameScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La música sigue al escenario de la partida. Este test fija el reparto para que
 * nadie cambie una pista sin querer.
 */
class MusicSceneMappingTest {

    private fun trackFor(level: Int, daily: Boolean = false, zen: Boolean = false) =
        MusicTrack.forScene(GameScene.forMode(level, daily, zen))

    @Test
    fun eachSceneHasItsOwnTrack() {
        assertEquals(MusicTrack.MORNING, MusicTrack.forScene(GameScene.MORNING))
        assertEquals(MusicTrack.MORNING, MusicTrack.forScene(GameScene.GREENHOUSE))
        assertEquals(MusicTrack.NIGHT, MusicTrack.forScene(GameScene.NIGHT))
        assertEquals(MusicTrack.FOREST, MusicTrack.forScene(GameScene.FOREST))
        assertEquals(MusicTrack.AUTUMN, MusicTrack.forScene(GameScene.AUTUMN))
    }

    @Test
    fun losNivelesSiguenASuSector() {
        // La musica acompaña al escenario de la partida. Como los escenarios
        // ahora van por sectores, el nivel 1 y el 11 ya no suenan igual.
        assertEquals(MusicTrack.MORNING, trackFor(1))
        assertEquals(MusicTrack.MORNING, trackFor(5))
        assertEquals(MusicTrack.NIGHT, trackFor(20))    // la puerta avisa de la noche
        assertEquals(MusicTrack.NIGHT, trackFor(21))    // empieza el sector de noche
        assertEquals(MusicTrack.FOREST, trackFor(31))
        assertEquals(MusicTrack.AUTUMN, trackFor(41))
    }

    @Test
    fun dentroDeUnSectorSuenaSuAmbienteYEnLaPuertaElSiguiente() {
        GameScene.allSectors().forEach { sector ->
            val interior = (sector.from until sector.to).map { MusicTrack.forScene(GameScene.forLevel(it)) }
            assertEquals(
                "dentro de '${sector.name}' deberia sonar siempre ${sector.scene.name}",
                setOf(MusicTrack.forScene(sector.scene)),
                interior.toSet()
            )
            val siguiente = GameScene.allSectors().firstOrNull { it.from == sector.to + 1 }
            if (siguiente != null) {
                assertEquals(
                    "la puerta ${sector.gateLevel} deberia sonar como el sector siguiente",
                    MusicTrack.forScene(siguiente.scene),
                    MusicTrack.forScene(GameScene.forLevel(sector.gateLevel))
                )
            }
        }
    }

    @Test
    fun dailyChallengeUsesNightAndZenUsesZenTrack() {
        assertEquals(MusicTrack.NIGHT, trackFor(2, daily = true))
        assertEquals(GameScene.NIGHT, GameScene.forMode(2, isDailyChallenge = true, isZenMode = false))
        assertEquals(GameScene.FOREST, GameScene.forMode(2, isDailyChallenge = false, isZenMode = true))
    }

    @Test
    fun menuAndZenTracksExist() {
        // El enum debe seguir apuntando a recursos distintos y existentes.
        assertEquals(6, MusicTrack.entries.size)
        assertEquals(6, MusicTrack.entries.map { it.resId }.distinct().size)
    }
}
