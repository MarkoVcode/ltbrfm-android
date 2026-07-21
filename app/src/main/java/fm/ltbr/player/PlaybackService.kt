package fm.ltbr.player

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * The station's media engine. A [MediaLibraryService] is the single component
 * that surfaces playback to Android Auto, the lock screen / notification,
 * Bluetooth controls and Wear OS. ExoPlayer handles the Icecast MP3 stream —
 * including ICY metadata, which it exposes as the item's MediaMetadata title,
 * so the live "now playing" text appears on the car screen automatically.
 */
class PlaybackService : MediaLibraryService() {

    companion object {
        const val STREAM_URL = "https://stream.ltbr.fm/live"
        const val ROOT_ID = "root"
        const val LIVE_ID = "live"
    }

    private var session: MediaLibrarySession? = null

    private fun rootItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("LTBR·FM")
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun liveItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(LIVE_ID)
            .setUri(STREAM_URL)
            .setMimeType("audio/mpeg")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("LTBR·FM — Live")
                    .setArtist("London Tower Block Radio")
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setArtworkUri(
                        Uri.parse("android.resource://fm.ltbr.player/drawable/station_art"),
                    )
                    .build(),
            )
            .build()

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        session = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(rootItem(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            if (parentId == ROOT_ID) {
                Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.of(liveItem()), params),
                )
            } else {
                Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.of(), params),
                )
            }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            if (mediaId == LIVE_ID) {
                Futures.immediateFuture(LibraryResult.ofItem(liveItem(), null))
            } else {
                Futures.immediateFuture(
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE),
                )
            }

        /**
         * Controllers (Auto, our own activity) hand over items that carry only
         * a mediaId; resolve them to the playable stream item.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(
                mediaItems.map { item ->
                    if (item.localConfiguration == null) liveItem() else item
                }.toMutableList(),
            )

        /** Lets Auto's "resume" affordance restart the live stream. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    ImmutableList.of(liveItem()),
                    /* startIndex = */ 0,
                    /* startPositionMs = */ 0L,
                ),
            )
    }
}
