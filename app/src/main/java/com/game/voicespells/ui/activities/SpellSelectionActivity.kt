package com.game.voicespells.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.game.voicespells.R
import com.game.voicespells.game.spells.*
import com.game.voicespells.ui.adapters.SpellAdapter

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
    )

    private val selectedSpells = mutableListOf<Spell>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spell_selection)

        recyclerViewSpells = findViewById(R.id.recycler_view_spells)
        confirmButton = findViewById(R.id.button_confirm_spells)

        setupRecyclerView()

        confirmButton.setOnClickListener {
            if (selectedSpells.isNotEmpty()) {
                Toast.makeText(this, "Selected ${selectedSpells.size} spells: ${selectedSpells.joinToString { it.name }}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "No spells selected.", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerViewSpells.layoutManager = GridLayoutManager(this, 2)
        spellAdapter = SpellAdapter(allSpells) { spell -> handleSpellSelection(spell) }
        recyclerViewSpells.adapter = spellAdapter
    }

    private fun handleSpellSelection(spell: Spell) {
        if (selectedSpells.contains(spell)) {
            selectedSpells.remove(spell)
            Toast.makeText(this, "${spell.name} deselected", Toast.LENGTH_SHORT).show()
        } else {
            selectedSpells.add(spell)
            Toast.makeText(this, "${spell.name} selected", Toast.LENGTH_SHORT).show()
        }
    }
}
