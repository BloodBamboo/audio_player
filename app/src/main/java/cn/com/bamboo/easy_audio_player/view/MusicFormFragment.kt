package cn.com.bamboo.easy_audio_player.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.view.get
import androidx.lifecycle.Observer
import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.adapter.MusicFormAdapter
import cn.com.bamboo.easy_audio_player.databinding.FragmentMusicFormBinding
import cn.com.bamboo.easy_audio_player.util.IntentKey
import cn.com.bamboo.easy_audio_player.view_model.MusicFormViewModel
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelFragment
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast

/**
 * 歌单列表
 */
class MusicFormFragment : BaseViewModelFragment<FragmentMusicFormBinding, MusicFormViewModel>() {

    lateinit var adapter: MusicFormAdapter

    /**
     * 页面布局
     * @return
     */
    override fun initContentView(): Int {
        return R.layout.fragment_music_form
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
        setTitleAndBackspace("歌单列表")
        toolbar?.inflateMenu(R.menu.menu_item)
        toolbar?.menu!![0].title = "新建"
        toolbar?.setOnMenuItemClickListener {
            viewModel.onCreateForm()
            return@setOnMenuItemClickListener true
        }
        viewModel.showFormDialog.observe(this, Observer {
            activity?.alert {
                this.message = "歌单名称"
                val nameEditText = EditText(activity)
                nameEditText.hint = "文件夹名称"
                nameEditText.setTextColor(activity!!.resources.getColor(R.color.text_primary))
                this.customView = nameEditText
                this.positiveButton("确定") {
                    if (nameEditText.text.isEmpty()) {
                        activity!!.toast("歌单名称不能为空")
                    } else {
                        viewModel.createForm(nameEditText.text.toString())
                    }
                }
                this.negativeButton("取消") {
                }
            }?.show()
        })
        adapter = MusicFormAdapter(this.context!!)
        adapter.itemViewOnClick = { item, position ->
            item?.let {
                startActivity(Intent(context, MusicListActivity::class.java).apply {
                    putExtra(IntentKey.FORM_ID, it.id)
                    putExtra(IntentKey.FORM_NAME, it.name)
                })
            }
        }
        adapter.itemViewOnLongOnClick = { item, position ->
            item?.let {
                viewModel.removeItem(it)
            }
        }
        binding.recyclerView.adapter = adapter
        viewModel.formList.observe(this, Observer(adapter::submitList))
    }


}