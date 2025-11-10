---
inclusion: always
---

---
inclusion: always
---

# Documentation Standards

## Core Rules

**Location**: All documentation in `docs/` directory (except root `README.md`)

**Structure**:
```
docs/
├── README.md              # Navigation index
├── architecture/          # System design, modules, patterns
├── development/           # Setup, coding standards, build/test
├── product/              # Vision, scope, design principles
├── ai-agents/            # Agent guides and hooks
└── modules/{name}/       # Module-specific docs
```

**Naming**: lowercase-with-hyphens.md (except `README.md`)

## When to Update Documentation

**Always update when**:
- Adding/removing modules
- Changing module dependencies
- Adding architectural patterns
- Modifying build procedures
- Updating coding standards

**Consider updating when**:
- Fixing bugs revealing design issues
- Adding significant classes
- Changing testing approaches

## AI Agent Workflow

### Before Making Changes
1. Check `docs/architecture/` for module boundaries and patterns
2. Verify dependency rules in `docs/architecture/modules.md`
3. Review coding standards in `docs/development/coding-standards.md`

### After Making Changes
1. **New module**: Create `docs/modules/{name}/README.md`, update `docs/architecture/modules.md` and `docs/README.md`
2. **Architecture change**: Update `docs/architecture/overview.md` or `patterns.md`, affected module READMEs
3. **New pattern**: Add to `docs/architecture/patterns.md` and `docs/development/quick-reference.md`
4. **Build change**: Update `docs/development/setup.md` and `build-and-test.md`

### Documentation Quality Checklist
- Clear and concise
- Includes code examples
- Explains "why" not just "what"
- Uses relative links: `[text](../path/file.md)`
- Updates timestamp

## Module README Template

```markdown
# {module-name}

**One-line purpose**

## Responsibilities
- Key responsibility 1
- Key responsibility 2

## Module Rules
✅ **Allowed**: What this module can do/depend on
❌ **Forbidden**: What this module cannot do/depend on

## Key Classes
Brief descriptions of main classes/interfaces

## Dependencies
**Production**: List
**Test**: List

## Testing Approach
Coverage target and key test patterns

## Package Structure
```
package/structure
```

---
**Last Updated**: YYYY-MM-DD
```

## Critical Constraints

**NEVER**:
- Create docs in root directory (except `README.md`)
- Duplicate documentation across locations
- Leave broken cross-references
- Skip timestamp updates

**ALWAYS**:
- Place docs in `docs/` directory
- Use relative links for cross-references
- Update docs when changing code
- Follow naming conventions
