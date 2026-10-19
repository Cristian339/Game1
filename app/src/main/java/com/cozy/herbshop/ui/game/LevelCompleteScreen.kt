package com.cozy.herbshop.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.ui.components.cozyBackground
import com.cozy.herbshop.ui.components.drawPaintedLeaf
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiPetal(
    val xFrac: Float,
    val delay: Float,
    val speed: Float,
    val sizePx: Float,
    val spin: Float,
    val sway: Float,
    val color: Color,
    val isLeaf: Boolean
)

@Composable
private fun PetalConfetti(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "confetti")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )

    val petals = remember {
        val palette = Herb.entries.map { it.defaultColor } + listOf(
            Color(0xFFFFF176),
            Color(0xFFFFCC80),
            Color(0xFFA5D6A7),
            Color(0xFFFFF59D)
        )
        val rnd = Random(777)
        List(34) { i ->
            ConfettiPetal(
                xFrac = rnd.nextFloat(),
                delay = rnd.nextFloat(),
                speed = 0.55f + rnd.nextFloat() * 0.9f,
                sizePx = 7f + rnd.nextFloat() * 11f,
                spin = rnd.nextFloat() * 360f,
                sway = (rnd.nextFloat() - 0.5f) * 0.22f,
                color = palette[i % palette.size],
                isLeaf = rnd.nextFloat() > 0.45f
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        petals.forEach { p ->
            val t = ((progress * p.speed + p.delay) % 1f)
            val y = -p.sizePx * 2f + t * (h + p.sizePx * 4f)
            val x = ((p.xFrac + p.sway * sin(t * Math.PI * 2.0).toFloat()) * w)
                .coerceIn(-20f, w + 20f)
            val alpha = (0.85f - t * 0.35f).coerceIn(0.25f, 0.9f)
            val rotation = p.spin + t * 420f

            if (p.isLeaf) {
                drawPaintedLeaf(
                    center = Offset(x, y),
                    length = p.sizePx * 1.6f,
                    width = p.sizePx * 0.55f,
                    color = p.color.copy(alpha = alpha),
                    rotationDeg = rotation
                )
            } else {
                withTransform({
                    rotate(rotation, pivot = Offset(x, y))
                }) {
                    drawOval(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x - p.sizePx / 2f, y - p.sizePx / 3.5f),
                        size = Size(p.sizePx, p.sizePx / 1.75f)
                    )
                    drawOval(
                        color = Color.White.copy(alpha = alpha * 0.4f),
                        topLeft = Offset(x - p.sizePx / 3f, y - p.sizePx / 3f),
                        size = Size(p.sizePx * 0.4f, p.sizePx * 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * Las tres chapas del nivel: terminarla en par, sin Pista y sin Deshacer.
 * Recompensan y nunca bloquean: el nivel ya esta terminado cuando se ven.
 */
@Composable
private fun MedalRow(
    movesCount: Int,
    parMoves: Int,
    usedHint: Boolean,
    usedUndo: Boolean
) {
    val chapas = listOf(
        Triple("🎯", "En par", movesCount <= parMoves),
        Triple("💡", "Sin Pista", !usedHint),
        Triple("↩️", "Sin Deshacer", !usedUndo)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Jugadas $movesCount · par $parMoves",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chapas.forEach { (icono, texto, ganada) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (ganada) icono else "🔒",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = texto,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (ganada) FontWeight.Bold else FontWeight.Normal,
                            color = if (ganada) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            val ganadas = chapas.count { it.third }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (ganadas == 3) {
                    "¡Las tres chapas! Este nivel lo tienes dominado."
                } else {
                    "$ganadas de 3 chapas. Repítelo para completarlas."
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LevelCompleteScreen(
    levelNumber: Int,
    unlockedHerb: Herb?,
    orderFulfilled: Boolean = false,
    bonusLeaves: Int = 0,
    baseReward: Int = 0,
    recipeName: String? = null,
    movesCount: Int? = null,
    parMoves: Int? = null,
    usedHint: Boolean = false,
    usedUndo: Boolean = false,
    /** El reto diario no da "siguiente nivel": se cierra al superarlo. */
    isDailyChallenge: Boolean = false,
    onNextLevelClicked: () -> Unit,
    onMenuClicked: () -> Unit
) {
    var animateBloom by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateBloom = true
    }

    val bloomScale by animateFloatAsState(
        targetValue = if (animateBloom) 1f else 0.3f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "bloomScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .cozyBackground()
    ) {
        PetalConfetti(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(bloomScale),
                    contentAlignment = Alignment.Center
                ) {
                    SparkleBurst(
                        modifier = Modifier.fillMaxSize(),
                        sparkles = 14,
                        seed = 7L,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    ),
                                    radius = 110f
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Park,
                            contentDescription = null,
                            modifier = Modifier.size(90.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isDailyChallenge) "¡Reto Diario Superado!" else "¡Nivel Resuelto!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = if (isDailyChallenge) {
                        "El reto de hoy queda cerrado: vuelve mañana por otro"
                    } else {
                        "Tu tienda de té ha florecido un poco más"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                if (movesCount != null && parMoves != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    MedalRow(movesCount = movesCount, parMoves = parMoves, usedHint = usedHint, usedUndo = usedUndo)
                }

                if (orderFulfilled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📜 ¡Pedido cumplido!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (recipeName != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = recipeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (bonusLeaves > 0) "+$bonusLeaves 🍃 de receta" else "Receta ya cobrada",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                val totalReward = baseReward + bonusLeaves
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalReward > 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (totalReward > 0) {
                            Text(
                                text = "🍃 +$totalReward Hojas de Oro",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (baseReward > 0 && bonusLeaves > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$baseReward por el nivel · $bonusLeaves por la receta",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        } else {
                            Text(
                                text = "🍃 Sin hojas extra",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ya completaste este nivel antes: el premio de la receta solo se cobra la primera vez.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                unlockedHerb?.let { herb ->
                    var herbPop by remember(herb) { mutableStateOf(false) }
                    LaunchedEffect(herb) {
                        delay(450)
                        herbPop = true
                    }
                    val herbScale by animateFloatAsState(
                        targetValue = if (herbPop) 1f else 0.75f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "herbPopScale"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .scale(herbScale),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box {
                            SparkleBurst(
                                modifier = Modifier.matchParentSize(),
                                sparkles = 10,
                                seed = 3L,
                                tint = herb.defaultColor
                            )
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    herb.defaultColor,
                                                    herb.defaultColor.copy(
                                                        red = herb.defaultColor.red * 0.7f,
                                                        green = herb.defaultColor.green * 0.7f,
                                                        blue = herb.defaultColor.blue * 0.7f
                                                    )
                                                ),
                                                center = Offset(16f, 14f),
                                                radius = 34f
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = herb.symbol, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "✨ ¡Nueva hierba en el Herbario!",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = herb.displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = herb.scientificName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // El reto del dia se cierra al superarlo: no hay "siguiente
                // nivel" que darle al jugador y no vuelve hasta manana.
                if (!isDailyChallenge) {
                    Button(
                        onClick = {
                            AudioHub.sound.playUiConfirm()
                            onNextLevelClicked()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = "Siguiente Nivel",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        AudioHub.sound.playUiClick()
                        onMenuClicked()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Menú Principal", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Chispas radiales animadas: da vida a los momentos de premio. */
@Composable
private fun SparkleBurst(
    modifier: Modifier = Modifier,
    sparkles: Int = 12,
    seed: Long = 0L,
    tint: Color = Color(0xFFFFD54F)
) {
    val progress = rememberInfiniteTransition(label = "sparkles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleProgress"
    )

    Canvas(modifier = modifier) {
        val rnd = Random(seed)
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = minOf(size.width, size.height) * 0.48f
        repeat(sparkles) {
            val angle = (it / sparkles.toFloat()) * 2f * Math.PI.toFloat() + rnd.nextFloat() * 0.4f
            val phase = rnd.nextFloat()
            val delay = phase * 0.6f
            val local = ((progress.value - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@repeat
            val radius = maxRadius * (0.25f + local * 0.75f)
            val alpha = (sin(local * Math.PI).toFloat()).coerceIn(0f, 1f)
            val dot = (2.2f + (1f - local) * 3.4f) * density
            drawCircle(
                color = tint.copy(alpha = alpha * 0.95f),
                radius = dot,
                center = Offset(
                    center.x + cos(angle) * radius,
                    center.y + sin(angle) * radius
                )
            )
            if (local < 0.35f) {
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.7f),
                    radius = dot * 0.45f,
                    center = Offset(
                        center.x + cos(angle) * radius,
                        center.y + sin(angle) * radius
                    )
                )
            }
        }
    }
}