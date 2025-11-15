# QuestWeaver Test Failures - Remaining Issues

**Date**: 2025-11-15  
**Status**: 10 test failures remaining (down from 23)  
**Project**: QuestWeaver Android RPG

## Context

After fixing Android Log mocking issues and CompletionDetector logic bugs, 10 test failures remain. These are primarily test expectation mismatches where error messages include prefixes that tests don't expect.

## Test Results Summary

```
80 tests completed, 10 failed, 1 skipped
```

### Tests Fixed Previously
- ✅ 22 EncounterViewModelTest failures (Android Log mocking)
- ✅ 2 CompletionDetectorTest failures (edge case logic)

## Remaining Failures

### 1. Error Message Format Mismatches (2 failures)

**Location**: `feature/encounter/src/test/java/dev/questweaver/feature/encounter/AdditionalTests.kt`

#### Failure 1: Initialization Error Message
```
Test: "Error handling > initialization failure updates error state"
Expected: "Cannot start encounter with no creatures"
Actual: "Failed to start encounter: Cannot start encounter with no creatures"
```

**Root Cause**: The `EncounterError.InitializationFailed.toUserMessage()` method adds a prefix to error messages.

**Fix Needed**: Either:
- Update test expectation to match actual error message format
- OR modify `toUserMessage()` to not add prefix for this specific error

#### Failure 2: Action Error Message
```
Test: "Error handling > action failure updates error state"
Expected: "No active creature"
Actual: "Action failed: No active creature"
```

**Root Cause**: The `EncounterError.ActionFailed.toUserMessage()` method adds a prefix.

**Fix Needed**: Update test expectation to match actual format.

### 2. Range Overlay Test Failure (1 failure)

**Location**: `feature/encounter/src/test/java/dev/questweaver/feature/encounter/AdditionalTests.kt`

```
Test: "Map integration > range overlays provided to map"
Error: Expected value to not be null, but was null.
Line: AdditionalTests.kt:313
```

**Root Cause**: The test expects `getWeaponRangeOverlay()` to return a non-null value, but it's returning null. This suggests:
- The active creature might not be set in the test state
- OR the creature's position is not properly initialized
- OR the mapState is missing

**Fix Needed**: 
1. Check test setup - ensure active creature is properly set with valid position
2. Verify mapState is initialized in the test
3. May need to mock the MapIntegration.buildWeaponRangeOverlay() call

### 3. Other Failures (7 failures)

The test output was truncated, but there are 7 additional failures not shown in the error log. These need to be identified by:

1. Running tests with full output: `gradle test --info`
2. OR checking the HTML test report at:
   ```
   file:///C:/Projects/questweaver/feature/encounter/build/reports/tests/testDebugUnitTest/index.html
   ```

## Files to Investigate

### Primary Files
1. `feature/encounter/src/test/java/dev/questweaver/feature/encounter/AdditionalTests.kt`
   - Contains the failing error message tests
   - Contains the range overlay test

2. `feature/encounter/src/main/java/dev/questweaver/feature/encounter/viewmodel/EncounterError.kt`
   - Contains `toUserMessage()` implementations
   - May need to adjust error message formatting

3. `feature/encounter/src/main/java/dev/questweaver/feature/encounter/viewmodel/EncounterViewModel.kt`
   - Contains `getWeaponRangeOverlay()` method
   - May need to check null handling

### Supporting Files
4. `feature/encounter/src/test/java/dev/questweaver/feature/encounter/viewmodel/EncounterViewModelTest.kt`
   - May have similar error message expectation issues

## Recommended Fix Approach

### Step 1: Fix Error Message Tests (Quick Win)
Update test expectations in `AdditionalTests.kt`:

```kotlin
// Line ~78 - Update expected error message
state.error shouldBe "Failed to start encounter: Cannot start encounter with no creatures"

// Line ~102 - Update expected error message  
state.error shouldBe "Action failed: No active creature"
```

### Step 2: Fix Range Overlay Test
In `AdditionalTests.kt` around line 313:

1. Check the test setup before calling `getWeaponRangeOverlay()`
2. Ensure:
   ```kotlin
   // Verify state has active creature
   state.activeCreatureId.shouldNotBeNull()
   
   // Verify creature has position
   state.creatures[state.activeCreatureId]?.position.shouldNotBeNull()
   
   // Then test overlay
   val overlay = viewModel.getWeaponRangeOverlay(30)
   overlay.shouldNotBeNull()
   ```

### Step 3: Identify Remaining 7 Failures
Run with full output:
```bash
gradle test --info > test-output.txt 2>&1
```

Or open the HTML report to see all failures with stack traces.

### Step 4: Verify All Fixes
After making changes:
```bash
gradle clean test
```

## Expected Outcome

After fixes, should achieve:
- **80 tests completed, 0 failed, 1 skipped** (the skipped test is intentional)
- All EncounterViewModelTest passing
- All CompletionDetectorTest passing
- All AdditionalTests passing

## Additional Notes

### Test Configuration
The following test configuration was added to `feature/encounter/build.gradle.kts`:

```kotlin
testOptions {
    unitTests {
        isReturnDefaultValues = true
        isIncludeAndroidResources = false
    }
}
```

This mocks Android framework classes automatically.

### Previous Fixes Applied
1. Replaced `android.util.Log` with `println` and `System.err.println` in EncounterViewModel
2. Fixed CompletionDetector edge cases:
   - Empty creature map now returns Defeat
   - All creatures at 0 HP now returns Defeat

## Commands for Next Session

```bash
# View full test report
start file:///C:/Projects/questweaver/feature/encounter/build/reports/tests/testDebugUnitTest/index.html

# Run tests with full output
gradle test --info

# Run only failing test class
gradle :feature:encounter:test --tests "dev.questweaver.feature.encounter.AdditionalTests"

# Run specific test
gradle :feature:encounter:test --tests "dev.questweaver.feature.encounter.AdditionalTests.Error handling.initialization failure updates error state"
```

## Priority

**High Priority**: Fix the 3 identified failures first (2 error message + 1 range overlay)  
**Medium Priority**: Identify and fix the remaining 7 failures  
**Goal**: Achieve 100% test pass rate (except intentionally skipped tests)
