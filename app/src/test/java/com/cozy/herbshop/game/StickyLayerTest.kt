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
 * La miel va pegada al fondo del frasco: no se puede sacar, asi que para
 * terminarlo hay que cubrirla con su mismo color. Mientras esta en la boca
 * bloquea el frasco; si tiene capas encima, corta la rafaga como un tope. Lo
 * peligroso no es que sea dificil, es que el motor y el solver no coincidan: si
 * el solver no ve el bloqueo, daria una pista ilegal, y si el generador lo
 * pusiera en medio, las capas de debajo quedarian atrapadas y el nivel no se
 * podria terminar.
 */
class StickyLayerTest {

    private val engine = GameEngine()

    private fun jar(id: Int, vararg herbs: Herb, stickyAt: Set<Int> = emptySet()) = Jar(
        id = id,
        capacity = 4,
        layers = herbs.mapIndexed { i, h -> JarLayer(herb = h, isSticky = i in stickyAt) }
    )

    @Test
    fun laCimaPegajosaBloqueaElFrasco() {
        // Pegajosa en la boca: no sale nada. Se cubre con su mismo color.
        val frasco = jar(0, Herb.MINT, Herb.MINT, stickyAt = setOf(1))
        assertTrue("la cima pegajosa no deberia dejar salir nada", frasco.topCount == 0)

        val destino = Jar(id = 1, capacity = 4)
        assertTrue(
            "el motor tampoco deberia dejar verter desde una cima pegajosa",
            engine.pour(frasco, destino) is PourResult.Invalid
        )
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
        // Si no coinciden, la Pista sugiere jugadas que el motor rechaza. Este
        // tablero se termina sacando la lavanda de encima de la miel y cubriendo
        // la miel con las tres mentas del otro frasco.
        val jars = listOf(
            jar(0, Herb.MINT, Herb.LAVENDER, stickyAt = setOf(0)),
            jar(1, Herb.LAVENDER, Herb.LAVENDER, Herb.LAVENDER),
            jar(2, Herb.MINT, Herb.MINT, Herb.MINT),
            Jar(id = 3, capacity = 4)
        )

        val solucion = LevelSolver().repartirYResolver(jars, null)
        assertTrue("el reparto deberia encontrar una solucion", solucion != null)

        var actuales = jars
        solucion!!.forEach { (from, to) ->
            val permiso = engine.pour(actuales[from], actuales[to])
            assertTrue(
                "movimiento ilegal $from -> $to (${permiso as? PourResult.Invalid})",
                permiso is PourResult.Success
            )
            actuales = engine.executePour(
                actuales, from, to,
                (permiso as PourResult.Success).countPoured
            )
        }
        assertTrue(
            "la solucion del solver no termina el nivel en el motor",
            engine.checkLevelCompleted(actuales)
        )
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