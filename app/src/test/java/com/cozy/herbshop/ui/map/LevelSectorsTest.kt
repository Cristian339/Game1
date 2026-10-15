package com.cozy.herbshop.ui.map

import com.cozy.herbshop.ui.components.GameScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los sectores son la estructura del mapa: un tramo con su ambiente que cierra
 * con una puerta de hierba. Antes los escenarios daban vueltas cada 5 niveles
 * (Amanecer, Invernadero, Noche, Bosque, Otoño, y otra vez Amanecer...), y el
 * mapa no contaba nada. Estos tests fijan la sectorizacion.
 */
class LevelSectorsTest {

    private val sectores = GameScene.allSectors()

    @Test
    fun losSectoresCubrenLosCincuentaNivelesSinHuecos() {
        assertEquals("se esperaban 5 sectores", 5, sectores.size)
        assertEquals("el primer sector deberia empezar en 1", 1, sectores.first().from)
        assertEquals("el ultimo deberia acabar en 50", 50, sectores.last().to)

        var esperado = 1
        sectores.forEach { sector ->
            assertEquals("hueco o solape antes de ${sector.from}", esperado, sector.from)
            assertTrue("sector ${sector.from}-${sector.to} vacio", sector.to >= sector.from)
            esperado = sector.to + 1
        }
    }

    @Test
    fun laPuertaDeHierbaCaeAlFinalDeCadaSector() {
        // Es lo que hace que el tramo signifique algo: cerrar el sector ES
        // conseguir la hierba nueva.
        val puertas = com.cozy.herbshop.game.LevelRepository.GATE_LEVELS
        sectores.forEach { sector ->
            assertTrue(
                "el nivel ${sector.to} deberia ser puerta de hierba",
                sector.gateLevel in puertas
            )
        }
        assertEquals(
            "deberia haber una puerta por sector",
            sectores.size,
            puertas.size
        )
    }

    @Test
    fun cadaNivelCaeEnSuSector() {
        (1..50).forEach { nivel ->
            val sector = GameScene.sectorOf(nivel)
            assertTrue("el nivel $nivel no cae en su sector", nivel in sector.from..sector.to)
        }
    }

    @Test
    fun losNivelesFueraDeRangoSeQuedanEnElPrimerSector() {
        assertEquals(sectores.first().name, GameScene.sectorOf(0).name)
        assertEquals(sectores.first().name, GameScene.sectorOf(-5).name)
        assertEquals(sectores.last().name, GameScene.sectorOf(999).name)
    }

    @Test
    fun todoElSectorUsaSuMismoEscenarioSalvoLaPuerta() {
        // Un bioma por sector. Antes se alternaba con un patron 2:1 y el jugador
        // veía el nivel 3, el 6 y el 9 con otro fondo, asi que el tramo no se
        // identificaba de un vistazo.
        sectores.forEach { sector ->
            (sector.from..sector.to).forEach { nivel ->
                val esperado = if (nivel == sector.gateLevel) {
                    sectores.firstOrNull { it.from == sector.to + 1 }?.scene ?: sector.scene
                } else {
                    sector.scene
                }
                assertEquals(
                    "el nivel $nivel deberia verse en $esperado (sector '${sector.name}')",
                    esperado,
                    GameScene.forLevel(nivel)
                )
            }
        }
    }

    @Test
    fun laPuertaEnseñaElEscenarioDelSectorQueViene() {
        // La puerta es la unica que se sale de su sector, y lo hace a proposito:
        // avisa del bioma al que se entra despues.
        sectores.dropLast(1).forEach { sector ->
            val siguiente = sectores.first { it.from == sector.to + 1 }
            assertEquals(
                "la puerta ${sector.gateLevel} deberia enseñar ${siguiente.scene.displayName}",
                siguiente.scene,
                GameScene.forLevel(sector.gateLevel)
            )
        }
    }

    @Test
    fun laUltimaPuertaSeQuedaEnSuSector() {
        // El ultimo sector no tiene ninguno despues: no hay nada que advance.
        val ultimo = sectores.last()
        assertEquals(ultimo.scene, GameScene.forLevel(ultimo.gateLevel))
    }

    @Test
    fun losSectoresNoRepitenEscenarioPrincipal() {
        // Si dos sectores compartieran el mismo ambiente, el mapa
        // seguiria sin avanzar a simple vista.
        val escenas = sectores.map { it.scene }
        assertEquals(
            "hay sectores con el mismo ambiente",
            escenas.size,
            escenas.toSet().size
        )
    }

    @Test
    fun losSectoresContiguosNoSonIguales() {
        // Evita el salto de Amanecer a Amanecer que con el round-robin pasaba.
        sectores.zipWithNext().forEach { (a, b) ->
            assertNotEquals(
                "los sectores '${a.name}' y '${b.name}' son identicos",
                a.scene,
                b.scene
            )
        }
    }
}