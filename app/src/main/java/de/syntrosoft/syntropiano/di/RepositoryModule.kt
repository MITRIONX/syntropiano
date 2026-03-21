package de.syntrosoft.syntropiano.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.syntrosoft.syntropiano.data.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository
}
