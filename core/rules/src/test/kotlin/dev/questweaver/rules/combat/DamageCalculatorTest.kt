package dev.questweaver.rules.combat

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.rules.modifiers.DamageModifier
import dev.questweaver.rules.modifiers.DamageType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Unit tests for DamageCalculator.
 *
 * Tests verify that:
 * - Basic damage calculation (dice + modifier)
 * - Critical hits double dice (not modifier)
 * - Resistance halves damage (rounded down)
 * - Vulnerability doubles damage
 * - Immunity reduces damage to zero
 * - Multiple modifiers apply correctly
 */
class DamageCalculatorTest : FunSpec({

    context("Basic damage calculation") {
        test("calculates damage from dice and modifier") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Slashing,
                isCritical = false,
                targetModifiers = emptySet()
            )
            
            outcome.diceRolls shouldHaveSize 2
            outcome.diceRolls.forEach { it shouldBeInRange 1..6 }
            outcome.diceTotal shouldBe outcome.diceRolls.sum()
            outcome.damageModifier shouldBe 3
            outcome.baseDamage shouldBe outcome.diceTotal + 3
            outcome.finalDamage shouldBe outcome.baseDamage
            outcome.isCritical shouldBe false
        }

        test("handles zero modifier") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "1d8",
                damageModifier = 0,
                damageType = DamageType.Piercing,
                isCritical = false,
                targetModifiers = emptySet()
            )
            
            outcome.damageModifier shouldBe 0
            outcome.baseDamage shouldBe outcome.diceTotal
            outcome.finalDamage shouldBe outcome.diceTotal
        }

        test("handles negative modifier") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "1d6",
                damageModifier = -1,
                damageType = DamageType.Bludgeoning,
                isCritical = false,
                targetModifiers = emptySet()
            )
            
            outcome.damageModifier shouldBe -1
            outcome.baseDamage shouldBe outcome.diceTotal - 1
        }

        test("parses various dice expressions") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            // Test different dice types
            val d4Outcome = calculator.calculateDamage("1d4", 0, DamageType.Fire, false, emptySet())
            d4Outcome.diceRolls shouldHaveSize 1
            d4Outcome.diceRolls[0] shouldBeInRange 1..4
            
            val d8Outcome = calculator.calculateDamage("1d8", 0, DamageType.Cold, false, emptySet())
            d8Outcome.diceRolls shouldHaveSize 1
            d8Outcome.diceRolls[0] shouldBeInRange 1..8
            
            val d12Outcome = calculator.calculateDamage("1d12", 0, DamageType.Lightning, false, emptySet())
            d12Outcome.diceRolls shouldHaveSize 1
            d12Outcome.diceRolls[0] shouldBeInRange 1..12
        }
    }

    context("Critical hit mechanics") {
        test("critical hit doubles dice count") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Slashing,
                isCritical = true,
                targetModifiers = emptySet()
            )
            
            outcome.diceRolls shouldHaveSize 4 // 2d6 becomes 4d6
            outcome.isCritical shouldBe true
        }

        test("critical hit does not double modifier") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "1d8",
                damageModifier = 5,
                damageType = DamageType.Piercing,
                isCritical = true,
                targetModifiers = emptySet()
            )
            
            outcome.diceRolls shouldHaveSize 2 // 1d8 becomes 2d8
            outcome.damageModifier shouldBe 5 // Modifier stays the same
            outcome.baseDamage shouldBe outcome.diceTotal + 5
        }

        test("critical hit with 3d4 becomes 6d4") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "3d4",
                damageModifier = 2,
                damageType = DamageType.Fire,
                isCritical = true,
                targetModifiers = emptySet()
            )
            
            outcome.diceRolls shouldHaveSize 6
            outcome.diceRolls.forEach { it shouldBeInRange 1..4 }
        }
    }

    context("Resistance mechanics") {
        test("resistance halves damage rounded down") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Fire,
                isCritical = false,
                targetModifiers = setOf(DamageModifier.Resistance(DamageType.Fire))
            )
            
            outcome.finalDamage shouldBe outcome.baseDamage / 2
            outcome.appliedModifiers shouldBe setOf(DamageModifier.Resistance(DamageType.Fire))
        }

        test("resistance rounds down odd damage") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            // Find a seed that produces odd damage
            for (seed in 1L..100L) {
                val testRoller = DiceRoller(seed)
                val testCalculator = DamageCalculator(testRoller)
                
                val outcome = testCalculator.calculateDamage(
                    damageDice = "1d6",
                    damageModifier = 0,
                    damageType = DamageType.Cold,
                    isCritical = false,
                    targetModifiers = setOf(DamageModifier.Resistance(DamageType.Cold))
                )
                
                if (outcome.baseDamage % 2 == 1) {
                    // Odd damage should round down
                    outcome.finalDamage shouldBe outcome.baseDamage / 2
                    break
                }
            }
        }

        test("resistance only applies to matching damage type") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Fire,
                isCritical = false,
                targetModifiers = setOf(DamageModifier.Resistance(DamageType.Cold)) // Different type
            )
            
            outcome.finalDamage shouldBe outcome.baseDamage // No reduction
            outcome.appliedModifiers shouldBe emptySet()
        }
    }

    context("Vulnerability mechanics") {
        test("vulnerability doubles damage") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Fire,
                isCritical = false,
                targetModifiers = setOf(DamageModifier.Vulnerability(DamageType.Fire))
            )
            
            outcome.finalDamage shouldBe outcome.baseDamage * 2
            outcome.appliedModifiers shouldBe setOf(DamageModifier.Vulnerability(DamageType.Fire))
        }

        test("vulnerability only applies to matching damage type") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Fire,
                isCritical = false,
                targetModifiers = setOf(DamageModifier.Vulnerability(DamageType.Lightning))
            )
            
            outcome.finalDamage shouldBe outcome.baseDamage
            outcome.appliedModifiers shouldBe emptySet()
        }
    }

    context("Immunity mechanics") {
        test("immunity reduces damage to zero") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Poison,
                isCritical = false,
                targetModifiers = setOf(DamageModifier.Immunity(DamageType.Poison))
            )
            
            outcome.finalDamage shouldBe 0
            outcome.baseDamage shouldBeGreaterThan 0 // Base damage is still calculated
            outcome.appliedModifiers shouldBe setOf(DamageModifier.Immunity(DamageType.Poison))
        }

        test("immunity only applies to matching damage type") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Fire,
                isCritical = false,
                targetModifiers = setOf(DamageModifier.Immunity(DamageType.Poison))
            )
            
            outcome.finalDamage shouldBe outcome.baseDamage
            outcome.appliedModifiers shouldBe emptySet()
        }
    }

    context("Multiple modifiers") {
        test("immunity overrides resistance") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Fire,
                isCritical = false,
                targetModifiers = setOf(
                    DamageModifier.Immunity(DamageType.Fire),
                    DamageModifier.Resistance(DamageType.Fire)
                )
            )
            
            outcome.finalDamage shouldBe 0 // Immunity takes precedence
        }

        test("immunity overrides vulnerability") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Fire,
                isCritical = false,
                targetModifiers = setOf(
                    DamageModifier.Immunity(DamageType.Fire),
                    DamageModifier.Vulnerability(DamageType.Fire)
                )
            )
            
            outcome.finalDamage shouldBe 0 // Immunity takes precedence
        }

        test("resistance and vulnerability cancel out") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            val outcome = calculator.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Fire,
                isCritical = false,
                targetModifiers = setOf(
                    DamageModifier.Resistance(DamageType.Fire),
                    DamageModifier.Vulnerability(DamageType.Fire)
                )
            )
            
            // Resistance halves, vulnerability doubles: (base / 2) * 2 = base
            outcome.finalDamage shouldBe outcome.baseDamage
        }
    }

    context("Input validation") {
        test("invalid dice format throws exception") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            shouldThrow<IllegalArgumentException> {
                calculator.calculateDamage(
                    damageDice = "invalid",
                    damageModifier = 0,
                    damageType = DamageType.Fire,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
            }
        }

        test("unsupported die type throws exception") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            shouldThrow<IllegalArgumentException> {
                calculator.calculateDamage(
                    damageDice = "2d7", // d7 is not supported
                    damageModifier = 0,
                    damageType = DamageType.Fire,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
            }
        }

        test("zero dice count throws exception") {
            val roller = DiceRoller(seed = 42L)
            val calculator = DamageCalculator(roller)
            
            shouldThrow<IllegalArgumentException> {
                calculator.calculateDamage(
                    damageDice = "0d6",
                    damageModifier = 0,
                    damageType = DamageType.Fire,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
            }
        }
    }

    context("Deterministic behavior") {
        test("same seed produces same damage outcome") {
            val seed = 12345L
            
            val roller1 = DiceRoller(seed)
            val calculator1 = DamageCalculator(roller1)
            val outcome1 = calculator1.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Slashing,
                isCritical = false,
                targetModifiers = emptySet()
            )
            
            val roller2 = DiceRoller(seed)
            val calculator2 = DamageCalculator(roller2)
            val outcome2 = calculator2.calculateDamage(
                damageDice = "2d6",
                damageModifier = 3,
                damageType = DamageType.Slashing,
                isCritical = false,
                targetModifiers = emptySet()
            )
            
            outcome1.diceRolls shouldBe outcome2.diceRolls
            outcome1.diceTotal shouldBe outcome2.diceTotal
            outcome1.baseDamage shouldBe outcome2.baseDamage
            outcome1.finalDamage shouldBe outcome2.finalDamage
        }
    }
})
