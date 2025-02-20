package com.example.poc_google_webrtc.utils

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * @Author: Jupyter.
 * @Date: 2/20/25.
 * @Email: koemheang200@gmail.com.
 */
open class SdpAdapter constructor(private val tag: String) : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {
    }

    override fun onSetSuccess() {
        println("SdpObserver->${tag}->onSetSuccess:")
    }

    override fun onCreateFailure(p0: String?) {
        println("SdpObserver->${tag}->onCreateFailure:${p0}")
    }

    override fun onSetFailure(p0: String?) {
        println("SdpObserver->${tag}->onSetFailure:${p0}")
    }
}