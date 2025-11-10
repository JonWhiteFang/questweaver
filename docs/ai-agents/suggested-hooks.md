# Suggested Agent Hooks for QuestWeaver

Based on the project structure and constraints, here are agent hooks that would be valuable:

## Code Quality & Architecture Enforcement

### 1. Module Dependency Validator [DONE]
When a file is saved in any module's `build.gradle.kts`, automatically check that all dependencies comply with the strict module boundary rules (e.g., core/domain has no Android deps, features don't cross-depend except encounter→map).

### 2. Event Sourcing Verifier [DONE]
When a file is saved in `core/domain` or `feature/*` that contains state mutations, verify that corresponding `GameEvent` instances are being generated and that state isn't being mutated directly.

### 3. Determinism Checker [DONE]
When a file is saved in `core/rules` or any test file, scan for unseeded `Random()` calls and flag them, ensuring all randomness uses `DiceRoller` with seeds.

### 4. Sealed Type Exhaustiveness Enforcer [DONE]
When a file containing sealed types is saved, check that all `when` expressions handling those types are exhaustive (no `else` branch for sealed types).

## Testing & Quality

### 5. Test Coverage Guardian [DONE]
When a Kotlin file is saved in `core/rules`, `core/domain`, or `core/data`, check if corresponding test file exists and meets coverage targets (90%/85%/80% respectively). Optionally run tests for that module.

### 6. Test Runner on Save [DONE]
When a test file is saved, automatically run just that test file to provide immediate feedback on test failures.

### 7. Property-Based Test Suggester [DONE]
When a file in `core/rules` is saved with deterministic logic (dice rolling, combat resolution), suggest property-based test cases using kotest's `checkAll`.

## Documentation & Standards

### 8. KDoc Completeness Checker [DONE]
When a file with public APIs is saved in `core/domain`, verify that all public functions have KDoc comments documenting parameters, return values, and behavior.

### 9. PII Logger Detector [DONE]
When any Kotlin file is saved, scan logging statements for potential PII leaks (email, name patterns) and flag violations.

### 10. Import Validator [DONE]
When a file in `core/domain` or `core/rules` is saved, verify no Android framework imports are present (android.*, androidx.* except annotations).

## Build & Performance

### 11. Gradle Sync on Dependency Change [DONE]
When `gradle/libs.versions.toml` or any `build.gradle.kts` is saved, automatically trigger Gradle sync to catch configuration errors early.

### 12. Performance Budget Checker [DONE]
When files in `feature/map/render` are saved, suggest running performance tests to verify the ≤4ms render budget is maintained.

## Code Generation & Boilerplate

### 13. MVI Boilerplate Generator [DONE]
When a new ViewModel file is created in `feature/*/viewmodel`, offer to generate the standard MVI pattern boilerplate (UiState data class, Intent sealed interface, StateFlow setup).

### 14. Event-to-Entity Mapper Generator [DONE]
When a new `GameEvent` is added in `core/domain/events`, offer to generate the corresponding Room entity in `core/data` and update the DAO.

### 15. Use Case Template Generator [DONE]
When a new use case file is created in `core/domain/usecases`, offer to generate the standard template with `suspend operator fun invoke()` and sealed Result type.

## Git & Collaboration

### 16. Pre-Commit Lint Runner
When user attempts to commit (manual trigger), run `gradle lint` and `detekt` to catch style violations before they enter the codebase.

### 17. Module README Updater [DONE]
When significant changes are made to a feature module, remind to update the module's README with new classes, dependencies, or testing approaches.

---

## Priority Recommendations

The most critical hooks for this project would be:

1. **Module Dependency Validator** - Enforces non-negotiable architectural boundaries
2. **Event Sourcing Verifier** - Ensures core event sourcing pattern is followed
3. **Determinism Checker** - Prevents breaking replay functionality
4. **Import Validator** - Catches Android dependencies in pure Kotlin modules

These hooks enforce the strict architectural constraints that are fundamental to the project's design.
