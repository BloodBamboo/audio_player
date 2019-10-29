package cn.com.bamboo.easy_audio_player.service

import android.media.MediaPlayer

class PlayerProvider(val callback: PlayerCallback) : PlayerConfig {

    private var currentState = PlayerConfig.STATE_IDLE

    private var mediaPlayer: MediaPlayer = MediaPlayer().apply {
        reset()
        setOnPreparedListener {
            callback.onPrepared()
        }

        setOnCompletionListener {
            callback.onCompletion()
            currentState = PlayerConfig.STATE_ENDED
        }
    }

    override fun setData(path: String) {
        reset()
        mediaPlayer.setDataSource(path)
    }

    override fun prepare() {
        mediaPlayer.prepare()
    }

    override fun play() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            currentState = PlayerConfig.STATE_PLAY
        }
    }

    override fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            currentState = PlayerConfig.STATE_PAUSE
        }
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    override fun getDuration(): Long {
        return mediaPlayer.duration.toLong()
    }

    override fun release() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    override fun reset() {
        mediaPlayer.reset()
    }

    override fun stop() {
        mediaPlayer.stop()
    }

    override fun getCurrentPosition(): Long {
        return mediaPlayer.currentPosition.toLong()
    }

    override fun prev(path: String) {
        setData(path)
        pause()
        play()
    }

    override fun next(path: String) {
        setData(path)
        pause()
        play()
    }

    override fun getState(): Int {
        return currentState
    }

    override fun seekTo(pos: Long) {
        mediaPlayer.seekTo(pos.toInt())
    }
}