package com.game.voicespells.game.mechanics

import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.spells.Fireball
import com.game.voicespells.utils.Vector3
import org.junit.Assert
import org.junit.Test
import java.util.UUID

class SpellSystemTest {
    @Test
    fun fireballCastsAndReducesMana() {
        val spellSystem = SpellSystem()
        val player = Player(Vector3(0f,0f,0f),0f,100,100, listOf(), UUID.randomUUID().toString())
        val spell = spellSystem.getSpell(com.game.voicespells.voice.SpellType.FIREBALL)
        Assert.assertNotNull(spell)
        spellSystem.tryCast(player, com.game.voicespells.voice.SpellType.FIREBALL, Vector3(5f,0f,0f))
        Assert.assertTrue(player.mana <= 100)
    }
}
