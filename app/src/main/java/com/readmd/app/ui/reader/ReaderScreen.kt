package com.readmd.app.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.readmd.app.ReadMdApp
import com.readmd.app.domain.HeadingNode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    docId: String,
    onBack: () -> Unit,
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

    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var currentMatch by remember { mutableIntStateOf(0) }
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

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = searchActive) {
        searchActive = false
        query = ""
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TocDrawer(
                headings = state.headings,
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
        },
    ) {
        Scaffold(
            topBar = {
                if (searchActive) {
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
                            Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (!state.rawMode) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = "Table of contents",
                                    )
                                }
                            }
                            IconButton(onClick = {
                                searchActive = true
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search in document")
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
                    ) { raw ->
                        if (raw) {
                            SelectionContainer {
                                Text(
                                    text = state.markdown ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rawScroll)
                                        .padding(16.dp),
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(renderedScroll)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                MarkdownText(
                                    markdown = state.markdown ?: "",
                                    headings = state.headings,
                                    controller = controller,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
