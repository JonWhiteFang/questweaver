---
inclusion: fileMatch
fileMatchPattern: ['**/ai/**/*.kt', '**/feature/**/viewmodel/*.kt']
---

# AI Integration Guidelines

## Core Principles

### AI Proposes, Rules Validate
**CRITICAL:** AI agents generate suggestions only. Rules Engine validates before commit.

```kotlin
// CORRECT: Validate before commit
val aiAction = tacticalAgent.decide(state, creature)
val validated = rulesEngine.validate(aiAction)
if (validated.isLegal) commit(validated.events)

// WRONG: Direct state mutation
creature.hp -= aiAction.damage // NEVER
```

### Deterministic Core, Stochastic Shell
- `core:rules` → 100% deterministic, NO LLM calls
- `ai/*` → Stochastic, results logged and cacheable
- Separate game logic from narrative generation

### Offline-First with Fallbacks
- On-device ONNX for critical path (intent parsing)
- Remote LLM for rich narration (optional)
- Template-based fallback always available

## Agent Types

### Intent Parser (On-Device)
**Location:** `ai/ondevice/`  
**Model:** ONNX DistilBERT (~80MB) in `app/src/main/assets/models/intent.onnx`  
**Input:** Player text (max 256 tokens)  
**Output:** `Intent(type: ActionType, confidence: Float, entities: List<Entity>)`  
**Fallback:** Keyword matching + regex

```kotlin
class IntentClassifier(private val session: OrtSession) {
    fun parse(text: String): Intent {
        val tokens = tokenize(text)
        val output = session.run(mapOf("input_ids" to tokens))
        return Intent(
            type = ActionType.values()[output.argmax()],
            confidence = output.max(),
            entities = extractEntities(text, output)
        )
    }
}
```

### Narrative Agent (Remote, Optional)
**Location:** `ai/gateway/`  
**Endpoints:** `POST /v1/narrate`, `/v1/dialogue`  
**Constraints:**
- Combat: max 150 tokens
- Exploration: max 500 tokens
- Context window: 6KB max
- Timeout: 4s soft, 8s hard
- Content: PG-13 default

**Prompt Template:**
```
You are the DM for a D&D-style game. The party is in {location}.

Recent events: {event_summary}
Player action: {player_action}

Narrate the outcome in 2-3 sentences. Be concise and actionable.
```

**Caching:** Hash `(world_state + action)` → 1 hour TTL

**Fallback Template:**
```kotlin
fun templateNarration(action: Action, result: ActionResult): String = when {
    result is AttackResult && result.hit -> 
        "${action.attacker.name} strikes ${action.target.name} for ${result.damage} damage!"
    result is AttackResult && !result.hit ->
        "${action.attacker.name}'s attack misses ${action.target.name}."
    // ...
}
```

### Tactical Agent (Deterministic)
**Location:** `ai/ondevice/tactical/`  
**Approach:** Behavior Tree + Heuristic Scoring (NO LLM)  
**Must:** Always return legal actions validated by `RulesEngine`

**Behavior Tree Priority:**
1. Low HP (<25%) → Disengage + Move to Safety
2. Ally Down → Heal if available
3. Control Spell → Target most enemies in AoE
4. High DPR → Attack best target (lowest AC, highest threat)
5. Default → Move to Cover

**Scoring:**
```kotlin
fun scoreAction(action: Action, state: EncounterState): Float =
    action.expectedDamage * 1.0f -
    action.resourceCost * 0.5f -
    action.alliesInAoE * 2.0f +
    (1.0f / action.distanceToObjective) * 0.3f
```

### Dialogue Agent (Remote, Optional)
**Same as Narrative Agent**  
**Context:** NPC persona + faction + relationship + history  
**Output:** Max 100 words  
**Memory:** Store summaries in Journal  
**Fallback:** Scripted dialogue trees

## Model Management

### On-Device (ONNX)
```kotlin
class ModelManager(private val context: Context) {
    private val env = OrtEnvironment.getEnvironment()
    
    fun loadIntent(): OrtSession {
        val modelBytes = context.assets.open("models/intent.onnx").readBytes()
        return env.createSession(modelBytes)
    }
    
    suspend fun warmUp() = withContext(Dispatchers.Default) {
        loadIntent().run(mapOf("input_ids" to dummyInput))
    }
}
```

### Remote Gateway
**Request:**
```json
{
  "context": {"location": "...", "recent_events": [...], "world_state": {...}},
  "action": {"type": "attack", "actor": "...", "target": "...", "result": "hit", "damage": 12},
  "max_tokens": 150,
  "temperature": 0.7
}
```

**Circuit Breaker:** 3 consecutive failures → fallback for 5 minutes

## Performance

### Caching
```kotlin
class NarrationCache(maxSize: Int = 1000) {
    private val cache = LruCache<String, CachedNarration>(maxSize)
    
    fun get(key: String): String? {
        val cached = cache.get(key) ?: return null
        return if (cached.expiresAt > System.currentTimeMillis()) cached.text else null
    }
    
    fun put(key: String, text: String, ttlMs: Long = 3600_000) {
        cache.put(key, CachedNarration(text, System.currentTimeMillis() + ttlMs))
    }
}
```

### Optimization
- **Batching:** Queue requests, batch every 500ms or 5 requests
- **Prefetching:** Predict top 3 likely actions, prefetch narration
- **Rate Limiting:** Max 30 requests/minute

## Testing

### Unit Tests
```kotlin
class IntentClassifierTest : FunSpec({
    test("classifies attack intent correctly") {
        val intent = classifier.parse("I attack the goblin with my sword")
        intent.type shouldBe ActionType.ATTACK
        intent.confidence shouldBeGreaterThan 0.8f
    }
})
```

### Property-Based Tests
```kotlin
class TacticalAgentTest : FunSpec({
    test("always selects legal actions") {
        checkAll(Arb.encounterState(), Arb.creature()) { state, creature ->
            val action = TacticalAgent().decide(state, creature)
            rulesEngine.validate(action).isLegal shouldBe true
        }
    }
})
```

### Integration Tests
```kotlin
test("falls back to template on timeout") {
    val narration = agent.narrate(context, action, timeout = 100.milliseconds)
    narration.source shouldBe NarrationSource.TEMPLATE
}
```

## Observability

### Metrics to Log
- Intent classification accuracy
- Narration latency (p50, p95, p99)
- Cache hit rate
- Fallback usage rate
- Token usage per session

```kotlin
logger.info { "AI narration: latency=${latency}ms, tokens=${tokens}, cache=${hit}, source=${source}" }
```

## Safety

### Content Filtering
```kotlin
class ContentFilter {
    private val bannedWords = setOf(/* ... */)
    private val maxViolenceLevel = ViolenceLevel.PG13
    
    fun filter(text: String): FilterResult {
        if (bannedWords.any { text.contains(it, ignoreCase = true) }) {
            return FilterResult.Blocked("Inappropriate content")
        }
        if (detectViolenceLevel(text) > maxViolenceLevel) {
            return FilterResult.Blocked("Violence level too high")
        }
        return FilterResult.Allowed(text)
    }
}
```

### Privacy
- No logging of personal data
- Local encryption for all AI-generated content
- Opt-in for remote features

## Fallback Strategy

**Priority Order:**
1. On-device model (if available)
2. Cached response
3. Remote API (with timeout)
4. Template-based generation
5. Log failure for analysis

## Token Budget

- Intent parsing: on-device (free)
- Narration: ~150 tokens/request
- Dialogue: ~100 tokens/request
- **Target:** <10k tokens/session average
