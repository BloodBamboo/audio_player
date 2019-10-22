package cn.com.bamboo.easy_audio_player.util

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun AppCompatActivity.loadFragment(fragment: Fragment, layoutId:Int) {
    val t = this.supportFragmentManager.beginTransaction()
    val fragments = supportFragmentManager.fragments
    if (!fragments.contains(fragment)) {
        t .add(layoutId, fragment)
    }
    for (f in fragments) {
        t.hide(f)
    }
    t.show(fragment)
    t .commitAllowingStateLoss()
}