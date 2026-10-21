package com.readmd.app.ui.reader

import android.content.Context
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.NetworkSchemeHandler
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j

/**
 * Builds the shared Markwon renderer used by the reader screen.
 *
 * @param dark selects the code-block highlight theme (Darkula vs default)
 * so code blocks follow the app's current light/dark mode.
 * @param onLinkClick receives every tapped link so the reader can follow
 * relative markdown links itself and hand everything else to the system.
 */
fun buildMarkwon(context: Context, dark: Boolean, onLinkClick: (String) -> Unit): Markwon {
    val highlightTheme = if (dark) {
        Prism4jThemeDarkula.create()
    } else {
        Prism4jThemeDefault.create()
    }
    return Markwon.builder(context)
        .usePlugin(
            ImagesPlugin.create()
                .addSchemeHandler(ContentSchemeHandler(context))
                .addSchemeHandler(NetworkSchemeHandler.create()),
        )
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, link -> onLinkClick(link) }
            }
        })
        .usePlugin(TablePlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(SyntaxHighlightPlugin.create(Prism4j(Prism4jLocator()), highlightTheme))
        .build()
}

