package com.cozy.herbshop.ui.collection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozy.herbshop.R
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.ui.components.GeneratedArt

/** Fondo ilustrado de la habitacion. */
val showcaseBackgroundRes: Int = R.drawable.bg_showcase

data class DecoSlot(val id: String, val artRes: Int, val name: String, val description: String)

/** Los cuatro articulos de decoracion de la tienda. */
val decorationSlots: List<DecoSlot> = listOf(
    DecoSlot("estante_roble", R.drawable.deco_shelf_oak, "Estante de Roble Cozy", "Elegante estanteria artesanal para tus frascos."),
    DecoSlot("luces_hadas", R.drawable.deco_fairy_garland, "Guirnalda de Luces Calidas", "Luces de ambiente suave colgadas sobre el mostrador."),
    DecoSlot("estufa_calida", R.drawable.deco_stove_warm, "Estufa de Lenia Classica", "Aporta calor y aroma a te tostado."),
    DecoSlot("plantas_cozy", R.drawable.deco_fern_pots, "Macetas de Helechos Silvestres", "Plantas decorativas frescas junto a la entrada.")
)

data class AsmrSlot(val id: String, val artRes: Int, val name: String, val description: String)

/** Los seis packs ASMR. Solo uno puede estar sonando a la vez. */
val asmrSlots: List<AsmrSlot> = listOf(
    AsmrSlot("asmr_chimenea", R.drawable.asmr_cover_fire, "Chimenea de Lenia", "Chisporroteo calido de lenia para tus tardes de juego."),
    AsmrSlot("asmr_lluvia", R.drawable.asmr_cover_rain, "Lluvia Relajante", "Lluvia cayendo en la ventana mientras juegas."),
    AsmrSlot("asmr_arroyo", R.drawable.asmr_cover_stream, "Arroyo Tranquilo", "Agua corriendo entre piedras, suave y constante."),
    AsmrSlot("asmr_bosque", R.drawable.asmr_cover_forest, "Bosque en Calma", "Bosque con pajaros y hojas, como escuchar la montana."),
    AsmrSlot("asmr_noche", R.drawable.asmr_cover_night, "Noche Estrellada", "Grillos y cigarras en una noche calida de verano."),
)

/**
 * "Mi Coleccion": los objetos que ha comprado el jugador(update) colocados en la
 * habitacion. Cada objeto apoya en la balda o en el suelo con su sombra, no flota.
 *
 * El reparto de la pantalla sigue las medidas reales del fondo: los tres listones
 * de madera estan al 42 %, 55 % y 66 % de la altura, y el suelo empieza al 74 %.
 * Por eso las bandas del Column estan calculadas para que el fondo de cada
 * objeto caiga justo encima del liston que le toca.
 */
@Composable
fun CollectionShowcaseScreen(
    unlockedHerbs: Set<String>,
    unlockedDecorations: Set<String>,
    unlockedAsmrPacks: Set<String>,
    selectedAsmrPack: String?,
    goldLeaves: Int,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    showBackButton: Boolean = true,
    onBackClicked: () -> Unit = {},
    onHerbClick: (Herb) -> Unit = {},
    onAsmrSelected: (String?) -> Unit = {},
    onSlotClick: () -> Unit = {}
) {
    var info by remember { mutableStateOf<Pair<String, String>?>(null) }
    info?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { info = null },
            title = { Text(text = title, fontWeight = FontWeight.Bold) },
            text = { Text(text = body) },
            confirmButton = { Button(onClick = { info = null }) { Text("Cerrar") } }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showBackground) {
            Image(
                painter = painterResource(showcaseBackgroundRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Column con las bandas medidas: cada objeto apoya en su liston.
        Column(modifier = Modifier.fillMaxSize()) {

            if (showBackButton) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF3E2F22)
                        )
                    }
                    Text(
                        text = "Mi Coleccion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2F22)
                    )
                }
            }

            // Pared: 24 % de alto, aqui cuelgan los objetos de sonido
            WallZone(
                selectedAsmrPack = selectedAsmrPack,
                unlockedAsmrPacks = unlockedAsmrPacks,
                onAsmrSelected = onAsmrSelected,
                onSlotClick = onSlotClick,
                onInfo = { title, body -> info = title to body },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.24f)
            )

            // Balda 1 (apoya en el listón del 42 %)
            HerbRow(
                herbs = Herb.entries.take(3),
                unlockedHerbs = unlockedHerbs,
                onHerbClick = onHerbClick,
                onSlotClick = onSlotClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.181f)
            )

            // Balda 2 (listón del 55 %)
            HerbRow(
                herbs = Herb.entries.drop(3).take(3),
                unlockedHerbs = unlockedHerbs,
                onHerbClick = onHerbClick,
                onSlotClick = onSlotClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.124f)
            )

            // Balda 3 (listón del 66 %)
            HerbRow(
                herbs = Herb.entries.drop(6),
                unlockedHerbs = unlockedHerbs,
                onHerbClick = onHerbClick,
                onSlotClick = onSlotClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.12f)
            )

            // Suelo: los objetos de decoracion, de pie
            FloorZone(
                unlockedDecorations = unlockedDecorations,
                onSlotClick = onSlotClick,
                onInfo = { title, body -> info = title to body },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.335f)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xCC3E2F22))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🍃 $goldLeaves", color = Color(0xFFFFF3D6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// Pared: objetos de sonido colgados
// ---------------------------------------------------------------------------

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WallZone(
    selectedAsmrPack: String?,
    unlockedAsmrPacks: Set<String>,
    onAsmrSelected: (String?) -> Unit,
    onSlotClick: () -> Unit,
    onInfo: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Carrusel: se ven de uno en uno y se desliza para pasar al siguiente.
    // La última página es "silenciar", así el silencio está a un desliz sin salir.
    val pages: List<AsmrSlot?> = asmrSlots + null
    val startIndex = remember(selectedAsmrPack) {
        val i = asmrSlots.indexOfFirst { it.id == selectedAsmrPack }
        if (i >= 0) i else pages.lastIndex
    }
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { pages.size })

    LaunchedEffect(selectedAsmrPack) {
        val target = asmrSlots.indexOfFirst { it.id == selectedAsmrPack }
        val page = if (target >= 0) target else pages.lastIndex
        if (pagerState.currentPage != page) pagerState.animateScrollToPage(page)
    }

    Column(
        modifier = modifier.padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            pageSpacing = 0.dp,
            contentPadding = PaddingValues(horizontal = 40.dp)
        ) { page ->
            val slot = pages[page]
            val selected = slot != null && slot.id == selectedAsmrPack
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = slot?.name ?: "Silencio",
                    fontSize = 11.sp,
                    maxLines = 1,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color(0xFF4A3826)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.72f)
                        .aspectRatio(1f)
                        .shadow(if (selected) 6.dp else 2.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Color(0xCCFFF6E2) else Color(0x1AFFFFFF))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) Color(0xCCB98A4E) else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (slot == null) {
                                onAsmrSelected(null)
                            } else if (slot.id !in unlockedAsmrPacks) {
                                onSlotClick()
                            } else if (selected) {
                                onInfo(slot.name, slot.description)
                            } else {
                                onAsmrSelected(slot.id)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (slot == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔇", fontSize = 34.sp)
                            Text(
                                text = "Sin ambiente",
                                fontSize = 10.sp,
                                color = Color(0xFF5A4632),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (slot.id in unlockedAsmrPacks) {
                        ShadowedSprite(
                            artRes = slot.artRes,
                            contentDescription = slot.name,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF0C8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "♪", fontSize = 12.sp, color = Color(0xFF3E2F22))
                            }
                        }
                    } else {
                        EmptyHook(Modifier.fillMaxSize())
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                // Ristra de madera: el objeto cuelga de ella
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(7.dp)
                        .shadow(2.dp, RoundedCornerShape(2.dp))
                        .background(Color(0xFF8A5A34))
                )
            }
        }

        // Puntos de pagina
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            pages.forEachIndexed { index, _ ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(width = if (active) 14.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) Color(0xFF8A5A34) else Color(0x558A5A34))
                )
            }
        }
    }
}

/**
 * Sprite con sombra suave: se copia la silueta del objeto en negro con tres
 * desplazamientos decrecientes, de forma que el objeto se separa del color de
 * fondo sin montar un recuadro oscuro. Es lo que evita que una hierba clara o
 * una decoracion se pierda contra la pared crema.
 */
@Composable
private fun ShadowedSprite(
    artRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (artRes == 0) return
    val painter = painterResource(artRes)
    listOf(3.2f to 0.09f, 2.2f to 0.13f, 1.2f to 0.18f).forEach { (off, alpha) ->
        Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier.offset(x = off.dp, y = (off * 1.15f).dp),
            colorFilter = ColorFilter.tint(Color.Black.copy(alpha = alpha)),
            contentScale = ContentScale.Fit
        )
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/**
 * Hueco de un objeto que el jugador todavia no ha comprado: capa gris encima y
 * aviso claro de que falta, en vez de dejar el hueco vacio sin explicacion.
 */
@Composable
private fun EmptyHook(modifier: Modifier = Modifier, label: String = "No comprada") {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x4D3A342E)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔒", fontSize = 18.sp)
            Text(
                text = label,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF6EFE4),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Balda: hierbas apoyadas en el listón
// ---------------------------------------------------------------------------

@Composable
private fun HerbRow(
    herbs: List<Herb>,
    unlockedHerbs: Set<String>,
    onHerbClick: (Herb) -> Unit,
    onSlotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        herbs.forEach { herb ->
            HerbSpot(
                herb = herb,
                owned = herb.id in unlockedHerbs,
                onHerbClick = onHerbClick,
                onSlotClick = onSlotClick
            )
        }
    }
}

@Composable
private fun RowScope.HerbSpot(
    herb: Herb,
    owned: Boolean,
    onHerbClick: (Herb) -> Unit,
    onSlotClick: () -> Unit
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { if (owned) onHerbClick(herb) else onSlotClick() },
            contentAlignment = Alignment.BottomCenter
        ) {
            if (owned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(24.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x4A000000))
                )
                ShadowedSprite(
                    artRes = GeneratedArt.resIdFor(herb),
                    contentDescription = herb.displayName,
                    modifier = Modifier.fillMaxHeight(0.80f)
                )
            } else {
                EmptyHook(Modifier.fillMaxSize(), label = "Sin\ndescubrir")
            }
        }
        Text(
            text = if (owned) herb.displayName else "",
            fontSize = 9.sp,
            maxLines = 1,
            color = if (owned) Color(0xFF4A3826) else Color(0x77FFFFFF),
            fontWeight = if (owned) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ---------------------------------------------------------------------------
// Suelo: decoracion de pie
// ---------------------------------------------------------------------------

@Composable
private fun FloorZone(
    unlockedDecorations: Set<String>,
    onSlotClick: () -> Unit,
    onInfo: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        decorationSlots.forEach { slot ->
            val owned = slot.id in unlockedDecorations
            Column(
                modifier = Modifier.width(78.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { if (owned) onInfo(slot.name, slot.description) else onSlotClick() },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (owned) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .width(34.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x55000000))
                        )
                        ShadowedSprite(
                            artRes = slot.artRes,
                            contentDescription = slot.name,
                            modifier = Modifier.fillMaxHeight(0.82f)
                        )
                    } else {
                        EmptyHook(Modifier.fillMaxSize(), label = "No\ncomprada")
                    }
                }
                Text(
                    text = if (owned) "✓" else "",
                    fontSize = 11.sp,
                    color = Color(0xFF3E2F22),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
