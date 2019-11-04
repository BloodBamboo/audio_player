package cn.com.bamboo.easy_audio_player.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import cn.com.bamboo.easy_audio_player.vo.PlayerRecord
import cn.com.bamboo.easy_audio_player.vo.PlayerRecordInfo

@Dao
interface PlayerRecordDao : BaseDao<PlayerRecord> {

    @Query(
        """select t1.id, t1.form_id, t1.music_id, t1.description, t1.progress, t1.record_time, t2.name as formName, t3.name as musicName 
        from player_record t1, music_form t2, music t3  
        where t1.form_id = :formId and t1.music_id = :musicId and t1.form_id = t2.id and t1.music_id = t3.id"""
    )
    fun loadPlayerRecordInfoById(formId: Int, musicId: Int): PlayerRecordInfo?

    @Query("select * from player_record where form_id = :formId and music_id = :musicId")
    fun loadPlayerRecordById(formId: Int, musicId: Int): PlayerRecord

    @Query(
        """select t1.id, t1.form_id, t1.music_id, t1.description, t1.progress, t1.record_time, t2.name as formName, t3.name as musicName
             from player_record t1, music_form t2, music t3  
             where t1.form_id = t2.id and t1.music_id = t3.id 
             ORDER BY t1.record_time DESC"""
    )
    fun loadPlayerRecordInfoAll(): DataSource.Factory<Int, PlayerRecordInfo>

    @Query(
        """select t1.id, t1.form_id, t1.music_id, t1.description, t1.progress, t1.record_time, t2.name as formName, t3.name as musicName
             from player_record t1, music_form t2, music t3  
             where t1.form_id = t2.id and t1.music_id = t3.id 
             ORDER BY t1.record_time DESC"""
    )
    fun loadPlayerRecordInfoAll2List(): List<PlayerRecordInfo>

    @Query("select * from player_record where form_id = :formId ORDER BY record_time DESC")
    fun loadPlayerRecordByFormId(formId: Int):List<PlayerRecord>
}