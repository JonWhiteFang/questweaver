# ai:gateway

**Remote LLM API client for rich narration**

## Purpose

The `ai:gateway` module provides optional integration with remote Large Language Model (LLM) APIs for rich narrative generation, NPC dialogue, and scene descriptions. It uses Retrofit for HTTP communication and includes caching, timeouts, and fallback to template-based narration.

## Responsibilities

- Call remote LLM APIs for narrative generation
- Generate NPC dialogue and responses
- Create scene descriptions and flavor text
- Cache responses to reduce API calls
- Handle timeouts and failures gracefully
- Fallback to template-based narration
- Manage API keys and authentication

## Key Classes and Interfaces

### API Client (Placeholder)

- `LLMGateway`: Main entry point for LLM calls
- `LLMApiService`: Retrofit interface for API calls
- `NarrationGenerator`: Generates narrative text
- `DialogueGenerator`: Generates NPC dialogue

### DTOs (Placeholder)

- `LLMRequest`: Request payload for LLM API
- `LLMResponse`: Response from LLM API
- `NarrationContext`: Context for narrative generation

### Caching (Placeholder)

- `NarrationCache`: LRU cache for responses
- `CacheKey`: Hash of context + action for cache lookup

### Fallback (Placeholder)

- `TemplateNarrator`: Template-based narration fallback
- `NarrationTemplate`: Predefined narrative templates

## Dependencies

### Production

- `core:domain`: Domain entities and events
- `retrofit`: HTTP client
- `retrofit-kotlinx-serialization`: Serialization converter
- `okhttp`: HTTP client
- `okhttp-logging`: Logging interceptor
- `kotlinx-serialization-json`: JSON serialization
- `kotlinx-coroutines-android`: Coroutines for Android

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `mockk`: Mocking library
- `okhttp-mockwebserver`: Mock HTTP server for testing

## Module Rules

### ✅ Allowed

- Retrofit HTTP client
- Remote API calls
- Response caching
- Dependencies on `core:domain`

### ❌ Forbidden

- Dependencies on other feature modules
- Business logic (belongs in `core:domain` or `core:rules`)
- Direct database access (use repositories)
- On-device AI (use `ai:ondevice`)

## Architecture Patterns

### LLM Gateway

```kotlin
class LLMGateway(
    private val apiService: LLMApiService,
    private val cache: NarrationCache,
    private val templateNarrator: TemplateNarrator
) {
    suspend fun generateNarration(
        context: NarrationContext,
        action: GameEvent
    ): String = withContext(Dispatchers.IO) {
        // Check cache first
        val cacheKey = CacheKey(context, action)
        cache.get(cacheKey)?.let { return@withContext it }
        
        try {
            // Call LLM API with timeout
            val response = withTimeout(4000) {
                apiService.generateNarration(
                    LLMRequest(context, action)
                )
            }
            
            // Cache and return
            cache.put(cacheKey, response.text)
            response.text
        } catch (e: TimeoutCancellationException) {
            // Timeout - use template
            templateNarrator.generate(context, action)
        } catch (e: Exception) {
            // API error - use template
            logger.warn(e) { "LLM API failed, using template" }
            templateNarrator.generate(context, action)
        }
    }
}
```

### Retrofit Configuration

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .client(
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(apiKey))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    )
    .build()
```

### Template Fallback

```kotlin
class TemplateNarrator {
    private val attackTemplates = listOf(
        "{attacker} swings at {target} and {result}!",
        "{attacker} attacks {target} with {weapon}, {result}.",
        "With a mighty blow, {attacker} {result} against {target}!"
    )
    
    fun generate(context: NarrationContext, action: GameEvent): String {
        return when (action) {
            is GameEvent.AttackResolved -> {
                val template = attackTemplates.random()
                template
                    .replace("{attacker}", context.attacker.name)
                    .replace("{target}", context.target.name)
                    .replace("{result}", if (action.hit) "hits" else "misses")
            }
            else -> "Something happens."
        }
    }
}
```

## Testing Approach

### Unit Tests

- Test template generation
- Test cache behavior
- Test timeout handling
- Mock API responses

### Integration Tests

- Test with MockWebServer
- Test error scenarios
- Test fallback behavior

### Coverage Target

**70%+** code coverage

### Example Test

```kotlin
class LLMGatewayTest : FunSpec({
    test("returns cached response when available") {
        val cache = NarrationCache()
        val context = NarrationContext(...)
        val action = GameEvent.AttackResolved(...)
        val cached = "Cached narration"
        
        cache.put(CacheKey(context, action), cached)
        
        val gateway = LLMGateway(mockk(), cache, mockk())
        val result = gateway.generateNarration(context, action)
        
        result shouldBe cached
    }
    
    test("falls back to template on timeout") {
        val apiService = mockk<LLMApiService>()
        coEvery { apiService.generateNarration(any()) } coAnswers {
            delay(5000) // Simulate timeout
            LLMResponse("Never returned")
        }
        
        val templateNarrator = TemplateNarrator()
        val gateway = LLMGateway(apiService, NarrationCache(), templateNarrator)
        
        val result = gateway.generateNarration(context, action)
        
        result shouldNotBe "Never returned"
        result shouldContain "hits" // Template-based
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :ai:gateway:build

# Run tests
./gradlew :ai:gateway:test

# Run tests with coverage
./gradlew :ai:gateway:test koverHtmlReport
```

## Package Structure

```
dev.questweaver.ai.gateway/
├── api/
│   ├── LLMApiService.kt
│   └── AuthInterceptor.kt
├── dto/
│   ├── LLMRequest.kt
│   ├── LLMResponse.kt
│   └── NarrationContext.kt
├── cache/
│   ├── NarrationCache.kt
│   └── CacheKey.kt
├── fallback/
│   ├── TemplateNarrator.kt
│   └── NarrationTemplate.kt
└── di/
    └── GatewayModule.kt
```

## Integration Points

### Consumed By

- `app` (provides narration service)
- `feature:encounter` (generates combat narration)

### Depends On

- `core:domain` (entities and events)

## Performance Considerations

### Timeouts

- **Connect**: 2s
- **Read**: 8s (hard timeout)
- **Soft Timeout**: 4s (fallback to template)

### Caching Strategy

- **LRU Cache**: 100 entries
- **Cache Key**: Hash of context + action
- **Eviction**: Least recently used

### Performance Testing

```kotlin
test("narration completes within soft timeout") {
    val gateway = LLMGateway(apiService, cache, templateNarrator)
    
    val duration = measureTimeMillis {
        gateway.generateNarration(context, action)
    }
    
    duration shouldBeLessThan 4000
}
```

## API Configuration

### Environment Variables

```kotlin
object LLMConfig {
    val apiKey: String = BuildConfig.LLM_API_KEY
    val baseUrl: String = BuildConfig.LLM_BASE_URL
    val model: String = "gpt-4" // or other model
}
```

### Request Format

```json
{
  "model": "gpt-4",
  "messages": [
    {
      "role": "system",
      "content": "You are a D&D Dungeon Master narrating combat."
    },
    {
      "role": "user",
      "content": "The fighter attacks the goblin and hits for 8 damage."
    }
  ],
  "max_tokens": 100,
  "temperature": 0.7
}
```

## Error Handling

```kotlin
suspend fun generateNarration(context: NarrationContext, action: GameEvent): String {
    return try {
        // Try LLM API
        withTimeout(4000) {
            val response = apiService.generateNarration(LLMRequest(context, action))
            cache.put(CacheKey(context, action), response.text)
            response.text
        }
    } catch (e: TimeoutCancellationException) {
        // Soft timeout - use template
        logger.info { "LLM timeout, using template" }
        templateNarrator.generate(context, action)
    } catch (e: HttpException) {
        // HTTP error - use template
        logger.warn { "LLM API error: ${e.code()}" }
        templateNarrator.generate(context, action)
    } catch (e: Exception) {
        // Unknown error - use template
        logger.error(e) { "LLM generation failed" }
        templateNarrator.generate(context, action)
    }
}
```

## Security

- **API Keys**: Store in BuildConfig, not in code
- **TLS**: Enforce TLS 1.2+ for all API calls
- **Rate Limiting**: Respect API rate limits
- **PII**: Never send player PII to remote APIs

## Notes

- This module is OPTIONAL - game works without it
- Always provide template-based fallback
- Cache responses to reduce API costs
- Handle timeouts gracefully (4s soft, 8s hard)
- Never block gameplay waiting for LLM
- Test with MockWebServer for reliable tests

---

**Last Updated**: 2025-11-10
