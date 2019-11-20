package cn.com.bamboo.easy_audio_player.util

object IntentKey {
    val STOP_SEVER ="stop_sever"
    val SERVICE_NAME = "MusicService"
    val MEDIA_ID_ROOT = "MusicService"
    val FORM_ID = "formId"
    val FORM_NAME = "form_name"
    val MUSIC_INFO = "music_info"

    val LOAD_FORM_LIST = "load_form_list"
    val LOAD_MUSIC_LIST = "load_music_list"
    val LOAD_PLAYER_RECORD = "load_player_record"
    val LOAD_PLAY_RECORD = "load_play_record"//是否播放历史记录
    val QUEUE_TYPE = "queue_type" // 0.歌单 1 音乐 2.播放记录列表
    val PLAYER_RECORD_FORMID_INT = "player_record_formid_int"
    val PLAYER_RECORD_MUSICID_INT = "player_record_musicid_int"
    val PLAYER_RECORD_DESCRIPTION_STRING = "player_record_description_string"
    val PLAYER_RECORD_PROGRESS_LONG = "player_record_progress_long"
    val PLAYER_RECORD_RECORDTIME_LONG = "player_record_recordtime_long"
}