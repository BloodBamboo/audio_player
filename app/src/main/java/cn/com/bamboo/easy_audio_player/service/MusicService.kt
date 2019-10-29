package cn.com.bamboo.easy_audio_player.service

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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


    //管理多个session,暂时没有使用
    private var currentMusic: Int = 0
    private var playerSpeed = 1.0f

    override fun onCreate() {
        super.onCreate()
        musicProvider = MusicProvider((application as MusicApp).database)
        mediaSessionCompat = MediaSessionCompat(this, IntentKey.SERVICE_NAME).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(
                getPlaybackStateCompat(
                    mapPlaybackState(player.getState()),
                    player.getCurrentPosition()
                )
            )
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
            if (player.getState() == PlayerConfig.STATE_PLAY) {
                player.pause()
            } else if (player.getState() == PlayerConfig.STATE_PAUSE) {
                player.play()
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

    private fun mapPlaybackState(playerConfigState: Int): Int {
        return when (playerConfigState) {
            PlayerConfig.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
            PlayerConfig.STATE_PLAY -> PlaybackStateCompat.STATE_PLAYING
            PlayerConfig.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            PlayerConfig.STATE_PAUSE -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_NONE
        }
    }

    private fun getPlaybackStateCompat(playbackState: Int, pos: Long): PlaybackStateCompat {
        return PlaybackStateCompat.Builder()
            .setState(playbackState, pos, playerSpeed)
            .build()
    }
}