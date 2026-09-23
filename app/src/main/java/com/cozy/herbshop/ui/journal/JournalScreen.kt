package com.cozy.herbshop.ui.journal

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozy.herbshop.data.audio.AudioHub
import com.cozy.herbshop.data.model.CustomerLetter
import com.cozy.herbshop.data.repository.UserPreferencesRepository

@Composable
fun JournalScreen(
    repository: UserPreferencesRepository,
    onBackClicked: () -> Unit
) {
    val unlockedLetters by repository.customerLettersFlow.collectAsState(initial = emptySet())
    var selectedLetter by remember { mutableStateOf<CustomerLetter?>(null) }

    val letters = remember {
        listOf(
            CustomerLetter(
                id = "letter_clara",
                senderName = "Abuela Clara",
                senderAvatar = "👵",
                letterTitle = "Carta de Agradecimiento - Dulce Calma",
                content = "Querido herbolario, la infusión de Manzanilla y Lavanda que me preparaste ayer me ayudó a dormir plácidamente toda la noche. ¡Hacía meses que no descansaba tan bien! Gracias por llenar tu tienda de tanto cariño."
            ),
            CustomerLetter(
                id = "letter_arthur",
                senderName = "Botánico Arthur",
                senderAvatar = "👨‍🔬",
                letterTitle = "Notas de Investigación - Menta & Romero",
                content = "¡Estimado colega! El elixir de concentración con Menta y Romero fue un éxito rotundo durante mis horas de estudio sobre la flora silvestre. Tu precisión con las capas de hierbas es admirable."
            ),
            CustomerLetter(
                id = "letter_flora",
                senderName = "Madame Flora",
                senderAvatar = "🌺",
                letterTitle = "Un Té Especial - Flor de Jamaica",
                content = "¡Hola querido! Mi té de Flor de Jamaica y Canela causó sensación en el té de la tarde con las vecinas del pueblo. Todas quieren saber el secreto de tu tienda."
            )
        )
    }

    selectedLetter?.let { letter ->
        AlertDialog(
            onDismissRequest = { selectedLetter = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = letter.senderAvatar, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = letter.senderName, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(text = letter.letterTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\"${letter.content}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedLetter = null }) {
                    Text("Guardar Carta")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                text = "Diario de la Tienda",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(letters) { letter ->
                val isUnlocked = letter.id in unlockedLetters

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isUnlocked) {
                            if (isUnlocked) AudioHub.sound.playDialogOpen()
                            selectedLetter = letter
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (isUnlocked) letter.senderAvatar else "✉️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isUnlocked) letter.senderName else "Cliente Misterioso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isUnlocked) letter.letterTitle else "Completa más pedidos para desbloquear.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        if (!isUnlocked) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        } else {
                            Icon(imageVector = Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
