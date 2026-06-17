package com.jimu.app.voice

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class AndroidSpeechToTextRepository(
    private val context: Context
) : SpeechToTextRepository {

    private var speechRecognizer: SpeechRecognizer? = null
    private var latestPartialText: String = ""
    private var finalDelivered = false

    override fun startListening(
        onReady: () -> Unit,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val preflightError = voiceRecognitionPreflightError(
            isRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context),
            networkProbe = { isNetworkAvailable(context) }
        )
        if (preflightError != null) {
            onError(preflightError)
            return
        }

        release()

        latestPartialText = ""
        finalDelivered = false

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    onReady()
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    if (!finalDelivered && latestPartialText.isNotBlank()) {
                        finalDelivered = true
                        onFinalResult(latestPartialText.trim())
                        return
                    }

                    onError(errorMessage(error))
                }

                override fun onResults(results: Bundle?) {
                    val text = extractBestResult(results)
                    val finalText = when {
                        text.isNotBlank() -> text
                        latestPartialText.isNotBlank() -> latestPartialText
                        else -> ""
                    }

                    if (!finalDelivered) {
                        finalDelivered = true
                        onFinalResult(finalText.trim())
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = extractBestResult(partialResults)
                    if (partial.isNotBlank()) {
                        latestPartialText = partial.trim()
                        onPartialResult(latestPartialText)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            startListening(buildRecognizerIntent())
        }
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
    }

    override fun cancel() {
        speechRecognizer?.cancel()
    }

    override fun release() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        latestPartialText = ""
        finalDelivered = false
    }

    private fun buildRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.SIMPLIFIED_CHINESE.toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    private fun extractBestResult(bundle: Bundle?): String {
        val list = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        return list?.firstOrNull()?.trim().orEmpty()
    }

    private fun errorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "录音异常"
            SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "当前语音识别需要联网"
            SpeechRecognizer.ERROR_NO_MATCH -> "没有识别到内容"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别服务正忙"
            SpeechRecognizer.ERROR_SERVER -> "识别服务异常"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "长时间未检测到说话"
            else -> "语音识别失败（$error）"
        }
    }
}

internal fun voiceRecognitionPreflightError(
    isRecognitionAvailable: Boolean,
    networkProbe: () -> Boolean
): String? {
    if (!isRecognitionAvailable) return "当前设备不支持系统语音识别"
    val isNetworkAvailable = try {
        networkProbe()
    } catch (_: SecurityException) {
        false
    } catch (_: RuntimeException) {
        false
    }
    if (!isNetworkAvailable) return "当前语音识别需要联网"
    return null
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
