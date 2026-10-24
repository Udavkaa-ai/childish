package ru.sovushka.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import ru.sovushka.app.ui.AppRoot
import ru.sovushka.app.ui.MicGate
import ru.sovushka.app.ui.SovushkaTheme

class MainActivity : ComponentActivity(), MicGate {
    private var micWaiter: CompletableDeferred<Boolean>? = null
    private val micRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micWaiter?.complete(granted)
        micWaiter = null
    }

    override suspend fun ensureMic(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) return true
        val w = CompletableDeferred<Boolean>()
        micWaiter = w
        micRequest.launch(Manifest.permission.RECORD_AUDIO)
        return w.await()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SovushkaTheme {
                AppRoot(this)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        graph.speaker.stop()
    }
}
