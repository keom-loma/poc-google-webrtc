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
import com.example.poc_google_webrtc.utils.JanusSignalingClient
import com.example.poc_google_webrtc.utils.WebRTCClient
import com.example.poc_google_webrtc.utils.WebRTCManager
import org.webrtc.SurfaceViewRenderer

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reLoadLibrary()
        setContent {
            val webRTCManager = WebRTCManager(this)
            val janusSignalingClient = JanusSignalingClient("webRTCManager", "signalingClient")

            PocgooglewebrtcTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        serverUrl = " webrtc://localhost/live",
                        keyStreamId = "livestream",
                        context = this
                    )
                }
            }
        }
    }

    private fun reLoadLibrary() {
        try {
            val loadLibrary = System.loadLibrary("jingle_peerconnection_so")
            println("LoadLibrary-=-=-=-=-=-=-= successful: $loadLibrary")
        } catch (e: Exception) {
            println("LoadLibrary-=-=-=-=-=-=-= exception: $")
        }
    }
}

@Composable
fun RemoteVideoView(
    modifier: Modifier = Modifier,
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

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    serverUrl: String,
    keyStreamId: String,
    context: Context
) {

    val webRTCManager = remember { WebRTCManager(context) }
    val signalingClient = remember { JanusSignalingClient(serverUrl, keyStreamId) }
    val webRTCClient = remember { WebRTCClient(webRTCManager, signalingClient) }
    println("Information of webRTCClient: $webRTCClient")
    Column(modifier = Modifier.fillMaxSize()) {
        RemoteVideoView(
            modifier = Modifier.fillMaxSize(),
            webRTCManager
        )
    }

}