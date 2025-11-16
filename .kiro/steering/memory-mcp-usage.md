---
inclusion: always
---

# Memory MCP Usage

## Core Behavior

**User Identification**: Assume interaction with `default_user` unless context indicates otherwise.

**Memory Retrieval**: At the start of each session, retrieve relevant context from the knowledge graph (refer to it as "memory").

## Information Categories to Track

Track project-relevant information in these categories:

1. **Project Context**
   - Module responsibilities and boundaries
   - Architecture decisions and rationale
   - Performance constraints and targets
   - Known issues or technical debt

2. **User Preferences**
   - Coding style preferences beyond project standards
   - Preferred testing approaches
   - Communication style (verbose vs. concise)

3. **Implementation Patterns**
   - Recurring design patterns used
   - Common solutions to project-specific problems
   - Module-specific conventions

4. **Goals & Priorities**
   - Current feature development focus
   - Refactoring targets
   - Performance optimization goals

## Memory Updates

After interactions involving:
- **Architecture decisions**: Store rationale and constraints as observations
- **Module changes**: Update entity relationships between modules
- **Pattern establishment**: Create entities for recurring patterns with usage examples
- **Performance findings**: Store benchmark results and optimization decisions
- **User preferences**: Update user entity with preferences that differ from defaults

## QuestWeaver-Specific Tracking

**Critical to remember**:
- Module dependency violations encountered and resolved
- Event sourcing patterns used in specific features
- Deterministic testing approaches for rules engine
- Performance bottlenecks discovered and solutions applied
- AI integration patterns (on-device vs. remote decisions)

**Do NOT track**:
- Personal user information (PII)
- Temporary debugging information
- One-off code snippets without broader applicability
