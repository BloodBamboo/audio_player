package cn.com.bamboo.easy_audio_player.view

import android.Manifest
import android.os.Bundle
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.util.loadFragment
import cn.com.bamboo.easy_common.help.Permission4MultipleHelp
import cn.com.edu.hnzikao.kotlin.base.BaseKotlinActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.jetbrains.anko.toast


class MainActivity : BaseKotlinActivity() {
    val musicFormFragment = MusicFormFragment()
    val playFragment = PlayerFragment()
    val playerRecordFragment = PlayerRecordFragment()
    lateinit var designNavigationView: BottomNavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_main)
        designNavigationView = findViewById(R.id.design_navigation_view)
        //权限申请
        Permission4MultipleHelp.request(this, arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ), success = {
        }, fail = { toast("请开启读写权限") })
        designNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_player -> {
                    this@MainActivity.loadFragment(playFragment, R.id.layout_fragment)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_record -> {
                    this@MainActivity.loadFragment(playerRecordFragment, R.id.layout_fragment)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_music_form -> {
                    this@MainActivity.loadFragment(musicFormFragment, R.id.layout_fragment)
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
        }
        loadFragment(playFragment, R.id.layout_fragment)
    }
}
