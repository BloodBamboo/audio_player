package cn.com.bamboo.easy_audio_player.adapter

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.vo.PlayerRecordInfo

class PlayerRecordAdapter(context: Context) :
    BasePagedListAdapter<PlayerRecordInfo, BasePagedListVieHolder<PlayerRecordInfo>>(
        context, R.layout.layout_player_record_item, differ
    ) {
    override fun convert(
        holder: BasePagedListVieHolder<PlayerRecordInfo>,
        item: PlayerRecordInfo?,
        position: Int
    ) {
        item?.let {
            holder.itemView.findViewById<TextView>(R.id.text_form_name).text = it.formName
            holder.itemView.findViewById<TextView>(R.id.text_music_name).text = it.musicName
            holder.itemView.findViewById<TextView>(R.id.text_music_progress).text =
                it.formatProgress(context)
            holder.itemView.findViewById<TextView>(R.id.text_music_record_time).text =
                it.formatRecordTime()
        }
    }

    companion object {
        private val differ = object : DiffUtil.ItemCallback<PlayerRecordInfo>() {
            override fun areItemsTheSame(
                oldItem: PlayerRecordInfo,
                newItem: PlayerRecordInfo
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: PlayerRecordInfo,
                newItem: PlayerRecordInfo
            ): Boolean {
                return oldItem.formId == newItem.formId
                        && oldItem.musicId == newItem.musicId
                        && oldItem.recordTime == newItem.recordTime
                        && oldItem.progress == newItem.progress
            }
        }
    }
}