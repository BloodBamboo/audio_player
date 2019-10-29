package cn.com.bamboo.easy_audio_player.view_model

import android.app.Application
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.edu.hnzikao.kotlin.base.BaseViewModel

class PlayerRecordViewModel(application: Application) : BaseViewModel(application) {
    val dao = getApplication<MusicApp>().database.playerRecordDao()
    val recordList = LivePagedListBuilder(
        dao.loadPlayRecordAll(),
        PagedList.Config.Builder().setPageSize(20)
            .setEnablePlaceholders(true)
            .build()
    ).build()




}