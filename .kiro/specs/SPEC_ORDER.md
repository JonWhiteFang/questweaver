# QuestWeaver Spec Order

This document defines the ordered list of specs that need to be created and completed to deliver QuestWeaver v1.0. Each spec builds on previous ones, following a logical dependency chain from foundational infrastructure to complete features.

## Phase 1: Foundation (Weeks 1-2)

### ✅ 1. Project Setup & Build Configuration [COMPLETED]
**Directory:** `.kiro/specs/01-project-setup`  
**Dependencies:** None  
**Deliverables:**
- Multi-module Gradle project structure
- Version catalog configuration
- Build performance optimization
- CI/CD pipeline setup
- Code quality tools (lint, detekt)

### ✅ 2. Core Domain Models [COMPLETED]
**Directory:** `.kiro/specs/02-core-domain`  
**Dependencies:** 01-project-setup  
**Deliverables:**
- Entity classes (Creature, Campaign, Encounter, MapGrid)
- Event sourcing architecture (GameEvent sealed hierarchy)
- Repository interfaces
- Use case interfaces
- Value objects (GridPos, Abilities, DiceRoll)

### ✅ 3. Database & Persistence Layer [COMPLETED]
**Directory:** `.kiro/specs/03-database-persistence`  
**Dependencies:** 02-core-domain  
**Deliverables:**
- Room database setup with SQLCipher encryption
- Event sourcing implementation (EventDao, EventEntity)
- Repository implementations
- Database migrations
- Encryption key management with Android Keystore

## Phase 2: Rules Engine (Weeks 3-4)

### ✅ 4. Deterministic Dice System [COMPLETED]
**Directory:** `.kiro/specs/04-dice-system`  
**Dependencies:** 02-core-domain  
**Deliverables:**
- Seeded random number generator
- DiceRoller with advantage/disadvantage
- DiceRoll value objects
- Property-based tests for determinism

### ✅ 5. Combat Rules Engine [COMPLETED]
**Directory:** `.kiro/specs/05-combat-rules`  
**Dependencies:** 04-dice-system  
**Deliverables:**
- Attack resolution (to-hit, damage)
- Saving throw mechanics
- Ability check resolution
- Condition effects (prone, stunned, etc.)
- SRD-compatible D&D 5e mechanics

### ✅ 6. Action Validation System [COMPLETED]
**Directory:** `.kiro/specs/06-action-validation`  
**Dependencies:** 05-combat-rules  
**Deliverables:**
- Action legality validation
- Resource tracking (spell slots, movement)
- Turn phase enforcement (action/bonus/reaction)
- Range and line-of-effect validation

## Phase 3: Map & Geometry (Weeks 5-6)

### ✅ 7. Grid System & Geometry [COMPLETED]
**Directory:** `.kiro/specs/07-grid-geometry`  
**Dependencies:** 02-core-domain  
**Deliverables:**
- MapGrid data structure
- GridPos operations (distance, neighbors)
- Line-of-effect calculations
- Range calculations (5ft, 10ft, etc.)
- Area-of-effect templates (cone, sphere, cube)

### ✅ 8. Pathfinding System [COMPLETED]
**Directory:** `.kiro/specs/08-pathfinding`  
**Dependencies:** 07-grid-geometry  
**Deliverables:**
- A* pathfinding implementation
- Movement cost calculation
- Obstacle detection
- Difficult terrain support
- Path validation

### ✅ 9. Tactical Map Rendering [COMPLETED]
**Directory:** `.kiro/specs/09-map-rendering`  
**Dependencies:** 07-grid-geometry, 08-pathfinding  
**Deliverables:**
- Compose Canvas grid renderer
- Token rendering with HP indicators
- Movement path visualization
- Range overlay rendering
- AoE template visualization
- Performance optimization (≤4ms per frame)

## Phase 4: Combat System (Weeks 7-8)

### ✅ 10. Initiative & Turn Management [COMPLETED]
**Directory:** `.kiro/specs/10-initiative-turns`  
**Dependencies:** 02-core-domain, 05-combat-rules  
**Deliverables:**
- Initiative tracker
- Turn order management
- Turn phase tracking (move/action/bonus/reaction)
- Round progression
- Surprise round handling

### ✅ 11. Combat Action Processing [COMPLETED]
**Directory:** `.kiro/specs/11-combat-actions`  
**Dependencies:** 05-combat-rules, 06-action-validation, 10-initiative-turns  
**Deliverables:**
- Attack action processing
- Movement action processing
- Spell casting framework
- Bonus action handling
- Reaction system

### ✅ 12. Encounter State Management [COMPLETED]
**Directory:** `.kiro/specs/12-encounter-state`  
**Dependencies:** 10-initiative-turns, 11-combat-actions  
**Deliverables:**
- EncounterViewModel with MVI pattern
- State persistence via event sourcing
- Encounter creation and initialization
- Encounter completion and cleanup
- State replay from events

## Phase 5: AI Systems (Weeks 9-10)

### 🔄 13. Intent Classification (On-Device) [IN PROGRESS - Tasks 1-11/13 Complete]
**Directory:** `.kiro/specs/13-intent-classification`  
**Dependencies:** 02-core-domain  
**Deliverables:**
- ONNX Runtime integration
- Intent classifier model wrapper
- Tokenizer implementation
- Entity extraction
- Keyword fallback system

**Note:** Initial implementation uses keyword fallback only. ONNX model creation is deferred to post-MVP.

#### ONNX Model Creation (Post-MVP Enhancement)
**Status:** Not required for MVP - keyword fallback is sufficient  
**When to implement:** After v1.0 release, based on user feedback  
**Estimated effort:** 1-2 weeks

**Steps to create ONNX model:**
1. **Data Collection** (3-5 days)
   - Collect 1000+ labeled player commands from real usage
   - Or generate synthetic training data using templates
   - Balance classes (equal examples per intent type)
   - Split: 70% train, 15% validation, 15% test

2. **Model Selection** (1 day)
   - Option A: Custom LSTM (5-10MB, fastest inference)
   - Option B: Fine-tuned DistilBERT (80MB quantized, better accuracy)
   - Recommendation: Start with LSTM for speed

3. **Training** (2-3 days)
   - Set up Python environment (PyTorch/TensorFlow)
   - Train model on collected data
   - Validate accuracy (target: 85%+ on test set)
   - Tune hyperparameters

4. **Export to ONNX** (1 day)
   - Export trained model to ONNX format (opset 13)
   - Quantize to INT8 for size reduction
   - Validate inference works with ONNX Runtime
   - Test on Android device

5. **Integration** (2-3 days)
   - Replace placeholder model file
   - Update vocabulary.json if needed
   - Test end-to-end classification
   - Benchmark performance (target: <300ms)
   - Compare accuracy vs keyword fallback

**Resources needed:**
- Python ML environment (PyTorch/TensorFlow + ONNX)
- Training data (real or synthetic)
- GPU for training (optional but recommended)
- Android device for testing

**Success criteria:**
- 85%+ accuracy on test dataset
- <300ms inference time on mid-tier Android device
- <100MB model size (preferably <80MB)
- Better than keyword fallback in user testing

**Alternative:** If model training is too complex, consider using a pre-trained intent classifier from Hugging Face and fine-tuning on D&D-specific commands.

### 14. Tactical AI Agent
**Directory:** `.kiro/specs/14-tactical-ai`  
**Dependencies:** 05-combat-rules, 06-action-validation, 12-encounter-state  
**Deliverables:**
- Behavior tree implementation
- Action scoring heuristics
- Target selection logic
- Positioning strategy
- Resource management (spell slots, abilities)

### 15. Narrative Generation System
**Directory:** `.kiro/specs/15-narrative-generation`  
**Dependencies:** 02-core-domain, 11-combat-actions  
**Deliverables:**
- Remote LLM gateway (Retrofit API)
- Prompt template system
- Response caching
- Template-based fallback narration
- Content filtering
- Timeout and retry logic

## Phase 6: Character Management (Week 11)

### 16. Character Data Model
**Directory:** `.kiro/specs/16-character-model`  
**Dependencies:** 02-core-domain  
**Deliverables:**
- Extended Creature model for PCs
- Inventory system
- Equipment and attunement
- Spell list management
- Character progression (leveling)

### 17. Character Sheet UI
**Directory:** `.kiro/specs/17-character-sheet`  
**Dependencies:** 16-character-model  
**Deliverables:**
- Character sheet Compose screens
- Ability score display
- Skill and save modifiers
- Inventory management UI
- Spell list UI
- Character import/export

### 18. Party Management
**Directory:** `.kiro/specs/18-party-management`  
**Dependencies:** 16-character-model, 17-character-sheet  
**Deliverables:**
- Party composition UI
- AI party member overview
- Party resource tracking (HP, spell slots)
- Rest mechanics (short/long rest)

## Phase 7: Campaign & Session Management (Week 12)

### 19. Campaign Data Model
**Directory:** `.kiro/specs/19-campaign-model`  
**Dependencies:** 02-core-domain, 16-character-model  
**Deliverables:**
- Campaign entity
- Session tracking
- Journal/log system
- World state persistence
- Campaign settings (difficulty, content rating)

### 20. Campaign UI & Navigation
**Directory:** `.kiro/specs/20-campaign-ui`  
**Dependencies:** 19-campaign-model  
**Deliverables:**
- Campaign creation flow
- Campaign selection screen
- Session history view
- Journal viewer
- Settings screen

### 21. Scene & Dialogue System
**Directory:** `.kiro/specs/21-scene-dialogue`  
**Dependencies:** 15-narrative-generation, 19-campaign-model  
**Deliverables:**
- Scene state management
- NPC dialogue agent
- Conversation history
- Scene transitions
- Dialogue UI

## Phase 8: Integration & Polish (Weeks 13-14)

### 22. Complete Encounter Flow
**Directory:** `.kiro/specs/22-encounter-flow`  
**Dependencies:** 09-map-rendering, 12-encounter-state, 14-tactical-ai, 15-narrative-generation  
**Deliverables:**
- End-to-end encounter flow
- Map + combat UI integration
- AI narration integration
- Victory/defeat conditions
- Loot and XP distribution

### 23. Player Action Processing
**Directory:** `.kiro/specs/23-player-actions`  
**Dependencies:** 13-intent-classification, 11-combat-actions, 21-scene-dialogue  
**Deliverables:**
- Natural language input processing
- Action disambiguation UI
- Context-aware action suggestions
- Action confirmation flow
- Undo/redo support

### 24. Cloud Sync & Backup
**Directory:** `.kiro/specs/24-cloud-sync`  
**Dependencies:** 03-database-persistence, 19-campaign-model  
**Deliverables:**
- Firebase Authentication
- Cloud Storage integration
- WorkManager backup jobs
- Conflict resolution
- Restore from backup

## Phase 9: Testing & Quality (Week 15)

### 25. Comprehensive Test Suite
**Directory:** `.kiro/specs/25-test-suite`  
**Dependencies:** All previous specs  
**Deliverables:**
- Unit tests for all modules (80%+ coverage)
- Integration tests for critical paths
- Property-based tests for rules engine
- UI screenshot tests with Paparazzi
- Performance benchmarks

### 26. Error Handling & Resilience
**Directory:** `.kiro/specs/26-error-handling`  
**Dependencies:** All previous specs  
**Deliverables:**
- Comprehensive error handling
- Graceful degradation for AI failures
- Network error recovery
- Database corruption recovery
- User-friendly error messages

## Phase 10: Release Preparation (Week 16)

### 27. Onboarding & Tutorial
**Directory:** `.kiro/specs/27-onboarding`  
**Dependencies:** 20-campaign-ui, 22-encounter-flow  
**Deliverables:**
- First-time user experience
- Tutorial encounter
- Feature highlights
- Help documentation
- Tips and hints system

### 28. Performance Optimization
**Directory:** `.kiro/specs/28-performance`  
**Dependencies:** All previous specs  
**Deliverables:**
- Map rendering optimization (60fps target)
- Database query optimization
- Memory usage optimization
- APK size reduction
- Battery usage optimization

### 29. Release Build & Distribution
**Directory:** `.kiro/specs/29-release-build`  
**Dependencies:** All previous specs  
**Deliverables:**
- ProGuard configuration
- Release signing setup
- Play Store listing materials
- Privacy policy and terms
- Crash reporting (Firebase Crashlytics)
- Analytics setup (opt-in)

---

## Dependency Graph

```
01 (Project Setup)
├─ 02 (Domain Models)
│  ├─ 03 (Database)
│  ├─ 04 (Dice System)
│  │  └─ 05 (Combat Rules)
│  │     └─ 06 (Action Validation)
│  │        └─ 11 (Combat Actions)
│  │           ├─ 12 (Encounter State)
│  │           │  ├─ 14 (Tactical AI)
│  │           │  └─ 22 (Encounter Flow)
│  │           └─ 15 (Narrative)
│  │              └─ 21 (Scene/Dialogue)
│  ├─ 07 (Grid Geometry)
│  │  ├─ 08 (Pathfinding)
│  │  │  └─ 09 (Map Rendering)
│  │  │     └─ 22 (Encounter Flow)
│  │  └─ 10 (Initiative/Turns)
│  │     └─ 12 (Encounter State)
│  ├─ 13 (Intent Classification)
│  │  └─ 23 (Player Actions)
│  ├─ 16 (Character Model)
│  │  ├─ 17 (Character Sheet)
│  │  │  └─ 18 (Party Management)
│  │  └─ 19 (Campaign Model)
│  │     ├─ 20 (Campaign UI)
│  │     └─ 24 (Cloud Sync)
│  └─ 25 (Test Suite)
│     └─ 26 (Error Handling)
│        └─ 27 (Onboarding)
│           └─ 28 (Performance)
│              └─ 29 (Release Build)
```

---

## Critical Path

The following specs are on the critical path and must be completed in order:

1. **01** → **02** → **05** → **06** → **11** → **12** → **22** → **23** → **29**

These represent the core gameplay loop: setup → domain → rules → actions → state → encounter → player input → release.

---

## Parallel Work Opportunities

The following specs can be worked on in parallel:

- **Phase 2 (Rules)** and **Phase 3 (Map)** can be developed simultaneously
- **Phase 5 (AI)** can start once domain models are complete
- **Phase 6 (Character)** can be developed alongside combat system
- **Testing (25)** should be ongoing throughout all phases

---

## MVP Subset (8-10 weeks)

For a minimal viable product, focus on these specs:

1. 01 (Project Setup)
2. 02 (Domain Models)
3. 03 (Database)
4. 04 (Dice System)
5. 05 (Combat Rules)
6. 07 (Grid Geometry)
7. 09 (Map Rendering)
8. 10 (Initiative/Turns)
9. 11 (Combat Actions)
10. 12 (Encounter State)
11. 14 (Tactical AI)
12. 16 (Character Model)
13. 17 (Character Sheet)
14. 22 (Encounter Flow)

This delivers a playable single-encounter experience with tactical combat and AI opponents.

---

## Notes

- Each spec should follow the standard workflow: Requirements → Design → Tasks
- Specs should be completed in order within each phase
- Dependencies must be satisfied before starting a spec
- Testing should be integrated into each spec, not deferred to Phase 9
- Performance considerations should be addressed during implementation, not as an afterthought

---

**Total Estimated Timeline:** 16 weeks (4 months) for v1.0 release  
**MVP Timeline:** 8-10 weeks for core playable experience
