package dev.questweaver.domain.dice

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

/**
 * Property-based tests for DiceRoller exhaustive verification.
 *
 * Uses kotest-property to verify dice roller behavior across arbitrary seed values
 * and modifiers. Property-based testing ensures correctness for all possible inputs,
 * not just specific test cases.
 *
 * Tests verify:
 * - All die types return values within valid ranges for arbitrary seeds
 * - Advantage always returns value >= both individual rolls
 * - Disadvantage always returns value <= both individual rolls
 * - Multiple dice sum equals individual roll sum
 * - Modifiers are correctly applied for arbitrary values
 */
class DiceRollerPropertyTest : FunSpec({

    context("Die type range properties") {
        test("d20 returns 1-20 for arbitrary seeds") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d20()

                roll.rolls[0] shouldBeInRange 1..20
                roll.dieType shouldBe DieType.D20
            }
        }

        test("d4 returns 1-4 for arbitrary seeds") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d4()

                roll.rolls[0] shouldBeInRange 1..4
                roll.dieType shouldBe DieType.D4
            }
        }

        test("d6 returns 1-6 for arbitrary seeds") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d6()

                roll.rolls[0] shouldBeInRange 1..6
                roll.dieType shouldBe DieType.D6
            }
        }

        test("d8 returns 1-8 for arbitrary seeds") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d8()

                roll.rolls[0] shouldBeInRange 1..8
                roll.dieType shouldBe DieType.D8
            }
        }

        test("d10 returns 1-10 for arbitrary seeds") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d10()

                roll.rolls[0] shouldBeInRange 1..10
                roll.dieType shouldBe DieType.D10
            }
        }

        test("d12 returns 1-12 for arbitrary seeds") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d12()

                roll.rolls[0] shouldBeInRange 1..12
                roll.dieType shouldBe DieType.D12
            }
        }

        test("d100 returns 1-100 for arbitrary seeds") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d100()

                roll.rolls[0] shouldBeInRange 1..100
                roll.dieType shouldBe DieType.D100
            }
        }
    }

    context("Advantage properties") {
        test("advantage always >= both individual rolls") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.rollWithAdvantage()

                roll.selectedValue shouldBeGreaterThanOrEqual roll.rolls[0]
                roll.selectedValue shouldBeGreaterThanOrEqual roll.rolls[1]
                roll.selectedValue shouldBe roll.rolls.maxOrNull()
            }
        }

        test("advantage selected value is within valid d20 range") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.rollWithAdvantage()

                roll.selectedValue shouldBeInRange 1..20
            }
        }

        test("advantage with arbitrary modifier applies correctly") {
            checkAll(Arb.long(), Arb.int(-10..10)) { seed, modifier ->
                val roller = DiceRoller(seed)
                val roll = roller.rollWithAdvantage(modifier)

                roll.result shouldBe roll.selectedValue + modifier
                roll.modifier shouldBe modifier
            }
        }
    }

    context("Disadvantage properties") {
        test("disadvantage always <= both individual rolls") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.rollWithDisadvantage()

                roll.selectedValue shouldBeLessThanOrEqual roll.rolls[0]
                roll.selectedValue shouldBeLessThanOrEqual roll.rolls[1]
                roll.selectedValue shouldBe roll.rolls.minOrNull()
            }
        }

        test("disadvantage selected value is within valid d20 range") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.rollWithDisadvantage()

                roll.selectedValue shouldBeInRange 1..20
            }
        }

        test("disadvantage with arbitrary modifier applies correctly") {
            checkAll(Arb.long(), Arb.int(-10..10)) { seed, modifier ->
                val roller = DiceRoller(seed)
                val roll = roller.rollWithDisadvantage(modifier)

                roll.result shouldBe roll.selectedValue + modifier
                roll.modifier shouldBe modifier
            }
        }
    }

    context("Multiple dice properties") {
        test("multiple dice sum equals individual roll sum") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D6)

                roll.naturalTotal shouldBe roll.rolls.sum()
                roll.rolls.size shouldBe count
            }
        }

        test("multiple d4 rolls all within valid range") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D4)

                roll.rolls.forEach { it shouldBeInRange 1..4 }
            }
        }

        test("multiple d6 rolls all within valid range") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D6)

                roll.rolls.forEach { it shouldBeInRange 1..6 }
            }
        }

        test("multiple d8 rolls all within valid range") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D8)

                roll.rolls.forEach { it shouldBeInRange 1..8 }
            }
        }

        test("multiple d10 rolls all within valid range") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D10)

                roll.rolls.forEach { it shouldBeInRange 1..10 }
            }
        }

        test("multiple d12 rolls all within valid range") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D12)

                roll.rolls.forEach { it shouldBeInRange 1..12 }
            }
        }

        test("multiple d20 rolls all within valid range") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D20)

                roll.rolls.forEach { it shouldBeInRange 1..20 }
            }
        }

        test("multiple d100 rolls all within valid range") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D100)

                roll.rolls.forEach { it shouldBeInRange 1..100 }
            }
        }
    }

    context("Modifier properties") {
        test("modifier correctly applied for arbitrary values") {
            checkAll(Arb.long(), Arb.int(-20..20)) { seed, modifier ->
                val roller = DiceRoller(seed)
                val roll = roller.d20(modifier)

                roll.total shouldBe roll.naturalTotal + modifier
                roll.modifier shouldBe modifier
            }
        }

        test("modifier applied to multiple dice for arbitrary values") {
            checkAll(Arb.long(), Arb.int(1..5), Arb.int(-10..10)) { seed, count, modifier ->
                val roller = DiceRoller(seed)
                val roll = roller.roll(count, DieType.D6, modifier)

                roll.total shouldBe roll.naturalTotal + modifier
                roll.modifier shouldBe modifier
            }
        }

        test("positive modifier increases total") {
            checkAll(Arb.long(), Arb.int(1..20)) { seed, modifier ->
                val roller = DiceRoller(seed)
                val roll = roller.d20(modifier)

                roll.total shouldBe roll.naturalTotal + modifier
                roll.total shouldBeGreaterThanOrEqual roll.naturalTotal
            }
        }

        test("negative modifier decreases total") {
            checkAll(Arb.long(), Arb.int(-20..-1)) { seed, modifier ->
                val roller = DiceRoller(seed)
                val roll = roller.d20(modifier)

                roll.total shouldBe roll.naturalTotal + modifier
                roll.total shouldBeLessThanOrEqual roll.naturalTotal
            }
        }

        test("zero modifier leaves total unchanged") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d20(modifier = 0)

                roll.total shouldBe roll.naturalTotal
            }
        }
    }

    context("Determinism properties") {
        test("same seed produces same roll for all die types") {
            checkAll(Arb.long()) { seed ->
                val roller1 = DiceRoller(seed)
                val roller2 = DiceRoller(seed)

                // Test all die types
                roller1.d4().rolls[0] shouldBe roller2.d4().rolls[0]
                roller1.d6().rolls[0] shouldBe roller2.d6().rolls[0]
                roller1.d8().rolls[0] shouldBe roller2.d8().rolls[0]
                roller1.d10().rolls[0] shouldBe roller2.d10().rolls[0]
                roller1.d12().rolls[0] shouldBe roller2.d12().rolls[0]
                roller1.d20().rolls[0] shouldBe roller2.d20().rolls[0]
                roller1.d100().rolls[0] shouldBe roller2.d100().rolls[0]
            }
        }

        test("same seed produces same advantage rolls") {
            checkAll(Arb.long()) { seed ->
                val roller1 = DiceRoller(seed)
                val roller2 = DiceRoller(seed)

                val roll1 = roller1.rollWithAdvantage()
                val roll2 = roller2.rollWithAdvantage()

                roll1.rolls shouldBe roll2.rolls
                roll1.selectedValue shouldBe roll2.selectedValue
            }
        }

        test("same seed produces same disadvantage rolls") {
            checkAll(Arb.long()) { seed ->
                val roller1 = DiceRoller(seed)
                val roller2 = DiceRoller(seed)

                val roll1 = roller1.rollWithDisadvantage()
                val roll2 = roller2.rollWithDisadvantage()

                roll1.rolls shouldBe roll2.rolls
                roll1.selectedValue shouldBe roll2.selectedValue
            }
        }

        test("same seed produces same multiple dice rolls") {
            checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
                val roller1 = DiceRoller(seed)
                val roller2 = DiceRoller(seed)

                val roll1 = roller1.roll(count, DieType.D6)
                val roll2 = roller2.roll(count, DieType.D6)

                roll1.rolls shouldBe roll2.rolls
            }
        }
    }

    context("Roll type properties") {
        test("normal rolls have NORMAL roll type") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.d20()

                roll.rollType shouldBe RollType.NORMAL
            }
        }

        test("advantage rolls have ADVANTAGE roll type") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.rollWithAdvantage()

                roll.rollType shouldBe RollType.ADVANTAGE
            }
        }

        test("disadvantage rolls have DISADVANTAGE roll type") {
            checkAll(Arb.long()) { seed ->
                val roller = DiceRoller(seed)
                val roll = roller.rollWithDisadvantage()

                roll.rollType shouldBe RollType.DISADVANTAGE
            }
        }
    }
})
