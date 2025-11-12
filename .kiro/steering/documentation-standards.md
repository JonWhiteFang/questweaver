---
inclusion: always
---

# Documentation Standards

## Documentation Location Rules

**Primary location**: `docs/` directory  
**Exceptions**: `README.md` and `AGENTS.md` in project root only

**Directory structure**:
```
docs/
├── README.md              # Navigation index
├── architecture/          # System design, modules, patterns
├── development/           # Setup, coding standards, build/test
├── product/              # Vision, scope, design principles
└── modules/{name}/       # Module-specific documentation
```

**File naming**: `lowercase-with-hyphens.md` (except `README.md`)

## When to Update Documentation

### Always Update
- Adding/removing modules → Update `docs/modules/{name}/README.md`, `docs/architecture/modules.md`, `docs/README.md`
- Changing module dependencies → Update affected module READMEs and `docs/architecture/modules.md`
- Adding architectural patterns → Update `docs/architecture/patterns.md` and `docs/development/quick-reference.md`
- Modifying build procedures → Update `docs/development/setup.md` and `build-and-test.md`
- Updating coding standards → Update `docs/development/coding-standards.md`

### Consider Updating
- Fixing bugs that reveal design issues → Update relevant architecture docs
- Adding significant classes → Update module README
- Changing testing approaches → Update `docs/development/build-and-test.md`

## Module README Template

When creating module documentation, use this structure:

```markdown
# {module-name}

**One-line purpose statement**

## Responsibilities
- Primary responsibility 1
- Primary responsibility 2

## Module Rules
✅ **Allowed Dependencies**: List allowed module dependencies
❌ **Forbidden**: List prohibited dependencies and patterns

## Key Classes
- `ClassName`: Brief description of purpose
- `AnotherClass`: Brief description of purpose

## Dependencies
**Production**: List production dependencies
**Test**: List test dependencies

## Testing Approach
**Coverage Target**: X%
**Key Patterns**: List testing patterns used

## Package Structure
```
package/
├── subpackage1/
└── subpackage2/
```

---
**Last Updated**: YYYY-MM-DD
```

## Documentation Quality Standards

Every documentation update must:
- Be clear and concise
- Include code examples for patterns
- Explain "why" not just "what"
- Use relative links: `[text](../path/file.md)`
- Update timestamp at bottom

## Critical Rules

**NEVER**:
- Create documentation in project root (except `README.md` and `AGENTS.md`)
- Duplicate content across multiple locations
- Leave broken cross-references after moving/renaming files
- Skip timestamp updates

**ALWAYS**:
- Place all documentation in `docs/` directory
- Use relative links for cross-references
- Update documentation when changing code structure
- Follow naming conventions (`lowercase-with-hyphens.md`)
