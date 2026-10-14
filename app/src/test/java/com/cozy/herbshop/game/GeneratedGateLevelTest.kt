package com.cozy.herbshop.game

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regresion de los niveles puerta.
 *
 * El generador era el problema de verdad: validaba el nivel con el solver y
 * despues le anadia misterio, congelado y un frasco sellado. Una capa misteriosa
 * corta la rafaga de vertido y un frasco sellado no se puede tocar, asi que el
 * jugador se encontraba un puzle mas dificil que el que se habia dado por
 * bueno. Con niveles de nivel 28 salia aproximadamente un 1,5% de niveles
 * irresolubles (4 de 300); con la validacion sobre el nivel final, 0 de 600.
 *
 * Un nivel puerta irresoluble es grave: el jugador no puede avanzar ni
 * desbloquear la hierba siguiente, asi que no basta con que sea raro.
 */
class GeneratedGateLevelTest {

    @Test
    fun losNivelesPuertaSalenGanables() {
        val repository = LevelRepository(null)
        val solver = com.cozy.herbshop.game.LevelSolver()
        val fallos = mutableListOf<String>()
        // Las cinco puertas son las que cierran cada sector. Se prueban todas:
        // si una sale irresoluble, el jugador se queda atascado ahi.
        val muestras = 40
        LevelRepository.GATE_LEVELS.sorted().forEach { numero ->
            repeat(muestras) { intento ->
                val level = repository.getLevel(numero)
                val jars = repository.convertToJars(level)
                if (jars.any { it.isTeapot } &&
                    solver.solve(jars, level.customerOrder?.requiredHerbs) == null
                ) {
                    fallos.add("nivel $numero intento $intento")
                }
            }
        }
        assertTrue(
            "niveles puerta irresolubles: ${fallos.take(5)}",
            fallos.isEmpty()
        )
    }

    @Test
    fun elNivelPuertaTraeLasHierbasDeSuRecetaAunqueNoEstenDesbloqueadas() {
        // La puerta 10 pide romero y regala romero. Ese romero todavia no esta en
        // el herbario del jugador, asi que el generador tiene que meterlo en el
        // nivel igualmente: si no, la puerta seria imposible de superar y el
        // jugador se quedaria atascado sin poder avanzar.
        val repository = LevelRepository(null)
        var herbario = HerbUnlocks.INITIAL
        LevelRepository.GATE_LEVELS.sorted().forEach { numero ->
            val receta = repository.orderForLevel(numero)?.requiredHerbs.orEmpty()
            repeat(5) {
                val jars = repository.convertToJars(repository.getLevel(numero))
                val enElNivel = jars.flatMap { jar -> jar.layers.map { capa -> capa.herb.id } }.toSet()
                receta.forEach { hierba ->
                    assertTrue(
                        "el nivel $numero no trae ${hierba.id}, que su receta pide",
                        hierba.id in enElNivel
                    )
                }
            }
            val premio = HerbUnlocks.nextLocked(herbario)
            if (premio != null) herbario = herbario + premio.id
        }
    }

    @Test
    fun elGeneradorValidaElNivelQueEntregaNoUnoMasFacil() {
        // El generador no puede devolver un nivel que el solver no sepa resolver:
        // si se queda sin presupuesto, es mejor seguir probando que enviar uno
        // que puede tener la partida bloqueada.
        val generator = LevelGenerator()
        val solver = LevelSolver()
        val repository = LevelRepository(null)
        val recipe = listOf(
            com.cozy.herbshop.data.model.Herb.CHAMOMILE,
            com.cozy.herbshop.data.model.Herb.CHAMOMILE,
            com.cozy.herbshop.data.model.Herb.ROSEMARY,
            com.cozy.herbshop.data.model.Herb.ROSEMARY
        )
        repeat(15) { intento ->
            val level = generator.generateSolvableLevel(
                levelNumber = 50,
                numHerbTypes = 8,
                emptyJars = 2,
                jarCapacity = 4,
                random = kotlin.random.Random(1000 + intento),
                requiredOrder = recipe
            )
            // Con la receta puesta, convertToJars reserva la tetera; sin ella el
            // nivel no tendria donde mezclar y el test no comprobaría nada.
            val jars = repository.convertToJars(
                level.copy(
                    customerOrder = com.cozy.herbshop.data.model.CustomerOrder(
                        id = "gate_50",
                        customerName = "Test",
                        dialogue = "",
                        recipeName = "Test",
                        requiredHerbs = recipe
                    )
                )
            )
            assertTrue(
                "nivel puerta generado sin solucion en la semilla ${1000 + intento}",
                solver.solve(jars, recipe) != null
            )
        }
    }
}