package com.example.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var bgmJob: Job? = null
    private var bgmTrack: AudioTrack? = null
    private var isBgmPlaying = false
    private var bgmVolume = 1.0f

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Vibration failed", e)
        }
    }

    fun playJump() {
        scope.launch {
            try {
                // Generate a lovely retro jump sweep (from 350Hz up to 900Hz)
                val rate = 22050
                val durationS = 0.15f
                val size = (rate * durationS).toInt()
                val buffer = ShortArray(size)
                for (i in 0 until size) {
                    val t = i.toFloat() / rate
                    // progressive sweep frequency
                    val f = 350f + (900f - 350f) * (t / durationS)
                    val s = sin(2.0 * Math.PI * f * t)
                    // Apply clean fade-out envelope
                    val envelope = 1f - (t / durationS)
                    buffer[i] = (s * 32767 * 0.35f * envelope).toInt().toShort()
                }
                playPcm(buffer, rate)
            } catch (e: Exception) {
                Log.e("SoundManager", "Jump sound failed", e)
            }
        }
    }

    fun playCoin() {
        scope.launch {
            try {
                // Dual note gold coin chime (C5 then E5)
                val rate = 22050
                val durationS = 0.22f
                val size = (rate * durationS).toInt()
                val buffer = ShortArray(size)
                
                val note1Len = (size * 0.35f).toInt()
                // C5 Note
                for (i in 0 until note1Len) {
                    val t = i.toFloat() / rate
                    val s = sin(2.0 * Math.PI * 523.25 * t)
                    buffer[i] = (s * 32767 * 0.25f).toInt().toShort()
                }
                // E5 Note
                for (i in note1Len until size) {
                    val t = i.toFloat() / rate
                    val note2T = t - (note1Len.toFloat() / rate)
                    val s = sin(2.0 * Math.PI * 659.25 * note2T)
                    val envelope = 1.0f - (note2T / (durationS * 0.65f))
                    buffer[i] = (s * 32767 * 0.25f * envelope.coerceIn(0f, 1f)).toInt().toShort()
                }
                playPcm(buffer, rate)
            } catch (e: Exception) {
                Log.e("SoundManager", "Coin sound failed", e)
            }
        }
    }

    fun playGameOver() {
        scope.launch {
            try {
                // A falling sad heavy chime (from 400Hz down to 100Hz)
                val rate = 22050
                val durationS = 0.6f
                val size = (rate * durationS).toInt()
                val buffer = ShortArray(size)
                for (i in 0 until size) {
                    val t = i.toFloat() / rate
                    val f = 400f - (400f - 100f) * (t / durationS)
                    val s = sin(2.0 * Math.PI * f * t)
                    val envelope = 1f - (t / durationS)
                    // Overlay a minor harmonic for retro feeling
                    val sHarmonic = sin(2.0 * Math.PI * (f * 1.2f) * t)
                    val combined = (s * 0.7f + sHarmonic * 0.3f)
                    buffer[i] = (combined * 32767 * 0.35f * envelope).toInt().toShort()
                }
                playPcm(buffer, rate)
            } catch (e: Exception) {
                Log.e("SoundManager", "GameOver sound failed", e)
            }
        }
    }

    fun setBgmVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        bgmVolume = clamped
        try {
            bgmTrack?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.setVolume(clamped)
                } else {
                    @Suppress("DEPRECATION")
                    it.setStereoVolume(clamped, clamped)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Volume adjust failed", e)
        }
    }

    @Synchronized
    fun startBgm() {
        if (isBgmPlaying) return
        isBgmPlaying = true
        bgmJob = scope.launch(Dispatchers.Default) {
            try {
                val rate = 22050
                // We'll prepare a repeating retro tune block (approx 4 seconds loop of retro skiing baseline melody!)
                val sequence = listOf(
                    196.0, 196.0, 261.6, 261.6, 293.7, 329.6, 392.0, 329.6,
                    293.7, 261.6, 220.0, 220.0, 196.0, 220.0, 261.6, 261.6
                )
                val noteDurationS = 0.25f
                val bufferList = ArrayList<ShortArray>()
                
                for (noteFreq in sequence) {
                    val len = (rate * noteDurationS).toInt()
                    val noteBuffer = ShortArray(len)
                    for (i in 0 until len) {
                        val t = i.toFloat() / rate
                        // Retro square/triangle hybrid vibe
                        val rawSine = sin(2.0 * Math.PI * noteFreq * t)
                        val square = if (rawSine > 0) 1.0 else -1.0
                        val combined = (rawSine * 0.7 + square * 0.3)
                        
                        // Gentle envelope for arcade chirp quality
                        val envelope = if (i < len * 0.1f) {
                            i / (len * 0.1f)
                        } else {
                            1f - ((i - len * 0.1f) / (len * 0.9f))
                        }
                        
                        noteBuffer[i] = (combined * 32767 * 0.08f * envelope).toInt().toShort() // Keep BGM soft
                    }
                    bufferList.add(noteBuffer)
                }

                // Create AudioTrack for looping
                val bufferSize = AudioTrack.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                bgmTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize.coerceAtLeast(10240))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                setBgmVolume(bgmVolume)
                bgmTrack?.play()

                while (isBgmPlaying && isActive) {
                    for (noteBuffer in bufferList) {
                        if (!isBgmPlaying || !isActive) break
                        bgmTrack?.write(noteBuffer, 0, noteBuffer.size)
                        yield()
                    }
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "BGM failed", e)
            } finally {
                cleanBgm()
            }
        }
    }

    @Synchronized
    fun stopBgm() {
        isBgmPlaying = false
        bgmJob?.cancel()
        bgmJob = null
        try {
            bgmTrack?.stop()
        } catch (e: Exception) {
            // Ignored
        }
        cleanBgm()
    }

    private fun cleanBgm() {
        try {
            bgmTrack?.release()
        } catch (e: Exception) {
            // Ignored
        }
        bgmTrack = null
    }

    private fun playPcm(buffer: ShortArray, rate: Int) {
        var track: AudioTrack? = null
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize.coerceAtLeast(buffer.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            // Wait for playback to complete (approx length of the buffer)
            Thread.sleep((buffer.size.toFloat() / rate * 1000).toLong() + 50)
        } catch (e: Exception) {
            Log.e("SoundManager", "playPcm failed", e)
        } finally {
            try {
                track?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun release() {
        stopBgm()
        scope.cancel()
    }
}
