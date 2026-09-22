package com.cozy.herbshop.data.model

import androidx.compose.ui.graphics.Color

enum class Herb(
    val id: String,
    val displayName: String,
    val description: String,
    val colorHex: String,
    val defaultColor: Color
) {
    CHAMOMILE("chamomile", "Manzanilla", "Relajante e infusión dorada con suaves flores blancas.", "#E6C200", Color(0xFFFBC02D)),
    LAVENDER("lavender", "Lavanda", "Flores violetas aromáticas para la calma profunda.", "#8E24AA", Color(0xFFAB47BC)),
    MINT("mint", "Menta", "Hojas frescas de verde vibrante con fresco aroma.", "#388E3C", Color(0xFF66BB6A)),
    ROSEMARY("rosemary", "Romero", "Especia leñosa y estimulante para la concentración.", "#2E7D32", Color(0xFF388E3C)),
    HIBISCUS("hibiscus", "Flor de Jamaica", "Pétalos rojizos intensos de sabor cítrico.", "#C2185B", Color(0xFFE91E63)),
    CINNAMON("cinnamon", "Canela", "Corteza aromática cálida con tonos terrosos.", "#8D6E63", Color(0xFFA1887F)),
    CALENDULA("calendula", "Caléndula", "Flores anaranjadas luminosas llena de vida.", "#F57C00", Color(0xFFFF9800)),
    THYME("thyme", "Tomillo", "Pequeñas hojas herbales y aromáticas de monte.", "#558B2F", Color(0xFF8BC34A));

    companion object {
        fun fromId(id: String): Herb {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CHAMOMILE
        }
    }
}

data class Jar(
    val id: Int,
    val capacity: Int = 4,
    val stack: List<Herb> = emptyList()
) {
    val isFull: Boolean get() = stack.size >= capacity
    val isEmpty: Boolean get() = stack.isEmpty()
    val topHerb: Herb? get() = stack.lastOrNull()

    val topCount: Int
        get() {
            if (isEmpty) return 0
            val top = topHerb ?: return 0
            var count = 0
            for (i in stack.indices.reversed()) {
                if (stack[i] == top) count++ else break
            }
            return count
        }

    val isCompleted: Boolean
        get() = isEmpty || (isFull && stack.all { it == stack.first() })
}

data class GameLevel(
    val levelNumber: Int,
    val jarCapacity: Int = 4,
    val jars: List<List<String>>
)

sealed class PourResult {
    data class Success(val countPoured: Int) : PourResult()
    data class Invalid(val reason: String) : PourResult()
}
