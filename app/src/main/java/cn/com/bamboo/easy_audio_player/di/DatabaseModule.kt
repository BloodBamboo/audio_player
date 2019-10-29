package cn.com.bamboo.easy_audio_player.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import cn.com.bamboo.easy_audio_player.db.MusicDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import androidx.sqlite.db.SupportSQLiteDatabase



@Module
class DatabaseModule {

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE player_record ADD COLUMN record_time INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    @Singleton
    @Provides
    fun provideMusicDatabase(application: Application): MusicDatabase {
        return Room.databaseBuilder(application, MusicDatabase::class.java, "musicDb.db")
            .fallbackToDestructiveMigration()
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}