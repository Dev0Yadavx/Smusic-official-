package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LikedSongEntity::class,
        RecentlyPlayedEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        SearchHistoryEntity::class,
        DownloadedSongEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SMusicDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun downloadedSongDao(): DownloadedSongDao

    companion object {
        @Volatile
        private var INSTANCE: SMusicDatabase? = null

        private fun safeAddColumn(db: SupportSQLiteDatabase, table: String, column: String, typeDef: String) {
            try {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $typeDef")
            } catch (_: Throwable) {
                // Column may already exist
            }
        }

        private fun migrateDatabase(db: SupportSQLiteDatabase) {
            // 1. liked_songs
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `liked_songs` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL DEFAULT '',
                    `albumId` TEXT NOT NULL DEFAULT '',
                    `artwork` TEXT NOT NULL DEFAULT '',
                    `duration` INTEGER NOT NULL DEFAULT 0,
                    `streamUrl` TEXT NOT NULL DEFAULT '',
                    `encryptedMediaUrl` TEXT NOT NULL DEFAULT '',
                    `mediaPreviewUrl` TEXT NOT NULL DEFAULT '',
                    `addedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            safeAddColumn(db, "liked_songs", "mediaPreviewUrl", "TEXT NOT NULL DEFAULT ''")
            safeAddColumn(db, "liked_songs", "encryptedMediaUrl", "TEXT NOT NULL DEFAULT ''")
            safeAddColumn(db, "liked_songs", "albumId", "TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_liked_songs_addedAt` ON `liked_songs` (`addedAt`)")

            // 2. recently_played
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recently_played` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `songId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL DEFAULT '',
                    `albumId` TEXT NOT NULL DEFAULT '',
                    `artwork` TEXT NOT NULL DEFAULT '',
                    `duration` INTEGER NOT NULL DEFAULT 0,
                    `streamUrl` TEXT NOT NULL DEFAULT '',
                    `encryptedMediaUrl` TEXT NOT NULL DEFAULT '',
                    `mediaPreviewUrl` TEXT NOT NULL DEFAULT '',
                    `playedAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            safeAddColumn(db, "recently_played", "mediaPreviewUrl", "TEXT NOT NULL DEFAULT ''")
            safeAddColumn(db, "recently_played", "encryptedMediaUrl", "TEXT NOT NULL DEFAULT ''")
            safeAddColumn(db, "recently_played", "albumId", "TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recently_played_songId` ON `recently_played` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recently_played_playedAt` ON `recently_played` (`playedAt`)")

            // 3. playlists
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlists` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT NOT NULL DEFAULT '',
                    `artwork` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            // 4. playlist_songs
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlist_songs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `playlistId` INTEGER NOT NULL,
                    `songId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL DEFAULT '',
                    `albumId` TEXT NOT NULL DEFAULT '',
                    `artwork` TEXT NOT NULL DEFAULT '',
                    `duration` INTEGER NOT NULL DEFAULT 0,
                    `streamUrl` TEXT NOT NULL DEFAULT '',
                    `encryptedMediaUrl` TEXT NOT NULL DEFAULT '',
                    `addedAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            safeAddColumn(db, "playlist_songs", "encryptedMediaUrl", "TEXT NOT NULL DEFAULT ''")
            safeAddColumn(db, "playlist_songs", "albumId", "TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_songId` ON `playlist_songs` (`songId`)")

            // 5. search_history
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `search_history` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `query` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_timestamp` ON `search_history` (`timestamp`)")

            // 6. downloaded_songs
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `downloaded_songs` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL DEFAULT '',
                    `albumId` TEXT NOT NULL DEFAULT '',
                    `artwork` TEXT NOT NULL DEFAULT '',
                    `localArtworkPath` TEXT NOT NULL DEFAULT '',
                    `duration` INTEGER NOT NULL DEFAULT 0,
                    `localFilePath` TEXT NOT NULL DEFAULT '',
                    `fileSize` INTEGER NOT NULL DEFAULT 0,
                    `downloadStatus` TEXT NOT NULL DEFAULT 'COMPLETED',
                    `progress` INTEGER NOT NULL DEFAULT 100,
                    `downloadedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            safeAddColumn(db, "downloaded_songs", "localArtworkPath", "TEXT NOT NULL DEFAULT ''")
            safeAddColumn(db, "downloaded_songs", "albumId", "TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloaded_songs_downloadedAt` ON `downloaded_songs` (`downloadedAt`)")
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateDatabase(db)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateDatabase(db)
            }
        }

        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateDatabase(db)
            }
        }

        fun getInstance(context: Context): SMusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SMusicDatabase::class.java,
                    "smusic.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
