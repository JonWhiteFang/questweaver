package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.ActionType
import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.conditions.ConditionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for ConditionValidator.
 *
 * Tests verify that:
 * - Stunned prevents all actions
 * - Incapacitated prevents actions and reactions
 * - Paralyzed prevents actions, reactions, movement
 * - Unconscious prevents all actions
 * - Prone doesn't prevent actions
 */
class ConditionValidatorTest : FunSpec({
    val validator = ConditionValidator(ConditionRegistry)
    val actorId = 1L

    context("Stunned condition") {
        test("prevents actions") {
            val conditions = setOf(Condition.Stunned)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.ConditionPreventsAction>()
            val prevented = failure as ValidationFailure.ConditionPreventsAction
            prevented.condition shouldBe Condition.Stunned
        }

        test("prevents bonus actions") {
            val conditions = setOf(Condition.Stunned)
            // Use CastSpell as a proxy for bonus action testing
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "healing_word",
                targetIds = listOf(actorId)
            )

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }

        test("prevents reactions") {
            val conditions = setOf(Condition.Stunned)
            val action = GameAction.OpportunityAttack(actorId = actorId, targetId = 2L)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }

        test("prevents movement") {
            val conditions = setOf(Condition.Stunned)
            val action = GameAction.Move(actorId = actorId, path = emptyList())

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }
    }

    context("Incapacitated condition") {
        test("prevents actions") {
            val conditions = setOf(Condition.Incapacitated)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.ConditionPreventsAction>()
            val prevented = failure as ValidationFailure.ConditionPreventsAction
            prevented.condition shouldBe Condition.Incapacitated
        }

        test("prevents reactions") {
            val conditions = setOf(Condition.Incapacitated)
            val action = GameAction.OpportunityAttack(actorId = actorId, targetId = 2L)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }

        test("doesn't prevent movement") {
            val conditions = setOf(Condition.Incapacitated)
            val action = GameAction.Move(actorId = actorId, path = emptyList())

            // Incapacitated doesn't prevent movement, only actions/reactions
            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Paralyzed condition") {
        test("prevents actions") {
            val conditions = setOf(Condition.Paralyzed)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.ConditionPreventsAction>()
            val prevented = failure as ValidationFailure.ConditionPreventsAction
            prevented.condition shouldBe Condition.Paralyzed
        }

        test("prevents reactions") {
            val conditions = setOf(Condition.Paralyzed)
            val action = GameAction.OpportunityAttack(actorId = actorId, targetId = 2L)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }

        test("prevents movement") {
            val conditions = setOf(Condition.Paralyzed)
            val action = GameAction.Move(actorId = actorId, path = emptyList())

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }
    }

    context("Unconscious condition") {
        test("prevents actions") {
            val conditions = setOf(Condition.Unconscious)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.ConditionPreventsAction>()
            val prevented = failure as ValidationFailure.ConditionPreventsAction
            prevented.condition shouldBe Condition.Unconscious
        }

        test("prevents reactions") {
            val conditions = setOf(Condition.Unconscious)
            val action = GameAction.OpportunityAttack(actorId = actorId, targetId = 2L)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }

        test("prevents movement") {
            val conditions = setOf(Condition.Unconscious)
            val action = GameAction.Move(actorId = actorId, path = emptyList())

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }
    }

    context("Prone condition") {
        test("doesn't prevent actions") {
            val conditions = setOf(Condition.Prone)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("doesn't prevent reactions") {
            val conditions = setOf(Condition.Prone)
            val action = GameAction.OpportunityAttack(actorId = actorId, targetId = 2L)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("doesn't prevent movement (can crawl)") {
            val conditions = setOf(Condition.Prone)
            val action = GameAction.Move(actorId = actorId, path = emptyList())

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Restrained condition") {
        test("doesn't prevent actions") {
            val conditions = setOf(Condition.Restrained)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("doesn't prevent reactions") {
            val conditions = setOf(Condition.Restrained)
            val action = GameAction.OpportunityAttack(actorId = actorId, targetId = 2L)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("prevents movement (speed becomes 0)") {
            val conditions = setOf(Condition.Restrained)
            val action = GameAction.Move(actorId = actorId, path = emptyList())

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }
    }

    context("Poisoned condition") {
        test("doesn't prevent actions") {
            val conditions = setOf(Condition.Poisoned)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("doesn't prevent reactions") {
            val conditions = setOf(Condition.Poisoned)
            val action = GameAction.OpportunityAttack(actorId = actorId, targetId = 2L)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Blinded condition") {
        test("doesn't prevent actions") {
            val conditions = setOf(Condition.Blinded)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("doesn't prevent reactions") {
            val conditions = setOf(Condition.Blinded)
            val action = GameAction.OpportunityAttack(actorId = actorId, targetId = 2L)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Free actions") {
        test("free action concept verified") {
            val conditions = setOf(Condition.Stunned, Condition.Paralyzed, Condition.Unconscious)
            
            // Free actions are a design concept - they would be implemented as specific
            // GameAction types that don't get blocked by conditions
            // For now, verify that the blocking conditions are correctly identified
            val blockingCondition = validator.getBlockingCondition(conditions)
            blockingCondition shouldBe Condition.Stunned
        }
    }

    context("Multiple conditions") {
        test("first blocking condition is returned") {
            val conditions = setOf(Condition.Stunned, Condition.Paralyzed)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.ConditionPreventsAction>()
            // Either Stunned or Paralyzed could be returned (both block actions)
            val prevented = failure as ValidationFailure.ConditionPreventsAction
            (prevented.condition == Condition.Stunned || prevented.condition == Condition.Paralyzed) shouldBe true
        }

        test("non-blocking conditions don't prevent actions") {
            val conditions = setOf(Condition.Prone, Condition.Poisoned, Condition.Blinded)
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("No conditions") {
        test("allows all actions") {
            val conditions = emptySet<Condition>()
            val action = GameAction.Attack(actorId = actorId, targetId = 2L, weaponId = null)

            val result = validator.validateConditions(action, conditions)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("ConditionRegistry helper methods") {
        test("getBlockingCondition returns first blocking condition") {
            val conditions = setOf(Condition.Prone, Condition.Stunned, Condition.Poisoned)

            val blocking = validator.getBlockingCondition(conditions)

            blocking shouldBe Condition.Stunned
        }

        test("getBlockingCondition returns null when no blocking conditions") {
            val conditions = setOf(Condition.Prone, Condition.Poisoned)

            val blocking = validator.getBlockingCondition(conditions)

            blocking shouldBe null
        }
    }
})
