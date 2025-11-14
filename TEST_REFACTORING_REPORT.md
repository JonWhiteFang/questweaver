# Test Refactoring Report: InitiativeEntry to InitiativeEntryData Migration

**Date:** 2025-11-14  
**Status:** Partially Complete - Constructor Calls Fixed, API Changes Pending

## Overview

The migration from the old 2-parameter `InitiativeEntry` class to the new 4-parameter `InitiativeEntryData` class has been partially completed. All constructor calls have been updated, but the `core/rules` module tests are failing due to deeper API changes in the `InitiativeTracker` class.

## What Was Completed

### ✅ Constructor Call Replacements

All test files have been updated to use `InitiativeEntryData` instead of `InitiativeEntry`:

**Pattern replaced:**
```kotlin
// OLD (2 parameters)
InitiativeEntry(creatureId = 10, initiative = 18)
InitiativeEntry(10, 18)

// NEW (4 parameters)
InitiativeEntryData(creatureId = 10, roll = 18, modifier = 0, total = 18)
InitiativeEntryData(10, 18, 0, 18)
```

**Files successfully updated:**
1. `core/domain/src/test/kotlin/dev/questweaver/domain/IntegrationVerificationTest.kt`
2. `core/domain/src/test/kotlin/dev/questweaver/domain/entities/EntitySerializationTest.kt`
3. `core/domain/src/test/kotlin/dev/questweaver/domain/entities/EncounterTest.kt`
4. `core/rules/src/test/kotlin/dev/questweaver/core/rules/initiative/InitiativeTrackerCreatureLifecycleTest.kt`
5. `core/rules/src/test/kotlin/dev/questweaver/core/rules/initiative/InitiativeTrackerDelayedTurnTest.kt`
6. `core/rules/src/test/kotlin/dev/questweaver/core/rules/initiative/InitiativeTrackerTurnAdvancementTest.kt`
7. `core/rules/src/test/kotlin/dev/questweaver/core/rules/initiative/InitiativeDeterminismPropertyTest.kt`

**Status:** ✅ All constructor calls have been replaced

## What Needs To Be Done

### ❌ API Changes in InitiativeTracker

The `InitiativeTracker` class API has changed significantly. Methods now return `InitiativeResult<RoundState>` instead of `RoundState` directly.

#### Problem 1: Return Type Wrapper

**Old API:**
```kotlin
fun initialize(order: List<InitiativeEntry>): RoundState
fun advanceTurn(state: RoundState): RoundState
fun addCreature(state: RoundState, entry: InitiativeEntry): RoundState
fun removeCreature(state: RoundState, creatureId: Long): RoundState
```

**New API:**
```kotlin
fun initialize(order: List<InitiativeEntry>): InitiativeResult<RoundState>
fun advanceTurn(state: RoundState): InitiativeResult<RoundState>
fun addCreature(state: RoundState, entry: InitiativeEntry): InitiativeResult<RoundState>
fun removeCreature(state: RoundState, creatureId: Long): InitiativeResult<RoundState>
```

#### Problem 2: State Property Access

Tests are trying to access properties directly on the result:
```kotlin
// CURRENT (BROKEN)
val state = tracker.initialize(initialOrder)
state.currentTurn!!.activeCreatureId shouldBe 1L  // ERROR: Unresolved reference

// NEEDS TO BE
val result = tracker.initialize(initialOrder)
val state = result.getOrThrow()  // or result.state, depending on InitiativeResult API
state.currentTurn!!.activeCreatureId shouldBe 1L
```

#### Problem 3: Import Statements

Tests need to import the new types:
```kotlin
import dev.questweaver.core.rules.initiative.models.InitiativeResult
import dev.questweaver.domain.events.InitiativeEntryData  // For domain tests
```

### Files Requiring API Updates

All files in `core/rules/src/test/kotlin/dev/questweaver/core/rules/initiative/`:

1. **InitiativeTrackerCreatureLifecycleTest.kt** (~400 errors)
   - Update all `initialize()` calls to unwrap result
   - Update all `advanceTurn()` calls to unwrap result
   - Update all `addCreature()` calls to unwrap result
   - Update all `removeCreature()` calls to unwrap result
   - Update all state property accesses

2. **InitiativeTrackerDelayedTurnTest.kt** (~200 errors)
   - Update all `initialize()` calls to unwrap result
   - Update all `advanceTurn()` calls to unwrap result
   - Update all `delayTurn()` calls to unwrap result
   - Update all state property accesses
   - Fix missing imports: `shouldContainKey`, `shouldNotContainKey`

3. **InitiativeTrackerTurnAdvancementTest.kt** (~150 errors)
   - Update all `initialize()` calls to unwrap result
   - Update all `advanceTurn()` calls to unwrap result
   - Update all state property accesses

4. **SurpriseHandlerTest.kt** (~6 errors)
   - Fix `endSurpriseRound()` calls - method signature changed
   - Remove extra parameters being passed

### Additional Issues

#### Missing Kotest Matchers
Some tests are missing imports:
```kotlin
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
```

#### InitiativeResult API Investigation Needed

Before fixing tests, determine the `InitiativeResult` API:
- Is it `result.getOrThrow()`?
- Is it `result.state`?
- Is it `result.value`?
- Does it have `isSuccess` / `isFailure` properties?

**Action:** Read `core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/models/InitiativeResult.kt` to understand the API.

## Recommended Approach for Next Session

### Step 1: Understand InitiativeResult API (5 minutes)

```kotlin
// Read this file first
core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/models/InitiativeResult.kt
```

Determine:
- How to unwrap the result to get `RoundState`
- Whether error handling is needed in tests
- If there are helper methods for testing

### Step 2: Create Helper Function (10 minutes)

Add a test helper to simplify unwrapping:

```kotlin
// In each test file, add at top level:
private fun InitiativeResult<RoundState>.unwrap(): RoundState {
    return when (this) {
        is InitiativeResult.Success -> this.state
        is InitiativeResult.Error -> throw AssertionError("Expected success but got error: ${this.message}")
    }
}

// Or if it's a sealed class with different structure, adjust accordingly
```

### Step 3: Fix Tests Systematically (60-90 minutes)

For each test file:

1. Add necessary imports
2. Add helper function
3. Update all method calls to unwrap results:

```kotlin
// Pattern to replace:
val state = tracker.initialize(order)

// With:
val state = tracker.initialize(order).unwrap()

// Or:
state = tracker.advanceTurn(state)

// With:
state = tracker.advanceTurn(state).unwrap()
```

4. Run tests for that file: `gradle :core:rules:test --tests "*FileName*"`
5. Fix any remaining issues
6. Move to next file

### Step 4: Fix SurpriseHandlerTest (10 minutes)

Check the `SurpriseHandler.endSurpriseRound()` method signature and update test calls to match.

### Step 5: Run Full Test Suite (5 minutes)

```bash
gradle :core:rules:test
gradle :core:domain:test
```

Verify all tests pass.

## Compilation Error Summary

**Total errors:** ~800+

**Breakdown by category:**
- Unresolved reference `InitiativeEntryData`: ~100 (FIXED ✅)
- Type mismatch `InitiativeResult<RoundState>` vs `RoundState`: ~400
- Unresolved reference to state properties: ~250
- Missing imports: ~10
- Wrong method signatures: ~6

## Test Files Status

| File | Constructor Calls | API Updates | Status |
|------|------------------|-------------|--------|
| IntegrationVerificationTest.kt | ✅ Fixed | N/A | ✅ Complete |
| EntitySerializationTest.kt | ✅ Fixed | N/A | ✅ Complete |
| EncounterTest.kt | ✅ Fixed | N/A | ✅ Complete |
| InitiativeTrackerCreatureLifecycleTest.kt | ✅ Fixed | ❌ Pending | ⚠️ Partial |
| InitiativeTrackerDelayedTurnTest.kt | ✅ Fixed | ❌ Pending | ⚠️ Partial |
| InitiativeTrackerTurnAdvancementTest.kt | ✅ Fixed | ❌ Pending | ⚠️ Partial |
| InitiativeDeterminismPropertyTest.kt | ✅ Fixed | ❌ Pending | ⚠️ Partial |
| SurpriseHandlerTest.kt | N/A | ❌ Pending | ❌ Broken |

## Key Files to Reference

### For Understanding Current API:
- `core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/InitiativeTracker.kt`
- `core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/models/InitiativeResult.kt`
- `core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/models/RoundState.kt`
- `core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/SurpriseHandler.kt`

### For Understanding Old vs New:
- `core/domain/src/main/kotlin/dev/questweaver/domain/events/InitiativeEntryData.kt` (new)
- `core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/models/InitiativeEntry.kt` (current in rules module)

## Example Fix Pattern

### Before (Broken):
```kotlin
test("advance turn updates active creature") {
    val tracker = InitiativeTracker()
    
    val initiativeOrder = listOf(
        InitiativeEntryData(creatureId = 1L, roll = 18, modifier = 3, total = 21),
        InitiativeEntryData(creatureId = 2L, roll = 15, modifier = 2, total = 17),
        InitiativeEntryData(creatureId = 3L, roll = 12, modifier = 1, total = 13)
    )
    
    val state = tracker.initialize(initiativeOrder)  // Returns InitiativeResult<RoundState>
    state.currentTurn!!.activeCreatureId shouldBe 1L  // ERROR: Unresolved reference
    
    val newState = tracker.advanceTurn(state)  // ERROR: Type mismatch
    newState.currentTurn!!.activeCreatureId shouldBe 2L
}
```

### After (Fixed):
```kotlin
test("advance turn updates active creature") {
    val tracker = InitiativeTracker()
    
    val initiativeOrder = listOf(
        InitiativeEntryData(creatureId = 1L, roll = 18, modifier = 3, total = 21),
        InitiativeEntryData(creatureId = 2L, roll = 15, modifier = 2, total = 17),
        InitiativeEntryData(creatureId = 3L, roll = 12, modifier = 1, total = 13)
    )
    
    val state = tracker.initialize(initiativeOrder).unwrap()  // Unwrap result
    state.currentTurn!!.activeCreatureId shouldBe 1L
    
    val newState = tracker.advanceTurn(state).unwrap()  // Unwrap result
    newState.currentTurn!!.activeCreatureId shouldBe 2L
}
```

## Estimated Time to Complete

- **Step 1 (Understand API):** 5 minutes
- **Step 2 (Helper function):** 10 minutes
- **Step 3 (Fix tests):** 60-90 minutes
- **Step 4 (SurpriseHandler):** 10 minutes
- **Step 5 (Verify):** 5 minutes

**Total:** ~90-120 minutes

## Commands to Run

```bash
# Check specific test file
gradle :core:rules:test --tests "*InitiativeTrackerCreatureLifecycleTest*"

# Run all rules tests
gradle :core:rules:test

# Run all domain tests
gradle :core:domain:test

# Run all tests
gradle test
```

## Notes

- The `core/domain` tests are likely passing since they don't use `InitiativeTracker` directly
- The `core/rules` tests are the main focus
- Consider creating a shared test utility file for the unwrap helper if it's used across multiple test files
- The `InitiativeEntry` class in `core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/models/` is different from `InitiativeEntryData` in `core/domain` - they serve different purposes (rules engine vs event sourcing)

## Success Criteria

✅ All tests compile without errors  
✅ All tests pass  
✅ No warnings about unresolved references  
✅ Test coverage maintained at previous levels  

---

**Next Session Action:** Start with Step 1 - read `InitiativeResult.kt` to understand the unwrapping API, then proceed systematically through the test files.
