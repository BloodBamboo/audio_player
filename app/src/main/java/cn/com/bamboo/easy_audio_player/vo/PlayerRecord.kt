package cn.com.bamboo.easy_audio_player.vo

import android.content.Context
import androidx.annotation.NonNull
import androidx.room.*
import cn.com.bamboo.easy_audio_player.MusicApp
import cn.com.bamboo.easy_common.util.StringUtil
import java.text.SimpleDateFormat
import java.util.*

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
    var description: String = "",
    @NonNull
    var progress: Long,
    @ColumnInfo(name = "record_time")
    @NonNull
    var recordTime: Long
)

class PlayerRecordInfo {
    var id:Int = -1
    @ColumnInfo(name = "form_id")
    var formId: Int = -1
    @ColumnInfo(name = "music_id")
    var musicId: Int = -1
    var description: String = ""
    var progress: Long = 0
    @ColumnInfo(name = "record_time")
    var recordTime: Long = System.currentTimeMillis()
    var formName:String = ""
    var musicName:String = ""

    fun formatProgress(context: Context):String {
        return StringUtil.timestampToMSS(context,progress)
    }

    fun formatRecordTime():String{
        val  format= SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(Date(recordTime!!))
    }
}