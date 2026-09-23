package com.cozy.herbshop.data.audio

import android.content.Context
import android.media.MediaPlayer
import com.cozy.herbshop.R

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
        val resId = when (soundName.lowercase()) {
            "lluvia", "rain" -> R.raw.amb_rain
            else -> R.raw.amb_rain
        }
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
}
