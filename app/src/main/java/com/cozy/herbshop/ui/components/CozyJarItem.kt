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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.scale
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
/**
 * Agrupa las capas contiguas de la misma hierba (y mismo estado) en una sola
 * columna de liquido. Al agrupar, dos Layers iguales se ven como una unica
 * columna continua en lugar de dos bloques; una capa helada o pegajosa corta el
 * grupo para poder pintar su marca encima.
 */
private class LiquidRun(
    val herb: Herb,
    val firstIndex: Int,
    val lastIndex: Int,
    val hidden: Boolean,
    val frozen: Boolean,
    val sticky: Boolean
)

private fun List<JarLayer>.groupLiquidRuns(): List<LiquidRun> {
    val runs = mutableListOf<LiquidRun>()
    this.forEachIndexed { index, layer ->
        val last = runs.lastOrNull()
        if (last != null && last.herb == layer.herb && last.hidden == layer.isHidden &&
            last.frozen == layer.isFrozen && last.sticky == layer.isSticky
        ) {
            runs[runs.size - 1] = LiquidRun(
                last.herb, last.firstIndex, index, last.hidden, last.frozen, last.sticky
            )
        } else {
            runs.add(LiquidRun(layer.herb, index, index, layer.isHidden, layer.isFrozen, layer.isSticky))
        }
    }
    return runs
}

/** Costura de miel con grapas: es la marca de que esa capa hace de tope. */
private fun DrawScope.drawStickyMark(topLeft: Offset, size: Size) {
    val seamY = topLeft.y + size.height * 0.28f
    drawLine(
        color = Color(0xFF6D4C41).copy(alpha = 0.55f),
        start = Offset(topLeft.x + size.width * 0.12f, seamY),
        end = Offset(topLeft.x + size.width * 0.88f, seamY),
        strokeWidth = 2.4.dp.toPx()
    )
    listOf(0.26f, 0.5f, 0.74f).forEach { fx ->
        drawCircle(
            color = Color(0xFF8D6E63).copy(alpha = 0.75f),
            radius = 1.9.dp.toPx(),
            center = Offset(topLeft.x + size.width * fx, seamY)
        )
    }
}

/** Hielo encima de una capa: bloquea el frasco entero hasta que se descongele. */
private fun DrawScope.drawFrozenOverlay(
    topLeft: Offset,
    size: Size,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val corner = CornerRadius(7.dp.toPx(), 7.dp.toPx())
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

/**
 * Columna de liquido: degradado, superficie ondulada animada, burbujas que
 * suben, halo suave para que el sprite destaque y la hierba generada una sola
 * vez por columna (no un bloque por capa).
 */
private fun DrawScope.drawLiquidColumn(
    topLeft: Offset,
    size: Size,
    herb: Herb,
    phase: Float,
    seed: Int,
    sprite: androidx.compose.ui.graphics.ImageBitmap?,
    isTopGroup: Boolean,
    sticky: Boolean = false,
    frozen: Boolean = false,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val radius = 9.dp.toPx()
    val base = herb.defaultColor
    val light = base.jarLighten(0.30f)
    val dark = base.jarDarken(0.30f)
    val rnd = Random(seed)

    // Cuerpo del liquido
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(light, base, dark),
            startY = topLeft.y,
            endY = topLeft.y + size.height
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(radius, radius)
    )

    // Halo claro detras del sprite para separarlo del color de fondo
    val cx = topLeft.x + size.width / 2f
    val cy = topLeft.y + size.height / 2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(cx, cy),
            radius = size.height * 0.75f
        ),
        radius = size.height * 0.75f,
        center = Offset(cx, cy)
    )

    // Sprites de la hierba: uno por columna, centrado y con buen contraste
    sprite?.let { img ->
        val maxH = size.height * 0.92f
        val targetH = maxOf(maxH, 20.dp.toPx())
        val w = targetH * (img.width.toFloat() / img.height.toFloat())
        val drawW = minOf(w, size.width * 1.15f)
        val drawH = drawW * (img.height.toFloat() / img.width.toFloat())
        drawImage(
            image = img,
            dstOffset = androidx.compose.ui.unit.IntOffset(
                x = (cx - drawW / 2f).toInt(),
                y = (cy - drawH / 2f).toInt()
            ),
            dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt()),
            alpha = 0.95f
        )
    }

    // Burbujas que suben lentamente
    repeat(4) { i ->
        val bx = topLeft.x + size.width * (0.2f + rnd.nextFloat() * 0.6f)
        val span = size.height + 10.dp.toPx()
        val progress = ((phase / (2f * Math.PI.toFloat())) + rnd.nextFloat() + i * 0.27f) % 1f
        val by = topLeft.y + size.height - progress * span
        drawCircle(
            color = Color.White.copy(alpha = 0.30f * (1f - progress)),
            radius = (1.2f + rnd.nextFloat() * 1.8f).dp.toPx(),
            center = Offset(bx, by)
        )
    }

    // Superficie ondulada (menisco) en la parte alta de la columna
    val amp = 2.2.dp.toPx()
    val wavePath = androidx.compose.ui.graphics.Path().apply {
        moveTo(topLeft.x, topLeft.y + amp)
        var x = topLeft.x
        val step = size.width / 16f
        while (x <= topLeft.x + size.width) {
            val rel = (x - topLeft.x) / size.width
            val y = topLeft.y + amp * kotlin.math.sin(
                phase * 1.6f + rel * 6.5f + seed * 0.7f
            )
            lineTo(x, y)
            x += step
        }
        lineTo(topLeft.x + size.width, topLeft.y - amp)
        lineTo(topLeft.x, topLeft.y - amp)
        close()
    }
    drawPath(
        path = wavePath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.10f)),
            startY = topLeft.y - amp,
            endY = topLeft.y + amp * 2.4f
        )
    )

    // Separacion suave entre columnas distintas
    if (!isTopGroup) {
        drawLine(
            color = dark.jarDarken(0.25f).copy(alpha = 0.35f),
            start = Offset(topLeft.x + 2.dp.toPx(), topLeft.y),
            end = Offset(topLeft.x + size.width - 2.dp.toPx(), topLeft.y),
            strokeWidth = 1.2.dp.toPx()
        )
    }

    // Brillo del cristal sobre el liquido
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            startY = topLeft.y,
            endY = topLeft.y + size.height * 0.45f
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(radius, radius)
    )

    // Las marcas del sector van encima del liquido: si no, el hielo y la miel
    // se ven como una hierba cualquiera y el jugador no sabe por que no sale.
    if (sticky) drawStickyMark(topLeft, size)
    if (frozen) drawFrozenOverlay(topLeft, size, textMeasurer)
}

private fun DrawScope.drawHerbLayer(
    topLeft: Offset,
    size: Size,
    layer: JarLayer,
    layerIndex: Int,
    jarId: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    herbSprite: androidx.compose.ui.graphics.ImageBitmap? = null
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

    // Capa pegajosa: una costura con grapas. Es la unica pista de que esa capa
    // hace de tope, asi que tiene que verse sin tener que deducirla.
    if (layer.isSticky) {
        drawStickyMark(topLeft, size)
    }

    if (!layer.isHidden) {
        // Sprite de la hierba generada, centrado sobre su capa.
        herbSprite?.let { sprite ->
            val h = size.height * 1.35f
            val w = h * (sprite.width.toFloat() / sprite.height.toFloat())
            drawImage(
                image = sprite,
                dstOffset = androidx.compose.ui.unit.IntOffset(
                    x = (topLeft.x + (size.width - w) / 2f).toInt(),
                    y = (topLeft.y + (size.height - h) / 2f).toInt()
                ),
                dstSize = androidx.compose.ui.unit.IntSize(w.toInt(), h.toInt()),
                alpha = 0.92f
            )
        }
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
        val symbolStyle = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        val textToDraw = layer.herb.symbol
        val layout = textMeasurer.measure(textToDraw, symbolStyle)
        val cx = topLeft.x + (size.width - layout.size.width) / 2f
        val cy = topLeft.y + (size.height - layout.size.height) / 2f

        drawText(
            textMeasurer = textMeasurer,
            text = textToDraw,
            style = symbolStyle,
            topLeft = Offset(cx, cy)
        )
    }

    if (layer.isHidden) {
        val hiddenStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        val hiddenShadow = TextStyle(color = Color.Black.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        val layout = textMeasurer.measure("?", hiddenStyle)
        val cx = topLeft.x + (size.width - layout.size.width) / 2f
        val cy = topLeft.y + (size.height - layout.size.height) / 2f

        drawText(textMeasurer = textMeasurer, text = "?", style = hiddenShadow, topLeft = Offset(cx + 1.5.dp.toPx(), cy + 1.5.dp.toPx()))
        drawText(textMeasurer = textMeasurer, text = "?", style = hiddenStyle, topLeft = Offset(cx, cy))
    }

    if (layer.isFrozen) {
        drawFrozenOverlay(topLeft, size, textMeasurer)
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

/**
 * Silueta de tetera: el pico y el asa se pintan sobre los bordes del cristal
 * (el Canvas recorta a los límites del frasco) y la tapa va en el hueco del
 * corcho ([drawTeapotLid]). Con [brewed] añade un resplandor cálido.
 */
private fun DrawScope.drawTeapot(brewed: Boolean) {
    val w = size.width
    val h = size.height
    val ceramic = Color(0xFFF7E9D6)
    val ceramicShade = Color(0xFFD9C0A2)
    val trim = Color(0xFFC9A227)
    val stroke = 1.6.dp.toPx()

    if (brewed) {
        drawSoftGlow(
            center = Offset(w / 2f, h * 0.45f),
            radius = w * 1.25f,
            color = Color(0xFFFFE082).copy(alpha = 0.40f)
        )
    }

    // Pico (derecha), dentro del ancho del frasco
    val spout = Path().apply {
        moveTo(w * 0.86f, h * 0.44f)
        cubicTo(w * 1.00f, h * 0.38f, w * 1.02f, h * 0.24f, w * 0.99f, h * 0.15f)
        lineTo(w * 0.90f, h * 0.17f)
        cubicTo(w * 0.92f, h * 0.25f, w * 0.88f, h * 0.32f, w * 0.84f, h * 0.54f)
        close()
    }
    drawPath(spout, color = ceramic)
    drawPath(spout, color = ceramicShade, style = Stroke(width = stroke))

    // Asa (izquierda), dentro de los límites del cristal
    drawArc(
        color = ceramic,
        startAngle = 100f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(0f, h * 0.30f),
        size = Size(w * 0.42f, h * 0.40f),
        style = Stroke(width = 5.5.dp.toPx())
    )
    drawArc(
        color = ceramicShade,
        startAngle = 100f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(0f, h * 0.30f),
        size = Size(w * 0.42f, h * 0.40f),
        style = Stroke(width = 1.2.dp.toPx())
    )

    // Cuello de la tetera
    drawRoundRect(
        color = ceramic,
        topLeft = Offset(-w * 0.04f, 0f),
        size = Size(w * 1.08f, h * 0.06f),
        cornerRadius = CornerRadius(h * 0.025f, h * 0.025f)
    )
    drawRect(
        color = ceramicShade,
        topLeft = Offset(-w * 0.04f, h * 0.055f),
        size = Size(w * 1.08f, 1.5.dp.toPx())
    )

    // Base
    drawRoundRect(
        color = ceramic,
        topLeft = Offset(-w * 0.04f, h * 0.94f),
        size = Size(w * 1.08f, h * 0.06f),
        cornerRadius = CornerRadius(h * 0.025f, h * 0.025f)
    )
    drawRect(
        color = ceramicShade,
        topLeft = Offset(-w * 0.04f, h * 0.94f),
        size = Size(w * 1.08f, 1.5.dp.toPx())
    )

    // Marca de receta servida
    if (brewed) {
        drawCircle(color = Color(0xFFFFF3C4).copy(alpha = 0.9f), radius = 4.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
        drawCircle(color = Color(0xFFC9A227), radius = 2.4.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
    }
}

/** Tapa de la tetera: ocupa el hueco del corcho sobre el frasco. */
private fun DrawScope.drawTeapotLid() {
    val w = size.width
    val h = size.height
    val ceramic = Color(0xFFF7E9D6)
    val ceramicShade = Color(0xFFD9C0A2)

    val lid = Path().apply {
        moveTo(w * 0.04f, h)
        lineTo(w * 0.96f, h)
        lineTo(w * 0.80f, h * 0.34f)
        lineTo(w * 0.20f, h * 0.34f)
        close()
    }
    drawPath(lid, color = ceramic)
    drawPath(lid, color = ceramicShade, style = Stroke(width = 1.4.dp.toPx()))
    drawCircle(color = Color(0xFFC9A227), radius = 2.6.dp.toPx(), center = Offset(w * 0.5f, h * 0.22f))
    drawCircle(color = Color(0xFFFFECB3).copy(alpha = 0.85f), radius = 1.1.dp.toPx(), center = Offset(w * 0.46f, h * 0.16f))
}

@Composable
fun CozyJarItem(
    jar: Jar,
    isSelected: Boolean,
    isPourSource: Boolean = false,
    isPourTarget: Boolean = false,
    isRecipeBrewed: Boolean = false,
    queuePosition: Int = 0,
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
    val resources = rememberResources()
    val herbSpriteFor: (Herb) -> androidx.compose.ui.graphics.ImageBitmap? = remember(resources) {
        { herb -> GeneratedArt.herb(resources, herb) }
    }
    // Fase continua para ondear la superficie del liquido y subir las burbujas.
    val liquidPhase by rememberInfiniteTransition(label = "liquid").animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquidPhase"
    )

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
            Canvas(
                modifier = Modifier
                    .width(if (jar.isTeapot) 60.dp else 38.dp)
                    .height(if (jar.isGrinder) 22.dp else 13.dp)
            ) {
                when {
                    jar.isTeapot -> drawTeapotLid()
                    jar.isGrinder -> drawGrinderLid()
                    else -> drawCork(jar.isSealed)
                }
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

                    // El liquido se agrupa: dos capas contiguas de la misma hierba
                    // forman una sola columna continua con una unica imagen.
                    val groups = jar.layers.groupLiquidRuns()
                    groups.forEach { group ->
                        val fromTop = size.height - ((group.lastIndex + 1) * layerHeight)
                        val toBottom = size.height - (group.firstIndex * layerHeight)
                        val groupTopLeft = Offset(inset, fromTop + 2.dp.toPx())
                        val groupSize = Size(size.width - inset * 2, (toBottom - fromTop) - 4.dp.toPx())

                        if (group.hidden) {
                            drawHerbLayer(
                                topLeft = groupTopLeft,
                                size = Size(groupSize.width, layerHeight - 4.dp.toPx()),
                                layer = jar.layers[group.firstIndex],
                                layerIndex = group.firstIndex,
                                jarId = jar.id,
                                textMeasurer = textMeasurer
                            )
                        } else {
                            drawLiquidColumn(
                                topLeft = groupTopLeft,
                                size = groupSize,
                                herb = group.herb,
                                phase = liquidPhase,
                                seed = jar.id * 31 + group.firstIndex,
                                sprite = herbSpriteFor(group.herb),
                                isTopGroup = group.firstIndex == groups.last().firstIndex,
                                sticky = group.sticky,
                                frozen = group.frozen,
                                textMeasurer = textMeasurer
                            )
                        }
                    }

                    if (jar.isCompleted && !jar.isEmpty && !jar.isTeapot) {
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

                    // El hielo bloquea el frasco entero, no solo la capa: se marca
                    // el cristal completo para que no parezca que solo esa capa
                    // esta fria y las de abajo se pueden sacar.
                    if (jar.hasFrozen) {
                        drawRect(color = Color(0x3381D4FA), size = size)
                        drawRect(
                            color = Color(0xFF4FC3F7).copy(alpha = 0.9f),
                            size = size,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    if (jar.isSealed) {
                        drawWaxSeal()
                    }

                    if (jar.isTeapot) {
                        drawTeapot(brewed = isRecipeBrewed)
                    }
                }
            }
        }

        if (queuePosition > 0) {
            // Movimiento encolado durante la animación: muestra su orden.
            val queueScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "queuePop"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(22.dp)
                    .scale(queueScale)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onTertiary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$queuePosition",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
        }

        if ((jar.isCompleted && !jar.isEmpty) || isRecipeBrewed) {
            Canvas(
                modifier = Modifier
                    .width(if (jar.isTeapot) 52.dp else 38.dp)
                    .height(if (jar.isTeapot) 34.dp else 24.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = if (jar.isTeapot) (-26).dp else (-18).dp)
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

/**
 * Tapa del molinillo: el corcho de siempre mas una manivela encima. Tiene que
 * verse de un vistazo que ese frasco no guarda hierba, la muele, porque su
 * contenido es el resultado de la conversion y no lo que le echaron.
 */
private fun DrawScope.drawGrinderLid() {
    val corkHeight = 13.dp.toPx()
    val corkTop = size.height - corkHeight
    val radius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
    val fill = Brush.verticalGradient(
        listOf(Color(0xFFC9A227).jarLighten(0.35f), Color(0xFFBC8A5F), Color(0xFF6D4C41)),
        startY = corkTop,
        endY = size.height
    )
    drawRoundRect(
        brush = fill,
        topLeft = Offset(0f, corkTop),
        size = Size(size.width, corkHeight),
        cornerRadius = radius
    )
    val crankY = size.height * 0.30f
    drawLine(
        color = Color(0xFF9E9E9E),
        start = Offset(size.width * 0.30f, crankY),
        end = Offset(size.width * 0.72f, crankY),
        strokeWidth = 2.2.dp.toPx()
    )
    drawCircle(
        color = Color(0xFF795548),
        radius = 2.6.dp.toPx(),
        center = Offset(size.width * 0.74f, crankY)
    )
}

private val SoftGlow = Color(0x33FFFFFF)