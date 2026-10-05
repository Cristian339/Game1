package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb

/**
 * Que hierba se gana en cada puerta de hierba.
 *
 * El juego no reparte las hierbas con una lista: al superar una puerta desbloquea
 * la primera que le falte, en el orden del enum. El mapa anuncia cual es, asi
 * que no puede llevar su propia lista: si las dos cosas se separan, el mapa
 * promete una hierba y la partida da otra.
 */
object HerbUnlocks {

    /** Las hierbas con las que arranca la tienda. */
    val INITIAL: Set<String> = setOf("chamomile", "lavender", "mint")

    /** Primera hierba que todavia le falta al jugador. */
    fun nextLocked(unlocked: Set<String>): Herb? =
        Herb.entries.firstOrNull { it.id !in unlocked }

    /**
     * Hierba que otorga cada puerta, leyendo las puertas de izquierda a derecha.
     * Se va añadiendo la hierba ganada a las que ya tiene, igual que hace la
     * partida, para que encadene bien.
     */
    fun herbByGate(gates: Collection<Int>): Map<Int, Herb> {
        var desbloqueadas = INITIAL
        return buildMap {
            gates.sorted().forEach { nivel ->
                val hierba = nextLocked(desbloqueadas) ?: return@forEach
                put(nivel, hierba)
                desbloqueadas = desbloqueadas + hierba.id
            }
        }
    }
}
