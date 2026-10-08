package com.cozy.herbshop.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.cozy.herbshop.R
import com.cozy.herbshop.ui.components.GameScene

enum class MusicTrack(val resId: Int) {
    MENU(R.raw.music_menu),
    MORNING(R.raw.music_morning),
    NIGHT(R.raw.music_night),
    FOREST(R.raw.music_forest),
    AUTUMN(R.raw.music_autumn),
    ZEN(R.raw.music_zen);

    companion object {
        /** Pista de fondo para el escenario de una partida (null en modo Zen). */
        fun forScene(scene: GameScene?): MusicTrack = when (scene) {
            GameScene.NIGHT -> NIGHT
            GameScene.FOREST -> FOREST
            GameScene.AUTUMN -> AUTUMN
            else -> MORNING
        }
    }
}

class MusicManager(private val context: Context) {

    private var player: MediaPlayer? = null
    private var currentTrack: MusicTrack? = null
    private var desiredTrack: MusicTrack? = null
    private var pendingTrack: MusicTrack? = null

    private val handler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null

    private var fadeMode: FadeMode = FadeMode.NONE
    private var fadeTarget: Float = 0f
    private var fadeStep: Float = 0f
    private var fadeOnDone: (() -> Unit)? = null

    private var currentVolume = 0f
    private val targetVolume = 0.55f
    private var lifecyclePaused = false

    private enum class FadeMode { NONE, IN, OUT_SWITCH, OUT_STOP }

    var musicEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (!value) {
                fadeOutAndStop()
            } else {
                (pendingTrack ?: desiredTrack ?: currentTrack)?.let { play(it) }
            }
        }

    fun play(track: MusicTrack) {
        desiredTrack = track
        if (!musicEnabled) {
            pendingTrack = track
            return
        }
        pendingTrack = null
        // Si la app está en segundo plano, no arrancamos nada: resume() se encarga.
        if (lifecyclePaused) return
        if (currentTrack == track && player?.isPlaying == true) {
            when (fadeMode) {
                // Se reactivó la música a mitad de fade-out: subimos de nuevo.
                FadeMode.OUT_STOP -> startFade(FadeMode.IN, target = targetVolume, step = 0.05f)
                // Se pidió la pista actual mientras cambiaba a otra: cancelamos el cambio.
                FadeMode.OUT_SWITCH -> {
                    cancelFadeCallbacks()
                    fadeMode = FadeMode.NONE
                    fadeOnDone = null
                }
                else -> Unit
            }
            return
        }

        val existing = player
        if (currentTrack == track && existing != null) {
            // Pista ya cargada pero detenida (p.ej. fade-out a 0): la revivimos donde estaba.
            if (revive(existing)) return
        }
        fadeOutAndSwitchTo(track)
    }

    private fun fadeOutAndSwitchTo(track: MusicTrack) {
        val old = player
        if (old == null || currentTrack == null || !old.isPlaying) {
            startNew(track)
            return
        }
        startFade(FadeMode.OUT_SWITCH, target = 0f, step = -0.1f) {
            stopPlayer(old)
            if (player === old) player = null
            startNew(track)
        }
    }

    private fun startNew(track: MusicTrack) {
        stopPlayer(player)
        player = null
        currentTrack = track
        desiredTrack = track
        currentVolume = 0f

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val p = try {
            MediaPlayer.create(context, track.resId, attrs, AudioManager.AUDIOFOCUS_NONE)
        } catch (_: Exception) {
            null
        }
        if (p == null) return

        try {
            p.isLooping = true
            p.setVolume(0f, 0f)
            p.start()
        } catch (_: Exception) {
            stopPlayer(p)
            return
        }
        player = p
        startFade(FadeMode.IN, target = targetVolume, step = 0.05f)
    }

    private fun revive(p: MediaPlayer): Boolean {
        return try {
            applyVolume(p, 0f)
            p.start()
            startFade(FadeMode.IN, target = targetVolume, step = 0.05f)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun fadeOutAndStop() {
        val old = player
        if (old == null || !old.isPlaying) {
            if (old != null) applyVolume(old, 0f)
            cancelFadeCallbacks()
            fadeMode = FadeMode.NONE
            fadeOnDone = null
            return
        }
        startFade(FadeMode.OUT_STOP, target = 0f, step = -0.1f) {
            try { old.pause() } catch (_: Exception) {}
        }
    }

    private fun startFade(mode: FadeMode, target: Float, step: Float, onDone: (() -> Unit)? = null) {
        cancelFadeCallbacks()
        fadeMode = mode
        fadeTarget = target
        fadeStep = step
        fadeOnDone = onDone
        val r = object : Runnable {
            override fun run() {
                if (fadeMode == FadeMode.NONE) return
                val p = player
                val next = currentVolume + fadeStep
                val finished = if (fadeStep > 0f) next >= fadeTarget else next <= fadeTarget
                if (finished) {
                    applyVolume(p, fadeTarget)
                    val done = fadeOnDone
                    cancelFadeCallbacks()
                    fadeMode = FadeMode.NONE
                    fadeOnDone = null
                    done?.invoke()
                } else {
                    applyVolume(p, next)
                    fadeRunnable = this
                    handler.postDelayed(this, 50)
                }
            }
        }
        fadeRunnable = r
        handler.post(r)
    }

    private fun applyVolume(p: MediaPlayer?, volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try { p?.setVolume(currentVolume, currentVolume) } catch (_: Exception) {}
    }

    private fun cancelFadeCallbacks() {
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
    }

    private fun stopPlayer(p: MediaPlayer?) {
        if (p == null) return
        try { p.stop() } catch (_: Exception) {}
        try { p.release() } catch (_: Exception) {}
    }

    fun pause() {
        lifecyclePaused = true
        // Solo cancelamos los callbacks: el estado del fade (modo/objetivo/progreso
        // por volumen) se conserva para reanudarlo en resume().
        cancelFadeCallbacks()
        try { player?.pause() } catch (_: Exception) {}
    }

    fun resume() {
        lifecyclePaused = false
        if (!musicEnabled) return

        val desired = desiredTrack ?: currentTrack
        if (desired == null) return

        if (desired != currentTrack) {
            // Un cambio de pista quedó interrumpido por el pause: lo retomamos.
            fadeOutAndSwitchTo(desired)
            return
        }

        val p = player
        if (p == null) {
            startNew(desired)
            return
        }

        try {
            if (!p.isPlaying) p.start()
        } catch (_: Exception) {
            startNew(desired)
            return
        }

        when {
            // Música reactivada (o cambio cancelado) mientras un fade-out seguía
            // programado: en vez de continuar hacia 0, subimos hasta el volumen objetivo.
            fadeMode == FadeMode.OUT_STOP ||
                (fadeMode == FadeMode.OUT_SWITCH && desired == currentTrack) ->
                startFade(FadeMode.IN, target = targetVolume, step = 0.05f)

            fadeMode != FadeMode.NONE -> {
                val mode = fadeMode
                val target = fadeTarget
                val step = fadeStep
                val done = fadeOnDone
                startFade(mode, target, step, done)
            }

            currentVolume < targetVolume - 0.01f ->
                startFade(FadeMode.IN, target = targetVolume, step = 0.05f)
        }
    }

    fun release() {
        cancelFadeCallbacks()
        fadeMode = FadeMode.NONE
        fadeOnDone = null
        stopPlayer(player)
        player = null
        currentTrack = null
        desiredTrack = null
        pendingTrack = null
        currentVolume = 0f
    }
}
