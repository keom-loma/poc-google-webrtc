/*
package com.example.poc_google_webrtc.utils

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import org.webrtc.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebRTCManager(private val context: Context) {
    private val eglBase: EglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    var localVideoSource: VideoSource? = null
    var localVideoTrack: VideoTrack? = null
    var localSurfaceViewRenderer: SurfaceViewRenderer? = null

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val factoryOptions = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(
        rtcConfig: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer
    ): PeerConnection? {
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        return peerConnection
    }

    fun startLocalVideo(surfaceViewRenderer: SurfaceViewRenderer) {
        localSurfaceViewRenderer = surfaceViewRenderer
        val videoCapturer = createCameraCapturer() ?: return
        localVideoSource = peerConnectionFactory?.createVideoSource(videoCapturer.isScreencast)
        videoCapturer.initialize(
            SurfaceTextureHelper.create(
                "CaptureThread",
                eglBase.eglBaseContext
            ), context, localVideoSource?.capturerObserver
        )
        videoCapturer.startCapture(1280, 720, 30)
        localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", localVideoSource)
        localVideoTrack?.addSink(localSurfaceViewRenderer)
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        return enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?.let { enumerator.createCapturer(it, null) }
    }
}

class WebRTCClient(
    private val webRTCManager: WebRTCManager,
    private val signalingClient: JanusSignalingClient
) {
    private var peerConnection: PeerConnection? = null

    fun initializePeerConnection(
        rtcConfig: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer
    ) {
        peerConnection = webRTCManager.createPeerConnection(rtcConfig, observer)
        signalingClient.connectJanusServer()
    }

    fun closeConnection() {
        peerConnection?.close()
        peerConnection = null
    }
}

class JanusSignalingClient(private val serverUrl: String, private val keyStreamId: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun connectJanusServer() {
        val requestBody = JSONObject().apply {
            put("janus", "create")
            put("transaction", "unique_transaction_id")
        }.toString()

        val request = Request.Builder()
            .url(serverUrl)
            .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("JanusSignalingClient", "Failed to connect to Janus: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("JanusSignalingClient", "Connected to Janus: ${response.body()?.string()}")
            }
        })
    }
}

@Composable
fun WebRTCVideoView(webRTCManager: WebRTCManager) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webRTCManager.startLocalVideo(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun MainScreen(webRTCManager: WebRTCManager) {
    Column(modifier = Modifier.fillMaxSize()) {
        WebRTCVideoView(webRTCManager)
    }
}
*/
