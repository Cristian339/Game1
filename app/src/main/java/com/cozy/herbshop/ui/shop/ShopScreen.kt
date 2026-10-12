package com.cozy.herbshop.ui.shop

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.billing.BillingManager
import com.cozy.herbshop.ui.components.cozyBackground
import com.cozy.herbshop.ui.components.shopDecorationsPreview
import com.cozy.herbshop.ui.components.GeneratedArt
import com.cozy.herbshop.ui.components.rememberResources
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.rememberCoroutineScope
import com.cozy.herbshop.data.model.DecorationItem
import com.cozy.herbshop.data.model.HelpItem
import com.cozy.herbshop.data.repository.UserPreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(
    billingManager: BillingManager,
    repository: UserPreferencesRepository,
    onBackClicked: () -> Unit,
    onWatchAdForStamina: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val resources = rememberResources()

    val isAdsRemoved by billingManager.isAdsRemoved.collectAsState()
    val isThemePackUnlocked by billingManager.isThemePackUnlocked.collectAsState()
    val goldLeaves by repository.goldLeavesFlow.collectAsState(initial = 50)
    val unlockedDecorations by repository.unlockedDecorationsFlow.collectAsState(initial = emptySet())
    val unlockedAsmrPacks by repository.unlockedAsmrPacksFlow.collectAsState(initial = setOf("asmr_cristal"))
    val helpStocks = HelpItem.entries.associateWith { help ->
        repository.helpStockFlow(help.id).collectAsState(
            initial = UserPreferencesRepository.STARTING_HELP_STOCK[help.id] ?: 0
        ).value
    }

    val decorations = remember {
        listOf(
            DecorationItem("estante_roble", "Estante de Roble Cozy", "Muebles", 30, "🪵", "Elegante estantería artesanal para frascos."),
            DecorationItem("luces_hadas", "Guirnalda de Luces Cálidas", "Iluminación", 40, "✨", "Luces de ambiente suave para el mostrador."),
            DecorationItem("estufa_calida", "Estufa de Leña Clásica", "Muebles", 60, "🔥", "Aporta calor y aroma a té tostado."),
            DecorationItem("plantas_cozy", "Macetas de Helechos Silvestres", "Plantas", 35, "🌿", "Plantas decorativas frescas para la entrada.")
        )
    }

    val asmrPacks = remember {
        listOf(
            DecorationItem("asmr_cristal", "ASMR Cristal Pro", "Audio", 0, "🔔", "Sin ambiente: añade un timbre cristalino cada vez que seleccionas un frasco o sirves la tetera."),
            DecorationItem("asmr_lluvia", "Lluvia Relajante", "Audio", 40, "🌧️", "Sonido ambiental de lluvia cayendo en la ventana."),
            DecorationItem("asmr_chimenea", "Chimenea de Leña", "Audio", 40, "🔥", "Cálido chisporroteo de leña para tus tardes de juego."),
            DecorationItem("asmr_arroyo", "Arroyo Tranquilo", "Audio", 55, "💧", "Agua corriendo entre piedras, suave y constante."),
            DecorationItem("asmr_bosque", "Bosque en Calma", "Audio", 55, "🌲", "Bosque con pájaros y hojas, como escuchar la montaña."),
            DecorationItem("asmr_noche", "Noche Estrellada", "Audio", 70, "🌙", "Grillos y cigarras en una noche cálida de verano.")
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
                text = "Tienda",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.weight(1f))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "🍃 $goldLeaves",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        Icon(
                            imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Decoración de la Herboristería",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Gasta tus Hojas de Oro (🍃) ganadas en los niveles para amueblar tu tienda.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shopDecorationsPreview(unlockedDecorations)
                )

                Spacer(modifier = Modifier.height(12.dp))

                decorations.forEach { item ->
                    val isPurchased = item.id in unlockedDecorations

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val decoArt = remember(item.id) { GeneratedArt.resId(resources, GeneratedArt.decorationArt(item.id)) }
                        if (decoArt != 0) {
                            Image(
                                painter = painterResource(decoArt),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(text = item.iconEmoji, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, fontWeight = FontWeight.Bold)
                            Text(text = item.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (!isPurchased && goldLeaves >= item.priceInLeaves) {
                                    AudioHub.sound.playPurchase()
                                } else {
                                    AudioHub.sound.playUiError()
                                }
                                scope.launch {
                                    repository.unlockDecoration(item.id, item.priceInLeaves)
                                }
                            },
                            enabled = !isPurchased && goldLeaves >= item.priceInLeaves,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = if (isPurchased) "Equipado ✓" else "${item.priceInLeaves} 🍃")
                        }
                    }
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
            border = BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🆘", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ayudas de Partida",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cuando se te acaban durante una partida, el juego te avisa y puedes ver un anuncio o comprar aquí. Tienes:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                HelpItem.entries.forEach { help ->
                    val stock = helpStocks[help] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val helpArt = remember(help.id) { GeneratedArt.resId(resources, help.iconRes) }
                        if (helpArt != 0) {
                            Image(
                                painter = painterResource(helpArt),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = help.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Tienes $stock · pack de ${help.packSize} usos",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val paid = goldLeaves >= help.packPrice
                                if (paid) {
                                    AudioHub.sound.playPurchase()
                                } else {
                                    AudioHub.sound.playUiError()
                                }
                                scope.launch {
                                    repository.grantHelpStock(help.id, help.packSize, help.packPrice)
                                }
                            },
                            enabled = goldLeaves >= help.packPrice,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "${help.packPrice} 🍃")
                        }
                    }
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
            border = BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎧", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Paquetes de Sonido ASMR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Desbloquea ambientes y efectos de audio ASMR con tus Hojas de Oro.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                asmrPacks.forEach { pack ->
                    val isPurchased = pack.id in unlockedAsmrPacks
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val packArt = remember(pack.id) { GeneratedArt.resId(resources, GeneratedArt.asmrArt(pack.id)) }
                        if (packArt != 0) {
                            Image(
                                painter = painterResource(packArt),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = pack.iconEmoji, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = pack.name, fontWeight = FontWeight.Bold)
                            Text(text = pack.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (!isPurchased && goldLeaves >= pack.priceInLeaves) {
                                    AudioHub.sound.playPurchase()
                                } else {
                                    AudioHub.sound.playUiError()
                                }
                                scope.launch {
                                    repository.unlockAsmrPack(pack.id, pack.priceInLeaves)
                                }
                            },
                            enabled = !isPurchased && goldLeaves >= pack.priceInLeaves,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = if (isPurchased) "Desbloqueado ✓" else "${pack.priceInLeaves} 🍃")
                        }
                    }
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
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quitar Anuncios (Compra Única)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Elimina los intersticiales para una experiencia 100% acogedora y sin distracciones.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        AudioHub.sound.playUiConfirm()
                        activity?.let {
                            billingManager.launchPurchaseFlow(it, BillingManager.PRODUCT_REMOVE_ADS)
                        }
                    },
                    enabled = !isAdsRemoved,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(text = if (isAdsRemoved) "Anuncios Eliminados ✓" else "Comprar - $0.99 USD")
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
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pack de Temas Estacionales",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Desbloquea los estilos visuales de Tienda de Invierno y Tienda de Primavera.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        AudioHub.sound.playUiConfirm()
                        activity?.let {
                            billingManager.launchPurchaseFlow(it, BillingManager.PRODUCT_THEME_PACK)
                        }
                    },
                    enabled = !isThemePackUnlocked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(text = if (isThemePackUnlocked) "Pack Adquirido ✓" else "Comprar - $1.99 USD")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recarga de Frascos Gratis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Mira un video corto para recibir +3 frascos frescos de inmediato.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        AudioHub.sound.playUiClick()
                        onWatchAdForStamina()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        "Ver Video (+3 Frascos)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
