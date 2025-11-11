package dev.questweaver.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Database migration definitions for QuestWeaver.
 * 
 * This file contains all Room database migrations, ensuring safe schema evolution
 * while preserving data integrity. Each migration is versioned and includes
 * validation logic to verify successful execution.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */

private const val TAG = "QuestWeaverMigrations"

/**
 * Migration from database version 1 to version 2.
 * 
 * This is a placeholder migration for future schema changes. When implementing
 * a real migration, follow these guidelines:
 * 
 * 1. Use additive changes when possible (add columns, not modify)
 * 2. Provide default values for new columns
 * 3. Maintain backward compatibility for event deserialization
 * 4. Test migration with production-like data
 * 5. Log all migration steps for debugging
 * 
 * Example future migration (adding campaign_id):
 * ```
 * database.execSQL(
 *     "ALTER TABLE events ADD COLUMN campaign_id INTEGER NOT NULL DEFAULT 0"
 * )
 * database.execSQL(
 *     "CREATE INDEX index_events_campaign_id ON events(campaign_id)"
 * )
 * ```
 * 
 * Requirements: 5.1, 5.2, 5.4
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.i(TAG, "Starting migration from version 1 to 2")
        
        try {
            // Placeholder: No schema changes yet
            // Future migrations will add SQL statements here
            
            // Validate migration success
            validateMigration(database)
            
            Log.i(TAG, "Successfully completed migration from version 1 to 2")
        } catch (e: Exception) {
            Log.e(TAG, "Migration from version 1 to 2 failed", e)
            throw e
        }
    }
    
    /**
     * Validates that the migration completed successfully.
     * 
     * Checks database integrity and verifies expected schema changes.
     * 
     * Requirements: 5.3
     */
    private fun validateMigration(database: SupportSQLiteDatabase) {
        // Verify events table still exists
        val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='events'")
        val tableExists = cursor.use { it.count > 0 }
        
        if (!tableExists) {
            throw IllegalStateException("Migration validation failed: events table not found")
        }
        
        Log.d(TAG, "Migration validation passed: events table exists")
    }
}

/**
 * Array of all available migrations.
 * 
 * Add new migrations to this array as they are created. Room will automatically
 * apply migrations in sequence when the database version changes.
 * 
 * Example:
 * ```
 * val ALL_MIGRATIONS = arrayOf(
 *     MIGRATION_1_2,
 *     MIGRATION_2_3,
 *     MIGRATION_3_4
 * )
 * ```
 * 
 * Requirements: 5.2
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2
)

/**
 * Callback for database creation and opening events.
 * 
 * Provides hooks for logging and validation during database lifecycle events.
 * 
 * Requirements: 5.4, 5.5
 */
class MigrationCallback : androidx.room.RoomDatabase.Callback() {
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.i(TAG, "Database created with version ${db.version}")
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        Log.d(TAG, "Database opened with version ${db.version}")
        
        // Verify database integrity on open
        try {
            val cursor = db.query("PRAGMA integrity_check")
            cursor.use {
                if (it.moveToFirst()) {
                    val result = it.getString(0)
                    if (result != "ok") {
                        Log.e(TAG, "Database integrity check failed: $result")
                    } else {
                        Log.d(TAG, "Database integrity check passed")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform integrity check", e)
        }
    }
}

/**
 * Fallback behavior documentation for migration failures.
 * 
 * When a migration fails, Room's behavior depends on the configuration:
 * 
 * 1. **With fallbackToDestructiveMigration()** (current v1 configuration):
 *    - Database is dropped and recreated
 *    - All data is lost
 *    - Suitable for development only
 * 
 * 2. **Without fallback** (production configuration):
 *    - Migration failure throws IllegalStateException
 *    - App should catch exception and:
 *      a. Log error to crash reporting
 *      b. Attempt to export events to JSON backup
 *      c. Notify user of data loss risk
 *      d. Offer to restore from cloud backup if available
 * 
 * 3. **Best practices for production**:
 *    - Remove fallbackToDestructiveMigration() before release
 *    - Implement automatic backup before migration
 *    - Test migrations with production-like data
 *    - Provide manual recovery tools for users
 * 
 * Requirements: 5.3, 5.5
 */
object MigrationFallbackDocumentation {
    const val PRODUCTION_RECOMMENDATION = """
        For production builds, remove fallbackToDestructiveMigration() and implement:
        1. Pre-migration backup to JSON
        2. Migration failure recovery flow
        3. Cloud backup restoration option
        4. User notification of data loss risk
    """
}
