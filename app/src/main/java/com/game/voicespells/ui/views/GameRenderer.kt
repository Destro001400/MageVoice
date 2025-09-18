package com.game.voicespells.ui.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.SurfaceView
import androidx.core.content.res.ResourcesCompat
import com.game.voicespells.R
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.mechanics.Projectile
import com.game.voicespells.game.spells.Spell
import com.game.voicespells.utils.GameConfig
import com.game.voicespells.utils.Vector3

class GameRenderer(private val surfaceView: SurfaceView) {

    private val playerPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
    private val mapPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }
    private val obstaclePaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }
    private val hudTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }
    private val spellPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val obstacles = listOf(
        RectF(10f, 10f, 15f, 20f),
        RectF(30f, 25f, 40f, 30f)
    )

    fun draw(canvas: Canvas, player: Player, activeSpells: List<Pair<Spell, Vector3>>) {
        canvas.drawColor(Color.parseColor("#508050"))
        drawMap(canvas)
        drawObstacles(canvas)
        activeSpells.forEach { (spell, position) -> drawSpell(canvas, spell, position) }
        drawPlayer(canvas, player)
        drawHUD(canvas, player.hp, player.mana)
    }

    fun drawProjectiles(canvas: Canvas, projectiles: List<Projectile>) {
        val projPaint = Paint().apply { color = Color.MAGENTA; style = Paint.Style.FILL }
        projectiles.forEach { p ->
            val screenX = p.position.x * (canvas.width / GameConfig.MAP_SIZE)
            val screenY = p.position.z * (canvas.height / GameConfig.MAP_SIZE)
            canvas.drawCircle(screenX, screenY, 8f, projPaint)
        }
    }

    private fun drawPlayer(canvas: Canvas, player: Player) {
        val screenX = player.position.x * (canvas.width / GameConfig.MAP_SIZE)
        val screenY = player.position.z * (canvas.height / GameConfig.MAP_SIZE)
        val playerSize = 64f  // Size for player_sprite.xml
        
        canvas.save()
        canvas.translate(screenX - playerSize/2, screenY - playerSize/2)
        canvas.rotate(player.rotation, playerSize/2, playerSize/2)
        
        // Draw player using the vector drawable
        ResourcesCompat.getDrawable(surfaceView.context.resources, R.drawable.player_sprite, null)?.let { drawable ->
            drawable.setBounds(0, 0, playerSize.toInt(), playerSize.toInt())
            drawable.draw(canvas)
        }
        
        canvas.restore()
    }

    private fun drawSpell(canvas: Canvas, spell: Spell, position: Vector3) {
        val screenX = position.x * (canvas.width / GameConfig.MAP_SIZE)
        val screenY = position.z * (canvas.height / GameConfig.MAP_SIZE)
        val spellSize = 48f  // Size for spell_*.xml drawables
        
        // Get the appropriate spell drawable
        val drawableId = when (spell.name) {
            "Fireball" -> R.drawable.spell_fireball
            "Freeze" -> R.drawable.spell_freeze
            "Lightning" -> R.drawable.spell_lightning
            "Stone" -> R.drawable.spell_stone
            "Gust" -> R.drawable.spell_gust
            else -> R.drawable.spell_fireball // fallback
        }
        
        canvas.save()
        canvas.translate(screenX - spellSize/2, screenY - spellSize/2)
        
        ResourcesCompat.getDrawable(surfaceView.context.resources, drawableId, null)?.let { drawable ->
            drawable.setBounds(0, 0, spellSize.toInt(), spellSize.toInt())
            drawable.draw(canvas)
        }
        
        canvas.restore()
    }

    private fun drawMap(canvas: Canvas) {
        // placeholder
    }

    private fun drawObstacles(canvas: Canvas) {
        obstacles.forEach { obstacleRect ->
            val screenRect = RectF(
                obstacleRect.left * (canvas.width / GameConfig.MAP_SIZE),
                obstacleRect.top * (canvas.height / GameConfig.MAP_SIZE),
                obstacleRect.right * (canvas.width / GameConfig.MAP_SIZE),
                obstacleRect.bottom * (canvas.height / GameConfig.MAP_SIZE)
            )
            canvas.drawRect(screenRect, obstaclePaint)
        }
    }

    fun drawHUD(canvas: Canvas, hp: Int, mana: Int) {
        canvas.drawText("HP: $hp", 50f, 80f, hudTextPaint)
        canvas.drawText("Mana: $mana", canvas.width - 250f, 80f, hudTextPaint)
    }
}
