package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import org.junit.Test

/**
 * Comprobacion de que los niveles que salen de assets/levels.json se pueden
 * terminar. Es importante porque un nivel predefinido con receta que el solver
 * no resuelve deja la partida bloqueada: si es puerta, la hierba nunca se
 * desbloquea y el jugador no puede seguir avanzando.
 */
class PredefinedLevelsTest {

    private val solver = LevelSolver()

    private fun jar(id: Int, cap: Int, ids: List<String>, teapot: Boolean = false): Jar {
        val layers = ids.map { raw ->
            val hidden = raw.startsWith("?") || raw.startsWith("mystery:")
            JarLayer(
                herb = Herb.fromId(raw.removePrefix("?").removePrefix("mystery:")),
                isHidden = hidden
            )
        }
        return Jar(id = id, capacity = cap, layers = layers, isTeapot = teapot)
    }

    /** Replica lo que hace LevelRepository con un nivel del json. */
    private fun fromJson(levelNumber: Int, capacity: Int, rawJars: List<List<String>>): List<Jar> {
        val order = recipeFor(levelNumber)
        val jars = rawJars.toMutableList()
        var teapotIndex = -1
        if (order != null) {
            teapotIndex = jars.indexOfLast { it.isEmpty() }
            if (teapotIndex < 0) {
                jars.add(emptyList())
                teapotIndex = jars.lastIndex
            }
        }
        val teapotCapacity = order?.size?.coerceAtLeast(1) ?: capacity
        return jars.mapIndexed { index, ids ->
            jar(
                id = index,
                cap = if (index == teapotIndex) teapotCapacity else capacity,
                ids = ids,
                teapot = index == teapotIndex
            )
        }
    }

    /**
     * Receta real del nivel. Se pide al repositorio en vez de replicar aqui las
     * reglas: si las puertas se mueven (ahora 10/20/30/40/50) este test se entera
     * solo, en vez de validar contra un reparto viejo que ya no existe.
     */
    private val repository = LevelRepository(context = null)

    private fun recipeFor(levelNumber: Int): List<Herb>? =
        repository.orderForLevel(levelNumber)?.requiredHerbs

    private val predefined = listOf(
        1 to (4 to listOf(
            listOf("chamomile", "lavender", "chamomile", "lavender"),
            listOf("lavender", "chamomile", "lavender", "chamomile"),
            emptyList()
        )),
        2 to (4 to listOf(
            listOf("mint", "chamomile", "mint", "chamomile"),
            listOf("chamomile", "lavender", "chamomile", "lavender"),
            listOf("lavender", "mint", "lavender", "mint"),
            emptyList(),
            emptyList()
        )),
        3 to (4 to listOf(
            listOf("?chamomile", "?mint", "?cinnamon", "mint"),
            listOf("?cinnamon", "?chamomile", "?mint", "cinnamon"),
            listOf("?mint", "?cinnamon", "?chamomile", "chamomile"),
            emptyList(),
            emptyList()
        )),
        4 to (4 to listOf(
            listOf("hibiscus", "chamomile", "hibiscus", "rosemary"),
            listOf("rosemary", "lavender", "rosemary", "chamomile"),
            listOf("lavender", "hibiscus", "chamomile", "lavender"),
            listOf("chamomile", "rosemary", "lavender", "hibiscus"),
            emptyList(),
            emptyList()
        )),
        5 to (4 to listOf(
            listOf("calendula", "thyme", "calendula", "mint"),
            listOf("thyme", "cinnamon", "mint", "calendula"),
            listOf("cinnamon", "mint", "thyme", "cinnamon"),
            listOf("mint", "calendula", "cinnamon", "thyme"),
            emptyList(),
            emptyList()
        ))
    )

    @Test
    fun predefinedLevelsAreSolvable() {
        val broken = mutableListOf<String>()
        predefined.forEach { (levelNumber, spec) ->
            val (capacity, raw) = spec
            val order = recipeFor(levelNumber)
            val jars = fromJson(levelNumber, capacity, raw)
            val solvable = solver.isSolvable(jars, order)
            println("nivel $levelNumber receta=${order?.joinToString(",") { it.id } ?: "-"} resoluble=$solvable")
            if (!solvable) broken.add(levelNumber.toString())
        }
        check(broken.isEmpty()) {
            "niveles predefinidos imposibles con su receta: ${broken.joinToString()}. " +
                "Un nivel de puerta irresoluble deja al jugador sin poder seguir."
        }
    }
}