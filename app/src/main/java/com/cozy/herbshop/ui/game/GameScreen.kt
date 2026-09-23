package com.cozy.herbshop.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.ui.components.CozyJarItem
import com.cozy.herbshop.ui.components.cozyBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.cos
import kotlin.math.sin

private const val POUR_ANGLE_DEG = 42f

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    levelNumber: Int,
    onBackClicked: () -> Unit,
    onLevelCompleted: () -> Unit,
    onWatchAdForHint: () -> Unit,
    onWatchAdForExtraJar: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentAlpha = remember { Animatable(1f) }

    LaunchedEffect(levelNumber) {
        if (levelNumber == 999) {
            viewModel.loadDailyChallenge()
        } else if (levelNumber == 0) {
            viewModel.loadZenLevel()
        } else {
            viewModel.loadLevel(levelNumber)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.isLevelCompleted }
            .collect { completed ->
                if (!completed) return@collect
                if (uiState.isZenMode) {
                    contentAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                    )
                    viewModel.loadNextZenLevel()
                    contentAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                    )
                } else {
                    onLevelCompleted()
                }
            }
    }

    val jarRootBounds = remember { mutableStateMapOf<Int, Rect>() }
    var overlayRootBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .cozyBackground()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha.value
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    AudioHub.sound.playUiBack()
                    onBackClicked()
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }

                Text(
                    text = if (uiState.isDailyChallenge) "📅 Desafío Diario" else if (uiState.isZenMode) "🧘 Modo Zen" else "Nivel ${uiState.levelNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Movs: ${uiState.movesCount}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isDailyChallenge) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌟 Reto del Día",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Premio: +3 🌸 | +50 🍃",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        uiState.customerOrder?.let { order ->
                            Text(
                                text = "Objetivo: Prepara \"${order.recipeName}\" para ${order.customerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Receta del reto: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                order.requiredHerbs.forEach { herb ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .size(16.dp)
                                            .background(
                                                color = herb.defaultColor,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        } ?: Text(
                            text = "Objetivo: Ordena todas las hierbas en frascos individuales.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                uiState.customerOrder?.let { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "👤 ${order.customerName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = order.recipeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${order.dialogue}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Receta pedida: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                order.requiredHerbs.forEach { herb ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .size(14.dp)
                                            .background(
                                                color = herb.defaultColor,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            uiState.hintMove?.let { (from, to) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "💡 Pista: Vierte del frasco ${from + 1} al frasco ${to + 1}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            val gridColumns = if (uiState.jars.size > 6) 4 else 3
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(uiState.jars) { index, jar ->
                        val pour = uiState.animatingPour
                        val isSource = pour != null && pour.sourceIndex == index
                        val isTarget = pour != null && pour.targetIndex == index

                        val srcBounds = pour?.let { jarRootBounds[it.sourceIndex] }
                        val dstBounds = pour?.let { jarRootBounds[it.targetIndex] }
                        val angle = if (isSource && srcBounds != null && dstBounds != null) {
                            if (dstBounds.left >= srcBounds.left) POUR_ANGLE_DEG else -POUR_ANGLE_DEG
                        } else 0f

                        CozyJarItem(
                            jar = jar,
                            isSelected = uiState.selectedJarIndex == index && uiState.animatingPour == null,
                            isPourSource = isSource,
                            isPourTarget = isTarget,
                            pourAngle = angle,
                            onClick = { viewModel.onJarClicked(index) },
                            onPositioned = { rect -> jarRootBounds[index] = rect },
                            modifier = Modifier
                                .then(
                                    if (isTarget) Modifier.background(Color.Transparent) else Modifier
                                )
                        )
                    }
                }

                uiState.animatingPour?.let { pour ->
                    val srcRoot = jarRootBounds[pour.sourceIndex]
                    val dstRoot = jarRootBounds[pour.targetIndex]

                    // Timeline real del vertido. Usamos Animatable (no animateFloatAsState)
                    // porque al entrar en composición ya arrancan en 0 y SÍ se animan:
                    //  130ms → el chorro empieza a salir y crece hasta 430ms
                    //  430ms → impacto en el destino (onda + salpicadura)
                    //  660ms → el chorro se desvanece
                    val streamExtent = remember { Animatable(0f) }
                    val streamAlpha = remember { Animatable(1f) }
                    val impactPulse = remember { Animatable(0f) }

                    LaunchedEffect(pour) {
                        launch {
                            streamExtent.animateTo(
                                1f,
                                tween(durationMillis = 300, delayMillis = 130, easing = LinearEasing)
                            )
                        }
                        launch {
                            delay(660)
                            streamAlpha.animateTo(0f, tween(durationMillis = 190, easing = LinearEasing))
                        }
                        launch {
                            delay(430)
                            impactPulse.animateTo(1f, tween(durationMillis = 340, easing = LinearEasing))
                        }
                    }

                    val infiniteTransition = rememberInfiniteTransition(label = "pourDrops")
                    val dropletPhase by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(460, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dropletPhase"
                    )

                    // Jitter fijo por vertido para que las gotas no viajen todas idénticas
                    val dropletJitter = remember(pour) {
                        List(5) { kotlin.random.Random.nextFloat() }
                    }

                    if (srcRoot != null && dstRoot != null && overlayRootBounds != Rect.Zero) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { coords ->
                                    val p = coords.positionInRoot()
                                    overlayRootBounds = Rect(
                                        p.x,
                                        p.y,
                                        p.x + coords.size.width,
                                        p.y + coords.size.height
                                    )
                                }
                        ) {
                            val src = Rect(
                                srcRoot.left - overlayRootBounds.left,
                                srcRoot.top - overlayRootBounds.top,
                                srcRoot.right - overlayRootBounds.left,
                                srcRoot.bottom - overlayRootBounds.top
                            )
                            val dst = Rect(
                                dstRoot.left - overlayRootBounds.left,
                                dstRoot.top - overlayRootBounds.top,
                                dstRoot.right - overlayRootBounds.left,
                                dstRoot.bottom - overlayRootBounds.top
                            )

                            val srcIsLeft = src.center.x <= dst.center.x
                            val herbColor = pour.herb.defaultColor
                            val pourDeg = if (srcIsLeft) POUR_ANGLE_DEG else -POUR_ANGLE_DEG

                            // Boca del frasco FUENTE: primero la posicionamos sin rotar
                            // y luego la rotamos alrededor del mismo pivote que usa
                            // CozyJarItem (transformOrigin), para que el chorro salga
                            // exactamente de la boca inclinada del frasco.
                            val mouthX0 = if (srcIsLeft) src.right - 10.dp.toPx() else src.left + 10.dp.toPx()
                            val mouthY0 = src.top + 18.dp.toPx()

                            val originXFrac = if (pourDeg > 0f) 0.62f else 0.38f
                            val pivot = Offset(
                                src.left + src.width * originXFrac,
                                src.top + src.height * 0.9f
                            )
                            val rad = Math.toRadians(pourDeg.toDouble())
                            val cosA = cos(rad).toFloat()
                            val sinA = sin(rad).toFloat()
                            val dx = mouthX0 - pivot.x
                            val dy = mouthY0 - pivot.y
                            val mouthX = pivot.x + dx * cosA - dy * sinA
                            val mouthY = pivot.y + dx * sinA + dy * cosA

                            // Boca del frasco destino
                            val targetX = dst.center.x
                            val targetY = dst.top + 20.dp.toPx()

                            // Punto de control del arco (por encima del medio)
                            val midX = (mouthX + targetX) / 2f
                            val arcHeight = max(36.dp.toPx(), kotlin.math.abs(targetX - mouthX) * 0.22f)
                            val controlY = min(mouthY, targetY) - arcHeight

                            val path = Path().apply {
                                moveTo(mouthX, mouthY)
                                quadraticBezierTo(midX, controlY, targetX, targetY)
                            }

                            // Calcular punto a lo largo de la curva para el chorro parcial
                            fun pointAt(t: Float): Offset {
                                val mt = 1f - t
                                val x = mt * mt * mouthX + 2f * mt * t * midX + t * t * targetX
                                val y = mt * mt * mouthY + 2f * mt * t * controlY + t * t * targetY
                                return Offset(x, y)
                            }

                            val extent = streamExtent.value.coerceIn(0f, 1f)
                            val alpha = streamAlpha.value.coerceIn(0f, 1f)

                            if (extent > 0.02f && alpha > 0.01f) {
                                val steps = 20
                                val pts = Array(steps + 1) { i -> pointAt(i / steps.toFloat()) }
                                val lastIdx = ((steps * extent).toInt()).coerceIn(1, steps)

                                // Sombra del chorro
                                val shadowPath = Path().apply {
                                    moveTo(mouthX, mouthY + 2.dp.toPx())
                                    for (i in 1..lastIdx) {
                                        val p = pts[i]
                                        lineTo(p.x, p.y + 2.dp.toPx())
                                    }
                                }
                                drawPath(
                                    path = shadowPath,
                                    color = Color.Black.copy(alpha = 0.18f * alpha),
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Chorro principal adelgazado: ancho de 12dp en la boca
                                // a 7dp al llegar al destino (más realista que un tubo uniforme)
                                for (i in 0 until lastIdx) {
                                    val t = i / steps.toFloat()
                                    drawLine(
                                        color = herbColor.copy(alpha = 0.92f * alpha),
                                        start = pts[i],
                                        end = pts[i + 1],
                                        strokeWidth = (12f - 5f * t).dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }

                                // Brillo interior del líquido (también adelgazado)
                                for (i in 0 until lastIdx) {
                                    val t = i / steps.toFloat()
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.35f * alpha),
                                        start = pts[i],
                                        end = pts[i + 1],
                                        strokeWidth = (3.2f - 1.6f * t).dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }

                                // Gotas cayendo por la curva con aceleración (gravedad)
                                if (extent > 0.3f) {
                                    repeat(5) { i ->
                                        val phase = (dropletPhase + i * 0.2f) % 1f
                                        val t = phase.pow(1.35f) * extent
                                        val p = pointAt(t)
                                        val jitterX = (dropletJitter[i] - 0.5f) * 10.dp.toPx()
                                        val dropR = (3.2f + i * 0.8f).dp.toPx()
                                        drawCircle(
                                            color = herbColor.copy(alpha = (0.8f - i * 0.1f) * alpha),
                                            radius = dropR,
                                            center = p + Offset(jitterX, 3f)
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.5f * alpha),
                                            radius = dropR * 0.45f,
                                            center = p + Offset(jitterX - 1.5f, 1f)
                                        )
                                    }
                                }

                                // Salpicadura en el punto de impacto
                                val splash = impactPulse.value
                                if (extent >= 0.95f && splash > 0f) {
                                    // Onda de impacto
                                    drawCircle(
                                        color = herbColor.copy(alpha = 0.35f * (1f - splash) * alpha),
                                        radius = (8f + 22f * splash).dp.toPx(),
                                        center = Offset(targetX, targetY),
                                        style = Stroke(width = 2.5.dp.toPx())
                                    )
                                    // Pequeñas gotas que rebotan
                                    val bounce = splash
                                    val angles = listOf(-2.4f, -1.57f, -0.7f)
                                    angles.forEachIndexed { i, ang ->
                                        val dist = (10f + 18f * bounce).dp.toPx()
                                        val bx = targetX + kotlin.math.cos(ang) * dist
                                        val by = targetY + kotlin.math.sin(ang) * dist * 0.7f
                                        drawCircle(
                                            color = herbColor.copy(alpha = (0.7f - i * 0.12f) * (1f - bounce * 0.5f) * alpha),
                                            radius = (3f - i * 0.5f).dp.toPx().coerceAtLeast(2f),
                                            center = Offset(bx, by)
                                        )
                                    }
                                    // Brillo fijo en la entrada del destino
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.55f * alpha),
                                        radius = 4.dp.toPx(),
                                        center = Offset(targetX, targetY)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { viewModel.undoMove() },
                    enabled = uiState.canUndo && uiState.animatingPour == null
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deshacer")
                }

                OutlinedButton(
                    onClick = {
                        AudioHub.sound.playUiClick()
                        onWatchAdForHint()
                    },
                    enabled = uiState.animatingPour == null
                ) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pista (Ad)")
                }

                OutlinedButton(
                    onClick = {
                        AudioHub.sound.playUiClick()
                        onWatchAdForExtraJar()
                    },
                    enabled = uiState.animatingPour == null
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+Frasco (Ad)")
                }
            }
        }
    }
}
