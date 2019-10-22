package cn.com.bamboo.easy_audio_player.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

open abstract class BasePagedListAdapter<T, VH : BasePagedListVieHolder<T>>(
    private val context: Context,
    val layoutId: Int,
    diff: DiffUtil.ItemCallback<T>
) : PagedListAdapter<T, VH>(diff) {

    var itemViewOnClick: ((item: T?, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return BasePagedListVieHolder<T>(
            LayoutInflater.from(context).inflate(
                layoutId,
                parent,
                false
            )
        ) as VH
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.itemView.setOnClickListener {
            itemViewOnClick?.run { this(getItem(position), position) }
        }
        convert(holder, getItem(position), position)
    }

    abstract fun convert(holder: VH, item: T?, position: Int)
}


class BasePagedListVieHolder<T>(itemView: View) :
    RecyclerView.ViewHolder(itemView)