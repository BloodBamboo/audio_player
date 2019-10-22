package cn.com.bamboo.easy_audio_player.service

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import cn.com.bamboo.easy_audio_player.util.IntentKey


class MusicService : MediaBrowserServiceCompat() {

    private lateinit var mediaPlayer:MediaPlayer
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var mPlaybackState: PlaybackStateCompat
    //管理多个session,暂时没有使用
    //private lateinit var mediaSessionManager: MediaSessionManager


    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener{

            }

            setOnCompletionListener {

            }
        }
        mPlaybackState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_NONE,0,1.0f)
            .build()
        mediaSessionCompat = MediaSessionCompat(this, IntentKey.SERVICE_NAME).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(mPlaybackState)
            setCallback(SessionCallback())
        }

        sessionToken = mediaSessionCompat.sessionToken
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.run {
            isActive = false
            release()
        }
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.detach()



    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(IntentKey.MEDIA_ID_ROOT, null)
    }

    private inner class SessionCallback: MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
        }

        override fun onPause() {
            super.onPause()
        }

        override fun onPrepare() {
            super.onPrepare()
        }

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
        }
    }


}