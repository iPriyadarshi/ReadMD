package com.readmd.app.ui.reader

import android.content.Context
import com.readmd.app.domain.CodeCopy
import com.readmd.app.domain.CodeWrap
import com.readmd.app.domain.ImageNames
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
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
 * @param mathTextSizePx text size for rendered formulas, matching the body text.
 * @param mathColor color for rendered formulas, matching the body text.
 * @param docId the open document; relative image paths are resolved against its folder.
 * @param onImageClick receives the address of a tapped image (for the full-screen viewer).
 * @param onCopyCode receives the exact text of a code block whose "Copy" link was tapped.
 * @param codeWrap with [CodeWrap.SCROLL], blocks with long lines get a link that opens them in
 * the sideways-scrolling code viewer, which receives the text through [onViewCode].
 */
fun buildMarkwon(
    context: Context,
    dark: Boolean,
    mathTextSizePx: Float,
    mathColor: Int,
    docId: String,
    onImageClick: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    codeWrap: CodeWrap,
    onViewCode: (String) -> Unit,
    onLinkClick: (String) -> Unit,
): Markwon {
    val highlightTheme = if (dark) {
        Prism4jThemeDarkula.create()
    } else {
        Prism4jThemeDefault.create()
    }
    val codeCopy = CodeCopyPlugin(scrollLongLines = codeWrap == CodeWrap.SCROLL)
    return Markwon.builder(context)
        .usePlugin(codeCopy)
        .usePlugin(
            ImagesPlugin.create()
                .addSchemeHandler(ContentSchemeHandler(context))
                .addSchemeHandler(NetworkSchemeHandler.create())
                // Images that can't be loaded (missing file, no network) leave a named box.
                .errorHandler { url, _ ->
                    ImagePlaceholderDrawable(
                        label = "Image not available: ${ImageNames.displayName(url)}",
                        textSizePx = mathTextSizePx * 0.85f,
                        color = mathColor,
                    )
                },
        )
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, link ->
                    val copy = CodeCopy.index(link)
                    val view = CodeCopy.viewIndex(link)
                    when {
                        copy != null -> codeCopy.blockAt(copy)?.let { onCopyCode(it.text) }
                        view != null -> codeCopy.blockAt(view)?.let { onViewCode(it.text) }
                        else -> onLinkClick(link)
                    }
                }
                builder.imageDestinationProcessor(FolderImageDestinationProcessor(docId))
            }
        })
        .usePlugin(ImageClickPlugin(onImageClick))
        .usePlugin(CalloutPlugin(dark, context.resources.displayMetrics.density))
        .usePlugin(MarkwonInlineParserPlugin.create())
        .usePlugin(
            JLatexMathPlugin.create(mathTextSizePx) { builder ->
                builder.inlinesEnabled(true)
                builder.theme().textColor(mathColor)
                // Formulas that fail to parse show their source instead of disappearing.
                builder.errorHandler { latex, _ -> LatexErrorDrawable(latex, mathTextSizePx, mathColor) }
            },
        )
        .usePlugin(TablePlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(SyntaxHighlightPlugin.create(Prism4j(Prism4jLocator()), highlightTheme))
        .build()
}

