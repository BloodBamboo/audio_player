package cn.com.bamboo.easy_audio_player.service

import android.media.MediaPlayer
import android.util.Log
import java.io.File

class PlayerProvider(val callback: PlayerCallback, override var isOncePlay: Boolean) :
    PlayerConfig {

    private var currentState = PlayerConfig.STATE_IDLE

    private var mediaPlayer: MediaPlayer? = null


    private fun initMediaPlayer(callback: PlayerCallback): MediaPlayer {
        return MediaPlayer().apply {
            setOnPreparedListener {
                callback.onPrepared()
            }

            setOnCompletionListener {
                Log.e("===", "setOnCompletionListener")
                //修正当前状态一定要在回调之前，不然就会在执行回调后，修改状态，程序执行不正常
                currentState = PlayerConfig.STATE_IDLE
                callback.onCompletion()
            }
            seekTo(0)
            reset()
        }
    }

    override fun setData(path: String?): Boolean {
        Log.e("===", "setData_path=${path}")
        path?.let {
            return if (File(path).exists()) {
                if (mediaPlayer != null) {
                    mediaPlayer?.setOnCompletionListener(null)
                    mediaPlayer?.setOnPreparedListener(null)
                    release()
                }
                mediaPlayer = initMediaPlayer(callback)
                mediaPlayer?.setDataSource(path)
                true
            } else {
                false
            }
        }
        return false
    }

    override fun prepare() {
        mediaPlayer?.prepare()
        currentState = PlayerConfig.STATE_PREPARE
        Log.e("===", "prepare_currentState=${currentState}")
    }

    override fun play() {
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
            currentState = PlayerConfig.STATE_PLAY
        }
        Log.e("===", "play_currentState=${currentState}")
    }

    override fun pause() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            currentState = PlayerConfig.STATE_PAUSE
        }
    }

    override fun isPlaying(): Boolean {
        return if (mediaPlayer == null) {
            false
        } else {
            mediaPlayer!!.isPlaying

        }
    }

    override fun getDuration(): Long {
        return if (mediaPlayer == null) {
            0
        } else {
            mediaPlayer!!.duration.toLong()
        }
    }

    override fun release() {
        Log.e("===", "release")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentState = PlayerConfig.STATE_IDLE
    }

    override fun reset() {
        Log.e("===", "reset")
        mediaPlayer?.reset()
        currentState = PlayerConfig.STATE_IDLE
    }

    override fun stop() {
        Log.e("===", "stop")
        mediaPlayer?.stop()
        currentState = PlayerConfig.STATE_IDLE
    }

    override fun getCurrentPosition(): Long {
        return if (mediaPlayer == null) {
            0
        } else {
            mediaPlayer!!.currentPosition.toLong()
        }
    }

    override fun prev(path: String) {
        playPath(path)
    }

    override fun next(path: String) {
        playPath(path)
    }

    private fun playPath(path: String) {
        if (setData(path)) {
            pause()
            play()
        }
    }

    override fun getState(): Int {
        Log.e("===", "getState=${currentState}")
        return currentState
    }

    override fun seekTo(pos: Long) {
        mediaPlayer?.seekTo(pos.toInt())
    }

    override fun setVolume(volumeDuck: Float) {
        mediaPlayer?.setVolume(volumeDuck, volumeDuck)
    }
}