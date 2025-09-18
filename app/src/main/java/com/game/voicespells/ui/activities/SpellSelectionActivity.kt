package com.game.voicespells.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.game.voicespells.R
import com.game.voicespells.game.spells.*
import com.game.voicespells.ui.adapters.SpellAdapter
import java.util.ArrayList // For selected spell IDs if passing back

class SpellSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerViewSpells: RecyclerView
    private lateinit var spellAdapter: SpellAdapter
    private lateinit var confirmButton: Button

    private val allSpells = listOf(
        Fireball(),
        Freeze(),
        Lightning(),
        Stone(),
        Gust()
        // TODO: Add more spells and categorize them as per full requirements
    )

    private val selectedSpells = mutableListOf<Spell>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spell_selection)

        recyclerViewSpells = findViewById(R.id.recycler_view_spells)
        confirmButton = findViewById(R.id.button_confirm_spells)

        setupRecyclerView()

        confirmButton.setOnClickListener {
            // Pass selected spell information back to the calling activity or save preferences
            // For now, just show a toast and finish
            if (selectedSpells.isNotEmpty()) {
                Toast.makeText(this, "Selected ${selectedSpells.size} spells: ${selectedSpells.joinToString { it.name }}", Toast.LENGTH_LONG).show()
                // Example: Pass back to MainActivity or GameActivity if started for result
                // val resultIntent = Intent()
                // resultIntent.putExtra("selected_spell_names", ArrayList(selectedSpells.map { it.name }))
                // setResult(Activity.RESULT_OK, resultIntent)
            } else {
                Toast.makeText(this, "No spells selected.", Toast.LENGTH_SHORT).show()
                // setResult(Activity.RESULT_CANCELED)
            }
            finish() // Close the activity
        }
    }

    private fun setupRecyclerView() {
        // Using a GridLayoutManager for a grid display, e.g., 2 columns
        recyclerViewSpells.layoutManager = GridLayoutManager(this, 2)
        
        spellAdapter = SpellAdapter(allSpells) { spell ->
            handleSpellSelection(spell)
        }
        recyclerViewSpells.adapter = spellAdapter
    }

    private fun handleSpellSelection(spell: Spell) {
        // Example selection logic: toggle selection
        if (selectedSpells.contains(spell)) {
            selectedSpells.remove(spell)
            Toast.makeText(this, "${spell.name} deselected", Toast.LENGTH_SHORT).show()
        } else {
            // Implement category limits or total limits if needed
            // For now, allow selecting multiple
            selectedSpells.add(spell)
            Toast.makeText(this, "${spell.name} selected", Toast.LENGTH_SHORT).show()
        }
        // TODO: Update adapter to visually reflect selection if desired
        // spellAdapter.notifyDataSetChanged() // Or more specific notifyItemChanged
    }
}
