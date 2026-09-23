package com.cozy.herbshop.data.audio

import android.content.Context

object AudioHub {
    lateinit var sound: SoundManager
        private set
    lateinit var music: MusicManager
        private set
    lateinit var ambient: AmbientManager
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        sound = SoundManager(context.applicationContext)
        music = MusicManager(context.applicationContext)
        ambient = AmbientManager(context.applicationContext)
        initialized = true
    }

    fun release() {
        if (!initialized) return
        sound.release()
        music.release()
        ambient.release()
        initialized = false
    }

    fun pauseAll() {
        if (!initialized) return
        sound.pause()
        music.pause()
        ambient.pause()
    }

    fun resumeAll() {
        if (!initialized) return
        sound.resume()
        music.resume()
        ambient.resume()
    }
}
