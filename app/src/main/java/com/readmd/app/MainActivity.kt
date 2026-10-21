package com.readmd.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.readmd.app.data.SharedContentImporter
import com.readmd.app.ui.ReadMdRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    /** A doc opened via ACTION_VIEW ("Open with ReadMD"), consumed once navigated. */
    private val externalDocId: MutableState<String?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A recreated activity (rotation) must not open the same document a second time.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            ReadMdRoot(
                externalDocId = externalDocId,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Opens what "Open with" or the share sheet handed to the app. */
    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> externalDocId.value = parseViewDocId(intent)
            Intent.ACTION_SEND -> {
                val importer = SharedContentImporter(applicationContext)
                lifecycleScope.launch {
                    val docId = withContext(Dispatchers.IO) { importer.importFrom(intent) }
                    if (docId != null) {
                        externalDocId.value = docId
                    } else {
                        Toast.makeText(this@MainActivity, "Nothing to open in the shared content", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun parseViewDocId(intent: Intent): String? {
        val uri = intent.data ?: return null
        (application as? ReadMdApp)?.container?.documents?.takePersistablePermission(uri)
        return uri.toString()
    }
}
