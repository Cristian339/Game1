package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.SectorRule

/**
 * Las mecanicas que el juego va estrenando por el camino.
 *
 * El jugador dijo que jugando no se enteraba de que estaban ahi, asi que el
 * nivel que estrena cada una le da un tutorial corto (con boton de saltar) la
 * primera vez que la ve. [introLevel] es tambien el nivel que el generador
 * fuerza a que la traiga y el que el mapa marca con "aqui se estrena": asi el
 * tutorial, la partida y la ficha no pueden contradecirse.
 *
 * Solo cuenta como estreno la primera aparicion de verdad: el misterio empieza
 * en el 3, la tetera en el primer pedido (el 4), las congeladas en el 5, el
 * molinillo en el 11, el tope de jugadas en el 21 y las pegajosas en el 31.
 */
enum class Mechanic(
    val id: String,
    val introLevel: Int,
    val emoji: String,
    val title: String,
    val body: String
) {
    MYSTERY(
        id = "mystery",
        introLevel = 3,
        emoji = "❓",
        title = "Capas misteriosas",
        body = "Algunas capas vienen tapadas con ❓. Se destapan solas cuando llegan a lo alto del frasco; mientras tanto cortan el bloque de vertido."
    ),
    TEAPOT(
        id = "teapot",
        introLevel = 4,
        emoji = "🫖",
        title = "La Tetera",
        body = "El frasco con tapa es la Tetera: se vierte una unidad cada vez y solo vale en el orden de la receta. Al completarla se sella y suelta vapor."
    ),
    FROZEN(
        id = "frozen",
        introLevel = 5,
        emoji = "❄️",
        title = "Capas congeladas",
        body = "Una capa congelada se descongela al llegar a lo alto del frasco. Mientras esté dentro, el bloque de vertido se corta en ella."
    ),
    GRINDER(
        id = "grinder",
        introLevel = 11,
        emoji = "⚙️",
        title = "El Molinillo",
        body = "El frasco con la manivela muele de una en una: admite cualquier hierba y la guarda convertida en la siguiente del ciclo. Tiene que quedar vacío para superar el nivel."
    ),
    MOVE_LIMIT(
        id = "move_limit",
        introLevel = 21,
        emoji = "⏳",
        title = "Tope de jugadas",
        body = "En este sector cada nivel tiene un tope de jugadas: el par más cuatro. Si se acaban no pierdes la partida: deja de aceptar jugadas hasta que uses ↩️ Deshacer o reinicies."
    ),
    STICKY(
        id = "sticky",
        introLevel = 31,
        emoji = "🍯",
        title = "Capas pegajosas",
        body = "Una capa pegajosa se agarra al frasco: las de encima salen bien, pero el vertido se corta al llegar a ella y hay que gastar otra jugada."
    );

    /**
     * Si el nivel trae la mecanica. Unas van en los frascos (capas, tetera,
     * molinillo) y otras en la regla del sector (el tope de jugadas), por eso
     * hacen falta las dos cosas.
     */
    fun isPresentIn(jars: List<Jar>, sectorRule: SectorRule): Boolean = when (this) {
        MYSTERY -> jars.any { jar -> jar.layers.any { it.isHidden } }
        TEAPOT -> jars.any { it.isTeapot }
        FROZEN -> jars.any { jar -> jar.layers.any { it.isFrozen } }
        GRINDER -> jars.any { it.isGrinder }
        MOVE_LIMIT -> sectorRule == SectorRule.MOVE_LIMIT
        STICKY -> jars.any { it.hasSticky }
    }

    companion object {
        /**
         * La mecanica que toca explicar: la mas temprana que el nivel trae y
         * que el jugador todavia no ha visto. Null si ya se las sabe todas.
         */
        fun toExplain(jars: List<Jar>, sectorRule: SectorRule, seen: Set<String>): Mechanic? =
            entries
                .filter { it.id !in seen && it.isPresentIn(jars, sectorRule) }
                .minByOrNull { it.introLevel }
    }
}
