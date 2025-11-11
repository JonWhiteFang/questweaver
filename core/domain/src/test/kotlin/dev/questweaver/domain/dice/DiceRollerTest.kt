package dev.questweaver.domain.dice

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for DiceRoller deterministic behavior.
 *
 * Tests verify that:
 * - Identical seeds produce identical roll sequences
 * - Different seeds produce different roll sequences
 * - All die types return values within valid ranges
 * - Advantage returns higher of two rolls
 * - Disadvantage returns lower of two rolls
 * - Modifiers are correctly applied to totals
 * - Multiple dice sum calculation is correct
 * - Input validation works correctly
 * - DiceRoll validation works correctly
 */
class DiceRollerTest : FunSpec({

    context("Deterministic behavior with seeded RNG") {
        test("identical seeds produce identical roll sequences") {
            val seed = 42L
            val roller1 = DiceRoller(seed)
            val roller2 = DiceRoller(seed)

            val rolls1 = List(10) { roller1.d20() }
            val rolls2 = List(10) { roller2.d20() }

            rolls1.map { it.rolls[0] } shouldBe rolls2.map { it.rolls[0] }
        }

        test("different seeds produce different roll sequences") {
            val roller1 = DiceRoller(seed = 42L)
            val roller2 = DiceRoller(seed = 43L)

            val rolls1 = List(100) { roller1.d20().rolls[0] }
            val rolls2 = List(100) { roller2.d20().rolls[0] }

            // With 100 rolls, different seeds should produce different sequences
            rolls1 shouldNotBe rolls2
        }

        test("same seed produces same sequence across different die types") {
            val seed = 12345L
            val roller1 = DiceRoller(seed)
            val roller2 = DiceRoller(seed)

            roller1.d4().rolls[0] shouldBe roller2.d4().rolls[0]
            roller1.d6().rolls[0] shouldBe roller2.d6().rolls[0]
            roller1.d8().rolls[0] shouldBe roller2.d8().rolls[0]
            roller1.d10().rolls[0] shouldBe roller2.d10().rolls[0]
            roller1.d12().rolls[0] shouldBe roller2.d12().rolls[0]
            roller1.d20().rolls[0] shouldBe roller2.d20().rolls[0]
            roller1.d100().rolls[0] shouldBe roller2.d100().rolls[0]
        }
    }

    context("Die type range validation") {
        test("d4 returns values between 1 and 4") {
            val roller = DiceRoller(seed = 42L)
            repeat(100) {
                val roll = roller.d4()
                roll.rolls[0] shouldBeInRange 1..4
            }
        }

        test("d6 returns values between 1 and 6") {
            val roller = DiceRoller(seed = 42L)
            repeat(100) {
                val roll = roller.d6()
                roll.rolls[0] shouldBeInRange 1..6
            }
        }

        test("d8 returns values between 1 and 8") {
            val roller = DiceRoller(seed = 42L)
            repeat(100) {
                val roll = roller.d8()
                roll.rolls[0] shouldBeInRange 1..8
            }
        }

        test("d10 returns values between 1 and 10") {
            val roller = DiceRoller(seed = 42L)
            repeat(100) {
                val roll = roller.d10()
                roll.rolls[0] shouldBeInRange 1..10
            }
        }

        test("d12 returns values between 1 and 12") {
            val roller = DiceRoller(seed = 42L)
            repeat(100) {
                val roll = roller.d12()
                roll.rolls[0] shouldBeInRange 1..12
            }
        }

        test("d20 returns values between 1 and 20") {
            val roller = DiceRoller(seed = 42L)
            repeat(100) {
                val roll = roller.d20()
                roll.rolls[0] shouldBeInRange 1..20
            }
        }

        test("d100 returns values between 1 and 100") {
            val roller = DiceRoller(seed = 42L)
            repeat(100) {
                val roll = roller.d100()
                roll.rolls[0] shouldBeInRange 1..100
            }
        }
    }

    context("Advantage mechanics") {
        test("advantage returns higher of two d20 rolls") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithAdvantage()

            roll.rolls shouldHaveSize 2
            roll.rollType shouldBe RollType.ADVANTAGE
            roll.selectedValue shouldBe roll.rolls.maxOrNull()
        }

        test("advantage roll contains both individual roll values") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithAdvantage()

            roll.rolls shouldHaveSize 2
            roll.rolls.forEach { it shouldBeInRange 1..20 }
        }

        test("advantage selected value is greater than or equal to both rolls") {
            val roller = DiceRoller(seed = 42L)
            repeat(50) {
                val roll = roller.rollWithAdvantage()
                roll.selectedValue shouldBe roll.rolls.maxOrNull()
                roll.selectedValue shouldBeInRange roll.rolls[0]..20
                roll.selectedValue shouldBeInRange roll.rolls[1]..20
            }
        }

        test("advantage applies modifier to selected value") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithAdvantage(modifier = 5)

            roll.modifier shouldBe 5
            roll.result shouldBe roll.selectedValue + 5
        }
    }

    context("Disadvantage mechanics") {
        test("disadvantage returns lower of two d20 rolls") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithDisadvantage()

            roll.rolls shouldHaveSize 2
            roll.rollType shouldBe RollType.DISADVANTAGE
            roll.selectedValue shouldBe roll.rolls.minOrNull()
        }

        test("disadvantage roll contains both individual roll values") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithDisadvantage()

            roll.rolls shouldHaveSize 2
            roll.rolls.forEach { it shouldBeInRange 1..20 }
        }

        test("disadvantage selected value is less than or equal to both rolls") {
            val roller = DiceRoller(seed = 42L)
            repeat(50) {
                val roll = roller.rollWithDisadvantage()
                roll.selectedValue shouldBe roll.rolls.minOrNull()
                roll.selectedValue shouldBeInRange 1..roll.rolls[0]
                roll.selectedValue shouldBeInRange 1..roll.rolls[1]
            }
        }

        test("disadvantage applies modifier to selected value") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithDisadvantage(modifier = 3)

            roll.modifier shouldBe 3
            roll.result shouldBe roll.selectedValue + 3
        }
    }

    context("Modifier application") {
        test("positive modifier is correctly applied to total") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d20(modifier = 5)

            roll.modifier shouldBe 5
            roll.total shouldBe roll.naturalTotal + 5
        }

        test("negative modifier is correctly applied to total") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d20(modifier = -2)

            roll.modifier shouldBe -2
            roll.total shouldBe roll.naturalTotal - 2
        }

        test("zero modifier does not change total") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d20(modifier = 0)

            roll.modifier shouldBe 0
            roll.total shouldBe roll.naturalTotal
        }

        test("modifier is applied to multiple dice total") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 3, die = DieType.D6, modifier = 4)

            roll.modifier shouldBe 4
            roll.total shouldBe roll.naturalTotal + 4
        }
    }

    context("Multiple dice sum calculation") {
        test("multiple dice sum equals sum of individual rolls") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 3, die = DieType.D6)

            roll.rolls shouldHaveSize 3
            roll.naturalTotal shouldBe roll.rolls.sum()
        }

        test("rolling 2d6 produces two individual results") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 2, die = DieType.D6)

            roll.rolls shouldHaveSize 2
            roll.rolls.forEach { it shouldBeInRange 1..6 }
        }

        test("rolling 4d8 produces four individual results") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 4, die = DieType.D8)

            roll.rolls shouldHaveSize 4
            roll.rolls.forEach { it shouldBeInRange 1..8 }
        }

        test("multiple dice with modifier calculates total correctly") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 2, die = DieType.D6, modifier = 3)

            roll.total shouldBe roll.rolls.sum() + 3
        }
    }

    context("Input validation") {
        test("count less than 1 throws IllegalArgumentException") {
            val roller = DiceRoller(seed = 42L)

            shouldThrow<IllegalArgumentException> {
                roller.roll(count = 0, die = DieType.D6)
            }
        }

        test("negative count throws IllegalArgumentException") {
            val roller = DiceRoller(seed = 42L)

            shouldThrow<IllegalArgumentException> {
                roller.roll(count = -1, die = DieType.D6)
            }
        }

        test("count of 1 is valid") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 1, die = DieType.D6)

            roll.rolls shouldHaveSize 1
        }

        test("large count is valid") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 10, die = DieType.D6)

            roll.rolls shouldHaveSize 10
        }
    }

    context("DiceRoll validation") {
        test("empty rolls list throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                DiceRoll(
                    dieType = DieType.D20,
                    rolls = emptyList(),
                    modifier = 0,
                    rollType = RollType.NORMAL
                )
            }
        }

        test("roll value out of range throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                DiceRoll(
                    dieType = DieType.D20,
                    rolls = listOf(21), // Out of range for d20
                    modifier = 0,
                    rollType = RollType.NORMAL
                )
            }
        }

        test("roll value below minimum throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                DiceRoll(
                    dieType = DieType.D20,
                    rolls = listOf(0), // Below minimum for d20
                    modifier = 0,
                    rollType = RollType.NORMAL
                )
            }
        }

        test("valid roll values are accepted") {
            val roll = DiceRoll(
                dieType = DieType.D20,
                rolls = listOf(10, 15),
                modifier = 0,
                rollType = RollType.NORMAL
            )

            roll.rolls shouldBe listOf(10, 15)
        }
    }

    context("Convenience methods") {
        test("d4 convenience method delegates to roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d4(modifier = 2)

            roll.dieType shouldBe DieType.D4
            roll.modifier shouldBe 2
            roll.rolls[0] shouldBeInRange 1..4
        }

        test("d6 convenience method delegates to roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d6(modifier = 1)

            roll.dieType shouldBe DieType.D6
            roll.modifier shouldBe 1
            roll.rolls[0] shouldBeInRange 1..6
        }

        test("d8 convenience method delegates to roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d8()

            roll.dieType shouldBe DieType.D8
            roll.rolls[0] shouldBeInRange 1..8
        }

        test("d10 convenience method delegates to roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d10()

            roll.dieType shouldBe DieType.D10
            roll.rolls[0] shouldBeInRange 1..10
        }

        test("d12 convenience method delegates to roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d12()

            roll.dieType shouldBe DieType.D12
            roll.rolls[0] shouldBeInRange 1..12
        }

        test("d20 convenience method delegates to roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d20()

            roll.dieType shouldBe DieType.D20
            roll.rolls[0] shouldBeInRange 1..20
        }

        test("d100 convenience method delegates to roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d100()

            roll.dieType shouldBe DieType.D100
            roll.rolls[0] shouldBeInRange 1..100
        }
    }

    context("DiceRoll computed properties") {
        test("naturalTotal is sum of rolls for normal roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 3, die = DieType.D6)

            roll.naturalTotal shouldBe roll.rolls.sum()
        }

        test("total includes modifier for normal roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.roll(count = 2, die = DieType.D6, modifier = 3)

            roll.total shouldBe roll.naturalTotal + 3
        }

        test("selectedValue equals naturalTotal for normal roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d20()

            roll.selectedValue shouldBe roll.naturalTotal
        }

        test("result equals total for normal roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.d20(modifier = 5)

            roll.result shouldBe roll.total
        }

        test("selectedValue is max for advantage roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithAdvantage()

            roll.selectedValue shouldBe roll.rolls.maxOrNull()
        }

        test("result includes modifier for advantage roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithAdvantage(modifier = 2)

            roll.result shouldBe roll.selectedValue + 2
        }

        test("selectedValue is min for disadvantage roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithDisadvantage()

            roll.selectedValue shouldBe roll.rolls.minOrNull()
        }

        test("result includes modifier for disadvantage roll") {
            val roller = DiceRoller(seed = 42L)
            val roll = roller.rollWithDisadvantage(modifier = 3)

            roll.result shouldBe roll.selectedValue + 3
        }
    }
})
