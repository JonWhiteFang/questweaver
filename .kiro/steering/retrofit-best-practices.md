---
inclusion: fileMatch
fileMatchPattern: ['**/*Api*.kt', '**/*Repository*.kt', '**/gateway/**/*.kt']
---

# Retrofit & Network Best Practices

## Setup (ai/gateway module)

```kotlin
val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = false
}

val retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
```

**Rule:** Add kotlinx-serialization converter last if mixing converters.

## API Interface (suspend functions only)

```kotlin
interface AIGatewayApi {
    @POST("v1/narrate")
    suspend fun narrate(@Body request: NarrateRequest): NarrateResponse
    
    @POST("v1/intent")
    suspend fun parseIntent(@Body request: IntentRequest): IntentResponse
}

@Serializable
data class NarrateRequest(
    val context: NarrationContext,
    val action: ActionSummary,
    @SerialName("max_tokens") val maxTokens: Int = 150,
    val temperature: Float = 0.7
)

@Serializable
data class NarrateResponse(
    val narration: String,
    @SerialName("cache_key") val cacheKey: String,
    @SerialName("latency_ms") val latencyMs: Int
)
```

**Rules:**
- Use `suspend fun` for all API calls (coroutines-first)
- Use `@SerialName` for snake_case JSON fields
- DTOs in `ai/gateway/dto/` package

## Error Handling (Repository Pattern)

**Rule:** Wrap API calls in repositories, return sealed Result types

```kotlin
suspend fun narrate(request: NarrateRequest): NarrationResult {
    return try {
        val response = api.narrate(request)
        NarrationResult.Success(response.narration)
    } catch (e: HttpException) {
        logger.error { "HTTP ${e.code()}: ${e.message}" }
        NarrationResult.ApiError(e.code())
    } catch (e: IOException) {
        logger.error { "Network error: ${e.message}" }
        NarrationResult.NetworkError
    } catch (e: SerializationException) {
        logger.error { "Serialization error: ${e.message}" }
        NarrationResult.ParseError
    }
}

sealed interface NarrationResult {
    data class Success(val text: String) : NarrationResult
    data class ApiError(val code: Int) : NarrationResult
    object NetworkError : NarrationResult
    object ParseError : NarrationResult
}
```

**Never log PII** - log IDs and error codes only

## OkHttp Configuration

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)  // LLM calls can be slow
    .writeTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(loggingInterceptor)
    .addInterceptor(authInterceptor)
    .cache(cache)
    .build()
```

### Logging (DEBUG builds only)
```kotlin
val loggingInterceptor = HttpLoggingInterceptor { message ->
    logger.debug { message }
}.apply {
    level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
}
```

### Auth Interceptor
```kotlin
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()
        
        return if (token != null) {
            chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            )
        } else {
            chain.proceed(request)
        }
    }
}
```

### Cache (10MB)
```kotlin
val cache = Cache(
    directory = File(context.cacheDir, "http_cache"),
    maxSize = 10L * 1024L * 1024L
)
```

## Timeouts (QuestWeaver Requirements)

**Performance Budget:** 4s soft timeout, 8s hard timeout for LLM calls

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .callTimeout(8, TimeUnit.SECONDS)  // Hard timeout
    .connectTimeout(4, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .build()
```

**Rule:** Always provide fallback to template narration on timeout

## Response Caching (LRU)

**Rule:** Cache narration responses by context+action hash

```kotlin
class NarrationCache(maxSize: Int = 1000) {
    private val cache = LruCache<String, CachedNarration>(maxSize)
    
    data class CachedNarration(val text: String, val expiresAt: Long)
    
    fun get(key: String): String? {
        val cached = cache.get(key) ?: return null
        return if (cached.expiresAt > System.currentTimeMillis()) {
            cached.text
        } else {
            cache.remove(key)
            null
        }
    }
    
    fun put(key: String, text: String, ttlMs: Long = 3600_000) {
        cache.put(key, CachedNarration(text, System.currentTimeMillis() + ttlMs))
    }
    
    fun generateKey(request: NarrateRequest): String =
        "${request.context.hashCode()}_${request.action.hashCode()}"
}
```

## Koin DI Setup

```kotlin
val networkModule = module {
    single { provideJson() }
    single { provideOkHttpClient(androidContext()) }
    single { provideRetrofit(get(), get()) }
    single { provideAIGatewayApi(get()) }
    single { AIGatewayRepository(get(), get()) }
    single { NarrationCache() }
}

fun provideJson() = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = false
}

fun provideOkHttpClient(context: Context) = OkHttpClient.Builder()
    .callTimeout(8, TimeUnit.SECONDS)
    .connectTimeout(4, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .addInterceptor(loggingInterceptor)
    .cache(Cache(File(context.cacheDir, "http_cache"), 10L * 1024L * 1024L))
    .build()

fun provideRetrofit(okHttpClient: OkHttpClient, json: Json) = Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()

fun provideAIGatewayApi(retrofit: Retrofit): AIGatewayApi =
    retrofit.create(AIGatewayApi::class.java)
```

## Testing with MockWebServer

```kotlin
class AIGatewayApiTest : FunSpec({
    val mockWebServer = MockWebServer()
    val json = Json { ignoreUnknownKeys = true }
    val retrofit = Retrofit.Builder()
        .baseUrl(mockWebServer.url("/"))
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    val api = retrofit.create(AIGatewayApi::class.java)
    
    afterSpec { mockWebServer.shutdown() }
    
    test("narrate returns successful response") {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "narration": "The goblin falls!",
                        "cache_key": "abc123",
                        "latency_ms": 1200
                    }
                """.trimIndent())
        )
        
        val response = api.narrate(mockRequest)
        
        response.narration shouldBe "The goblin falls!"
        response.cacheKey shouldBe "abc123"
    }
    
    test("narrate handles 500 error") {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        
        shouldThrow<HttpException> {
            api.narrate(mockRequest)
        }
    }
})
```

## QuestWeaver Fallback Strategy

**Critical Rule:** AI is optional - always provide fallback to template narration

```kotlin
suspend fun narrateWithFallback(request: NarrateRequest): String {
    // 1. Try cache first (instant)
    narrationCache.get(request)?.let { return it }
    
    // 2. Try on-device model (fast, ~100ms)
    onDeviceNarrator.narrate(request)
        .onSuccess { return it.also { narrationCache.put(request, it) } }
    
    // 3. Try remote API (slow, 4-8s timeout)
    return try {
        api.narrate(request).narration
            .also { narrationCache.put(request, it) }
    } catch (e: Exception) {
        // 4. Fall back to template (always works)
        logger.warn { "All AI failed, using template: ${e.message}" }
        templateNarrator.narrate(request)
    }
}
```

**Performance Budget:** On-device ≤300ms, remote ≤8s hard timeout

## Key Rules Summary

1. **Use `suspend fun`** for all API calls (coroutines-first)
2. **Return sealed Result types** from repositories, not raw responses
3. **Never log PII** - log IDs and error codes only
4. **Cache narration responses** by context+action hash (LRU)
5. **Respect timeouts:** 4s soft, 8s hard for LLM calls
6. **Always provide fallback** to template narration
7. **Use `@SerialName`** for snake_case JSON fields
8. **DTOs in `ai/gateway/dto/`** package
9. **Test with MockWebServer** and kotest
10. **Offline-first:** Core gameplay must work without network
