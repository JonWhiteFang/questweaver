# Design Document

## Overview

The Database & Persistence Layer implements event-sourced storage for QuestWeaver using Room with SQLCipher encryption. This design provides secure, offline-first persistence for all game state through immutable event logs. The architecture follows the repository pattern with clear separation between domain interfaces (in `core:domain`) and data implementations (in `core:data`).

Key design principles:
- **Event Sourcing**: All state mutations captured as immutable events
- **Encryption by Default**: SQLCipher with Android Keystore-managed keys
- **Offline-First**: No network dependencies for core persistence
- **Type Safety**: Room compile-time SQL verification and kotlinx-serialization for polymorphic events
- **Reactive Queries**: Flow-based observation for real-time UI updates

## Architecture

### Module Structure

```
core/data/
├── db/
│   ├── AppDatabase.kt              # Room database definition
│   ├── entities/
│   │   └── EventEntity.kt          # Room entity for events
│   ├── dao/
│   │   └── EventDao.kt             # Data access object
│   └── converters/
│       └── TypeConverters.kt       # Custom type converters
├── repositories/
│   └── EventRepositoryImpl.kt      # Repository implementation
├── security/
│   └── KeystoreManager.kt          # Encryption key management
└── di/
    └── DataModule.kt               # Koin DI configuration
```

### Dependency Flow

```
feature/* → EventRepository (interface in core:domain)
                ↓
         EventRepositoryImpl (in core:data)
                ↓
            EventDao (Room)
                ↓
         AppDatabase (Room + SQLCipher)
                ↓
         KeystoreManager (Android Keystore)
```

### Layer Responsibilities

**core:domain** (Pure Kotlin):
- `GameEvent` sealed interface hierarchy
- `EventRepository` interface
- Domain entities (Creature, Campaign, etc.)

**core:data** (Android):
- Room database configuration
- SQLCipher integration
- Repository implementations
- Encryption key management
- Type converters for serialization

## Components and Interfaces

### 1. AppDatabase (Room Database)

**Purpose**: Central database configuration with SQLCipher encryption

```kotlin
@Database(
    entities = [EventEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(GameEventConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    
    companion object {
        const val DATABASE_NAME = "questweaver.db"
    }
}
```

**Configuration**:
- Version: 1 (initial schema)
- Export schema: Enabled for migration tracking
- Encryption: SQLCipher with 256-bit AES
- Journal mode: WAL (Write-Ahead Logging) for better concurrency

**Database Builder**:
```kotlin
fun buildDatabase(context: Context, passphrase: ByteArray): AppDatabase {
    val factory = SupportFactory(passphrase)
    
    return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    )
    .openHelperFactory(factory)
    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
    .fallbackToDestructiveMigration() // v1 only - remove for production
    .build()
}
```

### 2. EventEntity (Room Entity)

**Purpose**: Database representation of GameEvent with serialization

```kotlin
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["session_id", "timestamp"]),
        Index(value = ["session_id"])
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "event_type")
    val eventType: String, // Discriminator for polymorphic deserialization
    
    @ColumnInfo(name = "event_data")
    val eventData: String // JSON-serialized GameEvent
)
```

**Design Decisions**:
- **Auto-generated ID**: Ensures unique primary key independent of event content
- **Composite Index**: `(session_id, timestamp)` for efficient session queries
- **Event Type Discriminator**: Enables polymorphic deserialization without parsing JSON
- **JSON Storage**: Flexible schema for event evolution

### 3. EventDao (Data Access Object)

**Purpose**: Type-safe database operations for events

```kotlin
@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: EventEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(events: List<EventEntity>): List<Long>
    
    @Query("SELECT * FROM events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: Long): List<EventEntity>
    
    @Query("SELECT * FROM events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: Long): Flow<List<EventEntity>>
    
    @Query("SELECT COUNT(*) FROM events WHERE session_id = :sessionId")
    suspend fun countBySession(sessionId: Long): Int
    
    @Query("DELETE FROM events WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: Long): Int
}
```

**Design Decisions**:
- **Conflict Strategy**: ABORT to detect duplicate events (should never happen)
- **Ordering**: Always by timestamp ASC for deterministic replay
- **Reactive Queries**: Flow-based observation for real-time updates
- **Batch Operations**: `insertAll` for transaction efficiency

### 4. KeystoreManager (Encryption Key Management)

**Purpose**: Secure generation and retrieval of database encryption keys

```kotlin
class KeystoreManager(private val context: Context) {
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    companion object {
        private const val KEY_ALIAS = "questweaver_db_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
    
    fun getOrCreateDatabaseKey(): ByteArray {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            retrieveKey()
        } else {
            generateAndStoreKey()
        }
    }
    
    private fun generateAndStoreKey(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setUserAuthenticationRequired(false) // Allow background access
        .build()
        
        keyGenerator.init(keyGenSpec)
        val secretKey = keyGenerator.generateKey()
        
        return secretKey.encoded
    }
    
    private fun retrieveKey(): ByteArray {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey.encoded
    }
}
```

**Design Decisions**:
- **Hardware-Backed**: Uses Android Keystore for hardware security module (HSM) support
- **AES-256-GCM**: Industry-standard authenticated encryption
- **No User Authentication**: Allows background sync without user interaction
- **Lazy Generation**: Key created on first database access

### 5. GameEventConverters (Type Converters)

**Purpose**: Serialize/deserialize polymorphic GameEvent types for Room storage

```kotlin
class GameEventConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        classDiscriminator = "type"
    }
    
    @TypeConverter
    fun fromGameEvent(event: GameEvent): String {
        return json.encodeToString(GameEvent.serializer(), event)
    }
    
    @TypeConverter
    fun toGameEvent(eventData: String): GameEvent {
        return json.decodeFromString(GameEvent.serializer(), eventData)
    }
}
```

**Design Decisions**:
- **kotlinx-serialization**: Type-safe polymorphic serialization
- **Class Discriminator**: `@SerialName` annotations on GameEvent subtypes
- **Ignore Unknown Keys**: Forward compatibility for event schema evolution
- **No Defaults**: Reduce JSON size by omitting default values

### 6. EventRepositoryImpl (Repository Implementation)

**Purpose**: Implement domain repository interface with Room backend

```kotlin
class EventRepositoryImpl(
    private val eventDao: EventDao
) : EventRepository {
    
    override suspend fun append(event: GameEvent) {
        val entity = event.toEntity()
        eventDao.insert(entity)
    }
    
    override suspend fun appendAll(events: List<GameEvent>) {
        val entities = events.map { it.toEntity() }
        eventDao.insertAll(entities)
    }
    
    override suspend fun forSession(sessionId: Long): List<GameEvent> {
        return eventDao.getBySession(sessionId).map { it.toDomain() }
    }
    
    override fun observeSession(sessionId: Long): Flow<List<GameEvent>> {
        return eventDao.observeBySession(sessionId)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun eventCount(sessionId: Long): Int {
        return eventDao.countBySession(sessionId)
    }
    
    private fun GameEvent.toEntity(): EventEntity {
        return EventEntity(
            sessionId = this.sessionId,
            timestamp = this.timestamp,
            eventType = this::class.simpleName ?: "Unknown",
            eventData = Json.encodeToString(GameEvent.serializer(), this)
        )
    }
    
    private fun EventEntity.toDomain(): GameEvent {
        return Json.decodeFromString(GameEvent.serializer(), this.eventData)
    }
}
```

**Design Decisions**:
- **Mapping Layer**: Separate entity/domain conversion for clean boundaries
- **Batch Support**: `appendAll` for transaction efficiency
- **Flow Transformation**: Map entities to domain objects reactively
- **Error Propagation**: Let Room exceptions bubble up (handled by use cases)

## Data Models

### EventEntity Schema

```sql
CREATE TABLE events (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    session_id INTEGER NOT NULL,
    timestamp INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    event_data TEXT NOT NULL
);

CREATE INDEX index_events_session_id_timestamp 
ON events(session_id, timestamp);

CREATE INDEX index_events_session_id 
ON events(session_id);
```

**Column Details**:
- `id`: Auto-incrementing primary key (internal use only)
- `session_id`: Foreign key to session/encounter (indexed)
- `timestamp`: Unix epoch milliseconds (indexed for ordering)
- `event_type`: Discriminator string (e.g., "AttackResolved")
- `event_data`: JSON-serialized GameEvent

**Index Strategy**:
- Composite `(session_id, timestamp)`: Optimizes session replay queries
- Single `session_id`: Optimizes count and delete operations

### GameEvent Serialization Format

Example serialized event:
```json
{
  "type": "AttackResolved",
  "sessionId": 12345,
  "timestamp": 1699564800000,
  "attackerId": 1,
  "targetId": 2,
  "roll": 18,
  "modifiers": 5,
  "hit": true,
  "damage": 12,
  "damageType": "slashing"
}
```

**Serialization Rules**:
- `@SerialName` on sealed subtypes for discriminator
- `@Serializable` on all GameEvent implementations
- Primitive types preferred (Long, Int, Boolean, String)
- Nested objects must also be `@Serializable`

## Error Handling

### Database Exceptions

**SQLiteException**: Database file corruption or disk full
- **Strategy**: Log error, notify user, offer to reset database
- **Recovery**: Export events to JSON backup before reset

**SQLiteDatabaseCorruptException**: Unrecoverable corruption
- **Strategy**: Attempt to salvage events via raw SQL, then recreate database
- **Recovery**: Restore from cloud backup if available

**IllegalStateException**: Database not initialized
- **Strategy**: Lazy initialization on first access
- **Recovery**: Create new database with encryption key

### Serialization Exceptions

**SerializationException**: Unknown event type or malformed JSON
- **Strategy**: Log error with event ID, skip event in replay
- **Recovery**: Manual inspection of event_data column

**JsonDecodingException**: Invalid JSON structure
- **Strategy**: Log error, mark event as corrupted
- **Recovery**: Attempt to parse with lenient JSON settings

### Encryption Exceptions

**KeyPermanentlyInvalidatedException**: Keystore key lost (device reset)
- **Strategy**: Generate new key, create new database
- **Recovery**: Restore from cloud backup if available

**KeyStoreException**: Keystore unavailable
- **Strategy**: Fallback to in-memory database (session only)
- **Recovery**: Retry on next app launch

### Error Handling Pattern

```kotlin
class EventRepositoryImpl(
    private val eventDao: EventDao,
    private val logger: Logger
) : EventRepository {
    
    override suspend fun append(event: GameEvent) {
        try {
            val entity = event.toEntity()
            eventDao.insert(entity)
        } catch (e: SQLiteException) {
            logger.error(e) { "Failed to append event: ${event::class.simpleName}" }
            throw PersistenceException("Failed to save event", e)
        } catch (e: SerializationException) {
            logger.error(e) { "Failed to serialize event: ${event::class.simpleName}" }
            throw PersistenceException("Failed to serialize event", e)
        }
    }
}
```

## Testing Strategy

### Unit Tests (kotest + MockK)

**EventRepositoryImplTest**:
- Verify event insertion and retrieval
- Verify event ordering by timestamp
- Verify Flow observation emits updates
- Verify batch insertion with transactions
- Mock EventDao for isolation

**KeystoreManagerTest**:
- Verify key generation on first access
- Verify key retrieval on subsequent access
- Verify key persistence across instances
- Use instrumented tests for Android Keystore

**TypeConvertersTest**:
- Verify serialization of all GameEvent subtypes
- Verify deserialization produces equivalent objects
- Verify unknown keys are ignored
- Verify polymorphic type discrimination

### Integration Tests (In-Memory Database)

**EventDaoTest**:
- Use `Room.inMemoryDatabaseBuilder()` for isolation
- Verify insert, query, and observe operations
- Verify index usage with EXPLAIN QUERY PLAN
- Verify transaction rollback on error

**End-to-End Persistence Test**:
- Create in-memory database with encryption
- Append events via repository
- Replay events and verify state reconstruction
- Verify Flow updates trigger UI recomposition

### Test Fixtures

```kotlin
object EventFixtures {
    fun mockAttackResolved(
        sessionId: Long = 1L,
        timestamp: Long = System.currentTimeMillis(),
        attackerId: Long = 1L,
        targetId: Long = 2L,
        hit: Boolean = true
    ) = GameEvent.AttackResolved(
        sessionId = sessionId,
        timestamp = timestamp,
        attackerId = attackerId,
        targetId = targetId,
        roll = 15,
        modifiers = 5,
        hit = hit,
        damage = if (hit) 12 else null,
        damageType = "slashing"
    )
}
```

### Coverage Targets

- **EventRepositoryImpl**: 85%+ (core persistence logic)
- **EventDao**: 90%+ (critical data access)
- **KeystoreManager**: 80%+ (encryption key management)
- **TypeConverters**: 95%+ (serialization correctness)

## Performance Considerations

### Query Optimization

**Index Usage**:
- Composite index `(session_id, timestamp)` for session replay
- Single index `session_id` for count/delete operations
- Verify with `EXPLAIN QUERY PLAN`

**Batch Operations**:
- Use `insertAll()` for multiple events (single transaction)
- Avoid N+1 queries with Flow observation

**WAL Mode**:
- Write-Ahead Logging for concurrent reads during writes
- Checkpoint on app background to reduce file size

### Memory Management

**Flow Observation**:
- Use `flowOn(Dispatchers.IO)` for database queries
- Cancel Flow collection when UI destroyed
- Avoid collecting in composables without lifecycle awareness

**Large Event Logs**:
- Paginate session queries if event count > 10,000
- Archive old sessions to separate database file
- Implement event log pruning for completed campaigns

### Encryption Overhead

**SQLCipher Performance**:
- ~5-15% overhead vs unencrypted SQLite
- Negligible for typical event insertion rates (<100/sec)
- Use `PRAGMA cipher_memory_security = OFF` for performance (if acceptable)

**Key Retrieval**:
- Cache key in memory after first retrieval
- Avoid repeated Keystore access (expensive)

## Security Considerations

### Encryption at Rest

**SQLCipher Configuration**:
- 256-bit AES encryption (FIPS 140-2 compliant)
- PBKDF2-HMAC-SHA512 key derivation (256,000 iterations)
- Random salt per database file

**Key Management**:
- Keys stored in Android Keystore (hardware-backed if available)
- Keys never written to disk or logs
- Keys cleared from memory after database close

### Data Integrity

**Transaction Guarantees**:
- ACID compliance via SQLite transactions
- Rollback on serialization errors
- Foreign key constraints (future: session table)

**Backup Security**:
- Cloud backups encrypted before upload
- Separate encryption key for cloud storage
- User authentication required for restore

### Attack Mitigation

**SQL Injection**: Room parameterized queries (compile-time safety)
**Data Exfiltration**: Encrypted database file unreadable without key
**Key Extraction**: Android Keystore prevents key export
**Rooted Devices**: Detect root and warn user (optional)

## Migration Strategy

### Version 1 → Version 2 (Future)

Example migration adding a `campaign_id` column:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE events ADD COLUMN campaign_id INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "CREATE INDEX index_events_campaign_id ON events(campaign_id)"
        )
    }
}
```

**Migration Testing**:
- Create database with version 1 schema
- Insert test events
- Trigger migration to version 2
- Verify data integrity and new column

### Schema Evolution Best Practices

- **Additive Changes**: Prefer adding columns over modifying
- **Default Values**: Provide defaults for new columns
- **Backward Compatibility**: Support reading old event formats
- **Version Tracking**: Export schema to `schemas/` directory

## Dependency Injection (Koin)

### DataModule Configuration

```kotlin
val dataModule = module {
    // Encryption key management
    single { KeystoreManager(androidContext()) }
    
    // Database instance (singleton)
    single {
        val keystoreManager: KeystoreManager = get()
        val passphrase = keystoreManager.getOrCreateDatabaseKey()
        buildDatabase(androidContext(), passphrase)
    }
    
    // DAOs
    single { get<AppDatabase>().eventDao() }
    
    // Repositories
    single<EventRepository> { EventRepositoryImpl(get()) }
}
```

**Lifecycle Management**:
- Database closed on app termination (via `onCleared()`)
- Keystore manager lifecycle matches application
- Repository instances shared across ViewModels

## Diagrams

### Database Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Application Layer                    │
│                  (ViewModels, Use Cases)                 │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   EventRepository (Interface)            │
│                     (core:domain)                        │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                EventRepositoryImpl                       │
│                  (core:data)                             │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                      EventDao                            │
│                   (Room Interface)                       │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    AppDatabase                           │
│              (Room + SQLCipher)                          │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                 Encrypted Database File                  │
│              (questweaver.db)                            │
└─────────────────────────────────────────────────────────┘
                         ▲
                         │
                  ┌──────┴──────┐
                  │             │
                  ▼             ▼
         ┌──────────────┐  ┌──────────────┐
         │   Keystore   │  │  SQLCipher   │
         │   Manager    │  │   Library    │
         └──────────────┘  └──────────────┘
```

### Event Persistence Flow

```
┌──────────────┐
│  Use Case    │
│ (Domain)     │
└──────┬───────┘
       │ GameEvent
       ▼
┌──────────────────┐
│ EventRepository  │
│ Impl (Data)      │
└──────┬───────────┘
       │ toEntity()
       ▼
┌──────────────────┐
│  EventEntity     │
│  (Room)          │
└──────┬───────────┘
       │ serialize
       ▼
┌──────────────────┐
│  JSON String     │
│  (kotlinx)       │
└──────┬───────────┘
       │ insert
       ▼
┌──────────────────┐
│  EventDao        │
│  (Room)          │
└──────┬───────────┘
       │ encrypt
       ▼
┌──────────────────┐
│  SQLCipher       │
│  Database        │
└──────────────────┘
```

### Key Management Flow

```
┌──────────────────┐
│  App Launch      │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ KeystoreManager  │
│ .getOrCreateKey()│
└────────┬─────────┘
         │
    ┌────┴────┐
    │ Exists? │
    └────┬────┘
         │
    ┌────┴────┐
    │   Yes   │   No
    ▼         ▼
┌────────┐ ┌──────────┐
│Retrieve│ │ Generate │
│  Key   │ │   Key    │
└───┬────┘ └────┬─────┘
    │           │
    └─────┬─────┘
          │
          ▼
    ┌──────────┐
    │ Return   │
    │ ByteArray│
    └────┬─────┘
         │
         ▼
    ┌──────────┐
    │ Open DB  │
    │ with Key │
    └──────────┘
```

## Open Questions

1. **Event Pruning Strategy**: When should old events be archived or deleted?
   - **Recommendation**: Keep all events for active campaigns, archive completed campaigns to separate file

2. **Cloud Sync Conflict Resolution**: How to handle conflicting events from multiple devices?
   - **Recommendation**: Use vector clocks or last-write-wins with timestamp tiebreaker

3. **Database File Size Limits**: What's the maximum acceptable database size?
   - **Recommendation**: Monitor size, warn at 500MB, enforce pruning at 1GB

4. **Encryption Performance**: Is 256-bit AES acceptable for low-end devices?
   - **Recommendation**: Benchmark on target devices (API 26+), consider 128-bit fallback if needed

5. **Migration Failure Recovery**: What happens if migration fails mid-process?
   - **Recommendation**: Backup database before migration, restore on failure, log to crash reporting 