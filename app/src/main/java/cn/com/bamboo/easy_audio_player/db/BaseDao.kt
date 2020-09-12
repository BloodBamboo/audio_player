package cn.com.bamboo.easy_audio_player.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update


@Dao
open interface BaseDao<T> {
    //插入单条数据
    @Insert
    fun insertItem(t: T)

    //插入list数据
    @Insert
    fun insertItems(items: List<T>)

    //更新item
    @Update
    fun updateItem(t: T)

    //删除item
    @Delete
    fun deleteItem(t: T)
}