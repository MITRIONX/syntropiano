package de.syntrosoft.syntropiano.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.syntrosoft.syntropiano.data.db.AppDatabase
import de.syntrosoft.syntropiano.data.db.dao.*
import de.syntrosoft.syntropiano.data.importer.SongParser
import de.syntrosoft.syntropiano.domain.audio.NoteMatcher
import de.syntrosoft.syntropiano.domain.audio.PitchDetector
import de.syntrosoft.syntropiano.domain.audio.YinPitchDetector
import de.syntrosoft.syntropiano.domain.engine.AchievementEngine
import de.syntrosoft.syntropiano.domain.engine.ScoreCalculator
import de.syntrosoft.syntropiano.domain.engine.XpCalculator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "syntropiano.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSongDao(db: AppDatabase): SongDao = db.songDao()
    @Provides fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()
    @Provides fun providePlayerProfileDao(db: AppDatabase): PlayerProfileDao = db.playerProfileDao()
    @Provides fun provideAchievementDao(db: AppDatabase): AchievementDao = db.achievementDao()

    @Provides @Singleton fun provideSongParser(): SongParser = SongParser()
    @Provides @Singleton fun provideScoreCalculator(): ScoreCalculator = ScoreCalculator()
    @Provides @Singleton fun provideXpCalculator(): XpCalculator = XpCalculator()
    @Provides @Singleton fun provideAchievementEngine(): AchievementEngine = AchievementEngine()
    @Provides @Singleton fun provideNoteMatcher(): NoteMatcher = NoteMatcher()
    @Provides @Singleton fun providePitchDetector(): PitchDetector = YinPitchDetector()

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context
}
