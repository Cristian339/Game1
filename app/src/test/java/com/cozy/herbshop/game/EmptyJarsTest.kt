package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Jar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regla del jugador: los frascos vacios se llenan y queda el minimo de maniobra.
 * La tetera y el molinillo son herramientas, no almacen, asi que no cuentan como
 * frascos vacios.
 *
 * La cuenta sale de [LevelRepository.emptyJarsForLevel], que es la misma que usa
 * el generador y el mapa: uno hasta seis colores, dos cuando hay siete u ocho o
 * cuando el sector del hielo bloquea un frasco entero (con un solo hueco el
 * solver no encuentra tablero que cerrar). Los niveles fijos del json se
 * comprueban en [PredefinedLevelsTest], leyendo el archivo de verdad.
 */
class EmptyJarsTest {

    private val repository = LevelRepository(null)

    private fun vaciosDeAlmacen(jars: List<Jar>): Int =
        jars.count { it.layers.isEmpty() && !it.isTeapot && !it.isGrinder }

    @Test
    fun ningunNivelGeneradoTraeMasFrascosVaciosDeLosQuePideSuSector() {
        val fallos = mutableListOf<String>()
        (6..50).forEach { numero ->
            val jars = repository.convertToJars(repository.getLevel(numero))
            val vacios = vaciosDeAlmacen(jars)
            val esperado = repository.emptyJarsForLevel(numero)
            if (vacios != esperado) {
                fallos.add("nivel $numero con $vacios vacios (esperado $esperado)")
            }
        }
        assertTrue("frascos vacios que no cuadran: $fallos", fallos.isEmpty())
    }

    @Test
    fun elZenYElRetoDiarioTambienDejanUnSoloFrascoVacio() {
        assertEquals(
            "el modo zen deberia dejar un solo frasco vacio",
            1,
            vaciosDeAlmacen(repository.convertToJars(repository.getZenLevel()))
        )
        listOf(7, 77, 200, 366).forEach { dia ->
            assertEquals(
                "el reto del dia $dia deberia dejar un solo frasco vacio",
                1,
                vaciosDeAlmacen(repository.convertToJars(repository.getDailyChallengeLevel(seed = dia)))
            )
        }
    }
}
