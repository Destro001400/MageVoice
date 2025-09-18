package com.game.voicespells.ui.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.SurfaceView
import com.game.voicespells.game.entities.Player
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
        color = Color.RED // Default spell color
        style = Paint.Style.FILL
    }

    // Example obstacles (static for now)
    private val obstacles = listOf(
        RectF(10f, 10f, 15f, 20f), // x1, y1, x2, y2 in game units
        RectF(30f, 25f, 40f, 30f)
    )

    fun draw(canvas: Canvas, player: Player, activeSpells: List<Pair<Spell, Vector3>>) {
        // Clear screen
        canvas.drawColor(Color.parseColor("#508050")) // A greenish ground color

        drawMap(canvas)
        drawObstacles(canvas) // Draw obstacles on the map

        // Draw spells currently active on the field
        activeSpells.forEach { (spell, position) ->
            drawSpell(canvas, spell, position)
        }

        drawPlayer(canvas, player)
        drawHUD(canvas, player.hp, player.mana)
    }

    private fun drawPlayer(canvas: Canvas, player: Player) {
        // Convert game coordinates to screen coordinates
        val screenX = player.position.x * (canvas.width / GameConfig.MAP_SIZE)
        val screenY = player.position.z * (canvas.height / GameConfig.MAP_SIZE) // Using Z for screen Y
        val playerSize = 20f // pixels

        canvas.save()
        canvas.translate(screenX, screenY)
        canvas.rotate(player.rotation) // Apply player rotation
        // Draw a triangle or a simple shape indicating direction
        val path = android.graphics.Path()
        path.moveTo(0f, -playerSize / 1.5f) // Tip of triangle
        path.lineTo(playerSize / 2f, playerSize / 2f)
        path.lineTo(-playerSize / 2f, playerSize / 2f)
        path.close()
        canvas.drawPath(path, playerPaint)
        canvas.restore()
    }

    // This method is for drawing spell effects on the ground or in the world
    private fun drawSpell(canvas: Canvas, spell: Spell, position: Vector3) {
        val screenX = position.x * (canvas.width / GameConfig.MAP_SIZE)
        val screenY = position.z * (canvas.height / GameConfig.MAP_SIZE)
        val spellEffectSize = 15f // pixels, can vary by spell

        // Customize drawing based on spell type if needed
        // For now, a generic circle
        when (spell.name) {
            "Fireball" -> spellPaint.color = Color.RED
            "Freeze" -> spellPaint.color = Color.CYAN
            "Lightning" -> spellPaint.color = Color.YELLOW
            // Add other spell colors as needed
            else -> spellPaint.color = Color.MAGENTA
        }
        canvas.drawCircle(screenX, screenY, spellEffectSize, spellPaint)
    }

    private fun drawMap(canvas: Canvas) {
        // Draw the game map boundaries (e.g., a large rectangle)
        // GameConfig.MAP_SIZE is in game units. We scale it to canvas size.
        // For simplicity, map is just a background color, actual terrain/details can be added.
        // No specific drawing needed if the background color in draw() serves as the map base.
        // If you had a tiled map or specific map features, they would be drawn here.
    }

    private fun drawObstacles(canvas: Canvas) {
        obstacles.forEach { obstacleRect ->
            val screenRect = RectF(
                obstacleRect.left * (canvas.width / GameConfig.MAP_SIZE),
                obstacleRect.top * (canvas.height / GameConfig.MAP_SIZE), // map Z to screen Y
                obstacleRect.right * (canvas.width / GameConfig.MAP_SIZE),
                obstacleRect.bottom * (canvas.height / GameConfig.MAP_SIZE) // map Z to screen Y
            )
            canvas.drawRect(screenRect, obstaclePaint)
        }
    }


    fun drawHUD(canvas: Canvas, hp: Int, mana: Int) {
        // This will draw HUD directly on the canvas. GameActivity can choose to use this
        // or its XML TextViews. If using this, you might hide the XML TextViews.
        canvas.drawText("HP: $hp", 50f, 80f, hudTextPaint)
        canvas.drawText("Mana: $mana", canvas.width - 250f, 80f, hudTextPaint)
    }
}
