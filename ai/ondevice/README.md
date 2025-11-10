# ai:ondevice

**On-device AI inference with ONNX Runtime**

## Purpose

The `ai:ondevice` module provides on-device AI capabilities using ONNX Runtime for Android. It handles intent classification from natural language input, enabling players to describe actions in plain text that are then parsed into structured game actions.

## Responsibilities

- Load and manage ONNX models
- Parse natural language player input into structured intents
- Provide fallback keyword matching when model unavailable
- Warm up models on background thread
- Manage model lifecycle and memory
- Handle tokenization and preprocessing

## Key Classes and Interfaces

### Models (Placeholder)

- `IntentClassifier`: Classifies player text into game actions
- `OnnxSessionManager`: Manages ONNX Runtime sessions
- `ModelLoader`: Loads models from assets

### Intent Parsing (Placeholder)

- `IntentParser`: Main entry point for intent parsing
- `Tokenizer`: Tokenizes input text
- `KeywordMatcher`: Fallback keyword-based parsing

## Dependencies

### Production

- `core:domain`: Domain entities and action types
- `onnxruntime-android`: ONNX Runtime for Android
- `kotlinx-coroutines-android`: Coroutines for Android

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `mockk`: Mocking library

## Module Rules

### ✅ Allowed

- ONNX Runtime integration
- Model loading from assets
- Intent classification
- Dependencies on `core:domain`

### ❌ Forbidden

- Dependencies on other feature modules
- Business logic (belongs in `core:domain` or `core:rules`)
- Direct database access (use repositories)
- Network calls (use `ai:gateway` for remote AI)

## Architecture Patterns

### Intent Classification

```kotlin
class IntentClassifier(
    private val session: OrtSession,
    private val tokenizer: Tokenizer
) {
    suspend fun classify(text: String): NLAction? = withContext(Dispatchers.Default) {
        try {
            // Tokenize input
            val tokens = tokenizer.tokenize(text)
            
            // Run inference
            val output = session.run(tokens)
            
            // Parse output to NLAction
            parseOutput(output)
        } catch (e: Exception) {
            null // Fallback to keyword matching
        }
    }
}
```

### Model Loading

```kotlin
class ModelLoader(private val context: Context) {
    fun loadModel(modelPath: String): OrtSession {
        val modelBytes = context.assets.open(modelPath).use { it.readBytes() }
        val env = OrtEnvironment.getEnvironment()
        return env.createSession(modelBytes)
    }
}
```

### Fallback Strategy

```kotlin
class IntentParser(
    private val classifier: IntentClassifier,
    private val keywordMatcher: KeywordMatcher
) {
    suspend fun parse(text: String): NLAction {
        // Try ML model first
        val mlResult = classifier.classify(text)
        if (mlResult != null) return mlResult
        
        // Fallback to keyword matching
        return keywordMatcher.match(text)
    }
}
```

## Testing Approach

### Unit Tests

- Test keyword matching fallback
- Test tokenization
- Test model output parsing
- Mock ONNX Runtime for tests

### Integration Tests

- Test with real model (if available)
- Test fallback behavior
- Test error handling

### Coverage Target

**70%+** code coverage

### Example Test

```kotlin
class KeywordMatcherTest : FunSpec({
    test("matches attack keyword") {
        val matcher = KeywordMatcher()
        
        val action = matcher.match("attack the goblin")
        
        action shouldBeInstanceOf NLAction.Attack::class
    }
    
    test("matches move keyword") {
        val matcher = KeywordMatcher()
        
        val action = matcher.match("move to the door")
        
        action shouldBeInstanceOf NLAction.Move::class
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :ai:ondevice:build

# Run tests
./gradlew :ai:ondevice:test

# Run tests with coverage
./gradlew :ai:ondevice:test koverHtmlReport
```

## Model Management

### Model Location

Models should be placed in: `app/src/main/assets/models/`

Example:
- `intent.onnx` - Intent classification model (~80MB)
- `tokenizer.json` - Tokenizer configuration

### Model Initialization

```kotlin
class OnDeviceAIModule(private val context: Context) {
    private lateinit var intentClassifier: IntentClassifier
    
    fun initialize() {
        viewModelScope.launch(Dispatchers.IO) {
            val loader = ModelLoader(context)
            val session = loader.loadModel("models/intent.onnx")
            val tokenizer = Tokenizer.fromAssets(context, "models/tokenizer.json")
            intentClassifier = IntentClassifier(session, tokenizer)
        }
    }
}
```

## Package Structure

```
dev.questweaver.ai.ondevice/
├── models/
│   ├── IntentClassifier.kt
│   ├── OnnxSessionManager.kt
│   └── ModelLoader.kt
├── inference/
│   ├── IntentParser.kt
│   └── Tokenizer.kt
├── intent/
│   └── KeywordMatcher.kt
└── di/
    └── OnDeviceAIModule.kt
```

## Integration Points

### Consumed By

- `app` (provides intent parsing service)
- `feature:encounter` (parses combat actions)

### Depends On

- `core:domain` (NLAction types)

## Performance Considerations

### Target: ≤300ms for intent classification

**Strategies:**

1. **Warm Up**: Load model on app start, not first use
2. **Background Thread**: Run inference on Dispatchers.Default
3. **Model Optimization**: Use quantized models for smaller size
4. **Caching**: Cache recent classifications

### Performance Testing

```kotlin
test("intent classification completes within budget") {
    val classifier = IntentClassifier(session, tokenizer)
    
    val duration = measureTimeMillis {
        classifier.classify("attack the goblin")
    }
    
    duration shouldBeLessThan 300
}
```

## Fallback Strategy

### Keyword Matching

When model is unavailable or fails:

```kotlin
class KeywordMatcher {
    private val attackKeywords = setOf("attack", "hit", "strike", "slash")
    private val moveKeywords = setOf("move", "walk", "run", "go")
    private val spellKeywords = setOf("cast", "spell", "magic")
    
    fun match(text: String): NLAction {
        val lower = text.lowercase()
        return when {
            attackKeywords.any { it in lower } -> NLAction.Attack(...)
            moveKeywords.any { it in lower } -> NLAction.Move(...)
            spellKeywords.any { it in lower } -> NLAction.CastSpell(...)
            else -> NLAction.Unknown(text)
        }
    }
}
```

## Model Training (External)

Models are trained externally and exported to ONNX format:

1. Train intent classification model (PyTorch/TensorFlow)
2. Export to ONNX format
3. Optimize for mobile (quantization, pruning)
4. Test on Android device
5. Place in `app/src/main/assets/models/`

## Error Handling

```kotlin
suspend fun parseIntent(text: String): NLAction {
    return try {
        // Try ML model
        classifier.classify(text) ?: keywordMatcher.match(text)
    } catch (e: OrtException) {
        // ONNX Runtime error - use fallback
        logger.warn { "ONNX inference failed: ${e.message}" }
        keywordMatcher.match(text)
    } catch (e: Exception) {
        // Unknown error - use fallback
        logger.error(e) { "Intent parsing failed" }
        keywordMatcher.match(text)
    }
}
```

## Notes

- Always provide keyword matching fallback
- Warm up models on background thread, not main thread
- Models should be in `app/src/main/assets/models/`
- Handle ONNX Runtime errors gracefully
- Test with and without model available
- Keep model size reasonable (<100MB)

---

**Last Updated**: 2025-11-10
