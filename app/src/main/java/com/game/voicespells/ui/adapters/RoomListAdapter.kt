package com.game.voicespells.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.game.voicespells.R
import com.game.voicespells.core.network.LanRoomManager

class RoomListAdapter(
    private var rooms: List<LanRoomManager.Room>,
    private val onRoomSelected: (LanRoomManager.Room) -> Unit
) : RecyclerView.Adapter<RoomListAdapter.RoomViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roomName: TextView = itemView.findViewById(R.id.textRoomName)
        val playerCount: TextView = itemView.findViewById(R.id.textPlayerCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.roomName.text = room.name
        holder.playerCount.text = "Jogadores: ${room.players.size}"
        holder.itemView.isSelected = position == selectedPosition
        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onRoomSelected(room)
        }
    }

    override fun getItemCount(): Int = rooms.size

    fun updateRooms(newRooms: List<LanRoomManager.Room>) {
        rooms = newRooms
        notifyDataSetChanged()
    }

    fun getSelectedRoom(): LanRoomManager.Room? =
        if (selectedPosition != RecyclerView.NO_POSITION) rooms[selectedPosition] else null
}
