package com.example.poc_google_webrtc.view

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poc_google_webrtc.viewmodel.WebRTCViewModel
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer


/**
 * @Author: Jupyter.
 * @Date: 2/20/25.
 * @Email: koemheang200@gmail.com.
 */

@Composable
fun MainScreen(viewModel: WebRTCViewModel = viewModel()) {
    var isStopStream by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var surfaceViewRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

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
                .height(300.dp)
        ) {
            AndroidView(
                factory = {
                    SurfaceViewRenderer(context).apply {
                        init(viewModel.eglBaseContext, null)
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        surfaceViewRenderer = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Button(

            onClick = {
                isStopStream = !isStopStream
                if (isStopStream) {
                    viewModel.onStopStream()
                } else {
                    viewModel.generateSdpOffer { sdp ->
                        viewModel.sendOfferToServer(sdp)
                    }
                }

            },
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(
                text = if (isStopStream) "Stop Streaming" else "Start Streaming",
                color = if (isStopStream) Color.Red else Color.Green,
            )
        }
    }
}