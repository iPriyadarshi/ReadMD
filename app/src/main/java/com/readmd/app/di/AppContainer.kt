package com.readmd.app.di

import android.content.Context
import com.readmd.app.data.AndroidDocumentMessages
import com.readmd.app.data.CrashStore
import com.readmd.app.data.AndroidDocumentSource
import com.readmd.app.data.DocumentRepository
import com.readmd.app.data.FolderScanner
import com.readmd.app.data.FolderSearcher
import com.readmd.app.data.recent.RecentFilesStore
import java.io.File

/** Simple manual dependency container (application-scoped). */
class AppContainer(context: Context) {
    /** For user-visible messages produced outside composables, so they follow the current language. */
    val appContext: Context = context.applicationContext

    val recentFiles = RecentFilesStore(context)
    val documents = DocumentRepository(AndroidDocumentSource(context), AndroidDocumentMessages(context))
    val folderScanner = FolderScanner(context)
    val folderSearcher = FolderSearcher(documents)

    /** Crash reports stay in the app's private storage until the user chooses to share one. */
    val crashes = CrashStore(File(context.filesDir, "crashes"))
}
