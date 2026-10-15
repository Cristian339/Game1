package com.cozy.herbshop.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.rotate
import com.cozy.herbshop.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.model.CustomerOrder
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.HelpItem
import com.cozy.herbshop.game.HerbUnlocks
import com.cozy.herbshop.game.LevelRepository
import com.cozy.herbshop.ui.components.CozyJarItem
import com.cozy.herbshop.ui.components.TutorialOverlay
import com.cozy.herbshop.ui.components.TutorialStep
import com.cozy.herbshop.ui.components.cozyBackground
import com.cozy.herbshop.ui.components.GameScene

import com.cozy.herbshop.ui.components.SceneArtwork
import com.cozy.herbshop.ui.components.GeneratedArt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.cos
import kotlin.math.sin

private const val POUR_ANGLE_DEG = 42f

/**
 * Instantanea de la victoria observada por la UI. Es un data class para que
 * [snapshotFlow] emita solo cuando alguno de estos cuatro datos cambia de verdad.
 */
private data class VictoryDecision(
    val completed: Boolean,
    val handled: Boolean,
    val celebrate: Boolean,
    val newHerb: Boolean
)

@Composable
private fun OrderHerbRow(label: String, herbs: List<com.cozy.herbshop.data.model.Herb>, dotSize: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        herbs.forEach { herb ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(dotSize)
                    .background(color = herb.defaultColor, shape = CircleShape)
            )
        }
    }
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    levelNumber: Int,
    onBackClicked: () -> Unit,
    onLevelCompleted: () -> Unit,
    onWatchAdForHelp: (HelpItem) -> Unit = {},
    onWatchAdForRestart: () -> Unit = {},
    asmrAmbientName: String? = null,
    asmrEnabled: Boolean = false,
    onToggleAsmr: () -> Unit = {},
    onOpenShop: () -> Unit = {},
    musicInGameEnabled: Boolean = true,
    onToggleMusic: () -> Unit = {},
    onSetMusic: (Boolean) -> Unit = {},
    onSetAsmr: (Boolean) -> Unit = {},
    asmrChoices: List<AsmrChoice> = emptyList(),
    selectedAsmrPack: String? = null,
    onSelectAsmr: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentAlpha = remember { Animatable(1f) }

    // Recuadro de logro: antes de pasar a la pantalla de victoria se celebra aquí
    // y el jugador pulsa "Continuar".
    var showVictoryCard by remember { mutableStateOf(false) }

    // La ficha del pedido ya no vive en la partida: antes ocupaba media pantalla
    // arriba y empujaba los frascos, que ademas se podian arrastrar. Ahora sale
    // una vez al entrar y despues se abre con el boton flotante.
    var showOrderSheet by remember(levelNumber) { mutableStateOf(true) }

    // Solo hay algo que mostrar si el nivel trae pedido: una puerta, el reto del
    // dia o un cliente con receta. En los niveles sin receta no sale ni boton ni
    // ventana, porque no hay nada que mirar.
    val hayPedido = uiState.customerOrder != null || uiState.isGateLevel || uiState.isDailyChallenge
    val tutorialEncima = uiState.showBasicTutorial || uiState.showRecipeTutorial
    // Misma regla que usa el mapa y la partida al ganar la puerta, para que la
    // ventana y el herbario no se contradigan.
    val herbarioPuertas = remember { HerbUnlocks.herbByGate(LevelRepository.GATE_LEVELS) }
    val premioPuerta = herbarioPuertas[uiState.levelNumber]

    BackHandler {
        if (showOrderSheet) {
            showOrderSheet = false
        } else if (showVictoryCard) {
            showVictoryCard = false
            onLevelCompleted()
        } else {
            viewModel.requestExitConfirmation()
        }
    }

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
        // La ventana intermedia solo sale si hay algo que celebrar: una hierba
        // nueva o una receta servida. Si no, se pasa directo al resumen.
        snapshotFlow {
            VictoryDecision(
                completed = uiState.isLevelCompleted,
                handled = uiState.completionHandled,
                celebrate = uiState.hasCelebration,
                newHerb = uiState.newlyUnlockedHerb != null
            )
        }
            .collect { decision ->
                if (!decision.completed) {
                    showVictoryCard = false
                    return@collect
                }
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
                } else if (!decision.handled) {
                    // Los premios (hierba nueva, receta) todavia no estan
                    // decidedos: se espera en silencio a que el ViewModel termine.
                } else if (decision.celebrate || decision.newHerb) {
                    showVictoryCard = true
                } else {
                    onLevelCompleted()
                }
            }
    }

    val jarRootBounds = remember { mutableStateMapOf<Int, Rect>() }
    var overlayRootBounds by remember { mutableStateOf(Rect.Zero) }

    val sceneForMode = remember(uiState.isDailyChallenge, uiState.isZenMode, uiState.levelNumber) {
        GameScene.forMode(uiState.levelNumber, uiState.isDailyChallenge, uiState.isZenMode)
    }
    val scene = sceneForMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFF3E9D8))
    ) {
        // Arte de fondo generado: queda tras el escenario procedural para que la
        // transición de tema y los detalles vectorsiles sigan funcionando.
        SceneArtwork(
            scene = scene,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha.value
                }
        ) {
            // Barra superior sobre una capsula translucida: el titulo, el boton de
            // volver y el contador de movimientos se leen siempre sea cual sea el
            // color del escenario de fondo.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xD9FFF6E6))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    AudioHub.sound.playUiBack()
                    viewModel.requestExitConfirmation()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFF3E2F22)
                    )
                }

                Text(
                    text = if (uiState.isDailyChallenge) "📅 Diario" else if (uiState.isZenMode) "🧘 Zen" else "Nivel ${uiState.levelNumber}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2F22),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // Boton del pedido. Va en la barra y no flotando por encima porque
            // encima esta la tetera y se tapaban.
            if (hayPedido && !showVictoryCard) {
                OrderSheetButton(
                    isGate = uiState.isGateLevel && !uiState.isDailyChallenge,
                    isDaily = uiState.isDailyChallenge,
                    onClick = {
                        AudioHub.sound.playUiClick()
                        showOrderSheet = true
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Text(
                    text = if (uiState.movesLeft != null) {
                        "🔄 ${uiState.movesCount}/${uiState.moveLimit}"
                    } else {
                        "🔄 ${uiState.movesCount}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (uiState.atMoveLimit) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (uiState.atMoveLimit) {
                        Color(0xFF9E2B25)
                    } else {
                        Color(0xFF3E2F22)
                    },
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Aviso del sector y del par. El limite nunca hace perder: solo
            // avisa de que se acabaron las jugadas de la partida limpia.
            LevelNudgeBar(
                movesCount = uiState.movesCount,
                parMoves = uiState.parMoves,
                movesLeft = uiState.movesLeft,
                atLimit = uiState.atMoveLimit
            )

            val teapotJar = remember(uiState.jars) { uiState.jars.firstOrNull { it.isTeapot } }
            val hasTeapot = teapotJar != null && uiState.customerOrder != null

            if (hasTeapot) {
                RecipeProgressCard(
                    requiredHerbs = uiState.customerOrder?.requiredHerbs.orEmpty(),
                    pouredHerbs = teapotJar?.stack.orEmpty(),
                    nextHerb = uiState.nextRecipeHerb,
                    rewardLeaves = uiState.recipeRewardNext,
                    isBrewed = uiState.teapotBrewed,
                    isRuined = uiState.teapotRuined
                )
            }

            AnimatedVisibility(
                visible = uiState.showRecipeCelebration && !uiState.isLevelCompleted,
                enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(200))
            ) {
                RecipeCelebrationBanner(
                    recipeName = uiState.customerOrder?.recipeName.orEmpty(),
                    rewardLeaves = uiState.recipeRewardNext,
                    onDismiss = { viewModel.dismissRecipeCelebration() }
                )
            }

            uiState.foundSecret?.let { secreta ->
                SecretFoundBanner(
                    herb = secreta,
                    onDismiss = { viewModel.dismissSecretFound() }
                )
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

            if (uiState.pendingActions.isNotEmpty()) {
                Text(
                    text = "▶ ${uiState.pendingActions.size} movimiento(s) en cola: se aplican en cuanto termine el vertido",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            val gridColumns = if (uiState.jars.size > 6) 4 else 3
            val orderJarIndex = remember(uiState.jars, uiState.customerOrder) {
                uiState.customerOrder?.let { order ->
                    uiState.jars.indexOfFirst { jar ->
                        jar.layers.size == order.requiredHerbs.size &&
                            jar.layers.map { it.herb } == order.requiredHerbs
                    }.takeIf { it >= 0 }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                // Rejilla fija, no perezosa. Con la perezosa los frascos se podian
                // arrastrar al tocar el cristal, lo que movia el tablero bajo el
                // dedo sin querer. Aqui no hay nada que desplazar: como mucho
                // caben tres filas (11 frascos en cuatro columnas).
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    uiState.jars.chunked(gridColumns).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            fila.forEach { jar ->
                                val index = uiState.jars.indexOf(jar)
                                val pour = uiState.animatingPour
                                val isSource = pour != null && pour.sourceIndex == index
                                val isTarget = pour != null && pour.targetIndex == index

                                val srcBounds = pour?.let { jarRootBounds[it.sourceIndex] }
                                val dstBounds = pour?.let { jarRootBounds[it.targetIndex] }
                                val angle = if (isSource && srcBounds != null && dstBounds != null) {
                                    if (dstBounds.left >= srcBounds.left) POUR_ANGLE_DEG else -POUR_ANGLE_DEG
                                } else 0f

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    CozyJarItem(
                                        jar = jar,
                                        isSelected = uiState.selectedJarIndex == index && uiState.animatingPour == null,
                                        isPourSource = isSource,
                                        isPourTarget = isTarget,
                                        isRecipeBrewed = uiState.teapotBrewed && jar.isTeapot,
                                        queuePosition = uiState.pendingActions.indexOf(index).takeIf { it >= 0 }?.plus(1) ?: 0,
                                        pourAngle = angle,
                                        onClick = { viewModel.onJarClicked(index) },
                                        onPositioned = { rect -> jarRootBounds[index] = rect },
                                        modifier = Modifier
                                            .then(
                                                if (isTarget) Modifier.background(Color.Transparent) else Modifier
                                            )
                                    )
                                    if (orderJarIndex == index) {
                                        Text(
                                            text = "✅",
                                            modifier = Modifier.padding(top = 18.dp, end = 4.dp)
                                        )
                                    }
                                }
                            }
                            // Rellena la ultima fila para que los frascos de la
                            // izquierda no queden pegados al borde.
                            repeat(gridColumns - fila.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
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

            // ============ Barra inferior: un solo boton despliega ayudas y audio ============
            var helpPanelOpen by remember { mutableStateOf(false) }
            var audioPanelOpen by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // --- Panel de ayudas (se despliega hacia abajo desde el boton) ---
                    AnimatedVisibility(
                        visible = helpPanelOpen,
                        enter = fadeIn() + expandVertically(animationSpec = tween(220)),
                        exit = fadeOut() + shrinkVertically(animationSpec = tween(180))
                    ) {
                        HelpOptionRow(
                            undoTokens = uiState.undoTokens,
                            hintStock = uiState.helpStocks[HelpItem.HINT.id] ?: 0,
                            extraJarStock = uiState.helpStocks[HelpItem.EXTRA_JAR.id] ?: 0,
                            enabled = uiState.animatingPour == null,
                            onUndo = {
                                AudioHub.sound.playUiClick()
                                viewModel.undoMove()
                            },
                            onHint = {
                                AudioHub.sound.playUiClick()
                                viewModel.requestHint()
                            },
                            onExtraJar = {
                                AudioHub.sound.playUiClick()
                                viewModel.addExtraJar()
                            }
                        )
                    }

                    // --- Panel de audio ---
                    AnimatedVisibility(
                        visible = audioPanelOpen,
                        enter = fadeIn() + expandVertically(animationSpec = tween(220)),
                        exit = fadeOut() + shrinkVertically(animationSpec = tween(180))
                    ) {
                        AudioOptionRow(
                            musicOn = musicInGameEnabled,
                            asmrOn = asmrEnabled,
                            selectedAsmrId = selectedAsmrPack,
                            choices = asmrChoices,
                            onMusic = {
                                AudioHub.sound.playUiToggle()
                                onToggleMusic()
                            },
                            onAsmr = {
                                AudioHub.sound.playUiToggle()
                                onToggleAsmr()
                            },
                            onSelect = { packId ->
                                AudioHub.sound.playUiToggle()
                                onSelectAsmr(packId)
                            },
                            onShop = {
                                AudioHub.sound.playUiClick()
                                onOpenShop()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpandToggleButton(
                            label = "Ayudas",
                            iconRes = R.drawable.token_undo,
                            expanded = helpPanelOpen,
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                AudioHub.sound.playUiToggle()
                                helpPanelOpen = !helpPanelOpen
                                if (helpPanelOpen) audioPanelOpen = false
                            },
                            modifier = Modifier.weight(1.3f)
                        )
                        ExpandToggleButton(
                            label = "Audio",
                            iconRes = R.drawable.icon_hint,
                            expanded = audioPanelOpen,
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = {
                                AudioHub.sound.playUiToggle()
                                audioPanelOpen = !audioPanelOpen
                                if (audioPanelOpen) helpPanelOpen = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                AudioHub.sound.playUiClick()
                                viewModel.restartLevel { onWatchAdForRestart() }
                            },
                            enabled = uiState.animatingPour == null,
                            modifier = Modifier.weight(1.1f).height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Reiniciar",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
        }

        // Boton del pedido y su ventana. La ficha ya no vive en la partida: antes ocupaba
        // media pantalla arriba y empujaba los frascos, que ademas se podian
        // arrastrar. Ahora sale una vez al entrar y se recupera desde la barra.
        if (showOrderSheet && hayPedido && !tutorialEncima && !showVictoryCard) {
            OrderSheetDialog(
                order = uiState.customerOrder,
                isGate = uiState.isGateLevel && !uiState.isDailyChallenge,
                isDaily = uiState.isDailyChallenge,
                unlockedHerb = premioPuerta,
                onDismiss = {
                    AudioHub.sound.playUiBack()
                    showOrderSheet = false
                }
            )
        }

        if (uiState.showBasicTutorial) {
            TutorialOverlay(
                steps = listOf(
                    TutorialStep(
                        emoji = "👆",
                        title = "Toca un frasco",
                        body = "Selecciona el frasco que quieras mover. Se iluminará al elegirlo."
                    ),
                    TutorialStep(
                        emoji = "💧",
                        title = "Verte en otro frasco",
                        body = "Toca otro frasco: si la hierba de arriba coincide, el contenido se vierte. Si no, no pasa nada."
                    ),
                    TutorialStep(
                        emoji = "🫙",
                        title = "Ordena las hierbas",
                        body = "Rellena un frasco entero con una sola hierba para completarlo. Ordena todos y ¡nivel superado!"
                    )
                ),
                onSkip = { viewModel.dismissTutorial() },
                onFinished = { viewModel.dismissTutorial() }
            )
        } else if (uiState.showRecipeTutorial) {
            TutorialOverlay(
                steps = listOf(
                    TutorialStep(
                        emoji = "📜",
                        title = "Receta obligatoria",
                        body = "Esta es una puerta de hierba: solo superas el nivel preparando la receta del cliente."
                    ),
                    TutorialStep(
                        emoji = "🫖",
                        title = "Sirve la receta en la Tetera",
                        body = "La tetera es el único recipiente donde se mezcla y acepta cualquier hierba. Toca el frasco y luego la tetera: se vierte una unidad cada vez, en orden. Si te equivocas, la mezcla ya no vale y toca usar ↩️ Deshacer."
                    ),
                    TutorialStep(
                        emoji = "💨",
                        title = "Al servir, sella y brilla",
                        body = "Cuando la mezcla esté completa la tetera se sella y emite vapor. El resto de frascos solo necesita quedar ordenado."
                    ),
                    TutorialStep(
                        emoji = "🎁",
                        title = "Gana la hierba nueva",
                        body = "Al completar la receta recibirás +50 🍃 y una hierba nueva para tu herbario."
                    )
                ),
                onSkip = { viewModel.dismissTutorial() },
                onFinished = { viewModel.dismissTutorial() }
            )
        }

        val depletedHelp = uiState.depletedHelp?.let { HelpItem.fromId(it) }
        if (depletedHelp != null) {
            HelpDepletedDialog(
                help = depletedHelp,
                onWatchAd = {
                    AudioHub.sound.playUiClick()
                    viewModel.dismissDepletedHelp()
                    onWatchAdForHelp(depletedHelp)
                },
                onBuy = { viewModel.replenishDepletedHelp(fromAd = false) },
                onDismiss = { viewModel.dismissDepletedHelp() }
            )
        }

        if (uiState.showExitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelExit() },
                title = {
                    Text("🚪 ¿Salir al Menú Principal?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Si sales ahora, perderás el progreso de este nivel actual.")
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.cancelExit()
                        onBackClicked()
                    }) {
                        Text("Salir")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.cancelExit() }) {
                        Text("Continuar Jugando")
                    }
                }
            )
        }

        // Recuadro de logro: cubre TODA la pantalla y queda por encima de
        // todo (incluida la barra de ayudas), asi que no se puede seguir
        // jugando ni tocar botones hasta pulsar Continuar.
        AnimatedVisibility(
            visible = showVictoryCard,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(160))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    // Consume los toques: nada de lo que hay debajo recibe pulsaciones.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                contentAlignment = Alignment.Center
            ) {
                VictoryCard(
                    isDailyChallenge = uiState.isDailyChallenge,
                    orderFulfilled = uiState.orderFulfilled,
                    recipeName = uiState.customerOrder?.recipeName,
                    baseReward = uiState.rewardBase,
                    recipeReward = uiState.rewardRecipe,
                    unlockedHerb = uiState.newlyUnlockedHerb,
                    onContinue = {
                        showVictoryCard = false
                        onLevelCompleted()
                    }
                )
            }
        }
    }
}

/**
 * Aviso de ayuda agotada. Siempre da las dos salidas: un anuncio por un uso
 * suelto, o un pack con Hojas de Oro. Asi el jugador nunca se queda parado
 * creyendo que el boton no hace nada.
 */
@Composable
private fun HelpDepletedDialog(
    help: HelpItem,
    onWatchAd: () -> Unit,
    onBuy: () -> Unit,
    onDismiss: () -> Unit
) {
    val (emoji, artRes) = when (help) {
        HelpItem.UNDO -> "↩️" to R.drawable.token_undo
        HelpItem.HINT -> "💡" to R.drawable.icon_hint
        HelpItem.EXTRA_JAR -> "🫙" to R.drawable.icon_extra_jar
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(artRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )
        },
        title = {
            Text(
                text = "$emoji Te has quedado sin ${help.title}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Puedes ver un anuncio para usarlo ahora mismo, o comprar un pack con tus Hojas de Oro.")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onWatchAd, modifier = Modifier.weight(1f)) {
                        Text("Ver anuncio", maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(onClick = onBuy, modifier = Modifier.weight(1f)) {
                        Text("${help.packPrice} 🍃", maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "El pack trae ${help.packSize} usos de ${help.title.lowercase()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Ahora no") }
        }
    )
}

/** Panel de celebración con el resumen de lo conseguido en la partida. */
@Composable
private fun VictoryCard(
    isDailyChallenge: Boolean,
    orderFulfilled: Boolean,
    recipeName: String?,
    baseReward: Int,
    recipeReward: Int,
    unlockedHerb: com.cozy.herbshop.data.model.Herb?,
    onContinue: () -> Unit
) {
    val pop = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    val total = baseReward + recipeReward

    Card(
        modifier = Modifier
            .padding(24.dp)
            .scale(pop.value),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "\uD83C\uDFC6", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "¡Lo lograste!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isDailyChallenge) "Reto del día superado" else "Nivel completado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (orderFulfilled && recipeName != null) {
                SummaryRow(text = "📜 Receta «$recipeName» servida", value = if (recipeReward > 0) "+$recipeReward 🍃" else "ya cobrada")
            }
            if (baseReward > 0) {
                SummaryRow(text = "🎯 Nivel superado", value = "+$baseReward 🍃")
            }
            if (total == 0) {
                SummaryRow(text = "🍃 Sin hojas extra (ya completado antes)", value = "")
            }
            if (isDailyChallenge) {
                SummaryRow(text = "📅 Racha del día", value = "+3 🌸")
            }
            unlockedHerb?.let { herb ->
                SummaryRow(text = "🌿 Nueva hierba: ${herb.displayName}", value = herb.symbol)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    AudioHub.sound.playUiConfirm()
                    onContinue()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Continuar ▶",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(text: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Barra de progreso de la Tetera: cuántos "\u25cf" de la receta lleva, cu\u00e1nto falta,
 * qu\u00e9 hierba toca ahora y cu\u00e1nto paga al terminar el nivel.
 */
@Composable
private fun RecipeProgressCard(
    requiredHerbs: List<com.cozy.herbshop.data.model.Herb>,
    pouredHerbs: List<com.cozy.herbshop.data.model.Herb>,
    nextHerb: com.cozy.herbshop.data.model.Herb?,
    rewardLeaves: Int,
    isBrewed: Boolean,
    isRuined: Boolean
) {
    val pulse by rememberInfiniteTransition(label = "recipePulse").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recipePulseValue"
    )

    val containerColor = when {
        isRuined -> MaterialTheme.colorScheme.errorContainer
        isBrewed -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val onContainer = when {
        isRuined -> MaterialTheme.colorScheme.onErrorContainer
        isBrewed -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (isBrewed) "\ud83e\udd6a" else "\ud83e\udde6", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isBrewed -> "\u00a1Receta servida!"
                        isRuined -> "Mezcla perdida"
                        else -> "Tetera: ${pouredHerbs.size}/${requiredHerbs.size}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onContainer,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.weight(1f))
                if (rewardLeaves > 0) {
                    Text(
                        text = "+$rewardLeaves \ud83c\udf43",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            if (!isBrewed) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    requiredHerbs.forEachIndexed { index, herb ->
                        val poured = pouredHerbs.getOrNull(index)
                        val isNext = index == pouredHerbs.size && !isRuined
                        val dotSize = if (isNext) (20 * pulse).dp else 18.dp
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(dotSize)
                                .background(
                                    color = when {
                                        poured != null -> herb.defaultColor
                                        isNext -> herb.defaultColor.copy(alpha = 0.30f)
                                        else -> herb.defaultColor.copy(alpha = 0.12f)
                                    },
                                    shape = CircleShape
                                )
                                .then(
                                    if (isNext) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = herb.defaultColor,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        isRuined -> "\u26a0\ufe0f Ya no sigue la receta: usa \u21a9\ufe0f Deshacer o Reinicia."
                        nextHerb != null -> "Ahora toca: ${nextHerb.displayName} (${nextHerb.symbol})"
                        else -> "Vierte las hierbas en la tetera"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Aviso de hierba secreta encontrada. Se va solo a los pocos segundos y deja la
 * ficha en el herbario, que es donde queda el recuerdo.
 */
@Composable
private fun SecretFoundBanner(herb: Herb, onDismiss: () -> Unit) {
    LaunchedEffect(herb.id) {
        delay(3400)
        onDismiss()
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = herb.symbol, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¡Hierba secreta encontrada!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${herb.displayName} ya está en tu herbario",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/** Aviso grande al servir la receta en la tetera. */
@Composable
private fun RecipeCelebrationBanner(
    recipeName: String,
    rewardLeaves: Int,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2600)
        onDismiss()
    }

    val glow by rememberInfiniteTransition(label = "celebrationGlow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrationGlowValue"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f * glow),
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "\ud83c\udf6f", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\u00a1\u00abReceta servida!\u00bb",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (recipeName.isNotEmpty()) {
                    Text(
                        text = recipeName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (rewardLeaves > 0) {
                Text(
                    text = "+$rewardLeaves \ud83c\udf43",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Linea de arriba del tablero con el par y el limite de jugadas del sector.
 * El par sale de la solucion con la que el generador comprobo el nivel, asi que
 * no es un numero inventado: es lo que cuesta el nivel bien hecho.
 */
@Composable
private fun LevelNudgeBar(
    movesCount: Int,
    parMoves: Int?,
    movesLeft: Int?,
    atLimit: Boolean
) {
    val texto = when {
        atLimit -> "Sin jugadas: usa ↩️ Deshacer para enmendar la última"
        movesLeft != null -> "Te quedan $movesLeft jugadas · par $parMoves"
        parMoves != null && movesCount <= parMoves -> "Vas en ${movesCount}/$parMoves · ¡en par!"
        parMoves != null -> "Jugadas $movesCount · par $parMoves"
        else -> return
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = when {
            atLimit -> Color(0xFFF7DDD8)
            movesLeft != null -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color(0xFFE6F0E0)
        }
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (atLimit) Color(0xFF8E2A22) else Color(0xFF33402F),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Pastilla flotante con el pedido del cliente. Antes era una ficha fija arriba que
 * ocupaba media pantalla y empujaba los frascos; asi solo aparece si hay algo
 * que mirar y se recupera con un toque.
 */
@Composable
private fun OrderSheetButton(
    isGate: Boolean,
    isDaily: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label, container, content) = when {
        isGate -> Quadruple("🌟", "Puerta", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        isDaily -> Quadruple("📅", "Reto", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> Quadruple("📜", "Pedido", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = container,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/** Ventana con el pedido: cliente, diálogo, receta y premio de la puerta. */
@Composable
private fun OrderSheetDialog(
    order: CustomerOrder?,
    isGate: Boolean,
    isDaily: Boolean,
    unlockedHerb: Herb?,
    onDismiss: () -> Unit
) {
    val (titulo, premio) = when {
        isGate -> "🌟 Puerta de Hierba" to "Superar este nivel da una hierba nueva"
        isDaily -> "📅 Reto del Día" to "+3 🌸 y +50 🍃 al completar"
        else -> "📜 Pedido del cliente" to null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                order?.let {
                    Text(
                        text = "👤 ${it.customerName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column {
                order?.let { pedido ->
                    Text(
                        text = "\"${pedido.dialogue}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${pedido.recipeName}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OrderHerbRow(
                        label = if (isGate) "Receta obligatoria, en orden: " else "Receta, en orden: ",
                        herbs = pedido.requiredHerbs,
                        dotSize = 16.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🫖 La tetera acepta cualquier hierba, pero de una en una y en orden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: Text(
                    text = "Ordena las hierbas para dejar cada frasco con una sola.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (unlockedHerb != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = unlockedHerb.symbol, style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ganas ${unlockedHerb.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = unlockedHerb.scientificName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                premio?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("¡Entendido!") }
        }
    )
}

/** Boton que se despliega: muestra su icono generado y gira al abrirse. */
@Composable
private fun ExpandToggleButton(
    label: String,
    iconRes: Int,
    expanded: Boolean,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "expandArrow"
    )
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (expanded) container else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (expanded) content else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.width(3.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation),
            tint = if (expanded) content else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/** Fila de ayudas con el arte generado: deshacer, pista y frasco extra. */
@Composable
private fun HelpOptionRow(
    undoTokens: Int,
    hintStock: Int,
    extraJarStock: Int,
    enabled: Boolean,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onExtraJar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HelpCard(
            R.drawable.token_undo, "Deshacer",
            stockLabel(undoTokens, "ficha", "fichas"), enabled, onUndo, Modifier.weight(1f)
        )
        HelpCard(
            R.drawable.icon_hint, "Pista",
            stockLabel(hintStock, "pista", "pistas"), enabled, onHint, Modifier.weight(1f)
        )
        HelpCard(
            R.drawable.icon_extra_jar, "+Frasco",
            stockLabel(extraJarStock, "frasco", "frascos"), enabled, onExtraJar, Modifier.weight(1f)
        )
    }
}

/** "3 fichas" / "sin fichas": el contador vacio avisa antes de pulsarlo. */
private fun stockLabel(count: Int, singular: String, plural: String): String = when {
    count <= 0 -> "sin $plural"
    count == 1 -> "1 $singular"
    else -> "$count $plural"
}

@Composable
private fun HelpCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit,
                alpha = if (enabled) 1f else 0.4f
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Panel de audio de la partida: musica y eleccion de ambiente. */
@Composable
private fun AudioOptionRow(
    musicOn: Boolean,
    asmrOn: Boolean,
    selectedAsmrId: String?,
    choices: List<AsmrChoice>,
    onMusic: () -> Unit,
    onAsmr: () -> Unit,
    onSelect: (String?) -> Unit,
    onShop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioCard(
                label = "Musica",
                state = if (musicOn) "sonando" else "silenciada",
                on = musicOn,
                container = MaterialTheme.colorScheme.primaryContainer,
                onClick = onMusic,
                modifier = Modifier.weight(0.85f)
            )
            // Antes esta tarjeta ponia "apagado" cuando habia ambiente y
            // "toca para apagar" cuando no, y solo llamaba a onSelect(null): no
            // habia forma de encenderla, solo de apagarla. Ahora es un
            // interruptor de verdad, enlazado al mismo estado que usa el juego al
            // entrar en un nivel.
            AudioCard(
                label = "Ambiente",
                state = when {
                    !asmrOn -> "apagado"
                    selectedAsmrId == null -> "sin pack: toca"
                    else -> "sonando: toca para parar"
                },
                on = asmrOn && selectedAsmrId != null,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = onAsmr,
                modifier = Modifier.weight(0.85f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (choices.isEmpty()) {
            Text(
                text = "Aun no tienes packs de ambiente ASMR. Toca aqui para verlos en la tienda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onShop() }
            )
        } else {
            Text(
                text = "Elige que ambiente suena:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { choice ->
                    val active = choice.id == selectedAsmrId
                    FilterChip(
                        selected = active,
                        onClick = { onSelect(if (active) null else choice.id) },
                        label = { Text(choice.label, fontSize = 11.sp, maxLines = 1) },
                        leadingIcon = if (active) {
                            { Text(text = "♪", fontSize = 11.sp) }
                        } else null
                    )
                }
            }
        }
    }
}

class AsmrChoice(val id: String, val label: String)

@Composable
private fun AudioCard(
    label: String,
    state: String,
    on: Boolean,
    container: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (on) container else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (on) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    maxLines = 1
                )
                Text(
                    text = state,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
