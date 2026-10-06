package com.cozy.herbshop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Escenarios procedurales de la pantalla de juego.
 * Todo se pinta con Canvas (sin assets externos: cero licencias ni atribuciones).
 */
enum class GameScene(val displayName: String) {
    MORNING("Amanecer"),
    GREENHOUSE("Invernadero"),
    NIGHT("Noche acogedora"),
    FOREST("Bosque"),
    AUTUMN("Jardín de otoño");

    companion object {
        val TOTAL: Int get() = entries.size

        /**
         * Tramos del camino. Cada sector agrupa diez niveles con el mismo ambiente
         * y se cierra con una puerta de hierba, de modo que "pasar de sector" y
         * "conseguir una hierba nueva" son la misma cosa y el mapa cuenta algo.
         *
         * Un ambiente por sector, sin alternar: si dentro de un tramo se van
         * alternando, el jugador ve el nivel 3, el 6 y el 9 con otro fondo y el
         * mapa deja de contar algo. La excepcion es la puerta, que enseña el
         * ambiente del sector siguiente (ver [forLevel]).
         */
        private val SECTORES = listOf(
            Sector(1, 10, "El primer amanecer", MORNING),
            Sector(11, 20, "El invernadero de la abuela", GREENHOUSE),
            Sector(21, 30, "Noches de té", NIGHT),
            Sector(31, 40, "El sendero del bosque", FOREST),
            Sector(41, 50, "La cosecha de otoño", AUTUMN)
        )

        fun sectorOf(level: Int): Sector {
            val n = level.coerceIn(1, 50)
            return SECTORES.firstOrNull { n in it.from..it.to } ?: SECTORES.first()
        }

        fun allSectors(): List<Sector> = SECTORES

        fun forLevel(level: Int): GameScene {
            val sector = sectorOf(level)
            if (level != sector.gateLevel) return sector.scene
            // La puerta de hierba enseña el bioma al que se va a entrar despues.
            // Es el unico nivel que se sale de su sector a proposito, y hace que
            // la puerta se note: al terminarla, el fondo que aparece a
            // continuacion ya se ha visto. El ultimo sector no tiene siguiente,
            // asi que se queda en el suyo.
            return SECTORES.firstOrNull { it.from == sector.to + 1 }?.scene ?: sector.scene
        }

        /**
         * Escenario de una partida concreta. Lo usan tanto el fondo de la partida
         * como la elección de música, para que nunca se desincronicen.
         */
        fun forMode(levelNumber: Int, isDailyChallenge: Boolean, isZenMode: Boolean): GameScene = when {
            isDailyChallenge -> NIGHT
            isZenMode -> FOREST
            else -> forLevel(levelNumber)
        }
    }

    /**
     * Un tramo del camino con su ambiente. La puerta de hierba cae en el ultimo
     * nivel del sector.
     */
    data class Sector(
        val from: Int,
        val to: Int,
        val name: String,
        val scene: GameScene
    ) {
        /** Ultimo nivel del sector: es la puerta de hierba. */
        val gateLevel: Int get() = to
    }
}

@Composable
fun Modifier.gameSceneBackground(scene: GameScene = GameScene.MORNING): Modifier {
    val dark = isSystemInDarkTheme()
    val base = MaterialTheme.colorScheme.background
    val root = this.background(base)
    return when (scene) {
        GameScene.MORNING -> root.morningScene(dark)
        GameScene.GREENHOUSE -> root.greenhouseScene(dark)
        GameScene.NIGHT -> root.nightScene(dark)
        GameScene.FOREST -> root.forestScene(dark)
        GameScene.AUTUMN -> root.autumnScene(dark)
    }
}

// ---------------------------------------------------------------------------
// Utilidades de pintura
// ---------------------------------------------------------------------------

private fun Color.shade(factor: Float): Color = Color(
    red = (red * (1f - factor)).coerceIn(0f, 1f),
    green = (green * (1f - factor)).coerceIn(0f, 1f),
    blue = (blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.lighten(factor: Float): Color = Color(
    red = (red + (1f - red) * factor).coerceIn(0f, 1f),
    green = (green + (1f - green) * factor).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
    alpha = alpha
)

private fun buildMotes(
    rnd: Random,
    count: Int,
    width: Float,
    height: Float,
    minRadius: Float,
    maxRadius: Float
): List<Triple<Float, Float, Float>> = List(count) {
    Triple(
        rnd.nextFloat() * width,
        rnd.nextFloat() * height,
        minRadius + rnd.nextFloat() * (maxRadius - minRadius)
    )
}

private fun DrawScope.drawMotes(motes: List<Triple<Float, Float, Float>>, color: Color) {
    motes.forEach { (x, y, r) -> drawCircle(color = color, radius = r, center = Offset(x, y)) }
}

private fun DrawScope.drawVignette(dark: Boolean, strength: Float = 1f) {
    val w = size.width
    val h = size.height
    val outer = if (dark) Color.Black.copy(alpha = 0.55f * strength)
    else Color(0xFF4E342E).copy(alpha = 0.16f * strength)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, outer),
            center = Offset(w * 0.5f, h * 0.45f),
            radius = maxOf(w, h) * 0.8f
        )
    )
}

private fun DrawScope.drawCenterGlow(dark: Boolean) {
    val w = size.width
    val h = size.height
    val core = if (dark) Color.White.copy(alpha = 0.045f) else Color.White.copy(alpha = 0.10f)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(core, core.copy(alpha = 0f)),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = maxOf(w, h) * 0.6f
        )
    )
}

private fun DrawScope.drawFlowerMotif(center: Offset, radius: Float, color: Color) {
    repeat(5) { i ->
        val angle = Math.toRadians((i * 72).toDouble())
        val px = center.x + cos(angle).toFloat() * radius * 0.55f
        val py = center.y + sin(angle).toFloat() * radius * 0.55f
        drawCircle(color = color, radius = radius * 0.45f, center = Offset(px, py))
    }
    drawCircle(
        color = color.copy(alpha = (color.alpha * 1.6f).coerceAtMost(1f)),
        radius = radius * 0.34f,
        center = center
    )
}

private fun DrawScope.drawWoodFloor(
    top: Float,
    light: Color,
    dark: Color,
    seam: Color,
    seed: Int,
    rows: Int = 7
) {
    val w = size.width
    val h = size.height
    if (h - top < 8f) return
    val plankH = (h - top) / rows
    for (i in 0 until rows) {
        val y = top + i * plankH
        drawRect(
            color = if (i % 2 == 0) light else dark,
            topLeft = Offset(0f, y),
            size = Size(w, plankH)
        )
        val rnd = Random(seed + i * 37)
        repeat(5) {
            val gy = y + plankH * rnd.nextFloat()
            val gx = w * rnd.nextFloat()
            val gl = w * (0.05f + rnd.nextFloat() * 0.12f)
            drawLine(
                color = seam.copy(alpha = 0.20f),
                start = Offset(gx, gy),
                end = Offset(gx + gl, gy + (rnd.nextFloat() - 0.5f) * 5f),
                strokeWidth = 1.4f
            )
        }
        drawLine(color = seam.copy(alpha = 0.45f), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 2f)
        val seamX = if (i % 2 == 0) w * 0.33f else w * 0.67f
        drawLine(
            color = seam.copy(alpha = 0.40f),
            start = Offset(seamX, y),
            end = Offset(seamX, y + plankH),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawTileFloor(top: Float, a: Color, b: Color, grout: Color, cols: Int = 6, rows: Int = 3) {
    val w = size.width
    val h = size.height
    if (h - top < 8f) return
    val tw = w / cols
    val th = (h - top) / rows
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val x = c * tw
            val y = top + r * th
            drawRect(color = if ((r + c) % 2 == 0) a else b, topLeft = Offset(x, y), size = Size(tw, th))
            drawRect(
                color = grout.copy(alpha = 0.35f),
                topLeft = Offset(x, y),
                size = Size(tw, th),
                style = Stroke(width = 1.6f)
            )
        }
    }
}

private fun DrawScope.drawBaseboard(top: Float, body: Color, highlight: Color) {
    val w = size.width
    val thickness = 16f
    drawRect(color = body, topLeft = Offset(0f, top - thickness), size = Size(w, thickness))
    drawLine(color = highlight, start = Offset(0f, top - thickness), end = Offset(w, top - thickness), strokeWidth = 2.5f)
    drawLine(color = highlight.copy(alpha = 0.5f), start = Offset(0f, top), end = Offset(w, top), strokeWidth = 1.5f)
}

private fun DrawScope.drawWoodBeam(top: Float, height: Float, light: Color, dark: Color, grain: Color) {
    val w = size.width
    drawRect(
        brush = Brush.verticalGradient(listOf(light, light.lighten(0.12f), dark)),
        topLeft = Offset(0f, top),
        size = Size(w, height)
    )
    val rnd = Random(778)
    repeat(10) {
        val y = top + height * (0.15f + rnd.nextFloat() * 0.7f)
        val x0 = w * rnd.nextFloat()
        val x1 = x0 + w * (0.06f + rnd.nextFloat() * 0.16f)
        drawLine(
            color = grain.copy(alpha = 0.35f),
            start = Offset(x0, y),
            end = Offset(x1, y + (rnd.nextFloat() - 0.5f) * 4f),
            strokeWidth = 1.6f
        )
    }
    drawLine(color = dark.shade(0.25f).copy(alpha = 0.6f), start = Offset(0f, top + height), end = Offset(w, top + height), strokeWidth = 4f)
    drawLine(color = Color.White.copy(alpha = 0.18f), start = Offset(0f, top + 2f), end = Offset(w, top + 2f), strokeWidth = 2f)
}

private fun DrawScope.drawHangingHerbs(
    anchorX: Float,
    anchorY: Float,
    length: Float,
    leafColor: Color,
    stemColor: Color,
    twine: Color
) {
    drawLine(color = twine, start = Offset(anchorX, anchorY), end = Offset(anchorX, anchorY + length * 0.20f), strokeWidth = 3f)
    val stemTop = anchorY + length * 0.20f
    val stemBottom = anchorY + length
    drawLine(color = stemColor, start = Offset(anchorX, stemTop), end = Offset(anchorX, stemBottom), strokeWidth = 3.5f)
    val count = 7
    for (i in 0 until count) {
        val t = i / (count - 1f)
        val y = stemTop + (stemBottom - stemTop) * t
        val side = if (i % 2 == 0) -1 else 1
        drawPaintedLeaf(
            center = Offset(anchorX + side * length * 0.13f, y),
            length = length * 0.32f,
            width = length * 0.10f,
            color = leafColor,
            rotationDeg = side * -38f + if (i % 3 == 0) 14f else -4f
        )
    }
    drawLine(
        color = twine,
        start = Offset(anchorX - length * 0.10f, stemTop),
        end = Offset(anchorX + length * 0.10f, stemTop),
        strokeWidth = 6f
    )
    drawPaintedLeaf(
        center = Offset(anchorX, stemBottom + length * 0.09f),
        length = length * 0.30f,
        width = length * 0.10f,
        color = leafColor.shade(0.14f),
        rotationDeg = 90f
    )
}

private fun DrawScope.drawStoneLantern(cx: Float, baseY: Float, width: Float, glowColor: Color, stone: Color) {
    val boxW = width * 0.62f
    val boxH = width * 0.48f
    val postH = width * 0.34f
    val baseH = width * 0.12f
    val boxBottom = baseY - baseH - postH
    val boxTop = boxBottom - boxH

    drawRoundRect(
        color = stone.shade(0.15f),
        topLeft = Offset(cx - width * 0.45f, baseY - baseH),
        size = Size(width * 0.9f, baseH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRect(
        color = stone,
        topLeft = Offset(cx - width * 0.13f, baseY - baseH - postH),
        size = Size(width * 0.26f, postH)
    )
    drawSoftGlow(
        center = Offset(cx, boxTop + boxH / 2f),
        radius = width * 1.4f,
        color = Color(0xFFFFC44D).copy(alpha = 0.40f)
    )
    drawRoundRect(
        color = glowColor,
        topLeft = Offset(cx - boxW / 2f, boxTop),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRect(
        color = stone.shade(0.5f).copy(alpha = 0.8f),
        topLeft = Offset(cx - boxW * 0.20f, boxTop + boxH * 0.24f),
        size = Size(boxW * 0.40f, boxH * 0.52f)
    )
    drawRoundRect(
        color = stone.shade(0.35f),
        topLeft = Offset(cx - boxW / 2f, boxTop),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 4f)
    )
    val roof = Path().apply {
        moveTo(cx - boxW * 0.85f, boxTop)
        lineTo(cx + boxW * 0.85f, boxTop)
        lineTo(cx + boxW * 0.32f, boxTop - boxH * 0.55f)
        lineTo(cx - boxW * 0.32f, boxTop - boxH * 0.55f)
        close()
    }
    drawPath(roof, color = stone.shade(0.12f))
    drawPath(roof, color = stone.lighten(0.3f).copy(alpha = 0.5f), style = Stroke(width = 2f))
    drawCircle(
        color = stone.shade(0.12f),
        radius = width * 0.07f,
        center = Offset(cx, boxTop - boxH * 0.55f - width * 0.04f)
    )
}

private fun DrawScope.drawJarShelf(
    left: Float,
    right: Float,
    y: Float,
    shelfH: Float,
    wood: Color,
    jars: List<Triple<Float, Float, Color>>
) {
    drawRect(
        brush = Brush.verticalGradient(listOf(wood.lighten(0.15f), wood)),
        topLeft = Offset(left, y),
        size = Size(right - left, shelfH)
    )
    drawRect(
        color = wood.shade(0.3f),
        topLeft = Offset(left, y + shelfH - 3f),
        size = Size(right - left, 3f)
    )
    drawRect(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(left + 6f, y + shelfH),
        size = Size(right - left - 12f, shelfH * 0.6f)
    )
    jars.forEach { (jx, jh, content) ->
        val jw = jh * 0.6f
        val top = y - jh
        drawRoundRect(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset(jx - jw / 2f, top),
            size = Size(jw, jh),
            cornerRadius = CornerRadius(jw * 0.2f, jw * 0.2f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(content.lighten(0.2f), content),
                startY = top + jh * 0.3f,
                endY = y
            ),
            topLeft = Offset(jx - jw / 2f + 3f, top + jh * 0.35f),
            size = Size(jw - 6f, jh * 0.65f - 3f),
            cornerRadius = CornerRadius(jw * 0.14f, jw * 0.14f)
        )
        drawRoundRect(
            color = Color(0xFF6D4C41),
            topLeft = Offset(jx - jw * 0.28f, top - jh * 0.13f),
            size = Size(jw * 0.56f, jh * 0.16f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(jx - jw / 2f + 4f, top + 5f),
            end = Offset(jx - jw / 2f + 4f, y - 4f),
            strokeWidth = 2.5f
        )
    }
}

internal fun DrawScope.drawStringLights(
    yBase: Float,
    sag: Float,
    count: Int,
    wire: Color,
    glow: Color,
    bulb: Color
) {
    val w = size.width
    fun wireY(t: Float) = yBase + sag * 4f * t * (1f - t)

    val path = Path()
    val steps = 48
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val x = w * t
        val y = wireY(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color = wire, style = Stroke(width = 2.5f))

    val r = 7f
    for (i in 0 until count) {
        val t = (i + 0.5f) / count
        val x = w * t
        val y = wireY(t) + 9f
        drawSoftGlow(center = Offset(x, y), radius = r * 3.6f, color = glow)
        drawRect(color = wire, topLeft = Offset(x - 3f, y - r - 5f), size = Size(6f, 7f))
        drawCircle(color = bulb, radius = r, center = Offset(x, y))
        drawCircle(color = Color.White.copy(alpha = 0.75f), radius = r * 0.42f, center = Offset(x - r * 0.3f, y - r * 0.3f))
    }
}

internal fun DrawScope.drawPottedPlant(baseX: Float, baseY: Float, sizePx: Float, pot: Color, leaves: List<Color>) {
    val potW = sizePx * 0.72f
    val potH = sizePx * 0.5f
    val rimH = potH * 0.24f

    drawOval(
        color = Color.Black.copy(alpha = 0.20f),
        topLeft = Offset(baseX - potW * 0.62f, baseY - potH * 0.12f),
        size = Size(potW * 1.24f, potH * 0.34f)
    )

    val body = Path().apply {
        moveTo(baseX - potW * 0.5f, baseY - potH)
        lineTo(baseX + potW * 0.5f, baseY - potH)
        lineTo(baseX + potW * 0.36f, baseY)
        lineTo(baseX - potW * 0.36f, baseY)
        close()
    }
    drawPath(body, color = pot)
    drawPath(body, color = pot.lighten(0.3f).copy(alpha = 0.35f), style = Stroke(width = 2f))

    drawRoundRect(
        color = pot.lighten(0.18f),
        topLeft = Offset(baseX - potW * 0.56f, baseY - potH - rimH),
        size = Size(potW * 1.12f, rimH),
        cornerRadius = CornerRadius(rimH * 0.45f, rimH * 0.45f)
    )
    drawOval(
        color = Color(0xFF3E2723).copy(alpha = 0.85f),
        topLeft = Offset(baseX - potW * 0.44f, baseY - potH - rimH * 0.5f),
        size = Size(potW * 0.88f, rimH * 0.9f)
    )

    val leafBaseY = baseY - potH - rimH
    val count = leaves.size
    for (i in 0 until count) {
        val t = if (count == 1) 0.5f else i / (count - 1f)
        val rot = -74f + t * 148f
        val len = sizePx * (0.62f - abs(t - 0.5f) * 0.34f)
        val rad = Math.toRadians(rot.toDouble())
        val center = Offset(
            baseX + cos(rad).toFloat() * len * 0.45f,
            leafBaseY + sin(rad).toFloat() * len * 0.45f
        )
        drawPaintedLeaf(
            center = center,
            length = len,
            width = len * 0.30f,
            color = leaves[i % leaves.size],
            rotationDeg = rot
        )
    }
}

private fun DrawScope.drawPine(baseX: Float, baseY: Float, height: Float, color: Color) {
    val w = height * 0.62f
    repeat(3) { i ->
        val bottomY = baseY - height * (i * 0.20f)
        val topY = baseY - height * (0.48f + i * 0.20f)
        val hw = (w * (1f - i * 0.18f)) / 2f
        val tri = Path().apply {
            moveTo(baseX, topY)
            lineTo(baseX + hw, bottomY)
            lineTo(baseX - hw, bottomY)
            close()
        }
        drawPath(tri, color = color)
    }
    drawRect(
        color = color,
        topLeft = Offset(baseX - height * 0.035f, baseY - height * 0.16f),
        size = Size(height * 0.07f, height * 0.16f)
    )
}

private fun DrawScope.drawTreeline(baseline: Float, height: Float, color: Color, seed: Int, count: Int) {
    val w = size.width
    val rnd = Random(seed)
    val step = w / count
    for (i in 0 until count) {
        val x = step * (i + 0.5f) + (rnd.nextFloat() - 0.5f) * step * 0.35f
        drawPine(
            baseX = x,
            baseY = baseline + rnd.nextFloat() * height * 0.05f,
            height = height * (0.7f + rnd.nextFloat() * 0.6f),
            color = color
        )
    }
}

private fun DrawScope.drawGrassTuft(x: Float, baseY: Float, size: Float, color: Color) {
    repeat(5) { i ->
        val rot = -90f + (i - 2) * 22f
        drawPaintedLeaf(
            center = Offset(x + (i - 2) * size * 0.26f, baseY - size * 0.34f),
            length = size * 0.95f,
            width = size * 0.20f,
            color = color,
            rotationDeg = rot
        )
    }
}

private fun DrawScope.drawWildflower(x: Float, y: Float, size: Float, petal: Color, core: Color) {
    repeat(5) { i ->
        val a = Math.toRadians((i * 72).toDouble())
        drawCircle(
            color = petal,
            radius = size * 0.34f,
            center = Offset(x + (cos(a) * size * 0.42f).toFloat(), y + (sin(a) * size * 0.42f).toFloat())
        )
    }
    drawCircle(color = core, radius = size * 0.30f, center = Offset(x, y))
}

private fun DrawScope.drawMushroom(x: Float, baseY: Float, size: Float, capColor: Color, stemColor: Color) {
    drawRoundRect(
        color = stemColor,
        topLeft = Offset(x - size * 0.16f, baseY - size * 0.52f),
        size = Size(size * 0.32f, size * 0.52f),
        cornerRadius = CornerRadius(size * 0.12f, size * 0.12f)
    )
    val cap = Path().apply {
        moveTo(x - size * 0.52f, baseY - size * 0.42f)
        quadraticBezierTo(x - size * 0.46f, baseY - size * 1.12f, x, baseY - size * 1.12f)
        quadraticBezierTo(x + size * 0.46f, baseY - size * 1.12f, x + size * 0.52f, baseY - size * 0.42f)
        close()
    }
    drawPath(cap, color = capColor)
    repeat(3) { i ->
        drawCircle(
            color = stemColor.copy(alpha = 0.85f),
            radius = size * 0.07f,
            center = Offset(x + (i - 1) * size * 0.26f, baseY - size * (0.76f + (i % 2) * 0.14f))
        )
    }
}

private fun DrawScope.drawHillBand(
    left: Float,
    right: Float,
    yBase: Float,
    amp: Float,
    color: Color,
    seed: Int,
    extendBelow: Float
) {
    val span = right - left
    val rnd = Random(seed)
    val path = Path().apply {
        moveTo(left, yBase)
        val seg = 6
        for (i in 1..seg) {
            val x0 = left + span * (i - 1) / seg
            val x1 = left + span * i / seg
            val cy = yBase - amp * (0.35f + rnd.nextFloat() * 0.65f)
            quadraticBezierTo((x0 + x1) / 2f, cy, x1, yBase - amp * 0.12f * rnd.nextFloat())
        }
        lineTo(right, yBase + extendBelow)
        lineTo(left, yBase + extendBelow)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawCloud(center: Offset, sizePx: Float, color: Color) {
    drawOval(
        color = color,
        topLeft = Offset(center.x - sizePx * 0.55f, center.y - sizePx * 0.12f),
        size = Size(sizePx * 1.1f, sizePx * 0.42f)
    )
    drawCircle(color = color, radius = sizePx * 0.30f, center = Offset(center.x - sizePx * 0.28f, center.y - sizePx * 0.10f))
    drawCircle(color = color, radius = sizePx * 0.38f, center = Offset(center.x + sizePx * 0.06f, center.y - sizePx * 0.20f))
    drawCircle(color = color, radius = sizePx * 0.26f, center = Offset(center.x + sizePx * 0.44f, center.y - sizePx * 0.02f))
}

private fun DrawScope.drawBamboo(
    x: Float,
    topY: Float,
    bottomY: Float,
    width: Float,
    stalk: Color,
    node: Color,
    leaf: Color
) {
    val hgt = bottomY - topY
    if (hgt <= 0f) return
    drawRect(color = stalk, topLeft = Offset(x - width / 2f, topY), size = Size(width, hgt))
    drawRect(
        color = stalk.lighten(0.28f).copy(alpha = 0.45f),
        topLeft = Offset(x - width / 2f, topY),
        size = Size(width * 0.32f, hgt)
    )
    val segs = 7
    for (i in 1 until segs) {
        val y = topY + hgt * i / segs
        drawLine(color = node, start = Offset(x - width / 2f, y), end = Offset(x + width / 2f, y), strokeWidth = 3f)
    }
    repeat(3) { i ->
        val ly = topY + hgt * (0.10f + i * 0.24f)
        val side = if (i % 2 == 0) 1 else -1
        drawPaintedLeaf(
            center = Offset(x + side * width * 1.9f, ly),
            length = width * 4.4f,
            width = width * 1.15f,
            color = leaf,
            rotationDeg = side * 26f
        )
        drawPaintedLeaf(
            center = Offset(x + side * width * 1.5f, ly + width * 2.4f),
            length = width * 3.6f,
            width = width * 0.95f,
            color = leaf.shade(0.1f),
            rotationDeg = side * 58f
        )
    }
}

private fun DrawScope.drawLightShaft(path: Path, topColor: Color, bottomColor: Color, start: Offset, end: Offset) {
    drawPath(
        path = path,
        brush = Brush.linearGradient(colors = listOf(topColor, bottomColor), start = start, end = end)
    )
}

private fun DrawScope.drawCandle(x: Float, baseY: Float, height: Float, body: Color, flameGlow: Color) {
    val w = height * 0.34f
    drawRect(
        color = Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(x - w * 0.9f, baseY - 4f),
        size = Size(w * 1.8f, height * 0.14f)
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(body.lighten(0.2f), body, body.shade(0.15f))),
        topLeft = Offset(x - w / 2f, baseY - height),
        size = Size(w, height),
        cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
    )
    drawLine(color = Color(0xFF3E2723), start = Offset(x, baseY - height), end = Offset(x, baseY - height - 5f), strokeWidth = 2f)
    val flameH = height * 0.45f
    val tip = baseY - height - flameH
    val fw = w * 0.85f
    val flame = Path().apply {
        moveTo(x, tip)
        cubicTo(x + fw, tip + flameH * 0.5f, x + fw * 0.8f, baseY - height - 2f, x, baseY - height - 2f)
        cubicTo(x - fw * 0.8f, baseY - height - 2f, x - fw, tip + flameH * 0.5f, x, tip)
        close()
    }
    drawSoftGlow(center = Offset(x, baseY - height - flameH * 0.45f), radius = height * 1.1f, color = flameGlow)
    drawPath(flame, color = Color(0xFFFFD54F))
    drawPath(
        flame,
        color = Color(0xFFFFF8E1),
        style = Stroke(width = 2f)
    )
    drawCircle(
        color = Color(0xFFFFF8E1),
        radius = fw * 0.34f,
        center = Offset(x, baseY - height - flameH * 0.22f)
    )
}

private fun DrawScope.drawArchedWindow(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    frame: Color,
    skyBrush: Brush,
    content: DrawScope.() -> Unit
) {
    val w = right - left
    val h = bottom - top
    val r = w / 2f
    val path = Path().apply {
        moveTo(left, bottom)
        lineTo(left, top + r)
        quadraticBezierTo(left, top, left + r, top)
        quadraticBezierTo(right, top, right, top + r)
        lineTo(right, bottom)
        close()
    }
    clipPath(path) {
        drawRect(brush = skyBrush, topLeft = Offset(left, top), size = Size(w, h))
        content()
    }
    drawPath(path, color = frame, style = Stroke(width = 7f))
    drawLine(color = frame, start = Offset(left + w / 2f, top + r * 0.15f), end = Offset(left + w / 2f, bottom), strokeWidth = 5f)
    drawLine(color = frame, start = Offset(left, top + h * 0.58f), end = Offset(right, top + h * 0.58f), strokeWidth = 5f)
    drawRoundRect(
        color = frame.shade(0.18f),
        topLeft = Offset(left - 10f, bottom),
        size = Size(w + 20f, 12f),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawStone(baseX: Float, baseY: Float, w: Float, h: Float, color: Color) {
    val path = Path().apply {
        moveTo(baseX - w / 2f, baseY)
        quadraticBezierTo(baseX - w * 0.55f, baseY - h, baseX - w * 0.15f, baseY - h)
        quadraticBezierTo(baseX + w * 0.45f, baseY - h * 1.05f, baseX + w * 0.5f, baseY - h * 0.35f)
        quadraticBezierTo(baseX + w * 0.52f, baseY, baseX - w / 2f, baseY)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = color.lighten(0.3f).copy(alpha = 0.5f), style = Stroke(width = 2f))
    drawOval(
        color = color.lighten(0.35f).copy(alpha = 0.35f),
        topLeft = Offset(baseX - w * 0.30f, baseY - h * 0.85f),
        size = Size(w * 0.35f, h * 0.30f)
    )
}

// ---------------------------------------------------------------------------
// Escenario 1 - Amanecer en la herboristería
// ---------------------------------------------------------------------------

private fun Modifier.morningScene(dark: Boolean): Modifier {
    val wallTop = if (dark) Color(0xFF262B27) else Color(0xFFFCF7EA)
    val wallBottom = if (dark) Color(0xFF1A1F1C) else Color(0xFFF1E5CD)
    val floorLight = if (dark) Color(0xFF3C3028) else Color(0xFFC1895C)
    val floorDark = if (dark) Color(0xFF2F251F) else Color(0xFFA9744C)
    val seamColor = if (dark) Color(0xFF141210) else Color(0xFF7B5136)
    val motifColor = if (dark) Color.White.copy(alpha = 0.035f) else Color(0xFFC7B593).copy(alpha = 0.45f)
    val beamLight = if (dark) Color(0xFF7A5A45) else Color(0xFFE3C7A1)
    val beamDark = if (dark) Color(0xFF4A342A) else Color(0xFFCBA984)
    val beamGrain = if (dark) Color(0xFF2C1F19) else Color(0xFF9C7A5A)
    val twine = if (dark) Color(0xFF9C8A76) else Color(0xFF8D6E63)
    val stemColor = if (dark) Color(0xFF6B5B4A) else Color(0xFF7A5C3E)
    val baseBody = if (dark) Color(0xFF101210) else Color(0xFFDCCBAA)
    val baseHi = if (dark) Color(0xFF3A403A) else Color(0xFFFFF6E3)
    val shelfWood = if (dark) Color(0xFF5A4238) else Color(0xFF9C7A5A)
    val frameWood = if (dark) Color(0xFF5A4238) else Color(0xFF8D6E63)
    val greens = listOf(
        Color(0xFF7CB342), Color(0xFF8BC34A), Color(0xFF9CCC65),
        Color(0xFF66BB6A), Color(0xFFA5C77A)
    )
    val potColors = listOf(Color(0xFFD98A6C), Color(0xFFC97B5A))

    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val floorTop = h * 0.80f
        val beamH = h * 0.05f
        val rnd = Random(1101)

        val motifs = buildList {
            var row = 0
            var y = h * 0.11f
            while (y < floorTop - 24f) {
                var x = if (row % 2 == 0) 32f else 80f
                while (x < w - 12f) {
                    add(Offset(x, y))
                    x += 96f
                }
                y += 84f
                row++
            }
        }
        val motes = buildMotes(rnd, 44, w, floorTop, 1f, 2.8f)

        val bundles = listOf(
            Triple(w * 0.22f, h * 0.165f, 0),
            Triple(w * 0.33f, h * 0.195f, 1),
            Triple(w * 0.67f, h * 0.185f, 2),
            Triple(w * 0.78f, h * 0.145f, 3)
        )

        val shelfY = h * 0.44f
        val shelfLeft = w * 0.03f
        val shelfRight = w * 0.30f
        val shelfJars = listOf(
            Triple(shelfLeft + (shelfRight - shelfLeft) * 0.16f, h * 0.052f, Color(0xFFF9C846)),
            Triple(shelfLeft + (shelfRight - shelfLeft) * 0.48f, h * 0.064f, Color(0xFF7E57C2)),
            Triple(shelfLeft + (shelfRight - shelfLeft) * 0.80f, h * 0.048f, Color(0xFF66BB6A))
        )

        val pictureCx = w * 0.84f
        val pictureCy = h * 0.44f
        val pictureW = w * 0.24f
        val pictureH = h * 0.14f

        val lowShelfY = h * 0.72f
        val lowShelfLeft = w * 0.60f
        val lowShelfRight = w * 0.97f
        val lowJars = listOf(
            Triple(lowShelfLeft + (lowShelfRight - lowShelfLeft) * 0.24f, h * 0.058f, Color(0xFFEF5350)),
            Triple(lowShelfLeft + (lowShelfRight - lowShelfLeft) * 0.66f, h * 0.050f, Color(0xFF42A5F5))
        )

        val beamPath = Path().apply {
            moveTo(w * 0.74f, 0f)
            lineTo(w * 1.02f, 0f)
            lineTo(w * 0.58f, h)
            lineTo(w * 0.06f, h)
            close()
        }

        val vignetteOuter = if (dark) Color.Black.copy(alpha = 0.5f) else Color(0xFF4E342E).copy(alpha = 0.15f)

        onDrawBehind {
            drawRect(brush = Brush.verticalGradient(listOf(wallTop, wallBottom), startY = 0f, endY = floorTop))
            motifs.forEach { c -> drawFlowerMotif(c, 9f, motifColor) }

            drawLightShaft(
                path = beamPath,
                topColor = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.20f),
                bottomColor = Color.White.copy(alpha = 0f),
                start = Offset(w * 0.95f, 0f),
                end = Offset(w * 0.25f, h)
            )
            drawMotes(motes, if (dark) Color.White.copy(alpha = 0.16f) else Color(0xFFFFF3C4).copy(alpha = 0.55f))

            drawJarShelf(shelfLeft, shelfRight, shelfY, h * 0.02f, shelfWood, shelfJars)

            drawRoundRect(
                color = Color(0xFFFFFDF5).copy(alpha = if (dark) 0.10f else 0.95f),
                topLeft = Offset(pictureCx - pictureW / 2f, pictureCy - pictureH / 2f),
                size = Size(pictureW, pictureH),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = frameWood,
                topLeft = Offset(pictureCx - pictureW / 2f, pictureCy - pictureH / 2f),
                size = Size(pictureW, pictureH),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 6f)
            )
            drawPaintedLeaf(
                center = Offset(pictureCx, pictureCy),
                length = pictureW * 0.6f,
                width = pictureW * 0.2f,
                color = if (dark) Color(0xFF8BC34A) else Color(0xFF558B2F),
                rotationDeg = -28f
            )
            drawLine(
                color = if (dark) Color(0xFF8BC34A) else Color(0xFF558B2F),
                start = Offset(pictureCx - pictureW * 0.28f, pictureCy + pictureH * 0.22f),
                end = Offset(pictureCx + pictureW * 0.28f, pictureCy + pictureH * 0.22f),
                strokeWidth = 3f
            )

            drawJarShelf(lowShelfLeft, lowShelfRight, lowShelfY, h * 0.018f, shelfWood, lowJars)

            drawWoodBeam(0f, beamH, beamLight, beamDark, beamGrain)
            bundles.forEach { (bx, length, idx) ->
                drawHangingHerbs(
                    anchorX = bx,
                    anchorY = beamH - 2f,
                    length = length,
                    leafColor = greens[idx % greens.size],
                    stemColor = stemColor,
                    twine = twine
                )
            }

            drawWoodFloor(floorTop, floorLight, floorDark, seamColor, seed = 4321, rows = 6)
            drawBaseboard(floorTop, baseBody, baseHi)

            val plantSize = h * 0.14f
            drawPottedPlant(
                baseX = w * 0.10f,
                baseY = h * 0.985f,
                sizePx = plantSize,
                pot = potColors[0],
                leaves = listOf(greens[0], greens[2], greens[4], greens[1], greens[3])
            )
            drawPottedPlant(
                baseX = w * 0.90f,
                baseY = h * 0.99f,
                sizePx = plantSize * 0.9f,
                pot = potColors[1],
                leaves = listOf(greens[3], greens[1], greens[0], greens[2], greens[4])
            )

            drawCenterGlow(dark)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, vignetteOuter),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = maxOf(w, h) * 0.8f
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Escenario 2 - Invernadero
// ---------------------------------------------------------------------------

private fun Modifier.greenhouseScene(dark: Boolean): Modifier {
    val skyTop = if (dark) Color(0xFF16211C) else Color(0xFFEAF6EC)
    val skyBottom = if (dark) Color(0xFF0E1613) else Color(0xFFD7ECD6)
    val foliageFar = if (dark) Color(0xFF1E3326) else Color(0xFFB7D9A8)
    val foliageMid = if (dark) Color(0xFF16281D) else Color(0xFF8FC47E)
    val foliageNear = if (dark) Color(0xFF0F1D15) else Color(0xFF6BA85C)
    val frameColor = if (dark) Color(0xFF5E6B60).copy(alpha = 0.75f) else Color(0xFFFFFDF7).copy(alpha = 0.9f)
    val tileA = if (dark) Color(0xFF2A2F2A) else Color(0xFFEDE6D6)
    val tileB = if (dark) Color(0xFF22271F) else Color(0xFFDCD2BC)
    val grout = if (dark) Color(0xFF12150F) else Color(0xFFB9AC91)
    val shelfWood = if (dark) Color(0xFF5A4238) else Color(0xFFA17B57)
    val potColor = if (dark) Color(0xFF8A5A44) else Color(0xFFD08A63)
    val vineStem = if (dark) Color(0xFF3E5A3C) else Color(0xFF4E8A3E)
    val vineLeaves = listOf(Color(0xFF7CB342), Color(0xFF9CCC65), Color(0xFF66BB6A), Color(0xFFAED581))
    val greens = listOf(Color(0xFF8BC34A), Color(0xFF7CB342), Color(0xFF9CCC65), Color(0xFF66BB6A))
    val crateColor = if (dark) Color(0xFF4A3527) else Color(0xFFB98A5E)

    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val glassBottom = h * 0.74f
        val floorTop = h * 0.76f
        val rnd = Random(2029)

        val bushClusters = buildList {
            repeat(26) {
                val cx = rnd.nextFloat() * w
                val cy = h * 0.14f + rnd.nextFloat() * (glassBottom - h * 0.16f)
                val r = w * (0.05f + rnd.nextFloat() * 0.08f)
                add(Triple(cx, cy, r))
            }
        }
        val motes = buildMotes(rnd, 40, w, glassBottom, 1f, 2.6f)

        val vinePoints = buildList {
            var x = w * 0.03f
            while (x < w * 0.99f) {
                add(Offset(x, h * 0.045f + sin(x / w * 12.0).toFloat() * h * 0.022f))
                x += w * 0.055f
            }
        }

        val shelfY = h * 0.60f
        val shelfLeft = w * 0.06f
        val shelfRight = w * 0.94f
        val pots = listOf(0.14f, 0.34f, 0.62f, 0.86f)
        val shelfH = h * 0.022f
        // El hueco entre la estanteria y el suelo estaba vacio y el escenario
        // parecia cortado a media pantalla. Se cierra con el faldon de madera de
        // la propia estanteria, asi que se lee como un mueble y no como un hueco.
        val apronTop = shelfY + shelfH

        val crateRect = Rect(w * 0.06f, floorTop + h * 0.09f, w * 0.27f, floorTop + h * 0.21f)

        val shaftA = Path().apply {
            moveTo(w * 0.05f, 0f)
            lineTo(w * 0.30f, 0f)
            lineTo(w * 0.62f, h)
            lineTo(w * 0.28f, h)
            close()
        }
        val shaftB = Path().apply {
            moveTo(w * 0.42f, 0f)
            lineTo(w * 0.58f, 0f)
            lineTo(w * 0.90f, h)
            lineTo(w * 0.68f, h)
            close()
        }

        onDrawBehind {
            drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyBottom), startY = 0f, endY = glassBottom))
            drawCircle(
                color = if (dark) Color(0xFFCFE8D6).copy(alpha = 0.25f) else Color(0xFFFFF59D).copy(alpha = 0.75f),
                radius = w * 0.16f,
                center = Offset(w * 0.78f, h * 0.14f)
            )
            drawHillBand(0f, w, h * 0.36f, h * 0.10f, foliageFar, seed = 21, extendBelow = h)
            drawHillBand(0f, w, h * 0.50f, h * 0.12f, foliageMid, seed = 42, extendBelow = h)
            bushClusters.forEach { (cx, cy, r) ->
                drawCircle(color = foliageNear.copy(alpha = 0.75f), radius = r, center = Offset(cx, cy))
                drawCircle(color = foliageMid.copy(alpha = 0.5f), radius = r * 0.65f, center = Offset(cx - r * 0.3f, cy - r * 0.3f))
            }

            val mullion = h * 0.006f + 4f
            var vx = 0f
            while (vx <= w + 1f) {
                drawRect(color = frameColor, topLeft = Offset(vx - mullion / 2f, 0f), size = Size(mullion, glassBottom))
                vx += w / 3f
            }
            listOf(0f, h * 0.36f, glassBottom).forEach { hy ->
                drawRect(color = frameColor, topLeft = Offset(0f, hy - mullion / 2f), size = Size(w, mullion))
            }

            drawLightShaft(
                shaftA,
                Color.White.copy(alpha = if (dark) 0.05f else 0.22f),
                Color.White.copy(alpha = 0f),
                Offset(w * 0.2f, 0f),
                Offset(w * 0.5f, h)
            )
            drawLightShaft(
                shaftB,
                Color.White.copy(alpha = if (dark) 0.04f else 0.16f),
                Color.White.copy(alpha = 0f),
                Offset(w * 0.5f, 0f),
                Offset(w * 0.8f, h)
            )
            drawMotes(motes, if (dark) Color.White.copy(alpha = 0.14f) else Color(0xFFFFF8C8).copy(alpha = 0.6f))

            val shelfH = h * 0.022f
            drawRect(
                brush = Brush.verticalGradient(listOf(shelfWood.lighten(0.15f), shelfWood)),
                topLeft = Offset(shelfLeft, shelfY),
                size = Size(shelfRight - shelfLeft, shelfH)
            )
            drawRect(
                color = shelfWood.shade(0.3f),
                topLeft = Offset(shelfLeft, shelfY + shelfH - 3f),
                size = Size(shelfRight - shelfLeft, 3f)
            )
            pots.forEach { p ->
                val px = shelfLeft + (shelfRight - shelfLeft) * p
                drawPottedPlant(
                    baseX = px,
                    baseY = shelfY - 1f,
                    sizePx = h * 0.09f,
                    pot = potColor,
                    leaves = listOf(greens[0], greens[2], greens[1], greens[3], greens[0])
                )
            }

            val vinePath = Path()
            vinePoints.forEachIndexed { i, p ->
                if (i == 0) vinePath.moveTo(p.x, p.y) else vinePath.lineTo(p.x, p.y)
            }
            drawPath(vinePath, color = vineStem, style = Stroke(width = 3f))
            vinePoints.forEachIndexed { i, p ->
                val side = if (i % 2 == 0) -1 else 1
                val len = w * 0.075f
                drawPaintedLeaf(
                    center = Offset(p.x, p.y + len * 0.35f),
                    length = len,
                    width = len * 0.32f,
                    color = vineLeaves[i % vineLeaves.size],
                    rotationDeg = 90f + side * 26f
                )
                if (i % 3 == 0) {
                    drawLine(color = vineStem, start = p, end = Offset(p.x, p.y + h * 0.05f), strokeWidth = 2f)
                    drawPaintedLeaf(
                        center = Offset(p.x + side * len * 0.35f, p.y + h * 0.05f),
                        length = len * 0.8f,
                        width = len * 0.26f,
                        color = vineLeaves[(i + 1) % vineLeaves.size],
                        rotationDeg = side * 40f
                    )
                }
            }

            // Faldon de la estanteria: cierra el hueco que dejaba el escenario partido por
            // la mitad y le da suelo a la estanteria de arriba.
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(shelfWood.shade(0.18f), shelfWood.shade(0.34f))
                ),
                topLeft = Offset(shelfLeft - w * 0.03f, apronTop),
                size = Size(shelfRight - shelfLeft + w * 0.06f, floorTop - apronTop)
            )
            // Tablones del faldon: sin esto es un bloque plano de color.
            var ax = shelfLeft - w * 0.03f
            while (ax < shelfRight + w * 0.03f) {
                drawRect(
                    color = shelfWood.shade(0.42f).copy(alpha = 0.5f),
                    topLeft = Offset(ax, apronTop),
                    size = Size(2.5f, floorTop - apronTop)
                )
                ax += w * 0.11f
            }
            drawRect(
                color = shelfWood.shade(0.45f).copy(alpha = 0.7f),
                topLeft = Offset(shelfLeft - w * 0.03f, floorTop - 4f),
                size = Size(shelfRight - shelfLeft + w * 0.06f, 4f)
            )

            drawTileFloor(floorTop, tileA, tileB, grout, cols = 6, rows = 3)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
                    startY = floorTop - h * 0.03f,
                    endY = floorTop + h * 0.04f
                ),
                topLeft = Offset(0f, floorTop - h * 0.03f),
                size = Size(w, h * 0.07f)
            )

            // Antes havia dos macetas enormes pegadas al borde inferior, cortadas
            // por la pantalla y con las hojas tan claras que parecian manchas. Se
            // sustituyen por un cajon de madera con dos plantas pequenas, enteras
            // y con contraste, que dan suelo sin robar protagonismo a los frascos.
            drawRect(color = crateColor.shade(0.18f), topLeft = crateRect.topLeft, size = crateRect.size)
            drawRect(
                color = crateColor.lighten(0.12f).copy(alpha = 0.6f),
                topLeft = Offset(crateRect.left, crateRect.top),
                size = Size(crateRect.width, crateRect.height * 0.16f)
            )
            crateRect.topLeft.let { corner ->
                drawRect(
                    color = crateColor.shade(0.35f).copy(alpha = 0.6f),
                    topLeft = corner,
                    size = Size(crateRect.width, crateRect.height)
                )
                listOf(0.34f, 0.68f).forEach { fx ->
                    drawRect(
                        color = crateColor.shade(0.5f).copy(alpha = 0.55f),
                        topLeft = Offset(corner.x + crateRect.width * fx, corner.y),
                        size = Size(3f, crateRect.height)
                    )
                }
            }
            drawPottedPlant(
                baseX = crateRect.left + crateRect.width * 0.28f,
                baseY = crateRect.top - 2f,
                sizePx = h * 0.085f,
                pot = potColor.shade(0.05f),
                leaves = listOf(greens[1], greens[3], greens[0])
            )
            drawPottedPlant(
                baseX = crateRect.left + crateRect.width * 0.72f,
                baseY = crateRect.top - 2f,
                sizePx = h * 0.070f,
                pot = potColor.shade(0.20f),
                leaves = listOf(greens[2], greens[0], greens[3])
            )

            drawCenterGlow(dark)
            drawVignette(dark, strength = 0.9f)
        }
    }
}

// ---------------------------------------------------------------------------
// Escenario 3 - Noche acogedora
// ---------------------------------------------------------------------------

private fun Modifier.nightScene(dark: Boolean): Modifier {
    val wallTop = if (dark) Color(0xFF23283A) else Color(0xFFFFF4DE)
    val wallBottom = if (dark) Color(0xFF151825) else Color(0xFFF7E4C4)
    val floorLight = if (dark) Color(0xFF2C2118) else Color(0xFF8B5E3C)
    val floorDark = if (dark) Color(0xFF22190F) else Color(0xFF734A2E)
    val seamColor = if (dark) Color(0xFF100C08) else Color(0xFF4E342E)
    val beamLight = if (dark) Color(0xFF6B4B36) else Color(0xFFE3C7A1)
    val beamDark = if (dark) Color(0xFF3E2A1E) else Color(0xFFCBA984)
    val beamGrain = if (dark) Color(0xFF20150E) else Color(0xFF9C7A5A)
    val wire = Color(0xFF6D5B4A)
    val bulbOn = Color(0xFFFFE082)
    val glow = Color(0xFFFFC44D).copy(alpha = if (dark) 0.35f else 0.45f)
    val baseBody = if (dark) Color(0xFF0C0E14) else Color(0xFFD8BF95)
    val baseHi = if (dark) Color(0xFF333A4A) else Color(0xFFFFF1D6)
    val shelfWood = if (dark) Color(0xFF5A4238) else Color(0xFF9C7A5A)
    val greens = listOf(
        Color(0xFF7CB342), Color(0xFF8BC34A), Color(0xFF9CCC65),
        Color(0xFF66BB6A), Color(0xFFA5C77A)
    )
    val stemColor = if (dark) Color(0xFF4E4132) else Color(0xFF7A5C3E)
    val twine = if (dark) Color(0xFF8A7A66) else Color(0xFF8D6E63)
    val frame = if (dark) Color(0xFF6D5B4A) else Color(0xFF8D6E63)
    val candleBody = if (dark) Color(0xFFE8D9B8) else Color(0xFFF6EAD0)
    val motifColor = if (dark) Color.White.copy(alpha = 0.03f) else Color(0xFFE3C08F).copy(alpha = 0.55f)

    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val floorTop = h * 0.82f
        val beamH = h * 0.045f
        val rnd = Random(31337)

        val motes = buildMotes(rnd, 34, w, floorTop, 1f, 2.4f)

        val motifs = buildList {
            var row = 0
            var y = h * 0.13f
            while (y < floorTop - 24f) {
                var x = if (row % 2 == 0) 36f else 84f
                while (x < w - 12f) {
                    add(Offset(x, y))
                    x += 98f
                }
                y += 86f
                row++
            }
        }

        val bundles = listOf(
            Triple(w * 0.12f, h * 0.17f, 0),
            Triple(w * 0.88f, h * 0.15f, 2),
            Triple(w * 0.78f, h * 0.20f, 3)
        )

        // El aparador cierra la pared. Antes la balda de las velas flotaba a media
        // altura y debajo quedaba un tramo de pared vacio que hacia que el
        // escenario pareciera cortado por la mitad.
        val shelfY = h * 0.735f
        val shelfH = h * 0.02f
        val candles = listOf(
            Triple(w * 0.09f, h * 0.075f, 0),
            Triple(w * 0.17f, h * 0.105f, 1),
            Triple(w * 0.25f, h * 0.065f, 2)
        )

        val winLeft = w * 0.62f
        val winRight = w * 1.05f
        val winTop = h * 0.24f
        val winBottom = h * 0.72f
        val moonCx = winLeft + (winRight - winLeft) * 0.35f
        val moonCy = winTop + (winBottom - winTop) * 0.30f
        val stars = List(26) {
            Triple(
                winLeft + rnd.nextFloat() * (winRight - winLeft),
                winTop + rnd.nextFloat() * (winBottom - winTop) * 0.75f,
                1.2f + rnd.nextFloat() * 1.8f
            )
        }

        onDrawBehind {
            drawRect(brush = Brush.verticalGradient(listOf(wallTop, wallBottom), startY = 0f, endY = floorTop))
            motifs.forEach { c -> drawFlowerMotif(c, 9f, motifColor) }

            drawSoftGlow(
                Offset(w * 0.5f, h * 0.18f),
                w * 0.55f,
                Color(0xFFFFC44D).copy(alpha = if (dark) 0.10f else 0.16f)
            )
            drawSoftGlow(
                Offset(w * 0.17f, shelfY - h * 0.07f),
                w * 0.35f,
                Color(0xFFFFB74D).copy(alpha = 0.18f)
            )

            drawArchedWindow(
                left = winLeft,
                top = winTop,
                right = winRight,
                bottom = winBottom,
                frame = frame,
                skyBrush = Brush.verticalGradient(
                    listOf(Color(0xFF101A38), Color(0xFF24325C)),
                    startY = winTop,
                    endY = winBottom
                )
            ) {
                stars.forEach { (sx, sy, sr) ->
                    drawCircle(color = Color(0xFFFFF9C4).copy(alpha = 0.9f), radius = sr, center = Offset(sx, sy))
                }
                drawSoftGlow(
                    center = Offset(moonCx, moonCy),
                    radius = (winRight - winLeft) * 0.30f,
                    color = Color(0xFFFFF59D).copy(alpha = 0.35f)
                )
                drawCircle(color = Color(0xFFFFFDE7), radius = (winRight - winLeft) * 0.11f, center = Offset(moonCx, moonCy))
                drawCircle(
                    color = Color(0xFF24325C),
                    radius = (winRight - winLeft) * 0.10f,
                    center = Offset(moonCx + (winRight - winLeft) * 0.055f, moonCy - (winRight - winLeft) * 0.03f)
                )
                drawHillBand(
                    left = winLeft,
                    right = winRight,
                    yBase = winBottom - (winBottom - winTop) * 0.22f,
                    amp = (winBottom - winTop) * 0.14f,
                    color = Color(0xFF0B1024),
                    seed = 99,
                    extendBelow = winBottom
                )
            }

            drawWoodBeam(0f, beamH, beamLight, beamDark, beamGrain)

            drawStringLights(
                yBase = beamH + h * 0.015f,
                sag = h * 0.055f,
                count = 9,
                wire = wire,
                glow = glow,
                bulb = bulbOn
            )

            bundles.forEach { (bx, length, idx) ->
                drawHangingHerbs(
                    anchorX = bx,
                    anchorY = beamH - 2f,
                    length = length,
                    leafColor = greens[idx % greens.size].shade(if (dark) 0.35f else 0.10f),
                    stemColor = stemColor,
                    twine = twine
                )
            }

            // Cuerpo del aparador: de la balda al suelo, a todo el ancho. Es lo que evita
            // el tramo de pared vacio que dejaba la escena partida en dos.
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(shelfWood.shade(0.16f), shelfWood.shade(0.34f))
                ),
                topLeft = Offset(0f, shelfY + shelfH),
                size = Size(w, floorTop - shelfY - shelfH)
            )
            var px = 0f
            while (px < w) {
                drawRect(
                    color = shelfWood.shade(0.44f).copy(alpha = 0.45f),
                    topLeft = Offset(px, shelfY + shelfH),
                    size = Size(2.5f, floorTop - shelfY - shelfH)
                )
                px += w * 0.12f
            }
            // Tablero superior, del ancho de la pared.
            drawRect(
                brush = Brush.verticalGradient(listOf(shelfWood.lighten(0.18f), shelfWood)),
                topLeft = Offset(0f, shelfY),
                size = Size(w, shelfH)
            )
            drawRect(
                color = shelfWood.shade(0.42f).copy(alpha = 0.75f),
                topLeft = Offset(0f, shelfY + shelfH - 3f),
                size = Size(w, 3f)
            )
            candles.forEach { (cx, ch, _) ->
                drawCandle(x = cx, baseY = shelfY - 1f, height = ch, body = candleBody, flameGlow = glow)
            }

            drawMotes(motes, if (dark) Color(0xFFFFE082).copy(alpha = 0.35f) else Color(0xFFFFE082).copy(alpha = 0.5f))

            drawWoodFloor(floorTop, floorLight, floorDark, seamColor, seed = 5150, rows = 6)
            drawBaseboard(floorTop, baseBody, baseHi)

            drawCenterGlow(dark)
            drawVignette(dark, strength = 1.05f)
        }
    }
}

// ---------------------------------------------------------------------------
// Escenario 4 - Bosque místico
// ---------------------------------------------------------------------------

private fun Modifier.forestScene(dark: Boolean): Modifier {
    val skyTop = if (dark) Color(0xFF14202A) else Color(0xFFEAF4E6)
    val skyBottom = if (dark) Color(0xFF0C141C) else Color(0xFFD8EAD2)
    val treeFar = if (dark) Color(0xFF152530) else Color(0xFFBBD9B2)
    val treeMid = if (dark) Color(0xFF101C24) else Color(0xFF9AC890)
    val treeNear = if (dark) Color(0xFF0A131A) else Color(0xFF77B06D)
    val groundTop = if (dark) Color(0xFF1B2A22) else Color(0xFFA9CB92)
    val groundBottom = if (dark) Color(0xFF101A15) else Color(0xFF87B476)
    val trunkColor = if (dark) Color(0xFF1A1310) else Color(0xFF7C5A3E)
    val trunkDark = if (dark) Color(0xFF120D0A) else Color(0xFF5E4029)
    val fernColor = if (dark) Color(0xFF16281D) else Color(0xFF5FA356)

    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val horizon = h * 0.58f
        val groundTopY = h * 0.66f
        val rnd = Random(6060)

        val spores = buildMotes(rnd, 46, w, h * 0.9f, 1f, 3f)
        val groundDots = buildMotes(rnd, 60, w, h - groundTopY, 1.2f, 3.4f).map {
            Triple(it.first, it.second + groundTopY, it.third)
        }
        val clouds = listOf(
            Triple(w * 0.22f, h * 0.14f, w * 0.20f),
            Triple(w * 0.68f, h * 0.10f, w * 0.16f)
        )

        val shaftA = Path().apply {
            moveTo(w * 0.10f, 0f)
            lineTo(w * 0.34f, 0f)
            lineTo(w * 0.66f, h)
            lineTo(w * 0.30f, h)
            close()
        }
        val shaftB = Path().apply {
            moveTo(w * 0.55f, 0f)
            lineTo(w * 0.70f, 0f)
            lineTo(w * 0.98f, h)
            lineTo(w * 0.76f, h)
            close()
        }

        onDrawBehind {
            drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyBottom), startY = 0f, endY = horizon))

            drawSoftGlow(
                center = Offset(w * 0.76f, h * 0.16f),
                radius = w * 0.42f,
                color = if (dark) Color(0xFFBFD8FF).copy(alpha = 0.16f) else Color(0xFFFFF3B0).copy(alpha = 0.55f)
            )
            drawCircle(
                color = if (dark) Color(0xFFE8EEFF) else Color(0xFFFFF9DB),
                radius = w * 0.055f,
                center = Offset(w * 0.76f, h * 0.16f)
            )

            if (!dark) {
                clouds.forEach { (cx, cy, cs) -> drawCloud(Offset(cx, cy), cs, Color.White.copy(alpha = 0.85f)) }
            }

            drawTreeline(baseline = horizon, height = h * 0.26f, color = treeFar, seed = 11, count = 14)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        if (dark) Color(0xFF9FB6C4).copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f)
                    ),
                    startY = horizon - h * 0.14f,
                    endY = horizon + h * 0.02f
                ),
                topLeft = Offset(0f, horizon - h * 0.14f),
                size = Size(w, h * 0.16f)
            )
            drawTreeline(baseline = horizon + h * 0.05f, height = h * 0.30f, color = treeMid, seed = 22, count = 11)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        if (dark) Color(0xFF9FB6C4).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.45f)
                    ),
                    startY = horizon - h * 0.06f,
                    endY = horizon + h * 0.10f
                ),
                topLeft = Offset(0f, horizon - h * 0.06f),
                size = Size(w, h * 0.16f)
            )
            drawTreeline(baseline = groundTopY + h * 0.02f, height = h * 0.34f, color = treeNear, seed = 33, count = 8)

            drawRect(
                brush = Brush.verticalGradient(listOf(groundTop, groundBottom), startY = groundTopY, endY = h),
                topLeft = Offset(0f, groundTopY),
                size = Size(w, h - groundTopY)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, groundTop.copy(alpha = 0.85f)),
                    startY = groundTopY - h * 0.06f,
                    endY = groundTopY + h * 0.04f
                ),
                topLeft = Offset(0f, groundTopY - h * 0.06f),
                size = Size(w, h * 0.10f)
            )
            groundDots.forEach { (x, y, r) ->
                drawCircle(color = groundBottom.shade(0.18f).copy(alpha = 0.5f), radius = r, center = Offset(x, y))
            }

            val floraRnd = Random(4242)
            val grassColor = groundBottom.shade(0.24f)
            repeat(30) {
                val gx = floraRnd.nextFloat() * w
                val gy = groundTopY + h * (0.04f + floraRnd.nextFloat() * 0.30f)
                drawGrassTuft(gx, gy, w * (0.030f + floraRnd.nextFloat() * 0.02f), grassColor.copy(alpha = 0.75f))
            }
            val petalColors = listOf(
                Color(0xFFFFF8E1), Color(0xFFFFE082), Color(0xFFF8BBD0),
                Color(0xFFE1BEE7), Color(0xFFFFF3C4)
            )
            repeat(16) {
                val fx = w * 0.04f + floraRnd.nextFloat() * w * 0.92f
                val fy = groundTopY + h * (0.05f + floraRnd.nextFloat() * 0.30f)
                val fs = w * (0.020f + floraRnd.nextFloat() * 0.014f)
                drawWildflower(
                    x = fx,
                    y = fy,
                    size = fs,
                    petal = petalColors[floraRnd.nextInt(petalColors.size)].copy(alpha = 0.92f),
                    core = if (dark) Color(0xFFFFD54F) else Color(0xFFFFB300)
                )
            }
            repeat(3) { i ->
                drawMushroom(
                    x = w * (0.20f + i * 0.30f) + floraRnd.nextFloat() * w * 0.06f,
                    baseY = groundTopY + h * (0.20f + floraRnd.nextFloat() * 0.14f),
                    size = w * (0.055f + floraRnd.nextFloat() * 0.02f),
                    capColor = if (dark) Color(0xFF8D6E63) else Color(0xFFE57373),
                    stemColor = if (dark) Color(0xFFD7CCC8) else Color(0xFFFFF8E1)
                )
            }
            repeat(3) { i ->
                drawStone(
                    baseX = w * (0.14f + i * 0.33f),
                    baseY = groundTopY + h * (0.26f + floraRnd.nextFloat() * 0.10f),
                    w = w * (0.10f + floraRnd.nextFloat() * 0.06f),
                    h = h * 0.035f,
                    color = if (dark) Color(0xFF5D5D6B) else Color(0xFFAFA896)
                )
            }

            drawLightShaft(
                shaftA,
                if (dark) Color(0xFFCFE0FF).copy(alpha = 0.05f) else Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0f),
                Offset(w * 0.2f, 0f),
                Offset(w * 0.5f, h)
            )
            drawLightShaft(
                shaftB,
                if (dark) Color(0xFFCFE0FF).copy(alpha = 0.04f) else Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0f),
                Offset(w * 0.6f, 0f),
                Offset(w * 0.9f, h)
            )

            val trunkW = w * 0.13f
            listOf(0f, w - trunkW).forEach { tx ->
                drawRect(color = trunkColor, topLeft = Offset(tx, 0f), size = Size(trunkW, h))
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            trunkDark,
                            trunkColor.lighten(0.18f),
                            trunkColor,
                            trunkDark
                        ),
                        startX = tx,
                        endX = tx + trunkW
                    ),
                    topLeft = Offset(tx, 0f),
                    size = Size(trunkW, h)
                )
                val barkRnd = Random(tx.toInt() + 7)
                repeat(7) {
                    val bx = tx + trunkW * (0.1f + barkRnd.nextFloat() * 0.8f)
                    val by = h * barkRnd.nextFloat()
                    val bh = h * (0.08f + barkRnd.nextFloat() * 0.14f)
                    drawLine(
                        color = trunkDark.copy(alpha = 0.6f),
                        start = Offset(bx, by),
                        end = Offset(bx + (barkRnd.nextFloat() - 0.5f) * 8f, by + bh),
                        strokeWidth = 2.5f
                    )
                }
            }

            repeat(4) { i ->
                val fx = if (i % 2 == 0) w * (0.05f + i * 0.02f) else w * (0.95f - i * 0.02f)
                repeat(5) { j ->
                    val rot = if (i % 2 == 0) -55f + j * 26f else 125f - j * 26f
                    drawPaintedLeaf(
                        center = Offset(fx + if (i % 2 == 0) 14f else -14f, h * (0.93f - j * 0.02f)),
                        length = w * 0.11f,
                        width = w * 0.035f,
                        color = fernColor.copy(alpha = 0.85f),
                        rotationDeg = rot
                    )
                }
            }

            if (dark) {
                spores.forEach { (x, y, r) ->
                    drawSoftGlow(center = Offset(x, y), radius = r * 4f, color = Color(0xFFFFF59D).copy(alpha = 0.30f))
                    drawCircle(color = Color(0xFFFFF59D), radius = r * 0.8f, center = Offset(x, y))
                }
            } else {
                drawMotes(spores, Color.White.copy(alpha = 0.65f))
            }

            // Velo superior para que los troncos no resten contraste al HUD
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(skyTop, skyTop.copy(alpha = 0f)),
                    startY = 0f,
                    endY = h * 0.17f
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, h * 0.17f)
            )

            drawCenterGlow(dark)
            drawVignette(dark, strength = 1f)
        }
    }
}

// ---------------------------------------------------------------------------
// Escenario 5 - Jardín de otoño
// ---------------------------------------------------------------------------

private fun Modifier.autumnScene(dark: Boolean): Modifier {
    val skyTop = if (dark) Color(0xFF2A2440) else Color(0xFFFFE9CE)
    val skyMid = if (dark) Color(0xFF1E1B32) else Color(0xFFFFD5A6)
    val skyBottom = if (dark) Color(0xFF171528) else Color(0xFFFFC59B)
    val mountainFar = if (dark) Color(0xFF37304F) else Color(0xFFE0A97F)
    val mountainNear = if (dark) Color(0xFF2A2440) else Color(0xFFCB8760)
    val bambooStalk = if (dark) Color(0xFF3E4A32) else Color(0xFF9CB86A)
    val bambooNode = if (dark) Color(0xFF2A331F) else Color(0xFF7E9A4E)
    val bambooLeaf = if (dark) Color(0xFF4A5A3A) else Color(0xFF8FB057)
    val groundTop = if (dark) Color(0xFF2A2338) else Color(0xFFEBD3AE)
    val groundBottom = if (dark) Color(0xFF1E1930) else Color(0xFFDCBB92)
    val stoneColor = if (dark) Color(0xFF3A3350) else Color(0xFFB6A288)
    val lanternBody = if (dark) Color(0xFFFFF3C4) else Color(0xFFFFF8E1)
    val leafColors = listOf(
        Color(0xFFE6834A), Color(0xFFF0A93F), Color(0xFFD9663F),
        Color(0xFFEFC15C), Color(0xFFC9603C)
    )

    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val horizon = h * 0.60f
        val groundY = h * 0.70f
        val rnd = Random(9091)

        val clouds = listOf(
            Triple(w * 0.30f, h * 0.16f, w * 0.22f),
            Triple(w * 0.72f, h * 0.11f, w * 0.17f)
        )
        val fallingLeaves = List(16) {
            Triple(
                rnd.nextFloat() * w,
                h * (0.08f + rnd.nextFloat() * 0.72f),
                rnd.nextFloat()
            )
        }
        val groundLeaves = List(18) {
            Triple(
                rnd.nextFloat() * w,
                groundY + rnd.nextFloat() * (h - groundY) * 0.9f,
                rnd.nextFloat()
            )
        }
        val motes = buildMotes(rnd, 34, w, groundY, 1f, 2.6f)

        onDrawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(skyTop, skyMid, skyBottom),
                    startY = 0f,
                    endY = horizon
                )
            )

            val sunCx = w * 0.28f
            val sunCy = horizon - h * 0.14f
            drawSoftGlow(
                center = Offset(sunCx, sunCy),
                radius = w * 0.45f,
                color = if (dark) Color(0xFFD8E4FF).copy(alpha = 0.16f) else Color(0xFFFFD180).copy(alpha = 0.55f)
            )
            drawCircle(
                color = if (dark) Color(0xFFEFF3FF) else Color(0xFFFFE0A3),
                radius = w * 0.07f,
                center = Offset(sunCx, sunCy)
            )

            if (!dark) {
                clouds.forEach { (cx, cy, cs) -> drawCloud(Offset(cx, cy), cs, Color.White.copy(alpha = 0.7f)) }
            }

            drawHillBand(0f, w, horizon, h * 0.16f, mountainFar, seed = 5, extendBelow = h)
            drawHillBand(0f, w, horizon + h * 0.07f, h * 0.14f, mountainNear, seed = 15, extendBelow = h)

            drawRect(
                brush = Brush.verticalGradient(listOf(groundTop, groundBottom), startY = groundY, endY = h),
                topLeft = Offset(0f, groundY),
                size = Size(w, h - groundY)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(mountainNear.copy(alpha = 0.75f), Color.Transparent),
                    startY = groundY - h * 0.03f,
                    endY = groundY + h * 0.05f
                ),
                topLeft = Offset(0f, groundY - h * 0.03f),
                size = Size(w, h * 0.08f)
            )

            repeat(5) { i ->
                val y = groundY + h * (0.055f + i * 0.06f)
                val arc = Path().apply {
                    moveTo(-12f, y)
                    quadraticBezierTo(w * 0.5f, y - h * 0.022f, w + 12f, y)
                }
                drawPath(
                    path = arc,
                    color = groundBottom.shade(0.12f).copy(alpha = 0.40f),
                    style = Stroke(width = 3f)
                )
            }

            val bambooW = w * 0.045f
            drawBamboo(w * 0.055f, -h * 0.02f, h * 1.02f, bambooW, bambooStalk, bambooNode, bambooLeaf)
            drawBamboo(w * 0.135f, -h * 0.06f, h * 1.02f, bambooW * 0.8f, bambooStalk.shade(0.1f), bambooNode, bambooLeaf)
            drawBamboo(w * 0.945f, -h * 0.04f, h * 1.02f, bambooW, bambooStalk, bambooNode, bambooLeaf)
            drawBamboo(w * 0.865f, -h * 0.08f, h * 1.02f, bambooW * 0.8f, bambooStalk.shade(0.1f), bambooNode, bambooLeaf)

            drawStone(w * 0.30f, h * 0.96f, w * 0.24f, h * 0.09f, stoneColor)
            drawStone(w * 0.62f, h * 0.99f, w * 0.30f, h * 0.11f, stoneColor.shade(0.12f))
            drawStone(w * 0.80f, h * 0.93f, w * 0.16f, h * 0.06f, stoneColor.lighten(0.1f))

            groundLeaves.forEach { (lx, ly, t) ->
                drawPaintedLeaf(
                    center = Offset(lx, ly),
                    length = w * 0.055f,
                    width = w * 0.018f,
                    color = leafColors[(t * leafColors.size).toInt() % leafColors.size].shade(0.1f),
                    rotationDeg = -180f + t * 360f
                )
            }

            fallingLeaves.forEach { (lx, ly, t) ->
                drawPaintedLeaf(
                    center = Offset(lx, ly),
                    length = w * 0.06f,
                    width = w * 0.02f,
                    color = leafColors[(t * leafColors.size).toInt() % leafColors.size],
                    rotationDeg = -60f + t * 240f
                )
            }

            drawStoneLantern(cx = w * 0.47f, baseY = h * 0.985f, width = w * 0.15f, glowColor = lanternBody, stone = stoneColor)

            drawMotes(motes, if (dark) Color(0xFFFFE082).copy(alpha = 0.35f) else Color(0xFFFFF3C4).copy(alpha = 0.7f))

            drawCenterGlow(dark)
            drawVignette(dark, strength = 1f)
        }
    }
}






