package cn.com.bamboo.easy_audio_player

import android.app.Application
import cn.com.bamboo.easy_audio_player.db.MusicDatabase
import cn.com.bamboo.easy_audio_player.di.DaggerDatabaseComponent
import cn.com.bamboo.easy_audio_player.util.Constant
import cn.com.bamboo.easy_common.util.SharedPreferencesUtil
import com.facebook.stetho.Stetho
import javax.inject.Inject

class MusicApp : Application() {

//    var activitycount = 0

    var lockScreenVisible = false
    var isPlaying = false


    @Inject
    lateinit var database: MusicDatabase

    override fun onCreate() {
        super.onCreate()
        SharedPreferencesUtil.initInstance(this, Constant.SHARED_PREFERENCES)
        DaggerDatabaseComponent.builder().application(this).build().inject(this)
        if (BuildConfig.DEBUG) {
            //chrome://inspect/#devices
            Stetho.initializeWithDefaults(this)
        }
//
//        this.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
//            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
//
//            }
//
//            override fun onActivityStarted(activity: Activity) {
//                activitycount++
//            }
//
//            override fun onActivityResumed(activity: Activity) {
//
//            }
//
//            override fun onActivityPaused(activity: Activity) {
//
//            }
//
//            override fun onActivityStopped(activity: Activity) {
//                activitycount--
//                if (activitycount == 0) {
//                    Log.e("=====", "back_home")
//                }
//            }
//
//            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
//
//            }
//
//            override fun onActivityDestroyed(activity: Activity) {
//
//            }
//        })
    }

}