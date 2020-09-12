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
import android.util.Log
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import cn.com.bamboo.easy_audio_player.BuildConfig
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.service.MusicService
import cn.com.bamboo.easy_audio_player.util.Constant
import cn.com.bamboo.easy_audio_player.util.IntentKey
import cn.com.bamboo.easy_audio_player.util.currentPlayBackPosition
import cn.com.bamboo.easy_audio_player.vo.PlayerRecordInfo
import cn.com.bamboo.easy_common.util.StringUtil
import cn.com.edu.hnzikao.kotlin.base.BaseViewModel

/**
 * 音乐播放viewModel
 */
class MusicViewModel(application: Application) : BaseViewModel(application) {

    val isConnected = MutableLiveData<Boolean>(false)
    val location = MutableLiveData<Boolean>(true)
    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val nowPlaying = MutableLiveData<MediaMetadataCompat>()

    val play = ObservableBoolean(true)

    val title = ObservableField<String>()
    val playTime =
        ObservableField<String>(getApplication<MusicApp>().getString(R.string.duration_unknown))
    val playDurationTime =
        ObservableField<String>(getApplication<MusicApp>().getString(R.string.duration_unknown))
    val progress = ObservableInt(0)
    val timingText = ObservableField<String>("定时")

    val formList = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
    val musicList = MutableLiveData<List<MediaSessionCompat.QueueItem>>()
    val showTiming = MutableLiveData<Boolean>()
    var playerRecordInfo: PlayerRecordInfo? = null
    var isShowTime = true

    var enableSeekBar = true

    private lateinit var mediaId: String

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var mediaBrowserConnectionCallback: MediaBrowserConnectionCallback
    private lateinit var mediaControllerCallback: MediaControllerCallback
    private lateinit var subscriptionCallback: SubscriptionCallback

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        /**
         * 连接服务成功时，注册mediaBrowser并获取MediaControllerCompat
         */
        override fun onConnected() {
            if (mediaBrowser.isConnected) {
                Log.e("===", "onConnected")
                mediaId = mediaBrowser.getRoot()
                mediaBrowser.unsubscribe(mediaId)
                subscriptionCallback = SubscriptionCallback()
                mediaBrowser.subscribe(mediaId, subscriptionCallback)
                mediaControllerCallback = MediaControllerCallback()
                mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                    registerCallback(mediaControllerCallback)
                }
                isConnected.postValue(true)
            }
        }

        /**
         * Invoked when the client is disconnected from the media browser.
         */
        override fun onConnectionSuspended() {
            Log.e("===", "onConnectionSuspended")
            mediaBrowser.unsubscribe(mediaId, subscriptionCallback)
            mediaController.unregisterCallback(mediaControllerCallback)
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
                PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_NONE -> {
                    play.set(true)
                    updatePosition = false
                    checkPlaybackPosition()
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
            playDurationTime.set(
                StringUtil.timestampToMSS(
                    getApplication<MusicApp>(),
                    metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                )
            )
            title.set(metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        }

        /**
         * 0歌单1音乐列表2播放记录列表
         */
        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>) {
            if (queue.isEmpty()) {
                return
            }
            Log.e("===", "onQueueChanged${queue[0].description.extras!!.get(IntentKey.QUEUE_TYPE)}")
            when (queue[0].description.extras!!.get(IntentKey.QUEUE_TYPE)) {
                0 -> {
                    formList.value = queue
                }
                1 -> {
                    musicList.value = queue
                }
                2 -> {
                    val item = queue[0].description.extras!!
                    showPlayerRecord(PlayerRecordInfo().apply {
                        musicId = item.getInt(IntentKey.PLAYER_RECORD_MUSICID_INT)
                        if (enableSeekBar) progress = item.getLong(IntentKey.PLAYER_RECORD_PROGRESS_LONG)
                        formId = item.getInt(IntentKey.PLAYER_RECORD_FORMID_INT)
                        musicName = queue[0].description.title.toString()
                        formName = queue[0].description.subtitle.toString()
                        id = queue[0].description.mediaId!!.toInt()
                        recordTime = item.getLong(IntentKey.PLAYER_RECORD_RECORDTIME_LONG)
                    })
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
            Log.e("===", "onSessionDestroyed")
            mediaBrowserConnectionCallback.onConnectionSuspended()
//            mediaController.unregisterCallback(this)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            when (event) {
                IntentKey.PLAY_TIMING_NEXT_LONG -> {
                    extras?.let {
                        if (isShowTime) {
                            val timeNum = extras.getLong(IntentKey.PLAY_TIMING_LONG)
                            val time = extras.getLong(IntentKey.PLAY_TIMING_NEXT_LONG)

                            timingText.set(
                                "定时${StringUtil.timestampToMSS(
                                    getApplication(),
                                    (timeNum - time - 1) * 1000
                                )}"
                            )
                        }
                    }
                }
                IntentKey.PLAY_TIMING_ERROR_STRING -> {
                    if (extras?.getString(IntentKey.PLAY_TIMING_ERROR_STRING) != null) {
                        setMessage(extras.getString(IntentKey.PLAY_TIMING_ERROR_STRING))
                    }
                }
                IntentKey.PLAY_TIMING_COMPLETE -> {
                    setMessage("定时结束")
                    timingText.set("定时")
                    if (playbackState.value?.state == PlaybackStateCompat.STATE_PLAYING) {
                        mediaController.transportControls.sendCustomAction(
                            IntentKey.PLAY_TIMING_PAUSE,
                            null
                        )
                    }
                }
            }
        }
    }

    fun showPlayerRecord(info: PlayerRecordInfo) {
        playerRecordInfo = info
        playTime.set(
            StringUtil.timestampToMSS(
                getApplication<MusicApp>(),
                info.progress
            )
        )
        loadMusicList(info.formId.toString())
    }

    fun playMusicByMusicId(mediaId: Int, progress: Long, play: Boolean) {
        mediaController.transportControls.playFromMediaId(
            mediaId.toString(),
            Bundle().apply {
                putLong(IntentKey.PLAYER_RECORD_PROGRESS_LONG, progress)
                putBoolean(IntentKey.LOAD_PLAY_RECORD, play)
            })
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

    override fun onCleared() {
        super.onCleared()
        Log.e("===", "onCleared")
        mediaController.transportControls.sendCustomAction(IntentKey.STOP_SEVER, null)
        mediaBrowser.disconnect()
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
            if (isShowTime) {
                setPlayTime()
            }
            if (updatePosition) {
                checkPlaybackPosition()
            }
        }, Constant.POSITION_UPDATE_INTERVAL_MILLIS)
    }

    private fun setPlayTime() {
        if (playbackState != null
            && nowPlaying != null
            && playbackState.value != null
            && nowPlaying.value != null
        ) {
            val pos = playbackState.value!!.currentPlayBackPosition
            val duration = nowPlaying.value!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            if (pos <= duration) {
                playTime.set(StringUtil.timestampToMSS(getApplication<MusicApp>(), pos))
            }
            progress.set((pos.toFloat() / duration * 100).toInt())
            if (BuildConfig.DEBUG) {
                Log.i("===setPlayTime", "${pos}")
            }
        }
    }

    fun onRefreshForm(view: View) {
        mediaController.transportControls.sendCustomAction(IntentKey.LOAD_FORM_LIST, null)
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
        showTiming.value = true
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

    fun onLocation(view: View) {
        location.value = true
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

    fun loadPlayerRecordInfo() {
        mediaController.transportControls.sendCustomAction(
            IntentKey.LOAD_PLAYER_RECORD, null
        )
    }

    fun startTiming(timeNum: Long) {
        if (timeNum == 0L) {
            timingText.set("定时")
        }

        val extras = Bundle()
        extras.putLong(IntentKey.PLAY_TIMING_LONG, timeNum)
        mediaController.transportControls.sendCustomAction(
            IntentKey.PLAY_TIMING_LONG, extras
        )
    }
}