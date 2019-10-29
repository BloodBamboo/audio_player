package cn.com.bamboo.easy_audio_player.db

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.com.bamboo.easy_audio_player.vo.Music
import cn.com.bamboo.easy_audio_player.vo.MusicForm
import cn.com.bamboo.easy_audio_player.vo.PlayerRecord

@Database(
    entities = [
        Music::class,
        MusicForm::class,
        PlayerRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun musicFormDao(): MusicFormDao
    abstract fun playerRecordDao(): PlayerRecordDao
}