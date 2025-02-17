package com.example.poc_google_webrtc.video_player

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun VideoPlayerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataToPass =
        remember { "https://oryx.lomasq.com/rtc/v1/whep/?app=live&stream=livestream&eip=91.108.105.153:8888" }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem =
                MediaItem.fromUri("https://youtu.be/eycriPX34Tk?list=RDMMeycriPX34Tk")
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()  // Clean up when the composable leaves the screen
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {

                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                setOnTouchListener { _, _ -> true }

                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = false

                loadUrl("https://test-webrtc-jupyter.vercel.app/")
                // loadUrl("http://local.mystream.test:8099/rtc/v1/whep/?app=live&stream=livestream")
                addJavascriptInterface(WebAppInterface(context), dataToPass)


                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript("receiveDataFromApp('$dataToPass')", null)
                        view?.evaluateJavascript(
                            """
                        document.getElementById('myButton').addEventListener('click', function() {
                            var inputValue = document.getElementById('inputField').value;
                            Android.handleButtonClick(inputValue);
                        });
                        """, null
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()

    )
}

class WebAppInterface(private val context: android.content.Context) {
    @JavascriptInterface
    fun handleButtonClick(inputValue: String) {
        android.widget.Toast.makeText(
            context,
            "Button clicked! Input: $inputValue",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}