package com.game.voicespells.game.entities

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.game.voicespells.game.spells.Spell
import com.game.voicespells.game.spells.SpellType
import com.game.voicespells.utils.GameConfig // Make sure GameConfig is created

data class Player(
    var position: Vector3,
    var rotation: Float, // Angle in degrees or radians, define convention
    var hp: Int = 100,
    var mana: Int = 100,
    var selectedSpells: List<Spell> = emptyList(), // Will be populated by SpellSelectionActivity
    val id: String // Unique player identifier
) {
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
        // Initialize selectedSpells with instances of the basic spells for now for testing
        // This would normally be handled by a spell selection screen and persistence
        // selectedSpells = listOf(Fireball(), Freeze(), Lightning(), Stone(), Gust())
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
            // Handle death logic (e.g., notify game manager, play animation)
            // For now, just set isAlive to false. Respawn logic will bring them back.
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
            // Optionally provide feedback to the player (e.g., sound effect, UI message)
            return
        }

        if (mana >= spellToCast.manaCost) {
            // Mana is deducted by the spell itself in its execute method,
            // but good to have a check here too or ensure spell's execute calls player.useMana()
            spellToCast.execute(this, targetPosition, allPlayers)
            if (mana < spellToCast.manaCost) { // Check if spell execution failed due to mana (if useMana is called inside execute)
                // This case should ideally be handled by spell.execute not proceeding or useMana returning false
            } else {
                 spellCooldowns[spellType] = now // Record successful cast time
            }
        } else {
            Log.d("Player", "$id cannot cast ${spellToCast.name}, not enough mana.")
            // Optionally provide feedback
        }
    }


    fun respawn(spawnPosition: Vector3 = Vector3(0f, 0f, 0f)) {
        Log.d("Player", "$id is respawning.")
        hp = maxHp
        mana = maxMana
        isAlive = true
        position = spawnPosition // Reset to a spawn point
        rotation = 0f // Reset rotation
        temporaryHp = 0
        temporaryHpExpiryTime = 0L
        slowPotency = 0f
        slowExpiryTime = 0L
        spellCooldowns.clear() // Reset all cooldowns on respawn
        // Clear other status effects
    }

    fun update(deltaTime: Float) {
        if (!isAlive) return

        // Regenerate HP (if not in combat, simple regen for now)
        if (hp < maxHp) {
            hp += (GameConfig.HP_REGEN * deltaTime).toInt()
            if (hp > maxHp) hp = maxHp
        }

        // Regenerate Mana
        if (mana < maxMana) {
            mana += (GameConfig.MANA_REGEN * deltaTime).toInt()
            if (mana > maxMana) mana = maxMana
        }

        // Update Status Effects
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


    // Called by Spells
    fun applyTemporaryHp(amount: Int, durationSeconds: Float) {
        temporaryHp = amount // This simple version replaces existing temp HP.
                            // A more complex one might stack or take the max.
        temporaryHpExpiryTime = System.currentTimeMillis() + (durationSeconds * 1000).toLong()
        Log.d("Player", "$id gained $amount temporary HP for $durationSeconds seconds.")

        // Ensure temporary HP removal after duration if not refreshed
        handler.removeCallbacksAndMessages(null) // Cancel previous expiry runnables for temp HP
        handler.postDelayed({
            if (System.currentTimeMillis() >= temporaryHpExpiryTime) {
                 if (temporaryHp > 0) Log.d("Player_TempHP_Handler", "$id temporary HP expired (via handler).")
                temporaryHp = 0 // Ensure it's cleared if update() hasn't caught it yet
            }
        }, (durationSeconds * 1000).toLong())
    }

    fun applySlowEffect(potency: Float, durationSeconds: Float) {
        slowPotency = potency.coerceIn(0f, 1f)
        slowExpiryTime = System.currentTimeMillis() + (durationSeconds * 1000).toLong()
        Log.d("Player", "$id slowed by ${potency*100}% for $durationSeconds seconds.")

        // Ensure slow removal after duration
        // Using a single handler for multiple effects could be complex.
        // The update() method is a more reliable way to manage expirations.
        // However, a handler can be used for immediate effect removal if needed.
    }

    fun applyKnockback(forceX: Float, forceZ: Float) {
        // This is a simplified instant knockback.
        // A real physics system would apply an impulse.
        // Ensure player stays within map bounds after knockback.
        val newX = position.x + forceX
        val newZ = position.z + forceZ

        // Clamp to map boundaries (assuming map centered at 0,0 and GameConfig.MAP_SIZE is full width/depth)
        val halfMapSize = GameConfig.MAP_SIZE / 2
        position.x = newX.coerceIn(-halfMapSize, halfMapSize)
        position.z = newZ.coerceIn(-halfMapSize, halfMapSize)

        Log.d("Player", "$id knocked back. New position approx: ${position.x}, ${position.z}")
    }

    // Basic movement - will be controlled by GameActivity touch controls
    fun move(deltaX: Float, deltaZ: Float, deltaTime: Float) {
        if (!isAlive) return

        val currentSpeed = GameConfig.PLAYER_SPEED * getSpeedMultiplier()
        val moveX = deltaX * currentSpeed * deltaTime
        val moveZ = deltaZ * currentSpeed * deltaTime

        val newX = position.x + moveX
        val newZ = position.z + moveZ

        val halfMapSize = GameConfig.MAP_SIZE / 2
        position.x = newX.coerceIn(-halfMapSize, halfMapSize)
        // Y position might be handled by gravity or terrain later
        position.z = newZ.coerceIn(-halfMapSize, halfMapSize)
    }

    fun lookAt(targetX: Float, targetZ: Float) {
        if (!isAlive) return
        // Calculate rotation based on where the player should look
        // This would typically update 'rotation' (e.g., Y-axis rotation)
        // For a top-down or 2.5D game, this might be simpler.
        // For full 3D, it involves atan2 or similar.
        val angle = kotlin.math.atan2(targetX - position.x, targetZ - position.z) * (180 / kotlin.math.PI)
        this.rotation = angle.toFloat()
    }

}
