package com.game.voicespells.game.entities

import com.game.voicespells.utils.Vector3
import org.junit.Assert
import org.junit.Test
import java.util.UUID

class PlayerTest {
    @Test
    fun damageAndRespawn() {
        val player = Player(Vector3(0f,0f,0f),0f,10,50, listOf(), UUID.randomUUID().toString())
        player.takeDamage(15)
        Assert.assertTrue(player.hp <= 0 || player.hp == 0)
    }
}
