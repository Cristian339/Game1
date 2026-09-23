package com.cozy.herbshop.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.ui.components.cozyBackground
import com.cozy.herbshop.ui.components.drawPaintedLeaf
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

@Composable
fun LevelCompleteScreen(
    levelNumber: Int,
    unlockedHerb: Herb?,
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
                        .scale(bloomScale)
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "¡Nivel Resuelto!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Tu tienda de té ha florecido un poco más",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                unlockedHerb?.let { herb ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
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
                                    text = "¡Nueva hierba en el Herbario!",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = herb.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

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
