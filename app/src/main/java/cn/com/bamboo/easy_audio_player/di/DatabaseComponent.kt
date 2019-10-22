package cn.com.bamboo.easy_audio_player.di

import android.app.Application
import cn.com.bamboo.easy_audio_player.MusicApp
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        DatabaseModule::class
    ]
)
interface DatabaseComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): DatabaseComponent
    }
    fun inject(app: MusicApp)
}