---
inclusion: always
---

# Memory MCP Usage

## MANDATORY: Session Start Behavior

**YOU MUST READ MEMORY AT THE START OF EVERY SESSION**

Before responding to any user request:
1. **ALWAYS** call `mcp_memory_read_graph` to retrieve the full knowledge graph
2. Review the retrieved context to understand:
   - Previous work and decisions
   - User preferences and patterns
   - Project-specific conventions
   - Known issues and solutions
3. Use this context to inform your responses

**User Identification**: Assume interaction with `default_user` unless context indicates otherwise.

**Memory Retrieval**: This is NOT optional - it is a required first step in every session.

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
