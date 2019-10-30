package cn.com.bamboo.easy_audio_player.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.bamboo.easy_audio_player.util.IntentKey
import cn.com.bamboo.easy_audio_player.vo.Music


class MusicService : MediaBrowserServiceCompat() {

    private var player: PlayerConfig = PlayerProvider(MyPlayerCallback())
    private lateinit var musicProvider: MusicProvider
    private lateinit var mediaSessionCompat: MediaSessionCompat
    //    private var formList: List<MusicForm>? = null
    private var musicList: List<Music>? = null
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: ControlNotificationBuilder
    protected lateinit var mediaController: MediaControllerCompat

    //管理多个session,暂时没有使用
    private var currentMusic: Int = 0
    private var playerSpeed = 1.0f
    private var isForegroundService = false

    override fun onCreate() {
        super.onCreate()
        musicProvider = MusicProvider((application as MusicApp).database)
        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }
        mediaSessionCompat = MediaSessionCompat(this, IntentKey.SERVICE_NAME).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(
                getPlaybackStateCompat(
                    mapPlaybackState(player.getState()),
                    player.getCurrentPosition()
                )
            )
            setCallback(SessionCallback())
            setSessionActivity(sessionActivityPendingIntent)
        }

        sessionToken = mediaSessionCompat.sessionToken
        // Because ExoPlayer will manage the MediaSession, add the service as a callback for
        // state changes.
        mediaController = MediaControllerCompat(this, mediaSessionCompat).also {
            it.registerCallback(MediaControllerCallback())
        }
        notificationBuilder = ControlNotificationBuilder(this)
        notificationManager = NotificationManagerCompat.from(this)

        becomingNoisyReceiver =
            BecomingNoisyReceiver(context = this, sessionToken = mediaSessionCompat.sessionToken)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.run {
            isActive = false
            release()
        }
        player.stop()
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        result.detach()
        musicProvider.getMusicFormList {
            val children = it?.map { item ->
                val extras = Bundle()
                extras.putInt(IntentKey.QUEUE_TYPE, 0)
                var temp = MediaDescriptionCompat.Builder()
                    .setMediaId(item.id.toString())
                    .setTitle(item.name)
                    .setDescription(item.description)
                    .setExtras(extras)
                    .build()
                MediaBrowserCompat.MediaItem(temp, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
            }
            children?.run {
                result.sendResult(this)
            }
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (clientPackageName.equals("cn.com.bamboo.easy_audio_player")) {
            BrowserRoot(IntentKey.MEDIA_ID_ROOT, null)
        } else {
            null
        }
    }

    private inner class SessionCallback: MediaSessionCompat.Callback() {
        override fun onSkipToQueueItem(id: Long) {
            playItem(id)
        }

        override fun onSkipToNext() {
            onSkipToQueueItem(currentMusic + 1.toLong())
        }

        override fun onSkipToPrevious() {
            onSkipToQueueItem(currentMusic - 1.toLong())
        }

        override fun onPause() {
            super.onPause()
            Log.e("=====",player.getState().toString())
            when (player.getState()) {
                PlayerConfig.STATE_PLAY -> player.pause()
                PlayerConfig.STATE_PAUSE -> player.play()
                PlayerConfig.STATE_IDLE -> onSkipToQueueItem(currentMusic.toLong())
            }

            mediaSessionCompat.setPlaybackState(
                getPlaybackStateCompat(
                    mapPlaybackState(player.getState()),
                    player.getCurrentPosition()
                )
            )
        }

        override fun onSeekTo(pos: Long) {
            if (player.getState() == PlayerConfig.STATE_PLAY) {
                player.seekTo(pos)
                mediaSessionCompat.setPlaybackState(
                    getPlaybackStateCompat(
                        mapPlaybackState(player.getState()),
                        player.getCurrentPosition()
                    )
                )
            }
        }

        override fun onStop() {
            player.stop()
            mediaSessionCompat.setPlaybackState(
                getPlaybackStateCompat(
                    mapPlaybackState(player.getState()),
                    player.getCurrentPosition()
                )
            )

        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {onPause()
                        return true}
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {onSkipToNext()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        onSkipToPrevious()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_STOP ->{
                        onStop()
                        return true
                    }
                }
            }

           return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            when (action) {
                IntentKey.LOAD_FORM_LIST -> {
                    musicProvider.getMusicFormList { it ->
                        val children = it?.mapIndexed { idx, item ->
                            val extras = Bundle()
                            extras.putInt(IntentKey.QUEUE_TYPE, 0)
                            var temp = MediaDescriptionCompat.Builder()
                                .setMediaId(item.id.toString())
                                .setTitle(item.name)
                                .setDescription(item.description)
                                .setExtras(extras)
                                .build()

                            MediaSessionCompat.QueueItem(temp, idx.toLong())
                        }
                        children?.run {
                            mediaSessionCompat.setQueue(this)
                        }
                    }
                }

                IntentKey.LOAD_MUSIC_LIST -> {
                    extras?.let {
                        val formId = it.get(IntentKey.FORM_ID) ?: return@let
                        musicProvider.getMusicList((formId as String).toInt()) { list ->
                            musicList = list
                            val children = list?.mapIndexed { idx, item ->
                                val extras = Bundle()
                                extras.putInt(IntentKey.QUEUE_TYPE, 1)
                                var temp = MediaDescriptionCompat.Builder()
                                    .setMediaId(item.id.toString())
                                    .setTitle(item.name)
                                    .setMediaUri(Uri.parse(item.path))
                                    .setExtras(extras)
                                    .build()

                                MediaSessionCompat.QueueItem(temp, idx.toLong())
                            }
                            children?.run {
                                mediaSessionCompat.setQueue(this)
                            }
                        }
                    }

                }
            }
        }
    }

    private fun playItem(pos: Long) {
        musicList?.let {
            val tempId = pos.toInt()
            if (tempId < 0 || tempId > it.size) {
                return@let
            }
            currentMusic = tempId
            val item = it[pos.toInt()]
            player.setData(item.path)
            player.prepare()
            player.play()
            mediaSessionCompat.setPlaybackState(
                getPlaybackStateCompat(
                    mapPlaybackState(player.getState()),
                    player.getCurrentPosition()
                )
            )
        }
    }

    private inner class MyPlayerCallback : PlayerCallback {
        override fun onPrepared() {
            mediaSessionCompat.setMetadata(
                MediaMetadataCompat
                    .Builder()
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        musicList!![currentMusic].name
                    )
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.getDuration())
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                        musicList!![currentMusic].id.toString()
                    )
                    .build()
            )
        }

        override fun onCompletion() {
            this@MusicService.playItem(currentMusic + 1.toLong())
        }
    }

    /**
     * Removes the [NOW_PLAYING_NOTIFICATION] notification.
     *
     * Since `stopForeground(false)` was already called (see
     * [MediaControllerCallback.onPlaybackStateChanged], it's possible to cancel the notification
     * with `notificationManager.cancel(NOW_PLAYING_NOTIFICATION)` if minSdkVersion is >=
     * [Build.VERSION_CODES.LOLLIPOP].
     *
     * Prior to [Build.VERSION_CODES.LOLLIPOP], notifications associated with a foreground
     * service remained marked as "ongoing" even after calling [Service.stopForeground],
     * and cannot be cancelled normally.
     *
     * Fortunately, it's possible to simply call [Service.stopForeground] a second time, this
     * time with `true`. This won't change anything about the service's state, but will simply
     * remove the notification.
     */
    private fun removeNowPlayingNotification() {
        stopForeground(true)
    }

    private fun mapPlaybackState(playerConfigState: Int): Int {
        return when (playerConfigState) {
            PlayerConfig.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
            PlayerConfig.STATE_PLAY -> PlaybackStateCompat.STATE_PLAYING
            PlayerConfig.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            PlayerConfig.STATE_PAUSE -> PlaybackStateCompat.STATE_PAUSED
            PlayerConfig.STATE_PREPARE -> PlaybackStateCompat.STATE_NONE
            else -> PlaybackStateCompat.STATE_NONE
        }
    }

    private fun getPlaybackStateCompat(playbackState: Int, pos: Long): PlaybackStateCompat {
        return PlaybackStateCompat.Builder()
            .setState(playbackState, pos, playerSpeed)
            .build()
    }

    /**
     * Class to receive callbacks about state changes to the [MediaSessionCompat]. In response
     * to those callbacks, this class:
     *
     * - Build/update the service's notification.
     * - Register/unregister a broadcast receiver for [AudioManager.ACTION_AUDIO_BECOMING_NOISY].
     * - Calls [Service.startForeground] and [Service.stopForeground].
     */
    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mediaController.playbackState?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
        }

        private fun updateNotification(state: PlaybackStateCompat) {
            val updatedState = state.state

            // Skip building a notification when state is "none" and metadata is null.
            val notification = if (mediaController.metadata != null
                && updatedState != PlaybackStateCompat.STATE_NONE
            ) {
                notificationBuilder.buildNotification(mediaSessionCompat.sessionToken)
            } else {
                null
            }

            when (updatedState) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {
                    becomingNoisyReceiver.register()

                    /**
                     * This may look strange, but the documentation for [Service.startForeground]
                     * notes that "calling this method does *not* put the service in the started
                     * state itself, even though the name sounds like it."
                     */
                    if (notification != null) {
                        notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)

                        if (!isForegroundService) {
                            ContextCompat.startForegroundService(
                                applicationContext,
                                Intent(applicationContext, this@MusicService.javaClass)
                            )
                            startForeground(NOW_PLAYING_NOTIFICATION, notification)
                            isForegroundService = true
                        }
                    }
                }
                else -> {
                    becomingNoisyReceiver.unregister()

                    if (isForegroundService) {
                        stopForeground(false)
                        isForegroundService = false

                        // If playback has ended, also stop the service.
                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
                            stopSelf()
                        }

                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        } else {
                            removeNowPlayingNotification()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper class for listening for when headphones are unplugged (or the audio
 * will otherwise cause playback to become "noisy").
 */
private class BecomingNoisyReceiver(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token
) : BroadcastReceiver() {

    private val noisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val controller = MediaControllerCompat(context, sessionToken)

    private var registered = false

    fun register() {
        if (!registered) {
            context.registerReceiver(this, noisyIntentFilter)
            registered = true
        }
    }

    fun unregister() {
        if (registered) {
            context.unregisterReceiver(this)
            registered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            controller.transportControls.pause()
        }
    }

}