package com.readmd.app

import android.app.Application
import com.readmd.app.data.ShortcutPublisher
import com.readmd.app.data.recent.RecentEntry
import com.readmd.app.di.AppContainer
import com.readmd.app.domain.ShortcutLabels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReadMdApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Keep the launcher shortcuts in step with the recent files.
        val shortcuts = ShortcutPublisher(this)
        appScope.launch {
            container.recentFiles.recents
                .map { recents -> recents.take(ShortcutLabels.MAX_SHORTCUTS).map { it.id to it.name } }
                .distinctUntilChanged()
                .collect { pairs -> shortcuts.publish(pairs.map { (id, name) -> RecentEntry(id, name, 0L) }) }
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
