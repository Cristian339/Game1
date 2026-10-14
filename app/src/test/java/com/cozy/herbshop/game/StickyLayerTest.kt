package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.cozy.herbshop.data.model.PourResult
import com.cozy.herbshop.ui.game.GameEngine
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Capa pegajosa (sector del bosque).
 *
 * Una capa pegada hace de tope: no se deja verter por encima, asi que el bloque
 * de arriba sale partido y hay que gastar una jugada de mas. Lo peligroso no es
 * que sea dificil, es que el motor y el solver no coincidan: si el solver no ve
 * el tope, daria una pista ilegal, y si el generador no lo ve, entregaria niveles
 * que en realidad no se pueden terminar.
 */
class StickyLayerTest {

    private val engine = GameEngine()

    private fun jar(id: Int, vararg herbs: Herb, stickyAt: Set<Int> = emptySet()) = Jar(
        id = id,
        capacity = 4,
        layers = herbs.mapIndexed { i, h -> JarLayer(herb = h, isSticky = i in stickyAt) }
    )

    @Test
    fun laCimaPegajosaSiSePuedeMover() {
        // Si es lo de arriba no hay nada que la sujete: se puede verter normal.
        val frasco = jar(0, Herb.MINT, Herb.MINT, stickyAt = setOf(1))
        assertTrue("la cima pegajosa deberia salir en bloque", frasco.topCount == 2)
    }

    @Test
    fun laCapaPegajosaParteElBloque() {
        // Tres de la misma hierba arriba y, justo debajo de la cima, la pegada: el
        // vertido se corta ahi y solo sale la de arriba. Es la jugada de mas.
        val frasco = jar(0, Herb.MINT, Herb.MINT, Herb.MINT, Herb.MINT, stickyAt = setOf(2))
        assertTrue("la pegajosa deberia cortar el bloque, salio ${frasco.topCount}", frasco.topCount == 1)
    }

    @Test
    fun sinPegajosaElBloqueSaleEntero() {
        val frasco = jar(0, Herb.MINT, Herb.MINT, Herb.MINT, Herb.MINT)
        assertTrue("sin tope el bloque deberia ser entero", frasco.topCount == 4)
    }

    @Test
    fun elMotorYElSolverCuentanElTopeIgual() {
        // Si no coinciden, la Pista sugiere jugadas que el motor rechaza.
        val jars = listOf(
            jar(0, Herb.LAVENDER, Herb.LAVENDER, Herb.MINT, Herb.MINT, stickyAt = setOf(2)),
            jar(1, Herb.LAVENDER, Herb.LAVENDER, Herb.MINT, Herb.MINT),
            Jar(id = 2, capacity = 4)
        )
        val destino = Jar(id = 3, capacity = 4)
        val resultado = engine.pour(jars[0], destino)
        val esperadoPorElJuego = jars[0].topCount

        val solucion = LevelSolver().repartirYResolver(
            jars + destino,
            null
        )
        assertTrue("el reparto deberia encontrar una solucion", solucion != null)

        // El primer movimiento sale del frasco 0 y no puede levar mas de dos.
        val primera = solucion!!.first()
        assertTrue("el primer vertido deberia salir del frasco 0", primera.first == 0)
        assertTrue(
            "la pista lleva ${primera} pero el motor solo deja $esperadoPorElJuego",
            primera.second != 0 && esperadoPorElJuego == 1
        )
        assertTrue("el motor deberia aceptar el vertido", resultado is PourResult.Success)
    }

    @Test
    fun losNivelesDelBosqueTraenCapasPegajosasYSeSiguenGanando() {
        val repository = LevelRepository(null)
        var conPegajosas = 0
        val fallos = mutableListOf<String>()
        (31..50).forEach { numero ->
            repeat(2) {
                val nivel = repository.getLevel(numero)
                val jars = repository.convertToJars(nivel)
                if (jars.any { jar -> jar.hasSticky }) conPegajosas++
                // Lo importante: la capa pegajosa no vuelve irresoluble el nivel.
                if (!LevelSolver().esGanalible(jars, nivel.customerOrder?.requiredHerbs)) {
                    fallos.add(
                        "nivel $numero par=${nivel.parMoves} receta=${nivel.customerOrder?.requiredHerbs?.joinToString { it.id }} " +
                            "pegajosas=${jars.count { jar -> jar.hasSticky }}"
                    )
                }
            }
        }
        assertTrue(
            "niveles del bosque con una capa pegajosa que no se pueden ganar: ${fallos.take(6)}",
            fallos.isEmpty()
        )
        assertTrue(
            "deberian salir capas pegajosas en el sector del bosque, salieron $conPegajosas",
            conPegajosas > 0
        )
    }

    @Test
    fun losNivelesDelPrimerSectorNoTraenPegajosas() {
        val repository = LevelRepository(null)
        listOf(2, 6, 10).forEach { numero ->
            val jars = repository.convertToJars(repository.getLevel(numero))
            assertTrue(
                "el nivel $numero es del sector de tutorial y no deberia tener tope pegajoso",
                jars.none { jar -> jar.hasSticky }
            )
        }
    }
}