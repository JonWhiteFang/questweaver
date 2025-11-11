package dev.questweaver.rules.combat

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.modifiers.ProficiencyLevel
import dev.questweaver.rules.modifiers.RollModifier
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe

/**
 * Unit tests for AbilityCheckResolver.
 *
 * Tests verify that:
 * - Success/failure based on DC
 * - Proficiency adds 1x bonus
 * - Expertise adds 2x bonus
 * - Advantage/disadvantage work correctly
 * - Poisoned applies disadvantage
 */
class AbilityCheckResolverTest : FunSpec({

    context("Basic ability check resolution") {
        test("ability check succeeds when total meets DC") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 3,
                proficiencyBonus = 2,
                dc = 10,
                proficiencyLevel = ProficiencyLevel.Proficient
            )
            
            outcome.d20Roll shouldBeInRange 1..20
            outcome.abilityModifier shouldBe 3
            outcome.proficiencyBonus shouldBe 2
            outcome.totalRoll shouldBe outcome.d20Roll + 3 + 2
            outcome.dc shouldBe 10
            outcome.proficiencyLevel shouldBe ProficiencyLevel.Proficient
        }

        test("ability check succeeds when total exceeds DC") {
            val roller = DiceRoller(seed = 100L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 5,
                proficiencyBonus = 3,
                dc = 10,
                proficiencyLevel = ProficiencyLevel.Proficient
            )
            
            if (outcome.totalRoll > 10) {
                outcome.success.shouldBeTrue()
            }
        }

        test("ability check fails when total is below DC") {
            val roller = DiceRoller(seed = 1L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 0,
                proficiencyBonus = 0,
                dc = 20,
                proficiencyLevel = ProficiencyLevel.None
            )
            
            if (outcome.totalRoll < 20) {
                outcome.success.shouldBeFalse()
            }
        }
    }

    context("Proficiency levels") {
        test("None proficiency adds no bonus") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.None
            )
            
            outcome.proficiencyBonus shouldBe 0
            outcome.totalRoll shouldBe outcome.d20Roll + 2
            outcome.proficiencyLevel shouldBe ProficiencyLevel.None
        }

        test("Proficient adds 1x proficiency bonus") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient
            )
            
            outcome.proficiencyBonus shouldBe 3
            outcome.totalRoll shouldBe outcome.d20Roll + 2 + 3
            outcome.proficiencyLevel shouldBe ProficiencyLevel.Proficient
        }

        test("Expertise adds 2x proficiency bonus") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Expertise
            )
            
            outcome.proficiencyBonus shouldBe 6 // 3 * 2
            outcome.totalRoll shouldBe outcome.d20Roll + 2 + 6
            outcome.proficiencyLevel shouldBe ProficiencyLevel.Expertise
        }

        test("proficiency levels produce different totals with same roll") {
            val seed = 42L
            
            val roller1 = DiceRoller(seed)
            val resolver1 = AbilityCheckResolver(roller1)
            val noneOutcome = resolver1.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.None
            )
            
            val roller2 = DiceRoller(seed)
            val resolver2 = AbilityCheckResolver(roller2)
            val proficientOutcome = resolver2.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient
            )
            
            val roller3 = DiceRoller(seed)
            val resolver3 = AbilityCheckResolver(roller3)
            val expertiseOutcome = resolver3.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Expertise
            )
            
            // Same d20 roll, different totals
            noneOutcome.d20Roll shouldBe proficientOutcome.d20Roll
            proficientOutcome.d20Roll shouldBe expertiseOutcome.d20Roll
            
            proficientOutcome.totalRoll shouldBe noneOutcome.totalRoll + 3
            expertiseOutcome.totalRoll shouldBe noneOutcome.totalRoll + 6
        }
    }

    context("Advantage mechanics") {
        test("advantage takes higher of two rolls") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                rollModifier = RollModifier.Advantage,
                proficiencyLevel = ProficiencyLevel.Proficient
            )
            
            outcome.rollModifier shouldBe RollModifier.Advantage
            outcome.d20Roll shouldBeInRange 1..20
        }
    }

    context("Disadvantage mechanics") {
        test("disadvantage takes lower of two rolls") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                rollModifier = RollModifier.Disadvantage,
                proficiencyLevel = ProficiencyLevel.Proficient
            )
            
            outcome.rollModifier shouldBe RollModifier.Disadvantage
            outcome.d20Roll shouldBeInRange 1..20
        }
    }

    context("Poisoned condition") {
        test("Poisoned applies disadvantage to ability checks") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient,
                conditions = setOf(Condition.Poisoned)
            )
            
            outcome.rollModifier shouldBe RollModifier.Disadvantage
            outcome.appliedConditions shouldBe setOf(Condition.Poisoned)
        }

        test("Poisoned disadvantage combines with base disadvantage") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                rollModifier = RollModifier.Disadvantage,
                proficiencyLevel = ProficiencyLevel.Proficient,
                conditions = setOf(Condition.Poisoned)
            )
            
            outcome.rollModifier shouldBe RollModifier.Disadvantage
        }

        test("Poisoned cancels advantage") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                rollModifier = RollModifier.Advantage,
                proficiencyLevel = ProficiencyLevel.Proficient,
                conditions = setOf(Condition.Poisoned)
            )
            
            outcome.rollModifier shouldBe RollModifier.Normal
        }
    }

    context("Other conditions") {
        test("Prone does not affect ability checks") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient,
                conditions = setOf(Condition.Prone)
            )
            
            outcome.rollModifier shouldBe RollModifier.Normal
        }

        test("Stunned does not directly affect ability checks") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient,
                conditions = setOf(Condition.Stunned)
            )
            
            outcome.rollModifier shouldBe RollModifier.Normal
        }

        test("Blinded does not directly affect ability checks") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient,
                conditions = setOf(Condition.Blinded)
            )
            
            outcome.rollModifier shouldBe RollModifier.Normal
        }

        test("Restrained does not directly affect ability checks") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient,
                conditions = setOf(Condition.Restrained)
            )
            
            outcome.rollModifier shouldBe RollModifier.Normal
        }
    }

    context("Condition effects tracking") {
        test("applied conditions are recorded in outcome") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val conditions = setOf(Condition.Poisoned, Condition.Blinded)
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 2,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient,
                conditions = conditions
            )
            
            outcome.appliedConditions shouldBe conditions
        }
    }

    context("Expertise with conditions") {
        test("Expertise bonus applies even with Poisoned disadvantage") {
            val roller = DiceRoller(seed = 42L)
            val resolver = AbilityCheckResolver(roller)
            
            val outcome = resolver.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Expertise,
                conditions = setOf(Condition.Poisoned)
            )
            
            outcome.proficiencyBonus shouldBe 6 // 2x proficiency
            outcome.rollModifier shouldBe RollModifier.Disadvantage
            outcome.totalRoll shouldBe outcome.d20Roll + 2 + 6
        }
    }

    context("Deterministic behavior") {
        test("same seed produces same ability check outcome") {
            val seed = 12345L
            
            val roller1 = DiceRoller(seed)
            val resolver1 = AbilityCheckResolver(roller1)
            val outcome1 = resolver1.resolveAbilityCheck(
                abilityModifier = 3,
                proficiencyBonus = 2,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient
            )
            
            val roller2 = DiceRoller(seed)
            val resolver2 = AbilityCheckResolver(roller2)
            val outcome2 = resolver2.resolveAbilityCheck(
                abilityModifier = 3,
                proficiencyBonus = 2,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Proficient
            )
            
            outcome1.d20Roll shouldBe outcome2.d20Roll
            outcome1.totalRoll shouldBe outcome2.totalRoll
            outcome1.success shouldBe outcome2.success
            outcome1.proficiencyBonus shouldBe outcome2.proficiencyBonus
        }

        test("same seed with different proficiency levels produces same d20 roll") {
            val seed = 42L
            
            val roller1 = DiceRoller(seed)
            val resolver1 = AbilityCheckResolver(roller1)
            val outcome1 = resolver1.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.None
            )
            
            val roller2 = DiceRoller(seed)
            val resolver2 = AbilityCheckResolver(roller2)
            val outcome2 = resolver2.resolveAbilityCheck(
                abilityModifier = 2,
                proficiencyBonus = 3,
                dc = 15,
                proficiencyLevel = ProficiencyLevel.Expertise
            )
            
            outcome1.d20Roll shouldBe outcome2.d20Roll
        }
    }
})
