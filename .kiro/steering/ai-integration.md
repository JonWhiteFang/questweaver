# AI Integration Guidelines

## Architecture Principles

### 1. AI Proposes, Rules Validate
- AI agents generate **suggestions** (actions, narration, dialogue)
- Rules Engine **validates** legality before commit
- Never trust AI output directly for game state mutations

```kotlin
// Good: AI proposes, rules validate
val aiAction = tacticalAgent.decide(state, creature)
val validated = rulesEngine.validate(aiAction)
if (validated.isLegal) {
    commit(validated.events)
}

// Bad: AI directly mutates state
val aiAction = tacticalAgent.decide(state, creature)
creature.hp -= aiAction.damage // NEVER DO THIS
```

### 2. Deterministic Core, Stochastic Shell
- Rules Engine: 100% deterministic, no LLM calls
- AI Agents: stochastic, but results logged and cacheable
- Separate concerns: game logic vs narrative generation

### 3. Offline-First with Graceful Degradation
- On-device models for critical path (intent parsing)
- Remote LLM for rich narration (optional, with fallback)
- Template-based narration when AI unavailable

## Agent Types

### Intent Parser (On-Device, ONNX)
**Purpose:** Classify player natural language input into structured actions

**Model:** Distilled BERT/DistilBERT (50-100MB quantized)

**Input:** Player text (max 256 tokens)

**Output:** `{action_type, confidence, entities[]}`

**Example:**
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

**Fallback:** Keyword matching + regex patterns

### Narrative Agent (Remote, Optional)
**Purpose:** Generate scene descriptions, outcomes, NPC dialogue

**Model:** GPT-3.5/4 or Llama 3 8B (via gateway)

**Context Window:** 6KB max (world state + recent events + player action)

**Constraints:**
- Max 150 tokens output in combat
- Max 500 tokens output in exploration
- Content filter: PG-13 default, configurable

**Prompt Template:**
```
You are the DM for a D&D-style game. The party is in {location}.

Recent events:
{event_summary}

Player action: {player_action}

Narrate the outcome in 2-3 sentences. Be concise and actionable.
```

**Caching:** Hash (world_state + action) → cache response for 1 hour

**Fallback:** Template-based narration
```kotlin
fun templateNarration(action: Action, result: ActionResult): String = when {
    result is AttackResult && result.hit -> 
        "${action.attacker.name} strikes ${action.target.name} for ${result.damage} damage!"
    result is AttackResult && !result.hit ->
        "${action.attacker.name}'s attack misses ${action.target.name}."
    // ...
}
```

### Tactical Agent (Hybrid)
**Purpose:** Select actions for AI-controlled creatures

**Approach:** Behavior Tree + Heuristic Scoring (no LLM)

**Behavior Tree Structure:**
```
Selector (pick first success)
├─ IfLowHP (< 25%)
│  └─ Sequence
│     ├─ HasDisengage? → Disengage
│     └─ MoveToSafety
├─ IfAllyDown
│  └─ HasHealingSpell? → CastHeal
├─ IfControlAvailable
│  └─ Sequence
│     ├─ EvaluateTargets (most enemies in AoE)
│     └─ CastControlSpell
├─ IfHighDPR
│  └─ Sequence
│     ├─ SelectBestTarget (lowest AC, highest threat)
│     └─ Attack
└─ MoveToCover
```

**Scoring Heuristics:**
```kotlin
fun scoreAction(action: Action, state: EncounterState): Float {
    var score = 0f
    
    // Expected damage
    score += action.expectedDamage * 1.0f
    
    // Resource economy (prefer cantrips over spell slots)
    score -= action.resourceCost * 0.5f
    
    // Risk to allies (avoid friendly fire)
    score -= action.alliesInAoE * 2.0f
    
    // Objective proximity (move toward goal)
    score += (1.0f / action.distanceToObjective) * 0.3f
    
    return score
}
```

**No LLM Required:** Pure Kotlin logic, deterministic given same state

### Dialogue Agent (Remote, Optional)
**Purpose:** Generate NPC responses in conversations

**Model:** Same as Narrative Agent

**Context:** NPC persona + faction + relationship + conversation history

**Prompt Template:**
```
You are {npc_name}, a {npc_role} in {faction}.

Personality: {personality_traits}
Current goal: {npc_goal}
Relationship with party: {relationship}

Conversation so far:
{history}

Player says: "{player_input}"

Respond in character (max 100 words). Be concise.
```

**Memory:** Store conversation summaries in Journal after each dialogue

**Fallback:** Scripted dialogue trees for key NPCs

## Model Management

### On-Device Models (ONNX)
**Location:** `app/src/main/assets/models/`

**Files:**
- `intent.onnx` (intent classifier, ~80MB)
- `tokenizer.json` (tokenizer config)

**Loading:**
```kotlin
class ModelManager(private val context: Context) {
    private val env = OrtEnvironment.getEnvironment()
    
    fun loadIntent(): OrtSession {
        val modelBytes = context.assets.open("models/intent.onnx").readBytes()
        return env.createSession(modelBytes)
    }
    
    // Warm up on background thread
    suspend fun warmUp() = withContext(Dispatchers.Default) {
        val session = loadIntent()
        session.run(mapOf("input_ids" to dummyInput))
    }
}
```

### Remote Models (Gateway)
**Endpoint:** `POST /v1/narrate`, `/v1/dialogue`

**Request:**
```json
{
  "context": {
    "location": "Goblin Cave",
    "recent_events": ["Party entered cave", "Goblins spotted"],
    "world_state": {...}
  },
  "action": {
    "type": "attack",
    "actor": "Thorin",
    "target": "Goblin",
    "result": "hit",
    "damage": 12
  },
  "max_tokens": 150,
  "temperature": 0.7
}
```

**Response:**
```json
{
  "narration": "Thorin's axe cleaves through the goblin's leather armor...",
  "cache_key": "abc123",
  "latency_ms": 1200
}
```

**Timeout:** 4s soft, 8s hard

**Retry:** 1 retry with exponential backoff

**Circuit Breaker:** After 3 consecutive failures, use fallback for 5 minutes

## Prompt Engineering

### Best Practices
1. **Be specific:** "Narrate in 2-3 sentences" not "describe the scene"
2. **Provide context:** Include recent events, not just current action
3. **Set constraints:** Token limits, tone, content rating
4. **Use examples:** Few-shot prompting for consistent format
5. **Validate output:** Check length, content filters, JSON structure

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

## Performance & Caching

### Response Caching
```kotlin
class NarrationCache(private val maxSize: Int = 1000) {
    private val cache = LruCache<String, CachedNarration>(maxSize)
    
    fun get(key: String): String? {
        val cached = cache.get(key) ?: return null
        if (cached.expiresAt < System.currentTimeMillis()) {
            cache.remove(key)
            return null
        }
        return cached.text
    }
    
    fun put(key: String, text: String, ttlMs: Long = 3600_000) {
        cache.put(key, CachedNarration(text, System.currentTimeMillis() + ttlMs))
    }
}
```

### Batching
- Queue multiple narration requests
- Batch every 500ms or 5 requests
- Single API call with array of contexts

### Prefetching
- Predict likely player actions (attack, move, cast spell)
- Prefetch narration for top 3 likely outcomes
- Cache results before player commits

## Testing AI Components

### Unit Tests
```kotlin
class IntentClassifierTest : FunSpec({
    test("classifies attack intent correctly") {
        val classifier = IntentClassifier(mockSession)
        val intent = classifier.parse("I attack the goblin with my sword")
        
        intent.type shouldBe ActionType.ATTACK
        intent.confidence shouldBeGreaterThan 0.8f
        intent.entities shouldContain Entity("goblin", EntityType.TARGET)
    }
})
```

### Integration Tests
```kotlin
class NarrativeAgentTest : FunSpec({
    test("generates narration within token limit") {
        val agent = NarrativeAgent(mockGateway)
        val narration = agent.narrate(mockContext, mockAction)
        
        narration.tokenCount shouldBeLessThan 150
        narration.text.shouldNotBeBlank()
    }
    
    test("falls back to template on timeout") {
        val agent = NarrativeAgent(slowGateway)
        val narration = agent.narrate(mockContext, mockAction, timeout = 100.milliseconds)
        
        narration.source shouldBe NarrationSource.TEMPLATE
    }
})
```

### Property-Based Tests
```kotlin
class TacticalAgentTest : FunSpec({
    test("always selects legal actions") {
        checkAll(Arb.encounterState(), Arb.creature()) { state, creature ->
            val agent = TacticalAgent()
            val action = agent.decide(state, creature)
            
            rulesEngine.validate(action).isLegal shouldBe true
        }
    }
})
```

## Observability

### Metrics
- Intent classification accuracy (log ground truth when available)
- Narration latency (p50, p95, p99)
- Cache hit rate
- Fallback usage rate
- Token usage per session

### Logging
```kotlin
logger.info {
    "AI narration: latency=${latency}ms, tokens=${tokens}, " +
    "cache=${cacheHit}, source=${source}"
}
```

### Debug UI
- Show AI confidence scores
- Display prompt + response in dev mode
- Toggle between AI and template narration
- Inspect behavior tree decisions

## Safety & Ethics

### Content Policy
- Default to PG-13 rating
- User-configurable content filters
- No generation of harmful content
- Respect user privacy (no logging of personal data)

### Bias Mitigation
- Diverse training data for intent classifier
- Regular audits of generated content
- User feedback mechanism for inappropriate content

### Transparency
- Clearly indicate AI-generated content
- Provide option to regenerate narration
- Allow users to override AI decisions

## Cost Management

### Token Budget
- Intent parsing: on-device (free)
- Narration: ~150 tokens/request
- Dialogue: ~100 tokens/request
- Target: <10k tokens/session average

### Rate Limiting
```kotlin
class RateLimiter(private val maxRequestsPerMinute: Int = 30) {
    private val requests = mutableListOf<Long>()
    
    suspend fun acquire() {
        val now = System.currentTimeMillis()
        requests.removeAll { it < now - 60_000 }
        
        if (requests.size >= maxRequestsPerMinute) {
            delay(60_000 - (now - requests.first()))
        }
        
        requests.add(now)
    }
}
```

### Fallback Strategy
1. Try on-device model (if available)
2. Try cached response
3. Try remote API (with timeout)
4. Fall back to template
5. Log failure for analysis
