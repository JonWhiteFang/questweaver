# Implementation Plan

- [x] 1. Set up core:data module structure and dependencies
  - Create `core/data/` module directory with Gradle build configuration
  - Add dependencies: Room 2.6.1, SQLCipher 4.5.5, kotlinx-serialization 1.6.3, Koin 3.5.6
  - Configure Kotlin serialization plugin in module build.gradle.kts
  - Create package structure: `db/`, `repositories/`, `security/`, `di/`
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. Implement encryption key management with Android Keystore
  - [x] 2.1 Create KeystoreManager class for secure key generation and retrieval
    - Implement `getOrCreateDatabaseKey()` method that checks for existing key
    - Implement `generateAndStoreKey()` using KeyGenerator with AES-256-GCM
    - Implement `retrieveKey()` to fetch existing key from Android Keystore
    - Configure KeyGenParameterSpec with hardware-backed security and no user authentication
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Define Room database entities and type converters





  - [x] 3.1 Create EventEntity Room entity class


    - Define entity with columns: id (auto-generated), session_id, timestamp, event_type, event_data
    - Add composite index on (session_id, timestamp) for query performance
    - Add single index on session_id for count/delete operations
    - Annotate with @Entity, @PrimaryKey, @ColumnInfo, @Index
    - _Requirements: 3.1, 3.5_
  
  - [x] 3.2 Create GameEventConverters type converter class


    - Implement @TypeConverter method to serialize GameEvent to JSON string
    - Implement @TypeConverter method to deserialize JSON string to GameEvent
    - Configure Json instance with ignoreUnknownKeys, encodeDefaults=false, classDiscriminator
    - Handle SerializationException with appropriate error messages
    - _Requirements: 3.4, 6.1, 6.2, 6.3, 6.5_

- [x] 4. Create EventDao interface with Room queries




  - Define @Insert method for single event with OnConflictStrategy.ABORT
  - Define @Insert method for batch events (insertAll) with transaction support
  - Define @Query method to retrieve events by session_id ordered by timestamp
  - Define @Query method with Flow return type for reactive observation
  - Define @Query methods for countBySession and deleteBySession
  - _Requirements: 3.2, 3.3_

- [ ] 5. Configure AppDatabase with SQLCipher encryption
  - [ ] 5.1 Create AppDatabase abstract class extending RoomDatabase
    - Annotate with @Database including EventEntity and version 1
    - Add @TypeConverters annotation for GameEventConverters
    - Define abstract method for eventDao()
    - Set exportSchema = true for migration tracking
    - _Requirements: 1.1, 1.2, 1.4_
  
  - [ ] 5.2 Implement database builder function with SQLCipher integration
    - Create buildDatabase() function accepting Context and passphrase ByteArray
    - Configure SupportFactory with encryption passphrase
    - Set journal mode to WRITE_AHEAD_LOGGING for concurrency
    - Configure fallbackToDestructiveMigration for v1 (remove for production)
    - _Requirements: 1.1, 1.5, 2.3_

- [ ] 6. Implement EventRepository with domain/entity mapping
  - [ ] 6.1 Create EventRepositoryImpl implementing EventRepository interface
    - Implement append() method to insert single GameEvent
    - Implement appendAll() method for batch insertion with transaction
    - Implement forSession() method to retrieve all events for a session
    - Implement observeSession() method returning Flow of GameEvents
    - Implement eventCount() method for session event counting
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  
  - [ ] 6.2 Add entity/domain mapping extension functions
    - Create GameEvent.toEntity() extension converting domain to EventEntity
    - Create EventEntity.toDomain() extension converting entity to GameEvent
    - Use kotlinx-serialization for JSON serialization/deserialization
    - Extract event type discriminator from GameEvent class name
    - _Requirements: 3.3, 3.4, 4.5_

- [ ] 7. Configure Koin dependency injection for data layer
  - Create DataModule.kt with Koin module definition
  - Provide KeystoreManager as singleton with androidContext()
  - Provide AppDatabase as singleton using buildDatabase() with encryption key
  - Provide EventDao from database instance
  - Provide EventRepository implementation as singleton
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 8. Add database migration support infrastructure
  - Define Migration object for version 1 to 2 (placeholder for future)
  - Configure Room database builder to include migration strategy
  - Add migration validation logic to verify database integrity post-migration
  - Implement logging for migration execution and errors
  - Document fallback behavior for migration failures
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 9. Write comprehensive tests for persistence layer
  - [ ] 9.1 Create EventRepositoryImplTest with in-memory database
    - Test event insertion and retrieval through repository
    - Test event ordering by timestamp is maintained
    - Test Flow observation emits updates on new events
    - Test batch insertion with multiple events
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  
  - [ ] 9.2 Create EventDaoTest with Room in-memory database
    - Test insert, query, and observe operations
    - Test composite index usage with EXPLAIN QUERY PLAN
    - Test transaction rollback on error
    - Test countBySession and deleteBySession operations
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  
  - [ ] 9.3 Create GameEventConvertersTest for serialization
    - Test serialization of all GameEvent subtypes
    - Test deserialization produces equivalent objects
    - Test unknown keys are ignored (forward compatibility)
    - Test polymorphic type discrimination with @SerialName
    - _Requirements: 8.5_
  
  - [ ] 9.4 Create KeystoreManagerTest with Robolectric
    - Test key generation on first access
    - Test key retrieval on subsequent access
    - Test key persistence across KeystoreManager instances
    - Mock Android Keystore for unit testing
    - _Requirements: 2.1, 2.2, 2.3_
