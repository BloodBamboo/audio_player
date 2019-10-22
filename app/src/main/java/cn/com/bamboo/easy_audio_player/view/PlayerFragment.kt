package cn.com.bamboo.easy_audio_player.view

import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.databinding.FragmentPlayerBinding
import cn.com.bamboo.easy_audio_player.view_model.MusicViewModel
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelFragment

class PlayerFragment : BaseViewModelFragment<FragmentPlayerBinding, MusicViewModel>() {

    /**
     * 页面布局
     * @return
     */
    override fun initContentView(): Int {
        return R.layout.fragment_player
    }

    /**
     * 初始化ViewModel的id
     * @return BR的id
     */
    override fun initVariableId(): Int {
        return BR.musicViewModel
    }
}