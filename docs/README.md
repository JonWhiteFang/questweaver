# QuestWeaver Documentation

**Comprehensive documentation for the QuestWeaver project**

## Documentation Structure

### 📐 Architecture
High-level system design and architectural patterns

- [Overview](architecture/overview.md) - Complete architecture and design specification
- [Modules](architecture/modules.md) - Module structure and dependency rules
- [Patterns](architecture/patterns.md) - MVI, Event Sourcing, Repository patterns

### 💻 Development
Developer guides and coding standards

- [Setup](development/setup.md) - Getting started with development
- [Coding Standards](development/coding-standards.md) - Code style and conventions
- [Build & Test](development/build-and-test.md) - Build commands and testing guidelines
- [Koin DI Patterns](development/koin-di-patterns.md) - Dependency injection patterns
- [Quick Reference](development/quick-reference.md) - Fast lookup for common patterns

### 📦 Product
Product vision and design context

- [Overview](product/overview.md) - Product vision, capabilities, and scope

### 🤖 AI Agents
AI agent-specific documentation

- [Agent Guide](ai-agents/agent-guide.md) - Complete guide for AI coding agents
- [Suggested Hooks](ai-agents/suggested-hooks.md) - Recommended agent hooks

### 📚 Modules
Module-specific documentation

- [app/](modules/app/README.md) - Android application module

## Quick Links

### For New Developers
1. Start with [Development Setup](development/setup.md)
2. Read [Architecture Overview](architecture/overview.md)
3. Review [Coding Standards](development/coding-standards.md)
4. Check [Quick Reference](development/quick-reference.md) for common patterns

### For AI Agents
1. Read [Agent Guide](ai-agents/agent-guide.md) first
2. Reference [Quick Reference](development/quick-reference.md) for patterns
3. Check [Modules](architecture/modules.md) for dependency rules

### For Product/Design
1. Start with [Product Overview](product/overview.md)
2. Review [Architecture Overview](architecture/overview.md) for technical context

## Key Principles

### Offline-First
Core gameplay must work without network connectivity. Cloud sync is optional only.

### Deterministic
All randomness uses seeded RNG. Same events must produce same outcomes for replay.

### Event-Sourced
Every state mutation produces immutable `GameEvent` instances. State is derived, never mutated directly.

### Module Boundaries
- `core/domain` and `core/rules` are pure Kotlin (NO Android dependencies)
- Feature modules cannot depend on other features (except `encounter` → `map`)
- Rules engine is 100% deterministic (NO AI calls)

### Privacy
Local data encrypted with SQLCipher. No PII in logs.

## Documentation Standards

All documentation in this project follows these standards:

- **Location**: All docs in `docs/` directory (except root README.md)
- **Format**: Markdown (.md) files
- **Structure**: Organized by category (architecture, development, product, etc.)
- **Updates**: Keep docs in sync with code changes
- **Cross-references**: Use relative links between docs

See [.kiro/steering/documentation-standards.md](../.kiro/steering/documentation-standards.md) for complete documentation guidelines.

---

**Last Updated**: 2025-11-10
