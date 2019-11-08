package cn.com.bamboo.easy_audio_player.service

import android.media.AudioManager
import android.util.Log

class AudioHelp(var callback: AudioHelpCallback) {
    interface AudioHelpCallback {
        fun pause()
        fun setVolume(volume:Float)
        fun play()
        fun isPlaying():Boolean
    }

    companion object {
        //当音频失去焦点，且不需要停止播放，只需要减小音量时，我们设置的媒体播放器音量大小
        //例如微信的提示音响起，我们只需要减小当前音乐的播放音量即可
        val VOLUME_DUCK = 0.2f
        //当我们获取音频焦点时设置的播放音量大小
        val VOLUME_NORMAL = 1.0f

        //没有获取到音频焦点，也不允许duck状态
        val AUDIO_NO_FOCUS_NO_DUCK = 0
        //没有获取到音频焦点，但允许duck状态
        val AUDIO_NO_FOCUS_CAN_DUCK = 1
        //完全获取音频焦点
        val AUDIO_FOCUSED = 2
    }

    //当前音频焦点的状态
    var currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK

    var mPlayOnFocusGain: Boolean = false

    /**
     * 根据音频焦点的设置重新配置播放器 以及 启动/重新启动 播放器。调用这个方法 启动/重新启动 播放器实例取决于当前音频焦点的状态。
     * 因此如果我们持有音频焦点，则正常播放音频；如果我们失去音频焦点，播放器将暂停播放或者设置为低音量，这取决于当前焦点设置允许哪种设置
     */
    private fun configurePlayerState() {
        Log.e("===", "configurePlayerState. currentAudioFocusState=${currentAudioFocusState}")
        if (currentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            callback.pause()
        } else {
            if (currentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we 'duck', ie: play softly
                callback.setVolume(VOLUME_DUCK)
            } else {
                callback.setVolume(VOLUME_NORMAL)
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                //播放的过程中因失去焦点而暂停播放，短暂暂停之后仍需要继续播放时会进入这里执行相应的操作
                callback.play()
                mPlayOnFocusGain = false
            }
        }
    }

    /**
     * 请求音频焦点成功之后监听其状态的Listener
     */
    val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
        when (it) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                currentAudioFocusState = AUDIO_FOCUSED
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                currentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
                mPlayOnFocusGain = callback != null && callback.isPlaying()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost audio focus, probably "permanently"
                currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
            }
        }
        configurePlayerState()
    }
}