package com.cozy.herbshop.ui.herbarium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import com.cozy.herbshop.ui.collection.CollectionShowcaseScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.TeaRecipe
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.ui.components.cozyBackground
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset

@Composable
fun HerbariumScreen(
    repository: UserPreferencesRepository,
    onBackClicked: () -> Unit,
    onShopClicked: () -> Unit = {}
) {
    val unlockedHerbsSet by repository.unlockedHerbsFlow.collectAsState(initial = emptySet())
    val secretosSet by repository.secretIngredientsFlow.collectAsState(initial = emptySet())
    var selectedHerb by remember { mutableStateOf<Herb?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val recipes = remember {
        listOf(
            TeaRecipe(
                id = "recipe_1",
                name = "Infusión Dulce Noche",
                description = "Una mezcla clásica y reconfortante para relajar la mente antes de dormir.",
                ingredients = listOf(Herb.CHAMOMILE, Herb.LAVENDER),
                benefits = "Promueve el sueño profundo y reduce el estrés.",
                requiredHerbIds = setOf("chamomile", "lavender")
            ),
            TeaRecipe(
                id = "recipe_2",
                name = "Elixir de Enfoque",
                description = "Combinación leñosa y mentolada que despeja los sentidos.",
                ingredients = listOf(Herb.MINT, Herb.ROSEMARY),
                benefits = "Estimula la memoria, la atención y alivia la fatiga mental.",
                requiredHerbIds = setOf("mint", "rosemary")
            ),
            TeaRecipe(
                id = "recipe_3",
                name = "Té Antioxidante de Jamaica",
                description = "Infusión rojiza e inténsa llena de vitaminas y sabor afrutado.",
                ingredients = listOf(Herb.HIBISCUS, Herb.CINNAMON),
                benefits = "Refuerza defensas, mejora la digestión y energiza el cuerpo.",
                requiredHerbIds = setOf("hibiscus", "cinnamon")
            ),
            TeaRecipe(
                id = "recipe_4",
                name = "Bálsamo Floral Dorado",
                description = "Flores anaranjadas y aromáticas llenas de sol.",
                ingredients = listOf(Herb.CALENDULA, Herb.THYME),
                benefits = "Efecto antiinflamatorio y protector de la garganta.",
                requiredHerbIds = setOf("calendula", "thyme")
            )
        )
    }

    selectedHerb?.let { herb ->
        HerbDetailDialog(herb = herb, onDismiss = { selectedHerb = null })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .cozyBackground()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                AudioHub.sound.playUiBack()
                onBackClicked()
            }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Herbario",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${unlockedHerbsSet.size}/8",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { AudioHub.sound.playTab(); selectedTab = 0 },
                text = {
                    Text(
                        "🌱 Hierbas",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { AudioHub.sound.playTab(); selectedTab = 1 },
                text = {
                    Text(
                        "📜 Recetario",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { AudioHub.sound.playTab(); selectedTab = 2 },
                text = {
                    Text(
                        "✨ Secretos",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 2) {
            // Hierbas secretas: una por puerta, escondidas en un frasco al azar.
            // Hasta que el jugador no las destapa en la partida no se ven.
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Herb.secretas) { secreta ->
                    val encontrada = secreta.id in secretosSet

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (encontrada) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(
                            1.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = if (encontrada) 0.20f else 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (encontrada) secreta.symbol else "❓", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (encontrada) secreta.displayName else "Hierba secreta",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (encontrada) {
                                        secreta.description
                                    } else {
                                        "Escondida en un frasco de la puerta ${secreta.gate}: " +
                                            "destápala en la partida para que entre al herbario."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                            if (encontrada) {
                                Text(text = "✨", fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Herb.delReparto) { herb ->
                    val isUnlocked = herb.id in unlockedHerbsSet

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isUnlocked) {
                                if (isUnlocked) AudioHub.sound.playDialogOpen()
                                selectedHerb = herb
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(
                            1.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = if (isUnlocked) 0.20f else 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        brush = if (isUnlocked) Brush.radialGradient(
                                            colors = listOf(
                                                herb.defaultColor.copy(alpha = 1f),
                                                herb.defaultColor.copy(
                                                    red = herb.defaultColor.red * 0.75f,
                                                    green = herb.defaultColor.green * 0.75f,
                                                    blue = herb.defaultColor.blue * 0.75f
                                                )
                                            ),
                                            center = Offset(22f, 18f),
                                            radius = 48f
                                        ) else Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .then(
                                        if (isUnlocked) Modifier.border(
                                            BorderStroke(
                                                2.5.dp,
                                                herb.defaultColor.copy(
                                                    red = herb.defaultColor.red * 0.6f,
                                                    green = herb.defaultColor.green * 0.6f,
                                                    blue = herb.defaultColor.blue * 0.6f
                                                )
                                            ),
                                            CircleShape
                                        ) else Modifier.border(
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                            CircleShape
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUnlocked) {
                                    Text(text = herb.symbol, fontSize = 26.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Bloqueado",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isUnlocked) herb.displayName else "???",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (isUnlocked) herb.description else "Desbloquea en los niveles puerta con su receta.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isUnlocked) 0.85f else 0.75f)
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipes) { recipe ->
                    val isRecipeUnlocked = recipe.requiredHerbIds.all { it in unlockedHerbsSet }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRecipeUnlocked) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(
                            1.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = if (isRecipeUnlocked) 0.20f else 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isRecipeUnlocked) recipe.name else "Receta Secreta ???",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (!isRecipeUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isRecipeUnlocked) recipe.description else "Desbloquea las hierbas requeridas en el juego para revelar esta receta.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                            )

                            if (isRecipeUnlocked) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✨ Beneficios: ${recipe.benefits}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ficha de una hierba, compartida por el Herbario y la vitrina de coleccion. */
@Composable
fun HerbDetailDialog(
    herb: com.cozy.herbshop.data.model.Herb,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = herb.symbol, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = herb.displayName, fontWeight = FontWeight.Bold)
                    Text(
                        text = herb.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        text = {
            Column {
                Text(text = herb.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "🌸 Aroma: ${herb.aromaProfile}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🌍 Origen: ${herb.origin}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
