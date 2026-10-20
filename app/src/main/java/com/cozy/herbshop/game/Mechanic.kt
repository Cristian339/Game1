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
 * El reparto es deliberadamente lento: primero la base (misterio y recetas),
 * luego una regla nueva por sector. Antes en el nivel 9 ya habia tres mecanicas
 * y el jugador no sabia cual estaba jugando.
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
        introLevel = 11,
        emoji = "❄️",
        title = "Frascos helados",
        body = "Un frasco helado no suelta nada: mientras tenga hielo no puedes sacar ninguna capa. Completa otro frasco y el hielo se romperá en todo el tablero."
    ),
    GRINDER(
        id = "grinder",
        introLevel = 21,
        emoji = "⚙️",
        title = "El Molinillo",
        body = "El frasco con la manivela muele de una en una: admite cualquier hierba y la guarda convertida en la siguiente del ciclo. Hay niveles que solo se cierran moliendo una hierba que sobra para convertirla en la que falta."
    ),
    STICKY(
        id = "sticky",
        introLevel = 31,
        emoji = "🍯",
        title = "Capas pegajosas",
        body = "La miel se queda pegada a la boca del frasco: si asoma, ese frasco no se puede vaciar. Cúbrela con una capa de su mismo color para poder seguir."
    ),
    MOVE_LIMIT(
        id = "move_limit",
        introLevel = 41,
        emoji = "⏳",
        title = "Tope de jugadas",
        body = "En el último sector cada nivel tiene un tope de jugadas: el par más cuatro. Si se acaban no pierdes la partida: deja de aceptar jugadas hasta que uses ↩️ Deshacer o reinicies."
    );

    /**
     * Si el nivel trae la mecanica. Unas van en los frascos (capas, tetera,
     * molinillo) y otras en la regla del sector (el tope de jugadas), por eso
     * hacen falta las dos cosas.
     */
    fun isPresentIn(jars: List<Jar>, sectorRule: SectorRule): Boolean = when (this) {
        MYSTERY -> jars.any { jar -> jar.layers.any { it.isHidden } }
        TEAPOT -> jars.any { jar -> jar.isTeapot }
        FROZEN -> jars.any { jar -> jar.hasFrozen }
        GRINDER -> jars.any { jar -> jar.isGrinder }
        STICKY -> jars.any { jar -> jar.hasSticky }
        MOVE_LIMIT -> sectorRule.limitaJugadas
    }

    companion object {
        /**
         * La mecanica que toca explicar.
         *
         * En el nivel que estrena una mecanica (11, 21, 31, 41) se muestra
         * siempre, aunque el jugador ya la haya visto: asi puede repetir el
         * nivel y volver a leer la explicacion, o saltarla con el boton del
         * tutorial. En el resto de niveles solo se muestra la primera vez que
         * aparece una mecanica que el jugador todavia no conoce.
         *
         * Las mecanicas iniciales (misterio y tetera) no se repiten en cada
         * visita al nivel 3/4: ya tienen su tutorial de bienvenida y el de
         * receta, que sale antes que este.
         */
        fun toExplain(
            levelNumber: Int,
            jars: List<Jar>,
            sectorRule: SectorRule,
            seen: Set<String>
        ): Mechanic? {
            val estreno = entries.firstOrNull {
                it.introLevel == levelNumber &&
                    it.introLevel >= FROZEN.introLevel &&
                    it.isPresentIn(jars, sectorRule)
            }
            if (estreno != null) return estreno

            return entries
                .filter { it.id !in seen && it.isPresentIn(jars, sectorRule) }
                .minByOrNull { it.introLevel }
        }
    }
}
