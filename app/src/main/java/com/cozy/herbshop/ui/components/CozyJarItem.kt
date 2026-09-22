package com.cozy.herbshop.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cozy.herbshop.data.model.Jar

@Composable
fun CozyJarItem(
    jar: Jar,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetY by animateFloatAsState(
        targetValue = if (isSelected) -16f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "jarSelectionOffset"
    )

    Box(
        modifier = modifier
            .offset(y = offsetY.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.width(36.dp).height(12.dp)) {
                drawRoundRect(
                    color = Color(0xFF8D6E63),
                    size = size,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .border(
                        width = if (isSelected) 3.dp else 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    val capacity = jar.capacity
                    val layerHeight = size.height / capacity

                    jar.stack.forEachIndexed { index, herb ->
                        val yTop = size.height - ((index + 1) * layerHeight)
                        drawRoundRect(
                            color = herb.defaultColor,
                            topLeft = Offset(4.dp.toPx(), yTop + 2.dp.toPx()),
                            size = Size(size.width - 8.dp.toPx(), layerHeight - 4.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(8.dp.toPx(), 8.dp.toPx()),
                        end = Offset(8.dp.toPx(), size.height - 12.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }
    }
}
