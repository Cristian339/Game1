package com.cozy.herbshop.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainMenuScreen(
    viewModel: MainViewModel,
    onPlayClicked: () -> Unit,
    onDailyChallengeClicked: () -> Unit,
    onZenModeClicked: () -> Unit,
    onJournalClicked: () -> Unit,
    onHerbariumClicked: () -> Unit,
    onShopClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onWatchAdForStamina: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Frascos diarios",
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${uiState.staminaCount}/5",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Cozy Herb Shop",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp
            )
            Text(
                text = "Tienda de Té & Hierbas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    viewModel.consumeStaminaForGame(
                        onSuccess = onPlayClicked,
                        onNoStamina = onWatchAdForStamina
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp)
                    .scale(pulseScale),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "JUGAR (Nivel ${uiState.currentLevel})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    viewModel.consumeStaminaForGame(
                        onSuccess = onDailyChallengeClicked,
                        onNoStamina = onWatchAdForStamina
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    text = "📅 Desafío Diario (+3 🌸)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onJournalClicked,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("📖 Diario", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onZenModeClicked,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("🧘 Modo Zen", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            MenuNavigationButton(
                text = "Herbario (${uiState.unlockedHerbsCount} descubiertas)",
                icon = Icons.Default.List,
                onClick = onHerbariumClicked
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuNavigationButton(
                text = "Tienda & Ads",
                icon = Icons.Default.ShoppingCart,
                onClick = onShopClicked
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuNavigationButton(
                text = "Ajustes",
                icon = Icons.Default.Settings,
                onClick = onSettingsClicked
            )
        }
    }
}

@Composable
private fun MenuNavigationButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(52.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
