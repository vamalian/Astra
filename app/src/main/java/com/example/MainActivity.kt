package com.example

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.AstraAdminApp
import com.example.ui.AstraAdminViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AstraAdminViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AstraAdminViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                AstraAdminApp(viewModel = viewModel)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Any interaction across the tablet window resets the inactivity lock timer
        if (ev?.actionMasked == MotionEvent.ACTION_DOWN) {
            viewModel.onUserInteraction()
        }
        return super.dispatchTouchEvent(ev)
    }
}
