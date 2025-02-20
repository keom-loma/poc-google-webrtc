package com.example.poc_google_webrtc.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poc_google_webrtc.utils.StringConstance
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SessionDescription.Type.ANSWER
import org.webrtc.SessionDescription.Type.OFFER
import org.webrtc.VideoTrack
import java.io.IOException

/**
 * @Author: Jupyter.
 * @Date: 2/20/25.
 * @Email: koemheang200@gmail.com.
 */
class WebRTCViewModel(application: Application):AndroidViewModel(application) {

    var peerConnection: PeerConnection? = null
    var remoteRenderer: VideoTrack? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    var eglBaseContext: EglBase.Context = EglBase.create().eglBaseContext

    private val iceServers = listOf(
        PeerConnection.IceServer.builder(StringConstance.STUN).createIceServer()
    )

    companion object {
        var streamUrl =
            StringConstance.STREAM_URL
    }

    init {
        viewModelScope.launch {
            initializePeerConnectionFactory()
        }
    }

    private fun initializePeerConnectionFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(getApplication())
                .setFieldTrials(StringConstance.SET_FIELD_TRIALS)
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    fun generateSdpOffer(onOfferCreated: (SessionDescription) -> Unit) {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair(StringConstance.OFFER_TO_RECEIVE_AUDIO, StringConstance.IS_VALUE))
            mandatory.add(MediaConstraints.KeyValuePair(StringConstance.OFFER_TO_RECEIVE_VIDEO, StringConstance.IS_VALUE))
        }



        // Create new peerConnection instance using the PeerConnectionFactory and an RTCConfiguration object that contains a list of ICE Server
        val config = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                stream?.videoTracks?.firstOrNull()?.let { track ->
                    remoteRenderer = track
                }
            }
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
        })


        // 1 This code snippet creates an SDP (Session Description Protocol) offer for a peer connection in a WebRTC application.
        // 2 peerConnection?.createOffer(...): Creates an SDP offer using the peerConnection object, passing in an SdpObserver object to handle the result.
        //   - Corrects the order of media lines in the SDP using fixMLineOrder
        //  - Creates a new SessionDescription object with the corrected SDP.
        //  - Sets the local description of the peer connection using setLocalDescription, passing in another SdpObserver object.
        // 3 onSetSuccess(): Called when the local description is set successfully. It calls the onOfferCreated function, passing in the modified SessionDescription object.

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    val correctedSdp = fixMLineOrder(it.description)
                    val modifiedSessionDescription = SessionDescription(OFFER, correctedSdp)
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            onOfferCreated(modifiedSessionDescription)
                        }
                        override fun onSetFailure(error: String?) {}
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, modifiedSessionDescription)
                }
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, mediaConstraints)
    }


    // sends a WebRTC offer (SessionDescription Protocol, SDP) to a server using the OkHttp library. It:
    // 1 Creates an HTTP POST request with the SDP offer as the request body.
    // 2 Sets the Content-Type header to application/sdp.
    // 3 Sends the request to the server specified by streamUrl.
    // 4 Handles the server's response:
    //  If successful, extracts the SDP answer from the response and sets it as the remote description.
    //  If failed, logs an error message with the exception details.
    fun sendOfferToServer(sdp: SessionDescription) {
        val client = OkHttpClient()
        val request = Request.Builder().url(streamUrl)
            .header(StringConstance.CONTENT_TYPE, StringConstance.APPLICATION_SDP)
            .post(sdp.description.toRequestBody())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body.use { responseBody ->
                    responseBody?.string()?.let { sdpResponse ->
                        setRemoteDescription(SessionDescription(ANSWER, sdpResponse))
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WebRTC", "Failed to send offer: ${e.message}")
            }
        })
    }

    //sets the remote description of a WebRTC peer connection using a provided SessionDescription.
    // It creates an SdpObserver to handle the outcome of setting the remote description, but does not implement any actions for success or failure cases.

    private fun setRemoteDescription(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, sessionDescription)
    }


    // rearranges the order of audio and video media lines in a Session Description Protocol (SDP) string.
    // Specifically, it ensures that the video line comes before the audio line, if both are present.
    // This is done to conform to the WebRTC standard, which requires video to be listed before audio in the SDP.
    private fun fixMLineOrder(sdp: String): String {
        val lines = sdp.split("\n").toMutableList()
        val mLines = lines.filter { it.startsWith("m=") }

        if (mLines.size > 1) {
            val audioIndex = lines.indexOfFirst { it.startsWith("m=audio") }
            val videoIndex = lines.indexOfFirst { it.startsWith("m=video") }

            if (audioIndex != -1 && videoIndex != -1 && audioIndex > videoIndex) {
                lines[audioIndex] = mLines[1]
                lines[videoIndex] = mLines[0]
            }
        }
        return lines.joinToString("\n")
    }

}