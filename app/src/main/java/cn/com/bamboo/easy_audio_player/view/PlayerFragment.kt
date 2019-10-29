package cn.com.bamboo.easy_audio_player.view

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.Observer
import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.adapter.PlayerListAdapter
import cn.com.bamboo.easy_audio_player.databinding.FragmentPlayerBinding
import cn.com.bamboo.easy_audio_player.util.IntentKey
import cn.com.bamboo.easy_audio_player.view_model.MusicViewModel
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelFragment

class PlayerFragment : BaseViewModelFragment<FragmentPlayerBinding, MusicViewModel>() {
    private var adapter: PlayerListAdapter = PlayerListAdapter()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initMedia(context!!)
        setTitleAndBackspace("播放音乐")
        viewModel.formList.observe(this, Observer {
            adapter.replaceData(it)
        })

        viewModel.musicList.observe(this, Observer {
            adapter.replaceData(it)
        })
        viewModel.nowPlaying.observe(this, Observer {
            adapter.metadata = it
            adapter.notifyDataSetChanged()
        })

        adapter.setOnItemClickListener { adapter, view, position ->
            val item = adapter.getItem(position) as MediaSessionCompat.QueueItem
            when (item.description.extras!![IntentKey.QUEUE_TYPE]) {
                0 -> {
                    viewModel.loadMusicList(item.description.mediaId!!)
                }
                1 -> {
                    viewModel.playMusic(item.queueId)
                }
            }
        }
        binding.recyclerView.adapter = adapter
        binding.progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                viewModel.seekTo(seekBar?.progress)
            }

        })
    }
}