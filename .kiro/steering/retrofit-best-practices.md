---
inclusion: fileMatch
fileMatchPattern: '**/*Api*.kt'
---

# Retrofit Best Practices for QuestWeaver

## Basic Setup with Kotlinx Serialization

```kotlin
val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.questweaver.example.com")
    .client(okHttpClient)
    .addConverterFactory(
        json.asConverterFactory("application/json".toMediaType())
    )
    .build()
```

**Important:** Add kotlinx-serialization converter **last** if mixing with other converters, as it assumes it can handle all types.

## API Interface Definition

```kotlin
interface AIGatewayApi {
    @POST("v1/narrate")
    suspend fun narrate(@Body request: NarrateRequest): NarrateResponse
    
    @POST("v1/intent")
    suspend fun parseIntent(@Body request: IntentRequest): IntentResponse
    
    @POST("v1/dialogue")
    suspend fun generateDialogue(@Body request: DialogueRequest): DialogueResponse
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

## Error Handling

### HTTP Errors
```kotlin
suspend fun narrate(request: NarrateRequest): Result<NarrateResponse> {
    return try {
        val response = api.narrate(request)
        Result.success(response)
    } catch (e: HttpException) {
        // HTTP error (4xx, 5xx)
        val errorBody = e.response()?.errorBody()?.string()
        logger.error { "HTTP ${e.code()}: $errorBody" }
        Result.failure(e)
    } catch (e: IOException) {
        // Network error (timeout, no connection)
        logger.error { "Network error: ${e.message}" }
        Result.failure(e)
    } catch (e: SerializationException) {
        // JSON parsing error
        logger.error { "Serialization error: ${e.message}" }
        Result.failure(e)
    }
}
```

### Custom Error Response
```kotlin
@Serializable
data class ErrorResponse(
    val error: String,
    val code: String,
    val details: Map<String, String>? = null
)

suspend fun narrateWithCustomError(request: NarrateRequest): Result<NarrateResponse> {
    return try {
        val response = api.narrate(request)
        Result.success(response)
    } catch (e: HttpException) {
        val errorBody = e.response()?.errorBody()?.string()
        val error = errorBody?.let { 
            json.decodeFromString<ErrorResponse>(it) 
        }
        logger.error { "API error: ${error?.error}" }
        Result.failure(ApiException(error))
    } catch (e: IOException) {
        Result.failure(e)
    }
}
```

## OkHttp Client Configuration

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(loggingInterceptor)
    .addInterceptor(authInterceptor)
    .addInterceptor(cacheInterceptor)
    .cache(cache)
    .build()
```

### Logging Interceptor
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
        val original = chain.request()
        val token = tokenProvider()
        
        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        
        return chain.proceed(request)
    }
}
```

### Cache Interceptor
```kotlin
class CacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Cache narration responses for 1 hour
        return if (request.url.encodedPath.contains("/narrate")) {
            response.newBuilder()
                .header("Cache-Control", "public, max-age=3600")
                .build()
        } else {
            response
        }
    }
}

val cache = Cache(
    directory = File(context.cacheDir, "http_cache"),
    maxSize = 10L * 1024L * 1024L // 10 MB
)
```

## Timeout Configuration

```kotlin
// Per-request timeout
interface AIGatewayApi {
    @POST("v1/narrate")
    @Headers("X-Timeout: 4000") // 4 second timeout
    suspend fun narrate(@Body request: NarrateRequest): NarrateResponse
}

// Dynamic timeout interceptor
class TimeoutInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val timeout = request.header("X-Timeout")?.toLongOrNull() ?: 30000L
        
        val newChain = chain.withConnectTimeout(timeout.toInt(), TimeUnit.MILLISECONDS)
            .withReadTimeout(timeout.toInt(), TimeUnit.MILLISECONDS)
        
        return newChain.proceed(request.newBuilder().removeHeader("X-Timeout").build())
    }
}
```

## Retry Logic

```kotlin
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelay: Long = 1000L
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response: Response? = null
        var exception: IOException? = null
        
        while (attempt < maxRetries) {
            try {
                response = chain.proceed(chain.request())
                
                // Retry on 5xx errors
                if (response.isSuccessful || response.code < 500) {
                    return response
                }
                
                response.close()
                attempt++
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelay * attempt) // Exponential backoff
                }
            } catch (e: IOException) {
                exception = e
                attempt++
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelay * attempt)
                }
            }
        }
        
        throw exception ?: IOException("Max retries exceeded")
    }
}
```

## Circuit Breaker Pattern

```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val resetTimeout: Long = 60_000L // 1 minute
) {
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var state = State.CLOSED
    
    enum class State { CLOSED, OPEN, HALF_OPEN }
    
    fun <T> execute(block: () -> T): T {
        when (state) {
            State.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeout) {
                    state = State.HALF_OPEN
                } else {
                    throw CircuitBreakerOpenException()
                }
            }
            State.HALF_OPEN -> {
                // Try one request
            }
            State.CLOSED -> {
                // Normal operation
            }
        }
        
        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }
    
    private fun onSuccess() {
        failureCount = 0
        state = State.CLOSED
    }
    
    private fun onFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        
        if (failureCount >= failureThreshold) {
            state = State.OPEN
        }
    }
}

// Usage
class AIGatewayRepository(private val api: AIGatewayApi) {
    private val circuitBreaker = CircuitBreaker()
    
    suspend fun narrate(request: NarrateRequest): Result<NarrateResponse> {
        return try {
            val response = circuitBreaker.execute {
                runBlocking { api.narrate(request) }
            }
            Result.success(response)
        } catch (e: CircuitBreakerOpenException) {
            logger.warn { "Circuit breaker open, using fallback" }
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Response Caching

```kotlin
class NarrationCache(private val maxSize: Int = 1000) {
    private val cache = LruCache<String, CachedNarration>(maxSize)
    
    data class CachedNarration(
        val text: String,
        val expiresAt: Long
    )
    
    fun get(key: String): String? {
        val cached = cache.get(key) ?: return null
        
        if (cached.expiresAt < System.currentTimeMillis()) {
            cache.remove(key)
            return null
        }
        
        return cached.text
    }
    
    fun put(key: String, text: String, ttlMs: Long = 3600_000) {
        cache.put(
            key,
            CachedNarration(text, System.currentTimeMillis() + ttlMs)
        )
    }
    
    fun generateKey(request: NarrateRequest): String {
        return "${request.context.hashCode()}_${request.action.hashCode()}"
    }
}

// Usage
suspend fun narrateWithCache(request: NarrateRequest): String {
    val cacheKey = narrationCache.generateKey(request)
    
    // Check cache first
    narrationCache.get(cacheKey)?.let { return it }
    
    // Fetch from API
    val response = api.narrate(request)
    
    // Cache response
    narrationCache.put(cacheKey, response.narration)
    
    return response.narration
}
```

## Rate Limiting

```kotlin
class RateLimiter(
    private val maxRequestsPerMinute: Int = 30
) {
    private val requests = mutableListOf<Long>()
    
    suspend fun acquire() {
        val now = System.currentTimeMillis()
        
        // Remove requests older than 1 minute
        requests.removeAll { it < now - 60_000 }
        
        if (requests.size >= maxRequestsPerMinute) {
            val oldestRequest = requests.first()
            val waitTime = 60_000 - (now - oldestRequest)
            delay(waitTime)
        }
        
        requests.add(now)
    }
}

// Usage
class AIGatewayRepository(
    private val api: AIGatewayApi,
    private val rateLimiter: RateLimiter
) {
    suspend fun narrate(request: NarrateRequest): NarrateResponse {
        rateLimiter.acquire()
        return api.narrate(request)
    }
}
```

## Koin Integration

```kotlin
val networkModule = module {
    single { provideJson() }
    single { provideOkHttpClient(get()) }
    single { provideRetrofit(get(), get()) }
    single { provideAIGatewayApi(get()) }
    single { AIGatewayRepository(get()) }
}

fun provideJson() = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun provideOkHttpClient(context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .cache(Cache(File(context.cacheDir, "http_cache"), 10L * 1024L * 1024L))
        .build()
}

fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
}

fun provideAIGatewayApi(retrofit: Retrofit): AIGatewayApi {
    return retrofit.create(AIGatewayApi::class.java)
}
```

## Testing

```kotlin
class AIGatewayApiTest : FunSpec({
    val mockWebServer = MockWebServer()
    val retrofit = Retrofit.Builder()
        .baseUrl(mockWebServer.url("/"))
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()
    val api = retrofit.create(AIGatewayApi::class.java)
    
    afterSpec {
        mockWebServer.shutdown()
    }
    
    test("narrate returns successful response") {
        val responseBody = """
            {
                "narration": "The goblin falls!",
                "cache_key": "abc123",
                "latency_ms": 1200
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )
        
        val request = NarrateRequest(
            context = mockContext,
            action = mockAction
        )
        
        val response = api.narrate(request)
        
        response.narration shouldBe "The goblin falls!"
        response.cacheKey shouldBe "abc123"
    }
    
    test("narrate handles 500 error") {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(500)
        )
        
        shouldThrow<HttpException> {
            api.narrate(mockRequest)
        }
    }
})
```

## QuestWeaver-Specific Patterns

### Fallback Strategy
```kotlin
suspend fun narrateWithFallback(request: NarrateRequest): String {
    return try {
        // Try on-device model first
        onDeviceNarrator.narrate(request)
    } catch (e: Exception) {
        try {
            // Try cached response
            narrationCache.get(request)
        } catch (e: Exception) {
            try {
                // Try remote API
                api.narrate(request).narration
            } catch (e: Exception) {
                // Fall back to template
                templateNarrator.narrate(request)
            }
        }
    }
}
```

### Batch Requests
```kotlin
@POST("v1/narrate/batch")
suspend fun narrateBatch(@Body requests: List<NarrateRequest>): List<NarrateResponse>

// Usage
suspend fun narrateMultiple(requests: List<NarrateRequest>): List<String> {
    return if (requests.size > 1) {
        api.narrateBatch(requests).map { it.narration }
    } else {
        listOf(api.narrate(requests.first()).narration)
    }
}
```
