package com.cozy.herbshop.data.model

/**
 * Las ayudas que se pueden usar durante la partida y su inventario.
 *
 * Antes solo "Deshacer" tenia fichas y "Pista"/"Frasco extra" dependedian de un
 * anuncio cada vez, asi que el jugador nunca recibia el aviso de que se habian
 * acabado. Aqui las tres son igual: un contador de usos y, cuando llega a cero,
 * el juego ofrece dos salidas (anuncio o compra con Hojas de Oro).
 *
 * @param id clave estable para persistencia y para la tienda.
 * @param title nombre visible en el boton de ayuda.
 * @param packSize cuantas unidades da un pack comprado en la tienda.
 * @param packPrice precio del pack en Hojas de Oro.
 */
enum class HelpItem(
    val id: String,
    val title: String,
    val iconRes: String,
    val packSize: Int,
    val packPrice: Int
) {
    UNDO("help_undo", "Deshacer", "token_undo", 3, 15),
    HINT("help_hint", "Pista", "icon_hint", 3, 20),
    EXTRA_JAR("help_extra_jar", "+Frasco", "icon_extra_jar", 2, 25);

    companion object {
        fun fromId(id: String): HelpItem? = entries.firstOrNull { it.id == id }
    }
}