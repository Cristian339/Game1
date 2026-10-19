package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.SectorRule
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Los niveles fijos de assets/levels.json tienen que cargarse enteros.
 *
 * Gson crea los data class sin pasar por el constructor y solo escribe los
 * campos que vienen en el json. Como el archivo no trae `sectorRule` (ni
 * `grinderJarIndex` ni `parMoves`), esos campos quedaban en null por muchos
 * valores por defecto que declararan: el primer copy() de un nivel fijo
 * reventaba con NullPointerException y pulsar JUGAR en el nivel 1 cerraba la
 * app entera. Ese copy() es justo el que hace LevelRepository.getLevel para
 * pegarle la receta al nivel.
 *
 * El test lee el json de verdad (el mismo archivo que empaqueta la app) con la
 * misma fabrica de Gson que usa el repositorio, para que un cambio en el
 * archivo o en la fabrica no se cuele.
 */
class JsonLevelsLoadTest {

    private val archivo = File("src/main/assets/levels.json")

    private fun nivelesDelArchivo(): List<GameLevel> {
        assertTrue("falta ${archivo.absolutePath}", archivo.exists())
        val type = object : TypeToken<List<GameLevel>>() {}.type
        return LevelRepository.gsonDeNiveles().fromJson(archivo.readText(), type)
    }

    @Test
    fun losNivelesDelJsonSeCopianComoLosUsaLaPartida() {
        val niveles = nivelesDelArchivo()
        assertTrue("el json deberia traer niveles", niveles.isNotEmpty())

        niveles.forEach { nivel ->
            // El copy() de getLevel: el camino que reventaba con NullPointerException.
            val copia = try {
                nivel.copy(customerOrder = null)
            } catch (e: Exception) {
                fail("el nivel ${nivel.levelNumber} no se puede copiar: $e")
                throw e
            }
            assertEquals("se perdio el numero de nivel", nivel.levelNumber, copia.levelNumber)
            assertEquals(
                "sectorRule del nivel ${nivel.levelNumber}: el json no lo trae, deberia quedar en NONE",
                SectorRule.NONE,
                copia.sectorRule
            )
            assertNull("grinderJarIndex del nivel ${nivel.levelNumber}", copia.grinderJarIndex)
            assertNull("parMoves del nivel ${nivel.levelNumber}", copia.parMoves)
            assertTrue("nivel ${nivel.levelNumber} sin frascos", copia.jars.isNotEmpty())
        }
    }
}
