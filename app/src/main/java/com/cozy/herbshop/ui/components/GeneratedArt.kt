package com.cozy.herbshop.ui.components

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.cozy.herbshop.R
import com.cozy.herbshop.data.model.Herb
import kotlin.math.absoluteValue

/**
 * Sprites generados para el juego, cargados una sola vez y cacheados en memoria.
 * Los nombres de recurso coinciden con el id de cada hierba (herb_chamomile...).
 */
object GeneratedArt {
    private var appContext: android.content.Context? = null
    private var herbCache: Map<String, ImageBitmap?>? = null
    private var miscCache: MutableMap<Int, ImageBitmap> = mutableMapOf()

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    fun herb(res: Resources, herb: Herb): ImageBitmap? {
        val map = herbCache ?: run {
            val loaded = Herb.entries.associate { it.id to decode(res, "herb_${it.id}") }
            herbCache = loaded
            loaded
        }
        return map[herb.id]
    }

    fun resId(res: Resources, name: String): Int =
        res.getIdentifier(name, "drawable", "com.cozy.herbshop")

    fun bitmap(res: Resources, resId: Int): ImageBitmap? = miscCache.getOrPut(resId) {
        BitmapFactory.decodeResource(res, resId)?.asImageBitmap() ?: ImageBitmap(1, 1)
    }

    fun loadBitmap(res: Resources, resId: Int): ImageBitmap = bitmap(res, resId)!!

    private fun decode(res: Resources, name: String): ImageBitmap? {
        val id = resId(res, name)
        if (id == 0) return null
        return BitmapFactory.decodeResource(res, id)?.asImageBitmap()
    }

    /** Recurso de la ilustracion de una hierba; 0 si no existe. */
    fun resIdFor(herb: Herb): Int {
        val ctx = appContext ?: return 0
        return resId(ctx.resources, "herb_${herb.id}")
    }

    /** Arte de cada artículo de decoración de la tienda. */
    fun decorationArt(decorationId: String): String = when (decorationId) {
        "estante_roble" -> "deco_shelf_oak"
        "luces_hadas" -> "deco_fairy_garland"
        "estufa_calida" -> "deco_stove_warm"
        "plantas_cozy" -> "deco_fern_pots"
        else -> decorationId
    }

    /** Portada de cada pack ASMR. */
    fun asmrArt(packId: String): String = when (packId) {
        "asmr_cristal" -> "asmr_cover_crystal"
        "asmr_lluvia" -> "asmr_cover_rain"
        "asmr_chimenea" -> "asmr_cover_fire"
        "asmr_arroyo" -> "asmr_cover_stream"
        "asmr_bosque" -> "asmr_cover_forest"
        "asmr_noche" -> "asmr_cover_night"
        else -> packId
    }

    /** Emblema de cada logro. */
    fun achievementArt(achievementId: String): String = when (achievementId) {
        "ach_1" -> "badge_maestro"
        "ach_2" -> "badge_botanico"
        "ach_3" -> "badge_ritual"
        "ach_4" -> "badge_diseno"
        else -> achievementId
    }

    /**
     * Retrato del cliente según su nombre. Los siete clientes del juego tienen
     * retrato propio; el resto reparte los extras de forma estable.
     */
    fun avatarFor(customerName: String): Int {
        val key = customerName.trim().lowercase()
        val exact = when (key) {
            "vendedora rosa", "abuela clara" -> if (key == "vendedora rosa") "avatar_vendedora_rosa" else "avatar_abuela_clara"
            "mercader zafiro" -> "avatar_mercader_zafiro"
            "sabia aurelia" -> "avatar_sabia_aurelia"
            "jardinera luna" -> "avatar_jardinera_luna"
            "ermitaño verde", "ermitano verde" -> "avatar_ermitano_verde"
            "botánico arthur", "botanico arthur" -> "avatar_botanico_arthur"
            else -> ""
        }
        val name = if (exact.isNotEmpty()) exact else {
            val extras = listOf(
                "avatar_jardinera_2", "avatar_botanica_joven", "avatar_abuela_2",
                "avatar_mercader_azul", "avatar_sabia_2", "avatar_vieja_2"
            )
            extras[key.hashCode().absoluteValue % extras.size]
        }
        val ctx = appContext ?: return 0
        return resId(ctx.resources, name)
    }
}

/** Imagen de recurso generada, con memoria gestionada por Compose. */
@Composable
fun GeneratedImage(resId: Int, contentDescription: String?, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun rememberResources(): Resources = LocalContext.current.resources.also { GeneratedArt.init(LocalContext.current) }