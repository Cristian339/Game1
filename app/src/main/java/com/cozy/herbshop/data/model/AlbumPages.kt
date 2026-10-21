package com.cozy.herbshop.data.model

/**
 * Las cinco paginas del Album de la Tienda.
 *
 * Cada sector del camino deja una pagina con cuatro recuerdos: la hierba que
 * representa el rincon, una receta de la casa, el cliente que cierra la puerta y
 * el sello del tramo. La pagina se abre al entrar al sector, la receta se guarda
 * a mitad de camino y el cliente y el sello llegan al superar la puerta.
 *
 * La tabla vive aqui, y no en la pantalla, para que las pruebas puedan comprobar
 * que las cartas, las hierbas y las recetas siguen cuadrando con el resto del
 * juego. La pantalla solo pinta lo que diga [progress].
 */
data class AlbumPage(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val startLevel: Int,
    val gateLevel: Int,
    val herb: Herb,
    val recipe: AlbumRecipe,
    val customerLetterId: String,
    val stampName: String,
    val stampDescription: String
) {
    /** Niveles que cubre la pagina, para la ficha del sector. */
    val levelRange: String get() = "$startLevel-$gateLevel"

    /** Nivel a partir del cual se guarda la receta: mitad del sector. */
    val recipeLevel: Int get() = startLevel + 5
}

/** Una receta guardada en una pagina, con la carta que la acompaña si la tiene. */
data class AlbumRecipe(
    val name: String,
    val ingredients: List<Herb>,
    val description: String,
    val letterId: String? = null
)

/** Los cuatro huecos de una pagina, en el orden en que se pintan. */
enum class AlbumSlot {
    HERB,
    RECIPE,
    CUSTOMER,
    STAMP
}

/** Que recuerdos de una pagina ha ganado ya el jugador. */
data class AlbumPageProgress(
    val herb: Boolean,
    val recipe: Boolean,
    val customer: Boolean,
    val stamp: Boolean
) {
    val unlockedCount: Int
        get() = listOf(herb, recipe, customer, stamp).count { it }

    val totalCount: Int get() = 4

    val isComplete: Boolean get() = unlockedCount == totalCount

    fun isUnlocked(slot: AlbumSlot): Boolean = when (slot) {
        AlbumSlot.HERB -> herb
        AlbumSlot.RECIPE -> recipe
        AlbumSlot.CUSTOMER -> customer
        AlbumSlot.STAMP -> stamp
    }
}

object AlbumPages {

    val ALL: List<AlbumPage> = listOf(
        AlbumPage(
            id = "amanecer",
            title = "Amanecer",
            subtitle = "La primera luz del pueblo",
            emoji = "🌅",
            startLevel = 1,
            gateLevel = 10,
            herb = Herb.CHAMOMILE,
            recipe = AlbumRecipe(
                name = "Dulce Calma",
                ingredients = listOf(Herb.CHAMOMILE, Herb.LAVENDER),
                description = "Una infusión dorada para bajar el ritmo y dormir sin prisa.",
                letterId = "letter_clara"
            ),
            customerLetterId = "letter_amanecer",
            stampName = "Primer amanecer",
            stampDescription = "La tienda abrió antes que el sol y el pueblo lo notó."
        ),
        AlbumPage(
            id = "invernadero",
            title = "Invernadero",
            subtitle = "Menta recién molida",
            emoji = "🌿",
            startLevel = 11,
            gateLevel = 20,
            herb = Herb.ROSEMARY,
            recipe = AlbumRecipe(
                name = "Elixir de Concentración",
                ingredients = listOf(Herb.MINT, Herb.ROSEMARY),
                description = "Menta fresca y romero para despejar la cabeza sin perder la calma.",
                letterId = "letter_arthur"
            ),
            customerLetterId = "letter_invernadero",
            stampName = "Mano verde",
            stampDescription = "El mortero nuevo dejó el invernadero oliendo a menta."
        ),
        AlbumPage(
            id = "noche",
            title = "Noche",
            subtitle = "Una taza a deshora",
            emoji = "🌙",
            startLevel = 21,
            gateLevel = 30,
            herb = Herb.HIBISCUS,
            recipe = AlbumRecipe(
                name = "Flor de Jamaica",
                ingredients = listOf(Herb.HIBISCUS, Herb.CINNAMON),
                description = "El té rojo que Madame Flora sirvió a las vecinas una tarde cualquiera.",
                letterId = "letter_flora"
            ),
            customerLetterId = "letter_noche",
            stampName = "Una taza a deshora",
            stampDescription = "La noche no pide prisa, pide orden."
        ),
        AlbumPage(
            id = "bosque",
            title = "Bosque",
            subtitle = "Savias, nudos y canela",
            emoji = "🌲",
            startLevel = 31,
            gateLevel = 40,
            herb = Herb.CINNAMON,
            recipe = AlbumRecipe(
                name = "Elixir de Canela Antigua",
                ingredients = listOf(Herb.CINNAMON, Herb.HIBISCUS, Herb.ROSEMARY),
                description = "La canela de la Sabia Aurelia, guardada para las noches del bosque."
            ),
            customerLetterId = "letter_bosque",
            stampName = "Savias y nudos",
            stampDescription = "Lo que se pega se queda pegado; tú lo sueltas sin romperlo."
        ),
        AlbumPage(
            id = "otono",
            title = "Otoño",
            subtitle = "La última cesta",
            emoji = "🍂",
            startLevel = 41,
            gateLevel = 50,
            herb = Herb.CALENDULA,
            recipe = AlbumRecipe(
                name = "Sérum de Caléndula",
                ingredients = listOf(Herb.CALENDULA, Herb.MINT, Herb.LAVENDER),
                description = "Flores de otoño y un toque de menta para cerrar el año con calma."
            ),
            customerLetterId = "letter_otono",
            stampName = "La última cesta",
            stampDescription = "El otoño más bonito de la comarca."
        )
    )

    /** Total de recuerdos del album: 5 paginas x 4 huecos. */
    val TOTAL_SLOTS: Int = ALL.size * 4

    /**
     * Progreso de una pagina.
     *
     * - La hierba se guarda al entrar al sector.
     * - La receta se guarda al llegar a la mitad del sector.
     * - El cliente y el sello llegan al cerrar la puerta.
     *
     * La carta se usa como segunda fuente para el cliente: si una partida vieja
     * ya tiene la carta pero el nivel siguiente aun no se ha actualizado, el
     * recuerdo no se queda bloqueado por un desfase de guardado.
     */
    fun progress(
        page: AlbumPage,
        currentLevel: Int,
        unlockedLetters: Set<String>
    ): AlbumPageProgress {
        if (currentLevel < page.startLevel) {
            return AlbumPageProgress(
                herb = false,
                recipe = false,
                customer = false,
                stamp = false
            )
        }

        val gateDone = currentLevel > page.gateLevel
        val customer = gateDone || page.customerLetterId in unlockedLetters

        return AlbumPageProgress(
            herb = true,
            recipe = currentLevel >= page.recipeLevel,
            customer = customer,
            stamp = customer
        )
    }

    /** Recuerdos guardados en todo el album. */
    fun totalProgress(currentLevel: Int, unlockedLetters: Set<String>): Int =
        ALL.sumOf { progress(it, currentLevel, unlockedLetters).unlockedCount }

    /** Pagina que toca al nivel indicado, para abrir el album por el sector actual. */
    fun pageIndexForLevel(level: Int): Int =
        ALL.indexOfLast { level >= it.startLevel }.coerceAtLeast(0)
}
