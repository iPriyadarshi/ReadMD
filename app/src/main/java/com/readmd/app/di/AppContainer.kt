package com.readmd.app.di

import android.content.Context
import com.readmd.app.data.DocumentRepository
import com.readmd.app.data.FolderScanner
import com.readmd.app.data.FolderSearcher
import com.readmd.app.data.recent.RecentFilesStore

/** Simple manual dependency container (application-scoped). */
class AppContainer(context: Context) {
    val recentFiles = RecentFilesStore(context)
    val documents = DocumentRepository(context)
    val folderScanner = FolderScanner(context)
    val folderSearcher = FolderSearcher(documents)
}
