package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Jar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regla del jugador: los frascos vacios se llenan y, como mucho, queda uno
 * esperando. La tetera y el molinillo son herramientas, no almacen, asi que no
 * cuentan como frascos vacios.
 *
 * La excepcion son las puertas 40 y 50: suman a las seis especies de siempre
 * las de su receta (siete y ocho colores) y con un solo hueco no queda ningun
 * tablero que el solver pueda dar por bueno sin quitarles el misterio y los
 * topes de su sector; llevan un vacio de mas. Los niveles fijos del json se
 * comprueban en [PredefinedLevelsTest], leyendo el archivo de verdad.
 */
class EmptyJarsTest {

    private val repository = LevelRepository(null)

    private fun vaciosDeAlmacen(jars: List<Jar>): Int =
        jars.count { it.layers.isEmpty() && !it.isTeapot && !it.isGrinder }

    @Test
    fun ningunNivelGeneradoTraeMasDeUnFrascoVacioEsperando() {
        val fallos = mutableListOf<String>()
        (6..50).forEach { numero ->
            val jars = repository.convertToJars(repository.getLevel(numero))
            val vacios = vaciosDeAlmacen(jars)
            val esperado = if (numero in setOf(40, 50)) 2 else 1
            if (vacios != esperado) {
                fallos.add("nivel $numero con $vacios vacios (esperado $esperado)")
            }
        }
        assertTrue("niveles con frascos vacios de mas: $fallos", fallos.isEmpty())
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
