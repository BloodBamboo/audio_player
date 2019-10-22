package cn.com.bamboo.easy_audio_player.view_model

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import cn.com.bamboo.easy_audio_player.service.MusicService
import cn.com.edu.hnzikao.kotlin.base.BaseViewModel


class MainViewModel(application: Application) : BaseViewModel(application) {
    val isConnected = MutableLiveData<Boolean>(false)
    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var mediaBrowserConnectionCallback: MediaBrowserConnectionCallback


    fun initMedia(context: Context) {
        mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
        mediaBrowser = MediaBrowserCompat(
            context,
            ComponentName(context, MusicService::class.java),
            mediaBrowserConnectionCallback, null
        )
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


    override fun onStart() {
        super.onStart()
        mediaBrowser. connect()
    }

    override fun onStop() {
        super.onStop()
        mediaBrowser. disconnect()
    }


    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        /**
         * Invoked after [MediaBrowserCompat.connect] when the request has successfully
         * completed.
         */
        override fun onConnected() {
            // Get a MediaController for the MediaSession.
            if (mediaBrowser.isConnected) {
                var mediaId = mediaBrowser.getRoot()
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
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            super.onChildrenLoaded(parentId, children)
        }
    }


    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            nowPlaying.postValue(metadata ?: NOTHING_PLAYING)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
        }

//        override fun onSessionEvent(event: String?, extras: Bundle?) {
//            super.onSessionEvent(event, extras)
//            when (event) {
//                NETWORK_FAILURE -> networkFailure.postValue(true)
//            }
//        }

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
}