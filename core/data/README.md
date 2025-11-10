# core:data

**Data persistence layer with Room + SQLCipher encryption**

## Purpose

The `core:data` module handles all data persistence for QuestWeaver using Room database with SQLCipher encryption. It implements repository interfaces defined in `core:domain` and provides encrypted local storage for campaigns, events, and game state.

## Responsibilities

- Implement Room database with SQLCipher encryption
- Define Room entities and DAOs
- Implement repository interfaces from `core:domain`
- Handle data migrations
- Provide encrypted local storage
- Map between Room entities and domain entities

## Key Classes and Interfaces

### Database (Placeholder)

- `AppDatabase`: Main Room database class
- `EventDao`: DAO for game events
- `CampaignDao`: DAO for campaigns
- `CreatureDao`: DAO for creatures

### Entities (Placeholder)

- `EventEntity`: Room entity for game events
- `CampaignEntity`: Room entity for campaigns
- `CreatureEntity`: Room entity for creatures
- `EncounterEntity`: Room entity for encounters

### Repositories (Placeholder)

- `EventRepositoryImpl`: Implementation of EventRepository
- `CampaignRepositoryImpl`: Implementation of CampaignRepository
- `CreatureRepositoryImpl`: Implementation of CreatureRepository

### Mappers (Placeholder)

- `EventMapper`: Maps between EventEntity and GameEvent
- `CampaignMapper`: Maps between CampaignEntity and Campaign
- `CreatureMapper`: Maps between CreatureEntity and Creature

## Dependencies

### Production

- `core:domain`: Domain entities and repository interfaces
- `room-runtime`: Room database runtime
- `room-ktx`: Room Kotlin extensions
- `sqlcipher-android`: SQLCipher for encryption
- `kotlinx-coroutines-android`: Coroutines for Android

### Annotation Processing

- `room-compiler`: Room annotation processor (KSP)

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `mockk`: Mocking library
- `room-testing`: Room testing utilities

## Module Rules

### ✅ Allowed

- Room database and DAOs
- SQLCipher encryption
- Repository implementations
- Data mapping logic
- Android dependencies (this is an Android library)

### ❌ Forbidden

- Business logic (belongs in `core:domain`)
- UI code
- Direct database access from other modules (use repositories)

## Architecture Patterns

### Repository Pattern

Implement interfaces from `core:domain`:

```kotlin
class EventRepositoryImpl(
    private val dao: EventDao
) : EventRepository {
    override suspend fun append(event: GameEvent) {
        dao.insert(event.toEntity())
    }
    
    override fun observeSession(sessionId: Long): Flow<List<GameEvent>> {
        return dao.observeBySession(sessionId)
            .map { entities -> entities.map { it.toDomain() } }
    }
}
```

### Entity Mapping

Separate Room entities from domain entities:

```kotlin
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val type: String,
    val data: String // JSON serialized event data
)

fun GameEvent.toEntity(): EventEntity = EventEntity(
    sessionId = sessionId,
    timestamp = timestamp,
    type = this::class.simpleName ?: "Unknown",
    data = Json.encodeToString(this)
)
```

### Database Encryption

SQLCipher with Android Keystore:

```kotlin
val passphrase = keyStore.getKey("db_key", null).encoded
val factory = SupportFactory(passphrase)

Room.databaseBuilder(context, AppDatabase::class.java, "questweaver.db")
    .openHelperFactory(factory)
    .build()
```

## Testing Approach

### Integration Tests

- Test DAOs with in-memory database
- Test repository implementations
- Test data migrations
- Test entity mapping

### Coverage Target

**80%+** code coverage

### Example Test

```kotlin
class EventRepositoryImplTest : FunSpec({
    lateinit var database: AppDatabase
    lateinit var repository: EventRepository
    
    beforeTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = EventRepositoryImpl(database.eventDao())
    }
    
    afterTest {
        database.close()
    }
    
    test("append event persists to database") {
        val event = GameEvent.AttackResolved(
            sessionId = 1,
            timestamp = System.currentTimeMillis(),
            attackerId = 1,
            targetId = 2,
            roll = 15,
            hit = true,
            damage = 8
        )
        
        repository.append(event)
        
        val events = repository.observeSession(1).first()
        events shouldHaveSize 1
        events.first() shouldBe event
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :core:data:build

# Run tests
./gradlew :core:data:test

# Run tests with coverage
./gradlew :core:data:test koverHtmlReport
```

## Database Schema

### Tables (Placeholder)

- `events`: Game event log (event sourcing)
- `campaigns`: Campaign metadata
- `creatures`: Creatures and party members
- `encounters`: Encounter instances
- `map_grids`: Tactical map data

### Indexes

- `events.sessionId`: Query events by session
- `creatures.campaignId`: Query creatures by campaign
- `encounters.campaignId`: Query encounters by campaign

## Package Structure

```
dev.questweaver.data/
├── db/
│   ├── AppDatabase.kt
│   ├── entities/       # Room entities
│   └── daos/          # Data Access Objects
├── repositories/      # Repository implementations
├── mappers/          # Entity <-> Domain mapping
└── di/               # Koin module
```

## Integration Points

### Consumed By

- `app` (provides database instance)
- `sync:firebase` (syncs data to cloud)

### Depends On

- `core:domain` (implements repository interfaces)

## Security

- **Encryption**: All data encrypted with SQLCipher
- **Key Management**: Database passphrase stored in Android Keystore
- **Biometric Lock**: (Future) Require biometric authentication to unlock database

## Migration Strategy

Room migrations for schema changes:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE creatures ADD COLUMN level INTEGER NOT NULL DEFAULT 1")
    }
}
```

## Performance Considerations

- **Indexes**: Add indexes on frequently queried columns
- **Transactions**: Use transactions for multi-row operations
- **Batch Operations**: Use batch inserts for event logging
- **Query Optimization**: Use Flow for reactive queries

## Notes

- Always use repository pattern - no direct DAO access from other modules
- Separate Room entities from domain entities for flexibility
- Use SQLCipher for all local data storage
- Test with in-memory database for fast tests
- Handle migrations carefully to avoid data loss

---

**Last Updated**: 2025-11-10
