package com.cozy.herbshop.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El album tiene que contar la misma historia que las cartas y los sectores.
 *
 * Estas pruebas vigilan lo que se rompe facil al cambiar textos: que no se
 * pierda ninguna carta, que las cinco paginas cubran los cinco tramos y que los
 * recuerdos se abran en el orden en que el jugador avanza.
 */
class AlbumPagesTest {

    @Test
    fun hayCincoPaginasUnaPorSector() {
        assertEquals("el camino tiene cinco tramos", 5, AlbumPages.ALL.size)
        assertEquals(
            "cada pagina tiene un id distinto",
            AlbumPages.ALL.size,
            AlbumPages.ALL.map { it.id }.distinct().size
        )
        assertEquals(
            "las paginas cubren del nivel 1 al 50 sin huecos",
            listOf(1 to 10, 11 to 20, 21 to 30, 31 to 40, 41 to 50),
            AlbumPages.ALL.map { it.startLevel to it.gateLevel }
        )
    }

    @Test
    fun lasOchoCartasSiguenDentroDelAlbum() {
        val usadas = AlbumPages.ALL.map { it.customerLetterId } +
            AlbumPages.ALL.mapNotNull { it.recipe.letterId }

        assertEquals(
            "cada carta debe aparecer una sola vez",
            usadas.size,
            usadas.distinct().size
        )
        assertEquals(
            "no puede quedar ninguna carta fuera del album",
            JournalLetters.TODAS.map { it.id }.toSet(),
            usadas.toSet()
        )
    }

    @Test
    fun cadaPaginaTieneLosCuatroRecuerdosYUnaRecetaCoherente() {
        AlbumPages.ALL.forEach { page ->
            assertTrue("${page.id} necesita una receta", page.recipe.name.isNotBlank())
            assertTrue("${page.id} necesita ingredientes", page.recipe.ingredients.isNotEmpty())
            assertTrue("${page.id} necesita una hierba del reparto", !page.herb.esSecreta)
            assertTrue(
                "${page.id} debe incluir su hierba en la receta",
                page.herb in page.recipe.ingredients
            )
            assertTrue("${page.id} necesita un sello", page.stampName.isNotBlank())
        }
    }

    @Test
    fun laPaginaSeAbreAlEntrarAlSectorYSeCompletaAlCerrarLaPuerta() {
        val page = AlbumPages.ALL.first()

        val alEmpezar = AlbumPages.progress(page, currentLevel = 1, unlockedLetters = emptySet())
        assertTrue("la hierba se guarda al entrar", alEmpezar.herb)
        assertFalse("la receta llega a mitad de sector", alEmpezar.recipe)
        assertFalse("el cliente llega con la puerta", alEmpezar.customer)
        assertFalse("el sello llega con la puerta", alEmpezar.stamp)

        val mitad = AlbumPages.progress(page, currentLevel = 6, unlockedLetters = emptySet())
        assertTrue("la receta se guarda a mitad de sector", mitad.recipe)
        assertEquals("a mitad de sector hay dos recuerdos", 2, mitad.unlockedCount)

        val puerta = AlbumPages.progress(
            page,
            currentLevel = 11,
            unlockedLetters = setOf("letter_amanecer")
        )
        assertTrue("el cliente se guarda al cerrar la puerta", puerta.customer)
        assertTrue("el sello se guarda al cerrar la puerta", puerta.stamp)
        assertTrue("la pagina queda completa", puerta.isComplete)
        assertEquals("la pagina completa tiene cuatro recuerdos", 4, puerta.unlockedCount)
    }

    @Test
    fun laCartaTambienDesbloqueaElClienteSiElNivelAunNoSeActualizo() {
        val page = AlbumPages.ALL[1]
        val progreso = AlbumPages.progress(
            page,
            currentLevel = page.gateLevel,
            unlockedLetters = setOf(page.customerLetterId)
        )

        assertTrue("la carta recibida basta para guardar al cliente", progreso.customer)
        assertTrue("el sello acompaña a la carta", progreso.stamp)
    }

    @Test
    fun lasPaginasFuturasNoSumanRecuerdos() {
        val progreso = AlbumPages.progress(
            AlbumPages.ALL.last(),
            currentLevel = 39,
            unlockedLetters = JournalLetters.TODAS.map { it.id }.toSet()
        )

        assertEquals("una pagina que aun no se ha abierto no cuenta", 0, progreso.unlockedCount)
        assertEquals(
            "el album no puede adelantar el otono",
            14,
            AlbumPages.totalProgress(
                currentLevel = 39,
                unlockedLetters = emptySet()
            )
        )
    }

    @Test
    fun elTotalDelAlbumLlegaAVeinteYLaPaginaActualSigueAlNivel() {
        assertEquals("cinco paginas por cuatro huecos", 20, AlbumPages.TOTAL_SLOTS)
        assertEquals(
            "con todo desbloqueado el album esta lleno",
            20,
            AlbumPages.totalProgress(
                currentLevel = 51,
                unlockedLetters = JournalLetters.TODAS.map { it.id }.toSet()
            )
        )
        assertEquals("el nivel 1 abre la primera pagina", 0, AlbumPages.pageIndexForLevel(1))
        assertEquals("el nivel 10 sigue en Amanecer", 0, AlbumPages.pageIndexForLevel(10))
        assertEquals("el nivel 11 abre Invernadero", 1, AlbumPages.pageIndexForLevel(11))
        assertEquals("el nivel 39 esta en Bosque", 3, AlbumPages.pageIndexForLevel(39))
        assertEquals("el nivel 51 se queda en Otono", 4, AlbumPages.pageIndexForLevel(51))
    }
}
