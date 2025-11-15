---
inclusion: always
---

# Memory MCP Usage for QuestWeaver

**Purpose**: Persistent cross-session knowledge storage via knowledge graph (entities, observations, relations).

## Storage Decision Framework

Store information ONLY when ALL three conditions are met:

1. **Durable**: Useful beyond current conversation
2. **Significant**: Affects identity, preferences, projects, or goals  
3. **Factual**: Not hypothetical, speculative, or temporary

## Entity Types & Usage

| Type | Use For | QuestWeaver Examples |
|------|---------|---------------------|
| `user` | Developer identity, preferences | "Prefers MVI pattern for ViewModels" |
| `project` | Active projects | QuestWeaver (Android RPG app) |
| `technology` | Languages, frameworks | Kotlin, Compose, Room, Koin |
| `architecture` | Patterns, decisions | MVI, Event Sourcing, Clean Architecture |
| `component` | Modules | core:domain, feature:map, ai:ondevice |
| `tool` | Development tools | Gradle, Android Studio, Git |

## What to Store for QuestWeaver

✅ **DO STORE:**
- Module dependency rules (e.g., "feature modules cannot depend on other features except encounter→map")
- Architecture patterns (MVI, Event Sourcing, Clean Architecture)
- Performance budgets (map render ≤4ms, AI ≤300ms)
- Technology stack decisions (Kotlin, Compose, Room, Koin)
- Security requirements (SQLCipher encryption)
- Testing targets (coverage: core/rules 90%+, domain 85%+)
- Design principles (offline-first, deterministic, event-sourced)
- Ongoing TODOs and milestones
- User preferences for patterns and tools

❌ **DO NOT STORE:**
- Troubleshooting steps or error messages
- Temporary debugging states
- Sensitive data (passwords, API keys, PII)
- Conversation text or chat history
- Ephemeral file paths or line numbers
- Hypothetical scenarios ("What if I used X?")

## Session Start Protocol

**At every session start:**

1. `search_nodes("QuestWeaver")` → Load project context and briefly acknowledge what was loaded
2. `search_nodes("User")` → Load user preferences and briefly acknowledge what was loaded
3. Use loaded context naturally in responses
4. **Always acknowledge memory operations** with brief, natural confirmations

## Tool Usage Patterns

### create_entities
**When**: New project introduced, new technology adopted long-term, user entity missing

**Always search first** to avoid duplicates:
```
search_nodes("ProjectName") → If not found → create_entities
```

### add_observations
**When**: Preference stated, decision made, constraint introduced, TODO added, progress reported

**Examples for QuestWeaver:**
- "Module dependency rule: core:domain must be pure Kotlin with no Android dependencies"
- "Performance budget: Map render ≤4ms per frame"
- "TODO: Implement tactical AI decision engine"

### create_relations
**When**: Connecting entities with meaningful relationships

**Common QuestWeaver relations:**
- User `works_on` QuestWeaver
- QuestWeaver `built_with` Kotlin
- QuestWeaver `implements` Event Sourcing
- QuestWeaver `contains` core:domain
- feature:encounter `depends_on` feature:map

**Use active voice verbs**: `works_on`, `uses`, `built_with`, `implements`, `contains`, `depends_on`

### search_nodes
**Preferred retrieval method** - use instead of `read_graph` for targeted queries

**When**: Session start, before creating entities, when user asks about memory

### delete_*
**Only when**: User explicitly requests deletion, memory contradicted by new information, project abandoned

## User Interaction Style

**When storing:**
- Always provide brief, natural confirmation: "Got it, I'll remember QuestWeaver uses Koin for DI."
- Avoid showing raw JSON, entity IDs, or technical details
- Acknowledge what was stored in human-readable terms

**When retrieving:**
- Briefly mention when loading from memory: "Loading your QuestWeaver project context..."
- Use context naturally: "Based on your QuestWeaver project, which uses MVI and Event Sourcing..."
- Provide human-readable summaries when asked

**When user asks "What do you know about my projects?":**
```
You're working on:
- QuestWeaver: Android RPG app with AI Game Master, built with Kotlin
  - Uses Clean Architecture + MVI + Event Sourcing
  - Key constraint: Offline-first design
  - TODO: Implement tactical AI decision engine
```

## Validation Checklist

Before storing, verify:
- [ ] Durable (useful in future sessions)?
- [ ] Significant (affects identity/preferences/projects/goals)?
- [ ] Factual (not hypothetical)?
- [ ] No sensitive data (passwords, PII)?
- [ ] Searched for duplicates?
- [ ] Appropriate entity type and structure?

**If ANY check fails → DO NOT STORE**

## Privacy Rules

**NEVER store:**
- Passwords, API keys, tokens, secrets
- PII (emails, phone numbers, addresses)
- Financial or health information
- Raw error logs with sensitive paths
- Conversation transcripts

## Quick Reference

**Session start**: `search_nodes` for project + user → Acknowledge loading → Use naturally  
**New project**: Search first → `create_entities` → `create_relations` (user works_on project)  
**Decision made**: `add_observations` to project entity  
**Preference stated**: `add_observations` to user entity  
**When in doubt**: Don't store - better to ask again than clutter memory

---

**Last Updated**: November 14, 2025
