package com.example.poc_google_webrtc

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.poc_google_webrtc.ui.theme.PocgooglewebrtcTheme
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import okio.use
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class MainActivity : ComponentActivity() {

    private var peerConnection: PeerConnection? = null
    private var remoteRenderer: VideoTrack? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private lateinit var surfaceViewRenderer: SurfaceViewRenderer

    private val iceServers = listOf(
        IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    companion object {
        var streamUrl =
            "https://oryx.lomasq.com/rtc/v1/whep/?app=live&stream=livestream&eip=91.108.105.153:8888"
    }

    private val eglBaseContext = EglBase.create().eglBaseContext
    private val options = PeerConnectionFactory.Options()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true // Dark icons
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PocgooglewebrtcTheme {
                val scope = rememberCoroutineScope()
                Column(
                    modifier = Modifier
                        .fillMaxSize()

                ) {
                    LaunchedEffect(Unit) {
                        scope.launch {
                            initializePeerConnectionFactory()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            content = {
                                AndroidView(
                                    factory = { context ->
                                        SurfaceViewRenderer(context).apply {
                                            init(eglBaseContext, null)
                                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                                            surfaceViewRenderer = this
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                        )

                        Button(
                            onClick = {
                                if (peerConnectionFactory == null) {
                                    Log.e("Error", "peerConnectionFactory is null")
                                } else {
                                    generateSdpOffer()
                                }
                            },
                            modifier = Modifier.padding(top = 20.dp)
                        ) {
                            Text(text = "Start Stream")
                        }
                    }
                }
            }
        }
    }

    private fun initializePeerConnectionFactory() {
        // Initialize WebRTC PeerConnectionFactory
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()
        )

        // Create PeerConnectionFactory once initialization is done
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    private fun generateSdpOffer() {
        if (peerConnectionFactory == null) {
            return
        }
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        val config = PeerConnection.RTCConfiguration(iceServers)

        // Initialize the PeerConnection
        peerConnection =
            peerConnectionFactory?.createPeerConnection(config, object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

                override fun onAddStream(stream: MediaStream?) {
                    stream?.let { mediaStream ->
                        if (mediaStream.videoTracks.isNotEmpty()) {
                            val remoteTrack = mediaStream.videoTracks[0]
                            remoteRenderer = remoteTrack
                            remoteRenderer?.addSink(surfaceViewRenderer)
                        }
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


        // Create SDP offer
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp != null) {
                    val correctSdp = fixMLineOrder(sdp.description)
                    val modifiedSessionDescription =
                        SessionDescription(SessionDescription.Type.OFFER, correctSdp)

                    // Corrected way to set local description
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            sendOfferToServer(modifiedSessionDescription) // Send offer after setting it
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

    private fun sendOfferToServer(it: SessionDescription) {
        val client = OkHttpClient()
        val request = Request.Builder().url(streamUrl).header("Content-Type", "application/sdp")
            .post(it.description.toString().toRequestBody()).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body.use { responseBody ->
                    val sdpResponse = responseBody?.string()
                    val sessionDescription = SessionDescription(
                        SessionDescription.Type.ANSWER, sdpResponse
                    )
                    setRemoteDescription(sessionDescription)

                }
            }

            override fun onFailure(call: Call, e: IOException) {}
        })
    }

    private fun setRemoteDescription(sessionDescription: SessionDescription) {
        peerConnection?.let {
            it.setRemoteDescription(sessionDescription.description.let {
                object : SdpObserver {
                    override fun onSetSuccess() {}

                    override fun onSetFailure(error: String?) {}

                    override fun onCreateSuccess(sdp: SessionDescription?) {}

                    override fun onCreateFailure(error: String?) {
                    }
                }
            }, sessionDescription)
        } ?: Log.e("SDP Answer", "PeerConnection is null!")
    }

    // Function to fix the order of m-lines in SDP
    private fun fixMLineOrder(sdp: String): String {
        val lines = sdp.split("\n").toMutableList()
        val mLines = lines.filter { it.startsWith("m=") }

        if (mLines.size > 1) {
            val audioIndex = lines.indexOfFirst { it.startsWith("m=audio") }
            val videoIndex = lines.indexOfFirst { it.startsWith("m=video") }

            if (audioIndex != -1 && videoIndex != -1 && audioIndex > videoIndex) {
                // Swap lines if the order is incorrect
                lines[audioIndex] = mLines[1]
                lines[videoIndex] = mLines[0]
            }
        }
        val result = lines.joinToString("\n")
        return result
    }
}

