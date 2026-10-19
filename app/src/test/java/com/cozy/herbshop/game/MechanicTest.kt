package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.cozy.herbshop.data.model.SectorRule
import com.cozy.herbshop.ui.map.LevelCatalog
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tutoriales de mecanica.
 *
 * El jugador dijo que jugando no se enteraba de las mecanicas, asi que cada una
 * se explica la primera vez que aparece. Lo que se vigila aqui es que el nivel
 * del estreno sea de verdad el primero que la trae (si no, el tutorial saldria
 * tarde o explicaria algo que no esta) y que el mapa anuncie el mismo nivel.
 */
class MechanicTest {

    private val repository = LevelRepository(null)

    private fun jarsDe(numero: Int): List<Jar> =
        repository.convertToJars(repository.getLevel(numero))

    @Test
    fun cadaMecanicaTieneUnEstrenoDistintoYEnOrden() {
        val estrenos = Mechanic.entries.map { it.introLevel }
        assertEquals("ningun nivel puede estrenar dos mecanicas", estrenos.distinct(), estrenos)
        assertEquals("los estrenos van en orden de dificultad", estrenos.sorted(), estrenos)
        assertEquals(
            "los ids tienen que ser unicos (se guardan en preferencias)",
            Mechanic.entries.size,
            Mechanic.entries.map { it.id }.distinct().size
        )
    }

    @Test
    fun elPrimerNivelQueTraeCadaMecanicaEsSuEstreno() {
        val primero = mutableMapOf<Mechanic, Int>()
        (1..50).forEach { numero ->
            val jars = jarsDe(numero)
            val regla = SectorRule.forLevel(numero)
            Mechanic.entries.forEach { mecanica ->
                if (mecanica !in primero && mecanica.isPresentIn(jars, regla)) {
                    primero[mecanica] = numero
                }
            }
        }
        Mechanic.entries.forEach { mecanica ->
            assertEquals(
                "el primer nivel con ${mecanica.id} deberia ser su estreno, " +
                    "para que el tutorial salga cuando toca",
                mecanica.introLevel,
                primero[mecanica]
            )
        }
    }

    @Test
    fun losNivelesFijosTraenLaMecanicaQueEstrenan() {
        // Los fijos de assets/levels.json no pasan por el generador, asi que su
        // garantia hay que mirarla en el archivo de verdad.
        val json = File("src/main/assets/levels.json")
        assertTrue("no se encuentra levels.json (${json.absolutePath})", json.exists())
        val type = object : TypeToken<List<GameLevel>>() {}.type
        val niveles: List<GameLevel> = LevelRepository.gsonDeNiveles().fromJson(json.readText(), type)
        niveles.forEach { nivel ->
            val estreno = Mechanic.entries.firstOrNull { it.introLevel == nivel.levelNumber }
                ?: return@forEach
            val enPartida = nivel.copy(customerOrder = repository.orderForLevel(nivel.levelNumber))
            val jars = repository.convertToJars(enPartida)
            assertTrue(
                "el nivel fijo ${nivel.levelNumber} anuncia ${estreno.id} pero no lo trae",
                estreno.isPresentIn(jars, enPartida.sectorRule)
            )
        }
    }

    @Test
    fun seExplicaLaMecanicaMasTempranaQueElJugadorNoHaVisto() {
        // Un tablero con todas las novedades juntas, para elegir cual toca.
        val jars = listOf(
            Jar(
                id = 0,
                capacity = 4,
                layers = listOf(
                    JarLayer(Herb.MINT),
                    JarLayer(Herb.MINT, isSticky = true),
                    JarLayer(Herb.MINT, isFrozen = true),
                    JarLayer(Herb.MINT)
                )
            ),
            Jar(id = 1, capacity = 4, layers = listOf(JarLayer(Herb.MINT, isHidden = true))),
            Jar(id = 2, capacity = 2, layers = emptyList(), isTeapot = true)
        )
        val todas = Mechanic.entries.map { it.id }.toSet()

        assertEquals(Mechanic.MYSTERY, Mechanic.toExplain(jars, SectorRule.NONE, todas - "mystery"))
        assertEquals(Mechanic.TEAPOT, Mechanic.toExplain(jars, SectorRule.NONE, todas - "teapot"))
        assertEquals(Mechanic.FROZEN, Mechanic.toExplain(jars, SectorRule.NONE, todas - "frozen"))
        assertEquals(Mechanic.STICKY, Mechanic.toExplain(jars, SectorRule.NONE, todas - "sticky"))
        // El tope de jugadas no esta en los frascos: lo pone la regla del sector.
        assertEquals(
            Mechanic.MOVE_LIMIT,
            Mechanic.toExplain(jars, SectorRule.MOVE_LIMIT, todas - "move_limit")
        )
        assertNull(
            "si ya se las sabe todas no hay tutorial que dar",
            Mechanic.toExplain(jars, SectorRule.NONE, todas)
        )
    }

    @Test
    fun laFichaDelMapaAnunciaDondeSeEstrenaCadaMecanica() {
        val catalogo = LevelCatalog(LevelRepository(null))
        Mechanic.entries.forEach { mecanica ->
            assertEquals(
                "el nivel ${mecanica.introLevel} deberia anunciar el estreno de ${mecanica.id}",
                mecanica,
                catalogo[mecanica.introLevel].newMechanic
            )
            assertEquals(
                "solo un nivel puede estrenar ${mecanica.id}",
                1,
                catalogo.all().count { it.newMechanic == mecanica }
            )
        }
        assertNull("el nivel 1 no estrena ninguna mecanica", catalogo[1].newMechanic)
    }
}
