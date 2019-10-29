package cn.com.bamboo.easy_audio_player.view

import android.os.Bundle
import android.view.View
import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.databinding.FragmentRecordBinding
import cn.com.bamboo.easy_audio_player.view_model.PlayerRecordViewModel
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelFragment

class PlayerRecordFragment: BaseViewModelFragment<FragmentRecordBinding, PlayerRecordViewModel>() {
    override fun initContentView(): Int {
        return R.layout.fragment_record
    }

    override fun initVariableId(): Int {
        return BR.recordViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitleAndBackspace("播放记录")
    }
}