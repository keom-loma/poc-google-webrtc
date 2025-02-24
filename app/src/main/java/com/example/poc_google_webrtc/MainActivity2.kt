package com.example.poc_google_webrtc

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.use
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.Logging
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
import java.io.IOException

/**
 * @Author: Jupyter.
 * @Date: 2/21/25.
 * @Email: koemheang200@gmail.com.
 */
class MainActivity2 : ComponentActivity() {

    companion object {
        const val VIDEO_WIDTH = 1280
        const val VIDEO_HEIGHT = 720
        var streamUrl =
            "https://oryx.lomasq.com/rtc/v1/whep/?app=live&stream=livestream&eip=91.108.105.153:8888"
    }

    private var peerConnection: PeerConnection? = null
    private var remoteRenderer: VideoTrack? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val options = PeerConnectionFactory.Options()

    private val videoTrack: VideoTrack? = null

    private val iceServers = listOf(
        IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )
    private lateinit var surfaceViewRenderer: SurfaceViewRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContentView(R.layout.activity_main2)

        val btnStream: Button = findViewById(R.id.connectSever)
        val btnStop: Button = findViewById(R.id.stopServer)
        val noStreamImage: View = findViewById(R.id.no_stream_image)
        val imageWrapper: FrameLayout = findViewById(R.id.image_wrapper)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        WindowCompat.setDecorFitsSystemWindows(window, false)
        surfaceViewRenderer = findViewById(R.id.surface_view2)

        initializePeerConnectionFactory()
        initializeSurfaceView()


        btnStream.setOnClickListener {
            btnStream.visibility = Button.GONE
            btnStop.visibility = Button.VISIBLE
            noStreamImage.visibility = View.GONE
            imageWrapper.visibility = View.GONE
            generateSdpOffer()
        }
        btnStop.setOnClickListener {
            btnStream.visibility = Button.VISIBLE
            btnStop.visibility = Button.GONE
            noStreamImage.visibility = View.VISIBLE
            imageWrapper.visibility = View.VISIBLE
            stopStream()
        }
    }

    private fun initializeSurfaceView() {
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val adjustedHeight = (screenWidth.toFloat() / VIDEO_WIDTH) * VIDEO_HEIGHT

        val layoutParams = surfaceViewRenderer.layoutParams
        layoutParams.width = screenWidth
        layoutParams.height = adjustedHeight.toInt()
        surfaceViewRenderer.layoutParams = layoutParams
        surfaceViewRenderer.apply {
            init(eglBaseContext, null)
            setMirror(false)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setEnableHardwareScaler(true)
            requestLayout()
        }
    }

    private fun initializePeerConnectionFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()

        )
        peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options)
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

        peerConnection = peerConnectionFactory?.createPeerConnection(
            PeerConnection.RTCConfiguration(iceServers),
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

                override fun onAddStream(stream: MediaStream?) {
                    stream?.let { mediaStream ->
                        if (mediaStream.videoTracks.isNotEmpty()) {
                            val remoteTrack = mediaStream.videoTracks[0]
                            remoteTrack.addSink { frame ->
                                Log.d(
                                    "VideoFrame",
                                    "Width: ${frame.buffer.width}, Height: ${frame.buffer.height}"
                                )

                            }

                            remoteRenderer = remoteTrack

                            runOnUiThread {
                                remoteRenderer?.addSink(surfaceViewRenderer)
                            }
                        } else {
                            Log.d(
                                "VideoFrame", "Else condition of addTrackVideo"
                            )
                        }
                    } ?: run {
                        Log.d(
                            "VideoFrame", "Else condition of run... block onAddVideo"
                        )
                    }
                }

                override fun onRemoveStream(stream: MediaStream?) {
                    println("onRemoveStream: ${stream?.videoTracks}")
                }
                override fun onDataChannel(channel: DataChannel?) {
                    println("onDataChannel: $channel")
                }
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

    private fun stopStream() {
        peerConnection?.close()
        peerConnection = null
    }

}