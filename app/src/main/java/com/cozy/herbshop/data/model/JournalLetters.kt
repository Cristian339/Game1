package com.cozy.herbshop.data.model

/**
 * Las cartas del diario.
 *
 * Tres ya estaban en el buzon del jugador cuando abrio la tienda, y las cinco
 * restantes llegan al terminar la puerta que cierra cada tramo del camino: asi
 * el diario cuenta por donde va la tienda sin que haya que ir a buscarlo.
 *
 * La tabla vive aqui, y no en la pantalla, porque la partida tambien la lee para
 * entregar la carta que toca: si las dos listas se separan, el diario promete una
 * carta que nunca llega.
 */
object JournalLetters {

    /** Cartas con las que arranca el diario. */
    val INICIALES: Set<String> = setOf("letter_clara", "letter_arthur", "letter_flora")

    private val CARTAS: List<CustomerLetter> = listOf(
        CustomerLetter(
            id = "letter_clara",
            senderName = "Abuela Clara",
            senderAvatar = "👵",
            letterTitle = "Carta de Agradecimiento - Dulce Calma",
            content = "Querido herbolario, la infusión de Manzanilla y Lavanda que me " +
                "preparaste ayer me ayudó a dormir plácidamente toda la noche. ¡Hacía " +
                "meses que no descansaba tan bien! Gracias por llenar tu tienda de " +
                "tanto cariño."
        ),
        CustomerLetter(
            id = "letter_arthur",
            senderName = "Botánico Arthur",
            senderAvatar = "👨‍🔬",
            letterTitle = "Notas de Investigación - Menta & Romero",
            content = "¡Estimado colega! El elixir de concentración con Menta y Romero " +
                "fue un éxito rotundo durante mis horas de estudio sobre la flora " +
                "silvestre. Tu precisión con las capas de hierbas es admirable."
        ),
        CustomerLetter(
            id = "letter_flora",
            senderName = "Madame Flora",
            senderAvatar = "🌺",
            letterTitle = "Un Té Especial - Flor de Jamaica",
            content = "¡Hola querido! Mi té de Flor de Jamaica y Canela causó sensación " +
                "en el té de la tarde con las vecinas del pueblo. Todas quieren saber " +
                "el secreto de tu tienda."
        ),

        // Una carta por sector: llegan al terminar la puerta de su tramo.
        CustomerLetter(
            id = "letter_amanecer",
            senderName = "Elisa, la panadera",
            senderAvatar = "🥐",
            letterTitle = "Gracias por los amaneceres",
            content = "Querida herbolista: tu tienda amanece antes que el pueblo. Ayer, " +
                "con la primera luz, ya tenías la manzanilla puesta y el mostrador " +
                "oliendo a lavanda. Los que madrugamos a la panadería nos paramos un " +
                "momento solo por ese olor. Gracias por empezar el día con nosotros.",
            gate = 10
        ),
        CustomerLetter(
            id = "letter_invernadero",
            senderName = "Don Prudencio, jardinero mayor",
            senderAvatar = "👨‍🌾",
            letterTitle = "Sobre el mortero nuevo",
            content = "¡Enhorabuena! Me han contado que ya mueles la hierba al momento, " +
                "y se nota: la infusión sale más fina y el invernadero huele a menta " +
                "recién triturada. Cuida ese molinillo como a un aprendiz: nunca le " +
                "pidas que guarde nada, porque solo sabe transformar.",
            gate = 20
        ),
        CustomerLetter(
            id = "letter_noche",
            senderName = "Nuria, la farolera",
            senderAvatar = "🏮",
            letterTitle = "Una taza a deshora",
            content = "Se me hace tarde escribiendo, pero tenía que decírtelo: la tienda " +
                "de noche es otra cosa. Los vecinos llegamos con las jugadas contadas " +
                "—la cena, los niños, la lumbre— y aun así sales con una taza caliente. " +
                "La noche no pide prisa, pide orden.",
            gate = 30
        ),
        CustomerLetter(
            id = "letter_bosque",
            senderName = "Mateo, el leñador",
            senderAvatar = "🪓",
            letterTitle = "Savias y nudos",
            content = "Vengo del bosque con las botas pegadas de savia y no sé cómo lo " +
                "haces: las hierbas que me diste venían tan bien puestas que parecía " +
                "que nadie las había tocado. Aquí, entre los pinos, lo que se pega se " +
                "queda pegado. Tú, en cambio, lo sueltas sin romperlo.",
            gate = 40
        ),
        CustomerLetter(
            id = "letter_otono",
            senderName = "Tía Rosa, la recolectora",
            senderAvatar = "🍂",
            letterTitle = "La última cesta del año",
            content = "Última carta del año, herbolista: con la hoja cayendo, tu tienda " +
                "ya lo hace todo a la vez —el mortero, la savia, la prisa de la noche " +
                "y la calma del amanecer— y aun así nadie se va sin su taza. Este otoño " +
                "ha sido el mejor de la comarca. Te espero en la próxima cosecha.",
            gate = 50
        )
    )

    /** Todas las cartas, en el orden en que se leen. */
    val TODAS: List<CustomerLetter> = CARTAS

    /** Una carta por id, para que el album pueda citarla sin duplicar el texto. */
    fun byId(id: String): CustomerLetter? = CARTAS.firstOrNull { it.id == id }

    /** Puertas que traen carta, de menor a mayor. */
    val PUERTAS: List<Int> = CARTAS.mapNotNull { it.gate }.sorted()

    /** La carta que llega al terminar el nivel [level], o null si no trae. */
    fun alTerminarPuerta(level: Int): CustomerLetter? =
        CARTAS.firstOrNull { it.gate == level }
}
