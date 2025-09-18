package com.game.voicespells.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.game.voicespells.game.spells.Spell
import com.game.voicespells.utils.Vector3

class AudioManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val loadedSounds = mutableMapOf<String, Int>() // Map spell name or sound key to SoundPool ID

    // Listener position for 3D audio calculations (simplified)
    private var listenerPosition: Vector3 = Vector3(0f, 0f, 0f)

    companion object {
        private const val TAG = "AudioManager"
        private const val MAX_STREAMS = 10
        private const val DEFAULT_SOUND_FALLOFF_DISTANCE = 20f // Game units for 3D audio volume calculation
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                Log.d(TAG, "Sound loaded successfully: $sampleId")
            } else {
                Log.e(TAG, "Error loading sound: $sampleId, status: $status")
            }
        }

        Log.d(TAG, "AudioManager initialized. Remember to load actual sound files.")
    }

    // Example function to load a sound - in real app, call this for each sound effect
    fun loadSound(soundKey: String, resourceId: Int): Int {
        val soundId = soundPool?.load(context, resourceId, 1) ?: 0
        if (soundId != 0) {
            loadedSounds[soundKey] = soundId
        }
        return soundId
    }

    private fun getSoundIdForSpell(spell: Spell): Int {
        val soundKey = when (spell.name.lowercase()) {
            "fireball" -> "fireball_sound"
            "freeze" -> "freeze_sound"
            "lightning" -> "lightning_sound"
            "stone" -> "stone_shield_sound"
            "gust" -> "gust_sound"
            else -> "default_spell_sound"
        }
        return loadedSounds[soundKey] ?: 0
    }

    fun playSpellSound(spell: Spell, position: Vector3) {
        val soundId = getSoundIdForSpell(spell)
        if (soundId == 0) {
            Log.w(TAG, "Sound not loaded for spell: ${spell.name}")
            return
        }
        play3DAudio(soundId, position)
    }

    fun play3DAudio(soundPoolId: Int, soundPosition: Vector3) {
        if (soundPoolId == 0) return

        val distance = calculateDistance(listenerPosition, soundPosition)

        var volume = 1.0f - (distance / DEFAULT_SOUND_FALLOFF_DISTANCE)
        volume = volume.coerceIn(0f, 1f)

        var leftVolume = volume
        var rightVolume = volume

        val dx = soundPosition.x - listenerPosition.x
        val panFactor = (dx / (DEFAULT_SOUND_FALLOFF_DISTANCE / 2f)).coerceIn(-1f, 1f)

        if (panFactor < 0) {
            rightVolume *= (1.0f + panFactor)
        } else {
            leftVolume *= (1.0f - panFactor)
        }

        leftVolume = leftVolume.coerceIn(0f, 1f)
        rightVolume = rightVolume.coerceIn(0f, 1f)

        Log.d(TAG, "Playing sound $soundPoolId at $soundPosition, distance: $distance, calculated L/R volume: $leftVolume/$rightVolume")
        soundPool?.play(soundPoolId, leftVolume, rightVolume, 1, 0, 1.0f)
    }

    fun setListenerPosition(position: Vector3) {
        this.listenerPosition = position
    }

    private fun calculateDistance(pos1: Vector3, pos2: Vector3): Float {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        val dz = pos1.z - pos2.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loadedSounds.clear()
        Log.d(TAG, "AudioManager released.")
    }
}
