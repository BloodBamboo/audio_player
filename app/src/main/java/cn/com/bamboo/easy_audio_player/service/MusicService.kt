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
import cn.com.bamboo.easy_audio_player.util.TimingUtil
import cn.com.bamboo.easy_audio_player.vo.Music


class MusicService : MediaBrowserServiceCompat() {

    private var player: PlayerConfig = PlayerProvider(MyPlayerCallback(), true)
    private var audioHelp: AudioHelp = AudioHelp(AudioHelpCallback())
    private lateinit var musicProvider: MusicProvider
    private lateinit var mediaSessionCompat: MediaSessionCompat
    //    private var formList: List<MusicForm>? = null
    private var musicList: List<Music>? = null
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: ControlNotificationBuilder
    private lateinit var lockScreenReceiver: LockScreenReceiver

    protected lateinit var mediaController: MediaControllerCompat
    protected lateinit var am: AudioManager
    private val timingUtil: TimingUtil = TimingUtil()


    //管理多个session,暂时没有使用
    private var currentMusic: Int = 0
    private var playerSpeed = 1.0f
    private var isForegroundService = false

    override fun onCreate() {
        super.onCreate()
        am = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
        lockScreenReceiver = LockScreenReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(lockScreenReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        timingUtil.onDestroy()
        Log.e("===", "service_onDestroy")
        saveCurrentPlayer()
        lockScreenReceiver?.let {
            unregisterReceiver(it)
        }

        mediaSessionCompat.run {
            isActive = false
            release()
        }
        player.release()
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

        override fun onPlayFromMediaId(musicId: String?, extras: Bundle?) {
            val progress = extras?.getLong(IntentKey.PLAYER_RECORD_PROGRESS_LONG)
            extras?.getBoolean(IntentKey.LOAD_PLAY_RECORD)?.let {
                player.isOncePlay = it
            }
            musicProvider.loadMusic(musicId!!) {
                val index = musicList?.indexOf(it)
                index?.run {
                    currentMusic = this
                    if (player.setData(it.path)) {
                        if (progress != null) {
                            playMusicPrepare(progress)
                            player.seekTo(progress)
                        } else {
                            playMusicPrepare()
                        }
                    }
                }
            }
        }

        override fun onSkipToNext() {
            saveCurrentPlayer()
            onSkipToQueueItem(currentMusic + 1.toLong())
        }

        override fun onSkipToPrevious() {
            saveCurrentPlayer()
            onSkipToQueueItem(currentMusic - 1.toLong())
        }

        override fun onPause() {
            playerOrPause()
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
            player.pause()
            saveCurrentPlayer()
            mediaSessionCompat.setPlaybackState(
                    getPlaybackStateCompat(
                        mapPlaybackState(player.getState()),
                        player.getCurrentPosition()
                    )
            )
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            Log.e("===", "onMediaButtonEvent = ${mediaButtonEvent?.action}")
            val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {onPause()
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
            Log.e("===", "${action}")
            when (action) {
                IntentKey.PLAY_TIMING_PAUSE -> {
                    if (player.getState() != PlayerConfig.STATE_PAUSE) {
                        pauseMusicAndSaveInfo()
                    }
                }

                IntentKey.PLAY_TIMING_LONG -> {
                    extras?.let {
                        val timing = it.getLong(IntentKey.PLAY_TIMING_LONG)
                        timingUtil.startTiming(timing, { time ->
                            val extras = Bundle()
                            extras.putLong(IntentKey.PLAY_TIMING_NEXT_LONG, time)
                            extras.putLong(IntentKey.PLAY_TIMING_LONG, timing)
                            mediaSessionCompat.sendSessionEvent(
                                IntentKey.PLAY_TIMING_NEXT_LONG,
                                extras
                            )
                        }, { throwable ->
                            val extras = Bundle()
                            extras.putString(IntentKey.PLAY_TIMING_ERROR_STRING, throwable.message)
                            mediaSessionCompat.sendSessionEvent(
                                IntentKey.PLAY_TIMING_NEXT_LONG,
                                extras
                            )
                        }, {
                            mediaSessionCompat.sendSessionEvent(
                                IntentKey.PLAY_TIMING_COMPLETE,
                                null
                            )
                        })
                    }
                }
                IntentKey.MUSIC_INFO -> {
                    mediaSessionCompat.setPlaybackState(
                        getPlaybackStateCompat(
                            mapPlaybackState(player.getState()),
                            player.getCurrentPosition()
                        )
                    )
                    setMetadata()
                }
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
                        val formId = it.getString(IntentKey.FORM_ID) ?: return@let
                        musicProvider.getMusicList(formId.toInt()) { list ->
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

                IntentKey.LOAD_PLAYER_RECORD -> {
                    musicProvider.getPlayerRecordList {
                        val children = it?.mapIndexed { idx,item ->
                            val extras = Bundle()
                            extras.putInt(IntentKey.QUEUE_TYPE, 2)
                            extras.putInt(IntentKey.PLAYER_RECORD_FORMID_INT, item.formId)
                            extras.putInt(IntentKey.PLAYER_RECORD_MUSICID_INT, item.musicId)
                            extras.putString(IntentKey.PLAYER_RECORD_DESCRIPTION_STRING, item.description)
                            extras.putLong(IntentKey.PLAYER_RECORD_PROGRESS_LONG, item.progress)
                            extras.putLong(IntentKey.PLAYER_RECORD_RECORDTIME_LONG, item.recordTime)
                            var temp = MediaDescriptionCompat.Builder()
                                .setMediaId(item.id.toString())
                                .setTitle(item.musicName)
                                .setSubtitle(item.formName)
                                .setExtras(extras)
                                .build()

                            MediaSessionCompat.QueueItem(temp, idx.toLong())
                        }
                        children?.run {
                            mediaSessionCompat.setQueue(this)
                        }
                    }
                }
                IntentKey.STOP_SEVER -> {
                    pauseMusicAndSaveInfo()
                    stopSelf()
                }
            }
        }
    }

    /**
     * 播放或者暂停
     */
    private fun playerOrPause() {
        when (player.getState()) {
            PlayerConfig.STATE_PLAY -> pauseMusicAndSaveInfo()
            PlayerConfig.STATE_PREPARE -> playMusic()
            PlayerConfig.STATE_PAUSE -> playMusic()
            PlayerConfig.STATE_IDLE -> {
                if (player.isPlaying()) {
                    pauseMusicAndSaveInfo()
                } else {
                    playItem(currentMusic.toLong())
                }
            }
        }
    }

    private fun playMusicPrepare(pos: Long = 0) {
        player.prepare()
        mediaSessionCompat.setPlaybackState(
            getPlaybackStateCompat(
                mapPlaybackState(player.getState()),
                pos
            )
        )
    }

    private fun playMusic() {
        Log.e("===", "playMusic")
        player.play()
        mediaSessionCompat.setPlaybackState(
            getPlaybackStateCompat(
                mapPlaybackState(player.getState()),
                player.getCurrentPosition()
            )
        )
        tryToGetAudioFocus()
    }

    private fun pauseMusicAndSaveInfo() {
        player.pause()
        giveUpAudioFocus()
        saveCurrentPlayer()
        mediaSessionCompat.setPlaybackState(
            getPlaybackStateCompat(
                mapPlaybackState(player.getState()),
                player.getCurrentPosition()
            )
        )
    }

    /**
     * 保存当前播放的音乐记录
     */
    private fun saveCurrentPlayer() {
        musicList?.let {
            val item = it[currentMusic]
            musicProvider.savePlayRecord(item.formId, item.id, player.getCurrentPosition())
        }
    }

    private fun playItem(pos: Long) {
        musicList?.let {
            val tempId = pos.toInt()
            if (tempId < 0 || tempId >= it.size) {
                if (player.getState() == PlayerConfig.STATE_PLAY) {
                    playerOrPause()
                }
                return@let
            }
            currentMusic = tempId
            val item = it[pos.toInt()]
            if (player.setData(item.path)) {
                playMusicPrepare()
            }
        }
    }

    /**
     * 尝试获取音频焦点
     * requestAudioFocus(OnAudioFocusChangeListener l, int streamType, int durationHint)
     * OnAudioFocusChangeListener l：音频焦点状态监听器
     * int streamType：请求焦点的音频类型
     * int durationHint：请求焦点音频持续性的指示
     *      AUDIOFOCUS_GAIN：指示申请得到的音频焦点不知道会持续多久，一般是长期占有
     *      AUDIOFOCUS_GAIN_TRANSIENT：指示要申请的音频焦点是暂时性的，会很快用完释放的
     *      AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK：指示要申请的音频焦点是暂时性的，同时还指示当前正在使用焦点的音频可以继续播放，只是要“duck”一下（降低音量）
     */
    private fun tryToGetAudioFocus() {
        var result =
        am.requestAudioFocus(
            audioHelp.audioFocusChangeListener,//状态监听器
            AudioManager.STREAM_MUSIC,//
            AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioHelp.currentAudioFocusState = AudioHelp.AUDIO_FOCUSED;
        } else {
            audioHelp.currentAudioFocusState = AudioHelp.AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * 放弃音频焦点
     */
    private fun giveUpAudioFocus() {
        //申请放弃音频焦点
        if (am.abandonAudioFocus(audioHelp.audioFocusChangeListener)
            == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //AudioManager.AUDIOFOCUS_REQUEST_GRANTED 申请成功
            audioHelp.currentAudioFocusState = AudioHelp.AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private inner class MyPlayerCallback : PlayerCallback {
        override fun onPrepared() {
            setMetadata()
            if (player.isOncePlay) {
                playMusic()
                val item = musicList!![currentMusic]
                musicProvider.savePlayRecord(item.formId, item.id, player.getCurrentPosition())
            }
            player.isOncePlay = true
        }

        override fun onCompletion() {
            this@MusicService.playItem(currentMusic + 1.toLong())
        }
    }

    /**
     * 设置媒体信息
     */
    private fun setMetadata() {
        if (musicList == null) {
            return
        }
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

    private inner class AudioHelpCallback : AudioHelp.AudioHelpCallback {
        override fun isPlaying(): Boolean {
            return player.isPlaying()
        }

        override fun pause() {
            playerOrPause()
        }

        override fun setVolume(volume: Float) {
            player.setVolume(volume)
        }

        override fun play() {
            playerOrPause()
        }
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
            Log.e("===", "updatedState=${updatedState}")
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

//                        // If playback has ended, also stop the service.
//                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
//                            stopSelf()
//                        }

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