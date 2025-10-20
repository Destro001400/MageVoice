package com.game.voicespells.presentation.activities

import android.content.Intent
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.game.voicespells.databinding.ActivityLobbyBinding
import com.game.voicespells.network.NetworkManager
import kotlinx.coroutines.launch
import java.util.UUID

class LobbyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLobbyBinding
    private lateinit var roomsAdapter: RoomsAdapter
    private val localPlayerId = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NetworkManager.init(this)

        setupRecyclerView()
        setupClickListeners()
        observeNetwork()

        // Start discovery immediately
        NetworkManager.startDiscovery()
    }

    private fun setupRecyclerView() {
        roomsAdapter = RoomsAdapter { serviceInfo ->
            // On room selected
            NetworkManager.connectToServer(serviceInfo, localPlayerId)
            navigateToGame("CLIENT")
        }
        binding.recyclerViewRooms.apply {
            layoutManager = LinearLayoutManager(this@LobbyActivity)
            adapter = roomsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonCreateRoom.setOnClickListener {
            NetworkManager.stopDiscovery() // Stop discovery before hosting
            NetworkManager.startServer(localPlayerId)
            navigateToGame("HOST")
        }

        binding.buttonRefreshRooms.setOnClickListener {
            NetworkManager.startDiscovery()
        }
        
        // The join button is not needed if selection is handled by the list
        binding.buttonJoinRoom.visibility = View.GONE
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            NetworkManager.discoveredServices.collect { services ->
                roomsAdapter.updateRooms(services)
            }
        }
    }

    private fun navigateToGame(role: String) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("PLAYER_ID", localPlayerId)
            putExtra("ROLE", role)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkManager.stopDiscovery()
    }
}

class RoomsAdapter(
    private val onRoomClick: (NsdServiceInfo) -> Unit
) : RecyclerView.Adapter<RoomsAdapter.RoomViewHolder>() {

    private val rooms = mutableListOf<NsdServiceInfo>()

    fun updateRooms(newRooms: List<NsdServiceInfo>) {
        rooms.clear()
        rooms.addAll(newRooms)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            textSize = 20f
            setPadding(16, 16, 16, 16)
        }
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.bind(room)
        holder.itemView.setOnClickListener { onRoomClick(room) }
    }

    override fun getItemCount() = rooms.size

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(serviceInfo: NsdServiceInfo) {
            (itemView as TextView).text = serviceInfo.serviceName
        }
    }
}