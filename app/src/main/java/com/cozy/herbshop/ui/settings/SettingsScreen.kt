package com.cozy.herbshop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.audio.AmbientManager
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import com.cozy.herbshop.ui.components.cozyBackground
import kotlinx.coroutines.launch

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.remember
import com.cozy.herbshop.data.model.Achievement
import com.cozy.herbshop.ui.components.GeneratedArt
import com.cozy.herbshop.ui.components.rememberResources
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.size

@Composable
fun SettingsScreen(
    repository: UserPreferencesRepository,
    onBackClicked: () -> Unit
) {
    val soundEnabled by repository.soundEnabledFlow.collectAsState(initial = true)
    val musicEnabled by repository.musicEnabledFlow.collectAsState(initial = true)
    val ambientEnabled by repository.ambientEnabledFlow.collectAsState(initial = true)
    val ambientSound by repository.ambientSoundFlow.collectAsState(initial = AmbientManager.DEFAULT_SOUND)
    val colorblindMode by repository.colorblindModeFlow.collectAsState(initial = true)
    val unlockedAchievements by repository.unlockedAchievementsFlow.collectAsState(initial = emptySet())
    val resources = rememberResources()

    val scope = rememberCoroutineScope()

    val achievements = remember {
        listOf(
            Achievement("ach_1", "Maestro Infusor", "Completa 10 niveles sin ayuda.", 10, 50, "🏆"),
            Achievement("ach_2", "Botánico Experto", "Desbloquea las 8 hierbas del Herbario.", 8, 100, "🌿"),
            Achievement("ach_3", "Ritual Mañanero", "Disfruta el Desafío Diario.", 1, 30, "☕"),
            Achievement("ach_4", "Diseñador Cozy", "Personaliza tu tienda con decoración.", 1, 40, "🏡")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .cozyBackground()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
                text = "Ajustes de la Tienda",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Efectos de Sonido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vertido de agua, cristal y UI",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { isChecked ->
                            AudioHub.sound.playUiToggle()
                            scope.launch { repository.setSoundEnabled(isChecked) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Música de Fondo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Melodías relajantes en menú y partida",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Switch(
                        checked = musicEnabled,
                        onCheckedChange = { isChecked ->
                            AudioHub.sound.playUiToggle()
                            scope.launch { repository.setMusicEnabled(isChecked) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Nature, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sonido Ambiente ASMR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Paisajes sonoros naturales en el Modo Zen",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Switch(
                        checked = ambientEnabled,
                        onCheckedChange = { isChecked ->
                            AudioHub.sound.playUiToggle()
                            scope.launch { repository.setAmbientEnabled(isChecked) }
                            if (!isChecked) AudioHub.ambient.stop()
                        }
                    )
                }

                if (ambientEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AmbientManager.OPTIONS.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AudioHub.sound.playUiClick()
                                    scope.launch { repository.setAmbientSound(option.id) }
                                    AudioHub.ambient.play(option.id)
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = ambientSound == option.id,
                                onClick = {
                                    AudioHub.sound.playUiClick()
                                    scope.launch { repository.setAmbientSound(option.id) }
                                    AudioHub.ambient.play(option.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${option.emoji} ${option.label}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Modo Accesibilidad / Daltónicos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Muestra símbolos distintivos en cada hierba",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Switch(
                        checked = colorblindMode,
                        onCheckedChange = { isChecked ->
                            AudioHub.sound.playUiToggle()
                            scope.launch { repository.setColorblindMode(isChecked) }
                        }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logros del Jugador",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                achievements.forEach { ach ->
                    val isUnlocked = ach.id in unlockedAchievements
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val achArt = remember(ach.id) { GeneratedArt.resId(resources, GeneratedArt.achievementArt(ach.id)) }
                        if (achArt != 0) {
                            Image(
                                painter = painterResource(achArt),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Fit,
                                alpha = if (isUnlocked) 1f else 0.35f
                            )
                        } else {
                            Text(text = ach.iconEmoji, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ach.title,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                            Text(
                                text = ach.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = if (isUnlocked) "Completado ✓" else "+${ach.rewardLeaves} 🍃",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cozy Herb Shop v1.0.0",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Juego casual relajante de ordenación y clasificación de especias y hierbas medicinales.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Diseñado para ser 100% offline y publicable en Google Play con costo cero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Audio: dominio público (CC0) — Kenney, BigSoundBank, OpenGameArt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gráficos: ilustraciones generadas con IA desde prompts propios + arte procedural original del juego + assets CC0 (ver CREDITS.txt y PROMPTS.md).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }
    }
}
