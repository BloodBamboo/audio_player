package cn.com.bamboo.easy_audio_player.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.get
import androidx.lifecycle.Observer
import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.adapter.MusicAdapter
import cn.com.bamboo.easy_audio_player.databinding.ActivityMusicFormListBinding
import cn.com.bamboo.easy_audio_player.util.IntentKey.FORM_ID
import cn.com.bamboo.easy_audio_player.util.IntentKey.FORM_NAME
import cn.com.bamboo.easy_audio_player.view_model.MusicFormListViewModel
import cn.com.bamboo.easy_file_manage.FileManageActivity
import cn.com.bamboo.easy_file_manage.FileManageActivity.Companion.RESULT_JSON
import cn.com.bamboo.easy_file_manage.util.FILE_REQUEST
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jetbrains.anko.toast

/**
 * 歌单内容列表管理
 */
class MusicListActivity :
    BaseViewModelActivity<ActivityMusicFormListBinding, MusicFormListViewModel>() {
    var formId: Int = -1
    private val musicAdapter = MusicAdapter(this)

    override fun initContentView(): Int {
        return R.layout.activity_music_form_list
    }

    override fun initVariableId(): Int {
        return BR.musicList
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitleAndBackspace(intent.getStringExtra(FORM_NAME))
        formId = intent.getIntExtra(FORM_ID, -1)
        if (formId < 0) {
            this.toast("formId小于0")
            finish()
            return
        }
        toolbar?.inflateMenu(R.menu.menu_item)
        toolbar?.menu!![0].title = "新增"
        toolbar?.setOnMenuItemClickListener {
            Intent(this, FileManageActivity::class.java).apply {
                putExtra(FileManageActivity.GET_PATHS, true)
                putExtra(FileManageActivity.GET_PATH_TYPE, 1)
                startActivityForResult(this, FILE_REQUEST)
            }
            return@setOnMenuItemClickListener true
        }
        binding.recyclerView.adapter = musicAdapter
        viewModel.LoadDataByFormId(formId)
        viewModel.musicList.observe(this, Observer(musicAdapter::submitList))
        musicAdapter.itemViewOnLongOnClick = { item, position ->
            item?.let {
                viewModel.removeItem(it)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            viewModel.saveMusicPath(
                Gson().fromJson(
                    data.getStringExtra(RESULT_JSON),
                    object : TypeToken<List<String>>() {}.type
                ), formId
            )
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}