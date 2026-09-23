package com.cozy.herbshop.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozy.herbshop.data.model.Jar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CozyJarItem(
    jar: Jar,
    isSelected: Boolean,
    isPourSource: Boolean = false,
    isPourTarget: Boolean = false,
    pourAngle: Float = 0f,
    onClick: () -> Unit,
    onPositioned: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val infiniteSteam = rememberInfiniteTransition(label = "steam")
    val steamOffsetY by infiniteSteam.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steamOffsetY"
    )

    val offsetY by animateFloatAsState(
        targetValue = when {
            isPourSource -> -40f
            isSelected -> -18f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 280),
        label = "jarSelectionOffset"
    )

    // El frasco origen se acerca al destino mientras se inclina (parece que lo alcance)
    val shiftX by animateFloatAsState(
        targetValue = if (isPourSource) (if (pourAngle >= 0f) 22f else -22f) else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "jarPourShift"
    )

    // Inclinación con muelle: ligero rebote al llegar al ángulo de vertido
    val rotationZ by animateFloatAsState(
        targetValue = when {
            isPourSource -> pourAngle
            isSelected -> -10f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
        label = "jarRotationOffset"
    )

    // Temblor sutil de la mano mientras se vierte
    val infiniteTilt = rememberInfiniteTransition(label = "pourWobble")
    val pourWobble by infiniteTilt.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pourWobbleValue"
    )

    val pourScale by animateFloatAsState(
        targetValue = if (isPourSource) 1.08f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "jarPourScale"
    )

    // Impacto en el frasco destino cuando el chorro lo alcanza (~430ms):
    // se hunde y estira un instante, luego el muelle lo devuelve a su sitio
    val targetLift = remember { Animatable(0f) }
    val targetSquashX = remember { Animatable(1f) }
    val targetSquashY = remember { Animatable(1f) }
    LaunchedEffect(isPourTarget) {
        if (isPourTarget) {
            delay(430)
            targetLift.snapTo(-7f)
            targetSquashX.snapTo(1.06f)
            targetSquashY.snapTo(0.93f)
            launch { targetLift.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 900f)) }
            launch { targetSquashX.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 700f)) }
            launch { targetSquashY.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 700f)) }
        } else {
            targetLift.snapTo(0f)
            targetSquashX.snapTo(1f)
            targetSquashY.snapTo(1f)
        }
    }

    val effectiveRotation = rotationZ + if (isPourSource) pourWobble else 0f
    val effectiveOffsetY = offsetY + if (isPourTarget) targetLift.value else 0f
    val effectiveScaleX = pourScale * (if (isPourTarget) targetSquashX.value else 1f)
    val effectiveScaleY = pourScale * (if (isPourTarget) targetSquashY.value else 1f)
    val transformOrigin = when {
        isPourSource && pourAngle > 0f -> TransformOrigin(0.62f, 0.9f)
        isPourSource && pourAngle < 0f -> TransformOrigin(0.38f, 0.9f)
        else -> TransformOrigin(0.5f, 0.85f)
    }

    Box(
        modifier = modifier
            .offset(x = shiftX.dp, y = effectiveOffsetY.dp)
            .graphicsLayer(
                rotationZ = effectiveRotation,
                scaleX = effectiveScaleX,
                scaleY = effectiveScaleY,
                transformOrigin = transformOrigin
            )
            .onGloballyPositioned { coordinates ->
                onPositioned?.invoke(
                    Rect(
                        coordinates.positionInRoot().x,
                        coordinates.positionInRoot().y,
                        coordinates.positionInRoot().x + coordinates.size.width,
                        coordinates.positionInRoot().y + coordinates.size.height
                    )
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.width(36.dp).height(12.dp)) {
                val corkColor = if (jar.isSealed) Color(0xFFC62828) else Color(0xFF8D6E63)
                drawRoundRect(
                    color = corkColor,
                    size = size,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                if (jar.isCompleted && !jar.isEmpty) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = 3.dp.toPx(),
                        center = Offset(size.width * 0.3f, steamOffsetY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = 4.dp.toPx(),
                        center = Offset(size.width * 0.5f, steamOffsetY - 4f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = 3.dp.toPx(),
                        center = Offset(size.width * 0.7f, steamOffsetY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .border(
                        width = if (isSelected) 3.dp else 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    val capacity = jar.capacity
                    val layerHeight = size.height / capacity

                    jar.layers.forEachIndexed { index, layer ->
                        val yTop = size.height - ((index + 1) * layerHeight)
                        val layerColor = if (layer.isHidden) Color(0xFF78909C) else layer.herb.defaultColor
                        val topLeft = Offset(4.dp.toPx(), yTop + 2.dp.toPx())
                        val layerSize = Size(size.width - 8.dp.toPx(), layerHeight - 4.dp.toPx())

                        drawRoundRect(
                            color = layerColor,
                            topLeft = topLeft,
                            size = layerSize,
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        if (!layer.isHidden && !layer.isFrozen) {
                            // Símbolo de accesibilidad para daltónicos
                            drawText(
                                textMeasurer = textMeasurer,
                                text = layer.herb.symbol,
                                style = TextStyle(
                                    fontSize = 12.sp
                                ),
                                topLeft = Offset(
                                    x = topLeft.x + (layerSize.width / 2) - 6.dp.toPx(),
                                    y = topLeft.y + (layerSize.height / 2) - 9.dp.toPx()
                                )
                            )
                        }

                        if (layer.isHidden) {
                            drawText(
                                textMeasurer = textMeasurer,
                                text = "?",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                topLeft = Offset(
                                    x = topLeft.x + (layerSize.width / 2) - 6.dp.toPx(),
                                    y = topLeft.y + (layerSize.height / 2) - 12.dp.toPx()
                                )
                            )
                        }

                        if (layer.isFrozen) {
                            drawRoundRect(
                                color = Color(0xB3E0F7FA),
                                topLeft = topLeft,
                                size = layerSize,
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawText(
                                textMeasurer = textMeasurer,
                                text = "❄",
                                style = TextStyle(
                                    color = Color(0xFF0288D1),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                topLeft = Offset(
                                    x = topLeft.x + (layerSize.width / 2) - 6.dp.toPx(),
                                    y = topLeft.y + (layerSize.height / 2) - 10.dp.toPx()
                                )
                            )
                        }
                    }

                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(8.dp.toPx(), 8.dp.toPx()),
                        end = Offset(8.dp.toPx(), size.height - 12.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )

                    if (jar.isSealed) {
                        // Sello de cera rojo decorativo
                        drawCircle(
                            color = Color(0xFFB71C1C),
                            radius = 12.dp.toPx(),
                            center = Offset(size.width / 2, 18.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = 6.dp.toPx(),
                            center = Offset(size.width / 2, 18.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
