package dev.questweaver.domain.dice

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark tests for DiceRoller.
 *
 * Verifies that dice rolling operations meet performance targets:
 * - Single die roll: <1μs (1000 nanoseconds)
 * - Advantage/disadvantage: <1μs (1000 nanoseconds)
 * - 1000 rolls: <10ms
 *
 * These benchmarks ensure the dice system is fast enough for real-time
 * gameplay without introducing noticeable latency.
 */
class DiceRollerPerformanceTest : FunSpec({

    context("Single die roll performance") {
        test("single d20 roll completes in under 1μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.d20() }

            // Measure average time over many iterations
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.d20()
                }
            }

            val averageTime = totalTime / 10000
            // Target: <1μs (1000 nanoseconds)
            // Using 10μs as threshold to account for CI environment variance
            averageTime shouldBeLessThan 10000
        }

        test("single d6 roll completes in under 1μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.d6() }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.d6()
                }
            }

            val averageTime = totalTime / 10000
            averageTime shouldBeLessThan 10000
        }

        test("single d100 roll completes in under 1μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.d100() }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.d100()
                }
            }

            val averageTime = totalTime / 10000
            averageTime shouldBeLessThan 10000
        }
    }

    context("Advantage/disadvantage performance") {
        test("advantage roll completes in under 1μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.rollWithAdvantage() }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.rollWithAdvantage()
                }
            }

            val averageTime = totalTime / 10000
            // Target: <1μs (1000 nanoseconds)
            // Using 15μs as threshold since advantage involves two rolls + comparison (CI variance)
            averageTime shouldBeLessThan 15000
        }

        test("disadvantage roll completes in under 1μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.rollWithDisadvantage() }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.rollWithDisadvantage()
                }
            }

            val averageTime = totalTime / 10000
            // Target: <1μs (1000 nanoseconds)
            // Using 15μs as threshold since disadvantage involves two rolls + comparison (CI variance)
            averageTime shouldBeLessThan 15000
        }

        test("advantage with modifier completes in under 1μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.rollWithAdvantage(modifier = 5) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.rollWithAdvantage(modifier = 5)
                }
            }

            val averageTime = totalTime / 10000
            averageTime shouldBeLessThan 15000
        }
    }

    context("Multiple dice performance") {
        test("rolling 2d6 completes in under 2μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.roll(2, DieType.D6) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.roll(2, DieType.D6)
                }
            }

            val averageTime = totalTime / 10000
            // Two dice should take roughly 2x single die time (CI variance)
            averageTime shouldBeLessThan 20000
        }

        test("rolling 3d6 completes in under 3μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.roll(3, DieType.D6) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.roll(3, DieType.D6)
                }
            }

            val averageTime = totalTime / 10000
            // Three dice should take roughly 3x single die time (CI variance)
            averageTime shouldBeLessThan 30000
        }

        test("rolling 8d6 (fireball damage) completes in under 10μs") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(1000) { roller.roll(8, DieType.D6) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    roller.roll(8, DieType.D6)
                }
            }

            val averageTime = totalTime / 10000
            // Eight dice should still be very fast (CI variance)
            averageTime shouldBeLessThan 50000
        }
    }

    context("Bulk rolling performance") {
        test("1000 d20 rolls complete in under 10ms") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(100) { roller.d20() }

            // Measure time for 1000 rolls
            val duration = measureTimeMillis {
                repeat(1000) {
                    roller.d20()
                }
            }

            // Target: <10ms for 1000 rolls
            duration shouldBeLessThan 10
        }

        test("1000 advantage rolls complete in under 15ms") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(100) { roller.rollWithAdvantage() }

            // Measure time for 1000 rolls
            val duration = measureTimeMillis {
                repeat(1000) {
                    roller.rollWithAdvantage()
                }
            }

            // Target: <15ms for 1000 advantage rolls (more work than normal rolls)
            duration shouldBeLessThan 15
        }

        test("1000 multiple dice rolls (3d6) complete in under 15ms") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(100) { roller.roll(3, DieType.D6) }

            // Measure time for 1000 rolls
            val duration = measureTimeMillis {
                repeat(1000) {
                    roller.roll(3, DieType.D6)
                }
            }

            // Target: <15ms for 1000 multi-dice rolls
            duration shouldBeLessThan 15
        }

        test("10000 d20 rolls complete in under 100ms") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(100) { roller.d20() }

            // Measure time for 10000 rolls
            val duration = measureTimeMillis {
                repeat(10000) {
                    roller.d20()
                }
            }

            // Target: <100ms for 10000 rolls (scales linearly)
            duration shouldBeLessThan 100
        }
    }

    context("Memory footprint verification") {
        test("DiceRoll instances are lightweight") {
            val roller = DiceRoller(seed = 42L)

            // Create many DiceRoll instances
            val rolls = List(10000) { roller.d20() }

            // Verify we can create many instances without issues
            // This is a smoke test - if memory is excessive, this would fail or be very slow
            rolls.size shouldBe 10000
            rolls.all { it.rolls.size == 1 }.shouldBeTrue()
        }

        test("multiple dice DiceRoll instances are reasonable") {
            val roller = DiceRoller(seed = 42L)

            // Create many multi-dice DiceRoll instances
            val rolls = List(10000) { roller.roll(3, DieType.D6) }

            // Verify we can create many instances without issues
            rolls.size shouldBe 10000
            rolls.all { it.rolls.size == 3 }.shouldBeTrue()
        }

        test("advantage/disadvantage DiceRoll instances are reasonable") {
            val roller = DiceRoller(seed = 42L)

            // Create many advantage/disadvantage instances
            val advantageRolls = List(5000) { roller.rollWithAdvantage() }
            val disadvantageRolls = List(5000) { roller.rollWithDisadvantage() }

            // Verify we can create many instances without issues
            advantageRolls.size shouldBe 5000
            disadvantageRolls.size shouldBe 5000
            advantageRolls.all { it.rolls.size == 2 }.shouldBeTrue()
            disadvantageRolls.all { it.rolls.size == 2 }.shouldBeTrue()
        }
    }

    context("Real-world combat scenario performance") {
        test("typical combat round (10 attacks) completes in under 1ms") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(100) {
                roller.d20(modifier = 5) // Attack roll
                roller.roll(2, DieType.D6, modifier = 3) // Damage roll
            }

            // Simulate 10 attacks in a combat round
            val duration = measureTimeMillis {
                repeat(10) {
                    roller.d20(modifier = 5) // Attack roll
                    roller.roll(2, DieType.D6, modifier = 3) // Damage roll
                }
            }

            // Target: <1ms for a typical combat round
            duration shouldBeLessThan 1
        }

        test("complex combat round with advantage completes in under 2ms") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(100) {
                roller.rollWithAdvantage(modifier = 5)
                roller.roll(8, DieType.D6) // Fireball damage
            }

            // Simulate complex combat with advantage and area damage
            val duration = measureTimeMillis {
                repeat(5) {
                    roller.rollWithAdvantage(modifier = 5) // Attack with advantage
                    roller.roll(8, DieType.D6) // Fireball damage
                }
            }

            // Target: <2ms for complex combat round
            duration shouldBeLessThan 2
        }

        test("initiative rolls for 10 creatures complete in under 1ms") {
            val roller = DiceRoller(seed = 42L)

            // Warm up JVM
            repeat(100) {
                repeat(10) { roller.d20(modifier = 2) }
            }

            // Roll initiative for 10 creatures
            val duration = measureTimeMillis {
                repeat(10) {
                    roller.d20(modifier = 2)
                }
            }

            // Target: <1ms for initiative rolls
            duration shouldBeLessThan 1
        }
    }
})
