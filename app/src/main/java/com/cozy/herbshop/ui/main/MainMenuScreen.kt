package com.cozy.herbshop.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CollectionsBookmark
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.ui.components.ShopDecorationLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.cozy.herbshop.R
import com.cozy.herbshop.ui.components.woodPlaqueBackground

@Composable
fun MainMenuScreen(
    viewModel: MainViewModel,
    onPlayClicked: () -> Unit,
    onDailyChallengeClicked: () -> Unit,
    onZenModeClicked: () -> Unit,
    onJournalClicked: () -> Unit,
    onLevelMapClicked: () -> Unit,
    onHerbariumClicked: () -> Unit,
    onShopClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onCollectionClicked: () -> Unit,
    onWatchAdForStamina: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val decorations by viewModel.decorations.collectAsState()

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
    ) {
        // Fondo ilustrado del mostrador con las decoraciones compradas encima.
        Image(
            painter = painterResource(R.drawable.bg_loading),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        ShopDecorationLayer(
            unlocked = decorations,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .woodPlaqueBackground()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Heart & Herb",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        shadow = Shadow(
                            color = androidx.compose.ui.graphics.Color(0xCC3E2723),
                            offset = androidx.compose.ui.geometry.Offset(0f, 3f),
                            blurRadius = 6f
                        )
                    ),
                    color = androidx.compose.ui.graphics.Color(0xFFFFF6E0),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Tienda de Té & Hierbas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        shadow = Shadow(
                            color = androidx.compose.ui.graphics.Color(0x993E2723),
                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    color = androidx.compose.ui.graphics.Color(0xFFEAD9B0),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    AudioHub.sound.playUiConfirm()
                    viewModel.consumeStaminaForGame(
                        onSuccess = onPlayClicked,
                        onNoStamina = {
                            AudioHub.sound.playNoStamina()
                            onWatchAdForStamina()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp)
                    .scale(pulseScale),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "JUGAR (Nivel ${uiState.currentLevel})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    AudioHub.sound.playUiClick()
                    viewModel.consumeStaminaForGame(
                        onSuccess = onDailyChallengeClicked,
                        onNoStamina = {
                            AudioHub.sound.playNoStamina()
                            onWatchAdForStamina()
                        }
                    )
                },
                // Superado el reto de hoy el boton se apaga: solo vuelve manana.
                enabled = !uiState.dailyChallengeDoneToday,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 1.dp
                )
            ) {
                Text(
                    text = if (uiState.dailyChallengeDoneToday) {
                        "📅 Reto Diario hecho · vuelve mañana"
                    } else {
                        "📅 Desafío Diario (+3 🌸)"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.dailyChallengeDoneToday) {
                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onTertiary
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        AudioHub.sound.playUiClick()
                        onJournalClicked()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Text(
                        "📖 Diario",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = {
                        AudioHub.sound.playUiConfirm()
                        onZenModeClicked()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Text(
                        "🧘 Zen",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        AudioHub.sound.playUiClick()
                        onLevelMapClicked()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Text(
                        "🗺️ Niveles",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = {
                        AudioHub.sound.playUiClick()
                        onHerbariumClicked()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Text(
                        "Herbario (${uiState.unlockedHerbsCount}/8)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MenuNavigationButton(
                text = "Tienda & Ads",
                icon = Icons.Default.ShoppingCart,
                onClick = {
                    AudioHub.sound.playUiClick()
                    onShopClicked()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuNavigationButton(
                text = "Ajustes",
                icon = Icons.Default.Settings,
                onClick = {
                    AudioHub.sound.playUiClick()
                    onSettingsClicked()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuNavigationButton(
                text = "Mi Colección",
                icon = Icons.Default.CollectionsBookmark,
                onClick = {
                    AudioHub.sound.playUiClick()
                    onCollectionClicked()
                }
            )
        }
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
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
