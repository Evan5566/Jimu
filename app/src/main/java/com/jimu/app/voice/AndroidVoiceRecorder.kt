package com.jimu.app.voice

import android.media.MediaRecorder
import java.io.File

class AndroidVoiceRecorder : VoiceRecorder {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    override fun start(outputFile: File) {
        cancel()

        currentOutputFile = outputFile

        @Suppress("DEPRECATION")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        isRecording = true
    }

    override fun stop(): File? {
        if (!isRecording) return null

        return try {
            recorder?.stop()
            currentOutputFile
        } catch (_: Exception) {
            currentOutputFile?.delete()
            null
        } finally {
            releaseInternal()
        }
    }

    override fun cancel() {
        if (isRecording) {
            try {
                recorder?.stop()
            } catch (_: Exception) {
            }
        }

        currentOutputFile?.delete()
        releaseInternal()
    }

    private fun releaseInternal() {
        try {
            recorder?.reset()
        } catch (_: Exception) {
        }

        try {
            recorder?.release()
        } catch (_: Exception) {
        }

        recorder = null
        currentOutputFile = null
        isRecording = false
    }
}