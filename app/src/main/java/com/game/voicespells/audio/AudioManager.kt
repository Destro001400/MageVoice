package com.game.voicespells.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.game.voicespells.game.spells.Spell // Assuming Spell class is available
import com.game.voicespells.utils.Vector3

// Assuming R.raw.some_sound_id would exist for actual sounds
// object SoundResources {
//     const val FIREBALL_SOUND_ID = R.raw.fireball
//     const val FREEZE_SOUND_ID = R.raw.freeze
//     // etc.
// }

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

        // Preload some example sounds (these would be actual resource IDs)
        // For now, using placeholder names for keys. Actual sounds need to be added to res/raw
        // loadSound("fireball_sound", R.raw.fireball_sound_resource) // Example
        // loadSound("freeze_sound", R.raw.freeze_sound_resource)
        // Since we don't have R.raw, this will be symbolic for now
        Log.d(TAG, "AudioManager initialized. Remember to load actual sound files.")
    }

    // Example function to load a sound - in real app, call this for each sound effect
    private fun loadSound(soundKey: String, resourceId: Int): Int {
        val soundId = soundPool?.load(context, resourceId, 1) ?: 0
        if (soundId != 0) {
            loadedSounds[soundKey] = soundId
        }
        return soundId
    }

    private fun getSoundIdForSpell(spell: Spell): Int {
        // This mapping would be more robust, e.g., based on spell.id or a dedicated field
        val soundKey = when (spell.name.lowercase()) {
            "fireball" -> "fireball_sound" // These keys must match what was used in loadSound
            "freeze" -> "freeze_sound"
            "lightning" -> "lightning_sound"
            "stone" -> "stone_shield_sound"
            "gust" -> "gust_sound"
            else -> "default_spell_sound"
        }
        return loadedSounds[soundKey] ?: 0 // Return 0 if not found (or a default sound ID)
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

        // Simplified 3D audio: calculate volume based on distance
        // For true 3D audio, you'd also need panning (left/right balance)
        // Android's SoundPool has limited support for true 3D positioning directly.
        // For more advanced 3D audio, consider using OpenSL ES or a game engine's audio system.

        val distance = calculateDistance(listenerPosition, soundPosition)

        // Volume falls off with distance. Max volume = 1.0, Min volume = 0.0
        var volume = 1.0f - (distance / DEFAULT_SOUND_FALLOFF_DISTANCE)
        volume = volume.coerceIn(0f, 1f)

        // Panning (simplified: left/right balance based on X coordinate relative to listener)
        // This is a very basic simulation. SoundPool setVolume takes (leftVolume, rightVolume)
        var leftVolume = volume
        var rightVolume = volume

        val dx = soundPosition.x - listenerPosition.x
        // If sound is to the left of listener, reduce right volume, and vice versa.
        // Max pan effect when dx is around half the falloff distance
        val panFactor = (dx / (DEFAULT_SOUND_FALLOFF_DISTANCE / 2f)).coerceIn(-1f, 1f)

        if (panFactor < 0) { // Sound is to the left
            rightVolume *= (1.0f + panFactor) // panFactor is negative, so this reduces volume
        } else { // Sound is to the right
            leftVolume *= (1.0f - panFactor)
        }
        
        leftVolume = leftVolume.coerceIn(0f, 1f)
        rightVolume = rightVolume.coerceIn(0f, 1f)

        Log.d(TAG, "Playing sound $soundPoolId at $soundPosition, distance: $distance, calculated L/R volume: $leftVolume/$rightVolume")
        soundPool?.play(soundPoolId, leftVolume, rightVolume, 1, 0, 1.0f)
    }

    fun setListenerPosition(position: Vector3) {
        this.listenerPosition = position
        // Log.d(TAG, "Listener position set to: $position")
    }

    private fun calculateDistance(pos1: Vector3, pos2: Vector3): Float {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y // If your game is 2.5D on XZ plane, Y might be ignored or handled differently
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
