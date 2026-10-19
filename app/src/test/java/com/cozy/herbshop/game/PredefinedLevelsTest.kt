package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.GameLevel
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Comprobacion de que los niveles fijos de assets/levels.json se pueden
 * terminar. Es importante porque un nivel fijo sin solucion deja la partida
 * bloqueada: no hay generador que lo rescate.
 *
 * El test lee el archivo de verdad, el mismo que empaqueta la app: antes
 * repetia los tableros a mano y validaba esa copia, asi que un cambio en el
 * json (o un tablero viejo que se queda atras) no se detectaba. En los tests
 * sin contexto LevelRepository genera los niveles 1-5 en vez de leer el json,
 * por eso el archivo se lee aparte con la misma fabrica de Gson que usa el
 * repositorio.
 */
class PredefinedLevelsTest {

    private val repository = LevelRepository(context = null)
    private val solver = LevelSolver()

    private fun nivelesDelJson(): List<GameLevel> {
        val json = File("src/main/assets/levels.json")
        assertTrue("no se encuentra levels.json (${json.absolutePath})", json.exists())
        val type = object : TypeToken<List<GameLevel>>() {}.type
        return LevelRepository.gsonDeNiveles().fromJson(json.readText(), type)
    }

    /** Quita los prefijos con los que el json marca misterio, congelado, etc. */
    private fun idLimpio(raw: String): String = raw
        .removePrefix("sealed:")
        .removePrefix("frozen:")
        .removePrefix("sticky:")
        .removePrefix("?")
        .removePrefix("mystery:")

    /**
     * El nivel tal y como lo juega la partida: con la receta pegada. Sin ella
     * convertToJars no reserva la tetera y el nivel 4 contaria un vacio de mas.
     */
    private fun comoLoJuegaLaPartida(nivel: GameLevel): GameLevel =
        nivel.copy(customerOrder = repository.orderForLevel(nivel.levelNumber))

    @Test
    fun losNivelesFijosSePuedenTerminar() {
        val rotos = mutableListOf<String>()
        nivelesDelJson().forEach { nivel ->
            val enPartida = comoLoJuegaLaPartida(nivel)
            val jars = repository.convertToJars(enPartida)
            if (!solver.isSolvable(jars, enPartida.customerOrder?.requiredHerbs)) {
                rotos.add(nivel.levelNumber.toString())
            }
        }
        assertTrue(
            "niveles fijos imposibles con su receta: ${rotos.joinToString()}. " +
                "Un nivel fijo irresoluble deja al jugador sin poder seguir.",
            rotos.isEmpty()
        )
    }

    @Test
    fun laRecetaDeCadaNivelFijoEstaEnSuTablero() {
        // El unico nivel fijo con receta es el 4 (el primero con tetera); tiene
        // que traer sus hierbas o el pedido no se podria servir de ninguna
        // manera.
        nivelesDelJson().forEach { nivel ->
            val receta = repository.orderForLevel(nivel.levelNumber)?.requiredHerbs
                ?: return@forEach
            val enElTablero = nivel.jars.flatten().map { idLimpio(it) }.toSet()
            receta.forEach { hierba ->
                assertTrue(
                    "el nivel fijo ${nivel.levelNumber} no trae ${hierba.id}, que su receta pide",
                    hierba.id in enElTablero
                )
            }
        }
    }

    @Test
    fun losNivelesFijosDejanUnSoloFrascoVacio() {
        // Regla del jugador: los frascos vacios se llenan y como mucho queda uno
        // esperando. La tetera (en el nivel 4, con receta, uno de los vacios se
        // reserva para ella) es una herramienta, no almacen, y no cuenta.
        nivelesDelJson().forEach { nivel ->
            val vacios = repository.convertToJars(comoLoJuegaLaPartida(nivel))
                .count { it.layers.isEmpty() && !it.isTeapot && !it.isGrinder }
            assertEquals(
                "el nivel fijo ${nivel.levelNumber} deberia dejar un solo frasco vacio",
                1,
                vacios
            )
        }
    }
}
