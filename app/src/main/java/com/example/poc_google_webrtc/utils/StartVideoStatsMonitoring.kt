package com.example.poc_google_webrtc.utils

import android.os.Handler
import android.os.Looper
import org.webrtc.VideoTrack

/**
 * @Author: Jupyter.
 * @Date: 2/21/25.
 * @Email: koemheang200@gmail.com.
 */
open class VideoStatsMonitoring {
    fun startVideoStatsMonitoring(remoteTrack: VideoTrack) {
        val handler = Handler(Looper.getMainLooper())
        val interval: Long = 1000 // 1 second interval

    }
}