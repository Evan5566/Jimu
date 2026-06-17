package com.jimu.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceRecognitionPreflightTest {

    @Test
    fun preflightReturnsNetworkHintWhenNetworkUnavailable() {
        assertEquals(
            "当前语音识别需要联网",
            voiceRecognitionPreflightError(isRecognitionAvailable = true, networkProbe = { false })
        )
    }

    @Test
    fun preflightReturnsUnsupportedHintWhenRecognitionUnavailable() {
        assertEquals(
            "当前设备不支持系统语音识别",
            voiceRecognitionPreflightError(isRecognitionAvailable = false, networkProbe = { true })
        )
    }

    @Test
    fun preflightPassesWhenRecognitionAndNetworkAreAvailable() {
        assertNull(
            voiceRecognitionPreflightError(isRecognitionAvailable = true, networkProbe = { true })
        )
    }

    @Test
    fun preflightReturnsNetworkHintWhenNetworkProbeThrows() {
        assertEquals(
            "当前语音识别需要联网",
            voiceRecognitionPreflightError(
                isRecognitionAvailable = true,
                networkProbe = { throw SecurityException("missing ACCESS_NETWORK_STATE") }
            )
        )
    }
}
