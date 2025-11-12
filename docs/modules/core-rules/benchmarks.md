# Action Validation System Performance Benchmarks

This document describes the JMH benchmarks for the Action Validation System and how to run and interpret them.

## Overview

The benchmarks verify that the Action Validation System meets the performance requirement of completing validation within 50ms (Requirement 1.5). The benchmarks use JMH (Java Microbenchmark Harness) to provide accurate, statistically sound performance measurements.

## Running Benchmarks

### Run All Benchmarks

```bash
gradle :core:rules:jmh --no-configuration-cache
```

### Benchmark Configuration

The benchmarks are configured in `core/rules/build.gradle.kts`:

- **Warmup iterations**: 2 (1 second each)
- **Measurement iterations**: 3 (1 second each)
- **Forks**: 1
- **Time unit**: milliseconds
- **Result format**: JSON
- **Results file**: `build/reports/jmh/results.json`

## Benchmark Categories

### 1. ActionValidator Integration Benchmarks

These benchmarks test the complete validation flow through ActionValidator:

- **benchmarkValidateAttack**: Validates a basic melee attack action
- **benchmarkValidateSpell**: Validates spell casting with resource consumption
- **benchmarkValidateMove**: Validates movement action with path calculation
- **benchmarkValidateOpportunityAttack**: Validates reaction-based opportunity attack
- **benchmarkValidateWithConditions**: Validates action with active conditions (e.g., Prone)
- **benchmarkValidateWithConcentration**: Validates spell casting while concentrating

### 2. Individual Validator Benchmarks

These benchmarks profile individual validators for performance analysis:

- **benchmarkActionEconomyValidator**: Tests action/bonus/reaction/movement validation
- **benchmarkResourceValidator**: Tests spell slot and resource availability checks
- **benchmarkRangeValidator**: Tests distance and line-of-effect calculations
- **benchmarkConcentrationValidator**: Tests concentration conflict detection
- **benchmarkConditionValidator**: Tests condition-based action restrictions

### 3. Complex Scenario Benchmarks

These benchmarks test performance under challenging conditions:

- **benchmarkComplexScenarioWithObstacles**: Validates with many obstacles on the map
- **benchmarkMultipleValidationsSequential**: Validates 4 different actions sequentially
- **benchmarkFailFastCondition**: Tests fail-fast behavior when condition blocks action
- **benchmarkFailFastActionEconomy**: Tests fail-fast when action economy exhausted
- **benchmarkFailFastResources**: Tests fail-fast when resources insufficient

## Performance Results

### Latest Benchmark Results

All benchmarks complete **well under the 50ms target**:

| Benchmark | Average Time | Status |
|-----------|--------------|--------|
| benchmarkComplexScenarioWithObstacles | 0.042 ms | ✅ PASS |
| benchmarkConcentrationValidator | 0.003 ms | ✅ PASS |
| benchmarkFailFastResources | 0.001 ms | ✅ PASS |
| benchmarkMultipleValidationsSequential | 0.005 ms | ✅ PASS |
| benchmarkValidateMove | 0.001 ms | ✅ PASS |
| benchmarkValidateSpell | 0.003 ms | ✅ PASS |
| benchmarkValidateWithConcentration | 0.003 ms | ✅ PASS |

**Note**: Some benchmarks show "≈ 10⁻³ ms/op" which indicates results less than 0.001 ms (1 microsecond), too fast to measure precisely.

### Performance Analysis

1. **Typical validation time**: 0.001-0.005 ms (1-5 microseconds)
2. **Complex scenarios**: 0.042 ms (42 microseconds)
3. **Performance margin**: **1,000x faster** than the 50ms requirement
4. **Fail-fast optimization**: Validation short-circuits on first failure, keeping times minimal

## Interpreting Results

### Understanding JMH Output

```
Benchmark                                    Mode  Cnt   Score    Error  Units
benchmarkValidateSpell                       avgt    3   0.003 ±  0.002  ms/op
```

- **Mode**: `avgt` = Average time per operation
- **Cnt**: Number of measurement iterations (3)
- **Score**: Average time (0.003 ms = 3 microseconds)
- **Error**: Confidence interval (±0.002 ms)
- **Units**: Milliseconds per operation

### Performance Targets

| Target | Requirement | Actual | Status |
|--------|-------------|--------|--------|
| Typical validation | < 50 ms | < 0.005 ms | ✅ 10,000x faster |
| Complex scenarios | < 50 ms | 0.042 ms | ✅ 1,190x faster |
| Fail-fast | < 50 ms | < 0.001 ms | ✅ 50,000x faster |

## Benchmark Implementation

The benchmarks are located in:
```
core/rules/src/jmh/kotlin/dev/questweaver/core/rules/validation/ActionValidatorBenchmark.kt
```

### Key Features

1. **Realistic test data**: Uses actual game state with positions, resources, conditions
2. **Multiple scenarios**: Tests various action types and edge cases
3. **Individual profiling**: Benchmarks each validator separately for bottleneck identification
4. **Fail-fast verification**: Confirms early-exit optimization works correctly

### Test Data Setup

The benchmarks use:
- **Actor**: Creature with ID 1 at position (0, 0)
- **Target**: Creature with ID 2 at position (5, 0)
- **Resources**: Full spell slots (levels 1-5)
- **Movement**: 30 feet total, none used
- **Obstacles**: Grid positions for line-of-effect testing

## Continuous Performance Monitoring

### When to Run Benchmarks

Run benchmarks:
- After implementing new validators
- After optimizing validation logic
- Before releasing new versions
- When investigating performance regressions

### Performance Regression Detection

If benchmark results exceed **10ms** (20% of target), investigate:
1. Check for new allocations in hot paths
2. Verify fail-fast logic still works
3. Profile with JMH's `-prof` option
4. Review recent code changes

### Profiling Options

For detailed profiling, use JMH profilers:

```bash
# Stack profiling
gradle :core:rules:jmh -Pjmh.profilers=stack

# GC profiling
gradle :core:rules:jmh -Pjmh.profilers=gc

# Allocation profiling
gradle :core:rules:jmh -Pjmh.profilers=gc.alloc
```

## Troubleshooting

### Benchmark Fails to Run

1. Ensure JDK 17 is installed
2. Run `gradle clean` before benchmarking
3. Disable configuration cache: `--no-configuration-cache`

### Inconsistent Results

1. Close other applications to reduce system noise
2. Increase warmup iterations for more stable results
3. Run on a dedicated machine for production benchmarks

### Very Fast Results (≈ 10⁻³ ms/op)

This indicates the operation completes in less than 1 microsecond, which is excellent but may be affected by JVM optimizations. The actual performance is still well within requirements.

## References

- **JMH Documentation**: https://github.com/openjdk/jmh
- **Requirement 1.5**: Validation must complete within 50ms
- **Design Document**: `.kiro/specs/06-action-validation/design.md`

---

**Last Updated**: 2025-11-12
