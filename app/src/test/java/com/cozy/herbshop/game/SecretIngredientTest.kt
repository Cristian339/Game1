package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.cozy.herbshop.data.model.PourResult
import com.cozy.herbshop.ui.game.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hierbas secretas de las puertas: una por puerta, escondida y tapada en un
 * frasco al azar, y al herbario en cuanto el jugador la destapa.
 */
class SecretIngredientTest {

    private val repository = LevelRepository(null)
    private val solver = LevelSolver()
    private val engine = GameEngine()

    /** Las puertas del camino: son las que esconden hierba secreta. */
    private val puertas = listOf(10, 20, 30, 40, 50)

    @Test
    fun hayUnaHierbaSecretaPorPuertaYSonTodasDistintas() {
        val secretas = puertas.map { Herb.secretaDePuerta(it) }
        assertTrue("cada puerta deberia traer su secreta", secretas.all { it != null })
        assertEquals("las secretas no pueden repetirse", puertas.size, secretas.distinct().size)
        assertEquals(
            "el enum no deberia traer secretas de mas ni de menos",
            puertas.size,
            Herb.secretas.size
        )
        Herb.secretas.forEach { secreta ->
            assertTrue(
                "la secreta ${secreta.id} no es de ninguna puerta",
                secreta.gate in puertas
            )
            assertTrue(
                "las secretas no pueden entrar en el reparto",
                secreta !in Herb.delReparto
            )
        }
    }

    @Test
    fun cadaPuertaEscondeSuSecretaTapadaYSigueSiendoGanable() {
        puertas.forEach { puerta ->
            val nivel = repository.getLevel(puerta)
            val jars = repository.convertToJars(nivel)
            val secreta = Herb.secretaDePuerta(puerta)!!
            val escondidas = jars.flatMap { it.layers }.filter { it.herb == secreta }

            assertEquals(
                "la puerta $puerta deberia esconder una sola ${secreta.id}",
                1,
                escondidas.size
            )
            assertTrue(
                "la secreta de la puerta $puerta tiene que estar tapada al empezar",
                escondidas.single().isHidden
            )
            assertTrue(
                "la puerta $puerta deja de ser ganable con la secreta dentro",
                solver.esGanalible(jars, nivel.customerOrder?.requiredHerbs)
            )
        }
    }

    @Test
    fun losNivelesQueNoSonPuertaNoTraenSecretas() {
        listOf(1, 5, 6, 7, 15, 23, 33, 39, 45, 47).forEach { numero ->
            val jars = repository.convertToJars(repository.getLevel(numero))
            val secretas = jars.flatMap { it.layers }.filter { it.herb.esSecreta }
            assertTrue(
                "el nivel $numero no es puerta y no deberia traer secretas",
                secretas.isEmpty()
            )
        }
    }

    @Test
    fun laSecretaSeDestapaAlVerterYQuedaALaVista() {
        val secreta = Herb.secretaDePuerta(10)!!
        val jars = listOf(
            // Las capas van de abajo arriba: la secreta al fondo, tapada.
            Jar(
                id = 0,
                layers = listOf(JarLayer(secreta, isHidden = true), JarLayer(Herb.CHAMOMILE))
            ),
            Jar(id = 1, layers = listOf(JarLayer(Herb.CHAMOMILE)))
        )
        assertTrue(
            "una capa tapada no puede contar como encontrada",
            engine.visibleSecrets(jars).isEmpty()
        )

        val resultado = engine.pour(jars[0], jars[1])
        assertTrue("verter la cima al mismo color deberia ser legal", resultado is PourResult.Success)
        val despues = engine.executePour(
            jars,
            0,
            1,
            (resultado as PourResult.Success).countPoured
        )
        assertEquals(
            "al destapar la capa, la secreta tiene que quedar a la vista",
            listOf(secreta),
            engine.visibleSecrets(despues)
        )
    }

    @Test
    fun laSecretaObligaADestaparSuFrascoParaTerminar() {
        // La secreta es la unica de su color: su frasco solo puede terminar vacio
        // o con ella sola, asi que hay que quitarle de encima lo que tenga (y al
        // quitarlo, la secreta queda a la vista). El nivel no se puede terminar
        // con la secreta escondida.
        val secreta = Herb.secretaDePuerta(10)!!
        val receta = listOf(Herb.MINT, Herb.MINT, Herb.LAVENDER, Herb.LAVENDER)
        val pedido = CustomerOrder(
            id = "test",
            customerName = "Cliente de prueba",
            dialogue = "",
            recipeName = "Infusion de prueba",
            requiredHerbs = receta
        )
        val tetera = Jar(
            id = 0,
            capacity = 4,
            layers = receta.map { JarLayer(it) },
            isTeapot = true
        )
        // La secreta al fondo, tapada, con una manzanilla encima.
        val conLaSecreta = Jar(
            id = 1,
            capacity = 4,
            layers = listOf(JarLayer(secreta, isHidden = true), JarLayer(Herb.CHAMOMILE))
        )
        val aManzanilla = Jar(id = 2, capacity = 4, layers = listOf(JarLayer(Herb.CHAMOMILE)))
        val jars = listOf(tetera, conLaSecreta, aManzanilla)

        assertFalse(
            "con una capa de otro color encima, el frasco de la secreta no termina",
            engine.isOrderFulfilled(jars, pedido)
        )

        val resultado = engine.pour(conLaSecreta, aManzanilla)
        assertTrue("verter la cima al mismo color deberia ser legal", resultado is PourResult.Success)
        val despues = engine.executePour(
            jars,
            1,
            2,
            (resultado as PourResult.Success).countPoured
        )

        assertTrue(
            "tras destapar la secreta, el frasco ya es de una sola hierba",
            engine.isOrderFulfilled(despues, pedido)
        )
        assertEquals(
            "seguir la jugada natural tiene que destapar la secreta",
            listOf(secreta),
            engine.visibleSecrets(despues)
        )
    }
}
