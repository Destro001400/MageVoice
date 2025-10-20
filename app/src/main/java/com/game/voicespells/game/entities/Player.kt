package com.game.voicespells.game.entities

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.game.voicespells.game.spells.Spell
import com.game.voicespells.game.spells.SpellType
import com.game.voicespells.utils.GameConfig
import com.game.voicespells.utils.Vector3 // CORRECTED IMPORT

data class Player(
    var position: Vector3,
    var rotation: Float, // Angle in degrees or radians, define convention
    var hp: Int = 100,
    var mana: Int = 100,
    var selectedSpells: List<Spell> = emptyList(),
    val id: String // Unique player identifier
) {
    // ... (rest of the file is unchanged)
    private val maxHp: Int = 100
    private val maxMana: Int = 100
    private var isAlive: Boolean = true

    // Cooldown tracking for spells
    private val spellCooldowns = mutableMapOf<SpellType, Long>() // SpellType to System.currentTimeMillis() of last cast

    // Status Effects (Simplified)
    private var temporaryHp: Int = 0
    private var temporaryHpExpiryTime: Long = 0L
    private var slowPotency: Float = 0f // 0.0 to 1.0 (e.g., 0.5 for 50% slow)
    private var slowExpiryTime: Long = 0L

    private val handler = Handler(Looper.getMainLooper()) // For timed effects removal

    init {
    }

    fun takeDamage(amount: Int) {
        if (!isAlive) return

        var actualDamage = amount
        if (temporaryHp > 0) {
            if (temporaryHp >= actualDamage) {
                temporaryHp -= actualDamage
                Log.d("Player", "$id absorbed $actualDamage damage with temporary HP. Temp HP left: $temporaryHp")
                actualDamage = 0
            } else {
                actualDamage -= temporaryHp
                temporaryHp = 0
                Log.d("Player", "$id absorbed part of damage with temporary HP. Temp HP depleted.")
            }
        }

        if (actualDamage > 0) {
            hp -= actualDamage
            Log.d("Player", "$id took $actualDamage damage. HP left: $hp")
        }

        if (hp <= 0) {
            hp = 0
            isAlive = false
            Log.d("Player", "$id has been defeated.")
        }
    }

    fun useMana(amount: Int): Boolean {
        if (mana >= amount) {
            mana -= amount
            Log.d("Player", "$id used $amount mana. Mana left: $mana")
            return true
        }
        Log.d("Player", "$id failed to use $amount mana. Not enough mana (has $mana).")
        return false
    }

    fun castSpell(spellToCast: Spell, targetPosition: Vector3, allPlayers: List<Player>) {
        if (!isAlive) {
            Log.d("Player", "$id cannot cast spell, is not alive.")
            return
        }

        val spellType = spellToCast.spellType
        val now = System.currentTimeMillis()
        val lastCastTime = spellCooldowns[spellType] ?: 0L

        if (now < lastCastTime + (spellToCast.cooldown * 1000).toLong()) {
            Log.d("Player", "$id cannot cast ${spellToCast.name}, spell on cooldown.")
            return
        }

        if (mana >= spellToCast.manaCost) {
            spellToCast.execute(this, targetPosition, allPlayers)
            if (mana < spellToCast.manaCost) {
            } else {
                 spellCooldowns[spellType] = now
            }
        } else {
            Log.d("Player", "$id cannot cast ${spellToCast.name}, not enough mana.")
        }
    }


    fun respawn(spawnPosition: Vector3 = Vector3(0f, 0f, 0f)) {
        Log.d("Player", "$id is respawning.")
        hp = maxHp
        mana = maxMana
        isAlive = true
        position = spawnPosition
        rotation = 0f
        temporaryHp = 0
        temporaryHpExpiryTime = 0L
        slowPotency = 0f
        slowExpiryTime = 0L
        spellCooldowns.clear()
    }

    fun update(deltaTime: Float) {
        if (!isAlive) return

        if (hp < maxHp) {
            hp += (GameConfig.HP_REGEN * deltaTime).toInt()
            if (hp > maxHp) hp = maxHp
        }

        if (mana < maxMana) {
            mana += (GameConfig.MANA_REGEN * deltaTime).toInt()
            if (mana > maxMana) mana = maxMana
        }

        val currentTime = System.currentTimeMillis()
        if (temporaryHp > 0 && currentTime >= temporaryHpExpiryTime) {
            Log.d("Player", "$id temporary HP expired.")
            temporaryHp = 0
            temporaryHpExpiryTime = 0L
        }
        if (slowPotency > 0f && currentTime >= slowExpiryTime) {
            Log.d("Player", "$id slow effect expired.")
            slowPotency = 0f
            slowExpiryTime = 0L
        }
    }

    fun getSpeedMultiplier(): Float {
        return if (slowPotency > 0f && System.currentTimeMillis() < slowExpiryTime) {
            1.0f - slowPotency
        } else {
            1.0f
        }
    }


    fun applyTemporaryHp(amount: Int, durationSeconds: Float) {
        temporaryHp = amount
        temporaryHpExpiryTime = System.currentTimeMillis() + (durationSeconds * 1000).toLong()
        Log.d("Player", "$id gained $amount temporary HP for $durationSeconds seconds.")

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (System.currentTimeMillis() >= temporaryHpExpiryTime) {
                 if (temporaryHp > 0) Log.d("Player_TempHP_Handler", "$id temporary HP expired (via handler).")
                temporaryHp = 0
            }
        }, (durationSeconds * 1000).toLong())
    }

    fun applySlowEffect(potency: Float, durationSeconds: Float) {
        slowPotency = potency.coerceIn(0f, 1f)
        slowExpiryTime = System.currentTimeMillis() + (durationSeconds * 1000).toLong()
        Log.d("Player", "$id slowed by ${potency*100}% for $durationSeconds seconds.")
    }

    fun applyKnockback(forceX: Float, forceZ: Float) {
        val newX = position.x + forceX
        val newZ = position.z + forceZ

        val halfMapSize = GameConfig.MAP_SIZE / 2
        position.x = newX.coerceIn(-halfMapSize, halfMapSize)
        position.z = newZ.coerceIn(-halfMapSize, halfMapSize)

        Log.d("Player", "$id knocked back. New position approx: ${position.x}, ${position.z}")
    }

    fun move(deltaX: Float, deltaZ: Float, deltaTime: Float) {
        if (!isAlive) return

        val currentSpeed = GameConfig.PLAYER_SPEED * getSpeedMultiplier()
        val moveX = deltaX * currentSpeed * deltaTime
        val moveZ = deltaZ * currentSpeed * deltaTime

        val newX = position.x + moveX
        val newZ = position.z + moveZ

        val halfMapSize = GameConfig.MAP_SIZE / 2
        position.x = newX.coerceIn(-halfMapSize, halfMapSize)
        position.z = newZ.coerceIn(-halfMapSize, halfMapSize)
    }

    fun lookAt(targetX: Float, targetZ: Float) {
        if (!isAlive) return
        val angle = kotlin.math.atan2(targetX - position.x, targetZ - position.z) * (180 / kotlin.math.PI)
        this.rotation = angle.toFloat()
    }

}