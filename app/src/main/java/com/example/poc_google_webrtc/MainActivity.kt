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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.net.MalformedURLException
import java.net.URL

class MainActivity : ComponentActivity() {

    private lateinit var signalingClient: SignalingClient
    private lateinit var webRTCManager: WebRTCManager

    private lateinit var eglBase: EglBase

    // private lateinit var surfaceViewRenderer: SurfaceViewRenderer
    private var peerConnection: PeerConnection? = null
    private var remoteRenderer: VideoTrack? = null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )


    private val keyStreamId: String = "livestream"
    private val serverUrl: String = "wss://your-signaling-server.com"


    private var peerConnectionFactory2: PeerConnectionFactory? = null
    private var localPeer: PeerConnection? = null
    private var videoView: SurfaceViewRenderer? = null

    private var localVideoTrack: VideoTrack? = null

    private var signalingSocket: Socket? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        /*signalingClient = SignalingClient(
            serverUrl = serverUrl,
            keyStreamId = keyStreamId
        ) { message ->
            webRTCManager.handleReceivedMessage(message)
        }
        eglBase = EglBase.create()

        signalingClient.connectJanusServer()

        webRTCManager = WebRTCManager(context = this)
        surfaceViewRenderer = SurfaceViewRenderer(this).apply {
            init(eglBase.eglBaseContext, null)
        }*/


        /*surfaceViewRenderer.setMirror(true)
        surfaceViewRenderer.setEnableHardwareScaler(true)
        window.statusBarColor = android.graphics.Color.WHITE*/
        // Set status bar icons to dark
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true // Dark icons
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PocgooglewebrtcTheme {

               /* MainScreen(
                    modifier = Modifier
                        .systemBarsPadding(),
                    context = this@MainActivity
                )*/

                Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .systemBarsPadding()
                         .background(color = Color.Red),
                     contentAlignment = Alignment.Center
                 ) {

                     Text("Center Content")
                 }
            }
        }



        // startStreaming(whepUrl = "https://oryx.lomasq.com/rtc/v1/whep/?app=live&stream=livestream&eip=91.108.105.153:8888")
        startStreaming(whepUrl = "http://local.mystream.test:8099/rtc/v1/whep/?app=live&stream=livestream")
    }

    private fun startStreaming(whepUrl: String) {
        println("WebRTC - Connecting to WHEP server: $whepUrl")

        val request = Request.Builder().url(whepUrl).post("".toRequestBody()).build()
        val client = OkHttpClient()

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
                                    .toRequestBody("application/json".toMediaTypeOrNull())

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


    /* private fun startStreaming(whepUrl: String) {
         println("WebRTC-=-=-=-=-=Connected to signaling server: $whepUrl")
         try {
             // Establish signaling connection
             signalingSocket = IO.socket(whepUrl)

             signalingSocket?.on(Socket.EVENT_CONNECT) {
                 println("WebRTC-=-=-=-=-=Connected to signaling server connected")
             }

             // Handle the offer from the server
             signalingSocket?.on("offer") { args ->
                 Log.d("WebRTC", "Received offer from signaling server: $args")
                 val offer = args[0] as JSONObject
                 val sdp = offer.getString("sdp")
                 val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)

                 // Set the remote description (server offer)
                 peerConnection?.setRemoteDescription(SimpleSdpObserver(), sessionDescription)

                 // Answer back with an answer
                 val answer = SessionDescription(SessionDescription.Type.ANSWER, "remote_sdp_here")
                 peerConnection?.setLocalDescription(SimpleSdpObserver(), answer)

                 // Send the answer back to the signaling server
                 signalingSocket?.emit("answer", answer)
             }

             // Handle ICE candidates
             signalingSocket?.on("candidate") { args ->
                 val candidate = args[0] as JSONObject
                 val iceCandidate = IceCandidate(
                     candidate.getString("id"),
                     candidate.getInt("label"),
                     candidate.getString("candidate")
                 )
                 peerConnection?.addIceCandidate(iceCandidate)
             }

             // Start the connection
             signalingSocket?.connect()
             createPeerConnection()

         } catch (e: Exception) {
             e.printStackTrace()
             println("WebRTC-=error:::::Connected to signaling server")
         }
     }*/

    private fun createPeerConnection() {

        val options = PeerConnectionFactory.Options()
        val  peerConnectionFactory1 =
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
        val result = localPeer?.iceConnectionState()

        if (localPeer != null) {

        } else {

        }
    }


    private fun startWebRTC(streamUrl: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val sdpOffer = createOffer()
            val sdpAnswer = sendOfferToServer(streamUrl, sdpOffer.toString())
            // establishPeerConnection(sdpAnswer.toString())
        }
    }

    @Throws(ResponseException::class)
    @OptIn(InternalAPI::class)
    private suspend fun sendOfferToServer(streamUrl: String, sdp: String): String {
        println("Establishing peer connection with stream URL: $streamUrl and SDP: $sdp")

        // Validate the URL format
        try {
            URL(streamUrl)  // Will throw an exception if the URL is malformed
        } catch (e: MalformedURLException) {
            throw IllegalArgumentException("Invalid stream URL: $streamUrl", e)
        }

        // Initialize HttpClient
        val client = HttpClient(CIO)

        try {
            println("Sending offer to server: $streamUrl")
            println("Request body: {\"sdp\": \"$sdp\", \"type\": \"offer\"}")

            val response: HttpResponse = client.post(streamUrl) {
                contentType(ContentType.Application.Json)
                body = """{"sdp": "$sdp", "type": "offer"}"""
            }
            /*val client = HttpClient(CIO) {
            engine {
                https {
                    // Use a custom SSLContext with hostname verification
                    sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                        override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                        override fun checkClientTrusted(certificates: Array<X509Certificate>, authType: String?) {}
                        override fun checkServerTrusted(certificates: Array<X509Certificate>, authType: String?) {
                            // Add your custom certificate validation logic if needed
                            // Here we let the client check the certificate correctly with hostname
                        }
                    }), java.security.SecureRandom())

                    hostnameVerifier = HostnameVerifier { hostname, session ->
                        HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                    }
                }
            }
        }*/
            // Check if response status is OK
            if (response.status.isSuccess()) {
                println("Offer sent successfully!")
                return response.bodyAsText() // Return the response text (typically an SDP answer or result)
            } else {
                println("Request failed with status: ${response.status}")
                throw Exception("Failed to send offer, server responded with status: ${response.status}")
            }
        } catch (e: HttpRequestTimeoutException) {
            println("Request timed out while sending offer: ${e.message}")
            throw e // Handle timeout error
        } catch (e: ResponseException) {
            println("Request failed: ${e.message}")
            throw e // Handle other types of response errors
        } catch (e: Exception) {
            println("Unexpected error occurred: ${e.message}")
            throw e
        } finally {
            // Close the HttpClient to release resources
            client.close()
        }
    }


    /* private fun establishPeerConnection(sdpAnswer: String) {
     println("establishPeerConnection sdpAnswer: $sdpAnswer")
     val answer = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
     peerConnection?.setRemoteDescription(object : SdpObserver {
         override fun onSetSuccess() {}
         override fun onSetFailure(error: String?) {}
         override fun onCreateSuccess(sdp: SessionDescription?) {}
         override fun onCreateFailure(error: String?) {}
     }, answer)
 }*/

    private fun createOffer() {
        val peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBase.eglBaseContext,
                    true,
                    true
                )
            )
            .createPeerConnectionFactory()


        peerConnection = peerConnectionFactory?.createPeerConnection(
            iceServers,
            object : CustomPeerConnectionObserver() {
                override fun onAddTrack(
                    receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?
                ) {
                    mediaStreams?.firstOrNull()?.videoTracks?.firstOrNull()?.let { videoTrack ->
                        remoteRenderer = videoTrack
                        // videoTrack.addSink(surfaceViewRenderer)
                    }
                }
            })


        if (peerConnection != null) {
            peerConnection!!.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                    peerConnection!!.setLocalDescription(this, sessionDescription)
                }

                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) {}
                override fun onSetFailure(error: String?) {}
            }, MediaConstraints())
        } else {
            Log.e("createOffer", "Failed to create PeerConnection.")
        }
    }

    @Composable
    fun MainScreen(modifier: Modifier = Modifier, context: Context, ) {
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
                    AndroidView(
                        factory = { context ->
                            SurfaceViewRenderer(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webRTCManager.remoteSurfaceViewRenderer = this
                            }
                        }, modifier = Modifier.fillMaxSize()
                    )
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
}