# Performance Tests

This directory contains performance benchmarks for the map rendering system.

## Overview

The `RenderingPerformanceTest` suite benchmarks critical rendering operations to ensure they meet the 60fps target (≤4ms per frame). Tests cover:

- Grid rendering and viewport culling
- Token rendering with various counts
- Full frame rendering with all layers
- Coordinate conversions
- Pan/zoom gesture performance
- Memory allocation efficiency
- Large grid stress tests

## CI Compatibility

These tests are designed to run reliably in CI environments:

### Features

1. **Tagged with `performance`** - Can be excluded from standard CI runs if needed
2. **JIT Warmup** - Each test includes a warmup phase to ensure JIT compilation doesn't affect results
3. **Environment-Aware Thresholds** - Automatically detects CI and applies 3x more lenient thresholds
4. **Memory Test Disabled in CI** - Memory allocation tests are skipped in CI due to GC non-determinism

### Running Tests

**Local development (strict thresholds):**
```bash
gradle :feature:map:test
```

**CI environment (lenient thresholds):**
```bash
# Automatically detected via CI environment variable
gradle :feature:map:test
```

**Skip performance tests in CI:**
```bash
gradle :feature:map:test -Dkotest.tags.exclude=performance
```

**Run only performance tests:**
```bash
gradle :feature:map:test -Dkotest.tags.include=performance
```

## Performance Targets

### Local Development
- Grid rendering (50x50): ≤1ms
- Full frame with all layers: ≤4ms
- Coordinate conversions: ≤4ms for full grid
- State updates: ≤0.5ms
- Pan/zoom gestures: ≤4ms per frame

### CI Environment
All thresholds are 3x more lenient to account for:
- Variable CPU load
- Different hardware specs
- JVM warmup variations
- Shared CI runner resources

## Test Structure

Each test follows this pattern:

```kotlin
test("operation should be fast") {
    // 1. Warmup JIT compiler
    warmup {
        // Run operation multiple times
    }
    
    // 2. Measure performance
    val duration = measureNanoTime {
        repeat(iterations) {
            // Operation under test
        }
    }
    
    // 3. Assert with CI-aware threshold
    val avgDuration = duration / iterations / 1_000_000
    avgDuration shouldBeLessThan (targetMs * ciMultiplier)
}
```

## Interpreting Results

**Test failures indicate:**
- Performance regression in the codebase
- Need to optimize the affected operation
- Possible need to adjust thresholds if hardware has changed

**In CI:**
- Failures are more likely due to resource contention
- Consider re-running the build before investigating
- Check CI runner specs if failures persist

## Adding New Performance Tests

When adding new tests:

1. Include a warmup phase
2. Use `ciMultiplier` for thresholds
3. Add descriptive comments explaining what's being measured
4. Document the performance target in the test name
5. Consider whether the test is reliable in CI

Example:
```kotlin
test("new operation should be fast") {
    // Warmup JIT
    warmup {
        newOperation()
    }
    
    val iterations = 1000
    val duration = measureNanoTime {
        repeat(iterations) {
            newOperation()
        }
    }
    
    val avgDurationMs = duration / iterations / 1_000_000
    avgDurationMs shouldBeLessThan (2L * ciMultiplier) // Local: 2ms, CI: 6ms
}
```

## Troubleshooting

**Tests fail locally but pass in CI:**
- Your machine may be slower than CI runners
- Check for background processes consuming CPU
- Consider increasing local thresholds

**Tests pass locally but fail in CI:**
- CI runner may be under heavy load
- Re-run the build
- Check if multiple builds are running simultaneously

**Inconsistent results:**
- Ensure warmup phase is adequate
- Check for GC interference (especially in memory tests)
- Consider increasing iteration count for more stable averages

## Related Documentation

- [Compose Performance Guidelines](../../../../../../.kiro/steering/compose-performance.md)
- [Build & Test Guidelines](../../../../../../docs/development/build-and-test.md)
- [Quick Reference](../../../../../../docs/development/quick-reference.md)
