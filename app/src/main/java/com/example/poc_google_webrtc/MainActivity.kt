package com.example.poc_google_webrtc

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.poc_google_webrtc.ui.theme.PocgooglewebrtcTheme
import com.example.poc_google_webrtc.utils.WebRTCManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import okio.use
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.SignalingState.CLOSED
import org.webrtc.PeerConnection.SignalingState.HAVE_LOCAL_OFFER
import org.webrtc.PeerConnection.SignalingState.HAVE_LOCAL_PRANSWER
import org.webrtc.PeerConnection.SignalingState.HAVE_REMOTE_OFFER
import org.webrtc.PeerConnection.SignalingState.HAVE_REMOTE_PRANSWER
import org.webrtc.PeerConnection.SignalingState.STABLE
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class MainActivity : ComponentActivity() {

    private var peerConnection: PeerConnection? = null
    private var remoteRenderer: VideoTrack? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null

    private val iceServers = listOf(
        IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private var localPeer: PeerConnection? = null

    private var localVideoTrack: VideoTrack? = null

    companion object {
        var streamUrl =
            "https://oryx.lomasq.com/rtc/v1/whep/?app=live&stream=livestream&eip=91.108.105.153:8888"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true // Dark icons
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PocgooglewebrtcTheme {
                startButton(
                    onClick = {
                        initializePeerConnectionFactory()
                    }
                )
            }
        }
    }

    private fun initializePeerConnectionFactory() {
        // Create initialization options for the PeerConnectionFactory
        val options = PeerConnectionFactory.Options()

        // Initialize WebRTC PeerConnectionFactory
        val initializer = PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        // Create PeerConnectionFactory once initialization is done
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()


        if (peerConnectionFactory == null) {
            println("PeerConnectionFactory initialization failed.")
            return
        } else {
            println("PeerConnectionFactory initialized successfully.")
            generateSdpOffer()
        }
    }

    private fun generateSdpOffer() {
        println("generateSdpOffer running...")
        // Ensure peerConnectionFactory is initialized before use
        if (peerConnectionFactory == null) {
            Log.e("SDP Offer", "PeerConnectionFactory is not initialized!")
            return
        }
        val mediaConstraints = MediaConstraints().apply {
            // Add any specific constraints here if necessary
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        val config = PeerConnection.RTCConfiguration(iceServers) // Customize as needed
        // Initialize the PeerConnection
        peerConnection =
            peerConnectionFactory?.createPeerConnection(config, object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

                override fun onAddStream(stream: MediaStream?) {
                    stream?.let {mediaStream->
                        val videoTrack = mediaStream.videoTracks.firstOrNull()
                        videoTrack?.let { vdeo ->
                            Log.d("WebRTC-=-=-=-=-=-=", "Remote Video Track added: ${vdeo}")
                            // Set up the remote video renderer
                            remoteRenderer?.addSink{vdeo}
                        }
                    }
                    println("onAddStream on method generate offer: ${stream?.videoTracks}: ")
                }
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}

                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                    Log.e("SignalState", "onSignalStateChange ${newState?.name}")
                }
            })!!

        // Create SDP offer
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                val modifiedSdp = sdp?.description?.replace("m=video 0 UDP/TLS/RTP/SAVPF 0",
                    "m=video 9 UDP/TLS/RTP/SAVPF 102")

                //m=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 35 36 37 38 102 103 104 105 106 107 108 109 127 125 39 40 41 42 43 44 45 46 47 48 112 113 114 115 116 117 118 49
                // a=rtpmap:100 VP8/90000
                // a=rtpmap:101 VP9/90000
                val codecLines = """
                    a=rtpmap:102 H264/90000
                """.trimIndent()

                // Append the codec lines to the SDP
                val finalSdp = modifiedSdp + codecLines
                println("finaSdp-=-=-=-=$finalSdp")
                Log.e("TAG", "finaSdp-=-=-=-=${finalSdp}")
                val modifiedSessionDescription = SessionDescription(SessionDescription.Type.OFFER, finalSdp)
                //val resultSetLocalDescription = peerConnection?.setLocalDescription(this, modifiedSessionDescription)

                peerConnection?.setLocalDescription(modifiedSessionDescription.let {
                    object : SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        println("resultSetLocalDescription onCreateSuccess: $p0")
                    }
                    override fun onSetSuccess() {
                        println("resultSetLocalDescription onSetSuccess:")
                    }
                    override fun onCreateFailure(p0: String?) {
                        println("resultSetLocalDescription onCreateFailure: $p0")
                    }
                    override fun onSetFailure(p0: String?) {
                        println("resultSetLocalDescription onSetFailure: $p0")
                    }

                }
                })

                sendOfferToServer(modifiedSessionDescription)
            }
            override fun onCreateFailure(error: String?) {
                Log.e("SDP Offer", "Error creating SDP offer: $error")
            }
            override fun onSetSuccess() {}

            override fun onSetFailure(error: String?) {
                Log.e("SDP Offer", "Error setting SDP: $error")
            }
        }, mediaConstraints)

        println("mediaConstraints: $mediaConstraints")
    }

    private fun sendOfferToServer(it: SessionDescription) {
        Log.e("SOCKET", "$it")
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(streamUrl)
            .header("Content-Type", "application/sdp")
            .post(it.description.toString().toRequestBody())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.e("WebRTC", "fetchSDPFromWHEP-=-=: ${response.body?.toString()}")
                val sdpResponse = response.body.toString()
                response.body.use {responseBody ->
                    val sdpResponse = responseBody?.string()
                    Log.e("WebRTC", "Response Body: $sdpResponse")
                    val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdpResponse)
                    println("Information on sessionDescription: ${sessionDescription.description}")
                    setRemoteDescription(sessionDescription)

                }
                println("Information on Response: $sdpResponse")
               //  setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, response.toString()))
                /*response.body?.string()?.let { sdpResponse ->
                    Log.e("WebRTC 900909090", sdpResponse)

                    // setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdpResponse))

                } ?: {
                    Log.e("WebRTC", "Empty SDP response")
                }*/
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("WebRTC", "Failed to fetch SDP: ${e.message}")
            }
        })
    }

    private fun setRemoteDescription(sessionDescription: SessionDescription) {
        println( "Received SDP setRemoteDescription: ${sessionDescription.description}")
        val signalingState = peerConnection?.signalingState()
        println( "signalingState: ${signalingState?.name}")
       /* if (signalingState == PeerConnection.SignalingState.STABLE) {
            Log.e("WebRTC", "STABLE state detected. Need to create an offer before setting remote description.")
            createAndSetLocalOffer {
                Log.e("WebRTC", "Local offer set. Now setting remote description.")
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.e("WebRTC", "Remote description set successfully")
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e("WebRTC", "Failed to set remote description: $error")
                    }

                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sessionDescription)
            }
        } else if (signalingState == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
            Log.e("WebRTC", "Local offer already exists! Proceeding to set remote description.")
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.e("WebRTC", "Remote description set successfully")
                }

                override fun onSetFailure(error: String?) {
                    Log.e("WebRTC", "Failed to set remote description: $error")
                }

                override fun onCreateSuccess(sdp: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, sessionDescription)
        } else {
            Log.e("WebRTC", "Skipping setRemoteDescription: Invalid signaling state: $signalingState")
        }*/
      peerConnection?.let {
            Log.e("SDP Answer", "PeerConnection State: ${it.signalingState()}")
            it.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.e("SDP Answer onSetSuccess", "Remote description set successfully")
                }

                override fun onSetFailure(error: String?) {
                    Log.e("SDP Answer onSetFailure", "Failed to set remote description onSetFailure: $error")
                }

                override fun onCreateSuccess(sdp: SessionDescription?) {
                    Log.e("SDP Answer onCreateSuccess", "Failed to set remote description onCreateSuccess: $sdp")
                }

                override fun onCreateFailure(error: String?) {
                    Log.e("SDP Answer onCreateFailure", "Failed to set remote description onCreateFailure: $error")
                }
            }, sessionDescription)
        } ?: Log.e("SDP Answer", "PeerConnection is null!")
    }
    private fun createAndSetLocalOffer(callback: () -> Unit) {
        val constraints = MediaConstraints()

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.e("WebRTC", "Offer created: ${sdp.description}")

                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.e("WebRTC", "Local offer set successfully")
                        callback() // Proceed to set the remote description
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e("WebRTC", "Failed to set local offer: $error")
                    }

                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sdp)
            }

            override fun onCreateFailure(error: String?) {
                Log.e("WebRTC", "Offer creation failed: $error")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    @Composable
    fun startButton(
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = {
                onClick()
            }) {
                Text(text = "Generate Offer")
            }
        }
    }

    @Composable
    fun RemoteVideoView(
        webRTCManager: WebRTCManager
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webRTCManager.remoteSurfaceViewRenderer = this
                }
            }, modifier = Modifier.fillMaxSize()
        )
    }
}



