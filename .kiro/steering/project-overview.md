# QuestWeaver Project Overview

**Product Name:** QuestWeaver  
**Platform:** Android (API 26+)  
**Architecture:** Clean Architecture + MVI + Event Sourcing

## Mission
Deliver a solo-friendly D&D-style experience with an AI GM that handles NPCs, scene narration, rules adjudication (SRD-compatible), and combat with a simple tactical map.

## Core Principles

### 1. Offline-First
- All core gameplay works without network
- On-device AI for intent parsing and basic narration
- Optional cloud sync for backups only

### 2. Deterministic & Reproducible
- Event-sourced architecture for full replay capability
- Seeded RNG for all randomness
- Every dice roll logged with modifiers and context

### 3. Modular & Swappable
- Clear boundaries between Rules, AI, Map, Data, UI
- Dependency injection for easy component replacement
- Interface-driven design for testability

### 4. Simple Tactical Map
- Focus on clarity over VTT-grade features
- Grid + tokens + walls + range overlays
- No photorealistic rendering or complex lighting

### 5. Privacy & Security
- Local encryption (SQLCipher) for saves
- Opt-in analytics and crash reporting
- No 3rd-party data sharing without consent

## Tech Stack (MVP)

- **Language:** Kotlin 100%, Coroutines + Flow
- **UI:** Jetpack Compose (including map via Canvas)
- **DI:** Koin
- **Persistence:** Room + SQLCipher
- **Networking:** Retrofit + OkHttp
- **Serialization:** kotlinx-serialization
- **Cloud:** Firebase (Auth + Storage + Functions)
- **AI On-device:** ONNX Runtime Mobile
- **AI Remote:** Firebase Functions or Ktor gateway (optional)
- **Testing:** kotest + MockK + Paparazzi

## Module Structure

```
questweaver/
├── app/                    # Android app & DI assembly
├── core/
│   ├── domain/            # Use cases, entities, sealed events
│   ├── data/              # Repositories, Room, DAOs
│   └── rules/             # Deterministic rules engine
├── feature/
│   ├── map/               # Map UI + geometry + pathfinding
│   ├── encounter/         # Turn engine, combat screen
│   └── character/         # PC sheet, AI party management
├── ai/
│   ├── ondevice/          # ONNX models + wrappers
│   └── gateway/           # Retrofit API + DTOs
└── sync/
    └── firebase/          # Firebase integration
```

## Key Constraints

### Non-Goals (v1)
- Photorealistic maps or VTT-grade features
- Full copyrighted 5e content (SRD-compatible only)
- Multiplayer over network
- Complex lighting or dynamic LoS

### Performance Budget
- Map render: ≤4ms on mid-tier device
- AI tactical decision: ≤300ms on-device
- LLM narration (remote): 4s soft timeout

## Development Phases

**MVP (8-10 weeks)**
- Core rules engine, map grid, single encounter flow
- PC import, AI allies/enemies (minimal BT)
- Local saves, basic narration

**v1.0**
- Cloud backup, dialogue agent
- Homebrew import/export, cover rules
- Journal auto-summaries

**v1.1+**
- Advanced tactics, faction memory, quests
- Map editor, condition icons, difficulty sliders
