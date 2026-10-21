package com.readmd.app.ui.reader

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.readmd.app.R
import com.readmd.app.domain.SpeechText
import java.util.Locale

enum class SpeechState { IDLE, PLAYING, PAUSED }

/**
 * Reads a document aloud with the platform text-to-speech engine, one chunk at a time.
 * Pausing stops the engine and remembers the chunk, so resuming restarts that chunk.
 * Call [release] when the screen goes away; all calls must come from the main thread.
 */
class SpeechController(private val context: Context) {

    var state by mutableStateOf(SpeechState.IDLE)
        private set

    /** Zero-based index of the chunk being read, and how many there are. */
    var position by mutableIntStateOf(0)
        private set
    var total by mutableIntStateOf(0)
        private set

    /** Set when speech can't start (no engine, language not supported); the screen shows it once. */
    var error by mutableStateOf<String?>(null)

    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var engineReady = false
    private var chunks: List<String> = emptyList()
    private var startWhenReady = false

    /** Starts reading [renderedText] from the beginning. */
    fun start(renderedText: String) {
        stopSpeaking()
        chunks = SpeechText.chunks(renderedText)
        total = chunks.size
        position = 0
        if (chunks.isEmpty()) {
            error = context.getString(R.string.speech_nothing)
            return
        }
        state = SpeechState.PLAYING
        ensureEngine { speakFrom(0) }
    }

    fun pause() {
        if (state != SpeechState.PLAYING) return
        engine?.stop()
        state = SpeechState.PAUSED
    }

    fun resume() {
        if (state != SpeechState.PAUSED) return
        state = SpeechState.PLAYING
        ensureEngine { speakFrom(position) }
    }

    fun stop() {
        stopSpeaking()
        state = SpeechState.IDLE
        position = 0
    }

    fun release() {
        stop()
        engine?.shutdown()
        engine = null
        engineReady = false
    }

    private fun stopSpeaking() {
        startWhenReady = false
        engine?.stop()
    }

    private fun ensureEngine(onReady: () -> Unit) {
        if (engineReady) {
            onReady()
            return
        }
        startWhenReady = true
        if (engine != null) return
        engine = TextToSpeech(context.applicationContext) { status ->
            main.post {
                if (status != TextToSpeech.SUCCESS) {
                    fail(context.getString(R.string.speech_unavailable))
                    return@post
                }
                val tts = engine ?: return@post
                val language = tts.setLanguage(Locale.getDefault())
                if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    fail(context.getString(R.string.speech_language))
                    return@post
                }
                tts.setOnUtteranceProgressListener(listener)
                engineReady = true
                if (startWhenReady && state == SpeechState.PLAYING) onReady()
                startWhenReady = false
            }
        }
    }

    private fun fail(message: String) {
        error = message
        engine?.shutdown()
        engine = null
        engineReady = false
        state = SpeechState.IDLE
        position = 0
    }

    private fun speakFrom(index: Int) {
        val tts = engine ?: return
        tts.stop()
        for (i in index until chunks.size) {
            tts.speak(chunks[i], TextToSpeech.QUEUE_ADD, null, "$UTTERANCE_PREFIX$i")
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val index = indexOf(utteranceId) ?: return
            main.post { if (state == SpeechState.PLAYING) position = index }
        }

        override fun onDone(utteranceId: String?) {
            val index = indexOf(utteranceId) ?: return
            main.post {
                if (state == SpeechState.PLAYING && index == chunks.lastIndex) {
                    state = SpeechState.IDLE
                    position = 0
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            main.post { fail(context.getString(R.string.speech_failed)) }
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit
    }

    private fun indexOf(utteranceId: String?): Int? =
        utteranceId?.removePrefix(UTTERANCE_PREFIX)?.toIntOrNull()

    private companion object {
        const val UTTERANCE_PREFIX = "chunk-"
    }
}
