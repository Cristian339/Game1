package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.JournalLetters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El diario: una carta por sector, entregada al terminar la puerta del tramo.
 */
class JournalTest {

    @Test
    fun hayUnaCartaPorPuertaYNingunaPuertaSeQuedaSinCarta() {
        val puertas = (1..50).filter { LevelRepository.isGateLevel(it) }
        assertEquals(
            "el diario tiene que traer una carta por puerta, ni mas ni menos",
            puertas,
            JournalLetters.PUERTAS
        )
    }

    @Test
    fun lasCartasNoSeRepitenYLleganConTexto() {
        val ids = JournalLetters.TODAS.map { it.id }
        assertEquals("hay cartas repetidas en el diario", ids.size, ids.distinct().size)
        assertTrue(
            "las cartas iniciales tienen que estar en la lista del diario",
            ids.containsAll(JournalLetters.INICIALES)
        )
        JournalLetters.TODAS.forEach { carta ->
            assertTrue("la carta ${carta.id} llega sin texto", carta.content.isNotBlank())
            assertTrue("la carta ${carta.id} llega sin titulo", carta.letterTitle.isNotBlank())
            assertTrue("la carta ${carta.id} llega sin firma", carta.senderName.isNotBlank())
            assertTrue("la carta ${carta.id} llega sin avatar", carta.senderAvatar.isNotBlank())
        }
    }

    @Test
    fun alTerminarLaPuertaLlegaLaCartaDeSuSector() {
        JournalLetters.PUERTAS.forEach { puerta ->
            val carta = JournalLetters.alTerminarPuerta(puerta)
            assertEquals(
                "la puerta $puerta no entrega la carta que le toca",
                puerta,
                carta?.gate
            )
            assertTrue(
                "la carta de la puerta $puerta no puede ser de las iniciales",
                carta!!.id !in JournalLetters.INICIALES
            )
        }
        assertTrue(
            "un nivel que no es puerta no puede entregar carta",
            JournalLetters.alTerminarPuerta(7) == null
        )
    }
}
