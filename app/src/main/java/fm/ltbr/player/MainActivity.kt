package fm.ltbr.player

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

private val Amber = Color(0xFFFF9B21)
private val Red = Color(0xFFFF3F1C)
private val Concrete = Color(0xFF8D857A)
private val Ink = Color(0xFF0D0B09)
private val Face = Color(0xFF2A2521)
private val Bone = Color(0xFFF4EFE6)

class MainActivity : ComponentActivity() {

    private var controller: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { Screen() }
    }

    private fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.stop()
        } else {
            c.setMediaItem(MediaItem.Builder().setMediaId(PlaybackService.LIVE_ID).build())
            c.prepare()
            c.play()
        }
    }

    @Composable
    private fun Screen() {
        var playing by remember { mutableStateOf(false) }
        var buffering by remember { mutableStateOf(false) }
        var nowPlaying by remember { mutableStateOf("") }

        DisposableEffect(Unit) {
            val token = SessionToken(
                this@MainActivity,
                ComponentName(this@MainActivity, PlaybackService::class.java),
            )
            val future = MediaController.Builder(this@MainActivity, token).buildAsync()
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    buffering = playbackState == Player.STATE_BUFFERING
                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    nowPlaying = mediaMetadata.title?.toString() ?: ""
                }
            }
            future.addListener({
                val c = future.get()
                controller = c
                c.addListener(listener)
                playing = c.isPlaying
                nowPlaying = c.mediaMetadata.title?.toString() ?: ""
            }, MoreExecutors.directExecutor())

            onDispose {
                controller?.removeListener(listener)
                controller?.release()
                controller = null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Wordmark, matching the ltbr.fm logotype.
            Row(verticalAlignment = Alignment.Bottom) {
                Text("LTBR", color = Bone, fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text(".", color = Amber, fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text(
                    "FM",
                    color = Bone.copy(alpha = 0.55f),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                "LONDON TOWER BLOCK RADIO",
                color = Concrete,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
            )

            Spacer(Modifier.height(56.dp))

            // TX LED: always red — solid on standby, gentle pulse on air.
            val pulse = rememberInfiniteTransition(label = "led")
            val ledAlpha by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 0.35f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                label = "ledAlpha",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .alpha(if (playing) ledAlpha else 1f)
                        .background(Red, CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "STANDBY",
                    color = Concrete,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                )
            }

            Spacer(Modifier.height(28.dp))

            Surface(
                onClick = { toggle() },
                shape = CircleShape,
                color = Face,
                modifier = Modifier.size(108.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (playing) "■" else "▶",
                        color = Amber,
                        fontSize = 34.sp,
                    )
                }
            }

            Spacer(Modifier.height(44.dp))

            Text(
                when {
                    buffering -> "TUNING…"
                    playing -> nowPlaying.ifEmpty { "ON AIR" }.uppercase()
                    else -> "— PRESS PLAY —"
                },
                color = if (playing || buffering) Amber else Concrete,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
