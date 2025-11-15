# Design Document

## Overview

The Intent Classification system provides on-device natural language understanding for QuestWeaver. It uses ONNX Runtime to run a lightweight intent classification model that converts player text input into structured game actions. The system is designed for offline operation, fast inference (≤300ms), and graceful degradation through keyword-based fallback.

**Key Design Principles:**
- **Offline-First**: All processing happens on-device with no network dependency
- **Fast Inference**: 300ms budget for classification + entity extraction
- **Graceful Degradation**: Keyword fallback when ML model unavailable
- **Deterministic Fallback**: Keyword matching produces consistent results
- **Testable**: Mock support for unit testing without real ONNX models

## Architecture

### Module Location

This system resides in the `ai/ondevice` module, which depends only on `core:domain`. It has NO Android dependencies in the core classification logic (Android-specific code isolated to initialization layer).

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    ai/ondevice Module                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         IntentClassificationUseCase                   │  │
│  │  (Orchestrates classification + entity extraction)    │  │
│  └────────────┬─────────────────────────┬─────────────────┘  │
│               │                         │                     │
│               ▼                         ▼                     │
│  ┌─────────────────────┐    ┌──────────────────────────┐   │
│  │  IntentClassifier   │    │   EntityExtractor         │   │
│  │  - ONNX inference   │    │   - Creature matching     │   │
│  │  - Confidence check │    │   - Location parsing      │   │
│  │  - Fallback trigger │    │   - Spell name matching   │   │
│  └──────┬──────────────┘    └──────────────────────────┘   │
│         │                                                     │
│         ▼                                                     │
│  ┌─────────────────────┐                                    │
│  │  OnnxSessionManager │                                    │
│  │  - Model loading    │                                    │
│  │  - Session warmup   │                                    │
│  │  - Request queue    │                                    │
│  └──────┬──────────────┘                                    │
│         │                                                     │
│         ▼                                                     │
│  ┌─────────────────────┐    ┌──────────────────────────┐   │
│  │     Tokenizer       │    │   KeywordFallback         │   │
│  │  - Text → tokens    │    │   - Regex patterns        │   │
│  │  - Vocab mapping    │    │   - Intent matching       │   │
│  │  - Padding/truncate │    │   - Confidence 0.5        │   │
│  └─────────────────────┘    └──────────────────────────┘   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  core:domain    │
                  │  - NLAction     │
                  │  - IntentType   │
                  │  - Creature     │
                  │  - GridPos      │
                  └─────────────────┘
```

## Components and Interfaces

### 1. IntentClassificationUseCase

**Purpose:** Orchestrates the full pipeline from text input to structured NLAction

**Interface:**
```kotlin
class IntentClassificationUseCase(
    private val intentClassifier: IntentClassifier,
    private val entityExtractor: EntityExtractor
) {
    suspend operator fun invoke(
        input: String,
        context: EncounterContext
    ): ActionResult
}
```

**Responsibilities:**
- Validate input text (non-empty, reasonable length)
- Call IntentClassifier to get intent type and confidence
- Call EntityExtractor to find entities in text
- Construct NLAction from intent + entities
- Return ActionResult.Success or ActionResult.RequiresChoice

**Error Handling:**
- Empty input → ActionResult.Failure("Input cannot be empty")
- Low confidence + no keyword match → ActionResult.RequiresChoice with suggestions
- Missing required entities → ActionResult.RequiresChoice with disambiguation options

### 2. IntentClassifier

**Purpose:** Classifies player text into intent types using ONNX model or keyword fallback

**Interface:**
```kotlin
interface IntentClassifier {
    suspend fun classify(text: String): IntentResult
}

data class IntentResult(
    val intent: IntentType,
    val confidence: Float,
    val usedFallback: Boolean
)

enum class IntentType {
    ATTACK,
    MOVE,
    CAST_SPELL,
    USE_ITEM,
    DASH,
    DODGE,
    HELP,
    HIDE,
    DISENGAGE,
    READY,
    SEARCH,
    UNKNOWN
}
```

**Implementation:**
```kotlin
class OnnxIntentClassifier(
    private val sessionManager: OnnxSessionManager,
    private val tokenizer: Tokenizer,
    private val keywordFallback: KeywordFallback,
    private val confidenceThreshold: Float = 0.6f
) : IntentClassifier {
    override suspend fun classify(text: String): IntentResult {
        return try {
            // Tokenize input
            val tokens = tokenizer.tokenize(text)
            
            // Run ONNX inference
            val probabilities = sessionManager.infer(tokens)
            
            // Find highest confidence intent
            val (intent, confidence) = findBestIntent(probabilities)
            
            if (confidence >= confidenceThreshold) {
                IntentResult(intent, confidence, usedFallback = false)
            } else {
                // Fall back to keywords
                keywordFallback.classify(text)
            }
        } catch (e: Exception) {
            logger.warn(e) { "ONNX inference failed, using keyword fallback" }
            keywordFallback.classify(text)
        }
    }
}
```

**Performance:**
- Target: ≤200ms for classification (leaves 100ms for entity extraction)
- ONNX inference: ~50-100ms on mid-tier devices
- Tokenization: ~10-20ms
- Keyword fallback: ~5-10ms

### 3. OnnxSessionManager

**Purpose:** Manages ONNX Runtime session lifecycle and inference execution

**Interface:**
```kotlin
class OnnxSessionManager(
    private val context: Context, // Android context for asset loading
    private val modelPath: String = "models/intent_classifier.onnx"
) {
    suspend fun initialize()
    suspend fun infer(tokens: IntArray): FloatArray
    fun isReady(): Boolean
    fun close()
}
```

**Implementation Details:**
- **Initialization:** Load model from assets on background thread (IO dispatcher)
- **Warmup:** Run dummy inference to warm up model (reduces first-inference latency)
- **Request Queue:** Queue requests during initialization, process when ready
- **Thread Safety:** Use Mutex to protect session access
- **Resource Management:** Close session in onDestroy/onCleared

**Model Specifications:**
- **Input:** `[batch_size=1, sequence_length=128]` Int32 tensor
- **Output:** `[batch_size=1, num_classes=12]` Float32 tensor (probabilities)
- **Model Size:** ~80MB (quantized INT8 model)
- **Format:** ONNX opset 13

### 4. Tokenizer

**Purpose:** Converts text into token indices for ONNX model input

**Interface:**
```kotlin
interface Tokenizer {
    fun tokenize(text: String): IntArray
}

class SimpleTokenizer(
    private val vocabulary: Map<String, Int>,
    private val maxLength: Int = 128,
    private val unknownTokenId: Int = 0,
    private val paddingTokenId: Int = 1
) : Tokenizer {
    override fun tokenize(text: String): IntArray {
        // 1. Lowercase and split on whitespace/punctuation
        val tokens = text.lowercase()
            .split(Regex("\\s+|(?=[.,!?;:])"))
            .filter { it.isNotBlank() }
        
        // 2. Map to vocabulary indices
        val indices = tokens.map { token ->
            vocabulary[token] ?: unknownTokenId
        }
        
        // 3. Pad or truncate to maxLength
        return when {
            indices.size > maxLength -> indices.take(maxLength).toIntArray()
            indices.size < maxLength -> {
                (indices + List(maxLength - indices.size) { paddingTokenId }).toIntArray()
            }
            else -> indices.toIntArray()
        }
    }
}
```

**Vocabulary:**
- Size: ~5,000 tokens (common D&D terms + general English)
- Stored in: `app/src/main/assets/models/vocabulary.json`
- Format: `{"token": index}` JSON map
- Special tokens: `[UNK]=0, [PAD]=1, [CLS]=2, [SEP]=3`

### 5. EntityExtractor

**Purpose:** Extracts game entities (creatures, locations, spells) from text

**Interface:**
```kotlin
class EntityExtractor {
    fun extract(
        text: String,
        context: EncounterContext
    ): EntityExtractionResult
}

data class EntityExtractionResult(
    val creatures: List<ExtractedCreature>,
    val locations: List<GridPos>,
    val spells: List<String>,
    val items: List<String>
)

data class ExtractedCreature(
    val creatureId: Long,
    val name: String,
    val matchedText: String,
    val startIndex: Int,
    val endIndex: Int
)

data class EncounterContext(
    val creatures: List<Creature>,
    val playerSpells: List<String>,
    val playerInventory: List<String>
)
```

**Implementation Strategy:**

**Creature Matching:**
1. Build case-insensitive name map from context.creatures
2. Scan text for creature names (longest match first)
3. Handle partial matches (e.g., "goblin" matches "Goblin Archer")
4. Disambiguate when multiple creatures have similar names

**Location Parsing:**
1. Regex patterns: `[A-Z]\d+` (e.g., "E5"), `\(\d+,\d+\)` (e.g., "(5,5)")
2. Convert to GridPos coordinates
3. Validate coordinates are within map bounds

**Spell Matching:**
1. Case-insensitive match against playerSpells list
2. Handle common misspellings (Levenshtein distance ≤2)
3. Match partial names (e.g., "fireball" matches "Fireball" or "Fire Bolt")

**Item Matching:**
1. Case-insensitive match against playerInventory
2. Handle plurals and articles (e.g., "a potion" → "Potion of Healing")

### 6. KeywordFallback

**Purpose:** Provides rule-based intent classification when ONNX model unavailable

**Interface:**
```kotlin
class KeywordFallback {
    fun classify(text: String): IntentResult
}
```

**Implementation:**
```kotlin
class KeywordFallback {
    private val patterns = mapOf(
        IntentType.ATTACK to listOf(
            Regex("\\battack\\b", RegexOption.IGNORE_CASE),
            Regex("\\bhit\\b", RegexOption.IGNORE_CASE),
            Regex("\\bstrike\\b", RegexOption.IGNORE_CASE),
            Regex("\\bshoot\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.MOVE to listOf(
            Regex("\\bmove\\b", RegexOption.IGNORE_CASE),
            Regex("\\bgo to\\b", RegexOption.IGNORE_CASE),
            Regex("\\bwalk\\b", RegexOption.IGNORE_CASE),
            Regex("\\brun\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.CAST_SPELL to listOf(
            Regex("\\bcast\\b", RegexOption.IGNORE_CASE),
            Regex("\\bspell\\b", RegexOption.IGNORE_CASE),
            Regex("\\bmagic\\b", RegexOption.IGNORE_CASE)
        ),
        // ... more patterns
    )
    
    fun classify(text: String): IntentResult {
        for ((intent, regexList) in patterns) {
            if (regexList.any { it.containsMatchIn(text) }) {
                return IntentResult(
                    intent = intent,
                    confidence = 0.5f, // Fixed confidence for keyword matches
                    usedFallback = true
                )
            }
        }
        
        return IntentResult(
            intent = IntentType.UNKNOWN,
            confidence = 0.0f,
            usedFallback = true
        )
    }
}
```

**Pattern Coverage:**
- Each intent type has 5-10 keyword patterns
- Patterns use word boundaries (`\b`) to avoid partial matches
- Case-insensitive matching
- Prioritize more specific patterns (e.g., "cast spell" before "spell")

## Data Models

### Domain Models (core:domain)

```kotlin
// Intent classification result
sealed interface ActionResult {
    data class Success(val action: NLAction) : ActionResult
    data class Failure(val reason: String) : ActionResult
    data class RequiresChoice(
        val options: List<ActionOption>,
        val prompt: String
    ) : ActionResult
}

// Natural language action
data class NLAction(
    val intent: IntentType,
    val originalText: String,
    val targetCreatureId: Long? = null,
    val targetLocation: GridPos? = null,
    val spellName: String? = null,
    val itemName: String? = null,
    val confidence: Float
)

// Disambiguation option
data class ActionOption(
    val description: String,
    val action: NLAction
)
```

## Error Handling

### Error Categories

1. **Model Loading Errors**
   - Missing model file → Log error, activate keyword fallback permanently
   - Corrupted model file → Log error, activate keyword fallback permanently
   - Insufficient memory → Log error, activate keyword fallback permanently

2. **Inference Errors**
   - ONNX Runtime exception → Log warning, fall back to keywords for this request
   - Invalid input shape → Log error, sanitize input and retry once
   - Timeout (>300ms) → Cancel inference, fall back to keywords

3. **Entity Extraction Errors**
   - Ambiguous creature reference → Return RequiresChoice with options
   - Invalid location format → Ignore location, proceed with other entities
   - Unknown spell name → Return RequiresChoice with spell suggestions

4. **Input Validation Errors**
   - Empty input → Return Failure("Input cannot be empty")
   - Input too long (>500 chars) → Truncate to 500 chars, log warning
   - Invalid characters → Sanitize input, log warning

### Logging Strategy

```kotlin
// Error logging (always)
logger.error(e) { "Failed to load ONNX model from $modelPath" }

// Warning logging (degraded mode)
logger.warn { "ONNX inference failed, using keyword fallback" }

// Info logging (normal operation)
logger.info { "Intent classified: $intent (confidence=$confidence)" }

// Debug logging (development only)
logger.debug { "Tokenized input: ${tokens.joinToString()}" }
```

## Testing Strategy

### Unit Tests (kotest)

**IntentClassifier Tests:**
```kotlin
class IntentClassifierTest : FunSpec({
    context("ONNX classification") {
        test("classifies attack intent with high confidence") {
            val classifier = createTestClassifier()
            val result = classifier.classify("attack the goblin")
            
            result.intent shouldBe IntentType.ATTACK
            result.confidence shouldBeGreaterThan 0.6f
            result.usedFallback shouldBe false
        }
        
        test("falls back to keywords when confidence low") {
            val classifier = createTestClassifier(lowConfidenceModel = true)
            val result = classifier.classify("hit the orc")
            
            result.intent shouldBe IntentType.ATTACK
            result.usedFallback shouldBe true
        }
    }
    
    context("keyword fallback") {
        test("matches attack keywords") {
            val fallback = KeywordFallback()
            val result = fallback.classify("strike the enemy")
            
            result.intent shouldBe IntentType.ATTACK
            result.confidence shouldBe 0.5f
        }
    }
})
```

**EntityExtractor Tests:**
```kotlin
class EntityExtractorTest : FunSpec({
    test("extracts creature by exact name") {
        val extractor = EntityExtractor()
        val context = EncounterContext(
            creatures = listOf(
                Creature(id = 1, name = "Goblin Archer", ...)
            ),
            playerSpells = emptyList(),
            playerInventory = emptyList()
        )
        
        val result = extractor.extract("attack the Goblin Archer", context)
        
        result.creatures shouldHaveSize 1
        result.creatures.first().creatureId shouldBe 1
    }
    
    test("extracts location from grid notation") {
        val extractor = EntityExtractor()
        val result = extractor.extract("move to E5", EncounterContext(...))
        
        result.locations shouldHaveSize 1
        result.locations.first() shouldBe GridPos(4, 4) // E=4, 5=4 (0-indexed)
    }
})
```

**Tokenizer Tests:**
```kotlin
class TokenizerTest : FunSpec({
    test("tokenizes simple text") {
        val vocab = mapOf("attack" to 10, "the" to 20, "goblin" to 30)
        val tokenizer = SimpleTokenizer(vocab, maxLength = 128)
        
        val tokens = tokenizer.tokenize("attack the goblin")
        
        tokens shouldContain 10
        tokens shouldContain 20
        tokens shouldContain 30
    }
    
    test("pads short sequences") {
        val tokenizer = SimpleTokenizer(emptyMap(), maxLength = 128)
        val tokens = tokenizer.tokenize("attack")
        
        tokens shouldHaveSize 128
        tokens.count { it == 1 } shouldBe 127 // Padding tokens
    }
})
```

### Integration Tests

**End-to-End Classification:**
```kotlin
class IntentClassificationIntegrationTest : FunSpec({
    test("classifies and extracts entities from player input") {
        val useCase = IntentClassificationUseCase(
            intentClassifier = OnnxIntentClassifier(...),
            entityExtractor = EntityExtractor()
        )
        
        val context = EncounterContext(
            creatures = listOf(Creature(id = 1, name = "Goblin", ...)),
            playerSpells = listOf("Fire Bolt"),
            playerInventory = emptyList()
        )
        
        val result = useCase("cast fire bolt at the goblin", context)
        
        result shouldBeInstanceOf ActionResult.Success::class
        val action = (result as ActionResult.Success).action
        action.intent shouldBe IntentType.CAST_SPELL
        action.spellName shouldBe "Fire Bolt"
        action.targetCreatureId shouldBe 1
    }
})
```

### Performance Tests

```kotlin
class IntentClassificationPerformanceTest : FunSpec({
    test("classification completes within 300ms budget") {
        val classifier = OnnxIntentClassifier(...)
        
        val duration = measureTimeMillis {
            classifier.classify("attack the goblin with my sword")
        }
        
        duration shouldBeLessThan 300
    }
    
    test("tokenization completes within 20ms") {
        val tokenizer = SimpleTokenizer(...)
        
        val duration = measureTimeMillis {
            tokenizer.tokenize("cast fireball at the group of goblins")
        }
        
        duration shouldBeLessThan 20
    }
})
```

### Test Coverage Targets

- **IntentClassifier:** 90%+ (critical path)
- **EntityExtractor:** 85%+ (complex logic)
- **Tokenizer:** 95%+ (deterministic)
- **KeywordFallback:** 90%+ (rule-based)
- **OnnxSessionManager:** 70%+ (Android-dependent)

### Test Dataset

Create a test dataset of 500 common player commands:
- 100 attack commands
- 100 movement commands
- 100 spell casting commands
- 50 item usage commands
- 50 tactical actions (dash, dodge, hide, etc.)
- 100 edge cases (ambiguous, misspelled, complex)

**Accuracy Target:** 85%+ correct intent classification on test dataset

## Dependencies

### Production Dependencies

```kotlin
// ai/ondevice/build.gradle.kts
dependencies {
    // Core domain
    implementation(project(":core:domain"))
    
    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // JSON parsing (for vocabulary)
    implementation(libs.kotlinx.serialization.json)
    
    // Logging
    implementation(libs.slf4j.api)
}
```

### Test Dependencies

```kotlin
testImplementation(libs.kotest.runner.junit5)
testImplementation(libs.kotest.assertions.core)
testImplementation(libs.kotest.property)
testImplementation(libs.mockk)
testImplementation(libs.kotlinx.coroutines.test)
```

## Asset Management

### Model Files

**Location:** `app/src/main/assets/models/`

**Files:**
- `intent_classifier.onnx` (~80MB) - Quantized INT8 model
- `vocabulary.json` (~200KB) - Token vocabulary
- `model_config.json` (~1KB) - Model metadata

**Model Config Format:**
```json
{
  "model_version": "1.0.0",
  "input_shape": [1, 128],
  "output_shape": [1, 12],
  "max_sequence_length": 128,
  "num_classes": 12,
  "confidence_threshold": 0.6,
  "intent_labels": [
    "ATTACK", "MOVE", "CAST_SPELL", "USE_ITEM",
    "DASH", "DODGE", "HELP", "HIDE",
    "DISENGAGE", "READY", "SEARCH", "UNKNOWN"
  ]
}
```

### Loading Strategy

```kotlin
class AssetLoader(private val context: Context) {
    fun loadModel(path: String): ByteArray {
        return context.assets.open(path).use { it.readBytes() }
    }
    
    fun loadVocabulary(path: String): Map<String, Int> {
        val json = context.assets.open(path).bufferedReader().use { it.readText() }
        return Json.decodeFromString(json)
    }
}
```

## Performance Optimization

### Model Optimization

1. **Quantization:** Use INT8 quantization to reduce model size from ~300MB to ~80MB
2. **Pruning:** Remove low-importance weights to reduce inference time
3. **Operator Fusion:** Fuse consecutive operations in ONNX graph

### Runtime Optimization

1. **Session Warmup:** Run dummy inference on initialization to warm up model
2. **Thread Pool:** Use ONNX Runtime's built-in thread pool (4 threads)
3. **Memory Reuse:** Reuse input/output tensors across inferences
4. **Batch Size 1:** Single-sample inference for lowest latency

### Caching Strategy

**No caching needed** - Classification is fast enough (<300ms) that caching adds complexity without benefit. Each player input is unique, so cache hit rate would be very low.

## Security Considerations

1. **Input Sanitization:** Validate and sanitize all text input before processing
2. **Model Integrity:** Verify model file hash on first load (optional)
3. **Resource Limits:** Enforce max input length (500 chars) to prevent DoS
4. **No PII Logging:** Never log raw player input in production builds

## Future Enhancements

1. **Model Updates:** Support downloading updated models from server
2. **Multi-Language:** Support non-English input with language detection
3. **Context-Aware:** Use conversation history to improve disambiguation
4. **Spell Correction:** Integrate spell checker for typo tolerance
5. **Voice Input:** Support speech-to-text for hands-free play

---

**Last Updated:** 2025-11-15
