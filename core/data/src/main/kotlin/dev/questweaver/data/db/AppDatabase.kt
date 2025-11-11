package dev.questweaver.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.questweaver.data.db.converters.GameEventConverters
import dev.questweaver.data.db.dao.EventDao
import dev.questweaver.data.db.entities.EventEntity
import dev.questweaver.data.db.migrations.ALL_MIGRATIONS
import dev.questweaver.data.db.migrations.MigrationCallback
import net.sqlcipher.database.SupportFactory

/**
 * Room database configuration with SQLCipher encryption for QuestWeaver.
 * 
 * This database implements event-sourced persistence with transparent 256-bit AES
 * encryption via SQLCipher. All game state mutations are captured as immutable
 * events stored in the EventEntity table, enabling full campaign replay and
 * deterministic state reconstruction.
 * 
 * Requirements: 1.1, 1.2, 1.4
 */
@Database(
    entities = [EventEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(GameEventConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to event persistence operations.
     * 
     * @return EventDao instance for database operations
     */
    abstract fun eventDao(): EventDao
    
    companion object {
        const val DATABASE_NAME = "questweaver.db"
    }
}

/**
 * Builds an encrypted AppDatabase instance with SQLCipher integration and migration support.
 * 
 * This function configures Room with:
 * - SQLCipher encryption using the provided passphrase
 * - Write-Ahead Logging (WAL) for better concurrency
 * - Migration strategy with all defined migrations
 * - Migration callback for logging and validation
 * - Fallback to destructive migration for v1 development (should be removed for production)
 * 
 * @param context Android application context
 * @param passphrase Encryption key as ByteArray (typically from Android Keystore)
 * @return Configured AppDatabase instance with encryption and migration support enabled
 * 
 * Requirements: 1.1, 1.5, 2.3, 5.1, 5.2, 5.3, 5.4, 5.5
 */
fun buildDatabase(context: Context, passphrase: ByteArray): AppDatabase {
    val factory = SupportFactory(passphrase)
    
    return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    )
    .openHelperFactory(factory)
    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    .addMigrations(*ALL_MIGRATIONS) // Add all defined migrations
    .addCallback(MigrationCallback()) // Add callback for logging and validation
    .fallbackToDestructiveMigration() // v1 only - remove for production
    .build()
}
