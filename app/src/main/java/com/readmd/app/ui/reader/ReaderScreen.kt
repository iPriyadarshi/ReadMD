package com.readmd.app.ui.reader

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.readmd.app.ReadMdApp
import com.readmd.app.domain.Bookmarks
import com.readmd.app.domain.CodeWrap
import com.readmd.app.domain.EditPolicy
import com.readmd.app.domain.ExportNames
import com.readmd.app.domain.ExternalLinks
import com.readmd.app.domain.Footnotes
import com.readmd.app.domain.HeadingAnchors
import com.readmd.app.domain.LeaveAction
import com.readmd.app.domain.HeadingNode
import com.readmd.app.domain.LineSpacing
import com.readmd.app.domain.PageMargin
import com.readmd.app.domain.MarkdownTables
import com.readmd.app.domain.MermaidBlocks
import com.readmd.app.domain.ReaderFont
import com.readmd.app.domain.ShareText
import com.readmd.app.domain.ReadingPositions
import com.readmd.app.domain.ReadingTime
import com.readmd.app.domain.TextScale
import com.readmd.app.domain.WindowLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    docId: String,
    onBack: () -> Unit,
    onOpenLinked: (String) -> Unit,
    canGoForward: Boolean,
    onForward: () -> Unit,
    initialQuery: String? = null,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as ReadMdApp).container
    val viewModel: ReaderViewModel = viewModel(factory = ReaderViewModel.factory(docId))
    val state by viewModel.state.collectAsState()

    val controller = remember { ReaderTextController() }
    val renderedScroll = rememberScrollState()
    val rawScroll = rememberScrollState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Bumped after each successful render so search state can re-anchor.
    var renderedGeneration by remember { mutableIntStateOf(0) }
    remember(controller) {
        controller.onRendered = { renderedGeneration++ }
    }

    // Opening from a folder search starts with the search bar showing the query,
    // which scrolls to the first match once the document has rendered.
    var searchActive by rememberSaveable { mutableStateOf(initialQuery != null) }
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var currentMatch by remember { mutableIntStateOf(0) }
    var textSizeMenuOpen by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val renderedText: CharSequence = remember(renderedGeneration) {
        controller.textView?.text ?: ""
    }
    val matches = remember(query, renderedText) { findMatches(renderedText.toString(), query) }

    val highlightColor = MaterialTheme.colorScheme.secondaryContainer
    val currentHighlightColor = MaterialTheme.colorScheme.tertiaryContainer

    // Apply/clear highlight spans whenever inputs change.
    LaunchedEffect(matches, currentMatch, renderedGeneration, searchActive, highlightColor, currentHighlightColor) {
        if (searchActive && query.isNotEmpty() && matches.isNotEmpty()) {
            controller.applySearch(
                matches = matches,
                currentIndex = currentMatch.coerceIn(0, matches.size - 1),
                matchColor = highlightColor.toArgb(),
                currentColor = currentHighlightColor.toArgb(),
            )
        } else {
            controller.clearSearch()
        }
    }

    // Scroll to the active match.
    LaunchedEffect(currentMatch, matches, renderedGeneration) {
        if (matches.isNotEmpty() && !state.rawMode) {
            val index = currentMatch.coerceIn(0, matches.size - 1)
            controller.pixelOffsetForMatch(matches, index)?.let {
                val target = (it - 40f).coerceAtLeast(0f)
                renderedScroll.animateScrollTo(target.roundToInt())
            }
        }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) focusRequester.requestFocus()
    }

    // Restore the saved reading position once the first render has been laid out,
    // then keep saving it (as a fraction of the scroll range) while the user reads.
    var positionRestored by remember { mutableStateOf(false) }
    LaunchedEffect(renderedGeneration) {
        if (positionRestored || renderedGeneration == 0) return@LaunchedEffect
        val saved = if (initialQuery != null) 0 else viewModel.savedPosition()
        if (saved > 0) {
            val max = withTimeoutOrNull(1_000) {
                snapshotFlow { renderedScroll.maxValue }.first { it > 0 }
            }
            if (max != null) renderedScroll.scrollTo(max * saved / ReadingPositions.FULL)
        }
        positionRestored = true
    }
    LaunchedEffect(positionRestored) {
        if (!positionRestored) return@LaunchedEffect
        snapshotFlow { renderedScroll.value }.collectLatest { value ->
            delay(400)
            val max = renderedScroll.maxValue
            if (max > 0) viewModel.savePosition(value * ReadingPositions.FULL / max)
        }
    }

    // Recomputed only when the label text changes, not on every scroll pixel.
    val scrollState = if (state.rawMode) rawScroll else renderedScroll
    val timeLeftLabel by remember(state.wordCount, scrollState) {
        derivedStateOf {
            val progress = if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue
            } else {
                0f
            }
            if (state.loading || state.error != null) null else ReadingTime.label(state.wordCount, progress)
        }
    }

    // Keep the display awake only while this screen is showing and the setting is on.
    val view = LocalView.current
    DisposableEffect(view, state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val snackbarHost = remember { SnackbarHostState() }
    var frontMatterExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.frontMatter.isEmpty()) {
        if (state.frontMatter.isEmpty()) controller.topInset = 0f
    }

    val anchors = remember(state.headings) { HeadingAnchors.anchors(state.headings) }
    val speech = remember { SpeechController(context) }
    DisposableEffect(speech) { onDispose { speech.release() } }
    LaunchedEffect(speech.error) {
        speech.error?.let {
            snackbarHost.showSnackbar(it)
            speech.error = null
        }
    }
    // Speech stops when the document is edited, switched to the raw view or the app is backgrounded.
    LaunchedEffect(state.editing, state.rawMode) {
        if (state.editing || state.rawMode) speech.stop()
    }
    var editorMenuOpen by remember { mutableStateOf(false) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    val printer = remember { DocumentPrinter(context) }
    val documentHtml = remember { DocumentHtml(context) }
    val saveHtml = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { destination ->
        val markdown = state.markdown
        if (destination != null && markdown != null) {
            scope.launch {
                val html = documentHtml.build(docId, state.title, markdown)
                val saved = documentHtml.save(destination, html)
                snackbarHost.showSnackbar(if (saved) "HTML saved" else "Couldn't save the file")
            }
        }
    }
    DisposableEffect(printer) { onDispose { printer.release() } }

    var codeShown by remember { mutableStateOf<String?>(null) }
    var tableShown by remember { mutableStateOf<Int?>(null) }
    var imageShown by remember { mutableStateOf<String?>(null) }
    var diagramShown by remember { mutableStateOf<Int?>(null) }
    val onLinkClick: (String) -> Unit = { link ->
        val diagram = MermaidBlocks.diagramIndex(link)
        val table = MarkdownTables.tableIndex(link)
        val footnote = Footnotes.referenceNumber(link)
        val footnoteBack = Footnotes.backNumber(link)
        if (diagram != null) {
            diagramShown = diagram
        } else if (table != null) {
            tableShown = table
        } else if (footnote != null || footnoteBack != null) {
            // A marker jumps to the "back" link in its definition, and that link jumps
            // back to the first marker.
            val target = if (footnote != null) {
                "${Footnotes.BACK_PREFIX}$footnote"
            } else {
                "${Footnotes.REFERENCE_PREFIX}$footnoteBack"
            }
            controller.offsetForLink(target)?.let { offset ->
                scope.launch { renderedScroll.animateScrollTo((offset - 20f).coerceAtLeast(0f).roundToInt()) }
            }
        } else scope.launch {
            when (val result = viewModel.resolveLink(link)) {
                is LinkResult.Open -> onOpenLinked(result.docId)
                is LinkResult.Failed -> snackbarHost.showSnackbar(result.text)
                is LinkResult.External -> openExternalLink(context, result.link)
            }
        }
    }

    codeShown?.let { code ->
        CodeDialog(
            code = code,
            onCopy = {
                copyToClipboard(context, code)
                scope.launch { snackbarHost.showSnackbar("Code copied") }
            },
            onDismiss = { codeShown = null },
        )
    }

    tableShown?.let { index ->
        state.tables.getOrNull(index)?.let { table ->
            TableDialog(table = table, onDismiss = { tableShown = null })
        }
    }

    imageShown?.let { source ->
        ImageViewerDialog(source = source, onDismiss = { imageShown = null })
    }

    diagramShown?.let { index ->
        state.diagrams.getOrNull(index)?.let { source ->
            MermaidDialog(source = source, onDismiss = { diagramShown = null })
        }
    }

    // Registered first so the drawer and search handlers below take priority; lets the
    // system back gesture go through onBack so forward history is recorded.
    BackHandler(onBack = onBack)

    // Leaving the editor with unsaved changes asks first.
    var confirmLeaveEditor by remember { mutableStateOf(false) }
    val unsavedChanges = EditPolicy.isDirty(state.editing, state.draft, state.markdown)
    fun leaveEditor() {
        when (EditPolicy.leaveAction(unsavedChanges, state.autosave)) {
            LeaveAction.LEAVE -> viewModel.discardEditing()
            LeaveAction.SAVE_AND_LEAVE -> viewModel.save(thenLeave = true)
            LeaveAction.ASK -> confirmLeaveEditor = true
        }
    }

    // While the screen is visible, notice when the file is changed by something else.
    val reloadOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(reloadOwner) {
        reloadOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                viewModel.checkForChanges()
                delay(RELOAD_CHECK_INTERVAL_MS)
            }
        }
    }

    // Keep the reader at the same relative position after a reload, and say what happened.
    var fractionBeforeReload by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(state.reloadCount) {
        if (state.reloadCount == 0) return@LaunchedEffect
        val max = renderedScroll.maxValue
        fractionBeforeReload = if (max > 0) renderedScroll.value.toFloat() / max else 0f
        snackbarHost.showSnackbar("Document updated")
    }
    LaunchedEffect(renderedGeneration) {
        val fraction = fractionBeforeReload ?: return@LaunchedEffect
        fractionBeforeReload = null
        val max = withTimeoutOrNull(1_000) { snapshotFlow { renderedScroll.maxValue }.first { it > 0 } }
        if (max != null) renderedScroll.scrollTo((max * fraction).roundToInt())
    }

    // Autosave also writes when the app is sent to the background mid-edit.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.saveIfAutosaving()
                speech.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = state.editing) { leaveEditor() }
    LaunchedEffect(state.saveError) {
        state.saveError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }
    if (confirmLeaveEditor) {
        AlertDialog(
            onDismissRequest = { confirmLeaveEditor = false },
            title = { Text("Save changes?") },
            text = { Text("Your edits to ${state.title} haven't been saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLeaveEditor = false
                        viewModel.save(thenLeave = true)
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmLeaveEditor = false }) { Text("Keep editing") }
                    TextButton(
                        onClick = {
                            confirmLeaveEditor = false
                            viewModel.discardEditing()
                        },
                    ) { Text("Discard") }
                }
            },
        )
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = searchActive) {
        searchActive = false
        query = ""
    }

    // Wide screens show the contents beside the reader instead of in a sliding drawer.
    val wide = WindowLayout.isWide(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp)
    val showTocPane = wide && !state.rawMode && !state.editing

    val tocPanel: @Composable (Boolean) -> Unit = { persistent ->
            TocDrawer(
                persistent = persistent,
                headings = state.headings,
                bookmarks = state.bookmarks,
                onCopyAnchor = { index ->
                    anchors.getOrNull(index)?.let { anchor ->
                        copyToClipboard(context, anchor)
                        scope.launch { snackbarHost.showSnackbar("Copied $anchor") }
                    }
                },
                onAddBookmark = {
                    val max = renderedScroll.maxValue
                    val y = renderedScroll.value
                    val permille = if (max > 0) y * ReadingPositions.FULL / max else 0
                    val offsets = state.headings.mapIndexedNotNull { i, heading ->
                        controller.offsetForHeading(i)?.let { heading.text to it }
                    }
                    val label = Bookmarks.labelFor(offsets, y.toFloat())
                        ?: "Position ${permille / 10}%"
                    viewModel.addBookmark(permille, label)
                    scope.launch { snackbarHost.showSnackbar("Bookmark added") }
                },
                onRemoveBookmark = viewModel::removeBookmark,
                onSelectBookmark = { bookmark ->
                    scope.launch {
                        drawerState.close()
                        val max = renderedScroll.maxValue
                        renderedScroll.animateScrollTo(max * bookmark.permille / ReadingPositions.FULL)
                    }
                },
                onSelect = { index ->
                    scope.launch {
                        drawerState.close()
                        val offset = controller.offsetForHeading(index)
                        if (offset != null) {
                            val target = (offset - 20f).coerceAtLeast(0f)
                            renderedScroll.animateScrollTo(target.roundToInt())
                        }
                    }
                },
            )
    }

    val readerScaffold: @Composable () -> Unit = {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                if (speech.state != SpeechState.IDLE) {
                    SpeechBar(
                        state = speech.state,
                        position = speech.position,
                        total = speech.total,
                        onPlayPause = {
                            if (speech.state == SpeechState.PLAYING) speech.pause() else speech.resume()
                        },
                        onStop = speech::stop,
                    )
                }
            },
            topBar = {
                if (state.editing) {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (unsavedChanges) "Editing ${state.title} •" else "Editing ${state.title}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { leaveEditor() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close editor")
                            }
                        },
                        actions = {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                IconButton(onClick = { viewModel.save() }, enabled = unsavedChanges) {
                                    Icon(Icons.Default.Save, contentDescription = "Save changes")
                                }
                            }
                            Box {
                                IconButton(onClick = { editorMenuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Editor options")
                                }
                                DropdownMenu(
                                    expanded = editorMenuOpen,
                                    onDismissRequest = { editorMenuOpen = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Autosave") },
                                        onClick = { viewModel.setAutosave(!state.autosave) },
                                        trailingIcon = { Switch(checked = state.autosave, onCheckedChange = null) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Discard changes") },
                                        enabled = unsavedChanges,
                                        onClick = {
                                            editorMenuOpen = false
                                            viewModel.discardEditing()
                                        },
                                    )
                                }
                            }
                        },
                    )
                } else if (searchActive) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { searchActive = false; query = "" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                            }
                        },
                        title = {
                            TextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                    currentMatch = 0
                                },
                                placeholder = { Text("Search document…") },
                                singleLine = true,
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = { query = ""; currentMatch = 0 }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                            )
                        },
                        actions = {
                            if (query.isNotEmpty()) {
                                Text(
                                    text = when {
                                        matches.isEmpty() -> "0/0"
                                        else -> "${currentMatch.coerceIn(0, matches.size - 1) + 1}/${matches.size}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                IconButton(
                                    onClick = {
                                        if (matches.isNotEmpty()) {
                                            currentMatch = (currentMatch - 1 + matches.size) % matches.size
                                        }
                                    },
                                    enabled = matches.isNotEmpty(),
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous match")
                                }
                                IconButton(
                                    onClick = {
                                        if (matches.isNotEmpty()) {
                                            currentMatch = (currentMatch + 1) % matches.size
                                        }
                                    },
                                    enabled = matches.isNotEmpty(),
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match")
                                }
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                timeLeftLabel?.let { label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (!state.rawMode && !showTocPane) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = "Table of contents",
                                    )
                                }
                            }
                            if (canGoForward) {
                                IconButton(onClick = onForward) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                                }
                            }
                            IconButton(onClick = viewModel::toggleFavorite) {
                                Icon(
                                    imageVector = if (state.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (state.isFavorite) "Remove from favorites" else "Add to favorites",
                                )
                            }
                            Box {
                                IconButton(onClick = { textSizeMenuOpen = true }) {
                                    Icon(Icons.Default.FormatSize, contentDescription = "Text settings")
                                }
                                DropdownMenu(
                                    expanded = textSizeMenuOpen,
                                    onDismissRequest = { textSizeMenuOpen = false },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.adjustTextScale(-1) },
                                            enabled = state.textScale > TextScale.MIN,
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Smaller text")
                                        }
                                        Text(
                                            text = "${state.textScale}%",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.width(48.dp),
                                            textAlign = TextAlign.Center,
                                        )
                                        IconButton(
                                            onClick = { viewModel.adjustTextScale(1) },
                                            enabled = state.textScale < TextScale.MAX,
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Larger text")
                                        }
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Reset") },
                                        onClick = { viewModel.resetTextScale() },
                                        enabled = state.textScale != TextScale.DEFAULT,
                                    )
                                    ChoiceItems(
                                        title = "Font",
                                        options = ReaderFont.entries,
                                        selected = state.font,
                                        label = { it.label },
                                        onSelect = viewModel::setFont,
                                    )
                                    ChoiceItems(
                                        title = "Line spacing",
                                        options = LineSpacing.entries,
                                        selected = state.lineSpacing,
                                        label = { it.label },
                                        onSelect = viewModel::setLineSpacing,
                                    )
                                    ChoiceItems(
                                        title = "Code blocks",
                                        options = CodeWrap.entries,
                                        selected = state.codeWrap,
                                        label = { it.label },
                                        onSelect = viewModel::setCodeWrap,
                                    )
                                    ChoiceItems(
                                        title = "Margins",
                                        options = PageMargin.entries,
                                        selected = state.pageMargin,
                                        label = { it.label },
                                        onSelect = viewModel::setPageMargin,
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Keep screen on") },
                                        onClick = { viewModel.setKeepScreenOn(!state.keepScreenOn) },
                                        trailingIcon = {
                                            Switch(
                                                checked = state.keepScreenOn,
                                                onCheckedChange = null,
                                            )
                                        },
                                    )
                                }
                            }
                            IconButton(onClick = {
                                searchActive = true
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search in document")
                            }
                            Box {
                                IconButton(onClick = { moreMenuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(
                                    expanded = moreMenuOpen,
                                    onDismissRequest = { moreMenuOpen = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Read aloud") },
                                        enabled = state.markdown != null && !state.rawMode,
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null) },
                                        onClick = {
                                            moreMenuOpen = false
                                            speech.start(controller.textView?.text?.toString().orEmpty())
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        enabled = viewModel.canEdit && state.markdown != null,
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            moreMenuOpen = false
                                            viewModel.startEditing()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share text") },
                                        enabled = state.markdown != null,
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                        onClick = {
                                            moreMenuOpen = false
                                            val markdown = state.markdown ?: return@DropdownMenuItem
                                            if (ShareText.fits(markdown)) {
                                                shareText(context, ShareText.subject(state.title), markdown)
                                            } else {
                                                scope.launch {
                                                    snackbarHost.showSnackbar("This document is too large to share as text")
                                                }
                                            }
                                        },
                                    )
                                    // Printing and saving as PDF share the system print dialog, which
                                    // lists real printers as well as "Save as PDF".
                                    DropdownMenuItem(
                                        text = { Text("Print") },
                                        enabled = state.markdown != null,
                                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                        onClick = {
                                            moreMenuOpen = false
                                            val markdown = state.markdown ?: return@DropdownMenuItem
                                            scope.launch {
                                                printer.print(state.title, documentHtml.build(docId, state.title, markdown))
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save as PDF") },
                                        enabled = state.markdown != null,
                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                        onClick = {
                                            moreMenuOpen = false
                                            val markdown = state.markdown ?: return@DropdownMenuItem
                                            scope.launch {
                                                printer.print(
                                                    ExportNames.fileName(state.title, "pdf").removeSuffix(".pdf"),
                                                    documentHtml.build(docId, state.title, markdown),
                                                )
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as HTML") },
                                        enabled = state.markdown != null,
                                        leadingIcon = { Icon(Icons.Default.Html, contentDescription = null) },
                                        onClick = {
                                            moreMenuOpen = false
                                            saveHtml.launch(ExportNames.fileName(state.title, "html"))
                                        },
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.setRawMode(!state.rawMode) }) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = if (state.rawMode) "Rendered view" else "Raw markdown",
                                )
                            }
                        },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                    state.editing -> MarkdownEditor(
                        text = state.draft,
                        onTextChange = viewModel::updateDraft,
                        textScale = state.textScale,
                        horizontalPadding = state.pageMargin.dp,
                    )

                    state.error != null -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = viewModel::retry) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                        }
                    }

                    else -> AnimatedContent(
                        targetState = state.rawMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "reader-mode",
                        modifier = Modifier.pinchToZoom(
                            onZoom = viewModel::pinchTextScale,
                            onEnd = viewModel::commitTextScale,
                        ),
                    ) { raw ->
                        if (raw) {
                            SelectionContainer {
                                Text(
                                    text = state.markdown ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall.let {
                                        it.copy(
                                            fontSize = it.fontSize * state.textScale / 100f,
                                            lineHeight = it.lineHeight * state.textScale / 100f,
                                        )
                                    },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rawScroll)
                                        .padding(horizontal = state.pageMargin.dp.dp, vertical = 16.dp),
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(renderedScroll)
                                    .padding(horizontal = state.pageMargin.dp.dp, vertical = 8.dp),
                            ) {
                                if (state.frontMatter.isNotEmpty()) {
                                    FrontMatterHeader(
                                        entries = state.frontMatter,
                                        expanded = frontMatterExpanded,
                                        onToggle = { frontMatterExpanded = !frontMatterExpanded },
                                        modifier = Modifier.onSizeChanged {
                                            // Scroll targets are measured from the top of the column.
                                            controller.topInset = it.height.toFloat()
                                        },
                                    )
                                }
                                MarkdownText(
                                    docId = docId,
                                    markdown = state.renderedMarkdown ?: state.markdown ?: "",
                                    headings = state.headings,
                                    controller = controller,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    textScale = state.textScale,
                                    font = state.font,
                                    lineSpacing = state.lineSpacing,
                                    onLinkClick = onLinkClick,
                                    onImageClick = { imageShown = it },
                                    onCopyCode = { code ->
                                        copyToClipboard(context, code)
                                        scope.launch { snackbarHost.showSnackbar("Code copied") }
                                    },
                                    codeWrap = state.codeWrap,
                                    onViewCode = { codeShown = it },
                                )
                            }
                        }
                    }
                }

                if (!state.loading && state.error == null) {
                    ScrollProgressBar(
                        scroll = if (state.rawMode) rawScroll else renderedScroll,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                }
            }
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            if (showTocPane) tocPanel(true)
            Box(Modifier.weight(1f)) { readerScaffold() }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { tocPanel(false) },
            content = readerScaffold,
        )
    }
}

/** Play/pause and stop controls shown while the document is being read aloud. */
@Composable
private fun SpeechBar(
    state: SpeechState,
    position: Int,
    total: Int,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
) {
    androidx.compose.material3.Surface(tonalElevation = 3.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (state == SpeechState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state == SpeechState.PLAYING) "Pause reading" else "Resume reading",
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Stop, contentDescription = "Stop reading")
            }
            Text(
                text = "Reading aloud · part ${position + 1} of $total",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Thin bar under the app bar showing how far through the document the user has scrolled. */
@Composable
private fun ScrollProgressBar(scroll: ScrollState, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = {
            if (scroll.maxValue > 0) scroll.value.toFloat() / scroll.maxValue else 0f
        },
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
        drawStopIndicator = {},
    )
}

/** A titled group of single-choice menu items with a check mark on [selected]. */
@Composable
private fun <T> ChoiceItems(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    HorizontalDivider()
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
    options.forEach { option ->
        DropdownMenuItem(
            text = { Text(label(option)) },
            onClick = { onSelect(option) },
            leadingIcon = {
                if (option == selected) {
                    Icon(Icons.Default.Check, contentDescription = "Selected")
                }
            },
        )
    }
}

/** Opens the system share sheet with the document's source text under its title. */
private fun shareText(context: android.content.Context, subject: String, text: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
        putExtra(android.content.Intent.EXTRA_TITLE, subject)
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(
            android.content.Intent.createChooser(send, "Share document")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: android.content.ActivityNotFoundException) {
        // No app can receive text; nothing to do.
    }
}

/** Puts [text] on the clipboard exactly as given. */
private fun copyToClipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Code", text))
}

/**
 * Web links open in a Custom Tab (the user's browser shown inside the app, falling back to
 * the plain browser when none supports tabs); mail, phone and other links go to whichever
 * app handles them.
 */
private fun openExternalLink(context: android.content.Context, link: String) {
    if (ExternalLinks.isInPage(link)) return
    val uri = android.net.Uri.parse(link.trim())
    try {
        if (ExternalLinks.isWeb(link)) {
            val tab = androidx.browser.customtabs.CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            tab.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            tab.launchUrl(context, uri)
        } else {
            context.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    } catch (_: android.content.ActivityNotFoundException) {
        // No app can open this link; nothing to do.
    }
}

private const val RELOAD_CHECK_INTERVAL_MS = 2_000L

private fun findMatches(text: String, query: String): List<IntRange> {
    if (query.isEmpty() || text.isEmpty()) return emptyList()
    val out = mutableListOf<IntRange>()
    var index = text.indexOf(query, ignoreCase = true)
    while (index >= 0 && out.size < MAX_MATCHES) {
        out += index until (index + query.length)
        index = text.indexOf(query, startIndex = index + query.length, ignoreCase = true)
    }
    return out
}

private const val MAX_MATCHES = 500
