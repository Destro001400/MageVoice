package com.game.voicespells.utils

object GameConfig {
    const val MAX_PLAYERS = 10
    const val MAP_SIZE = 50f // Usado para limites do mapa
    const val PLAYER_SPEED = 5f // Unidades por segundo
    const val RESPAWN_TIME = 10f // Segundos
    const val HP_REGEN = 5f // HP por segundo (idealmente fora de combate)
    const val MANA_REGEN = 10f // Mana por segundo
    const val FRIENDLY_FIRE_DAMAGE = 0.5f // Multiplicador para fogo amigo (0 = desligado, 1 = total)

    // Relacionado ao Reconhecimento de Voz
    const val VOICE_RECOGNITION_TIMEOUT_MS = 3000L // Milissegundos

    // Cooldowns de Magias (exemplos, podem ser movidos para cada magia individualmente se preferir)
    // const val FIREBALL_COOLDOWN = 2.0f
    // const val FREEZE_COOLDOWN = 5.0f
    // const val LIGHTNING_COOLDOWN = 3.0f
    // const val STONE_COOLDOWN = 10.0f
    // const val GUST_COOLDOWN = 4.0f
}
