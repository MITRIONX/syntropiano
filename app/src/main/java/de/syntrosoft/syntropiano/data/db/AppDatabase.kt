package de.syntrosoft.syntropiano.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.syntrosoft.syntropiano.data.db.converter.Converters
import de.syntrosoft.syntropiano.data.db.dao.*
import de.syntrosoft.syntropiano.data.db.entity.*

@Database(
    entities = [
        SongEntity::class,
        ProgressEntity::class,
        PlayerProfileEntity::class,
        AchievementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun progressDao(): ProgressDao
    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun achievementDao(): AchievementDao
}
