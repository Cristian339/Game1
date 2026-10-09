package com.cozy.herbshop.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()
    var soundEnabled: Boolean = true

    private val sfxMap = mapOf(
        "pour" to com.cozy.herbshop.R.raw.sfx_pour,
        "jar_select" to com.cozy.herbshop.R.raw.sfx_jar_select,
        "jar_click" to com.cozy.herbshop.R.raw.sfx_jar_click,
        "level_complete" to com.cozy.herbshop.R.raw.sfx_level_complete,
        "invalid" to com.cozy.herbshop.R.raw.sfx_invalid,
        "undo" to com.cozy.herbshop.R.raw.sfx_undo,
        "hint" to com.cozy.herbshop.R.raw.sfx_hint,
        "reveal" to com.cozy.herbshop.R.raw.sfx_reveal,
        "unfreeze" to com.cozy.herbshop.R.raw.sfx_unfreeze,
        "unseal" to com.cozy.herbshop.R.raw.sfx_unseal,
        "extra_jar" to com.cozy.herbshop.R.raw.sfx_extra_jar,
        "ui_click" to com.cozy.herbshop.R.raw.sfx_ui_click,
        "ui_back" to com.cozy.herbshop.R.raw.sfx_ui_back,
        "ui_toggle" to com.cozy.herbshop.R.raw.sfx_ui_toggle,
        "ui_confirm" to com.cozy.herbshop.R.raw.sfx_ui_confirm,
        "ui_error" to com.cozy.herbshop.R.raw.sfx_ui_error,
        "dialog_open" to com.cozy.herbshop.R.raw.sfx_dialog_open,
        "tab" to com.cozy.herbshop.R.raw.sfx_tab,
        "reward" to com.cozy.herbshop.R.raw.sfx_reward,
        "achievement" to com.cozy.herbshop.R.raw.sfx_achievement,
        "purchase" to com.cozy.herbshop.R.raw.sfx_purchase,
        "unlock" to com.cozy.herbshop.R.raw.sfx_unlock,
        "no_stamina" to com.cozy.herbshop.R.raw.sfx_no_stamina,
        // Stings musicales (one-shot) para los momentos de celebración
        "sting_recipe" to com.cozy.herbshop.R.raw.sting_recipe,
        "sting_level_complete" to com.cozy.herbshop.R.raw.sting_level_complete
    )

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()
        sfxMap.forEach { (name, resId) ->
            soundIds[name] = soundPool.load(context, resId, 1)
        }
    }

    private fun play(name: String, volume: Float = 1f, rate: Float = 1f) {
        if (!soundEnabled) return
        val id = soundIds[name] ?: return
        try {
            soundPool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
        } catch (_: Exception) {}
    }

    fun playJarClick() = play("jar_click")
    fun playJarSelect() = play("jar_select")
    fun playPourSound(volume: Float = 0.9f, rate: Float = 1f) = play("pour", volume, rate)

/**
 * Vertido grande (3 o mas unidades): mismo sonido base pero con tono mas grave y
 * mas cuerpo, para que se oiga el chorro lleno y no gotas sueltas.
 */
fun playPourBig(volume: Float = 0.95f, rate: Float = 0.62f) = play("pour", volume, rate)

/** Timbre cristalino del pack ASMR Cristal Pro al seleccionar frasco. */
fun playCrystalChime(volume: Float = 0.5f) = play("jar_select", volume, 1.85f)
    fun playLevelCompleteSound() = play("level_complete", 0.95f)
    fun playInvalidPour() = play("invalid", 0.7f)
    fun playUndo() = play("undo")
    fun playHint() = play("hint")
    fun playReveal() = play("reveal")
    fun playUnfreeze() = play("unfreeze")
    fun playUnseal() = play("unseal")
    fun playExtraJar() = play("extra_jar")
    fun playUiClick() = play("ui_click", 0.7f)
    fun playUiBack() = play("ui_back", 0.7f)
    fun playUiToggle() = play("ui_toggle", 0.7f)
    fun playUiConfirm() = play("ui_confirm", 0.8f)
    fun playUiError() = play("ui_error", 0.7f)
    fun playDialogOpen() = play("dialog_open", 0.7f)
    fun playTab() = play("tab", 0.7f)
    fun playReward() = play("reward", 0.85f)
    fun playAchievement() = play("achievement", 0.9f)
    fun playPurchase() = play("purchase", 0.85f)
    fun playUnlock() = play("unlock", 0.85f)
    fun playNoStamina() = play("no_stamina", 0.75f)

    /** Fanfarria musical al servir la receta en la tetera. */
    fun playStingRecipe() = play("sting_recipe", 1f)

    /** Fanfarria musical al superar el nivel. */
    fun playStingLevelComplete() = play("sting_level_complete", 1f)

    fun pause() {
        try { soundPool.autoPause() } catch (_: Exception) {}
    }

    fun resume() {
        try { soundPool.autoResume() } catch (_: Exception) {}
    }

    fun release() {
        soundPool.release()
    }
}
