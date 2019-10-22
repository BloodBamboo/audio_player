package cn.com.bamboo.easy_audio_player.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import cn.com.bamboo.easy_audio_player.vo.PlayerRecord

@Dao
interface PlayerRecordDao : BaseDao<PlayerRecord> {

    @Query("select * from player_record where form_id = :formId and music_id = :musicId")
    fun loadPlayRecordById(formId: Int, musicId: Int): PlayerRecord

    @Query("select * from player_record")
    fun loadPlayRecordAll(): DataSource.Factory<Int, PlayerRecord>
}