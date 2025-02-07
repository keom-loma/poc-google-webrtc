package com.example.poc_google_webrtc.video_player

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
                loadUrl("https://youtu.be/-p5IHoJ8tYY")
            }
           /* PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }*/
        },
        modifier = Modifier.fillMaxSize()
    )
}