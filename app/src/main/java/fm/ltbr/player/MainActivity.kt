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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

// Faceplate palette, matching the desktop receiver.
private val Amber = Color(0xFFFF9B21)
private val Red = Color(0xFFFF3F1C)
private val Concrete = Color(0xFF8D857A)
private val ConcreteDim = Color(0xFF5A544C)
private val Ink = Color(0xFF0A0908)
private val Bone = Color(0xFFF4EFE6)
private val KeyText = Color(0xFFC9C0B3)

private val PanelBrush = Brush.verticalGradient(
    listOf(Color(0xFF3A332C), Color(0xFF2A2521), Color(0xFF2A2521), Color(0xFF1D1916)),
)
private val KeyBrush = Brush.verticalGradient(
    listOf(Color(0xFF4A423A), Color(0xFF332D27), Color(0xFF241F1A)),
)

class MainActivity : ComponentActivity() {

    private var controller: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { Faceplate() }
    }

    private fun play() {
        val c = controller ?: return
        c.setMediaItem(MediaItem.Builder().setMediaId(PlaybackService.LIVE_ID).build())
        c.prepare()
        c.play()
    }

    private fun stop() {
        controller?.stop()
    }

    private fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) stop() else play()
    }

    /** "Artist - Title" for the scroller; station-name artist adds nothing. */
    private fun describe(m: MediaMetadata): String {
        val title = m.title?.toString().orEmpty()
        val artist = m.artist?.toString().orEmpty()
        return when {
            title.isEmpty() -> ""
            artist.isEmpty() || artist == "London Tower Block Radio" -> title
            else -> "$artist - $title"
        }
    }

    @Composable
    private fun Faceplate() {
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
                    nowPlaying = describe(mediaMetadata)
                }
            }
            future.addListener({
                val c = future.get()
                controller = c
                c.addListener(listener)
                playing = c.isPlaying
                nowPlaying = describe(c.mediaMetadata)
            }, MoreExecutors.directExecutor())

            onDispose {
                controller?.removeListener(listener)
                controller?.release()
                controller = null
            }
        }

        val scrollerText = when {
            buffering -> "LTBR FM · TUNING · STAND BY ·"
            playing -> "LTBR FM · ON AIR · ${nowPlaying.ifEmpty { "LIVE" }} ·"
            else -> "LTBR FM · LONDON TOWER BLOCK RADIO · PRESS PLAY ·"
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBrush)
                .padding(horizontal = 22.dp, vertical = 28.dp),
        ) {
            BrandRow(playing = playing)

            Spacer(Modifier.height(6.dp))
            Divider()
            Spacer(Modifier.height(22.dp))

            // Inset display window with the dot-matrix scroller.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Ink)
                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp),
            ) {
                DotMatrixDisplay(
                    text = scrollerText,
                    scrolling = playing || buffering,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TransportKey(glyph = if (playing) "❚❚" else "▶", active = playing) { toggle() }
                TransportKey(glyph = "■", active = false) { stop() }
            }

            Spacer(Modifier.weight(1f))

            PresetRow()
            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(14.dp))
            Text(
                "RECEIVING · STREAM.LTBR.FM/LIVE",
                color = ConcreteDim,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }

    @Composable
    private fun BrandRow(playing: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("LTBR", color = Bone, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(".", color = Amber, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(
                "FM",
                color = Bone.copy(alpha = 0.55f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.weight(1f))

            // TX LED: always red — solid on standby, gentle pulse on air.
            val pulse = rememberInfiniteTransition(label = "led")
            val ledAlpha by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 0.35f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                label = "ledAlpha",
            )
            Box(
                Modifier
                    .size(9.dp)
                    .alpha(if (playing) ledAlpha else 1f)
                    .background(Red, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text("STANDBY", color = Concrete, fontSize = 10.sp, letterSpacing = 3.sp)
        }
    }

    @Composable
    private fun TransportKey(glyph: String, active: Boolean, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 46.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(KeyBrush)
                .border(1.dp, Ink, RoundedCornerShape(4.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, color = if (active) Amber else KeyText, fontSize = 17.sp)
        }
    }

    @Composable
    private fun PresetRow() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresetChip("1", "LTBR", active = true)
            for (n in 2..6) PresetChip(n.toString(), "----", active = false)
        }
    }

    @Composable
    private fun RowScope.PresetChip(number: String, label: String, active: Boolean) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF221E1A))
                .border(1.dp, Color(0xFF100E0C), RoundedCornerShape(3.dp))
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(number, color = if (active) Amber else ConcreteDim, fontSize = 11.sp)
            Text(
                label,
                color = if (active) KeyText else ConcreteDim,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }

    @Composable
    private fun Divider() {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x73000000)),
        )
    }
}
