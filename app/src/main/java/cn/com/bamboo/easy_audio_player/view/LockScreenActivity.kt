package cn.com.bamboo.easy_audio_player.view

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.get
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.service.MusicService
import cn.com.bamboo.easy_audio_player.util.Constant
import cn.com.bamboo.easy_audio_player.util.IntentKey
import cn.com.bamboo.easy_audio_player.util.TimingUtil
import cn.com.bamboo.easy_audio_player.util.currentPlayBackPosition
import cn.com.bamboo.easy_common.util.SharedPreferencesUtil
import cn.com.bamboo.easy_common.util.StringUtil
import kotlinx.android.synthetic.main.activity_lock_screen.*

class LockScreenActivity : AppCompatActivity(R.layout.activity_lock_screen) {


    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var mediaBrowserConnectionCallback: MediaBrowserConnectionCallback
    private lateinit var mediaControllerCallback: MediaControllerCallback
    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())
    var playbackState: PlaybackStateCompat? = null
    var nowPlaying: MediaMetadataCompat? = null
    var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        super.onCreate(savedInstanceState)
        (application as MusicApp).lockScreenVisible = true
        buildToolbar()
        mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(this)
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MusicService::class.java),
            mediaBrowserConnectionCallback, null
        )
        mediaBrowser.connect()
        image_play.setOnClickListener {
            mediaController?.transportControls.pause()
        }

        image_prev.setOnClickListener {
            mediaController?.transportControls.skipToPrevious()
        }

        image_next.setOnClickListener {
            mediaController?.transportControls.skipToNext()
        }

        progress_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val toTime =
                        nowPlaying!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) * (it.progress / 100f)
                    mediaController?.transportControls.seekTo(toTime.toLong())
                }
            }
        })
        text_timing.setOnClickListener {
            TimingUtil.alertTiming(this){
                startTiming(it)
            }
        }
    }

    /**
     * 构建toolbar
     */
    private fun buildToolbar() {
        toolbar = findViewById(cn.com.bamboo.easy_common.R.id.toolbar)
        if (toolbar != null) {
            var titleTextView: TextView? =
                toolbar?.findViewById(cn.com.bamboo.easy_common.R.id.toolbar_title)
            if (titleTextView == null) {
                toolbar?.setTitle(title)
            } else {
                titleTextView.setText(title)
            }

            toolbar?.setNavigationIcon(cn.com.bamboo.easy_common.R.mipmap.ic_arrow_back_white)
            toolbar?.setNavigationOnClickListener {
                finish()
            }
        }
    }

    override fun onDestroy() {
        mediaBrowser.disconnect()
        mediaController.unregisterCallback(mediaControllerCallback)
        (application as MusicApp).lockScreenVisible = false
        super.onDestroy()
    }

    private fun startTiming(timeNum: Long) {
        if (timeNum == 0L) {
            text_timing.text = getString(R.string.duration_unknown)
        }

        val extras = Bundle()
        extras.putLong(IntentKey.PLAY_TIMING_LONG, timeNum)
        mediaController.transportControls.sendCustomAction(
            IntentKey.PLAY_TIMING_LONG, extras
        )
    }

    private fun checkPlaybackPosition(): Boolean {
        return handler.postDelayed({
            if (playbackState != null &&
                nowPlaying != null &&
                playbackState!!.state == PlaybackStateCompat.STATE_PLAYING
            ) {
                setProgress()
            }

            if (updatePosition) {
                checkPlaybackPosition()
            }
        }, Constant.POSITION_UPDATE_INTERVAL_MILLIS)
    }

    private fun setProgress() {
        if (playbackState == null || nowPlaying == null) {
            return
        }

        val pos = playbackState!!.currentPlayBackPosition
        val duration = nowPlaying!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        progress_bar.progress = (pos.toFloat() / duration * 100).toInt()
        if (pos <= duration) {
            text_start_time.text = StringUtil.timestampToMSS(this, pos)
        }
    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        /**
         * 连接服务成功时，注册mediaBrowser并获取MediaControllerCompat
         */
        override fun onConnected() {
            if (mediaBrowser.isConnected) {
                Log.e("===", "LockScreenActivity_onConnected")
                mediaControllerCallback = MediaControllerCallback()
                mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                    registerCallback(mediaControllerCallback)
                }

                mediaController.transportControls.sendCustomAction(IntentKey.MUSIC_INFO, null)
            }
        }

        /**
         * Invoked when the client is disconnected from the media browser.
         */
        override fun onConnectionSuspended() {
            Log.e("===", "onConnectionSuspended")
            mediaController.unregisterCallback(mediaControllerCallback)
        }

        /**
         * Invoked when the connection to the media browser failed.
         */
        override fun onConnectionFailed() {
            Log.e("===", "onConnectionFailed")
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            when (event) {
                IntentKey.PLAY_TIMING_NEXT_LONG -> {
                    extras?.let {
                        val timeNum = extras.getLong(IntentKey.PLAY_TIMING_LONG)
                        val time = extras.getLong(IntentKey.PLAY_TIMING_NEXT_LONG)
                        text_timing.text =
                            "${StringUtil.timestampToMSS(
                                getApplication(),
                                (timeNum - time - 1) * 1000
                            )}"
                    }
                }
                IntentKey.PLAY_TIMING_ERROR_STRING -> {
                    if (extras?.getString(IntentKey.PLAY_TIMING_ERROR_STRING) != null) {
                        Toast.makeText(this@LockScreenActivity,
                            extras.getString(IntentKey.PLAY_TIMING_ERROR_STRING),
                            Toast.LENGTH_SHORT).show()
                    }
                }
                IntentKey.PLAY_TIMING_COMPLETE -> {
                    Toast.makeText(this@LockScreenActivity,
                        "定时结束",
                        Toast.LENGTH_SHORT).show()
                    text_timing.text = getString(R.string.duration_unknown)
//                    if (playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
//                        mediaController.transportControls.sendCustomAction(IntentKey.PLAY_TIMING_PAUSE, null)
//                    }
                }
            }
        }

        /**
         * 播放状态改变状态回调
         */
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState = state

            when (state?.state) {
                PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_NONE -> {
                    image_play.setImageResource(R.drawable.exo_icon_play)
                    updatePosition = false
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    image_play.setImageResource(R.drawable.exo_icon_pause)
                    updatePosition = true
                    checkPlaybackPosition()
                }
            }
        }

        /**
         * 播放媒体信息切换回调
         */
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (metadata == null) {
                return
            }
            nowPlaying = metadata
            if (nowPlaying != null ) {
                setProgress()
            }

            text_end_time.text = StringUtil.timestampToMSS(
                baseContext,
                metadata!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            )
            text_name.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}