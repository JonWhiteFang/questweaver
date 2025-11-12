# sync:firebase

**Optional cloud backup and sync via Firebase**

## Purpose

The `sync:firebase` module provides optional cloud backup and synchronization capabilities using Firebase Cloud Storage and WorkManager. It allows players to back up their campaigns, characters, and game state to the cloud and sync across devices.

## Responsibilities

- Backup campaigns and game state to Firebase Cloud Storage
- Sync data across multiple devices
- Handle conflict resolution for concurrent edits
- Schedule periodic backups with WorkManager
- Encrypt data before upload
- Restore from cloud backup
- Manage Firebase Authentication (opt-in)

## Key Classes and Interfaces

### Sync (Placeholder)

- `SyncManager`: Main entry point for sync operations
- `BackupWorker`: WorkManager worker for periodic backups
- `ConflictResolver`: Resolves sync conflicts using event sourcing

### Firebase (Placeholder)

- `FirebaseStorageClient`: Wrapper for Firebase Cloud Storage
- `FirebaseAuthManager`: Manages Firebase Authentication

### Encryption (Placeholder)

- `BackupEncryptor`: Encrypts data before upload
- `BackupDecryptor`: Decrypts data after download

## Dependencies

### Production

- `core:domain`: Domain entities and events
- `core:data`: Repository access for data to sync
- `firebase-storage`: Firebase Cloud Storage
- `firebase-auth`: Firebase Authentication
- `work-runtime-ktx`: WorkManager for background sync
- `kotlinx-coroutines-android`: Coroutines for Android

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `mockk`: Mocking library

## Module Rules

### ✅ Allowed

- Firebase integration
- WorkManager for background tasks
- Data encryption before upload
- Dependencies on `core:domain` and `core:data`

### ❌ Forbidden

- Dependencies on feature modules
- Business logic (belongs in `core:domain` or `core:rules`)
- Unencrypted cloud storage

## Architecture Patterns

### Sync Manager

```kotlin
class SyncManager(
    private val storageClient: FirebaseStorageClient,
    private val eventRepo: EventRepository,
    private val encryptor: BackupEncryptor
) {
    suspend fun backup(campaignId: Long) = withContext(Dispatchers.IO) {
        // Get all events for campaign
        val events = eventRepo.forCampaign(campaignId)
        
        // Serialize and encrypt
        val json = Json.encodeToString(events)
        val encrypted = encryptor.encrypt(json)
        
        // Upload to Firebase
        storageClient.upload("campaigns/$campaignId/backup.enc", encrypted)
    }
    
    suspend fun restore(campaignId: Long) = withContext(Dispatchers.IO) {
        // Download from Firebase
        val encrypted = storageClient.download("campaigns/$campaignId/backup.enc")
        
        // Decrypt and deserialize
        val json = encryptor.decrypt(encrypted)
        val events = Json.decodeFromString<List<GameEvent>>(json)
        
        // Replay events to restore state
        events.forEach { eventRepo.append(it) }
    }
}
```

### WorkManager Backup

```kotlin
class BackupWorker(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val campaignId = inputData.getLong("campaignId", -1)
            if (campaignId == -1L) return Result.failure()
            
            syncManager.backup(campaignId)
            Result.success()
        } catch (e: Exception) {
            logger.error(e) { "Backup failed" }
            Result.retry()
        }
    }
}

// Schedule periodic backup
fun schedulePeriodicBackup(campaignId: Long) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
        .setRequiresBatteryNotLow(true)
        .build()
    
    val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
        .setConstraints(constraints)
        .setInputData(workDataOf("campaignId" to campaignId))
        .build()
    
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "backup_$campaignId",
        ExistingPeriodicWorkPolicy.KEEP,
        backupRequest
    )
}
```

### Conflict Resolution

```kotlin
class ConflictResolver {
    fun resolve(local: List<GameEvent>, remote: List<GameEvent>): List<GameEvent> {
        // Event sourcing makes this straightforward:
        // Merge events by timestamp, keeping all unique events
        val merged = (local + remote)
            .distinctBy { it.timestamp to it.sessionId }
            .sortedBy { it.timestamp }
        
        return merged
    }
}
```

## Testing Approach

### Unit Tests

- Test encryption/decryption
- Test conflict resolution
- Test WorkManager scheduling
- Mock Firebase services

### Integration Tests

- Test backup and restore flow
- Test sync with conflicts
- Test WorkManager execution

### Coverage Target

**70%+** code coverage

### Example Test

```kotlin
class SyncManagerTest : FunSpec({
    test("backup encrypts and uploads data") {
        val storageClient = mockk<FirebaseStorageClient>()
        val eventRepo = mockk<EventRepository>()
        val encryptor = mockk<BackupEncryptor>()
        
        val events = listOf(GameEvent.EncounterStarted(...))
        coEvery { eventRepo.forCampaign(1) } returns events
        every { encryptor.encrypt(any()) } returns "encrypted"
        coEvery { storageClient.upload(any(), any()) } just Runs
        
        val syncManager = SyncManager(storageClient, eventRepo, encryptor)
        syncManager.backup(1)
        
        coVerify { storageClient.upload("campaigns/1/backup.enc", "encrypted") }
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :sync:firebase:build

# Run tests
./gradlew :sync:firebase:test

# Run tests with coverage
./gradlew :sync:firebase:test koverHtmlReport
```

## Package Structure

```
dev.questweaver.sync.firebase/
├── auth/
│   └── FirebaseAuthManager.kt
├── storage/
│   └── FirebaseStorageClient.kt
├── workers/
│   └── BackupWorker.kt
├── encryption/
│   ├── BackupEncryptor.kt
│   └── BackupDecryptor.kt
├── sync/
│   ├── SyncManager.kt
│   └── ConflictResolver.kt
└── di/
    └── SyncModule.kt
```

## Integration Points

### Consumed By

- `app` (provides sync service)

### Depends On

- `core:domain` (entities and events)
- `core:data` (repository access)

## Firebase Configuration

### Setup

1. Create Firebase project
2. Add `google-services.json` to `app/`
3. Enable Firebase Authentication
4. Enable Firebase Cloud Storage
5. Configure storage rules

### Storage Rules

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /campaigns/{campaignId}/{allPaths=**} {
      allow read, write: if request.auth != null 
        && request.auth.uid == resource.metadata.userId;
    }
  }
}
```

## Encryption

### Before Upload

```kotlin
class BackupEncryptor(private val keyStore: KeyStore) {
    fun encrypt(data: String): ByteArray {
        val key = keyStore.getKey("backup_key", null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data.toByteArray())
    }
}
```

### After Download

```kotlin
class BackupDecryptor(private val keyStore: KeyStore) {
    fun decrypt(data: ByteArray): String {
        val key = keyStore.getKey("backup_key", null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return String(cipher.doFinal(data))
    }
}
```

## Sync Strategy

### Event Sourcing Advantage

Event sourcing makes sync straightforward:
1. Upload all events for a campaign
2. Download events from cloud
3. Merge by timestamp (no conflicts on events)
4. Replay merged events to rebuild state

### Conflict Resolution

Since events are immutable and timestamped:
- No true conflicts (events don't overwrite)
- Merge by timestamp
- Replay all events in order
- State is deterministically derived

## Performance Considerations

- **WiFi Only**: Schedule backups on unmetered network
- **Battery**: Don't backup when battery is low
- **Compression**: Compress data before encryption
- **Incremental**: Only upload new events since last backup

## User Experience

### Opt-In

- Sync is OPTIONAL and opt-in
- Game works fully offline without sync
- Clear UI for enabling/disabling sync

### Backup Frequency

- Manual backup: Immediate
- Automatic backup: Daily (configurable)
- On campaign completion: Automatic

### Restore Flow

1. User signs in with Firebase Auth
2. App lists available backups
3. User selects backup to restore
4. App downloads and decrypts
5. App replays events to restore state

## Error Handling

```kotlin
suspend fun backup(campaignId: Long): BackupResult {
    return try {
        // Check network
        if (!isNetworkAvailable()) {
            return BackupResult.NoNetwork
        }
        
        // Check auth
        if (!isAuthenticated()) {
            return BackupResult.NotAuthenticated
        }
        
        // Perform backup
        val events = eventRepo.forCampaign(campaignId)
        val encrypted = encryptor.encrypt(Json.encodeToString(events))
        storageClient.upload("campaigns/$campaignId/backup.enc", encrypted)
        
        BackupResult.Success
    } catch (e: FirebaseException) {
        logger.error(e) { "Firebase backup failed" }
        BackupResult.FirebaseError(e.message)
    } catch (e: Exception) {
        logger.error(e) { "Backup failed" }
        BackupResult.UnknownError(e.message)
    }
}
```

## Security

- **Encryption**: Always encrypt before upload
- **Authentication**: Require Firebase Auth
- **Storage Rules**: User can only access their own data
- **Key Management**: Use Android Keystore for encryption keys

## Notes

- This module is OPTIONAL - game works without it
- Always encrypt data before uploading
- Use WorkManager for reliable background sync
- Event sourcing simplifies conflict resolution
- Test with Firebase Emulator for local development
- Respect user's network and battery preferences

---

**Last Updated**: 2025-11-10
