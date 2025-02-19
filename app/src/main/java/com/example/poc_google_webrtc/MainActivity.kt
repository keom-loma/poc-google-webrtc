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
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class MainActivity : ComponentActivity() {


    private lateinit var eglBase: EglBase

    private var peerConnection: PeerConnection? = null
    private var remoteRenderer: VideoTrack? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
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
        initializePeerConnectionFactory()
        setContent {
            PocgooglewebrtcTheme {
                startButton(
                    onClick = {
                        generateSdpOffer()
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

        val config = PeerConnection.RTCConfiguration(emptyList()) // Customize as needed
        peerConnection =
            peerConnectionFactory?.createPeerConnection(config, object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}

                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            })!!

        // Initialize the PeerConnection
        peerConnection =
            peerConnectionFactory?.createPeerConnection(config, object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: org.webrtc.IceCandidate?) {
                    // Handle ICE candidate
                    candidate?.let {
                        // send to signaling server
                        println("ICE Candidate: $it")
                    }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                    // Handle ICE candidates removed
                }

                override fun onAddStream(stream: org.webrtc.MediaStream?) {
                    // Handle added stream (e.g., remote media stream)
                }

                override fun onRemoveStream(stream: org.webrtc.MediaStream?) {
                    // Handle removed stream
                }

                override fun onDataChannel(channel: org.webrtc.DataChannel?) {
                    // Handle data channel events
                }

                override fun onRenegotiationNeeded() {
                    // Handle renegotiation if needed
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    // Handle ICE connection state change
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                    // Handle ICE connection receiving change
                }

                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                    // Handle ICE gathering state change
                }

                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                    // Handle signaling state change
                }
            }) ?: run {
                Log.e("SDP Offer", "Failed to create PeerConnection")
                return
            }

        // Create SDP offer
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    // Set local description
                    println("SDP Offer-=-=-: ${it.description}")
                    Log.e("SDp form log", "${it.description}")
                    peerConnection?.setLocalDescription(this, it)

                    // Log and display the SDP offer
                    val sdpOffer = it.description
                    // sdpOfferTextView.text = sdpOffer

                    println("SDP Offer: $sdpOffer")
                    println("SDP Offer type: ${it.type}")

                    sendOfferToServer(it)

                    // Send the SDP offer to the signaling server
                    // sendSdpOfferToServer(sdpOffer)
                }
            }

            override fun onCreateFailure(error: String?) {
                Log.e("SDP Offer", "Error creating SDP offer: $error")
            }

            override fun onSetSuccess() {}

            override fun onSetFailure(error: String?) {
                Log.e("SDP Offer", "Error setting SDP: $error")
            }
        }, mediaConstraints)
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
                Log.e("WebRTC", "fetchSDPFromWHEP: $response")

                response.body?.string()?.let { sdpResponse ->
                    Log.e("WebRTC", sdpResponse)
                } ?: {
                    Log.e("WebRTC", "Empty SDP response")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("WebRTC", "Failed to fetch SDP: ${e.message}")
            }
        })
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

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) {
            println("WebRTC-=error:::::Connected onCreateSuccess")
        }

        override fun onSetSuccess() {
            println("WebRTC-=error:::::Connected onSetSuccess")
        }

        override fun onCreateFailure(error: String?) {
            println("WebRTC-=error:::::Connected to onCreateFailure")
        }

        override fun onSetFailure(error: String?) {
            println("WebRTC-=error:::::Connected to onSetFailure")
        }
    }

    private fun startStreaming(whepUrl: String) {
        println("WebRTC - Connecting to WHEP server: $whepUrl")

        val request = Request.Builder().url(whepUrl).post("".toRequestBody()).build()
        println("WebRTC - Connecting to request: $request")
        val client = OkHttpClient()
        println("WebRTC - Connecting to client: $client")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("WebRTC - Error fetching SDP offer: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    println("WebRTC - Failed to fetch SDP offer, code: ${response.code}")
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    println("WebRTC - Received empty SDP offer")
                    return
                }

                try {
                    val json = JSONObject(responseBody)
                    val sdp = json.getString("sdp")
                    val remoteDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)

                    createPeerConnection()

                    localPeer?.setRemoteDescription(SimpleSdpObserver(), remoteDescription)

                    localPeer?.createAnswer(object : SimpleSdpObserver() {
                        override fun onCreateSuccess(answer: SessionDescription?) {
                            if (answer != null) {
                                localPeer?.setLocalDescription(SimpleSdpObserver(), answer)

                                // Send answer back to the server
                                val answerJson = JSONObject().apply {
                                    put("sdp", answer.description)
                                    put("type", "answer")
                                }

                                val requestBody = answerJson.toString()
                                    .toRequestBody("application/sdp".toMediaTypeOrNull())

                                val answerRequest =
                                    Request.Builder().url(whepUrl).post(requestBody).build()
                                client.newCall(answerRequest).enqueue(object : Callback {
                                    override fun onFailure(call: Call, e: IOException) {
                                        println("WebRTC - Error sending answer: ${e.message}")
                                    }

                                    override fun onResponse(call: Call, response: Response) {
                                        if (response.isSuccessful) {
                                            println("WebRTC - Answer sent successfully")
                                        } else {
                                            println("WebRTC - Failed to send answer, code: ${response.code}")
                                        }
                                    }
                                })
                            }
                        }
                    }, MediaConstraints())

                } catch (e: JSONException) {
                    println("WebRTC - Error parsing SDP offer: ${e.message}")
                }
            }
        })
    }

    private fun createPeerConnection() {
        val options = PeerConnectionFactory.Options()
        val peerConnectionFactory1 =
            PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()

        val videoSource = peerConnectionFactory1?.createVideoSource(false)
        localVideoTrack = peerConnectionFactory1?.createVideoTrack("videoTrack", videoSource)


        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )
        try {
            localPeer = peerConnectionFactory1?.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: IceCandidate?) {
                        if (candidate != null) {
                            println("onIceCandidate not null")
                        } else {
                            println("onIceCandidate is null")
                        }
                    }

                    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                        if (p0 != null) {
                            println("onIceCandidatesRemoved not null")
                        } else {

                            println("onIceCandidatesRemoved is null")
                        }
                    }

                    override fun onAddStream(stream: MediaStream?) {
                        stream?.videoTracks?.forEach { videoTrack ->
                            println("onAddStream: $videoTrack")
                        }
                    }

                    override fun onRemoveStream(stream: MediaStream?) {
                        if (stream != null) {
                            println("onRemoveStream not null")
                        } else {

                            println("onRemoveStream is null")
                        }
                    }

                    override fun onDataChannel(channel: DataChannel?) {
                        if (channel != null) {
                            println("onDataChannel not null")
                        } else {
                            println("onDataChannel is null")
                        }
                    }

                    override fun onRenegotiationNeeded() {
                        println("onRenegotiationNeeded:")
                    }

                    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                        if (p0 != null) {
                            println("onSignalingChange not null")
                        } else {

                            println("onSignalingChange is null")
                        }
                    }

                    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
                    override fun onIceConnectionReceivingChange(receiving: Boolean) {
                        println("onIceConnectionReceivingChange:  $receiving")
                    }

                    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                        println("onIceGatheringChange:")
                    }
                },
            )

        } catch (e: Exception) {
            Log.e("WebRTC", "Error creating PeerConnection: ${e.message}")
        }

    }
}



