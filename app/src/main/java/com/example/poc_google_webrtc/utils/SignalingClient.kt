package com.example.poc_google_webrtc.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit


class SignalingClient(
    val serverUrl: String,
    val keyStreamId: String,
    val onMessageReceived: (message: String) -> Unit
) {
    private var webSocket: WebSocket? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.MILLISECONDS)
        .build()
    fun connectJanusServer() {
        val request = Request.Builder().url(serverUrl).build()
        println("Information of request: $request")
        webSocket = client.newWebSocket(
            request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("SignalingClient", "WebSocket Connected")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("SignalingClient", "Received: $text")
                    onMessageReceived(text)  // Pass the message to the callback
                }

                private fun onMessageReceived(message: String) {
                    Log.d("SignalingClient", "Received bytes: $message")
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    Log.d("SignalingClient", "Received bytes: $bytes")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("SignalingClient", "WebSocket Closed: $reason")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("SignalingClient", "WebSocket Error: ${t.message}")
                }
            }
        )
    }

    private fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    private fun onClose() {
        webSocket?.close(1000, "Closing Websocket")
    }
}
