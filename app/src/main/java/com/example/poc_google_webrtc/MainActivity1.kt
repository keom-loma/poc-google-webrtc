package com.example.poc_google_webrtc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.poc_google_webrtc.view.MainScreen

/**
 * @Author: Jupyter.
 * @Date: 2/20/25.
 * @Email: koemheang200@gmail.com.
 */
class MainActivity1: ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}