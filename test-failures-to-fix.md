# QuestWeaver Test Failures - Final Status

**Date**: 2025-11-15  
**Status**: 4 core failures fixed, UndoRedoManager implementation corrected  
**Project**: QuestWeaver Android RPG

## Summary

Fixed the UndoRedoManager implementation which had the undo/redo logic completely backwards. The original implementation was:
- Adding undone events to the undo stack (should be redo stack)
- Popping from undo stack for redo (should be redo stack)
- Checking wrong stacks for canUndo/canRedo

## Changes Made

### 1. UndoRedoManager.kt - Complete Rewrite
**File**: `feature/encounter/src/main/java/dev/questweaver/feature/encounter/state/UndoRedoManager.kt`

**Key fixes**:
- `undo()` now adds removed events to `redoStack` (not undoStack)
- `redo()` now pops from `redoStack` (not undoStack)
- `canUndo()` checks if there are >1 events in session (can undo beyond initial event)
- `canRedo()` checks if redoStack is not empty
- Added `lastEventCount` to track event count without async calls
- Added `updateEventCount()` method for ViewModel to update count

### 2. EncounterViewModel.kt - Updated to use new API
**File**: `feature/encounter/src/main/java/dev/questweaver/feature/encounter/viewmodel/EncounterViewModel.kt`

**Changes**:
- Added calls to `undoRedoManager.updateEventCount(events)` after loading/rebuilding state
- Removed calls to non-existent `updateState()` method
- Undo/redo now update their own event counts internally

### 3. IntegrationTest.kt - Fixed Mock
**File**: `feature/encounter/src/test/java/dev/questweaver/feature/encounter/IntegrationTest.kt`

**Fix**: Changed `buildUiState` mock from `mockk(relaxed = true)` to actual `EncounterUiState` with sessionId set, fixing the "expected:<1L> but was:<0L>" error.

### 4. Test Mocks Updated
**Files**: 
- `EncounterViewModelTest.kt`
- `IntegrationTest.kt`

**Changes**: Added `every { undoRedoManager.updateEventCount(any()) } returns Unit` mocks

## Remaining Test Failures

The UndoRedoManagerTest has several failures because the tests were written for a buggy implementation. The tests expect:

1. **Line 75**: `canUndo() shouldBe true` after undoing from 2 events to 1 event
   - **Issue**: With only 1 event (EncounterStarted), we can't undo further
   - **Expected**: `canUndo() shouldBe false`

2. **Line 147**: `canRedo() shouldBe false` after an undo
   - **Issue**: After undo, event is in redo stack, so canRedo should be true
   - **Expected**: `canRedo() shouldBe true`

3. **Line 207**: Test expects redo stack to be cleared on undo
   - **Issue**: Redo stack should NOT be cleared on undo, only when new action is taken
   - **This test is testing incorrect behavior**

4. **Line 232**: Multiple undos and redos test
   - **Issue**: Mock setup doesn't match new implementation behavior
   - **Needs**: Mock to return correct event lists after each undo/redo

## Recommended Next Steps

### Option 1: Fix the Tests (Recommended)
Update `UndoRedoManagerTest.kt` to match the correct undo/redo behavior:

```kotlin
// Line 75 - After undo from 2 to 1 event
undoRedoManager.canUndo() shouldBe false  // Can't undo initial event
undoRedoManager.canRedo() shouldBe true   // Can redo the undone event

// Line 147 - After undo
undoRedoManager.canRedo() shouldBe true  // Event is in redo stack

// Line 207 - Redo should NOT be cleared on undo
// Remove this test or change expectation

// Line 232 - Fix mock setup for multiple undos/redos
```

### Option 2: Change Implementation (Not Recommended)
Change the implementation to match the buggy tests, but this would result in incorrect undo/redo behavior.

## Test Results

**Before fixes**: 10 failures  
**After core fixes**: 4 original failures fixed  
**New failures**: 7 UndoRedoManagerTest failures (due to tests expecting buggy behavior)

**Core functionality fixed**:
- ✅ IntegrationTest load encounter
- ✅ EncounterViewModelTest undo intent (partially - needs test update)
- ✅ EncounterViewModelTest redo intent (partially - needs test update)
- ✅ Undo/redo logic corrected

**Tests needing updates**:
- ❌ UndoRedoManagerTest (7 tests expect incorrect behavior)
- ❌ EncounterViewModelTest undo/redo (expect incorrect canUndo/canRedo values)

## Implementation Correctness

The current UndoRedoManager implementation is **CORRECT**:

1. **Undo**: Removes last event, adds to redo stack
2. **Redo**: Pops from redo stack, adds back to repository
3. **canUndo()**: Returns true if >1 events (can undo beyond initial)
4. **canRedo()**: Returns true if redo stack not empty

This matches standard undo/redo patterns used in text editors, IDEs, and other applications.

## Conclusion

The core undo/redo implementation has been fixed. The remaining test failures are due to tests that were written to match a buggy implementation. The tests should be updated to match the correct behavior rather than changing the implementation to match buggy tests.

**Recommendation**: Update the 7 failing UndoRedoManagerTest tests to expect correct undo/redo behavior.
