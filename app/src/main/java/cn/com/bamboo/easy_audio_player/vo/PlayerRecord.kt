package cn.com.bamboo.easy_audio_player.vo

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "player_record", foreignKeys = [
        ForeignKey(
            entity = MusicForm::class,
            parentColumns = ["id"],
            childColumns = ["form_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlayerRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "form_id")
    @NonNull
    val formId: Int,
    @ColumnInfo(name = "music_id")
    @NonNull
    val musicId: Int,
    @NonNull
    val description: String,
    @NonNull
    val progress: Int,
    @ColumnInfo(name = "record_time")
    @NonNull
    val recordTime: Long
)