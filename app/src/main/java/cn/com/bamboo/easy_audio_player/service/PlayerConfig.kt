package cn.com.bamboo.easy_audio_player.service

interface PlayerConfig {
    companion object {
        /**
         * The player does not have any media to play.
         */
        val STATE_IDLE
            get() = 1
        /**
         * The player is able to immediately play from its current position. The player will be playing if
         * [.getPlayWhenReady] is true, and paused otherwise.
         */
        val STATE_PLAY
            get() = 2
        /**
         *暂停
         */
        val STATE_PAUSE
            get() = 3
        /**
         * The player has finished playing the media.
         */
        val STATE_ENDED
            get() = 4

        val STATE_PREPARE
            get() = 5
    }

    fun setData(path: String?)
    fun play()
    fun prepare()
    fun pause()
    fun isPlaying(): Boolean
    fun seekTo(pos: Long)
    fun getDuration(): Long
    fun release()
    fun reset()
    fun stop()
    fun getCurrentPosition(): Long
    fun prev(path: String)
    fun next(path: String)
    fun getState(): Int
}

interface PlayerCallback {
    fun onPrepared()
    fun onCompletion()
}