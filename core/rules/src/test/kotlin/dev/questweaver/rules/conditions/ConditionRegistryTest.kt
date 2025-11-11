package dev.questweaver.rules.conditions

import dev.questweaver.rules.modifiers.AbilityType
import dev.questweaver.rules.modifiers.RollModifier
import dev.questweaver.rules.modifiers.SaveEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Unit tests for ConditionRegistry.
 *
 * Tests verify that:
 * - Each condition applies correct modifiers
 * - Multiple conditions stack appropriately
 * - All condition effects are covered
 */
class ConditionRegistryTest : FunSpec({

    context("Prone condition effects") {
        test("Prone gives disadvantage to attacker") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Prone, isAttacker = true)
            effect shouldBe RollModifier.Disadvantage
        }

        test("Prone gives advantage to attacks against target") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Prone, isAttacker = false)
            effect shouldBe RollModifier.Advantage
        }

        test("Prone does not affect saving throws") {
            val strEffect = ConditionRegistry.getSavingThrowEffect(Condition.Prone, AbilityType.Strength)
            val dexEffect = ConditionRegistry.getSavingThrowEffect(Condition.Prone, AbilityType.Dexterity)
            val conEffect = ConditionRegistry.getSavingThrowEffect(Condition.Prone, AbilityType.Constitution)
            
            strEffect shouldBe null
            dexEffect shouldBe null
            conEffect shouldBe null
        }

        test("Prone does not affect ability checks") {
            val effect = ConditionRegistry.getAbilityCheckEffect(Condition.Prone)
            effect shouldBe null
        }

        test("Prone does not prevent actions") {
            ConditionRegistry.preventsActions(Condition.Prone).shouldBeFalse()
        }
    }

    context("Stunned condition effects") {
        test("Stunned does not affect attacker rolls") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Stunned, isAttacker = true)
            effect shouldBe null
        }

        test("Stunned gives advantage to attacks against target") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Stunned, isAttacker = false)
            effect shouldBe RollModifier.Advantage
        }

        test("Stunned auto-fails Strength saves") {
            val effect = ConditionRegistry.getSavingThrowEffect(Condition.Stunned, AbilityType.Strength)
            effect shouldBe SaveEffect.AutoFail
        }

        test("Stunned auto-fails Dexterity saves") {
            val effect = ConditionRegistry.getSavingThrowEffect(Condition.Stunned, AbilityType.Dexterity)
            effect shouldBe SaveEffect.AutoFail
        }

        test("Stunned does not affect other saves") {
            val conEffect = ConditionRegistry.getSavingThrowEffect(Condition.Stunned, AbilityType.Constitution)
            val intEffect = ConditionRegistry.getSavingThrowEffect(Condition.Stunned, AbilityType.Intelligence)
            val wisEffect = ConditionRegistry.getSavingThrowEffect(Condition.Stunned, AbilityType.Wisdom)
            val chaEffect = ConditionRegistry.getSavingThrowEffect(Condition.Stunned, AbilityType.Charisma)
            
            conEffect shouldBe null
            intEffect shouldBe null
            wisEffect shouldBe null
            chaEffect shouldBe null
        }

        test("Stunned does not affect ability checks") {
            val effect = ConditionRegistry.getAbilityCheckEffect(Condition.Stunned)
            effect shouldBe null
        }

        test("Stunned prevents actions") {
            ConditionRegistry.preventsActions(Condition.Stunned).shouldBeTrue()
        }
    }

    context("Poisoned condition effects") {
        test("Poisoned gives disadvantage to attacker") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Poisoned, isAttacker = true)
            effect shouldBe RollModifier.Disadvantage
        }

        test("Poisoned does not affect attacks against target") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Poisoned, isAttacker = false)
            effect shouldBe null
        }

        test("Poisoned does not affect saving throws") {
            val strEffect = ConditionRegistry.getSavingThrowEffect(Condition.Poisoned, AbilityType.Strength)
            val dexEffect = ConditionRegistry.getSavingThrowEffect(Condition.Poisoned, AbilityType.Dexterity)
            
            strEffect shouldBe null
            dexEffect shouldBe null
        }

        test("Poisoned gives disadvantage to ability checks") {
            val effect = ConditionRegistry.getAbilityCheckEffect(Condition.Poisoned)
            effect shouldBe RollModifier.Disadvantage
        }

        test("Poisoned does not prevent actions") {
            ConditionRegistry.preventsActions(Condition.Poisoned).shouldBeFalse()
        }
    }

    context("Blinded condition effects") {
        test("Blinded gives disadvantage to attacker") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Blinded, isAttacker = true)
            effect shouldBe RollModifier.Disadvantage
        }

        test("Blinded gives advantage to attacks against target") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Blinded, isAttacker = false)
            effect shouldBe RollModifier.Advantage
        }

        test("Blinded does not affect saving throws") {
            val strEffect = ConditionRegistry.getSavingThrowEffect(Condition.Blinded, AbilityType.Strength)
            val dexEffect = ConditionRegistry.getSavingThrowEffect(Condition.Blinded, AbilityType.Dexterity)
            
            strEffect shouldBe null
            dexEffect shouldBe null
        }

        test("Blinded does not affect ability checks") {
            val effect = ConditionRegistry.getAbilityCheckEffect(Condition.Blinded)
            effect shouldBe null
        }

        test("Blinded does not prevent actions") {
            ConditionRegistry.preventsActions(Condition.Blinded).shouldBeFalse()
        }
    }

    context("Restrained condition effects") {
        test("Restrained gives disadvantage to attacker") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Restrained, isAttacker = true)
            effect shouldBe RollModifier.Disadvantage
        }

        test("Restrained gives advantage to attacks against target") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Restrained, isAttacker = false)
            effect shouldBe RollModifier.Advantage
        }

        test("Restrained gives disadvantage to Dexterity saves") {
            val effect = ConditionRegistry.getSavingThrowEffect(Condition.Restrained, AbilityType.Dexterity)
            effect shouldBe SaveEffect.Disadvantage
        }

        test("Restrained does not affect other saves") {
            val strEffect = ConditionRegistry.getSavingThrowEffect(Condition.Restrained, AbilityType.Strength)
            val conEffect = ConditionRegistry.getSavingThrowEffect(Condition.Restrained, AbilityType.Constitution)
            
            strEffect shouldBe null
            conEffect shouldBe null
        }

        test("Restrained does not affect ability checks") {
            val effect = ConditionRegistry.getAbilityCheckEffect(Condition.Restrained)
            effect shouldBe null
        }

        test("Restrained does not prevent actions") {
            ConditionRegistry.preventsActions(Condition.Restrained).shouldBeFalse()
        }
    }

    context("Incapacitated condition effects") {
        test("Incapacitated does not affect attacker rolls") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Incapacitated, isAttacker = true)
            effect shouldBe null
        }

        test("Incapacitated does not affect attacks against target") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Incapacitated, isAttacker = false)
            effect shouldBe null
        }

        test("Incapacitated does not affect saving throws") {
            val strEffect = ConditionRegistry.getSavingThrowEffect(Condition.Incapacitated, AbilityType.Strength)
            val dexEffect = ConditionRegistry.getSavingThrowEffect(Condition.Incapacitated, AbilityType.Dexterity)
            
            strEffect shouldBe null
            dexEffect shouldBe null
        }

        test("Incapacitated does not affect ability checks") {
            val effect = ConditionRegistry.getAbilityCheckEffect(Condition.Incapacitated)
            effect shouldBe null
        }

        test("Incapacitated prevents actions") {
            ConditionRegistry.preventsActions(Condition.Incapacitated).shouldBeTrue()
        }
    }

    context("Paralyzed condition effects") {
        test("Paralyzed does not affect attacker rolls") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Paralyzed, isAttacker = true)
            effect shouldBe null
        }

        test("Paralyzed gives advantage to attacks against target") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Paralyzed, isAttacker = false)
            effect shouldBe RollModifier.Advantage
        }

        test("Paralyzed auto-fails Strength saves") {
            val effect = ConditionRegistry.getSavingThrowEffect(Condition.Paralyzed, AbilityType.Strength)
            effect shouldBe SaveEffect.AutoFail
        }

        test("Paralyzed auto-fails Dexterity saves") {
            val effect = ConditionRegistry.getSavingThrowEffect(Condition.Paralyzed, AbilityType.Dexterity)
            effect shouldBe SaveEffect.AutoFail
        }

        test("Paralyzed does not affect other saves") {
            val conEffect = ConditionRegistry.getSavingThrowEffect(Condition.Paralyzed, AbilityType.Constitution)
            val intEffect = ConditionRegistry.getSavingThrowEffect(Condition.Paralyzed, AbilityType.Intelligence)
            
            conEffect shouldBe null
            intEffect shouldBe null
        }

        test("Paralyzed does not affect ability checks") {
            val effect = ConditionRegistry.getAbilityCheckEffect(Condition.Paralyzed)
            effect shouldBe null
        }

        test("Paralyzed prevents actions") {
            ConditionRegistry.preventsActions(Condition.Paralyzed).shouldBeTrue()
        }
    }

    context("Unconscious condition effects") {
        test("Unconscious does not affect attacker rolls") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Unconscious, isAttacker = true)
            effect shouldBe null
        }

        test("Unconscious gives advantage to attacks against target") {
            val effect = ConditionRegistry.getAttackRollEffect(Condition.Unconscious, isAttacker = false)
            effect shouldBe RollModifier.Advantage
        }

        test("Unconscious auto-fails Strength saves") {
            val effect = ConditionRegistry.getSavingThrowEffect(Condition.Unconscious, AbilityType.Strength)
            effect shouldBe SaveEffect.AutoFail
        }

        test("Unconscious auto-fails Dexterity saves") {
            val effect = ConditionRegistry.getSavingThrowEffect(Condition.Unconscious, AbilityType.Dexterity)
            effect shouldBe SaveEffect.AutoFail
        }

        test("Unconscious does not affect other saves") {
            val conEffect = ConditionRegistry.getSavingThrowEffect(Condition.Unconscious, AbilityType.Constitution)
            val wisEffect = ConditionRegistry.getSavingThrowEffect(Condition.Unconscious, AbilityType.Wisdom)
            
            conEffect shouldBe null
            wisEffect shouldBe null
        }

        test("Unconscious does not affect ability checks") {
            val effect = ConditionRegistry.getAbilityCheckEffect(Condition.Unconscious)
            effect shouldBe null
        }

        test("Unconscious prevents actions") {
            ConditionRegistry.preventsActions(Condition.Unconscious).shouldBeTrue()
        }
    }

    context("Multiple conditions") {
        test("multiple conditions can apply different effects") {
            val proneEffect = ConditionRegistry.getAttackRollEffect(Condition.Prone, isAttacker = true)
            val poisonedEffect = ConditionRegistry.getAttackRollEffect(Condition.Poisoned, isAttacker = true)
            
            proneEffect shouldBe RollModifier.Disadvantage
            poisonedEffect shouldBe RollModifier.Disadvantage
        }

        test("conditions affecting different mechanics don't interfere") {
            val poisonedAttack = ConditionRegistry.getAttackRollEffect(Condition.Poisoned, isAttacker = true)
            val poisonedCheck = ConditionRegistry.getAbilityCheckEffect(Condition.Poisoned)
            
            poisonedAttack shouldBe RollModifier.Disadvantage
            poisonedCheck shouldBe RollModifier.Disadvantage
        }
    }

    context("Exhaustive condition coverage") {
        test("all conditions have attack roll effects defined") {
            val conditions = listOf(
                Condition.Prone,
                Condition.Stunned,
                Condition.Poisoned,
                Condition.Blinded,
                Condition.Restrained,
                Condition.Incapacitated,
                Condition.Paralyzed,
                Condition.Unconscious
            )
            
            // Should not throw exceptions
            conditions.forEach { condition ->
                ConditionRegistry.getAttackRollEffect(condition, isAttacker = true)
                ConditionRegistry.getAttackRollEffect(condition, isAttacker = false)
            }
        }

        test("all conditions have saving throw effects defined") {
            val conditions = listOf(
                Condition.Prone,
                Condition.Stunned,
                Condition.Poisoned,
                Condition.Blinded,
                Condition.Restrained,
                Condition.Incapacitated,
                Condition.Paralyzed,
                Condition.Unconscious
            )
            
            val abilities = listOf(
                AbilityType.Strength,
                AbilityType.Dexterity,
                AbilityType.Constitution,
                AbilityType.Intelligence,
                AbilityType.Wisdom,
                AbilityType.Charisma
            )
            
            // Should not throw exceptions
            conditions.forEach { condition ->
                abilities.forEach { ability ->
                    ConditionRegistry.getSavingThrowEffect(condition, ability)
                }
            }
        }

        test("all conditions have ability check effects defined") {
            val conditions = listOf(
                Condition.Prone,
                Condition.Stunned,
                Condition.Poisoned,
                Condition.Blinded,
                Condition.Restrained,
                Condition.Incapacitated,
                Condition.Paralyzed,
                Condition.Unconscious
            )
            
            // Should not throw exceptions
            conditions.forEach { condition ->
                ConditionRegistry.getAbilityCheckEffect(condition)
            }
        }

        test("all conditions have action prevention defined") {
            val conditions = listOf(
                Condition.Prone,
                Condition.Stunned,
                Condition.Poisoned,
                Condition.Blinded,
                Condition.Restrained,
                Condition.Incapacitated,
                Condition.Paralyzed,
                Condition.Unconscious
            )
            
            // Should not throw exceptions
            conditions.forEach { condition ->
                ConditionRegistry.preventsActions(condition)
            }
        }
    }

    context("Action prevention summary") {
        test("conditions that prevent actions") {
            val preventingConditions = listOf(
                Condition.Stunned,
                Condition.Incapacitated,
                Condition.Paralyzed,
                Condition.Unconscious
            )
            
            preventingConditions.forEach { condition ->
                ConditionRegistry.preventsActions(condition).shouldBeTrue()
            }
        }

        test("conditions that do not prevent actions") {
            val nonPreventingConditions = listOf(
                Condition.Prone,
                Condition.Poisoned,
                Condition.Blinded,
                Condition.Restrained
            )
            
            nonPreventingConditions.forEach { condition ->
                ConditionRegistry.preventsActions(condition).shouldBeFalse()
            }
        }
    }
})
