package com.cozy.herbshop.ui

import com.cozy.herbshop.data.model.HelpItem
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas del inventario de ayudas: los tres contadores, el aviso al agotarse y
 * los precios de los packs. Si una ayuda se puede usar sin stock, el jugador
 * vuelve a encontrarse con un boton que no hace nada.
 */
class HelpStockTest {

    private val startingStock = UserPreferencesRepository.STARTING_HELP_STOCK

    @Test
    fun todaAyudaArrancaConUsos() {
        HelpItem.entries.forEach { help ->
            assertNotNull("falta stock inicial para $help", startingStock[help.id])
            assertTrue("$help no puede empezar sin usos", startingStock[help.id]!! > 0)
        }
    }

    @Test
    fun losPackCuestanMasQueCederElUsoPorAnuncio() {
        // El anuncio da 1 uso gratis; el pack debe ser peor negocio por unidad,
        // o nadie tendria motivo para ver el anuncio.
        HelpItem.entries.forEach { help ->
            val perUse = help.packPrice.toDouble() / help.packSize
            assertTrue(
                "$help: el pack sale a $perUse por uso, no compensa frente al anuncio",
                perUse > 1.0
            )
        }
    }

    @Test
    fun losPreciosSubenPorPotencia() {
        // Deshacer es la ayuda basica y la que mas cuesta es la de +Frasco.
        assertTrue(HelpItem.UNDO.packPrice <= HelpItem.HINT.packPrice)
        assertTrue(HelpItem.HINT.packPrice < HelpItem.EXTRA_JAR.packPrice)
    }

    @Test
    fun packSizeEsPositivoYLosIdsSonUnicos() {
        HelpItem.entries.forEach { help ->
            assertTrue("${help.id} con pack vacio", help.packSize > 0)
            assertTrue("${help.id} sin precio", help.packPrice > 0)
        }
        assertEquals(HelpItem.entries.size, HelpItem.entries.map { it.id }.toSet().size)
    }

    @Test
    fun fromIdResuelveCadaAyudaYDescartaLoDesconocido() {
        HelpItem.entries.forEach { help ->
            assertEquals(help, HelpItem.fromId(help.id))
        }
        assertNull(HelpItem.fromId("no_existe"))
    }
}