package cn.com.bamboo.easy_audio_player.adapter

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.util.IntentKey
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

/**
 * 播放列表adapter，播放歌单也暂时使用
 */
class PlayerListAdapter :
    BaseQuickAdapter<MediaSessionCompat.QueueItem, BaseViewHolder>(
        R.layout.layout_music_item,
        null
    ) {

    var metadata: MediaMetadataCompat? = null

    override fun convert(helper: BaseViewHolder, item: MediaSessionCompat.QueueItem?) {
        helper.setText(R.id.text_music_name, item!!.description.title)
        metadata?.let {
            if (item!!.description.extras!!.get(IntentKey.QUEUE_TYPE) == 1 &&
                item!!.description.mediaId.equals(
                    it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                )
            ) {
                helper.setTextColor(
                    R.id.text_music_name,
                    mContext.resources.getColor(R.color.colorPrimary)
                )
            } else {
                helper.setTextColor(
                    R.id.text_music_name,
                    mContext.resources.getColor(R.color.text_primary)
                )
            }
        }
    }
}
