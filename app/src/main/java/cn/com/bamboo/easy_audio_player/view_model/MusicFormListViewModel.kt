package cn.com.bamboo.easy_audio_player.view_model

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.bamboo.easy_audio_player.vo.Music
import cn.com.bamboo.easy_common.util.ExecuteOnceMaybeObserver
import cn.com.bamboo.easy_common.util.RxJavaHelper
import cn.com.edu.hnzikao.kotlin.base.BaseViewModel
import io.reactivex.Maybe

class MusicFormListViewModel(application: Application) : BaseViewModel(application) {
    lateinit var musicList: LiveData<PagedList<Music>>
    val dao = getApplication<MusicApp>().database.musicDao()

    fun LoadDataByFormId(formId: Int) {
        musicList = LivePagedListBuilder(
            dao.loadMusicByFormId(formId),
            PagedList.Config.Builder().setPageSize(20).setEnablePlaceholders(true).build()
        ).build()
    }

    fun saveMusicPath(list: ArrayList<String>, formId: Int) {
        Maybe.create<String> {
            try {
                val musics = ArrayList<Music>()
                for (path in list) {
                    musics.add(
                        Music(
                            name = path.substringAfterLast("/"),
                            path = path,
                            formId = formId
                        )
                    )
                }
                dao.insertItems(musics)
                it.onSuccess("添加成功")
            } catch (e: Exception) {
                e.printStackTrace()
                it.onSuccess("添加失败")
            }
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {
                ExecuteOnceMaybeObserver<String>({
                    setMessage(it)
                })
            }
    }

    fun removeItem(item: Music) {
        Maybe.create<String> {
            try {
                dao.deleteItem(item)
                it.onSuccess("删除成功")
            } catch (e: Exception) {
                e.printStackTrace()
                it.onSuccess("删除失败")
            }
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe {
                ExecuteOnceMaybeObserver<String>({
                    setMessage(it)
                })
            }
    }
}