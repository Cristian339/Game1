package com.cozy.herbshop.data.audio

import android.content.Context
import android.media.MediaPlayer
import com.cozy.herbshop.R

data class AmbientOption(
    val id: String,
    val label: String,
    val emoji: String,
    val resId: Int
)

class AmbientManager(private val context: Context) {

    private var player: MediaPlayer? = null
    private var currentSound: String? = null

    var ambientEnabled: Boolean = true

    fun play(soundName: String) {
        if (!ambientEnabled) {
            stop()
            return
        }
        if (currentSound == soundName && player?.isPlaying == true) return
        stop()
        val resId = resIdFor(soundName)
        try {
            player = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(0.35f, 0.35f)
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_GAME)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                start()
            }
            currentSound = soundName
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
        currentSound = null
    }

    fun pause() {
        try { player?.pause() } catch (_: Exception) {}
    }

    fun resume() {
        if (!ambientEnabled) return
        try { player?.start() } catch (_: Exception) {}
    }

    fun release() = stop()

    companion object {
        val OPTIONS = listOf(
            AmbientOption("lluvia", "Lluvia", "🌧️", R.raw.amb_rain),
            AmbientOption("bosque", "Bosque", "🌲", R.raw.amb_forest),
            AmbientOption("arroyo", "Arroyo", "💧", R.raw.amb_stream),
            AmbientOption("lumbre", "Lumbre", "🔥", R.raw.amb_fire),
            AmbientOption("noche", "Noche", "🦗", R.raw.amb_night)
        )

        const val DEFAULT_SOUND = "lluvia"

        fun resIdFor(soundName: String): Int =
            OPTIONS.firstOrNull { it.id.equals(soundName, ignoreCase = true) }?.resId
                ?: R.raw.amb_rain
    }
}
