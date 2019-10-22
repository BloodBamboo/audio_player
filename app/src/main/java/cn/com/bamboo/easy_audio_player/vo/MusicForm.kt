package cn.com.bamboo.easy_audio_player.vo

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_form")
data class MusicForm(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @NonNull
    var name: String,
    var used: Boolean = true,
    var description: String? = null//歌单描述
)