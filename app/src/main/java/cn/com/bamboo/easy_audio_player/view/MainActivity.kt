package cn.com.bamboo.easy_audio_player.view

import android.Manifest
import android.os.Bundle
import cn.com.bamboo.easy_audio_player.BR
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_audio_player.databinding.ActivityMusicMainBinding
import cn.com.bamboo.easy_audio_player.view_model.MainViewModel
import cn.com.bamboo.easy_common.help.Permission4MultipleHelp
import cn.com.bamboo.easy_audio_player.util.loadFragment
import cn.com.edu.hnzikao.kotlin.base.BaseViewModelActivity
import org.jetbrains.anko.toast


class MainActivity : BaseViewModelActivity<ActivityMusicMainBinding, MainViewModel>() {
    val musicFormFragment = MusicFormFragment()
    val playFragment = PlayerFragment()

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initContentView(): Int {
        return R.layout.activity_music_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //权限申请
        Permission4MultipleHelp.request(this, arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ), success = {
            viewModel.initMedia(this)
        }, fail = { toast("请开启读写权限") })
        binding.designNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_player -> {
                    this@MainActivity.loadFragment(playFragment, R.id.layout_fragment)
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
