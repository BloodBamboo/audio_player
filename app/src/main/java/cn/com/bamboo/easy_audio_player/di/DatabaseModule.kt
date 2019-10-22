package cn.com.bamboo.easy_audio_player.di

import android.app.Application
import androidx.room.Room
import cn.com.bamboo.easy_audio_player.db.MusicDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideMusicDatabase(application: Application): MusicDatabase {
        return Room.databaseBuilder(application, MusicDatabase::class.java, "musicDb.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}