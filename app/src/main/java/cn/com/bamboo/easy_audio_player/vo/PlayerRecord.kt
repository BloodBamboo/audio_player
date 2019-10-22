package cn.com.bamboo.easy_audio_player.vo

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "player_record", foreignKeys = [
        ForeignKey(
            entity = MusicForm::class,
            parentColumns = ["id"],
            childColumns = ["form_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Music::class,
            parentColumns = ["id"],
            childColumns = ["music_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["form_id", "music_id"]
)
data class PlayerRecord(
    @ColumnInfo(name = "form_id")
    @NonNull
    val formId: Int,
    @ColumnInfo(name = "music_id")
    @NonNull
    val musicId: Int,
    @NonNull
    val description: String,
    @NonNull
    val progress: Long
)