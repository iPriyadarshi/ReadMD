package com.readmd.app.ui

import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.readmd.app.ReadMdApp
import com.readmd.app.ui.home.HomeScreen
import com.readmd.app.ui.home.HomeViewModel
import com.readmd.app.ui.reader.ReaderScreen
import com.readmd.app.ui.theme.AccentColor
import com.readmd.app.ui.theme.ReadMdTheme
import com.readmd.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.map

/**
 * Safely encodes a document ID into a URL-safe Base64 token.
 * This guarantees that content:// URIs with embedded slashes (/), colons (:),
 * and percent-encoded characters (%) travel through Jetpack Navigation without
 * being altered, split into extra path segments, or corrupted by double URL-decoding.
 */
private fun encodeDocId(id: String): String =
    Base64.encodeToString(id.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

private fun decodeDocId(encoded: String): String =
    String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)

@Composable
fun ReadMdRoot(
    externalDocId: MutableState<String?>,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as ReadMdApp).container

    val themeMode by container.recentFiles.themeMode
        .map { ThemeMode.fromOrdinal(it) }
        .collectAsState(initial = ThemeMode.SYSTEM)

    val accent by container.recentFiles.accent
        .collectAsState(initial = AccentColor.DEFAULT)

    ReadMdTheme(themeMode = themeMode, accent = accent) {
        val navController = rememberNavController()

        // Consume documents opened from outside the app (file manager etc.)
        val external = externalDocId.value
        LaunchedEffect(external) {
            if (external != null) {
                navController.navigate("reader/${encodeDocId(external)}")
                externalDocId.value = null
            }
        }

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory())
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenDoc = { id ->
                        navController.navigate("reader/${encodeDocId(id)}")
                    },
                )
            }
            composable(route = "reader/{docId}") { entry ->
                val rawArg = entry.arguments?.getString("docId").orEmpty()
                val docId = runCatching { decodeDocId(rawArg) }.getOrDefault(rawArg)
                ReaderScreen(
                    docId = docId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
