---
inclusion: always
---

# QuestWeaver Project Overview

**Platform:** Android (API 26+) | **Language:** Kotlin 100%  
**Architecture:** Clean Architecture + MVI + Event Sourcing

## What This Project Is

QuestWeaver is a solo D&D-style RPG where one player controls their PC while AI manages NPCs, narration, and rules. Features grid-based tactical combat with offline-first design.

## Critical Design Constraints

When implementing features, these constraints are non-negotiable:

1. **Offline-First**: Core gameplay must work without network. Cloud sync is optional only.

2. **Deterministic**: All randomness uses seeded RNG. Same events must produce same outcomes for replay.

3. **Event-Sourced**: Every state mutation produces immutable `GameEvent` instances. State is derived, never mutated directly.

4. **Module Boundaries**: 
   - `core/domain` and `core/rules` are pure Kotlin (NO Android dependencies)
   - Feature modules cannot depend on other features (except `encounter` → `map`)
   - Rules engine is 100% deterministic (NO AI calls)

5. **Privacy**: Local data encrypted with SQLCipher. No PII in logs.

## Tech Stack

- **UI:** Jetpack Compose + Material3 (Canvas for map rendering)
- **DI:** Koin 3.5.6 (pure Kotlin DSL)
- **Async:** Coroutines 1.8.1 + Flow
- **Database:** Room 2.6.1 + SQLCipher 4.5.5
- **Network:** Retrofit 2.11.0 + OkHttp 4.12.0
- **Serialization:** kotlinx-serialization 1.6.3
- **AI On-device:** ONNX Runtime 1.16.3
- **Testing:** kotest 5.9.1 + MockK 1.13.10

## Module Architecture

```
app/                    # DI assembly, navigation, theme
core/
  domain/              # Pure Kotlin: entities, use cases, events (NO Android)
  data/                # Room + SQLCipher, repository implementations
  rules/               # Deterministic rules engine (NO Android, NO AI)
feature/
  map/                 # Compose Canvas, pathfinding, geometry
  encounter/           # Turn engine, combat UI (depends on feature:map)
  character/           # Character sheets, party management
ai/
  ondevice/            # ONNX Runtime for intent parsing
  gateway/             # Retrofit client for remote LLM (optional)
sync/
  firebase/            # Cloud backup via WorkManager
```

## Dependency Rules (Strictly Enforced)

**Allowed:**
- `app` → all modules
- `feature/*` → `core:domain`, `core:rules`
- `feature/encounter` → `feature:map` (ONLY exception)
- `core:data` → `core:domain`
- `ai/*`, `sync/*` → `core:domain`

**Forbidden:**
- `core:domain` → any other module
- `core:rules` → Android dependencies or AI
- `feature/*` → other `feature/*` (except encounter→map)
- Circular dependencies

## Performance Budgets

- Map render: ≤4ms per frame (60fps target)
- AI tactical decision: ≤300ms on-device
- LLM narration: 4s soft timeout, 8s hard timeout
- Database queries: <50ms typical

## Scope Boundaries

**In Scope:**
- SRD-compatible D&D 5e mechanics
- Grid-based tactical combat (basic features)
- On-device AI for intent parsing
- Local encrypted storage + optional cloud backup

**Out of Scope (v1):**
- Photorealistic maps or VTT-grade features
- Full copyrighted 5e content (SRD only)
- Multiplayer or networked gameplay
- Complex lighting or dynamic line-of-sight

## Key Patterns to Follow

**MVI State Management:**
- Single immutable `UiState` data class
- Sealed `Intent` interface for user actions
- `StateFlow` for reactive state
- Unidirectional data flow

**Event Sourcing:**
- All mutations produce `GameEvent` instances
- Events are immutable and logged to database
- State derived from event replay
- Use sealed interfaces for event hierarchies

**Repository Pattern:**
- Interfaces in `core:domain`
- Implementations in `core:data`
- Return `Flow` for reactive queries
- Return `suspend fun` for one-shot operations

## When Making Decisions

Ask these questions:

1. **Does it work offline?** Core features must function without network.
2. **Is it deterministic?** Can outcomes be reproduced from event log?
3. **Does it respect module boundaries?** Check dependency rules above.
4. **Is it SRD-compatible?** Avoid copyrighted content.
5. **Does it maintain performance budgets?** Check targets above.
