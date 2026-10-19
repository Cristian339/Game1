package com.cozy.herbshop.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.ui.components.GameScene
import com.cozy.herbshop.ui.components.ScenePreviewThumb
import com.cozy.herbshop.ui.components.cozyBackground

/**
 * Ultimo nivel del mapa. A partir de aqui se le dice al jugador que el camino
 * sigue: mejor una seccion honesta que un hueco sin explicar.
 */
private const val TOTAL_LEVELS = 50

@Composable
fun LevelMapScreen(
    repository: com.cozy.herbshop.data.repository.UserPreferencesRepository,
    onLevelSelected: (Int) -> Unit,
    onBackClicked: () -> Unit,
    levelRepository: LevelRepository = LevelRepository(null)
) {
    val currentLevel by repository.currentLevelFlow.collectAsState(initial = 1)

    // La ficha se deduce de las reglas del generador, sin crear niveles: pedir
    // los cincuenta era lo que hacia caer la pantalla.
    val catalog = remember { LevelCatalog(levelRepository, TOTAL_LEVELS) }
    val fichas = remember(catalog) { catalog.all().associateBy { it.number } }

    var selected by remember { mutableStateOf<Int?>(null) }

    // Todos los niveles jugables: el bloqueo se comprueba al entrar, no en el mapa.
    val unlockedCount = TOTAL_LEVELS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .cozyBackground()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp)
        ) {
            IconButton(onClick = {
                AudioHub.sound.playUiBack()
                onBackClicked()
            }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mapa de Niveles",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    // currentLevel es el siguiente nivel jugable, no el ultimo
                    // superado: el contador va uno por detras.
                    text = buildString {
                        append("$unlockedCount niveles · ${(currentLevel - 1).coerceIn(0, TOTAL_LEVELS)} ")
                        append(if ((currentLevel - 1).coerceIn(0, TOTAL_LEVELS) == 1) "completado" else "completados")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Una cabecera por sector: al desplazarse se ve en que tramo esta
            // el jugador y cuanto le queda para la siguiente puerta.
            GameScene.allSectors().forEach { sector ->
                item(key = "sector-${sector.from}", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    SectorHeader(
                        sector = sector,
                        currentLevel = currentLevel,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items((sector.from..sector.to).toList(), key = { "nivel-$it" }) { levelNum ->
                    val info = fichas[levelNum]
                    if (info != null) {
                        LevelCard(
                            info = info,
                            isCompleted = levelNum < currentLevel,
                            isCurrent = levelNum == currentLevel,
                            isUnlocked = levelNum <= unlockedCount,
                            onClick = {
                                AudioHub.sound.playUiClick()
                                selected = if (selected == levelNum) null else levelNum
                            }
                        )
                    }
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                ComingSoonSection(modifier = Modifier.padding(top = 8.dp))
            }
        }

        AnimatedVisibility(
            visible = selected != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selected?.let { levelNum ->
                val info = fichas[levelNum]
                if (info != null) {
                    LevelDetailSheet(
                        info = info,
                        isCompleted = levelNum < currentLevel,
                        isUnlocked = levelNum <= unlockedCount,
                        onPlay = {
                            AudioHub.sound.playUiConfirm()
                            selected = null
                            onLevelSelected(levelNum)
                        },
                        onDismiss = {
                            AudioHub.sound.playUiBack()
                            selected = null
                        }
                    )
                }
            }
        }
    }
}

/**
 * Cabecera de un sector: su nombre, el ambiente y como va de ese tramo. Al
 * desplazarse el mapa se lee como un camino con etapas, no como una lista.
 *
 * La barra llega hasta el final del sector, que es la puerta de hierba: asi se
 * ve de un vistazo quantas plantas le faltan para lo que viene.
 */
@Composable
private fun SectorHeader(
    sector: GameScene.Sector,
    currentLevel: Int,
    modifier: Modifier = Modifier
) {
    val totalNiveles = sector.to - sector.from + 1
    val hechos = (currentLevel - sector.from).coerceIn(0, totalNiveles)
    val completado = hechos >= totalNiveles
    val empieza = hechos == 0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScenePreviewThumb(
                scene = sector.scene,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sector.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val estado = if (completado) {
                    "🌿 puerta superada"
                } else {
                    "🌿 puerta en el ${sector.to}"
                }
                Text(
                    text = "Niveles ${sector.from}-${sector.to} · $estado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(hechos.toFloat() / totalNiveles)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            completado -> MaterialTheme.colorScheme.tertiary
                            empieza -> Color.Transparent
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
    }
}

/** Tarjeta de un nivel: miniatura del escenario, numero y estado. */
@Composable
private fun LevelCard(
    info: LevelInfo,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isCompleted -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    Card(
        onClick = onClick,
        enabled = isUnlocked,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                if (isUnlocked) {
                    ScenePreviewThumb(
                        scene = info.scene,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Velo para que el numero se lea sobre cualquier imagen.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.45f))
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isCurrent) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(bottomEnd = 12.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "Ahora",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD54F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Completado",
                            tint = Color(0xFF6D4C00),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (isUnlocked) "${info.number}" else "·",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                if (isUnlocked) {
                    if (info.isGate) {
                        Text(
                            text = "🌿 Puerta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = info.scene.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Detalle de un nivel: tramo, mecanicas, receta y herbario que gana. */
@Composable
private fun LevelDetailSheet(
    info: LevelInfo,
    isCompleted: Boolean,
    isUnlocked: Boolean,
    onPlay: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScenePreviewThumb(
                        scene = info.scene,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nivel ${info.number}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = info.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Completado",
                            tint = Color(0xFFFFD54F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = info.mechanics,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                info.newMechanic?.let { mecanica ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎓 Aquí se estrena: ${mecanica.title}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                val order = info.order
                if (order != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "📜 ${order.recipeName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.dialogue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Receta (en este orden, en la Tetera)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        info.recipe.forEachIndexed { position, herb ->
                            RecipeSlot(herb = herb, position = position + 1)
                        }
                    }

                    if (info.unlockedHerb != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🌿 Supera este nivel y desbloqueas: ${info.unlockedHerb.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cerrar")
                    }
                    androidx.compose.material3.Button(
                        onClick = onPlay,
                        enabled = isUnlocked,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isCompleted) "Repetir" else "Jugar")
                    }
                }
            }
        }
    }
}

/** Una ranura de la receta, en el orden en que hay que servirla. */
@Composable
private fun RecipeSlot(herb: Herb, position: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(herb.defaultColor)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(9.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = herb.symbol, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "$position",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
