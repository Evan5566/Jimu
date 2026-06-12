package com.jimu.app.voice

interface SpeechToTextRepository {
    fun startListening(
        onReady: () -> Unit = {},
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    )

    fun stopListening()

    fun cancel()

    fun release()
}