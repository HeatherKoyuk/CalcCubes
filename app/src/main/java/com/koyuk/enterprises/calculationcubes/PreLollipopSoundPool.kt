package com.koyuk.enterprises.calculationcubes

import android.media.AudioManager
import android.media.SoundPool


internal object PreLollipopSoundPool {
    fun NewSoundPool(): SoundPool {
        return SoundPool(1, AudioManager.STREAM_MUSIC, 0)
    }
}