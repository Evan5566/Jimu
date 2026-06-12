package com.jimu.app.voice

import java.io.File

interface VoiceRecorder {
    fun start(outputFile: File)
    fun stop(): File?
    fun cancel()
}