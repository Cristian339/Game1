package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.cozy.herbshop.data.model.PourResult
import com.cozy.herbshop.data.model.SectorRule
import com.cozy.herbshop.ui.game.GameEngine
import com.cozy.herbshop.ui.map.LevelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Molinillo (sector del invernadero).
 *
 * No es un frasco de almacen: admite cualquier hierba, de una en una, y la
 * guarda ya molida (la siguiente del ciclo). Para terminar el nivel tiene que
 * quedar vacio, como la tetera tiene que cuadrar con la receta.
 *
 * Lo que se vigila aqui es que el motor, el solver y el generador cuenten lo
 * mismo: si el generador contara el molinillo como un frasco vacio mas, daria
 * por buenos niveles que el jugador no puede terminar; y si el solver lo viera
 * como destino, la Pista mandaria jugadas que el motor rechaza.
 */
class GrinderTest {

    private val engine = GameEngine()

    private fun frasco(id: Int, vararg herbs: Herb) = Jar(
        id = id,
        capacity = 4,
        layers = herbs.map { JarLayer(herb = it) }
    )

    private fun molinillo(id: Int, vararg herbs: Herb) = Jar(
        id = id,
        capacity = 1,
        layers = herbs.map { JarLayer(herb = it) },
        isGrinder = true
    )

    @Test
    fun elMolinilloGuardaLoQueLeEchanYaMolido() {
        val vasija = molinillo(0)
        val origen = frasco(1, Herb.MINT, Herb.MINT)
        val permiso = engine.pour(origen, vasija)
        assertTrue("deberia dejar echar una hierba", permiso is PourResult.Success)

        val despues = engine.executePour(
            listOf(vasija, origen),
            1,
            0,
            (permiso as PourResult.Success).countPoured
        )
        val dentro = despues[0].layers.map { it.herb }
        assertEquals(
            "el molinillo deberia guardar la hierba molida",
            listOf(Herb.MINT.molida),
            dentro
        )
        assertTrue("la hierba molida no puede ser la misma que entro", Herb.MINT.molida != Herb.MINT)
    }

    @Test
    fun elMolinilloCogeUnaSolaCapaAunqueVenganVariasJuntas() {
        // Un frasco normal se lleva el bloque entero de golpe; el molinillo muele
        // de una en una, que es justo lo que lo hace una jugada de mas.
        val vasija = molinillo(0)
        val origen = frasco(1, Herb.MINT, Herb.MINT, Herb.MINT, Herb.MINT)
        val permiso = engine.pour(origen, vasija)
        assertTrue("deberia dejar echar una hierba", permiso is PourResult.Success)
        assertEquals(
            "el molinillo no puede tragarse el bloque entero",
            1,
            (permiso as PourResult.Success).countPoured
        )
    }

    @Test
    fun elMolinilloAdmiteCualquierHierbaYNoSeMueleASiMismo() {
        val origen = frasco(1, Herb.ROSEMARY)

        // No hay que pedirle que la hierba coincida con nada: admite cualquiera.
        val vasija = molinillo(0)
        assertTrue(
            "el molinillo deberia admitir una hierba cualquiera",
            engine.pour(origen, vasija) is PourResult.Success
        )

        // Pero lleno, lleno: no hay hueco para una segunda.
        val lleno = molinillo(0, Herb.MINT.molida)
        assertFalse(
            "un molinillo con algo dentro no deberia admitir mas",
            engine.pour(origen, lleno) is PourResult.Success
        )
        assertFalse(
            "el molinillo no deberia molerse a si mismo",
            engine.pour(lleno, lleno) is PourResult.Success
        )
    }

    @Test
    fun elMolinilloSiempreMueleEnElMismoSentidoYCierraElCiclo() {
        // El ciclo no puede dejar ninguna hierba sin molida: la ultima vuelve a
        // la primera, o el nivel se quedaria sin salida.
        Herb.entries.forEach { herb ->
            assertTrue(
                "la molida de ${herb.id} deberia ser otra hierba",
                herb.molida != herb
            )
        }
        val moliendas = Herb.entries.map { it.molida }
        assertEquals(
            "el ciclo no puede repetir dos veces el mismo destino",
            Herb.entries.size,
            moliendas.distinct().size
        )
    }

    @Test
    fun elMolinilloTieneQueQuedarVacioParaGanar() {
        val ordenados = listOf(
            frasco(0, Herb.MINT, Herb.MINT, Herb.MINT, Herb.MINT),
            frasco(1, Herb.ROSEMARY, Herb.ROSEMARY, Herb.ROSEMARY, Herb.ROSEMARY)
        )
        assertTrue(
            "con el molinillo vacio el nivel deberia estar terminado",
            engine.checkLevelCompleted(ordenados + molinillo(2))
        )
        assertFalse(
            "con una hierba dentro el nivel no puede darse por terminado",
            engine.checkLevelCompleted(ordenados + molinillo(2, Herb.MINT.molida))
        )
    }

    @Test
    fun losNivelesDelInvernaderoTraenMolinilloYSeSiguenGanando() {
        val repository = LevelRepository(null)
        var conMolinillo = 0
        val fallos = mutableListOf<String>()
        val sinMolinillo = mutableListOf<Int>()
        (11..20).forEach { numero ->
            repeat(2) {
                val nivel = repository.getLevel(numero)
                val jars = repository.convertToJars(nivel)
                if (jars.any { it.isGrinder }) conMolinillo++ else sinMolinillo.add(numero)
                // Lo importante: el frasco de mas no vuelve irresoluble el nivel.
                if (!LevelSolver().esGanalible(jars, nivel.customerOrder?.requiredHerbs)) {
                    fallos.add(
                        "nivel $numero par=${nivel.parMoves} " +
                            "frascos=${jars.size} receta=${nivel.customerOrder?.requiredHerbs?.joinToString { it.id }}"
                    )
                }
            }
        }
        assertTrue(
            "el molinillo no puede hacer que un nivel del invernadero sea imposible: ${fallos.take(6)}",
            fallos.isEmpty()
        )
        assertTrue(
            "el sector del invernadero deberia traer molinillo en todos sus niveles, " +
                "faltaba en ${sinMolinillo.distinct().take(6)}",
            sinMolinillo.isEmpty()
        )
        assertTrue(
            "deberian salir molinillos, salieron $conMolinillo",
            conMolinillo > 0
        )
    }

    @Test
    fun elMolinilloNoEsUnFrascoVacioMas() {
        // Si el molinillo contara como frasco vacio se podria terminar el nivel
        // echandole una hierba, porque el nivel se daria por bueno con el lleno.
        val repository = LevelRepository(null)
        (11..20).forEach { numero ->
            val nivel = repository.getLevel(numero)
            val jars = repository.convertToJars(nivel)
            val vasija = jars.firstOrNull { it.isGrinder }
                ?: throw AssertionError("el nivel $numero deberia traer molinillo")
            assertEquals(
                "el molinillo del nivel $numero deberia empezar vacio",
                0,
                vasija.layers.size
            )
            assertTrue(
                "el molinillo del nivel $numero deberia caber una sola hierba",
                vasija.capacity == 1
            )
            assertFalse(
                "el nivel $numero no puede darse por terminado con una hierba dentro del molinillo",
                engine.checkLevelCompleted(
                    jars.map { jar ->
                        if (jar.isGrinder) jar.copy(layers = listOf(JarLayer(Herb.MINT.molida))) else jar
                    },
                    nivel.customerOrder,
                    LevelRepository.isGateLevel(numero)
                )
            )
        }
    }

    @Test
    fun losSectoresSinMolinilloNoTraenEseFrasco() {
        val repository = LevelRepository(null)
        (1..50).filterNot { SectorRule.forLevel(it).traeMolinillo }.forEach { numero ->
            val nivel = repository.getLevel(numero)
            val jars = repository.convertToJars(nivel)
            assertFalse(
                "el nivel $numero es de un sector sin molinillo y no deberia traerlo",
                jars.any { it.isGrinder }
            )
            assertTrue(
                "el nivel $numero no deberia declarar molinillo",
                nivel.grinderJarIndex == null
            )
        }
    }

    @Test
    fun laPistaDeUnNivelConMolinilloNuncaMandaEcharNadaDentro() {
        // El reparto que genera la Pista no usa el molinillo a proposito: sus
        // jugadas tienen que ser ejecutables tal cual en el motor.
        val repository = LevelRepository(null)
        val solver = LevelSolver()
        (11..20).forEach { numero ->
            val nivel = repository.getLevel(numero)
            var jars = repository.convertToJars(nivel)
            val order = nivel.customerOrder
            val vasija = jars.indexOfFirst { it.isGrinder }
            assertTrue("el nivel $numero deberia traer molinillo", vasija >= 0)

            val solucion = solver.repartirYResolver(jars, order?.requiredHerbs)
            assertTrue("el nivel $numero deberia tener solucion", solucion != null)

            solucion!!.forEach { (from, to) ->
                assertTrue(
                    "nivel $numero: la Pista manda echar al molinillo ($from -> $to)",
                    to != vasija
                )
                val permiso = engine.pour(jars[from], jars[to], order)
                assertTrue(
                    "nivel $numero: movimiento ilegal $from -> $to (${permiso as? PourResult.Invalid})",
                    permiso is PourResult.Success
                )
                jars = engine.executePour(
                    jars, from, to,
                    (permiso as PourResult.Success).countPoured,
                    order
                )
            }

            assertTrue(
                "nivel $numero: la solucion no completa el nivel",
                engine.checkLevelCompleted(jars, order, LevelRepository.isGateLevel(numero))
            )
        }
    }

}
