package com.game.voicespells.game.spells

import android.util.Log
import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3
import kotlin.math.sqrt

class Lightning : Spell() {
    override val name: String = "Lightning"
    override val manaCost: Int = 30
    override val damage: Int = 25
    override val cooldown: Float = 3.0f // Example cooldown
    override val voiceCommand: String = "lightning"
    override val spellType: SpellType = SpellType.LIGHTNING

    // Lightning é instantâneo. Pode atingir o alvo mais próximo em um cone,
    // ou um jogador especificamente alvejado, ou apenas um ponto causando dano em área.
    // Para simplificar, vamos assumir que atinge um único jogador alvo próximo a `targetPosition`.
    // Uma versão mais avançada poderia encadear para inimigos próximos ou ser uma AoE.
    private val hitRadius: Float = 1.5f // Raio para verificar em torno de targetPosition

    override fun execute(
        caster: Player,
        targetPosition: Vector3, // O ponto para onde o raio é direcionado
        playersInScene: List<Player>
    ) {
        if (caster.mana >= manaCost) {
            caster.useMana(manaCost)
            Log.d("Lightning", "${caster.id} casts Lightning towards $targetPosition!")

            // Encontra um jogador para atingir perto da targetPosition
            // Este é um direcionamento simplificado. Um jogo real pode ter mira automática ou exigir direcionamento preciso.
            var targetHit: Player? = null
            var minDistance = Float.MAX_VALUE

            playersInScene.forEach { player ->
                if (player.id != caster.id) {
                    val distance = calculateDistance(player.position, targetPosition)
                    if (distance < hitRadius && distance < minDistance) {
                        minDistance = distance
                        targetHit = player
                    }
                }
            }

            if (targetHit != null) {
                Log.d("Lightning", "Lightning strikes ${targetHit!!.id} for $damage damage!")
                targetHit!!.takeDamage(damage)
            } else {
                Log.d("Lightning", "Lightning missed or no target in range at $targetPosition.")
            }

        } else {
            Log.d("Lightning", "${caster.id} tried to cast Lightning but not enough mana.")
        }
    }

    private fun calculateDistance(v1: Vector3, v2: Vector3): Float {
        val dx = v1.x - v2.x
        val dy = v1.y - v2.y
        val dz = v1.z - v2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
