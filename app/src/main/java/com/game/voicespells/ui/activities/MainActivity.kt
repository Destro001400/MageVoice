package com.game.voicespells.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.game.voicespells.R // Assuming R is in com.game.voicespells

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val playButton: Button = findViewById(R.id.button_play)
        val configureSpellsButton: Button = findViewById(R.id.button_configure_spells)
        val settingsButton: Button = findViewById(R.id.button_settings)

        checkAndRequestPermissions()

        playButton.setOnClickListener {
            if (hasPermissions()) {
                // Navigate to GameActivity - Placeholder for now
                // val intent = Intent(this, GameActivity::class.java)
                // startActivity(intent)
                Toast.makeText(this, "Navigating to GameActivity...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required to play", Toast.LENGTH_LONG).show()
                checkAndRequestPermissions() // Prompt again if not granted
            }
        }

        configureSpellsButton.setOnClickListener {
            // Navigate to SpellSelectionActivity - Placeholder for now
            // val intent = Intent(this, SpellSelectionActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Navigating to Spell Configuration...", Toast.LENGTH_SHORT).show()
        }

        settingsButton.setOnClickListener {
            // Navigate to SettingsActivity - Placeholder for now
            // val intent = Intent(this, SettingsActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Navigating to Settings...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.INTERNET)
        }
        // Add other permissions like ACCESS_WIFI_STATE if needed for LAN directly here

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            var allPermissionsGranted = true
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                Toast.makeText(this, "Permissions Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions were denied. The app might not function correctly.", Toast.LENGTH_LONG).show()
                // Handle cases where permissions are denied, e.g., disable features or explain why they are needed.
            }
        }
    }
}
