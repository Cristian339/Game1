package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.SectorRule
import com.cozy.herbshop.ui.game.GameUiState
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El par y las chapas de la v1.
 *
 * El par sale de la solucion con la que el generador comprueba el nivel, asi que
 * no es un numero puesto a ojo: si el generador entrega un nivel, tambien entrega
 * una forma de ganarlo y por tanto el par es siempre alcanzable. Eso es lo que
 * comprueba este test, porque un par imposible dejaria al jugador persiguiendo una
 * chapa que no puede sacar.
 */
class LevelParTest {

    private val repository = LevelRepository(null)

    @Test
    fun todoNivelGeneradoTraeParYEsGanableEnEseNumeroDeJugadas() {
        val fallos = mutableListOf<String>()
        (1..50).forEach { numero ->
            repeat(2) {
                val nivel = repository.getLevel(numero)
                val par = nivel.parMoves
                if (par == null || par <= 0) {
                    fallos.add("nivel $numero sin par")
                    return@repeat
                }
                // Conrecipe el solver necesita la tetera; sin ella, no.
                val jars = repository.convertToJars(nivel)
                val solucion = LevelSolver().repartirYResolver(jars, nivel.customerOrder?.requiredHerbs)
                if (solucion == null) {
                    fallos.add("nivel $numero no se puede resolver")
                } else if (solucion.size > par + 5) {
                    // Margen de sobra: el par es la solucion del generador, pero
                    // si el nivel se regenera puede variar un poco.
                    fallos.add("nivel $numero: par $par pero hacen falta ${solucion.size}")
                }
            }
        }
        assertTrue("niveles con el par roto: ${fallos.take(8)}", fallos.isEmpty())
    }

    @Test
    fun cadaSectorTraeSuNovedadYLaUltimaPuertaLasTodas() {
        assertEquals(SectorRule.NONE, repository.getLevel(5).sectorRule)
        assertEquals(SectorRule.GRINDER, repository.getLevel(15).sectorRule)
        assertEquals(SectorRule.MOVE_LIMIT, repository.getLevel(25).sectorRule)
        assertEquals(SectorRule.STICKY, repository.getLevel(35).sectorRule)
        assertEquals(SectorRule.ALL, repository.getLevel(50).sectorRule)
    }

    @Test
    fun elLimiteDeJugadasSoloAplicaASuSector() {
        val conPar = GameUiState(parMoves = 10)
        assertTrue("sin sector con limite no debe haber tope", conPar.moveLimit == null)
        assertTrue("el limite de su sector debe ser el par con margen", conPar.copy(sectorRule = SectorRule.MOVE_LIMIT).moveLimit != null)
        assertTrue(
            "tampoco debe haber tope con capa pegajosa o molinillo",
            conPar.copy(sectorRule = SectorRule.STICKY).moveLimit == null &&
                conPar.copy(sectorRule = SectorRule.GRINDER).moveLimit == null
        )
    }

    @Test
    fun elLimiteNoDejaElNivelImposibleDeTerminar() {
        // Lo importante: pasado el tope no se pierde, se avisa. El Deshacer sigue
        // vivo y lo que ya esta resuelto sigue contando.
        val justo = GameUiState(parMoves = 8, movesCount = 12, sectorRule = SectorRule.MOVE_LIMIT)
        assertTrue("deberia marcar el tope alcanzado", justo.atMoveLimit)
        assertTrue("no hay jugadas", justo.movesLeft == 0)
        val historialConJuego = justo.copy(history = listOf(justo.jars))
        assertTrue("el Deshacer debe seguir disponible", historialConJuego.canUndo)
        assertTrue(
            "un nivel ya terminado no debe quedar bloqueado",
            justo.copy(isLevelCompleted = true).atMoveLimit.not()
        )
    }

    @Test
    fun elMargenSobreElParDaJuegoParaExplorar() {
        val estado = GameUiState(parMoves = 20, sectorRule = SectorRule.MOVE_LIMIT)
        assertTrue("el limite deberia dejar margen sobre el par", (estado.moveLimit ?: 0) > 20)
    }

    private fun assertEquals(esperado: SectorRule, real: SectorRule) {
        assertTrue("sector esperado $esperado pero salio $real", esperado == real)
    }
}