package com.game.voicespells.game.spells

enum class SpellType(val command: String) {
    FIREBALL("fireball"),
    FREEZE("freeze"),
    LIGHTNING("lightning"),
    STONE("stone"),
    GUST("gust"),
    UNKNOWN("unknown"); // For unrecognized commands or errors

    companion object {
        fun fromCommand(command: String?): SpellType {
            return values().find { it.command == command?.lowercase() } ?: UNKNOWN
        }
    }
}
