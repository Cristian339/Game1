package com.cozy.herbshop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.cozy.herbshop.R

/**
 * Arte de escenario generado para el juego: fondo pintado a sangre completa y una
 * capa frontal recortada que enmarca la parte baja de la pantalla.
 *
 * Los fondos se recortan con ContentScale.Crop para cubrir cualquier proporción
 * sin deformarse; las capas frontales se ajustan por ancho y se alinean abajo,
 * de modo que la zona central de juego siempre queda libre.
 */
/** Miniatura cuadrada del escenario (vistas previas de niveles). */
val GameScene.previewArtRes: Int
    get() = when (this) {
        GameScene.MORNING -> R.drawable.sky_morning
        GameScene.GREENHOUSE -> R.drawable.sky_greenhouse
        GameScene.NIGHT -> R.drawable.sky_night
        GameScene.FOREST -> R.drawable.sky_forest
        GameScene.AUTUMN -> R.drawable.sky_autumn
    }

@Composable
fun SceneArtwork(
    scene: GameScene,
    modifier: Modifier = Modifier,
    dark: Boolean = isSystemInDarkTheme()
) {
    Box(modifier = modifier.background(Color(0xFFF3E9D8))) {
        // El arte esta dibujado en horizontal (unos 1400x700). Con
        // ContentScale.Crop sobre una pantalla de 1080x2400 solo se ve la franja
        // central, un 25% del ancho: en el invernadero eso partia la estanteria
        // en dos y dejaba un tramo de suelo vacio, y los helechos de primer plano
        // quedaban cortados por los bordes y parecían manchas. El escenario se
        // pinta con el canvas procedural, que esta hecho para esta proporcion y
        // no se deforma ni se recorta.
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .gameSceneBackground(scene)
        )
        if (dark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x66000000), Color(0x33000000), Color(0x59000000))
                        )
                    )
            )
        }
    }
}

/** Miniatura de escenario con bordes redondeados, para listas y selectores. */
@Composable
fun ScenePreviewThumb(scene: GameScene, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(scene.previewArtRes),
        contentDescription = scene.displayName,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

/**
 * Capa de decoración de la tienda: coloca el arte generado de cada artículo
 * comprado sobre el fondo ilustrado del menú principal.
 */
@Composable
fun ShopDecorationLayer(
    unlocked: Set<String>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if ("luces_hadas" in unlocked) {
            Image(
                painter = painterResource(R.drawable.deco_fairy_garland),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.82f),
                contentScale = ContentScale.FillWidth
            )
        }
        if ("plantas_cozy" in unlocked) {
            Image(
                painter = painterResource(R.drawable.deco_fern_pots),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.34f),
                contentScale = ContentScale.FillWidth
            )
        }
        if ("estufa_calida" in unlocked) {
            Image(
                painter = painterResource(R.drawable.deco_stove_warm),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.3f),
                contentScale = ContentScale.FillWidth
            )
        }
        if ("estante_roble" in unlocked) {
            Image(
                painter = painterResource(R.drawable.deco_shelf_oak),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.26f),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}