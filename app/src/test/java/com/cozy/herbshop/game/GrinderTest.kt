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
 * Molinillo (sector de la noche, niveles 21-30).
 *
 * No es un frasco de almacen: admite cualquier hierba, de una en una, y la
 * transforma en la siguiente del ciclo. Para terminar el nivel tiene que quedar
 * vacio, como la tetera tiene que cuadrar con la receta. En los niveles sin
 * receta el generador crea un desequilibrio (5 de A y 3 de B, con A.molida == B)
 * para que moler sea obligatorio; en los que tienen receta arranca con una
 * hierba ya molida dentro, que hay que devolver al tablero.
 *
 * Lo que se vigila aqui es que el motor, el solver y el generador cuenten lo
 * mismo: si el generador contara el molinillo como un frasco vacio mas, daria
 * por buenos niveles que el jugador no puede terminar; y si el solver no viera
 * su transformacion, la Pista mandaria jugadas que el motor rechaza.
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
    fun elMolinilloLlenoExplicaQueHayQueVaciarlo() {
        // El aviso generico de "frasco lleno" no sirve aqui: el jugador tiene que
        // saber que la hierba de dentro se puede sacar para volver a moler.
        val lleno = molinillo(0, Herb.MINT.molida)
        val origen = frasco(1, Herb.ROSEMARY)
        val rechazo = engine.pour(origen, lleno) as PourResult.Invalid
        assertTrue(
            "el aviso no explica como seguir: ${rechazo.reason}",
            rechazo.reason.contains("vacíalo") || rechazo.reason.contains("vacialo")
        )
        assertFalse(
            "no deberia quedarse en el aviso generico de frasco lleno",
            rechazo.reason.contains("frasco ya está lleno")
        )
    }

    @Test
    fun elMolinilloSiempreMueleEnElMismoSentidoYCierraElCiclo() {
        // El ciclo no puede dejar ninguna hierba sin molida: la ultima vuelve a
        // la primera, o el nivel se quedaria sin salida.
        Herb.delReparto.forEach { herb ->
            assertTrue(
                "la molida de ${herb.id} deberia ser otra hierba",
                herb.molida != herb
            )
        }
        val moliendas = Herb.delReparto.map { it.molida }
        assertEquals(
            "el ciclo no puede repetir dos veces el mismo destino",
            Herb.delReparto.size,
            moliendas.distinct().size
        )
        // Las secretas no entran en el ciclo: si una cae en el mortero sale a una
        // hierba del reparto, nunca a otra secreta ni a si misma.
        Herb.entries.filter { it.esSecreta }.forEach { secreta ->
            assertTrue(
                "la secreta ${secreta.id} deberia molerse en una hierba del reparto",
                secreta.molida in Herb.delReparto
            )
        }
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
    fun sinMolerNoSePuedeCerrarUnNivelDeLaNoche() {
        // En los niveles sin receta el generador cambia una B por una A con
        // A.molida == B. Quedan 5 A y 3 B, y como un frasco terminado es
        // exactamente 4 de un color, ni A ni B pueden cerrarse: hace falta moler
        // una A para dejarlo en 4 y 4. Sin el molinillo el nivel no se termina.
        val repository = LevelRepository(null)
        var comprobados = 0
        (21..29).forEach { numero ->
            val nivel = repository.getLevel(numero)
            if (nivel.customerOrder != null) return@forEach
            val jars = repository.convertToJars(nivel)
            val cuentas = jars
                .filterNot { it.isTeapot || it.isGrinder }
                .flatMap { jar -> jar.layers }
                .groupingBy { it.herb }
                .eachCount()
            val par = cuentas.entries.firstOrNull { (herb, cuantas) ->
                cuantas == 5 && cuentas[herb.molida] == 3
            }
            assertTrue(
                "el nivel $numero no trae el desequilibrio que hace obligatorio moler: " +
                    cuentas.filterValues { it != 4 },
                par != null
            )
            assertTrue(
                "el nivel $numero ya esta cerrado sin moler: ${par!!.key.id} x${par.value}",
                cuentas.values.any { it % 4 != 0 }
            )
            comprobados++
        }
        assertTrue("no se comprobo ningun nivel de la noche", comprobados > 0)
    }

    @Test
    fun losNivelesDeLaNocheTraenMolinilloYSeSiguenGanando() {
        val repository = LevelRepository(null)
        var conMolinillo = 0
        val fallos = mutableListOf<String>()
        val sinMolinillo = mutableListOf<Int>()
        (21..30).forEach { numero ->
            repeat(2) {
                val nivel = repository.getLevel(numero)
                val jars = repository.convertToJars(nivel)
                if (jars.any { it.isGrinder }) conMolinillo++ else sinMolinillo.add(numero)
                // Lo importante: el molinillo no vuelve irresoluble el nivel.
                if (!LevelSolver().esGanalible(jars, nivel.customerOrder?.requiredHerbs)) {
                    fallos.add(
                        "nivel $numero par=${nivel.parMoves} " +
                            "frascos=${jars.size} receta=${nivel.customerOrder?.requiredHerbs?.joinToString { it.id }}"
                    )
                }
            }
        }
        assertTrue(
            "el molinillo no puede hacer que un nivel de la noche sea imposible: ${fallos.take(6)}",
            fallos.isEmpty()
        )
        assertTrue(
            "el sector de la noche deberia traer molinillo en todos sus niveles, " +
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
        (21..30).forEach { numero ->
            val nivel = repository.getLevel(numero)
            val jars = repository.convertToJars(nivel)
            val vasija = jars.firstOrNull { it.isGrinder }
                ?: throw AssertionError("el nivel $numero deberia traer molinillo")
            assertEquals(
                "el molinillo del nivel $numero deberia caber una sola hierba",
                1,
                vasija.capacity
            )
            // Con receta arranca con una capa ya molida (la que hay que devolver
            // al tablero); sin receta arranca vacio y se llena al moler.
            val esperado = if (nivel.customerOrder != null) 1 else 0
            assertEquals(
                "el molinillo del nivel $numero deberia arrancar con $esperado capas",
                esperado,
                vasija.layers.size
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
    fun laPistaDeUnNivelConMolinilloEsEjecutable() {
        // La Pista puede usar el molinillo (moler una hierba o sacar la que ya
        // trae), pero cada jugada tiene que ser legal en el motor y la solucion
        // completa tiene que dejar el molinillo vacio.
        val repository = LevelRepository(null)
        val solver = LevelSolver()
        (21..30).forEach { numero ->
            val nivel = repository.getLevel(numero)
            var jars = repository.convertToJars(nivel)
            val order = nivel.customerOrder
            val vasija = jars.indexOfFirst { it.isGrinder }
            assertTrue("el nivel $numero deberia traer molinillo", vasija >= 0)

            val solucion = solver.repartirYResolver(jars, order?.requiredHerbs)
            assertTrue("el nivel $numero deberia tener solucion", solucion != null)

            solucion!!.forEach { (from, to) ->
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
            assertTrue(
                "nivel $numero: la solucion deja algo en el molinillo",
                jars[vasija].isEmpty
            )
        }
    }

    @Test
    fun laFichaDelMapaCuentaElMolinilloYAnunciaLaNovedad() {
        // El mapa y la partida leen la misma tabla de sectores, y el frasco de mas
        // tiene que salir en la cuenta o la ficha mentiria. Los niveles 1-5 salen
        // de assets/levels.json (que en un test sin contexto no se lee), asi que
        // se miran del 6 en adelante, que es donde manda la formula.
        val repository = LevelRepository(null)
        val catalog = LevelCatalog(repository)
        val fallos = mutableListOf<String>()
        var enReserva = 0
        val reserva = mutableListOf<Int>()
        (6..50).forEach { numero ->
            val nivel = repository.getLevel(numero)
            val jars = repository.convertToJars(nivel)
            val info = catalog[numero]
            val hierbasDeVerdad = jars.flatMap { jar -> jar.stack }.distinct().size

            // Las cuentas de la ficha son el tope del reparto normal; el nivel de
            // reserva trae menos hierbas, y eso vale mientras sea la excepcion:
            // si todos los niveles cayeran ahi, la ficha no serviria de nada.
            if (jars.size > info.jarCount) {
                fallos.add("nivel $numero: la ficha dice ${info.jarCount} frascos y hay ${jars.size}")
            }
            if (hierbasDeVerdad > info.herbTypes) {
                fallos.add(
                    "nivel $numero: la ficha dice ${info.herbTypes} hierbas y hay $hierbasDeVerdad"
                )
            }
            if (jars.size != info.jarCount || hierbasDeVerdad != info.herbTypes) {
                enReserva++
                reserva.add(numero)
            }
            // La ficha no puede anunciar un molinillo que la partida no trae, ni
            // callarse el que trae.
            val traeMolino = jars.any { it.isGrinder }
            if (info.mechanics.contains("Molinillo") != traeMolino) {
                fallos.add(
                    "nivel $numero: la ficha dice \"${info.mechanics}\" y el molinillo " +
                        if (traeMolino) "si esta" else "no esta"
                )
            }
            // Toda novedad del sector tiene que estar en la ficha, incluido el
            // "Todo a la vez", que se anuncia regla a regla.
            SectorRule.forLevel(numero).novedades.forEach { novedad ->
                if (!info.mechanics.contains(novedad)) {
                    fallos.add("nivel $numero: la ficha se calla la novedad de su sector ($novedad)")
                }
            }
        }
        assertTrue(
            "fichas del mapa que no cuadran con el nivel: ${fallos.take(8)}",
            fallos.isEmpty()
        )
        // El tablero de reserva es la red de seguridad, no el camino: si saltara
        // en mas de unos pocos niveles, la ficha se quedaria corta en muchos.
        assertTrue(
            "niveles que caen al tablero de reserva: $enReserva ($reserva) (esperado 4 o menos)",
            enReserva <= 4
        )
    }
}
