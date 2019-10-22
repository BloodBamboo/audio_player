package cn.com.bamboo.easy_audio_player.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import cn.com.bamboo.easy_audio_player.vo.Music

@Dao
interface MusicDao : BaseDao<Music> {

    @Query("select * from Music where id = :id")
    fun loadMusicById(id: Int): Music


    @Query("select * from Music where form_id = :formId")
    fun loadMusicByFormId(formId: Int): DataSource.Factory<Int, Music>
}