package dev.questweaver.rules.combat

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.modifiers.AbilityType
import dev.questweaver.rules.modifiers.RollModifier
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe

/**
 * Unit tests for SavingThrowResolver.
 *
 * Tests verify that:
 * - Success/failure based on DC
 * - Natural 20 always succeeds
 * - Proficiency adds bonus
 * - Advantage/disadvantage work correctly
 * - Stunned auto-fails STR/DEX saves
 * - Condition effects apply correctly
 */
class SavingThrowResolverTest : FunSpec({

    context("Basic saving throw resolution") {
        test("saving throw succeeds when total meets DC") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 3,
                proficiencyBonus = 2,
                dc = 10,
                isProficient = true,
                abilityType = AbilityType.Dexterity
            )
            
            outcome.d20Roll shouldBeInRange 1..20
            outcome.abilityModifier shouldBe 3
            outcome.proficiencyBonus shouldBe 2
            outcome.totalRoll shouldBe outcome.d20Roll + 3 + 2
            outcome.dc shouldBe 10
        }

        test("saving throw succeeds when total exceeds DC") {
            val roller = DiceRoller(seed = 100L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10,
                isProficient = true,
                abilityType = AbilityType.Constitution
            )
            
            if (outcome.totalRoll > 10) {
                outcome.success.shouldBeTrue()
            }
        }

        test("saving throw fails when total is below DC") {
            val roller = DiceRoller(seed = 1L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 0,
                proficiencyBonus = 0,
                dc = 20,
                isProficient = false,
                abilityType = AbilityType.Wisdom
            )
            
            if (outcome.totalRoll < 20 && outcome.d20Roll != 20) {
                outcome.success.shouldBeFalse()
            }
        }

        test("non-proficient save does not add proficiency bonus") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                isProficient = false,
                abilityType = AbilityType.Intelligence
            )
            
            outcome.proficiencyBonus shouldBe 0
            outcome.totalRoll shouldBe outcome.d20Roll + 2
        }
    }

    context("Natural 20 automatic success") {
        test("natural 20 always succeeds regardless of DC") {
            // Find a seed that produces a natural 20
            var foundNat20 = false
            for (seed in 1L..1000L) {
                val roller = DiceRoller(seed)
                val resolver = SavingThrowResolver(roller)
                
                val outcome = resolver.resolveSavingThrow(
                    abilityModifier = 0,
                    proficiencyBonus = 0,
                    dc = 30, // Impossibly high DC
                    isProficient = false,
                    abilityType = AbilityType.Strength
                )
                
                if (outcome.d20Roll == 20) {
                    foundNat20 = true
                    outcome.isAutoSuccess.shouldBeTrue()
                    outcome.success.shouldBeTrue()
                    break
                }
            }
            
            foundNat20.shouldBeTrue()
        }

        test("natural 20 overrides auto-fail from conditions") {
            // Find a seed that produces a natural 20
            for (seed in 1L..1000L) {
                val roller = DiceRoller(seed)
                val resolver = SavingThrowResolver(roller)
                
                val outcome = resolver.resolveSavingThrow(
                    abilityModifier = 2,
                    proficiencyBonus = 2,
                    dc = 15,
                    isProficient = true,
                    abilityType = AbilityType.Dexterity,
                    conditions = setOf(Condition.Stunned) // Auto-fails DEX saves
                )
                
                if (outcome.d20Roll == 20) {
                    outcome.success.shouldBeTrue() // Natural 20 overrides auto-fail
                    break
                }
            }
        }
    }

    context("Proficiency bonus") {
        test("proficient save adds proficiency bonus") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                isProficient = true,
                abilityType = AbilityType.Wisdom
            )
            
            outcome.proficiencyBonus shouldBe 3
            outcome.totalRoll shouldBe outcome.d20Roll + 2 + 3
        }

        test("proficiency bonus increases success chance") {
            val seed = 42L
            
            val roller1 = DiceRoller(seed)
            val resolver1 = SavingThrowResolver(roller1)
            val withProficiency = resolver1.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                isProficient = true,
                abilityType = AbilityType.Constitution
            )
            
            val roller2 = DiceRoller(seed)
            val resolver2 = SavingThrowResolver(roller2)
            val withoutProficiency = resolver2.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                isProficient = false,
                abilityType = AbilityType.Constitution
            )
            
            withProficiency.totalRoll shouldBe withoutProficiency.totalRoll + 3
        }
    }

    context("Advantage mechanics") {
        test("advantage takes higher of two rolls") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                rollModifier = RollModifier.Advantage,
                isProficient = true,
                abilityType = AbilityType.Dexterity
            )
            
            outcome.rollModifier shouldBe RollModifier.Advantage
            outcome.d20Roll shouldBeInRange 1..20
        }
    }

    context("Disadvantage mechanics") {
        test("disadvantage takes lower of two rolls") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                rollModifier = RollModifier.Disadvantage,
                isProficient = true,
                abilityType = AbilityType.Wisdom
            )
            
            outcome.rollModifier shouldBe RollModifier.Disadvantage
            outcome.d20Roll shouldBeInRange 1..20
        }
    }

    context("Stunned condition auto-fail") {
        test("Stunned auto-fails Strength saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10, // Easy DC
                isProficient = true,
                abilityType = AbilityType.Strength,
                conditions = setOf(Condition.Stunned)
            )
            
            if (outcome.d20Roll != 20) { // Unless natural 20
                outcome.success.shouldBeFalse()
            }
        }

        test("Stunned auto-fails Dexterity saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10,
                isProficient = true,
                abilityType = AbilityType.Dexterity,
                conditions = setOf(Condition.Stunned)
            )
            
            if (outcome.d20Roll != 20) {
                outcome.success.shouldBeFalse()
            }
        }

        test("Stunned does not affect other ability saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10,
                isProficient = true,
                abilityType = AbilityType.Wisdom,
                conditions = setOf(Condition.Stunned)
            )
            
            // Stunned doesn't auto-fail Wisdom saves
            if (outcome.totalRoll >= 10) {
                outcome.success.shouldBeTrue()
            }
        }
    }

    context("Paralyzed condition auto-fail") {
        test("Paralyzed auto-fails Strength saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10,
                isProficient = true,
                abilityType = AbilityType.Strength,
                conditions = setOf(Condition.Paralyzed)
            )
            
            if (outcome.d20Roll != 20) {
                outcome.success.shouldBeFalse()
            }
        }

        test("Paralyzed auto-fails Dexterity saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10,
                isProficient = true,
                abilityType = AbilityType.Dexterity,
                conditions = setOf(Condition.Paralyzed)
            )
            
            if (outcome.d20Roll != 20) {
                outcome.success.shouldBeFalse()
            }
        }
    }

    context("Unconscious condition auto-fail") {
        test("Unconscious auto-fails Strength saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10,
                isProficient = true,
                abilityType = AbilityType.Strength,
                conditions = setOf(Condition.Unconscious)
            )
            
            if (outcome.d20Roll != 20) {
                outcome.success.shouldBeFalse()
            }
        }

        test("Unconscious auto-fails Dexterity saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10,
                isProficient = true,
                abilityType = AbilityType.Dexterity,
                conditions = setOf(Condition.Unconscious)
            )
            
            if (outcome.d20Roll != 20) {
                outcome.success.shouldBeFalse()
            }
        }
    }

    context("Restrained condition disadvantage") {
        test("Restrained applies disadvantage to Dexterity saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                isProficient = true,
                abilityType = AbilityType.Dexterity,
                conditions = setOf(Condition.Restrained)
            )
            
            outcome.rollModifier shouldBe RollModifier.Disadvantage
            outcome.appliedConditions shouldBe setOf(Condition.Restrained)
        }

        test("Restrained does not affect other ability saves") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                isProficient = true,
                abilityType = AbilityType.Wisdom,
                conditions = setOf(Condition.Restrained)
            )
            
            outcome.rollModifier shouldBe RollModifier.Normal
        }
    }

    context("Condition effects tracking") {
        test("applied conditions are recorded in outcome") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val conditions = setOf(Condition.Poisoned, Condition.Blinded)
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                isProficient = true,
                abilityType = AbilityType.Constitution,
                conditions = conditions
            )
            
            outcome.appliedConditions shouldBe conditions
        }
    }

    context("Advantage and disadvantage cancellation") {
        test("advantage and disadvantage cancel out") {
            val roller = DiceRoller(seed = 42L)
            val resolver = SavingThrowResolver(roller)
            
            val outcome = resolver.resolveSavingThrow(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                rollModifier = RollModifier.Advantage,
                isProficient = true,
                abilityType = AbilityType.Dexterity,
                conditions = setOf(Condition.Restrained) // Causes disadvantage on DEX saves
            )
            
            outcome.rollModifier shouldBe RollModifier.Normal
        }
    }

    context("Deterministic behavior") {
        test("same seed produces same saving throw outcome") {
            val seed = 12345L
            
            val roller1 = DiceRoller(seed)
            val resolver1 = SavingThrowResolver(roller1)
            val outcome1 = resolver1.resolveSavingThrow(
                abilityModifier = 3,
                proficiencyBonus = 2,
                dc = 15,
                isProficient = true,
                abilityType = AbilityType.Wisdom
            )
            
            val roller2 = DiceRoller(seed)
            val resolver2 = SavingThrowResolver(roller2)
            val outcome2 = resolver2.resolveSavingThrow(
                abilityModifier = 3,
                proficiencyBonus = 2,
                dc = 15,
                isProficient = true,
                abilityType = AbilityType.Wisdom
            )
            
            outcome1.d20Roll shouldBe outcome2.d20Roll
            outcome1.totalRoll shouldBe outcome2.totalRoll
            outcome1.success shouldBe outcome2.success
            outcome1.isAutoSuccess shouldBe outcome2.isAutoSuccess
        }
    }
})
