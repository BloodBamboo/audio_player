package cn.com.bamboo.easy_audio_player.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import cn.com.bamboo.easy_audio_player.vo.MusicForm

@Dao
interface MusicFormDao : BaseDao<MusicForm> {
    @Query("select * from music_form where id = :id")
    fun loadById(id: Int): MusicForm


    @Query("select * from music_form")
    fun loadAll(): DataSource.Factory<Int, MusicForm>

    @Query("select * from music_form")
    fun loadAllList(): List<MusicForm>
}