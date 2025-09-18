package com.game.voicespells.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.game.voicespells.R
import com.game.voicespells.core.network.LanRoomManager
import com.game.voicespells.ui.adapters.RoomListAdapter

class LobbyActivity : AppCompatActivity() {
    private lateinit var roomManager: LanRoomManager
    private lateinit var roomList: RecyclerView
    private lateinit var createRoomBtn: Button
    private lateinit var refreshBtn: Button
    private lateinit var joinRoomBtn: Button
    private lateinit var chatInput: EditText
    private lateinit var chatView: TextView
    private lateinit var playerListView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var roomAdapter: RoomListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        roomManager = LanRoomManager(this)
        roomList = findViewById(R.id.recyclerViewRooms)
        createRoomBtn = findViewById(R.id.buttonCreateRoom)
        refreshBtn = findViewById(R.id.buttonRefreshRooms)
        joinRoomBtn = findViewById(R.id.buttonJoinRoom)
        chatInput = findViewById(R.id.editTextChat)
        chatView = findViewById(R.id.textViewChat)
        playerListView = findViewById(R.id.textViewPlayers)
        progressBar = findViewById(R.id.progressBarLobby)

        roomList.layoutManager = LinearLayoutManager(this)
        roomAdapter = RoomListAdapter(emptyList()) { room ->
            updatePlayerList(room)
        }
        roomList.adapter = roomAdapter

        createRoomBtn.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            roomManager.createRoom { success ->
                progressBar.visibility = View.GONE
                if (success) refreshRooms() else showError("Falha ao criar sala")
            }
        }
        refreshBtn.setOnClickListener { refreshRooms() }
        joinRoomBtn.setOnClickListener { joinSelectedRoom() }
        chatInput.setOnEditorActionListener { _, _, _ ->
            sendChatMessage(chatInput.text.toString())
            chatInput.text.clear()
            true
        }
        refreshRooms()
    }

    private fun refreshRooms() {
        progressBar.visibility = View.VISIBLE
        roomManager.getAvailableRooms { rooms ->
            progressBar.visibility = View.GONE
            roomAdapter.updateRooms(rooms)
            if (rooms.isNotEmpty()) updatePlayerList(rooms[0])
            else playerListView.text = "Jogadores:"
        }
    }

    private fun joinSelectedRoom() {
        val selectedRoom = roomAdapter.getSelectedRoom()
        if (selectedRoom == null) {
            showError("Selecione uma sala!")
            return
        }
        progressBar.visibility = View.VISIBLE
        roomManager.joinRoom(selectedRoom.id) { success ->
            progressBar.visibility = View.GONE
            if (success) navigateToGame() else showError("Falha ao entrar na sala")
        }
    }
    private fun updatePlayerList(room: LanRoomManager.Room) {
        playerListView.text = "Jogadores: " + room.players.joinToString(", ")
    }

    private fun sendChatMessage(message: String) {
        // TODO: Integrate with chat system
        chatView.append("\nVocê: $message")
    }

    private fun navigateToGame() {
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showError(msg: String) {
        // TODO: Show error to user
    }
}
