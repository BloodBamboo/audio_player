package cn.com.bamboo.easy_audio_player.vo

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = MusicForm::class,
        parentColumns = ["id"],
        childColumns = ["form_id"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )]
)
data class Music(
    @PrimaryKey(autoGenerate = true)
    @NonNull
    var id: Int = 0,
    @NonNull
    var name: String,
    @NonNull
    var path: String,
    @ColumnInfo(name = "form_id")
    @NonNull
    var formId: Int
)