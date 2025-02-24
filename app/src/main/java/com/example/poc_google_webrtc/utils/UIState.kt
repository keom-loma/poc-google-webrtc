package com.example.poc_google_webrtc.utils

/**
 * @Author: Jupyter.
 * @Date: 2/21/25.
 * @Email: koemheang200@gmail.com.
 */
sealed class UIState {
    data object Loading : UIState()
    data object Streaming : UIState()
    data object Error : UIState()
    data object NoStream : UIState()
}