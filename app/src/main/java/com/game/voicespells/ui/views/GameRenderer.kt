package com.game.voicespells.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import android.view.SurfaceHolder
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.entities.Vector3
import com.game.voicespells.game.spells.Spell // Para desenhar efeitos de magias
import com.game.voicespells.utils.GameConfig

class GameRenderer(
    private val surfaceHolder: SurfaceHolder,
    private val context: Context // Necessário para carregar recursos, etc.
) {
    private val playerPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
    private val otherPlayerPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val mapBackgroundPaint = Paint().apply {
        color = Color.parseColor("#33691E") // Um verde escuro para o chão
    }
    private val mapGridPaint = Paint().apply {
        color = Color.parseColor("#4CAF50") // Linhas de grade verdes mais claras
        strokeWidth = 2f
    }
    private val hudTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f // Ajuste o tamanho conforme necessário
        isAntiAlias = true
    }
    private val hudBackgroundPaint = Paint().apply{
        color = Color.parseColor("#80000000") // Fundo semi-transparente para o HUD
    }

    private val TAG = "GameRenderer"

    // Dimensões do canvas (serão atualizadas em onSurfaceChanged)
    private var canvasWidth: Int = 0
    private var canvasHeight: Int = 0

    // Simples câmera/viewport (para transladar o mundo)
    private var cameraX: Float = 0f
    private var cameraY: Float = 0f // No nosso caso, o Y do canvas é o Z do mundo

    fun onSurfaceChanged(width: Int, height: Int) {
        canvasWidth = width
        canvasHeight = height
        Log.d(TAG, "Renderer surface changed: $width x $height")
    }

    fun drawFrame(localPlayer: Player, otherPlayers: List<Player>) {
        val canvas: Canvas? = try {
            surfaceHolder.lockCanvas()
        } catch (e: Exception) {
            Log.e(TAG, "Error locking canvas: ${e.message}")
            null
        }

        canvas?.let {
            try {
                // Limpar a tela
                it.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR) // Limpa com preto
                it.drawPaint(mapBackgroundPaint) // Desenha o fundo do mapa

                // Atualizar câmera para seguir o jogador local (centralizado)
                // O mundo se move na direção oposta à do jogador para mantê-lo centralizado
                cameraX = localPlayer.position.x - canvasWidth / 2f
                cameraY = localPlayer.position.z - canvasHeight / 2f // Usamos Z do jogador para o Y do canvas

                // Salvar estado do canvas antes de transladar para a câmera
                it.save()
                it.translate(-cameraX, -cameraY)

                // Desenhar o mapa e elementos do jogo
                drawMap(it)
                drawPlayer(it, localPlayer, isLocal = true)
                otherPlayers.forEach { player ->
                    drawPlayer(it, player, isLocal = false)
                }
                // drawSpellEffects(it, activeSpells) // Para mais tarde

                // Restaurar o canvas (remove a translação da câmera)
                it.restore()

                // Desenhar HUD (sobre tudo, sem translação de câmera)
                drawHUD(it, localPlayer.hp, localPlayer.mana)

            } finally {
                try {
                    surfaceHolder.unlockCanvasAndPost(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unlocking canvas: ${e.message}")
                }
            }
        }
    }

    private fun drawMap(canvas: Canvas) {
        // Desenha uma grade simples para o mapa
        // Os limites do mapa são definidos por GameConfig.MAP_SIZE
        val mapSize = GameConfig.MAP_SIZE
        val gridSize = 5f // Tamanho de cada célula da grade no mundo

        // Desenha linhas verticais
        var x = -mapSize / 2
        while (x <= mapSize / 2) {
            canvas.drawLine(x, -mapSize / 2, x, mapSize / 2, mapGridPaint)
            x += gridSize
        }
        // Desenha linhas horizontais
        var z = -mapSize / 2 // Usamos z como y no canvas
        while (z <= mapSize / 2) {
            canvas.drawLine(-mapSize / 2, z, mapSize / 2, z, mapGridPaint)
            z += gridSize
        }

        // TODO: Desenhar obstáculos
    }

    private fun drawPlayer(canvas: Canvas, player: Player, isLocal: Boolean) {
        val playerSize = 1.0f // Tamanho do jogador em unidades do mundo

        // A posição X e Z do jogador são usadas diretamente. Y é ignorado na renderização 2D.
        val screenX = player.position.x
        val screenY = player.position.z // Mapeia Z do mundo para Y do canvas

        val paintToUse = if (isLocal) playerPaint else otherPlayerPaint
        canvas.drawCircle(screenX, screenY, playerSize / 2, paintToUse) // Desenha jogador como um círculo

        // Desenhar indicador de rotação (uma linha simples)
        val lineLength = playerSize
        val angleRad = Math.toRadians(player.rotation.toDouble())
        // No canvas, o ângulo 0 é para a direita. No jogo, rotação 0 pode ser "para cima" (positivo Z).
        // Ajuste aqui se necessário. Assumindo que rotação 0 no Player é para o +Z (equivalente a 'para cima' no canvas padrão)
        // e que sin para X e cos para Z é a convenção para a frente.
        val lineEndX = screenX + lineLength * kotlin.math.sin(angleRad).toFloat()
        val lineEndY = screenY + lineLength * kotlin.math.cos(angleRad).toFloat() // Linha aponta na direção Z

        canvas.drawLine(screenX, screenY, lineEndX, lineEndY, hudTextPaint.apply { strokeWidth = 3f })
    }

    // Placeholder - será implementado depois
    fun drawSpell(spell: Spell, position: Vector3) {
        // Lógica para desenhar efeitos de magia (projéteis, áreas de efeito)
        // Ex: um círculo para explosão de Fireball, uma área azul para Freeze
    }

    private fun drawHUD(canvas: Canvas, hp: Int, mana: Int) {
        // Desenha um fundo semi-transparente para o HUD para melhor legibilidade
        // canvas.drawRect(10f, 10f, 350f, 120f, hudBackgroundPaint) // Exemplo de área de fundo

        // Usar os IDs dos TextViews é para a Activity. Aqui desenhamos diretamente no Canvas.
        val hpText = "HP: $hp"
        val manaText = "Mana: $mana"

        // Posição do texto do HUD (canto superior esquerdo)
        canvas.drawText(hpText, 30f, 60f, hudTextPaint)
        canvas.drawText(manaText, 30f, 110f, hudTextPaint)

        // TODO: Desenhar outros elementos do HUD (ex: ícones de magias selecionadas, cooldowns)
    }

    fun cleanup() {
        // Liberar recursos se necessário (ex: bitmaps carregados)
        Log.d(TAG, "Renderer cleanup.")
    }
}
