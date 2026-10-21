package com.readmd.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.readmd.app.R
import com.readmd.app.ReadMdApp
import com.readmd.app.data.recent.RecentEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Supplies the rows of the recents widget's list. */
class RecentsWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = RecentsFactory(applicationContext)

    private class RecentsFactory(private val context: Context) : RemoteViewsFactory {

        private var items: List<RecentEntry> = emptyList()

        override fun onCreate() = Unit

        // Called on a binder thread each time the widget asks for fresh data.
        override fun onDataSetChanged() {
            val container = (context.applicationContext as ReadMdApp).container
            items = runBlocking { container.recentFiles.recents.first() }
                .take(RecentsWidgetProvider.MAX_ITEMS)
        }

        override fun onDestroy() {
            items = emptyList()
        }

        override fun getCount(): Int = items.size

        override fun getViewAt(position: Int): RemoteViews {
            val item = items.getOrNull(position)
                ?: return RemoteViews(context.packageName, R.layout.widget_recents_item)
            return RemoteViews(context.packageName, R.layout.widget_recents_item).apply {
                setTextViewText(R.id.item_title, item.name)
                setOnClickFillInIntent(R.id.item_title, Intent().setData(Uri.parse(item.id)))
            }
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = false
    }
}
