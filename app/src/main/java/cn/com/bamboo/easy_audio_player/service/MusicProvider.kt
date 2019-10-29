package cn.com.bamboo.easy_audio_player.service

import cn.com.bamboo.easy_audio_player.db.MusicDatabase
import cn.com.bamboo.easy_audio_player.vo.Music
import cn.com.bamboo.easy_audio_player.vo.MusicForm
import cn.com.bamboo.easy_common.util.RxJavaHelper
import io.reactivex.Maybe

class MusicProvider(var database: MusicDatabase) {


    fun getMusicFormList(list: (List<MusicForm>) -> Unit) {
        Maybe.create<List<MusicForm>> {
            it.onSuccess(database.musicFormDao().loadAllList())
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {
                list(it)
            }
    }

    fun getMusicList(formId: Int, list: (List<Music>) -> Unit) {
        Maybe.create<List<Music>> {
            it.onSuccess(database.musicDao().loadMusicListByFormId(formId))
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {
                list(it)
            }
    }
}