package cn.com.bamboo.easy_audio_player.view

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.adapter.PlayerRecordAdapter
import cn.com.bamboo.easy_audio_player.databinding.FragmentRecordBinding
import cn.com.bamboo.easy_audio_player.util.PlayerRecordEvent
import cn.com.bamboo.easy_audio_player.view_model.PlayerRecordViewModel
import cn.com.bamboo.easy_common.util.RxBus
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelFragment

class PlayerRecordFragment: BaseViewModelFragment<FragmentRecordBinding, PlayerRecordViewModel>() {

    private lateinit var adapter: PlayerRecordAdapter

    override fun initContentView(): Int {
        return R.layout.fragment_record
    }

    override fun initVariableId(): Int {
        return BR.recordViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitleAndBackspace("播放记录")
        adapter = PlayerRecordAdapter(context!!)
        adapter.itemViewOnClick = { item, position ->
            item?.let {
                RxBus.default?.post(PlayerRecordEvent(it))
            }
        }

        adapter.itemViewOnLongOnClick = { item, position ->
            item?.let {
                viewModel.removeItem(it)
            }
        }
        binding.recyclerView.adapter = adapter
        viewModel.recordList.observe(this, Observer(adapter::submitList))
    }
}