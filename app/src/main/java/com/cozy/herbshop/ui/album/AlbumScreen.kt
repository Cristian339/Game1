package com.cozy.herbshop.ui.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.model.AlbumPage
import com.cozy.herbshop.data.model.AlbumPages
import com.cozy.herbshop.data.model.AlbumSlot
import com.cozy.herbshop.data.model.JournalLetters
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.ui.components.cozyBackground
import com.cozy.herbshop.ui.herbarium.HerbDetailDialog
import kotlinx.coroutines.launch

/**
 * Album de la Tienda: cinco paginas, una por sector, con cuatro recuerdos cada
 * una. Sustituye la lista de cartas del antiguo diario por algo que se ve
 * llenarse mientras se juega.
 *
 * La pantalla no guarda progreso propio: lo deduce de [currentLevel] y de las
 * cartas ya recibidas. Asi no hay dos contadores que puedan desincronizarse.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumScreen(
    repository: UserPreferencesRepository,
    onBackClicked: () -> Unit
) {
    val currentLevelState by repository.currentLevelFlow.collectAsState(initial = null)
    val currentLevel = currentLevelState ?: 1
    val unlockedLetters by repository.customerLettersFlow.collectAsState(
        initial = JournalLetters.INICIALES
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { AlbumPages.ALL.size }
    )
    var selectedPage by rememberSaveable { mutableIntStateOf(0) }
    var jumpedToCurrentSector by rememberSaveable { mutableStateOf(false) }
    var selectedMemory by remember { mutableStateOf<AlbumMemory?>(null) }
    var lockedHint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Al entrar, abrimos el album por el sector en el que esta el jugador. Se
    // hace una sola vez para no arrastrarle la pagina si luego cambia de nivel.
    LaunchedEffect(currentLevelState) {
        val level = currentLevelState ?: return@LaunchedEffect
        if (!jumpedToCurrentSector) {
            val target = AlbumPages.pageIndexForLevel(level)
            pagerState.scrollToPage(target)
            selectedPage = target
            jumpedToCurrentSector = true
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedPage = pagerState.currentPage
    }

    val savedMemories = AlbumPages.totalProgress(currentLevel, unlockedLetters)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .cozyBackground()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        AlbumHeader(
            savedMemories = savedMemories,
            onBackClicked = onBackClicked
        )

        Spacer(modifier = Modifier.height(10.dp))

        AlbumPageTabs(
            selectedPage = selectedPage,
            currentLevel = currentLevel,
            onSelect = { index ->
                AudioHub.sound.playTab()
                selectedPage = index
                scope.launch { pagerState.animateScrollToPage(index) }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageSpacing = 10.dp
        ) { index ->
            val page = AlbumPages.ALL[index]
            AlbumPageCard(
                page = page,
                index = index,
                currentLevel = currentLevel,
                unlockedLetters = unlockedLetters,
                onOpenMemory = { selected ->
                    AudioHub.sound.playDialogOpen()
                    selectedMemory = selected
                },
                onLockedMemory = { slot ->
                    AudioHub.sound.playUiClick()
                    lockedHint = unlockHint(page, slot)
                }
            )
        }

        AlbumPageDots(
            selectedPage = selectedPage,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Toca un recuerdo para verlo · Desliza para cambiar de página",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }

    selectedMemory?.let { memory ->
        if (memory.slot == AlbumSlot.HERB) {
            HerbDetailDialog(
                herb = memory.page.herb,
                onDismiss = { selectedMemory = null }
            )
        } else {
            AlbumMemoryDialog(
                memory = memory,
                onDismiss = { selectedMemory = null }
            )
        }
    }

    lockedHint?.let { hint ->
        AlertDialog(
            onDismissRequest = { lockedHint = null },
            title = { Text("Aún no está en el álbum", fontWeight = FontWeight.Bold) },
            text = { Text(hint) },
            confirmButton = {
                Button(onClick = { lockedHint = null }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun AlbumHeader(
    savedMemories: Int,
    onBackClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            AudioHub.sound.playUiBack()
            onBackClicked()
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver"
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Álbum de la Tienda",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$savedMemories de ${AlbumPages.TOTAL_SLOTS} recuerdos guardados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "📖 $savedMemories/${AlbumPages.TOTAL_SLOTS}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AlbumPageTabs(
    selectedPage: Int,
    currentLevel: Int,
    onSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    // Al abrir el album por el sector actual, la pestaña elegida tiene que verse.
    LaunchedEffect(selectedPage) {
        if (selectedPage in AlbumPages.ALL.indices) {
            listState.animateScrollToItem(selectedPage)
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        itemsIndexed(AlbumPages.ALL) { index, page ->
            val selected = index == selectedPage
            val pageUnlocked = currentLevel >= page.startLevel
            val borderColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .border(1.dp, borderColor, RoundedCornerShape(50))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = page.emoji, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (!pageUnlocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Página bloqueada",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumPageCard(
    page: AlbumPage,
    index: Int,
    currentLevel: Int,
    unlockedLetters: Set<String>,
    onOpenMemory: (AlbumMemory) -> Unit,
    onLockedMemory: (AlbumSlot) -> Unit
) {
    val pageUnlocked = currentLevel >= page.startLevel
    val progress = AlbumPages.progress(page, currentLevel, unlockedLetters)
    val accent = page.herb.defaultColor

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f))
                            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = page.emoji, fontSize = 25.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = page.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "Pág. ${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Niveles ${page.levelRange}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (pageUnlocked) {
                            "${progress.unlockedCount}/4 recuerdos"
                        } else {
                            "Se abre en el nivel ${page.startLevel}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!pageUnlocked) {
                    LockedPageBody(
                        page = page,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AlbumSlotCard(
                                page = page,
                                slot = AlbumSlot.HERB,
                                progress = progress,
                                onOpenMemory = onOpenMemory,
                                onLockedMemory = onLockedMemory,
                                modifier = Modifier.weight(1f)
                            )
                            AlbumSlotCard(
                                page = page,
                                slot = AlbumSlot.CUSTOMER,
                                progress = progress,
                                onOpenMemory = onOpenMemory,
                                onLockedMemory = onLockedMemory,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AlbumSlotCard(
                                page = page,
                                slot = AlbumSlot.RECIPE,
                                progress = progress,
                                onOpenMemory = onOpenMemory,
                                onLockedMemory = onLockedMemory,
                                modifier = Modifier.weight(1f)
                            )
                            AlbumSlotCard(
                                page = page,
                                slot = AlbumSlot.STAMP,
                                progress = progress,
                                onOpenMemory = onOpenMemory,
                                onLockedMemory = onLockedMemory,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (progress.isComplete) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✓ Página completa",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress.unlockedCount / 4f)
                                        .clip(RoundedCornerShape(50))
                                        .background(accent)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${progress.unlockedCount}/4",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedPageBody(
    page: AlbumPage,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.32f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔒", fontSize = 34.sp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Una página espera su momento",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Se abre al llegar al nivel ${page.startLevel}. " +
                "Sigue el camino y vuelve a guardar este rincón.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AlbumSlotCard(
    page: AlbumPage,
    slot: AlbumSlot,
    progress: com.cozy.herbshop.data.model.AlbumPageProgress,
    onOpenMemory: (AlbumMemory) -> Unit,
    onLockedMemory: (AlbumSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val unlocked = progress.isUnlocked(slot)
    val accent = slotAccent(page, slot)
    val letter = JournalLetters.byId(page.customerLetterId)

    val label = when (slot) {
        AlbumSlot.HERB -> "HIERBA PRENSADA"
        AlbumSlot.RECIPE -> "RECETA"
        AlbumSlot.CUSTOMER -> "CLIENTE"
        AlbumSlot.STAMP -> "SELLO"
    }
    val contentEmoji = when (slot) {
        AlbumSlot.HERB -> page.herb.symbol
        AlbumSlot.RECIPE -> "🍵"
        AlbumSlot.CUSTOMER -> letter?.senderAvatar ?: "✉️"
        AlbumSlot.STAMP -> "🏅"
    }
    val title = when (slot) {
        AlbumSlot.HERB -> page.herb.displayName
        AlbumSlot.RECIPE -> page.recipe.name
        AlbumSlot.CUSTOMER -> letter?.senderName ?: "Cliente"
        AlbumSlot.STAMP -> page.stampName
    }
    val subtitle = when (slot) {
        AlbumSlot.HERB -> page.herb.scientificName
        AlbumSlot.RECIPE -> "Receta de la casa"
        AlbumSlot.CUSTOMER -> "Carta guardada"
        AlbumSlot.STAMP -> "Puerta ${page.gateLevel}"
    }
    val lockedSubtitle = when (slot) {
        AlbumSlot.HERB -> "Nivel ${page.startLevel}"
        AlbumSlot.RECIPE -> "Nivel ${page.recipeLevel}"
        AlbumSlot.CUSTOMER -> "Puerta ${page.gateLevel}"
        AlbumSlot.STAMP -> "Puerta ${page.gateLevel}"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (unlocked) {
                    accent.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                }
            )
            .border(
                width = 1.dp,
                color = if (unlocked) {
                    accent.copy(alpha = 0.42f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                if (unlocked) {
                    onOpenMemory(AlbumMemory(page, slot))
                } else {
                    onLockedMemory(slot)
                }
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = if (unlocked) {
                    accent.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))

            if (unlocked) {
                Text(text = contentEmoji, fontSize = 27.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Recuerdo bloqueado",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = if (unlocked) title else "Por descubrir",
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (unlocked) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = if (unlocked) subtitle else lockedSubtitle,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontStyle = if (unlocked && slot == AlbumSlot.HERB) FontStyle.Italic else FontStyle.Normal,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumPageDots(
    selectedPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumPages.ALL.forEachIndexed { index, _ ->
            val active = index == selectedPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(if (active) 18.dp else 6.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun AlbumMemoryDialog(
    memory: AlbumMemory,
    onDismiss: () -> Unit
) {
    val page = memory.page
    val letter = JournalLetters.byId(page.customerLetterId)
    val (emoji, title, subtitle) = when (memory.slot) {
        AlbumSlot.CUSTOMER -> Triple(
            letter?.senderAvatar ?: "✉️",
            letter?.senderName ?: "Cliente del camino",
            "Carta guardada · Puerta ${page.gateLevel}"
        )
        AlbumSlot.RECIPE -> Triple(
            "🍵",
            page.recipe.name,
            "Receta de la casa · ${page.title}"
        )
        AlbumSlot.STAMP -> Triple(
            "🏅",
            page.stampName,
            "Sello de la puerta ${page.gateLevel} · ${page.title}"
        )
        AlbumSlot.HERB -> Triple(
            page.herb.symbol,
            page.herb.displayName,
            "Hierba prensada · ${page.title}"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 30.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = title, fontWeight = FontWeight.Bold)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                when (memory.slot) {
                    AlbumSlot.CUSTOMER -> {
                        if (letter != null) {
                            Text(
                                text = letter.letterTitle,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "\"${letter.content}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    AlbumSlot.RECIPE -> {
                        Text(
                            text = page.recipe.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ingredientes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        page.recipe.ingredients.forEach { herb ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = herb.symbol, fontSize = 19.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = herb.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        val recipeLetter = page.recipe.letterId?.let { JournalLetters.byId(it) }
                        if (recipeLetter != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "La carta que la acompaña",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${recipeLetter.content}\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    AlbumSlot.STAMP -> {
                        Text(
                            text = page.stampDescription,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Página ${page.title} · niveles ${page.levelRange}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                        )
                    }

                    AlbumSlot.HERB -> Unit
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private fun slotAccent(page: AlbumPage, slot: AlbumSlot): Color = when (slot) {
    AlbumSlot.HERB -> page.herb.defaultColor
    AlbumSlot.RECIPE -> Color(0xFFD98A6C)
    AlbumSlot.CUSTOMER -> Color(0xFF88A087)
    AlbumSlot.STAMP -> Color(0xFFE8C872)
}

private fun unlockHint(page: AlbumPage, slot: AlbumSlot): String = when (slot) {
    AlbumSlot.HERB ->
        "La hierba de esta página se guarda al entrar al sector, en el nivel ${page.startLevel}."
    AlbumSlot.RECIPE ->
        "La receta se guarda al llegar a la mitad del sector, en el nivel ${page.recipeLevel}."
    AlbumSlot.CUSTOMER ->
        "La carta de este cliente llega al cerrar la puerta ${page.gateLevel}."
    AlbumSlot.STAMP ->
        "El sello se estampa al cerrar la puerta ${page.gateLevel}."
}

private data class AlbumMemory(
    val page: AlbumPage,
    val slot: AlbumSlot
)
