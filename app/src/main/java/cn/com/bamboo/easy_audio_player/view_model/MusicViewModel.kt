package cn.com.bamboo.easy_audio_player.view_model

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.service.MusicService
import cn.com.bamboo.easy_audio_player.util.IntentKey
import cn.com.bamboo.easy_audio_player.util.currentPlayBackPosition
import cn.com.edu.hnzikao.kotlin.base.BaseViewModel

/**
 * 音乐播放viewModel
 */
class MusicViewModel(application: Application) : BaseViewModel(application) {

    val POSITION_UPDATE_INTERVAL_MILLIS = 1000L
    val isConnected = MutableLiveData<Boolean>(false)
    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }

    val play = ObservableBoolean(true)

    val title = ObservableField<String>()
    val playTime =
        ObservableField<String>(getApplication<MusicApp>().getString(R.string.duration_unknown))
    val playDurationTime =
        ObservableField<String>(getApplication<MusicApp>().getString(R.string.duration_unknown))
    val progress = ObservableInt(0)

    val formList = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
    val musicList = MutableLiveData<List<MediaSessionCompat.QueueItem>>()

    private lateinit var mediaId: String

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var mediaBrowserConnectionCallback: MediaBrowserConnectionCallback

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        /**
         * 连接服务成功时，注册mediaBrowser并获取MediaControllerCompat
         */
        override fun onConnected() {
            if (mediaBrowser.isConnected) {
                mediaId = mediaBrowser.getRoot()
                mediaBrowser.unsubscribe(mediaId)
                mediaBrowser.subscribe(mediaId, SubscriptionCallback())

                mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                    registerCallback(MediaControllerCallback())
                }
                isConnected.postValue(true)
            }
        }

        /**
         * Invoked when the client is disconnected from the media browser.
         */
        override fun onConnectionSuspended() {
            mediaBrowser.unsubscribe(mediaId)
            isConnected.postValue(false)
        }

        /**
         * Invoked when the connection to the media browser failed.
         */
        override fun onConnectionFailed() {
            isConnected.postValue(false)
        }
    }

    /**
     * 向媒体浏览器服务(MediaBrowserService)发起数据订阅请求的回调接口
     */
    private inner class SubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {
        /**
         * 注册成功后获取歌单列表
         */
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            super.onChildrenLoaded(parentId, children)
            formList.value = children.mapIndexed { idx, value ->
                MediaSessionCompat.QueueItem(value.description, idx.toLong())
            }
        }
    }


    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        /**
         * 播放状态改变状态回调
         */
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
            when (state?.state) {
                PlaybackStateCompat.STATE_PAUSED,PlaybackStateCompat.STATE_NONE  -> {
                    play.set(true)
                    updatePosition = false
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    play.set(false)
                    updatePosition = true
                    checkPlaybackPosition()
                }
            }
        }

        /**
         * 播放媒体信息切换回调
         */
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            nowPlaying.postValue(metadata ?: NOTHING_PLAYING)
            playDurationTime.set(timestampToMSS(metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)))
            title.set(metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        }

        /**
         * 歌单或者音乐列表
         */
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>) {
            when (queue[0].description.extras!!.get(IntentKey.QUEUE_TYPE)) {
                0 -> {
                    formList.value = queue
                }
                1 -> {
                    musicList.value = queue
                }
            }
        }

        /**
         * Normally if a [MediaBrowserServiceCompat] drops its connection the callback comes via
         * [MediaControllerCompat.Callback] (here). But since other connection status events
         * are sent to [MediaBrowserCompat.ConnectionCallback], we catch the disconnect here and
         * send it on to the other callback.
         */
        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }


    @Suppress("PropertyName")
    val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
        .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
        .build()

    @Suppress("PropertyName")
    val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        mediaBrowser.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        updatePosition = false
    }

    /**
     * 初始化播放浏览
     */
    fun initMedia(context: Context) {
        mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
        mediaBrowser = MediaBrowserCompat(
            context,
            ComponentName(context, MusicService::class.java),
            mediaBrowserConnectionCallback, null
        )
        mediaBrowser.connect()
    }

    private fun checkPlaybackPosition(): Boolean {
        return handler.postDelayed({
            if (playbackState.value != null &&
                nowPlaying.value != null &&
                playbackState.value!!.state == PlaybackStateCompat.STATE_PLAYING
            ) {
                val pos = playbackState.value!!.currentPlayBackPosition
                val duration = nowPlaying.value!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                if (pos <= duration) {
                    playTime.set(timestampToMSS(pos))
                }
                progress.set((pos.toFloat() / duration * 100).toInt())
                }

            if (updatePosition) {
                checkPlaybackPosition()
            }
        }, POSITION_UPDATE_INTERVAL_MILLIS)
    }

    /**
     * 格式化播放时间
     */
    fun timestampToMSS(totalSeconds: Long?): String {
        if (totalSeconds == null) {
            return getApplication<MusicApp>().getString(R.string.duration_unknown)
        }
        val temp = totalSeconds / 1000
        val minutes = temp / 60
        val remainingSeconds = temp - (minutes * 60)
        return if (totalSeconds < 0) getApplication<MusicApp>().getString(R.string.duration_unknown)
        else getApplication<MusicApp>().getString(R.string.duration_format).format(
            minutes,
            remainingSeconds
        )
    }

    fun onRefreshForm(view: View) {
        mediaController.transportControls.sendCustomAction(IntentKey.LOAD_MUSIC_LIST, null)
    }

    fun onMusicForm(view: View) {
        formList.value?.let {
            formList.value = it
        }
    }

    fun onMusic(view: View) {
        musicList.value?.let {
            musicList.value = it
        }
    }

    fun onTiming(view: View) {
        mediaController.transportControls.stop()
    }

    fun onPrev(view: View) {
        mediaController.transportControls.skipToPrevious()
    }

    fun onPlay(view: View) {
        mediaController.transportControls.pause()
    }

    fun onNext(view: View) {
        mediaController.transportControls.skipToNext()
    }

    fun loadMusicList(formId: String) {

        mediaController.transportControls.sendCustomAction(
            IntentKey.LOAD_MUSIC_LIST,
            Bundle().apply {
                putString(IntentKey.FORM_ID, formId)
            })
    }

    fun playMusic(id: Long) {
        mediaController.transportControls.skipToQueueItem(id)
    }

    fun seekTo(progress: Int?) {
        if (progress != null && nowPlaying.value != null) {
            val toTime =
                nowPlaying.value!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) * (progress / 100f)
            mediaController.transportControls.seekTo(toTime.toLong())
        }
    }
}