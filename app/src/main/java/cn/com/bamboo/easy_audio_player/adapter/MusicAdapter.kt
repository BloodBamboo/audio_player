package cn.com.bamboo.easy_audio_player.adapter

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.vo.Music

class MusicAdapter(context: Context) :
    BasePagedListAdapter<Music, BasePagedListVieHolder<Music>>(
        context, R.layout.layout_music_item, differ
    ) {
    companion object {
        private val differ = object : DiffUtil.ItemCallback<Music>() {
            override fun areItemsTheSame(oldItem: Music, newItem: Music): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Music, newItem: Music): Boolean {
                return oldItem.name == newItem.name
            }
        }
    }

    override fun convert(holder: BasePagedListVieHolder<Music>, item: Music?, position: Int) {
        item?.let {
            holder.itemView.findViewById<TextView>(R.id.text_music_name).text = it.name
        }
    }
}