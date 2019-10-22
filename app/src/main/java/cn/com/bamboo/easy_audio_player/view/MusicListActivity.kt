package cn.com.bamboo.easy_audio_player.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.view.get
import androidx.lifecycle.Observer
import cn.com.bamboo.easy_file_manage.FileManageActivity
import cn.com.bamboo.easy_file_manage.util.FILE_REQUEST
import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.adapter.MusicAdapter
import cn.com.bamboo.easy_audio_player.view_model.MusicFormListViewModel
import cn.com.bamboo.easy_audio_player.vo.Music
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelActivity
import org.jetbrains.anko.toast

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
        setTitleAndBackspace("歌单列表")
        formId = intent.getIntExtra(FORM_ID, -1)
        if (formId < 0) {
            this.toast("formId小于0")
            finish()
            return
        }
        toolbar?.inflateMenu(R.menu.menu_item)
        toolbar?.menu!![0].title = "新增"
        toolbar?.setOnMenuItemClickListener {
            val intent = Intent(this, FileManageActivity::class.java)
            intent.putExtra(FileManageActivity.GET_PATHS, true)
            startActivityForResult(intent, FILE_REQUEST)
            return@setOnMenuItemClickListener true
        }
        binding.recyclerView.adapter = musicAdapter
        viewModel.LoadDataByFormId(formId)
        viewModel.musicList.observe(this, Observer(musicAdapter::submitList))
        musicAdapter.itemViewOnClick = { item: Music?, position: Int ->
            this.toast("播放音乐")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            var list = ArrayList<String>()
            for (i in 0..data.clipData.itemCount) {
                list.add(data.clipData.getItemAt(i).htmlText)
                Log.e("======", data.clipData.getItemAt(i).htmlText)
            }
            viewModel.saveMusicPath(list, formId)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}