package cn.com.bamboo.easy_audio_player.view

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import androidx.lifecycle.Observer
import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.adapter.PlayerListAdapter
import cn.com.bamboo.easy_audio_player.databinding.FragmentPlayerBinding
import cn.com.bamboo.easy_audio_player.util.IntentKey
import cn.com.bamboo.easy_audio_player.util.PlayerRecordEvent
import cn.com.bamboo.easy_audio_player.view_model.MusicViewModel
import cn.com.bamboo.easy_common.util.RxBus
import cn.com.bamboo.easy_common.util.RxJavaHelper
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelFragment
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_player.view.*
import org.jetbrains.anko.alert

class PlayerFragment : BaseViewModelFragment<FragmentPlayerBinding, MusicViewModel>() {
    private var adapter: PlayerListAdapter = PlayerListAdapter()
    private var playerRecordDispos: Disposable? = null

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

        viewModel.musicList.observe(this, Observer { it ->
            adapter.replaceData(it)
            viewModel.playerRecordInfo?.let { info ->
                viewModel.title.set(info.musicName)
                viewModel.playMusicByMusicId(info.musicId, info.progress)
                binding.recyclerView.scrollToPosition(adapter.indexOfByMusicId(info.musicId.toString()))
            }
        })
        viewModel.nowPlaying.observe(this, Observer {
            viewModel.playerRecordInfo?.let { info ->
                viewModel.progress.set(
                    (info.progress.toFloat() / it.getLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION
                    ) * 100).toInt()
                )
            }
            viewModel.playerRecordInfo = null
            adapter.metadata = it
            adapter.notifyDataSetChanged()
        })

        viewModel.isConnected.observe(this, Observer {
            if (it) {
                viewModel.loadPlayerRecordInfo()
            }
        })
        viewModel.showTiming.observe(this, Observer {
            activity?.alert {
                title = "定时,单位分钟"
                val editText = EditText(context)
                editText.setText("45")
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setTextColor(context!!.resources.getColor(R.color.text_primary))
                customView = editText
                positiveButton("确定") {
                    viewModel.startTiming(editText.text.toString().toLong() * 60)
                }
                negativeButton("取消") {

                }
            }?.show()
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
                2 -> {

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
        playerRecordDispos = RxBus.default?.toObservable(PlayerRecordEvent::class.java)!!
            .compose(RxJavaHelper.schedulersTransformer())
            .subscribe {
                viewModel.showPlayerRecord(it.info)
            }
    }

    override fun onDestroy() {
        playerRecordDispos?.dispose()
        super.onDestroy()
    }
}