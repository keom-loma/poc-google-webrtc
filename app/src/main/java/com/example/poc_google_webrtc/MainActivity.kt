package com.example.poc_google_webrtc

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.poc_google_webrtc.ui.theme.PocgooglewebrtcTheme
import com.example.poc_google_webrtc.utils.CustomPeerConnectionObserver
import com.example.poc_google_webrtc.utils.SignalingClient
import com.example.poc_google_webrtc.utils.WebRTCManager
import com.example.poc_google_webrtc.video_player.VideoPlayerScreen
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.InternalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class MainActivity : ComponentActivity() {

    private lateinit var signalingClient: SignalingClient
    private lateinit var webRTCManager: WebRTCManager

    private lateinit var eglBase: EglBase
    private lateinit var surfaceViewRenderer: SurfaceViewRenderer
    private var peerConnection: PeerConnection? = null
    private var remoteRenderer: VideoTrack? = null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )


    private val keyStreamId: String = "livestream"
    private val serverUrl: String = "wss://your-signaling-server.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signalingClient = SignalingClient(
            serverUrl = serverUrl, keyStreamId = keyStreamId
        ) { message ->
            webRTCManager.handleReceivedMessage(message)
        }
        eglBase = EglBase.create()

        signalingClient.connectJanusServer()

        webRTCManager = WebRTCManager(context = this)
        surfaceViewRenderer = SurfaceViewRenderer(this).apply {
            init(eglBase.eglBaseContext, null)
        }
        surfaceViewRenderer.setMirror(true)
        surfaceViewRenderer.setEnableHardwareScaler(true)
        window.statusBarColor = android.graphics.Color.WHITE
        // Set status bar icons to dark
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true // Dark icons
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PocgooglewebrtcTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .background(color = Color.Red)
                ) {
                    VideoPlayerScreen(
                        modifier = Modifier
                            .systemBarsPadding()
                    )
                }

            }
        }
    }

    private fun startWebRTC(streamUrl: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val sdpOffer = createOffer()
            println("Information of sdpOffer: $sdpOffer")
            // val sdpAnswer = sendOfferToServer(streamUrl, sdpOffer.toString())
            // establishPeerConnection(sdpAnswer.toString())
        }
    }

    @Throws(ResponseException::class)
    @OptIn(InternalAPI::class)
    private suspend fun sendOfferToServer(streamUrl: String, sdp: String): String {
        println("Establishing peer connection with stream URL: $streamUrl and SDP: $sdp")

        // Initialize HttpClient
        val client = HttpClient(CIO)

        try {
            val response: HttpResponse = client.post(streamUrl) {
                contentType(ContentType.Application.Json)
                body = """{"sdp": "$sdp", "type": "offer"}"""
            }

            // Check if response status is OK
            if (response.status.isSuccess()) {
                return response.bodyAsText() // Return the response text (typically an SDP answer or result)
            } else {
                throw Throwable()
            }
        } catch (e: HttpRequestTimeoutException) {
            println("Request timed out: ${e.message}")
            throw e // You can handle the timeout error here
        } catch (e: ResponseException) {
            println("Request failed: ${e.message}")
            throw e // Handle other types of response errors
        } finally {
            // Close the HttpClient to release resources
            client.close()
        }
    }

    private fun establishPeerConnection(sdpAnswer: String) {
        println("establishPeerConnection sdpAnswer: $sdpAnswer")
        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, answer)
    }

    override fun onDestroy() {
        super.onDestroy()
        surfaceViewRenderer.release()
        eglBase.release()
        peerConnection?.close()
    }

    private fun createOffer() {
        val peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()


        peerConnection = peerConnectionFactory.createPeerConnection(iceServers,
            object : CustomPeerConnectionObserver() {
                override fun onAddTrack(
                    receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?
                ) {
                    mediaStreams?.firstOrNull()?.videoTracks?.firstOrNull()?.let { videoTrack ->
                        remoteRenderer = videoTrack
                        videoTrack.addSink(surfaceViewRenderer)
                    }
                }
            })
        peerConnection!!.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                peerConnection!!.setLocalDescription(this, sessionDescription)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())

    }

    @Composable
    fun MainScreen(
        modifier: Modifier = Modifier, context: Context,

        ) {
        var streamUrl by remember { mutableStateOf("https://oryx.lomasq.com/rtc/v1/whep/?app=live&stream=livestream&eip=91.108.105.153:8888") }
        var isPlaying by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 36.dp, start = 16.dp, end = 16.dp
                )
        ) {
            TextField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                label = { Text("Enter Stream URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    isPlaying = true
                    startWebRTC(streamUrl)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Start Streaming")
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Red
                        )
                ) {
                    WebView(context).apply {
                        webViewClient = WebViewClient() // Handle page navigation within the WebView
                        settings.javaScriptEnabled = true // Enable JavaScript (optional)
                        loadUrl("https://youtu.be/eycriPX34Tk?list=RDMMeycriPX34Tk")
                    }
                    /* AndroidView(
                         factory = { surfaceViewRenderer },
                         modifier = Modifier.fillMaxSize()
                     )*/
                }
            }
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

