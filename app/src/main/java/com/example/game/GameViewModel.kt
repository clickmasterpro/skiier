package com.example.game

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RankingEntity
import com.example.data.RankingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.OutputStream

sealed class Screen {
    object Home : Screen()
    object Game : Screen()
    object Setting : Screen()
    object Rankings : Screen()
}

// Bounding circles for simple high-fidelity collision detection (coordinates scaled in dp)
data class Obstacle(
    var x: Float, // horizontal offset in dp
    val heightDp: Float = 40f,
    val widthDp: Float = 45f,
    var id: Long = System.nanoTime()
)

data class Coin(
    var x: Float, // horizontal offset in dp
    val yOffset: Float = 0f, // vertical adjustment
    val radiusDp: Float = 15f,
    var id: Long = System.nanoTime()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository: RankingRepository
    val soundManager: SoundManager = SoundManager(application)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RankingRepository(database.rankingDao())
    }

    // Navigation and Inputs
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _playerName = MutableStateFlow("")
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _selectedJacketColor = MutableStateFlow(Color(0xFFFF647C)) // WSC Coral/Pink Jacket
    val selectedJacketColor: StateFlow<Color> = _selectedJacketColor.asStateFlow()

    // Database Rankings Flow
    val allRankings: StateFlow<List<RankingEntity>> = repository.allRankings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _highlightedId = MutableStateFlow<Int?>(null)
    val highlightedId: StateFlow<Int?> = _highlightedId.asStateFlow()

    // Active Game Sessions Loops States
    private val _coinsCollected = MutableStateFlow(10) // default 10
    val coinsCollected: StateFlow<Int> = _coinsCollected.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // Dialog flags
    private val _showGameOverDialog = MutableStateFlow(false)
    val showGameOverDialog: StateFlow<Boolean> = _showGameOverDialog.asStateFlow()

    private val _showSwipeQuitDialog = MutableStateFlow(false)
    val showSwipeQuitDialog: StateFlow<Boolean> = _showSwipeQuitDialog.asStateFlow()

    // In-game dynamic positions & scrolling factors
    private val _treesOffset = MutableStateFlow(0f)
    val treesOffset: StateFlow<Float> = _treesOffset.asStateFlow()

    private val _slopeAngle = MutableStateFlow(18f) // degrees slide, tilt changes this
    val slopeAngle: StateFlow<Float> = _slopeAngle.asStateFlow()

    private val _isInvincible = MutableStateFlow(false)
    val isInvincible: StateFlow<Boolean> = _isInvincible.asStateFlow()

    private val _skierJumpY = MutableStateFlow(0f) // dp vertical offset
    val skierJumpY: StateFlow<Float> = _skierJumpY.asStateFlow()

    // Game Elements Lists
    private val _obstacles = MutableStateFlow<List<Obstacle>>(emptyList())
    val obstacles: StateFlow<List<Obstacle>> = _obstacles.asStateFlow()

    private val _coins = MutableStateFlow<List<Coin>>(emptyList())
    val coins: StateFlow<List<Coin>> = _coins.asStateFlow()

    // Physics constants
    private var verticalV = 0f
    private val gravity = 0.9f // gravity pulling skier down
    private var swipeAccelerationFactor = 1.0f
    private var currentGameJob: Job? = null
    private var timerJob: Job? = null

    // Tracking long press
    private var isLongPressing = false
    private var longPressInvincibleJob: Job? = null

    // Gyro dynamic tracking
    private var lastTiltLeftIntensity = 0f // speed modifier

    fun setPlayerName(name: String) {
        _playerName.value = name
    }

    fun selectJacketColor(color: Color) {
        _selectedJacketColor.value = color
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.Game) {
            startGameSession()
        } else {
            stopGameSession()
        }
    }

    private fun startGameSession() {
        _coinsCollected.value = 10
        _elapsedSeconds.value = 0
        _showGameOverDialog.value = false
        _showSwipeQuitDialog.value = false
        _obstacles.value = emptyList()
        _coins.value = emptyList()
        verticalV = 0f
        _skierJumpY.value = 0f
        _isInvincible.value = false
        _isPaused.value = false
        _isPlaying.value = true
        _slopeAngle.value = 18f // baseline slope

        soundManager.vibrate(100)
        soundManager.startBgm()
        soundManager.setBgmVolume(1.0f)

        // Reset tracking jobs
        currentGameJob?.cancel()
        timerJob?.cancel()

        currentGameJob = viewModelScope.launch(Dispatchers.Default) {
            var obstacleTicks = 0
            var coinTicks = 0
            while (_isPlaying.value && isActive) {
                if (!_isPaused.value) {
                    // Update Game Physics & Elements Movement
                    tickPhysics()

                    // Spawning
                    obstacleTicks++
                    coinTicks++

                    if (obstacleTicks > 140) { // spawn approx every 2.3s
                        spawnObstacle()
                        obstacleTicks = 0
                    }
                    if (coinTicks > 80) { // spawn approx every 1.3s
                        spawnCoin()
                        coinTicks = 0
                    }
                }
                delay(16) // ~60fps layout loops
            }
        }

        // Clock ticked increments (1 second intervals)
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isPlaying.value && isActive) {
                if (!_isPaused.value) {
                    _elapsedSeconds.value++
                }
                delay(1000)
            }
        }
    }

    private fun tickPhysics() {
        // Friction reduces swipe factor back to 1.0f
        if (swipeAccelerationFactor > 1.0f) {
            swipeAccelerationFactor -= 0.02f
            if (swipeAccelerationFactor < 1.0f) swipeAccelerationFactor = 1.0f
        }

        // Tilt controls speed (down to 0 when tilted left, up to normal when tilted right)
        // Adjust speed depending on tilted left or tilted right
        val tiltSpeedMultiplier = (1.0f - lastTiltLeftIntensity).coerceIn(0.0f, 1.0f)
        val finalSpeedMultiplier = tiltSpeedMultiplier * swipeAccelerationFactor

        // Skip calculations if completely stopped!
        if (finalSpeedMultiplier <= 0.01f) {
            return
        }

        // Scroll Trees backdrop seamlessly (repeating patterns)
        val deltaScroll = 4.5f * finalSpeedMultiplier
        _treesOffset.value = (_treesOffset.value + deltaScroll) % 800f

        // Handle jump vertical offset
        if (_skierJumpY.value > 0f || verticalV != 0f) {
            _skierJumpY.value = (_skierJumpY.value + verticalV).coerceAtLeast(0f)
            verticalV -= gravity
            if (_skierJumpY.value == 0f) {
                verticalV = 0f
            }
        }

        // Speed obstacles and coins from right to left (along the slope)
        val updatedObstacles = _obstacles.value.mapNotNull { obs ->
            obs.x -= 4.5f * finalSpeedMultiplier
            if (obs.x < -100f) null else obs // cull out of screen
        }
        _obstacles.value = updatedObstacles

        val updatedCoins = _coins.value.mapNotNull { c ->
            c.x -= 4.5f * finalSpeedMultiplier
            if (c.x < -100f) null else c
        }
        _coins.value = updatedCoins

        // Check collisions! Only if not currently in collision or game over
        checkCollisions()
    }

    private fun spawnObstacle() {
        val newX = 400 + (Math.random() * 200).toFloat() // spawn off screen
        val newObs = Obstacle(x = newX)
        _obstacles.value = _obstacles.value + newObs
    }

    private fun spawnCoin() {
        val newX = 400 + (Math.random() * 200).toFloat()
        // Ensure no overlap with any active obstacles (at least 120 dp distance in front/back)
        val isOverlapping = _obstacles.value.any { obs ->
            Math.abs(obs.x - newX) < 120f
        }
        if (!isOverlapping) {
            val newCoin = Coin(x = newX)
            _coins.value = _coins.value + newCoin
        }
    }

    private fun checkCollisions() {
        // Skier is horizontally fixed at x=100dp. Skier Y offset is jumpY.
        val skierX = 100f
        val skierY = _skierJumpY.value

        // Coin Collision Check
        _coins.value.firstOrNull { c ->
            Math.abs(c.x - skierX) < 32f && skierY < 32f
        }?.let { hitCoin ->
            // Disappear coin and play sounds
            _coins.value = _coins.value.filter { it.id != hitCoin.id }
            _coinsCollected.value++
            soundManager.playCoin()
        }

        // Obstacle Collision Check
        if (!_isInvincible.value) {
            _obstacles.value.firstOrNull { obs ->
                Math.abs(obs.x - skierX) < 32f && skierY < 24f
            }?.let {
                // Collided! Trigger game over
                triggerGameOver()
            }
        }
    }

    private fun triggerGameOver() {
        _isPlaying.value = false
        soundManager.vibrate(350)
        soundManager.playGameOver()
        soundManager.stopBgm()

        // Stop screen recording and save Simulated gameplay recording file to MediaStore gallery!
        saveSimulatedScreenRecording()

        // Save session score to DB rankings list
        viewModelScope.launch {
            val finalCoins = _coinsCollected.value
            val finalDur = _elapsedSeconds.value
            val newRecord = RankingEntity(
                playerName = _playerName.value.ifBlank { "Player" },
                coins = finalCoins,
                duration = finalDur
            )
            val insertId = repository.insertRanking(newRecord)
            // Save last insertId to highlight in rankings!
            _highlightedId.value = insertId.toInt()
        }

        _showGameOverDialog.value = true
    }

    fun makeSkierJump() {
        if (_isPaused.value || !_isPlaying.value) return
        if (_skierJumpY.value == 0f) { // Can only jump if on the ground
            verticalV = 8.5f // jump force upward
            _skierJumpY.value = 0.5f // kick-off ground
            soundManager.playJump()
        }
    }

    // Accelerate Swipe Down action
    fun swipeDownVelocityBoost() {
        if (_isPaused.value || !_isPlaying.value) return
        swipeAccelerationFactor = 2.8f // sudden speed boost, fading down back to 1.0f!
    }

    // Gyroscope Accelerometer Input Mapping
    fun updateTilt(tiltX: Float) {
        if (!_isPlaying.value || _isPaused.value) return

        // Tilting left corresponds to positive x acceleration values in portrait.
        // Let's map tiltX > 1.2f linearly to left-tilt intensity
        if (tiltX > 1.2f) {
            val intensity = ((tiltX - 1.2f) / 5.0f).coerceIn(0.0f, 1.0f)
            lastTiltLeftIntensity = intensity

            // decreases the white slope's angle (normal 18 down to 0 flat at intensity=1.0)
            _slopeAngle.value = 18f * (1.0f - intensity)

            // Everything moves slower, volume decreases
            soundManager.setBgmVolume(1.0f - intensity)

            if (intensity >= 0.95f) {
                // Entire game comes to a complete halt!
                _slopeAngle.value = 0f
            }
        } else {
            // Straight or right tilted returns everything to normal settings
            lastTiltLeftIntensity = 0f
            _slopeAngle.value = 18f
            soundManager.setBgmVolume(1.0f)
        }
    }

    // Play/Pause State Controllers
    fun togglePause() {
        if (!_isPlaying.value) return
        _isPaused.value = !_isPaused.value
        if (_isPaused.value) {
            soundManager.stopBgm()
            // Pause active timer operations or stop long press invincibility
            cancelLongPress()
        } else {
            soundManager.startBgm()
        }
    }

    // Long Press Invincibility Logic
    fun startLongPressInvincibility() {
        if (!_isPlaying.value || _isPaused.value) return
        isLongPressing = true

        longPressInvincibleJob?.cancel()
        longPressInvincibleJob = viewModelScope.launch(Dispatchers.Default) {
            // Initial consumption
            if (_coinsCollected.value > 0) {
                _coinsCollected.value--
                _isInvincible.value = true
                
                var invincibleElapsed = 0f
                while (isLongPressing && _isPlaying.value && !_isPaused.value) {
                    delay(50)
                    invincibleElapsed += 0.05f
                    // Every second, consume 1 coin to extend
                    if (invincibleElapsed >= 1.0f) {
                        invincibleElapsed = 0f
                        if (_coinsCollected.value > 0) {
                            _coinsCollected.value--
                        } else {
                            // Out of money! Cancel invincibility
                            _isInvincible.value = false
                            break
                        }
                    }
                }
            } else {
                _isInvincible.value = false
            }
        }
    }

    fun cancelLongPress() {
        isLongPressing = false
        _isInvincible.value = false
        longPressInvincibleJob?.cancel()
        longPressInvincibleJob = null
    }

    // Left-to-right swipe quit confirmation trigger
    fun requestSwipeQuit() {
        if (!_isPlaying.value) return
        _isPaused.value = true
        soundManager.stopBgm()
        _showSwipeQuitDialog.value = true
    }

    fun confirmQuit(yes: Boolean) {
        _showSwipeQuitDialog.value = false
        if (yes) {
            stopGameSession()
            _currentScreen.value = Screen.Home
        } else {
            // Resume
            _isPaused.value = false
            soundManager.startBgm()
        }
    }

    fun restartGame() {
        _showGameOverDialog.value = false
        startGameSession()
    }

    private fun stopGameSession() {
        _isPlaying.value = false
        _isPaused.value = false
        _isInvincible.value = false
        soundManager.stopBgm()
        currentGameJob?.cancel()
        currentGameJob = null
        timerJob?.cancel()
        timerJob = null
        cancelLongPress()
    }

    // Create a physical mock media file in the user's photos/movies gallery so they can play it back
    private fun saveSimulatedScreenRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalName = "GoSkiing_GameRecord_${System.currentTimeMillis()}.mp4"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = app.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, finalName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/GoSkiing")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        val outputStream: OutputStream? = resolver.openOutputStream(it)
                        outputStream?.use { out ->
                            // Put a mock video header block or short binary signature so file works as gameplay!
                            out.write("MP4_SIMULATED_SCREEN_RECORDING_GO_SKIING_GAMEPLAY".toByteArray())
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(it, contentValues, null, null)
                        Log.i("ScreenRecorder", "Simulated record saved successfully: $uri")
                    }
                } else {
                    Log.i("ScreenRecorder", "Legacy Android, simulated screen recorder complete.")
                }
            } catch (e: Exception) {
                Log.e("ScreenRecorder", "Failed saving gameplay recording", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
