package com.cozy.herbshop.data.model

import androidx.compose.ui.graphics.Color

enum class Herb(
    val id: String,
    val displayName: String,
    val scientificName: String,
    val description: String,
    val aromaProfile: String,
    val origin: String,
    val symbol: String,
    val colorHex: String,
    val defaultColor: Color
) {
    CHAMOMILE("chamomile", "Manzanilla", "Matricaria chamomilla", "Relajante e infusión dorada con suaves flores blancas.", "Dulce, floral y manzana", "Europa y Asia Central", "🌼", "#E6C200", Color(0xFFFBC02D)),
    LAVENDER("lavender", "Lavanda", "Lavandula angustifolia", "Flores violetas aromáticas para la calma profunda.", "Fresco, floral y herbáceo", "Región Mediterránea", "🪻", "#8E24AA", Color(0xFFAB47BC)),
    MINT("mint", "Menta", "Mentha piperita", "Hojas frescas de verde vibrante con fresco aroma mentolado.", "Mentolado fresco e intenso", "Europa y Medio Oriente", "🍃", "#388E3C", Color(0xFF66BB6A)),
    ROSEMARY("rosemary", "Romero", "Salvia rosmarinus", "Especia leñosa y estimulante para la concentración.", "Pino leñoso y alcanforado", "Cuenca del Mediterráneo", "🌿", "#2E7D32", Color(0xFF388E3C)),
    HIBISCUS("hibiscus", "Flor de Jamaica", "Hibiscus sabdariffa", "Pétalos rojizos intensos de sabor cítrico y refrescante.", "Cítrico, afrutado y ácido", "África Tropical", "🌺", "#C2185B", Color(0xFFE91E63)),
    CINNAMON("cinnamon", "Canela", "Cinnamomum verum", "Corteza aromática cálida con tonos terrosos y especiados.", "Cálido, especiado y dulce", "Sri Lanka", "🪵", "#8D6E63", Color(0xFFA1887F)),
    CALENDULA("calendula", "Caléndula", "Calendula officinalis", "Flores anaranjadas luminosas llena de vitalidad.", "Suave, ligeramente amargo", "Sur de Europa", "🌸", "#F57C00", Color(0xFFFF9800)),
    THYME("thyme", "Tomillo", "Thymus vulgaris", "Pequeñas hojas herbales y aromáticas de monte.", "Herbal silvestre y picante", "Mediterráneo Occidental", "🌱", "#558B2F", Color(0xFF8BC34A));

    companion object {
        fun fromId(id: String): Herb {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CHAMOMILE
        }
    }
}

data class JarLayer(
    val herb: Herb,
    val isHidden: Boolean = false,
    val isFrozen: Boolean = false
)

data class CustomerOrder(
    val id: String,
    val customerName: String,
    val dialogue: String,
    val recipeName: String,
    val requiredHerbs: List<Herb>
)

data class Jar(
    val id: Int,
    val capacity: Int = 4,
    val layers: List<JarLayer> = emptyList(),
    val isSealed: Boolean = false
) {
    val stack: List<Herb> get() = layers.map { it.herb }
    val isFull: Boolean get() = layers.size >= capacity
    val isEmpty: Boolean get() = layers.isEmpty()
    val topLayer: JarLayer? get() = layers.lastOrNull()

    // Solo se obtiene la hierba superior si el frasco no está sellado, la capa está revelada y no está congelada
    val topHerb: Herb? get() = if (isSealed) null else topLayer?.takeIf { !it.isHidden && !it.isFrozen }?.herb

    val topCount: Int
        get() {
            if (isEmpty || isSealed) return 0
            val top = topHerb ?: return 0
            var count = 0
            for (i in layers.indices.reversed()) {
                val layer = layers[i]
                if (!layer.isHidden && !layer.isFrozen && layer.herb == top) count++ else break
            }
            return count
        }

    val isCompleted: Boolean
        get() = isEmpty || (isFull && !isSealed && layers.all { !it.isHidden && !it.isFrozen && it.herb == layers.first().herb })

    fun unseal(): Jar = copy(isSealed = false)

    fun unfreezeTopLayer(): Jar {
        if (layers.isEmpty() || !layers.last().isFrozen) return this
        val newLayers = layers.toMutableList()
        newLayers[newLayers.lastIndex] = newLayers.last().copy(isFrozen = false)
        return copy(layers = newLayers)
    }

    /**
     * Revela automáticamente la capa superior si estaba oculta.
     */
    fun revealTopLayer(): Jar {
        if (layers.isEmpty() || !layers.last().isHidden) return this
        val newLayers = layers.toMutableList()
        newLayers[newLayers.lastIndex] = newLayers.last().copy(isHidden = false)
        return copy(layers = newLayers)
    }
}

fun Jar(id: Int, capacity: Int = 4, stack: List<Herb>, isSealed: Boolean = false): Jar {
    return Jar(
        id = id,
        capacity = capacity,
        layers = stack.map { JarLayer(herb = it, isHidden = false) },
        isSealed = isSealed
    )
}

data class GameLevel(
    val levelNumber: Int,
    val jarCapacity: Int = 4,
    val jars: List<List<String>>,
    val customerOrder: CustomerOrder? = null,
    val isDailyChallenge: Boolean = false,
    val isZenMode: Boolean = false
)

sealed class PourResult {
    data class Success(val countPoured: Int) : PourResult()
    data class Invalid(val reason: String) : PourResult()
}

data class TeaRecipe(
    val id: String,
    val name: String,
    val description: String,
    val ingredients: List<Herb>,
    val benefits: String,
    val requiredHerbIds: Set<String>
)

data class DecorationItem(
    val id: String,
    val name: String,
    val category: String,
    val priceInLeaves: Int,
    val iconEmoji: String,
    val description: String
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val targetCount: Int,
    val rewardLeaves: Int,
    val iconEmoji: String
)

data class CustomerLetter(
    val id: String,
    val senderName: String,
    val senderAvatar: String,
    val letterTitle: String,
    val content: String
)
