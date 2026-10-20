package com.cozy.herbshop.ui.map

import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.game.HerbUnlocks
import com.cozy.herbshop.game.LevelRepository
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * El mapa enseña informacion del nivel (receta, mecanicas, herbario). Si esa
 * ficha se equivoca, el mapa miente: mejor comprobarlo que confiar en que
 * coincide con lo que genera la partida.
 */
class LevelCatalogTest {

    private val catalog = LevelCatalog(LevelRepository(null), totalLevels = 30)

    @Test
    fun todasLasFichasTienenNumeroYTitulo() {
        val todas = catalog.all()
        assertEquals(30, todas.size)
        todas.forEach { info ->
            assertTrue("nivel sin numero", info.number in 1..30)
            assertTrue("nivel ${info.number} sin tramo", info.subtitle.isNotBlank())
        }
    }

    @Test
    fun laRecetaCoincideConLaDelNivel() {
        // El mapa no debe inventarse la receta: tiene que ser la del repositorio.
        listOf(10, 20, 30, 40, 50).forEach { numero ->
            val esperada = LevelRepository(null).orderForLevel(numero)?.requiredHerbs.orEmpty()
            assertEquals("receta distinta en el nivel $numero", esperada, catalog[numero].recipe)
        }
    }

    @Test
    fun laFichaNoNecesitaGenerarElNivel() {
        // Regresión: pedir las fichas generaba los 50 niveles con el solver y
        // tumbaba la pantalla. Ahora la ficha sale de las reglas del generador,
        // así que tiene que ser rápida incluso para los 50 niveles.
        val catalogo = LevelCatalog(LevelRepository(null), totalLevels = 50)
        val inicio = System.currentTimeMillis()
        val todas = catalogo.all()
        val pasado = System.currentTimeMillis() - inicio

        assertEquals(50, todas.size)
        assertTrue(
            "las 50 fichas tardaron ${pasado}ms: eso significa que esta generando niveles",
            pasado < 2_000
        )
    }

    @Test
    fun elNumeroDeFrascosCoincideConLoQueGeneraElGenerador() {
        // Aunque la ficha se deduzca con formulas, tiene que coincidir con las
        // que usa LevelRepository, o el mapa contaria cosas que no son ciertas.
        val repositorio = LevelRepository(null)
        listOf(5, 12, 30).forEach { numero ->
            val real = repositorio.convertToJars(repositorio.getLevel(numero))
            // Se cuentan todos los frascos, tetera incluida: es lo que ve el
            // jugador en pantalla. La tetera es una de las vacias que ya contaba
            // la ficha, asi queAqui no hay que restarla.
            val esperada = real.size
            assertEquals("frascos distintos en el nivel $numero", esperada, catalog[numero].jarCount)
        }
    }

    @Test
    fun losNivelesPuertaAnuncianSuHierba() {
        // Una puerta sin hierba que anunciar seria confuso: es la recompensa grande.
        listOf(10, 20, 30, 40, 50).forEach { numero ->
            val info = catalog[numero]
            assertTrue("nivel $numero deberia ser puerta", info.isGate)
            assertNotNull("nivel $numero sin hierba que ganar", info.unlockedHerb)
            assertTrue("hierba repetida en la puerta $numero", info.unlockedHerb in Herb.entries)
        }
    }

    @Test
    fun laLineaDeMecanicasNoEstaVacia() {
        catalog.all().forEach { info ->
            assertTrue("mecanicas vacias en el ${info.number}", info.mechanics.isNotBlank())
            assertTrue("deberia mencionar los frascos", info.mechanics.contains("frascos"))
        }
    }

    @Test
    fun elNivel1NoEsPuertaYNoTieneReceta() {
        // Los primeros niveles son de tutorial: no deben prometer receta ni hierba.
        val info = catalog[1]
        assertTrue("el nivel 1 no deberia ser puerta", !info.isGate)
        assertTrue("el nivel 1 no deberia tener receta", info.recipe.isEmpty())
        assertTrue("sin receta no hay tetera", !info.mechanics.contains("Tetera"))
    }

    /**
     * Los cinco primeros niveles son fijos de assets/levels.json. El mapa los
     * cuenta con las formulas del generador, que no son las suyas: el nivel 1
     * tiene 3 frascos y el mapa decia 5, y el nivel 4 no lleva nada sellado
     * aunque el mapa lo anunciaba.
     */
    @Test
    fun losNivelesFijosSeCuentanDesdeElJson() {
        val json = File("src/main/assets/levels.json")
        assertTrue("no se encuentra levels.json (${json.absolutePath})", json.exists())

        val predefined: List<GameLevel> = LevelRepository.gsonDeNiveles().fromJson(json.readText(), type)
        val catalogo = LevelCatalog(LevelRepository(null), totalLevels = 30, predefinedLevels = predefined)

        predefined.forEach { nivel ->
            val info = catalogo[nivel.levelNumber]
            val capas = nivel.jars.flatten()
            val ids = capas.map { id ->
                id.removePrefix("sealed:").removePrefix("frozen:")
                    .removePrefix("?").removePrefix("mystery:")
            }
            assertEquals(
                "hierras distintas en el nivel ${nivel.levelNumber}",
                ids.toSet().size,
                info.herbTypes
            )
            assertEquals(
                "frascos distintos en el nivel ${nivel.levelNumber}",
                nivel.jars.size,
                info.jarCount
            )
            assertEquals(
                "misterio mal anunciado en el ${nivel.levelNumber}",
                capas.any { it.startsWith("?") || it.startsWith("mystery:") },
                info.hasMystery
            )
            assertEquals(
                "congeladas mal anunciadas en el ${nivel.levelNumber}",
                capas.any { it.startsWith("frozen:") },
                info.hasFrozen
            )
            assertEquals(
                "sellado mal anunciado en el ${nivel.levelNumber}",
                capas.any { it.startsWith("sealed:") },
                info.hasSealed
            )
        }
    }

    @Test
    fun lasPuertasCuentanElFrascoDeLaTetera() {
        // Con receta el generador anade la tetera, que es una herramienta mas.
        // La ficha cuenta todos los frascos que ve el jugador y la nombra aparte
        // en la linea de mecanicas; el total tiene que incluirla.
        val repositorio = LevelRepository(null)
        listOf(10, 20, 30).forEach { numero ->
            val info = catalog[numero]
            val real = repositorio.convertToJars(repositorio.getLevel(numero))
            assertTrue("el nivel $numero deberia tener tetera", info.order != null)
            assertTrue("el nivel $numero deberia anunciar la tetera", info.mechanics.contains("Tetera"))
            assertTrue("el nivel $numero deberia tener tetera en el tablero", real.any { it.isTeapot })
            assertEquals("frascos de la puerta $numero", real.size, info.jarCount)
        }
    }

    @Test
    fun lasPuertasAnuncianLaHierbaQueDaLaPartida() {
        // El mapa promete una hierba concreta en cada puerta. Si el juego no da
        // esa, el mapa esta mintiendo: por eso se comprueba contra la misma regla
        // que usa la partida al ganar.
        val esperada = listOf(
            10 to Herb.ROSEMARY,
            20 to Herb.HIBISCUS,
            30 to Herb.CINNAMON,
            40 to Herb.CALENDULA,
            50 to Herb.THYME
        )
        esperada.forEach { (nivel, hierba) ->
            assertEquals(
                "el nivel $nivel no anuncia la hierba que se gana",
                hierba,
                catalog[nivel].unlockedHerb
            )
        }
    }

    @Test
    fun laRecetaDeLaPuertaUsaLaHierbaQueLaPuertaRegala() {
        // Es a proposito y no un descuido: el cliente pide la hierba que luego te
        // da ("prepara manzanilla con romero y te dare romero"). El generador
        // mete esa hierba en el nivel aunque el jugador todavia no la tenga
        // desbloqueada, asi que la puerta se puede superar; que el nivel la traiga
        // de verdad lo comprueba GeneratedGateLevelTest.
        listOf(10, 20, 30, 40, 50).forEach { nivel ->
            val info = catalog[nivel]
            assertTrue(
                "el nivel $nivel deberia pedir la misma hierba que otorga",
                info.unlockedHerb in info.recipe
            )
        }
    }

    private val type = object : TypeToken<List<GameLevel>>() {}.type
}