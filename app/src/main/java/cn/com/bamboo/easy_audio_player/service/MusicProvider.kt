package cn.com.bamboo.easy_audio_player.service

import cn.com.bamboo.easy_audio_player.db.MusicDatabase
import cn.com.bamboo.easy_audio_player.vo.Music
import cn.com.bamboo.easy_audio_player.vo.MusicForm
import cn.com.bamboo.easy_audio_player.vo.PlayerRecord
import cn.com.bamboo.easy_audio_player.vo.PlayerRecordInfo
import cn.com.bamboo.easy_common.util.RxJavaHelper
import io.reactivex.Maybe

class MusicProvider(var database: MusicDatabase) {

    fun getMusicFormList(list: (List<MusicForm>) -> Unit) {
        Maybe.create<List<MusicForm>?> {
            it.onSuccess(database.musicFormDao().loadAllList())
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {
                it?.let {
                    list(it)
                }
            }
    }

    fun getMusicList(formId: Int, list: (List<Music>) -> Unit) {
        Maybe.create<List<Music>?> {
            it.onSuccess(database.musicDao().loadMusicListByFormId(formId))
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {
                it?.let {
                    list(it)
                }
            }
    }

    fun getPlayerRecordList(list: (List<PlayerRecordInfo>) -> Unit) {
        Maybe.create<List<PlayerRecordInfo>?>{
            it.onSuccess(database.playerRecordDao().loadPlayerRecordInfoAll2List())
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {
                it?.let {
                    list(it)
                }
            }
    }

    fun savePlayRecord(formId: Int, musicId: Int, progress:Long) {
        Maybe.create<Boolean> {
            try {
                val playerRecord:PlayerRecord = database.playerRecordDao().loadPlayerRecordById(formId, musicId)
                if (playerRecord == null) {
                    database.playerRecordDao()
                        .insertItem(PlayerRecord(formId = formId,
                            musicId =musicId,
                            progress = progress,
                            recordTime = System.currentTimeMillis()))
                } else {
                    database.playerRecordDao().updateItem(playerRecord.apply {
                        this.progress = progress
                        this.recordTime = System.currentTimeMillis()
                    })
                }
                val list = database.playerRecordDao().loadPlayerRecordByFormId(formId).drop(3)
                if (list.isNotEmpty()) {
                    for (item in list) {
                        database.playerRecordDao().deleteItem(item)
                    }
                }
                it.onSuccess(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            it.onSuccess(false)
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {

            }
    }

    fun loadMusic(musicId: String, callback :(Music) -> Unit) {
        Maybe.create<Music?> {
            it.onSuccess(database.musicDao().loadMusicById(musicId.toInt()))
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {
                it?.let {
                    callback(it)
                }
            }
    }
}