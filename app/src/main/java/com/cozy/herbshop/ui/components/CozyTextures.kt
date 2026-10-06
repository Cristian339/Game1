package com.cozy.herbshop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.cozy.herbshop.ui.theme.SageGreen
import com.cozy.herbshop.ui.theme.SoftGold
import com.cozy.herbshop.ui.theme.TerracottaWarm
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private fun Color.cozyLighten(factor: Float): Color = Color(
    red = (red + (1f - red) * factor).coerceIn(0f, 1f),
    green = (green + (1f - green) * factor).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.cozyDarken(factor: Float): Color = Color(
    red = (red * (1f - factor)).coerceIn(0f, 1f),
    green = (green * (1f - factor)).coerceIn(0f, 1f),
    blue = (blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = alpha
)

/**
 * Fondo cozy para todas las pantallas: color base + moteado de papel,
 * manchas suaves de acuarela y viñeta cálida. Se cachea por tamaño.
 */
@Composable
fun Modifier.cozyBackground(): Modifier {
    val background = MaterialTheme.colorScheme.background
    val dark = isSystemInDarkTheme()
    val speckA = if (dark) Color.White.copy(alpha = 0.04f) else Color(0xFF5D4037).copy(alpha = 0.055f)
    val speckB = if (dark) SageGreen.copy(alpha = 0.05f) else SageGreen.copy(alpha = 0.10f)
    val blobA = if (dark) SageGreen.copy(alpha = 0.07f) else SageGreen.copy(alpha = 0.12f)
    val blobB = if (dark) TerracottaWarm.copy(alpha = 0.05f) else TerracottaWarm.copy(alpha = 0.09f)
    val blobC = if (dark) SoftGold.copy(alpha = 0.04f) else SoftGold.copy(alpha = 0.08f)
    val vignetteInner = Color.Transparent
    val vignetteOuter = if (dark) Color.Black.copy(alpha = 0.28f) else Color(0xFF4E342E).copy(alpha = 0.08f)
    val fiberColor = if (dark) Color.White.copy(alpha = 0.03f) else Color(0xFF6D4C41).copy(alpha = 0.05f)

    return this
        .background(background)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val rnd = Random(20260924)
            val speckles = List(120) {
                Triple(rnd.nextFloat() * w, rnd.nextFloat() * h, 0.5f + rnd.nextFloat() * 1.7f)
            }
            val fibers = List(22) {
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                val len = w * (0.03f + rnd.nextFloat() * 0.09f)
                val dy = (rnd.nextFloat() - 0.5f) * 6f
                Triple(Offset(x, y), Offset(x + len, y + dy), 0.7f + rnd.nextFloat() * 0.9f)
            }
            val vignette = Brush.radialGradient(
                colors = listOf(vignetteInner, vignetteOuter),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = maxOf(w, h) * 0.75f
            )
            val wallWash = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    if (dark) Color.Black.copy(alpha = 0.06f) else Color(0x0A000000),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            fun softBlob(center: Offset, radius: Float, color: Color): Brush =
                Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                )

            val blob1 = softBlob(Offset(w * 0.12f, h * 0.10f), w * 0.62f, blobA)
            val blob2 = softBlob(Offset(w * 0.88f, h * 0.78f), w * 0.55f, blobB)
            val blob3 = softBlob(Offset(w * 0.75f, h * 0.15f), w * 0.40f, blobC)
            val c1 = Offset(w * 0.12f, h * 0.10f)
            val c2 = Offset(w * 0.88f, h * 0.78f)
            val c3 = Offset(w * 0.75f, h * 0.15f)
            val r1 = w * 0.62f
            val r2 = w * 0.55f
            val r3 = w * 0.40f

            onDrawBehind {
                drawCircle(brush = blob1, radius = r1, center = c1)
                drawCircle(brush = blob2, radius = r2, center = c2)
                drawCircle(brush = blob3, radius = r3, center = c3)

                fibers.forEach { (a, b, stroke) ->
                    drawLine(color = fiberColor, start = a, end = b, strokeWidth = stroke)
                }
                speckles.forEach { (x, y, r) ->
                    drawCircle(color = speckA, radius = r, center = Offset(x, y))
                }
                val moteRnd = Random(555)
                repeat(26) {
                    val mx = moteRnd.nextFloat() * w
                    val my = moteRnd.nextFloat() * h
                    val mr = 1.2f + moteRnd.nextFloat() * 2.2f
                    drawCircle(color = speckB, radius = mr, center = Offset(mx, my))
                }

                drawRect(brush = wallWash)
                drawRect(brush = vignette)
            }
        }
}

/** Hoja pintada sencilla (óvalo con nervio) usada en menú y adornos. */
fun DrawScope.drawPaintedLeaf(
    center: Offset,
    length: Float,
    width: Float,
    color: Color,
    rotationDeg: Float
) {
    withTransform({
        rotate(rotationDeg, pivot = center)
    }) {
        val path = Path().apply {
            moveTo(center.x - length / 2f, center.y)
            quadraticBezierTo(
                center.x, center.y - width,
                center.x + length / 2f, center.y
            )
            quadraticBezierTo(
                center.x, center.y + width,
                center.x - length / 2f, center.y
            )
            close()
        }
        drawPath(path, color = color)
        drawPath(path, color = color.cozyLighten(0.35f).copy(alpha = 0.55f), style = Stroke(width = 1.2f))
        drawLine(
            color = color.cozyDarken(0.28f).copy(alpha = 0.7f),
            start = Offset(center.x - length / 2f, center.y),
            end = Offset(center.x + length / 2f, center.y),
            strokeWidth = 1.4f
        )
    }
}

/**
 * Rótulo de madera para el menú principal: tablero con veta, borde biselado
 * y textura de vetas horizontales.
 */
@Composable
fun Modifier.woodPlaqueBackground(): Modifier {
    val dark = isSystemInDarkTheme()
    val light1 = if (dark) Color(0xFF8D6E63) else Color(0xFF9C7A5A)
    val mid = if (dark) Color(0xFF6D4C41) else Color(0xFF7A5A42)
    val dark1 = if (dark) Color(0xFF4E342E) else Color(0xFF5D4037)
    val edge = dark1.cozyDarken(0.25f)

    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val radius = h * 0.28f
        val rnd = Random(4242)
        val grains = List(16) {
            val y = h * (0.08f + rnd.nextFloat() * 0.84f)
            val x0 = w * rnd.nextFloat() * 0.25f
            val x1 = w - w * rnd.nextFloat() * 0.25f
            val amp = (rnd.nextFloat() - 0.5f) * h * 0.04f
            Triple(y, Offset(x0, y), Offset(x1, y + amp))
        }
        val fill = Brush.verticalGradient(listOf(light1, mid, dark1))
        val sheen = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.12f)),
            start = Offset(0f, 0f),
            end = Offset(w * 0.3f, h)
        )

        onDrawBehind {
            drawRoundRect(brush = fill, size = size, cornerRadius = CornerRadius(radius, radius))
            grains.forEach { (_, a, b) ->
                val path = Path().apply {
                    moveTo(a.x, a.y)
                    quadraticBezierTo((a.x + b.x) / 2f, a.y + (b.y - a.y) * 0.3f - 4f, b.x, b.y)
                }
                drawPath(path, color = dark1.cozyDarken(0.2f).copy(alpha = 0.35f), style = Stroke(width = 1.6f))
            }
            drawRoundRect(brush = sheen, size = size, cornerRadius = CornerRadius(radius, radius))
            drawRoundRect(
                color = edge,
                size = size,
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = 5f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(4f, 4f),
                size = Size(w - 8f, h - 8f),
                cornerRadius = CornerRadius(radius * 0.9f, radius * 0.9f),
                style = Stroke(width = 1.6f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Decoraciones compradas en la tienda ("Decoración de la Herboristería").
// Se dibujan en el menú principal y en la vista previa de la propia tienda.
// Todo procedural, sin assets externos.
// ---------------------------------------------------------------------------

private fun DrawScope.drawShopMiniJar(cx: Float, baseY: Float, jarH: Float, content: Color) {
    val jw = jarH * 0.62f
    val top = baseY - jarH
    drawRoundRect(
        color = Color.White.copy(alpha = 0.40f),
        topLeft = Offset(cx - jw / 2f, top),
        size = Size(jw, jarH),
        cornerRadius = CornerRadius(jw * 0.22f, jw * 0.22f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(content.cozyLighten(0.25f), content),
            startY = top + jarH * 0.34f,
            endY = baseY
        ),
        topLeft = Offset(cx - jw / 2f + 3f, top + jarH * 0.36f),
        size = Size(jw - 6f, jarH * 0.64f - 3f),
        cornerRadius = CornerRadius(jw * 0.16f, jw * 0.16f)
    )
    drawRoundRect(
        color = Color(0xFF6D4C41),
        topLeft = Offset(cx - jw * 0.30f, top - jarH * 0.14f),
        size = Size(jw * 0.60f, jarH * 0.16f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawLine(
        color = Color.White.copy(alpha = 0.55f),
        start = Offset(cx - jw / 2f + 4f, top + jarH * 0.14f),
        end = Offset(cx - jw / 2f + 4f, baseY - jarH * 0.10f),
        strokeWidth = 2.5f
    )
}

/** Estufa de leña con puerta encendida y resplandor en el suelo. */
private fun DrawScope.drawShopStove(cx: Float, baseY: Float, sizePx: Float, dark: Boolean) {
    val sw = sizePx * 0.94f
    val sh = sizePx * 0.88f
    val bodyTop = baseY - sh
    val iron = if (dark) Color(0xFF2B2724) else Color(0xFF4A4441)
    val ironLight = if (dark) Color(0xFF3A3532) else Color(0xFF5E5652)
    val ironDark = if (dark) Color(0xFF171412) else Color(0xFF332E2B)

    drawOval(
        color = Color.Black.copy(alpha = 0.22f),
        topLeft = Offset(cx - sw * 0.62f, baseY - sh * 0.05f),
        size = Size(sw * 1.24f, sh * 0.15f)
    )

    listOf(-1f, 1f).forEach { s ->
        drawRoundRect(
            color = ironDark,
            topLeft = Offset(cx + s * sw * 0.34f - sw * 0.05f, baseY - sh * 0.13f),
            size = Size(sw * 0.10f, sh * 0.13f),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }

    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(ironDark, ironLight, iron)),
        topLeft = Offset(cx - sw / 2f, bodyTop + sh * 0.13f),
        size = Size(sw, sh * 0.74f),
        cornerRadius = CornerRadius(sw * 0.12f, sw * 0.12f)
    )
    drawRoundRect(
        color = ironLight,
        topLeft = Offset(cx - sw * 0.56f, bodyTop + sh * 0.06f),
        size = Size(sw * 1.12f, sh * 0.09f),
        cornerRadius = CornerRadius(sw * 0.05f, sw * 0.05f)
    )

    val doorW = sw * 0.56f
    val doorH = sh * 0.34f
    val doorX = cx - doorW / 2f
    val doorY = bodyTop + sh * 0.32f
    drawSoftGlow(
        center = Offset(cx, doorY + doorH / 2f),
        radius = sw * 1.15f,
        color = Color(0xFFFF9800).copy(alpha = if (dark) 0.42f else 0.28f)
    )
    drawRoundRect(
        color = ironDark,
        topLeft = Offset(doorX - sw * 0.05f, doorY - sh * 0.04f),
        size = Size(doorW + sw * 0.10f, doorH + sh * 0.08f),
        cornerRadius = CornerRadius(sw * 0.08f, sw * 0.08f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFF8A3D), Color(0xFFF4511E))),
        topLeft = Offset(doorX, doorY),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(doorW * 0.18f, doorW * 0.18f)
    )
    repeat(3) { i ->
        val lx = doorX + doorW * (0.24f + i * 0.26f)
        drawLine(
            color = Color(0xFF4E342E).copy(alpha = 0.85f),
            start = Offset(lx, doorY + doorH * 0.78f),
            end = Offset(lx + doorW * 0.12f, doorY + doorH * 0.32f),
            strokeWidth = sw * 0.055f
        )
    }
    drawSoftGlow(
        center = Offset(cx, doorY + doorH * 0.7f),
        radius = doorW * 0.55f,
        color = Color(0xFFFFD54F).copy(alpha = 0.55f)
    )
    drawCircle(color = ironLight, radius = sw * 0.045f, center = Offset(doorX + doorW + sw * 0.015f, doorY + doorH * 0.5f))

    // Tetera sobre la placa
    val kw = sw * 0.34f
    val kh = sh * 0.16f
    val kx = cx + sw * 0.16f
    val ky = bodyTop + sh * 0.06f
    drawRoundRect(
        color = if (dark) Color(0xFF6D4C41) else Color(0xFF8D6E63),
        topLeft = Offset(kx - kw / 2f, ky - kh),
        size = Size(kw, kh),
        cornerRadius = CornerRadius(kw * 0.30f, kw * 0.30f)
    )
    drawLine(
        color = if (dark) Color(0xFF6D4C41) else Color(0xFF8D6E63),
        start = Offset(kx + kw * 0.45f, ky - kh * 0.8f),
        end = Offset(kx + kw * 0.85f, ky - kh * 1.15f),
        strokeWidth = sh * 0.02f
    )
    drawArc(
        color = if (dark) Color(0xFF6D4C41) else Color(0xFF8D6E63),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(kx - kw * 0.3f, ky - kh * 1.5f),
        size = Size(kw * 0.6f, kh * 0.7f),
        style = Stroke(width = sh * 0.018f)
    )
}

/** Estante de pie con tres repisas y frascos. */
private fun DrawScope.drawShopShelfUnit(cx: Float, baseY: Float, sizePx: Float, dark: Boolean) {
    val sw = sizePx * 1.02f
    val sh = sizePx * 0.98f
    val top = baseY - sh
    val wood = if (dark) Color(0xFF5A4238) else Color(0xFF8D6E63)
    val woodLight = if (dark) Color(0xFF6D4C41) else Color(0xFFA17B57)
    val woodDark = if (dark) Color(0xFF3E2C25) else Color(0xFF6D4C41)
    val jarColors = listOf(
        Color(0xFFFBC02D), Color(0xFFAB47BC), Color(0xFF66BB6A),
        Color(0xFFE91E63), Color(0xFF8BC34A), Color(0xFFFF9800)
    )

    drawOval(
        color = Color.Black.copy(alpha = 0.20f),
        topLeft = Offset(cx - sw * 0.60f, baseY - sh * 0.04f),
        size = Size(sw * 1.20f, sh * 0.13f)
    )

    drawRect(
        color = woodDark.copy(alpha = 0.55f),
        topLeft = Offset(cx - sw * 0.46f, top + sh * 0.03f),
        size = Size(sw * 0.92f, sh * 0.94f)
    )

    listOf(-1f, 1f).forEach { s ->
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(woodLight, wood, woodDark)),
            topLeft = Offset(cx + s * sw * 0.50f - sw * 0.045f, top),
            size = Size(sw * 0.09f, sh),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    repeat(3) { i ->
        val sy = top + sh * (0.24f + i * 0.28f)
        drawRect(
            brush = Brush.verticalGradient(listOf(woodLight, wood)),
            topLeft = Offset(cx - sw * 0.50f, sy),
            size = Size(sw, sh * 0.05f)
        )
        drawRect(
            color = woodDark.copy(alpha = 0.55f),
            topLeft = Offset(cx - sw * 0.50f, sy + sh * 0.05f - 2f),
            size = Size(sw, 2f)
        )
        val jarCount = if (i == 1) 3 else 2
        for (j in 0 until jarCount) {
            val t = if (jarCount == 1) 0.5f else j / (jarCount - 1f)
            drawShopMiniJar(
                cx = cx - sw * 0.30f + t * sw * 0.60f,
                baseY = sy,
                jarH = sh * 0.17f,
                content = jarColors[(i * 2 + j) % jarColors.size]
            )
        }
    }

    drawRoundRect(
        color = woodDark,
        topLeft = Offset(cx - sw * 0.50f, baseY - sh * 0.045f),
        size = Size(sw, sh * 0.045f),
        cornerRadius = CornerRadius(3f, 3f)
    )
}

/**
 * Dibuja las decoraciones desbloqueadas dentro de la banda de suelo indicada.
 * [left]/[right] = ancho disponible, [top]/[bottom] = banda de suelo.
 */
internal fun DrawScope.drawShopFloorDecorations(
    decorations: Set<String>,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    dark: Boolean
) {
    val bandW = right - left
    val bandH = bottom - top
    if (bandW <= 0f || bandH <= 0f) return
    val baseY = bottom - bandH * 0.12f

    if ("estufa_calida" in decorations) {
        drawShopStove(
            cx = left + bandW * 0.17f,
            baseY = baseY,
            sizePx = bandH * 0.78f,
            dark = dark
        )
    }

    if ("plantas_cozy" in decorations) {
        val potColor = if (dark) Color(0xFF8A5A44) else Color(0xFFD08A63)
        val leaves = listOf(
            Color(0xFF7CB342), Color(0xFF9CCC65), Color(0xFF66BB6A),
            Color(0xFFAED581), Color(0xFF8BC34A), Color(0xFF7CB342)
        )
        listOf(0.39f, 0.61f).forEach { t ->
            drawPottedPlant(
                baseX = left + bandW * t,
                baseY = baseY,
                sizePx = bandH * 0.56f,
                pot = potColor,
                leaves = leaves
            )
        }
    }

    if ("estante_roble" in decorations) {
        drawShopShelfUnit(
            cx = left + bandW * 0.84f,
            baseY = baseY,
            sizePx = bandH * 0.86f,
            dark = dark
        )
    }
}

/**
 * Escenario procedural del menú: pared cozy, guirnalda de hierbas colgadas,
 * estante con frascos y suelo de tablas de madera.
 *
 * @param decorations ids de decoraciones desbloqueadas (de la tienda).
 */
@Composable
fun Modifier.shopSceneBackground(decorations: Set<String> = emptySet()): Modifier {
    val dark = isSystemInDarkTheme()
    val wallTop = if (dark) Color(0xFF232A26) else Color(0xFFEFE6D4)
    val wallBottom = if (dark) Color(0xFF1A1F1C) else Color(0xFFE2D3B8)
    val stripe = if (dark) Color.White.copy(alpha = 0.025f) else Color(0x14000000)
    val floorA = if (dark) Color(0xFF3A2E26) else Color(0xFFB08968)
    val floorB = if (dark) Color(0xFF2E241E) else Color(0xFF9C7550)
    val floorLine = floorB.cozyDarken(0.30f)
    val woodDark = if (dark) Color(0xFF4A342A) else Color(0xFF6D4C41)
    val woodLight = if (dark) Color(0xFF6D4C41) else Color(0xFF8D6E63)
    val background = MaterialTheme.colorScheme.background
    val speckA = if (dark) Color.White.copy(alpha = 0.035f) else Color(0xFF5D4037).copy(alpha = 0.05f)

    return this
        .background(background)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val floorTop = h * 0.78f
            val shelfY = h * 0.52f
            val rnd = Random(90210)
            val speckles = List(90) {
                Triple(rnd.nextFloat() * w, rnd.nextFloat() * floorTop, 0.5f + rnd.nextFloat() * 1.5f)
            }
            val stripeW = w / 9f
            val jars = List(5) { i ->
                val jx = w * (0.10f + i * 0.18f) + rnd.nextFloat() * w * 0.02f
                val jh = h * (0.055f + rnd.nextFloat() * 0.03f)
                val jw = jh * 0.55f
                val colors = listOf(
                    Color(0xFFFBC02D), Color(0xFFAB47BC), Color(0xFF66BB6A),
                    Color(0xFFE91E63), Color(0xFF8BC34A), Color(0xFFFF9800)
                )
                Triple(jx, jh, colors[i % colors.size])
            }
            val bundles = List(6) { i ->
                val bx = w * (0.08f + i * 0.16f)
                val depth = h * (0.07f + rnd.nextFloat() * 0.05f)
                val leafColor = listOf(
                    Color(0xFF7CB342), Color(0xFF9CCC65), Color(0xFF66BB6A),
                    Color(0xFFAED581), Color(0xFF8BC34A), Color(0xFF7CB342)
                )[i]
                Triple(bx, depth, leafColor)
            }
            val vignette = Brush.radialGradient(
                colors = listOf(Color.Transparent, if (dark) Color.Black.copy(alpha = 0.45f) else Color(0x28000000)),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = maxOf(w, h) * 0.78f
            )

            onDrawBehind {
                val wall = Brush.verticalGradient(listOf(wallTop, wallBottom), startY = 0f, endY = floorTop)
                drawRect(brush = wall)

                var sx = 0f
                var idx = 0
                while (sx < w) {
                    if (idx % 2 == 0) {
                        drawRect(color = stripe, topLeft = Offset(sx, 0f), size = Size(stripeW, floorTop))
                    }
                    sx += stripeW
                    idx++
                }

                speckles.forEach { (x, y, r) ->
                    drawCircle(color = speckA, radius = r, center = Offset(x, y))
                }

                bundles.forEach { (bx, depth, leafColor) ->
                    drawLine(
                        color = woodDark,
                        start = Offset(bx, 0f),
                        end = Offset(bx, depth * 0.35f),
                        strokeWidth = 3f
                    )
                    val stemTop = depth * 0.35f
                    val stemBottom = depth
                    drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(bx, stemTop),
                        end = Offset(bx, stemBottom),
                        strokeWidth = 3f
                    )
                    val leafCount = 6
                    for (li in 0 until leafCount) {
                        val t = li / (leafCount - 1f)
                        val ly = stemTop + (stemBottom - stemTop) * t
                        val side = if (li % 2 == 0) -1 else 1
                        drawPaintedLeaf(
                            center = Offset(bx + side * depth * 0.14f, ly),
                            length = depth * 0.34f,
                            width = depth * 0.11f,
                            color = leafColor,
                            rotationDeg = side * -35f
                        )
                    }
                    drawPaintedLeaf(
                        center = Offset(bx, stemBottom + depth * 0.10f),
                        length = depth * 0.30f,
                        width = depth * 0.10f,
                        color = leafColor.cozyDarken(0.1f),
                        rotationDeg = 90f
                    )
                }

                if ("luces_hadas" in decorations) {
                    drawStringLights(
                        yBase = h * 0.19f,
                        sag = h * 0.035f,
                        count = 9,
                        wire = if (dark) Color(0xFF3E2723) else Color(0xFF5D4037),
                        glow = if (dark) Color(0xFFFFE082).copy(alpha = 0.35f) else Color(0xFFFFE082).copy(alpha = 0.30f),
                        bulb = if (dark) Color(0xFFFFF3C4) else Color(0xFFFFE082)
                    )
                }

                val shelfH = h * 0.022f
                drawRect(
                    brush = Brush.verticalGradient(listOf(woodLight, woodDark)),
                    topLeft = Offset(w * 0.04f, shelfY),
                    size = Size(w * 0.92f, shelfH)
                )
                drawRect(
                    color = woodDark.cozyDarken(0.25f),
                    topLeft = Offset(w * 0.04f, shelfY + shelfH - 3f),
                    size = Size(w * 0.92f, 3f)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.12f),
                    topLeft = Offset(w * 0.06f, shelfY + shelfH),
                    size = Size(w * 0.88f, h * 0.012f)
                )

                jars.forEach { (jx, jh, contentColor) ->
                    val jw = jh * 0.58f
                    val top = shelfY - jh
                    val glass = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(jx, top, jx + jw, shelfY),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(jw * 0.18f, jw * 0.18f)
                            )
                        )
                    }
                    drawPath(glass, color = Color.White.copy(alpha = 0.35f))
                    drawPath(glass, color = Color(0xFF546E7A).copy(alpha = 0.35f), style = Stroke(width = 2f))
                    val fillTop = top + jh * 0.35f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(contentColor.cozyLighten(0.15f), contentColor),
                            startY = fillTop,
                            endY = shelfY
                        ),
                        topLeft = Offset(jx + 3f, fillTop),
                        size = Size(jw - 6f, shelfY - fillTop - 3f),
                        cornerRadius = CornerRadius(jw * 0.12f, jw * 0.12f)
                    )
                    drawRoundRect(
                        color = woodDark,
                        topLeft = Offset(jx + jw * 0.18f, top - jh * 0.14f),
                        size = Size(jw * 0.64f, jh * 0.16f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(jx + 4f, top + 6f),
                        end = Offset(jx + 4f, shelfY - 6f),
                        strokeWidth = 2.5f
                    )
                }

                val plankH = (h - floorTop) / 6f
                for (i in 0 until 6) {
                    val py = floorTop + i * plankH
                    val c = if (i % 2 == 0) floorA else floorB
                    drawRect(color = c, topLeft = Offset(0f, py), size = Size(w, plankH))
                    drawLine(
                        color = floorLine.copy(alpha = 0.5f),
                        start = Offset(0f, py),
                        end = Offset(w, py),
                        strokeWidth = 2f
                    )
                    val seamX = if (i % 2 == 0) w * 0.33f else w * 0.66f
                    drawLine(
                        color = floorLine.copy(alpha = 0.45f),
                        start = Offset(seamX, py),
                        end = Offset(seamX, py + plankH),
                        strokeWidth = 2f
                    )
                    val grainRnd = Random(1000 + i)
                    repeat(4) {
                        val gy = py + plankH * grainRnd.nextFloat()
                        val gx = w * grainRnd.nextFloat()
                        val gl = w * (0.05f + grainRnd.nextFloat() * 0.1f)
                        drawLine(
                            color = floorLine.copy(alpha = 0.2f),
                            start = Offset(gx, gy),
                            end = Offset(gx + gl, gy),
                            strokeWidth = 1.2f
                        )
                    }
                }

                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent),
                        startY = floorTop - h * 0.04f,
                        endY = floorTop + h * 0.03f
                    ),
                    topLeft = Offset(0f, floorTop - h * 0.04f),
                    size = Size(w, h * 0.07f)
                )

                if (decorations.isNotEmpty()) {
                    drawShopFloorDecorations(
                        decorations = decorations,
                        left = 0f,
                        right = w,
                        top = floorTop + h * 0.01f,
                        bottom = h * 0.965f,
                        dark = dark
                    )
                }

                drawRect(brush = vignette)
            }
        }
}

/**
 * Mini-escenario para previsualizar en la Tienda las decoraciones desbloqueadas
 * (mismo dibujo que el menú, en versión compacta).
 */
@Composable
fun Modifier.shopDecorationsPreview(decorations: Set<String>): Modifier {
    val dark = isSystemInDarkTheme()
    val wallTop = if (dark) Color(0xFF232A26) else Color(0xFFEFE6D4)
    val wallBottom = if (dark) Color(0xFF1A1F1C) else Color(0xFFE2D3B8)
    val stripe = if (dark) Color.White.copy(alpha = 0.025f) else Color(0x14000000)
    val floorA = if (dark) Color(0xFF3A2E26) else Color(0xFFB08968)
    val floorB = if (dark) Color(0xFF2E241E) else Color(0xFF9C7550)
    val floorLine = floorB.cozyDarken(0.30f)

    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val floorTop = h * 0.48f
        val stripeW = w / 7f

        onDrawBehind {
            drawRect(
                brush = Brush.verticalGradient(listOf(wallTop, wallBottom), startY = 0f, endY = floorTop),
                topLeft = Offset(0f, 0f),
                size = Size(w, floorTop)
            )

            var sx = 0f
            var idx = 0
            while (sx < w) {
                if (idx % 2 == 0) {
                    drawRect(color = stripe, topLeft = Offset(sx, 0f), size = Size(stripeW, floorTop))
                }
                sx += stripeW
                idx++
            }

            if ("luces_hadas" in decorations) {
                drawStringLights(
                    yBase = h * 0.14f,
                    sag = h * 0.06f,
                    count = 7,
                    wire = if (dark) Color(0xFF3E2723) else Color(0xFF5D4037),
                    glow = if (dark) Color(0xFFFFE082).copy(alpha = 0.35f) else Color(0xFFFFE082).copy(alpha = 0.30f),
                    bulb = if (dark) Color(0xFFFFF3C4) else Color(0xFFFFE082)
                )
            }

            val plankH = (h - floorTop) / 3f
            for (i in 0 until 3) {
                val py = floorTop + i * plankH
                drawRect(color = if (i % 2 == 0) floorA else floorB, topLeft = Offset(0f, py), size = Size(w, plankH))
                drawLine(
                    color = floorLine.copy(alpha = 0.5f),
                    start = Offset(0f, py),
                    end = Offset(w, py),
                    strokeWidth = 2f
                )
            }
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent),
                    startY = floorTop - h * 0.05f,
                    endY = floorTop + h * 0.04f
                ),
                topLeft = Offset(0f, floorTop - h * 0.05f),
                size = Size(w, h * 0.09f)
            )

            drawShopFloorDecorations(
                decorations = decorations,
                left = 0f,
                right = w,
                top = floorTop + h * 0.02f,
                bottom = h * 0.97f,
                dark = dark
            )
        }
    }
}

/** Brillo especular animado opcional para gotas/partículas. */
fun DrawScope.drawSoftGlow(center: Offset, radius: Float, color: Color) {
    val brush = Brush.radialGradient(
        colors = listOf(color, color.copy(alpha = 0f)),
        center = center,
        radius = radius
    )
    drawCircle(brush = brush, radius = radius, center = center)
}

internal fun DrawScope.wobbleOffset(phase: Float, amplitude: Float): Offset =
    Offset(x = sin(phase * Math.PI * 2.0).toFloat() * amplitude, y = cos(phase * Math.PI * 2.0).toFloat() * amplitude)

