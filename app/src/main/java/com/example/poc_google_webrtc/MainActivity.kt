package com.example.poc_google_webrtc

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.poc_google_webrtc.ui.theme.PocgooglewebrtcTheme
import com.example.poc_google_webrtc.utils.SignalingClient
import com.example.poc_google_webrtc.utils.WebRTCManager
import com.example.poc_google_webrtc.video_player.VideoPlayerScreen
import org.webrtc.SurfaceViewRenderer

class MainActivity : ComponentActivity() {


    private lateinit var signalingClient: SignalingClient
    private lateinit var webRTCManager: WebRTCManager


    private val keyStreamId: String = "livestream"
    private val serverUrl: String = "wss://your-signaling-server.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signalingClient = SignalingClient(
            serverUrl = serverUrl,
            keyStreamId = keyStreamId
        ) { message ->
            webRTCManager.handleReceivedMessage(message)
        }

        signalingClient.connectJanusServer()

        webRTCManager = WebRTCManager(context = this)


        setContent {
            PocgooglewebrtcTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VideoPlayerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                    /*MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        context = this
                    )*/
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    context: Context
) {
    val webRTCManager = remember { WebRTCManager(context) }
    Column(modifier = Modifier.fillMaxSize()) {
        RemoteVideoView(webRTCManager)
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
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webRTCManager.remoteSurfaceViewRenderer = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

