# QuestWeaver Product Overview

**Platform:** Android (API 26+) | **Type:** Solo D&D-style RPG with AI Game Master

## Product Vision

QuestWeaver is an offline-first Android app delivering single-player D&D experiences. One human player controls their PC while AI manages NPCs, party members, narration, and rules adjudication. Features a simple tactical map for grid-based combat.

## Core Capabilities

- **AI Game Master**: Scene narration, NPC dialogue, rules adjudication, combat tactics
- **Tactical Combat**: Grid-based map with tokens, movement, attacks, spells, conditions
- **Event Sourcing**: Full campaign replay with deterministic outcomes
- **Offline-First**: Core gameplay without network; optional cloud sync
- **SRD Rules**: D&D 5e-compatible mechanics with homebrew support

## Design Principles

When implementing features or making decisions:

1. **Solo-Friendly First**: Design for one player; AI handles all other characters
2. **Deterministic Core**: Use seeded RNG; ensure reproducible outcomes
3. **Offline Capable**: Core features must work without network connectivity
4. **Privacy-First**: Local encryption by default; opt-in for cloud features
5. **Clarity Over Complexity**: Simple, clear UI over VTT-grade features
6. **Modular Design**: Swappable components (AI models, rules engines, storage)

## Scope Boundaries

### In Scope
- SRD-compatible D&D 5e mechanics
- Grid-based tactical combat with basic features
- On-device AI for intent parsing and basic narration
- Local encrypted storage with optional cloud backup
- Single-player campaign management

### Out of Scope (v1)
- Photorealistic maps or advanced VTT features (dynamic lighting, fog of war)
- Full copyrighted 5e content (use SRD only)
- Multiplayer or networked gameplay
- Complex line-of-sight or lighting systems
- Real-time voice/video integration

## Feature Decision Framework

When evaluating new features or changes, ask:

1. **Does it work offline?** Core features must function without network
2. **Is it deterministic?** Can outcomes be reproduced from event log?
3. **Does it respect privacy?** Is data encrypted locally by default?
4. **Is it simple enough?** Does it maintain clarity over complexity?
5. **Is it SRD-compatible?** Avoid copyrighted content

## User Experience Priorities

1. **Immediate Playability**: Minimize setup; get into gameplay quickly
2. **Clear Feedback**: Always show what's happening and why
3. **Graceful Degradation**: Fallback to templates when AI unavailable
4. **Responsive UI**: Maintain 60fps on mid-tier devices
5. **Transparent AI**: Clearly indicate AI-generated content

## Technical Constraints

- **Min SDK**: API 26 (Android 8.0+)
- **Performance**: Map render ≤4ms, AI decisions ≤300ms on-device
- **Storage**: Local SQLCipher encryption required
- **AI**: On-device ONNX for critical path; optional remote for rich narration
- **Rules**: 100% deterministic; no AI in rules engine

---

**Last Updated**: 2025-11-10
