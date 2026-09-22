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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun CozyJarItem(
    jar: Jar,
    isSelected: Boolean,
    onClick: () -> Unit,
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
        targetValue = if (isSelected) -18f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "jarSelectionOffset"
    )

    val rotationZ by animateFloatAsState(
        targetValue = if (isSelected) -12f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "jarRotationOffset"
    )

    Box(
        modifier = modifier
            .offset(y = offsetY.dp)
            .graphicsLayer(rotationZ = rotationZ)
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
