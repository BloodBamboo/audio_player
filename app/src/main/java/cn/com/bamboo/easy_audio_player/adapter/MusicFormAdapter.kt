package cn.com.bamboo.easy_audio_player.adapter

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.vo.MusicForm


/**
 * 歌单adapter
 */
class MusicFormAdapter(context: Context) :
    BasePagedListAdapter<MusicForm, BasePagedListVieHolder<MusicForm>>(
        context, R.layout.layout_music_form_item, differ
    ) {
    override fun convert(
        holder: BasePagedListVieHolder<MusicForm>,
        item: MusicForm?,
        position: Int
    ) {
        item?.let {
            holder.itemView.findViewById<TextView>(R.id.text_music_form).text = it.name
        }
    }

    companion object {
        private val differ = object : DiffUtil.ItemCallback<MusicForm>() {
            override fun areItemsTheSame(oldItem: MusicForm, newItem: MusicForm): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MusicForm, newItem: MusicForm): Boolean {
                return oldItem.name == newItem.name
            }
        }
    }
}