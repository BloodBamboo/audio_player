package cn.com.bamboo.easy_audio_player.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import cn.com.bamboo.easy_audio_player.MusicApp

/**
 *create by yx
 *on 2020/5/31
 * 锁屏监听
 */
class LockScreenReceiver : BroadcastReceiver() {
    private var mWakeLock: PowerManager.WakeLock? = null


    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            Log.e(
                "===",
                "lockScreenVisible === " + intent.action
            )
            if (intent.action == Intent.ACTION_SCREEN_ON
            ) {
                releaseWakeLock()
            } else if (intent.action == Intent.ACTION_SCREEN_OFF
                && (context?.applicationContext as MusicApp).isPlaying
            ) {
                acquireWakeLock(context)
            } else {
                releaseWakeLock()
            }
        }
    }


    fun releaseWakeLock() {
        if (mWakeLock != null) {
            Log.e(
                "===",
                "mWakeLock!!.release()\n"
            )
            mWakeLock!!.release()
            mWakeLock = null
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    private fun acquireWakeLock(context: Context?) {
        if (context == null) {
            return
        }
        if (mWakeLock == null) {
            val pm: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PlaySrvice")
            if (mWakeLock != null) {
                mWakeLock!!.acquire()
                Log.e(
                    "===",
                    "lacquireWakeLock"
                )
            }
        } else {
            releaseWakeLock()
        }
    }
}