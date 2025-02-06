package com.example.poc_google_webrtc.utils

import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class WebRTCClient(
    private val webRTCManager: WebRTCManager,
    private val singleClient: JanusSignalingClient

) {
    private var peerConnection: PeerConnection? = null

    fun initializePeerConnection(
        rtcConfig: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer
    ) {
        peerConnection = webRTCManager.createPeerConnection(rtcConfig, observer)
        singleClient.connectJanusServer()
    }


    fun handleRemoteSessionDescription(sdp: SessionDescription?) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                TODO("Not yet implemented")
            }

            override fun onSetSuccess() {
                TODO("Not yet implemented")
            }

            override fun onCreateFailure(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onSetFailure(p0: String?) {
                TODO("Not yet implemented")
            }
        }, sdp)
    }

    fun closeConnection() {
        peerConnection?.close()
        peerConnection = null
    }
}






