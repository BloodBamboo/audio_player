package cn.com.bamboo.easy_audio_player.view_model

import android.app.Application
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.edu.hnzikao.kotlin.base.BaseViewModel

/**
 * 音乐播放viewModel
 */
class MusicViewModel(application: Application) : BaseViewModel(application) {
    fun show(): String {
        return getApplication<MusicApp>().database.toString()
    }
}