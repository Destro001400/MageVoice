package com.game.voicespells.presentation.viewmodels

// ... (imports) ...

class GameViewModel(application: Application) : AndroidViewModel(application), VoiceRecognitionListener {

    // ... (other LiveData) ...
    private val _networkStatus = MutableLiveData<String>()
    val networkStatus: LiveData<String> = _networkStatus

    // ... (dependencies & internal state) ...

    init {
        _networkStatus.value = "Welcome! Host or Join a game."
        voiceRecognitionManager.initialize()
        setupNetworkListener()
    }

    fun hostGame() {
        _networkStatus.value = "Starting host..."
        _gameMode.value = GameMode.HOST
        // ... (rest of host logic)
        _networkStatus.value = "Host started. Waiting for players..."
    }

    fun joinGame() {
        _networkStatus.value = "Searching for host..."
        _gameMode.value = GameMode.CLIENT
        networkManager.findHost {
            _networkStatus.postValue("Host found at $it. Connecting...")
            networkManager.startClient(it)
        }
    }

    private fun setupNetworkListener() {
        networkManager.connectionEvents.onEach { event ->
            when (event) {
                is ConnectionEvent.ClientConnected -> {
                    if (_gameMode.value == GameMode.HOST) {
                        _networkStatus.postValue("Player connected: ${event.clientId.substring(0, 4)}")
                        // ... (rest of logic)
                    }
                }
                is ConnectionEvent.ClientDisconnected -> {
                    if (_gameMode.value == GameMode.HOST) {
                        _networkStatus.postValue("Player disconnected.")
                        // ... (rest of logic)
                    }
                }
                is ConnectionEvent.EventReceived -> {
                    if (_gameMode.value == GameMode.CLIENT && event.event is GameStateUpdate) {
                        if(gameMode.value != GameMode.CLIENT) _networkStatus.postValue("Connected! Game in progress.")
                        // ... (rest of logic)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    // ... (rest of the file)
}
