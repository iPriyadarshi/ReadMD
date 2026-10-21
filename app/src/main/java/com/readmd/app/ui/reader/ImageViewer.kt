package com.readmd.app.ui.reader

import com.readmd.app.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.readmd.app.domain.ImageNames
import com.readmd.app.domain.ImageZoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** Full-screen image with pinch to zoom, drag to pan and double-tap to toggle zoom. Back closes it. */
@Composable
fun ImageViewerDialog(source: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(source) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(source) { mutableStateOf(false) }
    LaunchedEffect(source) {
        val loaded = loadBitmap(context, source)
        bitmap = loaded
        failed = loaded == null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val image = bitmap
            when {
                image != null -> ZoomableImage(image, ImageNames.displayName(source))
                failed -> Text(
                    text = stringResource(R.string.image_open_failed, ImageNames.displayName(source)),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_image), tint = Color.White)
            }
        }
    }
}

@Composable
private fun ZoomableImage(bitmap: Bitmap, description: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewport = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = ImageZoom.doubleTapTarget(scale)
                        offset = Offset(
                            ImageZoom.clampOffset(offset.x, viewport.width.toFloat(), scale),
                            ImageZoom.clampOffset(offset.y, viewport.height.toFloat(), scale),
                        )
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = ImageZoom.scaleBy(scale, zoom)
                    offset = Offset(
                        ImageZoom.clampOffset(offset.x + pan.x, viewport.width.toFloat(), scale),
                        ImageZoom.clampOffset(offset.y + pan.y, viewport.height.toFloat(), scale),
                    )
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

/** Decodes the image at [source], sub-sampled so very large images don't exhaust memory. */
private suspend fun loadBitmap(context: Context, source: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        fun open() = when (Uri.parse(source).scheme) {
            "http", "https" -> URL(source).openConnection().apply {
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
            }.getInputStream()
            else -> context.contentResolver.openInputStream(Uri.parse(source))
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageZoom.sampleSize(bounds.outWidth, bounds.outHeight)
        }
        open()?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}

private const val NETWORK_TIMEOUT_MS = 10_000
