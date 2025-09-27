package com.game.voicespells.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.game.voicespells.databinding.ActivityMainBinding
// import com.game.voicespells.ui.activities.GameActivity // Will be created later
// import com.game.voicespells.ui.activities.SpellSelectionActivity // Will be created later
// import com.game.voicespells.ui.activities.SettingsActivity // Will be created later

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allPermissionsGranted = true
            permissions.entries.forEach {
                if (!it.value) {
                    allPermissionsGranted = false
                }
            }
            if (allPermissionsGranted) {
                // Permissions granted, proceed with action that required permissions
                navigateToGameActivity()
            } else {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
                Toast.makeText(this, "Algumas permissões são necessárias para jogar.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonPlay.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.buttonConfigureSpells.setOnClickListener {
            // Navigate to SpellSelectionActivity - To be implemented
            // val intent = Intent(this, SpellSelectionActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Configurar Magias - A ser implementado", Toast.LENGTH_SHORT).show()
        }

        binding.buttonSettings.setOnClickListener {
            // Navigate to SettingsActivity - To be implemented
            // val intent = Intent(this, SettingsActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Configurações - A ser implementado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.INTERNET)
        }
        // Add other permissions like ACCESS_WIFI_STATE if explicitly needed for LAN discovery early on
        // For now, only RECORD_AUDIO and INTERNET as per core requirements for voice and potential online play.

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Permissions already granted
            navigateToGameActivity()
        }
    }

    private fun navigateToGameActivity() {
        // Placeholder for GameActivity navigation
        // val intent = Intent(this, GameActivity::class.java)
        // startActivity(intent)
        Toast.makeText(this, "Navegando para GameActivity (A ser implementado)", Toast.LENGTH_SHORT).show()
    }
}
