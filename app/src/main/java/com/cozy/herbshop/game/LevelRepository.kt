package com.cozy.herbshop.game

import android.content.Context
import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class LevelRepository(
    private val context: Context?,
    private val levelGenerator: LevelGenerator = LevelGenerator()
) {
    private val gson = Gson()
    private var jsonLevelsCache: List<GameLevel>? = null

    fun loadPredefinedLevels(): List<GameLevel> {
        if (jsonLevelsCache != null) return jsonLevelsCache!!
        if (context == null) return emptyList()

        return try {
            context.assets.open("levels.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<GameLevel>>() {}.type
                    val levels: List<GameLevel> = gson.fromJson(reader, type)
                    jsonLevelsCache = levels
                    levels
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLevel(levelNumber: Int): GameLevel {
        val predefined = loadPredefinedLevels()
        val found = predefined.find { it.levelNumber == levelNumber }
        if (found != null) return found

        val numHerbs = minOf(3 + (levelNumber / 3), Herb.entries.size)
        val jarCapacity = 4
        val emptyJars = 2

        return levelGenerator.generateSolvableLevel(
            levelNumber = levelNumber,
            numHerbTypes = numHerbs,
            emptyJars = emptyJars,
            jarCapacity = jarCapacity
        )
    }

    fun convertToJars(level: GameLevel): List<Jar> {
        return level.jars.mapIndexed { index, herbIds ->
            val stack = herbIds.map { Herb.fromId(it) }
            Jar(id = index, capacity = level.jarCapacity, stack = stack)
        }
    }
}
