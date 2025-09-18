package com.game.voicespells.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.game.voicespells.R
import com.game.voicespells.game.spells.Spell

class SpellAdapter(
    private val spells: List<Spell>,
    private val onSpellClickListener: (Spell) -> Unit
) : RecyclerView.Adapter<SpellAdapter.SpellViewHolder>() {

    // Keep track of selected spells if needed, for now just click listener
    private val selectedSpells = mutableSetOf<Spell>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpellViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_spell, parent, false)
        return SpellViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpellViewHolder, position: Int) {
        val spell = spells[position]
        holder.bind(spell, onSpellClickListener)
        // Update UI based on selection state if selection is handled here
        // holder.itemView.alpha = if (selectedSpells.contains(spell)) 1.0f else 0.5f 
    }

    override fun getItemCount(): Int = spells.size

    class SpellViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val spellIcon: ImageView = itemView.findViewById(R.id.image_view_spell_icon)
        private val spellName: TextView = itemView.findViewById(R.id.text_view_spell_name)
        private val spellDescription: TextView = itemView.findViewById(R.id.text_view_spell_description)

        fun bind(spell: Spell, clickListener: (Spell) -> Unit) {
            spellName.text = spell.name
            val description = "Cost: ${spell.manaCost} Mana, Dmg: ${spell.damage}, CD: ${spell.cooldown}s"
            spellDescription.text = description

            // Set a placeholder icon, or load dynamically based on spell
            // e.g., when (spell) { is Fireball -> spellIcon.setImageResource(R.drawable.ic_fireball) ... }
            spellIcon.setImageResource(android.R.drawable.star_on) // Placeholder icon
            
            itemView.setOnClickListener {
                clickListener(spell)
                // Example of toggling selection visual state - this would require more logic
                // if (selectedSpells.contains(spell)) {
                //     selectedSpells.remove(spell)
                //     itemView.alpha = 0.5f
                // } else {
                //     selectedSpells.add(spell)
                //     itemView.alpha = 1.0f
                // }
            }
        }
    }
}
