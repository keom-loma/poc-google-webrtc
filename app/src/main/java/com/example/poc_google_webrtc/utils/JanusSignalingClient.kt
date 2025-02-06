package com.example.poc_google_webrtc.utils

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class JanusSignalingClient(private val serverUrl: String, private val keyStreamId: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun connectJanusServer() {
        val requestBody = JSONObject().apply {
            put("keyStreamId", keyStreamId)
        }.toString()

        val request = Request.Builder()
            .url(serverUrl)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .build()

        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("JanusSignalingClient", "Failed to connect to Janus: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("JanusSignalingClient", "Connected to Janus: ${response.body?.string()}")
                }
            },
        )
    }
}
