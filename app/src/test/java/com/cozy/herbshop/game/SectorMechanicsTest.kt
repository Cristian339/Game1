package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.SectorRule
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cada sector anuncia una mecanica nueva y tiene que traerla de verdad en todos
 * sus niveles, no solo en el estreno. La ficha del mapa y el tutorial se apoyan
 * en la regla del sector; si un nivel del 31 al 40 saliera sin miel, el jugador
 * veria un tutorial que no sirve para nada y la ficha mentiria.
 *
 * Se leen los niveles con el mismo parser que la partida, para contar tambien
 * las capas heladas, pegajosas y ocultas que traen los ids.
 */
class SectorMechanicsTest {

    private val repository = LevelRepository(null)

    private fun jarsDe(numero: Int) = repository.convertToJars(repository.getLevel(numero))

    @Test
    fun elSectorDelHieloTraeFrioEnTodosSusNiveles() {
        (11..20).forEach { numero ->
            val jars = jarsDe(numero)
            assertTrue(
                "el nivel $numero anuncia frascos helados y no trae ninguna capa helada",
                jars.any { it.hasFrozen }
            )
        }
    }

    @Test
    fun elSectorDelBosqueTraeMielEnTodosSusNiveles() {
        (31..40).forEach { numero ->
            val jars = jarsDe(numero)
            assertTrue(
                "el nivel $numero anuncia capas pegajosas y no trae ninguna",
                jars.any { it.hasSticky }
            )
        }
    }

    @Test
    fun elSectorDeLaNocheTraeMolinilloEnTodosSusNiveles() {
        (21..30).forEach { numero ->
            val jars = jarsDe(numero)
            assertTrue(
                "el nivel $numero anuncia molinillo y no lo trae",
                jars.any { it.isGrinder }
            )
        }
    }

    @Test
    fun elSectorFinalTraeTodasLasMecanicasJuntas() {
        (41..50).forEach { numero ->
            val jars = jarsDe(numero)
            assertTrue("nivel $numero de Todo sin hielo", jars.any { it.hasFrozen })
            assertTrue("nivel $numero de Todo sin miel", jars.any { it.hasSticky })
            assertTrue("nivel $numero de Todo sin molinillo", jars.any { it.isGrinder })
            assertTrue(
                "nivel $numero de Todo sin capa misteriosa",
                jars.any { jar -> jar.layers.any { it.isHidden } }
            )
            assertTrue(
                "nivel $numero deberia llevar tope de jugadas",
                SectorRule.forLevel(numero).limitaJugadas
            )
        }
    }
}
