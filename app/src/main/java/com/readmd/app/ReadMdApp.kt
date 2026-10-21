package com.readmd.app

import android.app.Application
import android.os.Build
import com.readmd.app.data.ShortcutPublisher
import com.readmd.app.data.recent.RecentEntry
import com.readmd.app.di.AppContainer
import com.readmd.app.domain.AppInfo
import com.readmd.app.domain.CrashReport
import com.readmd.app.domain.ShortcutLabels
import com.readmd.app.widget.RecentsWidgetProvider
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

        installCrashHandler()
        appScope.launch {
            container.recentFiles.crashReportsEnabled.collect { container.crashes.enabled = it }
        }

        // Keep the launcher shortcuts in step with the recent files.
        val shortcuts = ShortcutPublisher(this)
        appScope.launch {
            container.recentFiles.recents
                .map { recents -> recents.take(ShortcutLabels.MAX_SHORTCUTS).map { it.id to it.name } }
                .distinctUntilChanged()
                .collect { pairs -> shortcuts.publish(pairs.map { (id, name) -> RecentEntry(id, name, 0L) }) }
        }

        // And the home-screen widget's list.
        appScope.launch {
            container.recentFiles.recents
                .map { recents -> recents.take(RecentsWidgetProvider.MAX_ITEMS).map { it.id to it.name } }
                .distinctUntilChanged()
                .collect { RecentsWidgetProvider.refresh(this@ReadMdApp) }
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Saves a report of any crash on this device, then lets the system handle it as usual.
     * Nothing is sent anywhere; see [CrashStore].
     */
    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // Reporting must never make a crash worse.
            runCatching {
                val now = System.currentTimeMillis()
                container.crashes.save(CrashReport.format(error, thread.name, appInfo(), now), now)
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun appInfo(): AppInfo {
        val info = packageManager.getPackageInfo(packageName, 0)
        return AppInfo(
            versionName = info.versionName.orEmpty(),
            versionCode = info.longVersionCode,
            androidRelease = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
        )
    }
}
