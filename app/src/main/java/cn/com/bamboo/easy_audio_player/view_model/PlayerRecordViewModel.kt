package cn.com.bamboo.easy_audio_player.view_model

import android.app.Application
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.bamboo.easy_audio_player.vo.PlayerRecord
import cn.com.bamboo.easy_audio_player.vo.PlayerRecordInfo
import cn.com.bamboo.easy_common.util.RxJavaHelper
import cn.com.edu.hnzikao.kotlin.base.BaseViewModel
import io.reactivex.Maybe

class PlayerRecordViewModel(application: Application) : BaseViewModel(application) {
    fun removeItem(info: PlayerRecordInfo) {
        Maybe.create<Boolean> {
            dao.deleteItem(
                PlayerRecord(
                    info.id,
                    info.formId,
                    info.musicId,
                    info.description,
                    info.progress,
                    info.recordTime
                )
            )
        }
            .compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {

            }

    }

    val dao = getApplication<MusicApp>().database.playerRecordDao()
    val recordList = LivePagedListBuilder(
        dao.loadPlayerRecordInfoAll(),
        PagedList.Config.Builder().setPageSize(20)
            .setEnablePlaceholders(true)
            .build()
    ).build()




}