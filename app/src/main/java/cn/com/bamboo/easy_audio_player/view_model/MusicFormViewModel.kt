package cn.com.bamboo.easy_audio_player.view_model

import android.app.Application
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.bamboo.easy_audio_player.vo.MusicForm
import cn.com.bamboo.easy_common.util.ExecuteOnceMaybeObserver
import cn.com.bamboo.easy_common.util.RxJavaHelper
import cn.com.edu.hnzikao.kotlin.base.BaseViewModel
import io.reactivex.Maybe

class MusicFormViewModel(application: Application) : BaseViewModel(application) {
    var showFormDialog: MutableLiveData<Boolean> = MutableLiveData()
    var formResult: MutableLiveData<String> = MutableLiveData()
    val formList = LivePagedListBuilder(
        getApplication<MusicApp>().database.musicFormDao().loadAll(),
        PagedList.Config.Builder().setPageSize(20)
            .setEnablePlaceholders(true)
            .build()
    ).build()


    fun onCreateForm(view: View) {
        showFormDialog.value = true
    }

    fun createForm(name: String) {
        Maybe.create<String> {
            try {
                getApplication<MusicApp>().database.musicFormDao()
                    .insertItem(MusicForm(name = name, used = true))
            } catch (e: Exception) {
                it.onSuccess("创建失败" + e.message)
            }
            it.onSuccess("创建成功")
        }.compose(RxJavaHelper.schedulersTransformerMaybe())
            .subscribe(
                ExecuteOnceMaybeObserver<String>({
                    formResult.value = it
                })
            )
    }


}