# AI D&D DM — Android Architecture & Design Spec (Single-Player + AI Party)

**Product Name (codename):** *QuestWeaver*

**Target Platform:** Android (API 26+, phones & tablets). Optional cloud services.

**Primary Use Case:** One human plays a single PC. All other party members and NPCs are AI-controlled. The app manages narrative, rules facilitation, combat, and persistence for long-running campaigns. A simple tactical map shows creature positions, walls/obstacles, ranges, and areas of effect.

---

## 1) Goals & Non‑Goals

### Goals
- Deliver a *solo-friendly* D&D‑style experience with an AI GM that handles NPCs, scene narration, rules adjudication (core SRD‑compatible), and combat.
- Persist full campaign state across sessions with robust save/restore and rollback.
- Provide a simple, legible tactical map for combat and exploration (grid + tokens + walls/objects + range overlays).
- Work offline (core play) with optional cloud sync for backups and cross-device restore.
- Be modular so we can swap AI models, rulesets, and storage layers.

### Non‑Goals (v1)
- Photorealistic maps or VTT‑grade features (lighting, dynamic LoS) — keep it simple.
- Full, copyrighted 5e content. We target SRD-like rules and user-supplied homebrew.
- Multiplayer over network (future enhancement).

---

## 2) User Stories (MVP → v1.1)

- **US1:** As a player, I create/import a character and start a new campaign.
- **US2:** I describe actions in natural language; the AI DM narrates outcomes and manages checks.
- **US3:** When combat starts, a simple grid map appears; I can see creatures, obstacles, ranges, and turn order.
- **US4:** I can move my PC by tapping squares; AI controls allies/enemies according to tactics and rules.
- **US5:** I can inspect any token to see stats, conditions, and current HP; the log shows dice and rules references.
- **US6:** The AI runs NPC dialogue and quest logic; conversations are summarized to notes automatically.
- **US7:** The session auto-saves; I can resume exactly where I left off.
- **US8:** I can export/import campaigns and enable optional cloud backup.
- **US9:** I can switch the AI’s strictness/creativity and battle difficulty.
- **US10:** I can add homebrew items, creatures, or rules tweaks via JSON.

---

## 3) Functional Requirements

1. **Narrative & Rules Agent**
   - Interprets player intent; proposes checks and DCs aligned to SRD‑style logic and table-configurable rule variants.
   - Resolves outcomes; writes concise narration; logs dice and modifiers.
2. **Combat Engine**
   - Initiative, turn order, movement (grid), actions/bonus/reaction economy, conditions, ranged/melee attack resolution, AoE templates.
   - Simple cover/obstacle rules; line-of-effect checks (LoE simplified vs LoS).
3. **Tactical Map**
   - 2D grid (square, 5 ft per tile), tokens, fog/reveal (minimal), walls/objects as blocked tiles or half/three‑quarters cover flags.
   - Range/area overlays (cone, circle, line, cube templates).
4. **Party & NPC Manager**
   - AI companions with roles (tank, striker, controller, support) and behavior trees/tactical profiles.
   - NPC state (goals, relationships, faction, memory of promises/secrets).
5. **Assets & Data**
   - Creatures, classes, spells, items, conditions, features (SRD‑compatible schema + homebrew extension).
6. **Persistence**
   - Local DB (SQLite/Room) for campaign, session logs, maps, entities, AI memory embeddings.
   - Optional cloud sync/backup (Firebase/Drive) with conflict resolution.
7. **Offline-first**
   - All core features offline; on‑device NLU where possible; defer long‑form LLM calls until online.
8. **Observability**
   - Play log, dice traces, rule citations; opt‑in analytics (events, errors) privacy‑safe.

---

## 4) Non‑Functional Requirements

- **Performance:** <16ms frame budget during map interaction; AI turns <2s average on‑device or <5s via network.
- **Reliability:** Autosave on state mutation; crash‑safe recovery; deterministic reruns via random seeds for rules engine.
- **Security:** Local encryption (SQLCipher) for saves; OPAQUE/Google sign‑in for cloud sync.
- **Privacy:** No 3rd‑party data sharing without explicit consent; redaction of personal data in logs.
- **Modularity:** Clear boundaries for Rules, AI, Map, Data, UI.

---

## 5) Architecture Overview

### High‑Level

```
+---------------------+           +-----------------------+          +-------------------+
| Android App (UI)    |<--------->| Domain Layer / UseCases|<-------->| Persistence Layer |
| Compose UI + Map    |           | (Kotlin)              |          | Room/SQLite +     |
| Engine (LibGDX/     |           +-----------+-----------+          | SQLCipher        |
| Skia/Compose Canvas)|                       |                      +---------+---------+
+----------+----------+                       |                                |
           |                                  v                                v
           |                     +----------------------+           +----------------------+
           |                     | Rules Engine         |           | AI Services          |
           |                     | (Deterministic Core) |           | (Narrative, Tactics) |
           |                     +----------+-----------+           +----------+-----------+
           |                                |                                  |
           v                                v                                  v
+-------------------+        +--------------------------+        +-------------------------+
| Input/NLU Adapter |        | Map/Combat Subsystem     |        | Cloud (optional)       |
| (on‑device or API)|        | (Grid, Path, AoE, LoE)   |        | Firebase/Function/LLM  |
+-------------------+        +--------------------------+        +-------------------------+
```

### Key Modules

- **UI Layer (Jetpack Compose)**
  - Screens: Home, Character, Encounter, Map, Dialogue, Journal, Settings.
  - State via **MVI** (Unidirectional Data Flow): `ViewModel` → `UiState` → `Intent`.
- **Domain Layer**
  - Use cases: `StartCampaign`, `ResumeCampaign`, `ProcessPlayerAction`, `RunCombatRound`, `MoveToken`, `CastSpell`, `SaveGame`, `SyncCloud`.
  - Business entities: `Creature`, `Party`, `Encounter`, `Action`, `Effect`, `MapGrid`, `Tile`, `Obstacle`, `Condition`.
- **Rules Engine** (deterministic, no LLM)
  - Dice, modifiers, proficiency, advantage/disadvantage, AC/HP, saving throws, conditions, concentration, rests.
  - Configuration for rule variants.
- **AI Services**
  - *Narrative Agent:* turns world state + player intent into narration and next hooks.
  - *Tactical Agent:* selects actions for AI party & enemies using behavior trees + value heuristics.
  - *Dialogue Agent:* runs NPC conversations; writes succinct notes to Journal.
  - Adapters for **on‑device** (e.g., GGUF/ONNX small LLM) or **remote** (server LLM) with caching.
- **Map/Combat Subsystem**
  - Grid render (Compose Canvas or lightweight engine), token placement, snapping, movement ranges, AoE templates, cover flags, pathfinding (A*), LoE ray tests.
- **Persistence Layer**
  - Room entities + DAOs; encryption. Event‑sourced session log for replay.
- **Cloud Integration (optional)**
  - Firebase Auth, Firestore/Cloud Storage for backups; Cloud Functions for heavy AI calls.

---

## 6) Data Model (Room Entities, simplified)

```sql
-- Campaign & Session
Campaign(id PK, name, createdAt, ruleset, settingsJson)
Session(id PK, campaignId FK, startedAt, endedAt, seed, version)
Event(id PK, sessionId FK, idx, type, payloadJson, timestamp) -- event-sourced log

-- World & Map
Map(id PK, campaignId FK, name, gridWidth, gridHeight, tileSize, theme, dataJson)
TileMap(id PK, mapId FK, x, y, flags INT) -- bits: BLOCKED, HALF_COVER, THREE_QTR_COVER, DIFFICULT

-- Entities
Creature(id PK, campaignId FK, name, kind, level, abilitiesJson, ac, hpMax, hpCur, speed, sensesJson,
         inventoryJson, featuresJson, aiProfileId FK, portraitRef, notes)
PartyMember(campaignId FK, creatureId FK, role, isPlayerControlled BOOL)

-- Combat State
Encounter(id PK, campaignId FK, mapId FK, round, activeInitiativeIdx, stateJson)
InitiativeEntry(encounterId FK, orderIdx, creatureId FK, passivePerception)
ConditionState(id PK, creatureId FK, condition, expiresRound INT, dataJson)

-- AI
AIProfile(id PK, name, role, paramsJson)
Memory(id PK, campaignId FK, scope ENUM('npc','faction','world','party','pc'), key, value, vectorRef)

-- Journal & Assets
JournalEntry(id PK, campaignId FK, title, text, tags, createdAt)
Asset(id PK, campaignId FK, type, uri, metaJson)
```

**Notes**
- Use `payloadJson` in `Event` to store normalized domain events (e.g., `AttackResolved`, `MoveCommitted`, `SpellCast`).
- `stateJson` in `Encounter` holds transient structures (e.g., readied actions) reconstructed from events on load.
- Vector search for AI memory optional (on‑device FAISS lite or simple tag search v1).

---

## 7) Map & Combat Design

### Grid & Rendering
- **Grid:** Square 5‑ft tiles; per‑tile flags for blocked/cover/difficult.
- **Tokens:** Circle/square stamps with ring overlays for selection, HP bar, conditions icons.
- **UI Gestures:** Tap select; long‑press context; drag to path; pinch to zoom; two‑finger pan.

### Movement & Pathfinding
- A* with diagonal cost = 1.414; disallow diagonal through orthogonally blocked corners.
- Calculate **movement budget** (speed → tiles/round). Difficult terrain costs 2 tiles.

### Line of Effect (LoE)
- Ray cast from center of source tile to target tile corners; blocked if intersecting `BLOCKED` edges.

### Areas of Effect
- Templates: **Circle** (radius ft), **Cone** (degrees, length), **Line** (length, 5‑ft wide), **Cube**.
- Render overlay shapes snapped to grid, count affected tiles.

### Cover & Ranges
- Half/Three‑quarters cover from flags on edge adjacency; apply +2/+5 AC.
- Range bands: show min/max; mark disadvantage band.

### Initiative & Turn Engine
- Stable initiative list; `activeInitiativeIdx` pointer.
- Turn phases: **Start → Move/Action/Bonus/Free → End**; reactions triggered by events.

### Dice & Determinism
- Seeded RNG per Session; all rolls logged (`d20[adv=1]+mods=+7 -> 22`).

---

## 8) AI System Design

### 8.1 Agents & Orchestration

- **Intent Parser**: Classifies player input → {action type, targets, checks}. Uses on‑device NLU first.
- **Narrative Agent**: Produces short, actionable narration and choices. Has access to `WorldState` and `JournalMemory`.
- **Dialogue Agent**: Persona‑conditioned outputs; constrained by `NPCFacts` and `QuestState`.
- **Tactical Agent**: Behavior Tree (BT) + heuristic scoring:
  - BT nodes: *MaintainDistance, FocusFire, ProtectHealer, UseControl, DisengageLowHP*.
  - Score actions by expected DPR, resource economy (spell slots), risk to allies, objective proximity.

**Coordinator** decides which agent runs based on `GamePhase` and queues UI updates.

### 8.2 Safety & Rules Guardrails
- All AI suggestions pass through **Rules Engine Validator** (non‑LLM) before commit.
- Hard caps on narration length in combat; summaries routed to Journal.
- Content filters (on‑device) for tone; toggles for PG/PG‑13.

### 8.3 Model Options
- **On‑device:** Distilled 1–3B model (GGUF/ONNX) for intent + micro‑narration; quantized for ARM.
- **Remote (optional):** Larger LLM via server for rich scenes, with caching and rate limiting.
- **Retrieval:** Lightweight embeddings for campaign memory; RAG context window <6 KB.

---

## 9) Domain Events (examples)

- `EncounterStarted{mapId, participants[], seed}`
- `MoveCommitted{creatureId, pathTiles[], cost}`
- `AttackRolled{attackerId, targetId, roll, mods, adv}`
- `DamageApplied{targetId, amount, type}`
- `ConditionApplied{targetId, condition, untilRound}`
- `SpellCast{casterId, spellId, slotLevel, targets[]}`
- `TurnAdvanced{fromIdx, toIdx, round}`
- `JournalAdded{title, text, tags[]}`

Event sourcing enables **undo/redo**, deterministic replays, and easy debug telemetry.

---

## 10) Use‑Case Interactions (Sequence Sketches)

### 10.1 Player Action → Outcome
```
Player → UI → IntentParser → UseCase.ProcessPlayerAction → RulesEngine
      → WorldState Mutations → EventLog → UiState → Render → Journal
```

### 10.2 AI Turn
```
TurnEngine → TacticalAgent (BT) → CandidateActions → RulesEngine.Validate
          → Choose + Commit → EventLog → UiState → Render
```

### 10.3 Start Encounter
```
UseCase.StartEncounter → BuildInitiative → SpawnTokens → EventLog
                      → UiState(Map, TurnOrder)
```

---

## 11) Tech Stack

- **Language:** Kotlin (100%), Coroutines + Flow.
- **UI:** Jetpack Compose; `Canvas` for grid; `LazyColumn` for log.
- **Graphics:** Compose Canvas or Skia; optional LibGDX if needed.
- **DI:** Koin or Hilt.
- **Persistence:** Room + SQLCipher; DataStore for small settings.
- **Sync:** Firebase Auth; Firestore/Cloud Storage; WorkManager for background sync.
- **AI:**
  - On‑device: GGML/GGUF inference via Kotlin JNI (e.g., llama.cpp binding) or ONNX Runtime Mobile.
  - Server (optional): gRPC/HTTPS to a lightweight gateway; response cache (OkHttp + disk LRU).
- **Testing:** JUnit5, kotest, MockK; screenshot tests (Paparazzi); property‑based tests for rules.
- **Maps/Path:** A* in Kotlin; simple ray casting; AoE utilities.

---

## 12) Public Interfaces (selected)

```kotlin
// Use cases
interface ProcessPlayerAction { suspend fun invoke(input: NLAction): ActionResult }
interface RunCombatRound { suspend fun invoke(encounterId: Id): RoundResult }
interface MoveToken { suspend fun invoke(encounterId: Id, creatureId: Id, to: Tile): MoveResult }

// Rules
interface RulesEngine {
  fun checkAttack(attacker: Creature, target: Creature, weapon: Weapon, adv: Advantage): AttackOutcome
  fun applyDamage(target: Creature, dmg: Damage): CreatureState
  fun savingThrow(creature: Creature, ability: Ability, dc: Int, adv: Advantage): CheckResult
}

// Tactical AI
interface TacticalAgent { suspend fun decide(state: EncounterState, unit: Creature): ActionPlan }

// Map
data class MapGrid(val w: Int, val h: Int, val tiles: IntArray /* bitflags */)
fun lineOfEffect(a: Tile, b: Tile, grid: MapGrid): Boolean
```

---

## 13) Screens & UX Wireframe (text)

- **Home:** Continue, New, Settings, Import/Export.
- **Character:** PC sheet (collapsed sections), gear, spells; AI party overview.
- **Scene/Dialogue:** Chat‑like transcript; quick actions (Investigate, Persuade, Travel). "Propose Action" button.
- **Combat/Map:** Full‑screen grid; left rail for turn order; bottom action bar; right drawer: token sheet.
- **Journal:** Auto‑summaries; search; pin to map locations.

**Accessibility:** font scaling, colorblind token shapes, haptics for confirmations.

---

## 14) Persistence & Sync Strategy

- **Autosave** for every committed event; compress large payloads (Zstd) when archiving.
- **Cloud backup** uses hourly deltas (events since last backup). Conflicts resolved by latest `Event.idx`.
- **Export** campaign bundle (`.qwv` ZIP: schema.json + sqlite + assets).

---

## 15) Performance Budget

- Map render: ≤ 4 ms on mid‑tier device (1080p); path preview ≤ 2 ms; overlays ≤ 2 ms.
- AI tactical decision: ≤ 300 ms on device; fallback to cached tactic; escalate to server if allowed.
- LLM narration (remote): soft timeout 4 s; degrade to template narration.

---

## 16) Security & Compliance

- **Content licensing:** Use SRD/ORC‑compatible content only; no bundled copyrighted text.
- **Storage:** SQLCipher with user PIN/biometric; encrypted exports (optional).
- **Network:** TLS 1.2+; certificate pinning for AI gateway.

---

## 17) Telemetry & Debug

- **Opt‑in** analytics: session length, encounters, crashes, AI latency; all anonymized.
- **Dev HUD:** seed, fps, RNG stream, event inspector; replay last N turns.

---

## 18) Testing Plan

- **Unit:** Rules engine (attacks, saves, conditions), pathfinding edge cases, AoE coverage.
- **Property‑based:** Dice distributions, initiative ordering invariants.
- **Integration:** AI → Rules → Map commits; persistence round‑trip.
- **UX:** Accessibility checks; offline mode scenarios; long‑running campaign (>10k events).

---

## 19) Roadmap

**MVP (8–10 weeks)**
- Core rules engine, map grid, single encounter flow, PC import, AI for allies/enemies (BT minimal), local saves, basic narration.

**v1.0**
- Cloud backup, dialogue agent, homebrew import/export, cover rules, cones/lines, journal autosummaries.

**v1.1**
- Advanced tactics (focus fire, objective control), faction memory, quests, map editor, condition icons, difficulty sliders.

**v1.2+**
- Procedural adventures, dynamic encounters, richer fog/LoS, encounters generator, remote LLM with cache.

---

## 20) Open Questions & Risks

- **On‑device LLM latency** on lower‑end devices — may require aggressive distillation and caching.
- **Rules complexity creep** — keep SRD‑compatible core; modular variants.
- **Event log growth** — periodic compaction (snapshotting) to cap DB size.
- **UI complexity** — commit to simple map; avoid VTT bloat.

---

## 21) Appendix A — Behavior Tree Sketch

```
Selector
 ├─ IfLowHP → DisengageOrDodge
 ├─ IfAllyDown → Help/Heal
 ├─ IfControlAvailable → CastControlSpell
 ├─ IfHighDPR → AttackBestTarget
 └─ MoveToBetterCover
```

---

## 22) Appendix B — Minimal JSON for Homebrew Creatures

```json
{
  "name": "Goblin Slinger",
  "ac": 14,
  "hpMax": 11,
  "speed": 30,
  "abilities": {"str": 8, "dex": 14, "con": 10, "int": 10, "wis": 8, "cha": 8},
  "actions": [
    {"type": "attack", "name": "Sling", "toHit": 4, "damage": "1d4+2", "range": [30, 120]},
    {"type": "bonus", "name": "Nimble Escape", "effect": "Disengage or Hide"}
  ],
  "tags": ["goblin", "ranged"]
}
```

---

## 23) Acceptance Criteria (MVP)

- Start a campaign with a PC, generate an encounter with 3 goblins, win/lose condition.
- Map shows positions, obstacles, ranges; movement & attacks work with logged rolls.
- AI allies make coherent choices (move, attack, dodge) in <1 s average.
- Session restores perfectly after app kill; export/import works on another device.

---

## 24) Implementation Notes

- Prefer **event sourcing** over CRUD mutations; rebuild state into `EncounterState` structs.
- Keep AI outputs *propositional*; the Rules Engine commits — guarantees legality and determinism.
- Use **sealed classes** for actions/effects in Kotlin for exhaustiveness.
- Make every render derived from `UiState` only; no imperative UI mutations.

---

---

## 25) Stack Options & Trade‑offs (Pick‑List with Pros/Cons)

Below are curated choices per layer. Each option includes pros/cons and a **recommended default** for this project’s goals (offline‑first, simple tactical map, AI‑driven solo play).

### 25.1 Dependency Injection
**Option A — Hilt (Dagger)**  
**Pros:**
- Compile‑time graph validation; great tooling & docs.
- Scopes align with Android lifecycles out‑of‑the‑box.
- Widely used; onboarding easier for new contributors.
**Cons:**
- Annotation/gradle complexity; slower builds than pure Kotlin.
- Advanced multibinding ergonomics can feel heavy for small apps.
**Best for:** Stable, long‑lived Android apps with multiple modules.

**Option B — Koin**  
**Pros:**
- Pure Kotlin DSL; very fast to wire; minimal boilerplate.
- Great for prototyping and iterating on architecture.
**Cons:**
- Runtime resolution (vs compile‑time); errors are discovered later.
- Performance overhead on cold start vs generated graphs (usually minor).
**Best for:** Rapid development and smaller teams.

**Recommendation:** *Hilt* if you plan a multi‑module codebase + external contributors; *Koin* if you optimize for speed of iteration during MVP.

---

### 25.2 Networking
**Option A — Retrofit + OkHttp**  
**Pros:** Battle‑tested; interceptors; converters (kotlinx‑serialization, Moshi); great error handling.  
**Cons:** REST‑centric; GraphQL needs extra libs.  
**Best for:** Clean REST/gRPC gateway.

**Option B — Ktor Client**  
**Pros:** Multiplatform; unified server/client stack; good for custom protocols/websockets.  
**Cons:** Fewer turnkey examples vs Retrofit.  
**Best for:** If you also use Ktor server or plan multiplatform.

**Recommendation:** *Retrofit + OkHttp* for Android‑only client; switch to *Ktor* if you adopt Ktor server.

---

### 25.3 JSON Serialization
**Option A — kotlinx‑serialization**  
**Pros:** First‑party Kotlin; good with sealed classes (great for event sourcing).  
**Cons:** Requires `@Serializable` annotations; polymorphism config can be nuanced.

**Option B — Moshi**  
**Pros:** Mature; good adapters; no code‑gen required (but optional).  
**Cons:** Sealed class support less ergonomic than kotlinx‑serialization.

**Recommendation:** *kotlinx‑serialization* (pairs well with sealed domain events).

---

### 25.4 Persistence (Local)
**Option A — Room + SQLCipher**  
**Pros:** Official; compile‑time queries; migrations; encryption with SQLCipher.  
**Cons:** Verbose entities/DAOs; complex query models for event stores.

**Option B — Realm Kotlin**  
**Pros:** Object DB; reactive flows; simple models; optional Device Sync later.  
**Cons:** Vendor lock‑in; less SQL control; binary size.

**Option C — SQLite Direct (SQLDelight) + SQLCipher**  
**Pros:** Type‑safe SQL; excellent control/perf; multiplatform.  
**Cons:** More schema management overhead; steeper learning curve.

**Recommendation:** *Room + SQLCipher* (fast onboarding). Consider *SQLDelight* if you want tighter control and portability.

---

### 25.5 Cloud & Sync
**Option A — Firebase (Auth + Firestore/Storage + Functions)**  
**Pros:** Turnkey auth; offline cache; triggers for AI gateway; generous free tier.  
**Cons:** Document DB quirks (query limits); vendor lock‑in.

**Option B — Supabase (Auth + Postgres + Storage + Functions)**  
**Pros:** SQL + Row Level Security; great for event logs; open source.  
**Cons:** Offline cache story is DIY; mobile SDK maturity varies.

**Option C — Self‑hosted (Postgres + Hasura/Ktor)**  
**Pros:** Full control; best for advanced analytics & costs at scale.  
**Cons:** Ops overhead; security & monitoring are on you.

**Recommendation:** *Firebase* for MVP; reevaluate to *Supabase/Postgres* if you outgrow Firestore querying.

---

### 25.6 AI Inference (On‑device)
**Option A — ONNX Runtime Mobile**  
**Pros:** Broad hardware accel; good for classifiers/NLU; AARs easy to integrate.  
**Cons:** Smaller model zoo for chat‑style LLMs vs GGUF.

**Option B — llama.cpp JNI (GGML/GGUF)**  
**Pros:** Popular for small LLMs; quantized models; active community.  
**Cons:** JNI integration; binary size; thermal constraints on phones.

**Option C — ML Kit + Custom TF Lite**  
**Pros:** Easy distribution; good tooling; AOT quantization.  
**Cons:** LLM support limited; best for NLU/ASR not full chat.

**Recommendation:** *ONNX* for **intent parsing & classifiers**; *llama.cpp* for **micro‑narration** if needed. Keep remote LLM optional behind a gateway.

---

### 25.7 AI Gateway (Remote, Optional)
**Option A — Ktor Server (self‑host) + Token Bucket**  
**Pros:** Full control; custom prompts/guardrails; caching.  
**Cons:** You manage scaling and secrets.

**Option B — Firebase Functions as proxy**  
**Pros:** Zero server management; integrates with Firebase Auth/RTDB.  
**Cons:** Cold starts; request time limits.

**Recommendation:** *Ktor Server* if you already operate infra; otherwise *Firebase Functions* for simplicity.

---

### 25.8 Map Rendering Engine
**Option A — Jetpack Compose Canvas (pure Compose)**  
**Pros:** Single UI toolkit; minimal deps; easy state integration; good for simple 2D grid/tokens.  
**Cons:** Less raw performance than dedicated engines for very large maps or particle effects.

**Option B — LibGDX (via Android launcher)**  
**Pros:** High‑perf 2D; pathfinding/utils libs; battle‑tested.  
**Cons:** Two UI stacks (LibGDX scene2d vs Compose); integration complexity.

**Option C — Skia/Compose Multiplatform Canvas**  
**Pros:** Portable rendering if you target desktop later.  
**Cons:** Adds complexity without immediate Android benefit.

**Recommendation:** *Compose Canvas* (MVP). Switch to *LibGDX* only if you hit perf ceilings or need advanced effects.

---

### 25.9 Pathfinding & Geometry
**Option A — Custom A* + simple ray cast**  
**Pros:** Minimal; easy to tune; deterministic.  
**Cons:** You own edge cases (corner cutting, tie‑breakers).

**Option B — LibGDX AI / gdx‑ai (if using LibGDX)**  
**Pros:** Ready‑made graphs & heuristics.  
**Cons:** Coupled to LibGDX.

**Recommendation:** *Custom A* + ray casting* in Kotlin for independence.

---

### 25.10 Vector Search for AI Memory
**Option A — SQLite FTS5 + cosine on stored embedding vectors**  
**Pros:** Zero extra binary; works offline; adequate for small memories.  
**Cons:** No ANN; slower on large sets.

**Option B — FAISS‑lite (JNI)**  
**Pros:** Fast ANN; scalable.  
**Cons:** Native build; larger apk.

**Recommendation:** Start with *SQLite FTS5*; migrate to *FAISS* if memories exceed ~50k chunks.

---

### 25.11 Logging & Analytics
**Option A — Firebase Analytics + Crashlytics**  
**Pros:** Easy setup; realtime crash signals.  
**Cons:** External service; ensure privacy toggles.

**Option B — Open source (Sentry self‑host/OSS)**  
**Pros:** Control over data; self‑host option.  
**Cons:** Ops overhead.

**Recommendation:** *Crashlytics* + opt‑in analytics; provide local log export for privacy‑focused users.

---

### 25.12 Testing
**Option A — JUnit5 + kotest + MockK + Turbine (Flow)**  
**Pros:** Expressive; great property‑based testing.  
**Cons:** More dependencies.

**Option B — Plain JUnit + Mockito**  
**Pros:** Simple; fewer deps.  
**Cons:** Less expressive for Kotlin coroutines & Flows.

**Recommendation:** *kotest + MockK* for Kotlin ergonomics; add Paparazzi for Compose screenshots.

---

### 25.13 Build System
**Option A — Gradle Kotlin DSL + Version Catalog**  
**Pros:** Type‑safe, centralized versions.  
**Cons:** Initial setup effort.

**Recommendation:** *Gradle Kotlin DSL* (no serious alternative).

---

### 25.14 Security
**Option A — SQLCipher + BiometricPrompt**  
**Pros:** Encrypt at rest; smooth UX.  
**Cons:** Key mgmt complexity (solve with Android Keystore).

**Recommendation:** Use *Android Keystore* to wrap SQLCipher key; PIN/biometric unlock toggle.

---

### 25.15 Default MVP Stack (Suggested)
- **DI:** Koin (swap to Hilt by v1.0 if team grows)
- **UI/Map:** Jetpack Compose + Compose Canvas
- **Networking:** Retrofit + OkHttp
- **Serialization:** kotlinx‑serialization
- **Persistence:** Room + SQLCipher (event‑sourced `Event` table as designed)
- **Sync:** Firebase (Auth + Storage; Functions for AI proxy)
- **AI On‑device:** ONNX Runtime (intent) + optional llama.cpp micro‑narration
- **AI Remote:** Ktor or Firebase Functions gateway (feature‑flagged)
- **Vector Memory:** SQLite FTS5 (upgrade path to FAISS)
- **Testing:** kotest + MockK + Paparazzi
- **Analytics:** Crashlytics + opt‑in Analytics

**Why this default?** Fast to build, offline‑first, minimal native dependencies, and easy to migrate components as needs evolve.

---

### 25.16 Migration Paths
- **Koin → Hilt:** Introduce Hilt in parallel modules; migrate feature by feature.
- **Compose Canvas → LibGDX:** Isolate render commands behind `MapRenderer` interface; keep model pure Kotlin.
- **SQLite FTS5 → FAISS:** Add JNI module; reindex embeddings with background worker.
- **Firebase → Supabase/Postgres:** Add sync adapter layer; write dual‑writer until cutover.

---

---

## 26) Final Picks & Implementation Blueprint (Tailored)

This section converts the recommendations in §25.15 into concrete choices, module layout, Gradle setup, and first implementation stubs you can paste into a repo.

### 26.1 Chosen Stack (MVP)
- **DI:** Koin
- **UI/Map:** Jetpack Compose + Compose Canvas
- **Networking:** Retrofit + OkHttp
- **Serialization:** kotlinx‑serialization
- **Persistence:** Room + SQLCipher
- **Sync:** Firebase (Auth + Cloud Storage + Functions proxy)
- **AI (on‑device):** ONNX Runtime Mobile for intent/NLU; (optional) llama.cpp JNI for micro‑narration behind feature flag
- **AI (remote):** Firebase Functions or Ktor gateway (env‑switchable)
- **Vector Memory:** SQLite FTS5 (upgrade path to FAISS)
- **Testing:** kotest + MockK + Paparazzi
- **Crash/Analytics:** Crashlytics + opt‑in Analytics

---

### 26.2 Project Structure
```
questweaver/
  settings.gradle.kts
  build.gradle.kts                     // root
  gradle/libs.versions.toml            // version catalog
  app/                                 // Android app & DI assemble
  core/domain/                         // use cases, entities, sealed actions/events
  core/data/                           // repositories, Room, DAOs
  core/rules/                          // deterministic rules engine
  feature/map/                         // map UI + geometry + pathfinding
  feature/encounter/                   // turn engine, initiative, combat screen
  feature/character/                   // PC sheet, AI party mgmt
  ai/ondevice/                         // ONNX models + wrappers
  ai/gateway/                          // Retrofit API + DTOs
  sync/firebase/                       // Firebase glue (auth, backup)
  common/testing/                      // test utils, fixtures
```

---

### 26.3 Version Catalog (gradle/libs.versions.toml)
```toml
[versions]
androidGradle = "8.13.1"
kotlin = "1.9.24"
composeBom = "2024.06.00"
coroutines = "1.8.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
serialization = "1.6.3"
room = "2.6.1"
work = "2.9.0"
koin = "3.5.6"
onnx = "1.16.3"
firebaseBom = "33.1.2"
crashlyticsGradle = "2.9.9"
sqlcipher = "4.5.5"
sqlite = "2.4.0"
paparazzi = "1.3.3"
kotest = "5.9.1"
mockk = "1.13.10"

[libraries]
# Kotlin + coroutines
kotlinx-coroutines = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-activity = { module = "androidx.activity:activity-compose", version = "1.9.0" }

# DI
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }

# Networking
retrofit-core = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-kotlinx = { module = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter", version = "1.0.0" }
okhttp-core = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

# Room + SQLCipher
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
sqlcipher = { module = "net.zetetic:android-database-sqlcipher", version.ref = "sqlcipher" }
sqlite-ktx = { module = "androidx.sqlite:sqlite-ktx", version.ref = "sqlite" }

# WorkManager
work-runtime = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }

# ONNX
onnx-runtime = { module = "com.microsoft.onnxruntime:onnxruntime-android", version.ref = "onnx" }

# Firebase (BOM-managed)
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { module = "com.google.firebase:firebase-auth" }
firebase-storage = { module = "com.google.firebase:firebase-storage" }
firebase-functions = { module = "com.google.firebase:firebase-functions" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics" }

# Testing
kotest-runner = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest" }
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
paparazzi = { module = "app.cash.paparazzi:paparazzi", version.ref = "paparazzi" }

[plugins]
android-app = { id = "com.android.application", version.ref = "androidGradle" }
android-lib = { id = "com.android.library", version.ref = "androidGradle" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "crashlyticsGradle" }
``` 

---

### 26.4 Root build.gradle.kts (highlights)
```kotlin
plugins { /* none in root */ }

buildscript { repositories { google(); mavenCentral() } }

allprojects { repositories { google(); mavenCentral() } }

subprojects {
  pluginManager.withPlugin("org.jetbrains.kotlin.android") {
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
      jvmToolchain(17)
    }
  }
}
```

---

### 26.5 App module build.gradle.kts (core setup)
```kotlin
plugins {
  alias(libs.plugins.android.app)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.crashlytics)
}

android {
  namespace = "dev.questweaver.app"
  compileSdk = 34
  defaultConfig {
    applicationId = "dev.questweaver"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
  packaging { resources.excludes += "META-INF/**" }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.activity)
  debugImplementation(libs.compose.ui.tooling)

  implementation(libs.kotlinx.coroutines)
  implementation(libs.serialization.json)

  implementation(libs.koin.android)

  implementation(libs.retrofit.core)
  implementation(libs.retrofit.kotlinx)
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.logging)

  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  kapt(libs.room.compiler)
  implementation(libs.sqlcipher)
  implementation(libs.sqlite.ktx)

  implementation(libs.work.runtime)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.functions)
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.crashlytics)

  implementation(project(":core:domain"))
  implementation(project(":core:data"))
  implementation(project(":core:rules"))
  implementation(project(":feature:map"))
  implementation(project(":feature:encounter"))
  implementation(project(":feature:character"))
  implementation(project(":ai:ondevice"))
  implementation(project(":ai:gateway"))
  implementation(project(":sync:firebase"))
}
```

> **Note:** If you prefer KSP over KAPT for Room, switch to the Room KSP artifact and apply KSP plugin.

---

### 26.6 SQLCipher with Room (Database + Passphrase)
```kotlin
// core/data/src/main/java/.../AppDatabase.kt
@Database(
  entities = [EventEntity::class, CreatureEntity::class, MapEntity::class /*...*/],
  version = 1,
  exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun eventDao(): EventDao
  // ...
}

// Provide encrypted Room instance
fun provideDatabase(ctx: Context, passphrase: ByteArray): AppDatabase {
  val factory = net.sqlcipher.database.SupportFactory(passphrase)
  return Room.databaseBuilder(ctx, AppDatabase::class.java, "questweaver.db")
    .openHelperFactory(factory)
    .fallbackToDestructiveMigrationOnDowngrade()
    .build()
}
```

**Key management:** generate a random 256‑bit key, wrap it with Android Keystore; unlock via PIN/biometric using `BiometricPrompt`.

---

### 26.7 Minimal Entities & DAOs (Event Sourcing)
```kotlin
@Entity(tableName = "events")
data class EventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sessionId: Long,
  val idx: Int,
  val type: String,
  val payload: String, // JSON via kotlinx-serialization
  val ts: Long
)

@Dao
interface EventDao {
  @Query("SELECT * FROM events WHERE sessionId = :sessionId ORDER BY idx ASC")
  suspend fun forSession(sessionId: Long): List<EventEntity>

  @Insert
  suspend fun insertAll(events: List<EventEntity>)
}
```

---

### 26.8 Koin DI Setup
```kotlin
// app/src/main/java/.../QuestWeaverApp.kt
class QuestWeaverApp : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@QuestWeaverApp)
      modules(listOf(coreModule, dataModule, rulesModule, aiModule, mapModule, encounterModule, syncModule))
    }
  }
}

// app/src/main/AndroidManifest.xml -> android:name=".QuestWeaverApp"
```

```kotlin
// core/data/di/DataModule.kt
val dataModule = module {
  single { provideDatabase(get(), get(named("dbKey"))) }
  single<EventRepository> { EventRepositoryImpl(get()) }
  // ...
}

// Provide dbKey at runtime after unlock
```

---

### 26.9 Retrofit Gateway (LLM proxy)
```kotlin
interface AIGatewayApi {
  @POST("v1/narrate")
  suspend fun narrate(@Body req: NarrateReq): NarrateResp

  @POST("v1/intent")
  suspend fun intent(@Body req: IntentReq): IntentResp
}

fun provideRetrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
  .baseUrl(baseUrl)
  .addConverterFactory(
    kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
      .let { com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory("application/json".toMediaType()) }
  )
  .client(client)
  .build()
```

---

### 26.10 ONNX Runtime Wrapper (Intent Parsing)
```kotlin
class IntentClassifier(context: Context) {
  private val env = OrtEnvironment.getEnvironment()
  private val session: OrtSession
  init {
    val modelBytes = context.assets.open("models/intent.onnx").readBytes()
    session = env.createSession(modelBytes)
  }
  fun classify(text: String): IntentLabel { /* tokenize → run → argmax */ }
}
```

Ship a tiny intent model (e.g., 100–300KB quantized) for offline first‑pass.

---

### 26.11 Map Renderer: Interface + Compose Canvas Impl
```kotlin
interface MapRenderer {
  fun draw(state: MapState, drawScope: DrawScope)
}

data class MapState(
  val w: Int, val h: Int, val tileSize: Float,
  val blocked: Set<GridPos>, val difficult: Set<GridPos>,
  val tokens: List<Token>
)

data class Token(val id: Id, val pos: GridPos, val isEnemy: Boolean, val hpPct: Float)

data class GridPos(val x: Int, val y: Int)
```

```kotlin
@Composable
fun TacticalMap(state: MapState, onTap: (GridPos) -> Unit, modifier: Modifier = Modifier) {
  Canvas(modifier.pointerInput(Unit) {
    detectTapGestures { offset ->
      val gx = (offset.x / state.tileSize).toInt(); val gy = (offset.y / state.tileSize).toInt()
      onTap(GridPos(gx, gy))
    }
  }) {
    // grid
    for (x in 0..state.w) drawLine( start = Offset(x*state.tileSize, 0f), end = Offset(x*state.tileSize, size.height))
    for (y in 0..state.h) drawLine( start = Offset(0f, y*state.tileSize), end = Offset(size.width, y*state.tileSize))
    // blocked tiles
    state.blocked.forEach { (x,y) ->
      drawRect(topLeft = Offset(x*state.tileSize, y*state.tileSize), size = Size(state.tileSize, state.tileSize), alpha = 0.2f)
    }
    // tokens
    state.tokens.forEach { t ->
      val cx = t.pos.x*state.tileSize + state.tileSize/2
      val cy = t.pos.y*state.tileSize + state.tileSize/2
      drawCircle(center = Offset(cx, cy), radius = state.tileSize*0.35f)
      // HP bar
      drawRect( topLeft = Offset(cx - state.tileSize*0.4f, cy + state.tileSize*0.45f), size = Size(state.tileSize*0.8f * t.hpPct, state.tileSize*0.1f))
    }
  }
}
```

---

### 26.12 Pathfinding (A* skeleton)
```kotlin
fun aStar(start: GridPos, goal: GridPos, grid: MapGrid, blocked: Set<GridPos>): List<GridPos> {
  // standard A* with 4/8-way movement; disallow diagonal corner cutting
}
```

---

### 26.13 WorkManager: Cloud Backup Job
```kotlin
class BackupWorker(appCtx: Context, params: WorkerParameters) : CoroutineWorker(appCtx, params) {
  override suspend fun doWork(): Result {
    // query events since last sync → write bundle to Firebase Storage
    return Result.success()
  }
}

fun scheduleBackups(ctx: Context) {
  WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
    "backup", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.HOURS).build()
  )
}
```

---

### 26.14 Crashlytics & Analytics (opt‑in)
- Show toggle on first run; store consent in DataStore.
- Wrap `Firebase.analytics.logEvent(...)` behind `Analytics.log(...)` that no‑ops when disabled.

---

### 26.15 App Startup Checklist
1. Splash → decrypt DB key (PIN/biometric) → init Koin → load last `UiState` from event replay.
2. Warm ONNX session on a background thread (lifecycle aware).
3. If user opted into cloud: `scheduleBackups()`.

---

### 26.16 MVP Backlog (2–3 sprints)
- **Sprint 1:** Room schema + event log; Map Canvas with grid + token render; A*; basic PC import; dice roller.
- **Sprint 2:** Turn engine, attacks/saves in Rules; initiative UI; AI allies (BT minimal); journal log.
- **Sprint 3:** ONNX intent parser; Retrofit gateway stub; encrypted export/import; Crashlytics + opt‑in analytics.

---

### 26.17 Env & Feature Flags
- `BuildConfig.AI_REMOTE_ENABLED`, `AI_ONDEVICE_ENABLED`, `ANALYTICS_ENABLED` (persisted toggle).
- `GatewayBaseUrl` injected via Koin; dev/staging/prod flavors.

---

### 26.18 Coding Standards
- Sealed classes for actions/effects/events; exhaustive `when`.
- `UiState` immutable; single source of truth; no imperative map mutations.
- All randomness via seeded RNG service; log every roll in `Event`.

---

*End of Spec*

