package com.cozy.herbshop.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val POUR_LABEL = "jarPour"

private fun Color.jarLighten(factor: Float): Color = Color(
    red = (red + (1f - red) * factor).coerceIn(0f, 1f),
    green = (green + (1f - green) * factor).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.jarDarken(factor: Float): Color = Color(
    red = (red * (1f - factor)).coerceIn(0f, 1f),
    green = (green * (1f - factor)).coerceIn(0f, 1f),
    blue = (blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = alpha
)

/** Capa de hierba con gradiente pintado, moteado y brillo de superficie. */
private fun DrawScope.drawHerbLayer(
    topLeft: Offset,
    size: Size,
    layer: JarLayer,
    layerIndex: Int,
    jarId: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val corner = CornerRadius(7.dp.toPx(), 7.dp.toPx())
    val baseColor = if (layer.isHidden) Color(0xFF78909C) else layer.herb.defaultColor
    val topColor = if (layer.isHidden) Color(0xFF90A4AE) else baseColor.jarLighten(0.22f)
    val bottomColor = if (layer.isHidden) Color(0xFF546E7A) else baseColor.jarDarken(0.18f)

    val fill = Brush.verticalGradient(
        colors = listOf(topColor, baseColor, bottomColor),
        startY = topLeft.y,
        endY = topLeft.y + size.height
    )
    drawRoundRect(brush = fill, topLeft = topLeft, size = size, cornerRadius = corner)

    drawRoundRect(
        color = bottomColor.jarDarken(0.15f).copy(alpha = 0.8f),
        topLeft = topLeft,
        size = size,
        cornerRadius = corner,
        style = Stroke(width = 1.4.dp.toPx())
    )

    if (!layer.isHidden) {
        val rnd = Random(jarId * 9973 + layerIndex * 131 + layer.herb.ordinal * 17)
        repeat(9) {
            val sx = topLeft.x + 3.dp.toPx() + rnd.nextFloat() * (size.width - 6.dp.toPx())
            val sy = topLeft.y + 3.dp.toPx() + rnd.nextFloat() * (size.height - 6.dp.toPx())
            val sr = (0.6f + rnd.nextFloat() * 1.5f).dp.toPx()
            drawCircle(color = topColor.jarLighten(0.3f).copy(alpha = 0.45f), radius = sr, center = Offset(sx, sy))
        }
        repeat(5) {
            val sx = topLeft.x + 4.dp.toPx() + rnd.nextFloat() * (size.width - 8.dp.toPx())
            val sy = topLeft.y + 4.dp.toPx() + rnd.nextFloat() * (size.height - 8.dp.toPx())
            val sr = (1.2f + rnd.nextFloat() * 1.8f).dp.toPx()
            drawCircle(color = bottomColor.jarDarken(0.25f).copy(alpha = 0.55f), radius = sr, center = Offset(sx, sy))
        }
        repeat(3) {
            val sx = topLeft.x + 6.dp.toPx() + rnd.nextFloat() * (size.width - 12.dp.toPx())
            val sy = topLeft.y + 5.dp.toPx() + rnd.nextFloat() * (size.height - 10.dp.toPx())
            drawPaintedLeaf(
                center = Offset(sx, sy),
                length = size.width * 0.22f,
                width = size.width * 0.07f,
                color = topColor.jarLighten(0.15f).copy(alpha = 0.85f),
                rotationDeg = rnd.nextFloat() * 60f - 30f
            )
        }
    }

    val gloss = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f), Color.Transparent),
        startY = topLeft.y,
        endY = topLeft.y + size.height * 0.55f
    )
    drawRoundRect(
        brush = gloss,
        topLeft = Offset(topLeft.x + 1.5.dp.toPx(), topLeft.y + 1.5.dp.toPx()),
        size = Size(size.width - 3.dp.toPx(), size.height * 0.5f),
        cornerRadius = corner
    )

    if (!layer.isHidden && !layer.isFrozen) {
        val symbolStyle = TextStyle(fontSize = 12.sp)
        val layout = textMeasurer.measure(layer.herb.symbol, symbolStyle)
        drawText(
            textMeasurer = textMeasurer,
            text = layer.herb.symbol,
            style = symbolStyle,
            topLeft = Offset(
                x = topLeft.x + (size.width - layout.size.width) / 2f,
                y = topLeft.y + (size.height - layout.size.height) / 2f
            )
        )
    }

    if (layer.isHidden) {
        val hiddenStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        val layout = textMeasurer.measure("?", hiddenStyle)
        drawText(
            textMeasurer = textMeasurer,
            text = "?",
            style = hiddenStyle,
            topLeft = Offset(
                x = topLeft.x + (size.width - layout.size.width) / 2f,
                y = topLeft.y + (size.height - layout.size.height) / 2f
            )
        )
    }

    if (layer.isFrozen) {
        drawRoundRect(
            color = Color(0xB3E0F7FA),
            topLeft = topLeft,
            size = size,
            cornerRadius = corner
        )
        val iceShine = Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
            start = Offset(topLeft.x, topLeft.y),
            end = Offset(topLeft.x + size.width, topLeft.y + size.height)
        )
        drawRoundRect(brush = iceShine, topLeft = topLeft, size = size, cornerRadius = corner)
        for (i in 0..3) {
            val fx = topLeft.x + size.width * (0.18f + i * 0.22f)
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(fx, topLeft.y + 2.dp.toPx()),
                end = Offset(fx + 6.dp.toPx(), topLeft.y + size.height - 2.dp.toPx()),
                strokeWidth = 1.4.dp.toPx()
            )
        }
        val frozenStyle = TextStyle(color = Color(0xFF0288D1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        val layout = textMeasurer.measure("❄", frozenStyle)
        drawText(
            textMeasurer = textMeasurer,
            text = "❄",
            style = frozenStyle,
            topLeft = Offset(
                x = topLeft.x + (size.width - layout.size.width) / 2f,
                y = topLeft.y + (size.height - layout.size.height) / 2f
            )
        )
    }
}

/** Corcho de madera o lacre con vetas, bisel y brillo. */
private fun DrawScope.drawCork(sealed: Boolean) {
    val radius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
    val fill = if (sealed) {
        Brush.verticalGradient(
            listOf(Color(0xFFEF5350), Color(0xFFC62828), Color(0xFF8E0000)),
            startY = 0f,
            endY = size.height
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFC9A227).jarLighten(0.35f), Color(0xFFBC8A5F), Color(0xFF6D4C41)),
            startY = 0f,
            endY = size.height
        )
    }
    drawRoundRect(brush = fill, size = size, cornerRadius = radius)

    if (!sealed) {
        val rnd = Random(31)
        repeat(6) {
            val y = size.height * (0.2f + rnd.nextFloat() * 0.6f)
            val x0 = size.width * rnd.nextFloat() * 0.2f
            val x1 = size.width * (0.7f + rnd.nextFloat() * 0.3f)
            drawLine(
                color = Color(0xFF4E342E).copy(alpha = 0.35f),
                start = Offset(x0, y),
                end = Offset(x1, y + (rnd.nextFloat() - 0.5f) * 3f),
                strokeWidth = 1.2f
            )
        }
    } else {
        drawCircle(color = Color(0xFFFFD54F), radius = size.height * 0.28f, center = Offset(size.width * 0.5f, size.height * 0.5f))
        drawCircle(color = Color(0xFFFFF8E1), radius = size.height * 0.14f, center = Offset(size.width * 0.5f, size.height * 0.5f))
    }

    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
            startY = 1f,
            endY = size.height * 0.45f
        ),
        topLeft = Offset(2.dp.toPx(), 1.dp.toPx()),
        size = Size(size.width - 4.dp.toPx(), size.height * 0.45f),
        cornerRadius = radius
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.22f),
        size = size,
        cornerRadius = radius,
        style = Stroke(width = 1.6.dp.toPx())
    )
}

/** Acabados de vidrio: reflejos, sombra interior y borde superior. */
private fun DrawScope.drawGlassOverlay() {
    val w = size.width
    val h = size.height

    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
            startY = 0f,
            endY = h * 0.14f
        )
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.10f)),
            startY = h * 0.75f,
            endY = h
        )
    )

    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.10f), Color.Transparent),
            startX = 0f,
            endX = w * 0.42f
        ),
        topLeft = Offset(1.5.dp.toPx(), 3.dp.toPx()),
        size = Size(w * 0.30f, h - 8.dp.toPx()),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.22f)),
            startX = w * 0.72f,
            endX = w
        ),
        topLeft = Offset(w * 0.74f, 5.dp.toPx()),
        size = Size(w * 0.22f, h - 14.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    drawOval(
        color = Color.White.copy(alpha = 0.38f),
        topLeft = Offset(3.dp.toPx(), 0.5.dp.toPx()),
        size = Size(w - 6.dp.toPx(), 5.dp.toPx())
    )
    drawOval(
        color = Color(0xFF546E7A).copy(alpha = 0.25f),
        topLeft = Offset(3.dp.toPx(), h - 4.dp.toPx()),
        size = Size(w - 6.dp.toPx(), 4.dp.toPx())
    )
}

private fun DrawScope.drawWaxSeal() {
    val center = Offset(size.width / 2f, 20.dp.toPx())
    val outer = 13.dp.toPx()
    drawCircle(color = Color(0xFFB71C1C).copy(alpha = 0.35f), radius = outer + 3.dp.toPx(), center = center)
    val sealFill = Brush.radialGradient(
        colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C), Color(0xFF7F0000)),
        center = Offset(center.x - 3f, center.y - 3f),
        radius = outer
    )
    drawCircle(brush = sealFill, radius = outer, center = center)
    for (i in 0 until 8) {
        val ang = (i / 8f) * 2f * Math.PI.toFloat()
        val p0 = Offset(center.x + cos(ang) * outer * 0.55f, center.y + sin(ang) * outer * 0.55f)
        val p1 = Offset(center.x + cos(ang) * outer * 0.9f, center.y + sin(ang) * outer * 0.9f)
        drawLine(color = Color(0xFFFFD54F).copy(alpha = 0.9f), start = p0, end = p1, strokeWidth = 2f)
    }
    drawCircle(color = Color(0xFFFFD54F), radius = outer * 0.42f, center = center)
    drawCircle(color = Color(0xFFFFECB3), radius = outer * 0.20f, center = Offset(center.x - 1.5f, center.y - 1.5f))
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = 2.5.dp.toPx(),
        center = Offset(center.x - outer * 0.35f, center.y - outer * 0.4f)
    )
}

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
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steamOffsetY"
    )
    val steamAlpha by infiniteSteam.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steamAlpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = when {
            isPourSource -> -40f
            isSelected -> -10f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 280),
        label = "jarSelectionOffset"
    )

    val shiftX by animateFloatAsState(
        targetValue = if (isPourSource) (if (pourAngle >= 0f) 22f else -22f) else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "jarPourShift"
    )

    val rotationZ by animateFloatAsState(
        targetValue = when {
            isPourSource -> pourAngle
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
        label = "jarRotationOffset"
    )

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

    val jarShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    val outlineColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.tertiary

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.width(38.dp).height(13.dp)) {
                drawCork(jar.isSealed)
            }

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(140.dp)
                    .shadow(
                        elevation = if (isPourSource) 12.dp else 5.dp,
                        shape = jarShape,
                        clip = false,
                        ambientColor = Color(0x44000000),
                        spotColor = Color(0x55000000)
                    )
                    .clip(jarShape)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.30f),
                                Color(0xFFCFD8DC).copy(alpha = 0.18f),
                                Color(0xFF90A4AE).copy(alpha = 0.22f)
                            )
                        )
                    )
                    .border(
                        width = if (isSelected) 3.dp else 2.dp,
                        color = if (isSelected) accentColor else outlineColor.copy(alpha = 0.35f),
                        shape = jarShape
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    val capacity = jar.capacity
                    val layerHeight = size.height / capacity
                    val inset = 4.dp.toPx()

                    jar.layers.forEachIndexed { index, layer ->
                        val yTop = size.height - ((index + 1) * layerHeight)
                        val topLeft = Offset(inset, yTop + 2.dp.toPx())
                        val layerSize = Size(size.width - inset * 2, layerHeight - 4.dp.toPx())
                        drawHerbLayer(
                            topLeft = topLeft,
                            size = layerSize,
                            layer = layer,
                            layerIndex = index,
                            jarId = jar.id,
                            textMeasurer = textMeasurer
                        )
                    }

                    if (jar.isCompleted && !jar.isEmpty) {
                        val shimmer = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.0f),
                                SoftGlow,
                                Color.White.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = size.height
                        )
                        drawRect(brush = shimmer, size = Size(size.width * 0.2f, size.height), topLeft = Offset(size.width * 0.55f, 0f))
                    }

                    drawGlassOverlay()

                    if (jar.isSealed) {
                        drawWaxSeal()
                    }
                }
            }
        }

        if (jar.isCompleted && !jar.isEmpty) {
            Canvas(
                modifier = Modifier
                    .width(38.dp)
                    .height(24.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-18).dp)
            ) {
                listOf(0.3f, 0.5f, 0.7f).forEachIndexed { i, fx ->
                    val r = (3f + i * 0.5f).dp.toPx()
                    val cy = size.height - 4f + steamOffsetY + i * 2f
                    if (cy > 0f) {
                        drawCircle(
                            color = Color.White.copy(alpha = (steamAlpha + i * 0.05f).coerceIn(0f, 0.55f)),
                            radius = r,
                            center = Offset(size.width * fx, cy)
                        )
                    }
                }
            }
        }
    }
}

private val SoftGlow = Color(0x33FFFFFF)
