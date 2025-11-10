---
inclusion: fileMatch
fileMatchPattern: ['**/di/**/*.kt', '**/*Module.kt', '**/Application.kt']
---

# Koin Dependency Injection Patterns

## Core Principles

1. **Module per layer** - Separate modules for domain, data, rules, UI, AI
2. **Pure Kotlin DSL** - No annotations, runtime resolution
3. **Verify modules** - Use `verify()` in tests to catch missing dependencies
4. **Scoped appropriately** - `single` for singletons, `factory` for new instances, `viewModel` for ViewModels

## Module Organization

```
app/
  di/
    AppModule.kt          # Application-level dependencies
core/domain/
  di/
    DomainModule.kt       # Use cases
core/data/
  di/
    DataModule.kt         # Repositories, DAOs, Database
core/rules/
  di/
    RulesModule.kt        # Rules engine, dice roller
feature/map/
  di/
    MapModule.kt          # Map ViewModels, renderers
feature/encounter/
  di/
    EncounterModule.kt    # Encounter ViewModels
ai/ondevice/
  di/
    OnDeviceModule.kt     # ONNX models, classifiers
ai/gateway/
  di/
    GatewayModule.kt      # Retrofit, API clients
```

## Domain Module (Use Cases)

```kotlin
// core/domain/di/DomainModule.kt
val domainModule = module {
    // Use cases - factory for new instance per injection
    factory { ProcessPlayerAction(get(), get()) }
    factory { RunCombatRound(get(), get()) }
    factory { MoveToken(get(), get()) }
    factory { StartEncounter(get(), get()) }
}
```

**Rules:**
- Use `factory` for use cases (stateless, new instance each time)
- Inject dependencies via constructor
- No Android dependencies in this module

## Data Module (Repositories, Database)

```kotlin
// core/data/di/DataModule.kt
val dataModule = module {
    // Database - single instance
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "questweaver.db"
        )
        .openHelperFactory(SupportFactory(get())) // SQLCipher
        .build()
    }
    
    // DAOs - single instance from database
    single { get<AppDatabase>().eventDao() }
    single { get<AppDatabase>().creatureDao() }
    single { get<AppDatabase>().campaignDao() }
    single { get<AppDatabase>().encounterDao() }
    
    // Repositories - single instance
    single<EventRepository> { EventRepositoryImpl(get()) }
    single<CreatureRepository> { CreatureRepositoryImpl(get()) }
    single<CampaignRepository> { CampaignRepositoryImpl(get()) }
    single<EncounterRepository> { EncounterRepositoryImpl(get()) }
    
    // SQLCipher passphrase from Android Keystore
    single { provideDbPassphrase(androidContext()) }
}

private fun provideDbPassphrase(context: Context): ByteArray {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val key = keyStore.getKey("db_key", null) as SecretKey
    return key.encoded
}
```

**Rules:**
- Use `single` for database, DAOs, repositories (stateful, shared)
- Repository interfaces in domain, implementations here
- Use `androidContext()` for Android dependencies

## Rules Module (Deterministic Engine)

```kotlin
// core/rules/di/RulesModule.kt
val rulesModule = module {
    // Rules engine - single instance (stateless but expensive to create)
    single { RulesEngine() }
    
    // Dice roller - factory with seed parameter
    factory { (seed: Long) -> DiceRoller(seed) }
    
    // Combat resolver - single instance
    single { CombatResolver(get()) }
    
    // Action validator - single instance
    single { ActionValidator(get()) }
}
```

**Rules:**
- Use `single` for stateless but expensive-to-create components
- Use `factory` with parameters for seeded components
- NO Android dependencies, NO AI dependencies

## Feature Module (ViewModels)

```kotlin
// feature/encounter/di/EncounterModule.kt
val encounterModule = module {
    // ViewModels - viewModel scope (tied to lifecycle)
    viewModel { EncounterViewModel(get(), get(), get()) }
    
    // UI-specific helpers - factory
    factory { InitiativeTracker() }
    factory { TurnEngine(get()) }
}
```

**Rules:**
- Use `viewModel` for ViewModels (lifecycle-aware)
- Use `factory` for UI helpers (new instance per screen)
- Can depend on `core:domain` and `core:rules` only

## AI Module (On-Device)

```kotlin
// ai/ondevice/di/OnDeviceModule.kt
val onDeviceModule = module {
    // ONNX environment - single instance (expensive)
    single { OrtEnvironment.getEnvironment() }
    
    // Model manager - single instance
    single { ModelManager(androidContext(), get()) }
    
    // Intent classifier - single instance (loads model once)
    single { IntentClassifier(get()) }
    
    // Tactical agent - single instance (stateless)
    single { TacticalAgent(get()) }
}
```

**Rules:**
- Use `single` for models (expensive to load)
- Warm up models on background thread in Application.onCreate()
- Use `androidContext()` for asset access

## AI Module (Gateway)

```kotlin
// ai/gateway/di/GatewayModule.kt
val gatewayModule = module {
    // JSON serializer - single instance
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = false
        }
    }
    
    // OkHttp client - single instance
    single { provideOkHttpClient(androidContext()) }
    
    // Retrofit - single instance
    single { provideRetrofit(get(), get()) }
    
    // API interface - single instance
    single { provideAIGatewayApi(get()) }
    
    // Repository - single instance
    single { AIGatewayRepository(get(), get()) }
    
    // Cache - single instance
    single { NarrationCache() }
}

private fun provideOkHttpClient(context: Context) = OkHttpClient.Builder()
    .callTimeout(8, TimeUnit.SECONDS)
    .connectTimeout(4, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .addInterceptor(loggingInterceptor)
    .cache(Cache(File(context.cacheDir, "http_cache"), 10L * 1024L * 1024L))
    .build()

private fun provideRetrofit(okHttpClient: OkHttpClient, json: Json) = Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()

private fun provideAIGatewayApi(retrofit: Retrofit): AIGatewayApi =
    retrofit.create(AIGatewayApi::class.java)
```

**Rules:**
- Use `single` for network components (expensive, stateful)
- Extract provider functions for complex setup
- Use `BuildConfig` for environment-specific URLs

## Application Setup

```kotlin
// app/src/main/kotlin/dev/questweaver/QuestWeaverApplication.kt
class QuestWeaverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.ERROR) // Only errors in production
            androidContext(this@QuestWeaverApplication)
            modules(
                domainModule,
                dataModule,
                rulesModule,
                mapModule,
                encounterModule,
                characterModule,
                onDeviceModule,
                gatewayModule,
                firebaseModule
            )
        }
        
        // Warm up AI models on background thread
        lifecycleScope.launch(Dispatchers.Default) {
            get<ModelManager>().warmUp()
        }
    }
}
```

**Rules:**
- Call `startKoin` in Application.onCreate()
- Use `androidLogger(Level.ERROR)` in production
- Warm up expensive resources on background thread

## Testing with Koin

### Unit Tests (Mock Dependencies)

```kotlin
class ProcessPlayerActionTest : FunSpec({
    val rulesEngine = mockk<RulesEngine>()
    val eventRepo = mockk<EventRepository>()
    
    beforeTest {
        startKoin {
            modules(module {
                single { rulesEngine }
                single { eventRepo }
                factory { ProcessPlayerAction(get(), get()) }
            })
        }
    }
    
    afterTest {
        stopKoin()
    }
    
    test("processes attack action successfully") {
        val useCase: ProcessPlayerAction = get()
        // Test implementation
    }
})
```

### Integration Tests (Real Dependencies)

```kotlin
class EncounterIntegrationTest : FunSpec({
    beforeTest {
        startKoin {
            modules(domainModule, dataModule, rulesModule)
        }
    }
    
    afterTest {
        stopKoin()
    }
    
    test("complete encounter flow") {
        val startEncounter: StartEncounter = get()
        val runRound: RunCombatRound = get()
        // Test implementation
    }
})
```

### Module Verification Tests

```kotlin
class ModuleVerificationTest : FunSpec({
    test("verify domain module") {
        koinApplication {
            modules(domainModule)
        }.verify()
    }
    
    test("verify data module") {
        koinApplication {
            modules(dataModule)
        }.verify(
            extraTypes = listOf(
                Context::class,
                AppDatabase::class
            )
        )
    }
    
    test("verify all modules together") {
        koinApplication {
            modules(
                domainModule,
                dataModule,
                rulesModule,
                encounterModule
            )
        }.verify(
            extraTypes = listOf(
                Context::class,
                AppDatabase::class,
                SavedStateHandle::class
            )
        )
    }
})
```

**Rules:**
- Always verify modules in tests
- Use `extraTypes` for Android/external dependencies
- Catch missing dependencies at test time, not runtime

## Parameterized Injection

```kotlin
// Define factory with parameters
val rulesModule = module {
    factory { (seed: Long) -> DiceRoller(seed) }
}

// Inject with parameters
class EncounterViewModel(
    private val sessionSeed: Long
) : ViewModel() {
    private val diceRoller: DiceRoller by inject { parametersOf(sessionSeed) }
}
```

## Lazy Injection

```kotlin
class EncounterViewModel : ViewModel() {
    // Lazy injection - only created when first accessed
    private val rulesEngine: RulesEngine by inject()
    private val eventRepo: EventRepository by inject()
    
    // Or use get() for immediate injection
    private val tacticalAgent: TacticalAgent = get()
}
```

## Scoping Summary

| Scope | Use Case | Example |
|-------|----------|---------|
| `single` | Shared instance, stateful or expensive | Database, Repository, RulesEngine, OkHttpClient |
| `factory` | New instance each time, stateless | Use cases, UI helpers |
| `viewModel` | Lifecycle-aware, tied to screen | ViewModels |
| `factory { params }` | Parameterized creation | DiceRoller with seed |

## Common Mistakes to Avoid

```kotlin
// ❌ WRONG: Creating dependencies manually
class EncounterViewModel : ViewModel() {
    private val rulesEngine = RulesEngine() // Don't do this
}

// ✅ CORRECT: Inject dependencies
class EncounterViewModel(
    private val rulesEngine: RulesEngine
) : ViewModel()

// ❌ WRONG: Using single for stateful per-request objects
single { ProcessPlayerAction(get(), get()) }

// ✅ CORRECT: Use factory for stateless use cases
factory { ProcessPlayerAction(get(), get()) }

// ❌ WRONG: Circular dependencies
single { A(get()) } // depends on B
single { B(get()) } // depends on A

// ✅ CORRECT: Extract shared dependency
single { SharedDep() }
single { A(get()) }
single { B(get()) }
```

## Quick Reference

**Module Definition:**
```kotlin
val myModule = module {
    single { /* singleton */ }
    factory { /* new instance */ }
    viewModel { /* ViewModel */ }
    factory { params -> /* parameterized */ }
}
```

**Injection:**
```kotlin
// Constructor injection (preferred)
class MyClass(private val dep: Dependency)

// Property injection
private val dep: Dependency by inject()

// Immediate get
private val dep: Dependency = get()

// Parameterized
private val dep: Dependency by inject { parametersOf(param) }
```

**Testing:**
```kotlin
beforeTest { startKoin { modules(testModule) } }
afterTest { stopKoin() }
koinApplication { modules(module) }.verify()
```

## Key Rules Summary

1. **One module per layer** - domain, data, rules, features, AI
2. **Use `single` for expensive/stateful** - database, repositories, models
3. **Use `factory` for cheap/stateless** - use cases, helpers
4. **Use `viewModel` for ViewModels** - lifecycle-aware
5. **Constructor injection preferred** - explicit dependencies
6. **Verify modules in tests** - catch missing deps early
7. **NO circular dependencies** - extract shared deps
8. **Warm up expensive resources** - models, database on background thread
9. **Use `androidContext()`** - for Android dependencies
10. **Clean up in tests** - `stopKoin()` in `afterTest`
