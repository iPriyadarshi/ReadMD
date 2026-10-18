package com.readmd.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.readmd.app.ui.ReadMdRoot

class MainActivity : ComponentActivity() {

    /** A doc opened via ACTION_VIEW ("Open with ReadMD"), consumed once navigated. */
    private val externalDocId: MutableState<String?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        externalDocId.value = parseDocId(intent)
        setContent {
            ReadMdRoot(
                externalDocId = externalDocId,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        externalDocId.value = parseDocId(intent)
    }

    private fun parseDocId(intent: Intent?): String? {
        val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
        (application as? ReadMdApp)?.container?.documents?.takePersistablePermission(uri)
        return uri.toString()
    }
}
