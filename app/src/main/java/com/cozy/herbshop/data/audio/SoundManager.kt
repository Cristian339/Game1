package com.cozy.herbshop.data.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

class SoundManager(context: Context) {

    private var toneGenerator: ToneGenerator? = null
    var soundEnabled: Boolean = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playJarClick() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 60)
        } catch (_: Exception) {}
    }

    fun playPourSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 100)
        } catch (_: Exception) {}
    }

    fun playLevelCompleteSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_S, 250)
        } catch (_: Exception) {}
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
