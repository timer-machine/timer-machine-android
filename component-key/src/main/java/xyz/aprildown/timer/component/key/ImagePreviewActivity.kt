package xyz.aprildown.timer.component.key

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color.Companion as ComposeColor

class ImagePreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(
                AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)
            ) // DefaultDarkScrim
        )
        super.onCreate(savedInstanceState)
        val path = intent?.getStringExtra(EXTRA_PATH)
        if (path.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            var data: String? by rememberSaveable { mutableStateOf(path) }

            LaunchedEffect(Unit) {
                addOnNewIntentListener { intent ->
                    intent.getStringExtra(EXTRA_PATH)?.takeIf { it.isNotBlank() }?.let {
                        data = it
                    }
                }
            }

            ImagePreview(
                data = data,
                onDismiss = ::finish,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    companion object {
        private const val EXTRA_PATH = "path"
        fun getIntent(context: Context, path: String): Intent {
            return Intent(context, ImagePreviewActivity::class.java)
                .putExtra(EXTRA_PATH, path)
        }
    }
}

@Composable
private fun ImagePreview(
    data: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoomableState = rememberZoomableState(
        zoomSpec = ZoomSpec(maxZoomFactor = 5f),
    )
    val density = LocalDensity.current
    val padding = 16.dp
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(data)
            .crossfade(true)
            .listener { _, result ->
                val image = result.image
                val paddingPx = density.run { padding.toPx() } * 2f
                zoomableState.setContentLocation(
                    ZoomableContentLocation.scaledInsideAndCenterAligned(
                        Size(
                            width = image.width.toFloat() + paddingPx,
                            height = image.height.toFloat() + paddingPx
                        )
                    )
                )
            }
            .build(),
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.5f))
            .zoomable(state = zoomableState, onClick = { onDismiss() })
            .padding(padding),
    )
}
