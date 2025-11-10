---
inclusion: always
---

# Documentation Standards

## Documentation Structure

All project documentation MUST be organized in the `docs/` directory with the following structure:

```
docs/
├── README.md                    # Documentation index and navigation
├── architecture/                # System design and architecture
│   ├── overview.md             # Complete architecture specification
│   ├── modules.md              # Module structure and dependencies
│   └── patterns.md             # Architectural patterns (MVI, Event Sourcing, etc.)
├── development/                # Developer guides and standards
│   ├── setup.md                # Getting started guide
│   ├── coding-standards.md     # Code style and conventions
│   ├── build-and-test.md       # Build and testing guidelines
│   ├── koin-di-patterns.md     # DI patterns
│   └── quick-reference.md      # Quick lookup reference
├── product/                    # Product and design documentation
│   └── overview.md             # Product vision and scope
├── ai-agents/                  # AI agent-specific documentation
│   ├── agent-guide.md          # Complete guide for AI agents
│   └── suggested-hooks.md      # Recommended agent hooks
└── modules/                    # Module-specific documentation
    └── {module-name}/
        └── README.md           # Module documentation
```

## Documentation Rules

### 1. Location Rules

**MUST:**
- Place ALL documentation in `docs/` directory (except root `README.md`)
- Keep root `README.md` as the main project entry point
- Place module-specific docs in `docs/modules/{module-name}/README.md`
- Keep steering rules in `.kiro/steering/` (for AI agent guidance)

**MUST NOT:**
- Create documentation files in root directory (except `README.md`)
- Scatter documentation across multiple locations
- Duplicate documentation in multiple places

### 2. File Naming

- Use lowercase with hyphens: `coding-standards.md`, `quick-reference.md`
- Use descriptive names: `overview.md`, `setup.md`, `patterns.md`
- Module READMEs: Always `README.md` (uppercase)

### 3. Content Organization

**Architecture Documentation** (`docs/architecture/`):
- System design and high-level architecture
- Module structure and dependency rules
- Architectural patterns and principles
- Data models and interfaces

**Development Documentation** (`docs/development/`):
- Setup and getting started guides
- Coding standards and conventions
- Build and test procedures
- Common patterns and quick references

**Product Documentation** (`docs/product/`):
- Product vision and goals
- Feature scope and boundaries
- User experience priorities
- Design principles

**AI Agent Documentation** (`docs/ai-agents/`):
- Complete agent instructions
- Critical rules and constraints
- Common tasks and patterns
- Suggested automation hooks

**Module Documentation** (`docs/modules/{module}/`):
- Module purpose and responsibilities
- Key classes and interfaces
- Dependencies and integration points
- Testing approach

### 4. Cross-References

- Use relative links: `[Coding Standards](../development/coding-standards.md)`
- Link to related documentation
- Keep links up-to-date when moving files

### 5. Maintenance Rules

**When creating new features:**
- Update relevant architecture docs if module structure changes
- Add module README for new modules
- Update quick reference if adding common patterns

**When changing code:**
- Update corresponding documentation
- Keep code examples in docs synchronized
- Update version/date stamps

**When refactoring:**
- Update all affected documentation
- Fix broken cross-references
- Consolidate duplicate information

### 6. Documentation Index

The `docs/README.md` file MUST:
- Provide clear navigation to all documentation
- Group docs by category
- Include quick links for common use cases
- List key principles and constraints
- Reference this steering document

## Documentation Templates

### Module README Template

```markdown
# {module-name}

**Brief description of module purpose**

## Purpose

What this module does and why it exists.

## Responsibilities

- Key responsibility 1
- Key responsibility 2

## Key Classes and Interfaces

### ClassName

Brief description and purpose.

## Dependencies

### Production
- List of production dependencies

### Test
- List of test dependencies

## Module Rules

### ✅ Allowed
- What this module can do

### ❌ Forbidden
- What this module cannot do

## Architecture Patterns

Key patterns used in this module.

## Testing Approach

How to test this module.

## Building and Testing

```bash
# Build commands
# Test commands
```

## Package Structure

```
package/structure/here
```

## Integration Points

### Consumes
- What this module depends on

### Provides
- What this module provides to others

---

**Last Updated**: YYYY-MM-DD
```

## When to Update Documentation

### Always Update:
- When adding new modules
- When changing module dependencies
- When adding new architectural patterns
- When changing build procedures
- When updating coding standards
- When adding new features that affect architecture

### Consider Updating:
- When fixing bugs that reveal design issues
- When adding significant new classes
- When changing testing approaches
- When updating dependencies

## Documentation Quality Standards

### Good Documentation:
- Clear and concise
- Up-to-date with code
- Includes examples
- Explains "why" not just "what"
- Uses consistent formatting
- Has working cross-references

### Bad Documentation:
- Outdated or incorrect
- Duplicates information
- Missing context
- No examples
- Inconsistent formatting
- Broken links

## AI Agent Instructions

When working on this project:

1. **Check documentation first** before making architectural decisions
2. **Update docs** when making changes that affect architecture or patterns
3. **Follow the structure** defined in this document
4. **Keep docs in sync** with code changes
5. **Use relative links** for cross-references
6. **Update timestamps** when modifying documentation

### Common Documentation Tasks:

**Adding a new module:**
1. Create `docs/modules/{module-name}/README.md`
2. Update `docs/architecture/modules.md` with module info
3. Update `docs/README.md` to link to new module

**Changing architecture:**
1. Update `docs/architecture/overview.md` or `docs/architecture/patterns.md`
2. Update affected module READMEs
3. Update `docs/development/quick-reference.md` if patterns change

**Adding new patterns:**
1. Document in `docs/architecture/patterns.md`
2. Add to `docs/development/quick-reference.md`
3. Update `docs/development/coding-standards.md` if needed

**Updating build process:**
1. Update `docs/development/setup.md`
2. Update `docs/development/build-and-test.md`
3. Update affected module READMEs

## Enforcement

This steering document is **always included** in AI agent context. Agents MUST:

- Place all new documentation in `docs/` directory
- Follow the structure defined here
- Update documentation when making code changes
- Keep cross-references working
- Maintain consistency across all docs

Violations of these standards should be flagged and corrected immediately.

---

**Last Updated**: 2025-11-10
