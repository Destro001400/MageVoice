package com.game.voicespells.presentation.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.games.app.GameActivity
import com.game.voicespells.databinding.ActivityGameBinding
import com.game.voicespells.presentation.viewmodels.GameMode
import com.game.voicespells.presentation.viewmodels.GameViewModel

class GameActivity : GameActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    // --- JNI Bridge ---
    private external fun updatePlayers(playerData: FloatArray)
    private external fun updateProjectiles(projectileData: FloatArray)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ... */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.buttonHost.setOnClickListener { viewModel.hostGame() }
        binding.buttonJoin.setOnClickListener { viewModel.joinGame() }
        binding.buttonMic.setOnClickListener { checkPermissionAndStartVoiceRecognition() }
        binding.viewJoystick.setOnMoveListener { angle, strength -> viewModel.movePlayer(angle, strength) }
    }

    private fun observeViewModel() {
        viewModel.gameMode.observe(this) { mode ->
            val isGameplayVisible = mode == GameMode.HOST || mode == GameMode.CLIENT
            binding.gameplayLayout.visibility = if (isGameplayVisible) View.VISIBLE else View.GONE
            binding.networkLayout.visibility = if (isGameplayVisible) View.GONE else View.VISIBLE
        }

        viewModel.players.observe(this) { players ->
            val playerData = FloatArray(players.size * 3)
            players.values.forEachIndexed { index, player ->
                val baseIndex = index * 3
                playerData[baseIndex] = player.id.hashCode().toFloat() // Using hashcode for a float ID
                playerData[baseIndex + 1] = player.x
                playerData[baseIndex + 2] = player.y
            }
            updatePlayers(playerData)
        }

        viewModel.projectiles.observe(this) { projectiles ->
            val projectileData = FloatArray(projectiles.size * 4)
            projectiles.forEachIndexed { index, projectile ->
                val baseIndex = index * 4
                projectileData[baseIndex] = projectile.id.toFloat()
                projectileData[baseIndex + 1] = projectile.x
                projectileData[baseIndex + 2] = projectile.y
                projectileData[baseIndex + 3] = if (projectile.type == "Fireball") 1.0f else 0.0f
            }
            updateProjectiles(projectileData)
        }

        viewModel.voiceStatus.observe(this) { status ->
            binding.textViewVoiceStatus.text = status
        }
    }

    private fun checkPermissionAndStartVoiceRecognition() { /* ... */ }

    companion object {
        init { System.loadLibrary("magevoice") }
    }
}
