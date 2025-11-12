package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.Range
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.state.EncounterState
import dev.questweaver.core.rules.validation.state.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for RangeValidator.
 *
 * Tests verify that:
 * - Touch range requires 5 feet or less
 * - Ranged actions fail beyond max range
 * - Line-of-effect is blocked by obstacles
 * - Self-targeted actions are always valid
 * - Edge case: exactly at max range
 */
class RangeValidatorTest : FunSpec({
    val validator = RangeValidator()
    val actorId = 1L
    val targetId = 2L

    fun createEncounterState(
        actorPos: GridPos,
        targetPos: GridPos? = null,
        obstacles: Set<GridPos> = emptySet()
    ): EncounterState {
        val positions = mutableMapOf(actorId to actorPos)
        if (targetPos != null) {
            positions[targetId] = targetPos
        }
        return EncounterState(
            positions = positions,
            obstacles = obstacles
        )
    }

    context("Touch range") {
        test("requires 5 feet or less") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(2, 0) // 2 squares = 10 feet
            val encounterState = createEncounterState(actorPos, targetPos)
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null // Unarmed strike (touch range)
            )

            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.OutOfRange>()
            val outOfRange = failure as ValidationFailure.OutOfRange
            outOfRange.actualDistance shouldBe 10
            outOfRange.maxRange shouldBe 5
        }

        test("succeeds at 5 feet") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(1, 0) // 1 square = 5 feet
            val encounterState = createEncounterState(actorPos, targetPos)
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null
            )

            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("succeeds at same position") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(0, 0) // Same square
            val encounterState = createEncounterState(actorPos, targetPos)
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null
            )

            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Ranged actions") {
        test("fail beyond max range") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(15, 0) // 15 squares = 75 feet
            val encounterState = createEncounterState(actorPos, targetPos)
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "fire_bolt", // Default spell range is 60 feet
                targetIds = listOf(targetId)
            )

            // Default spell range is 60 feet, so 75 feet should fail
            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.OutOfRange>()
        }

        test("succeed within max range") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(5, 0) // 5 squares = 25 feet
            val encounterState = createEncounterState(actorPos, targetPos)
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "fire_bolt",
                targetIds = listOf(targetId)
            )

            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("succeed exactly at max range") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(12, 0) // 12 squares = 60 feet (default spell range)
            val encounterState = createEncounterState(actorPos, targetPos)
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile",
                targetIds = listOf(targetId)
            )

            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Line-of-effect") {
        test("blocked by obstacles") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(4, 0)
            val obstacles = setOf(GridPos(2, 0)) // Obstacle in the middle
            val encounterState = createEncounterState(actorPos, targetPos, obstacles)
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "fire_bolt",
                targetIds = listOf(targetId)
            )

            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.LineOfEffectBlocked>()
            val blocked = failure as ValidationFailure.LineOfEffectBlocked
            blocked.blockingObstacle shouldBe GridPos(2, 0)
        }

        test("succeeds with clear path") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(4, 0)
            val obstacles = setOf(GridPos(2, 1)) // Obstacle to the side
            val encounterState = createEncounterState(actorPos, targetPos, obstacles)
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "fire_bolt",
                targetIds = listOf(targetId)
            )

            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("diagonal path blocked by obstacle") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(3, 3)
            val obstacles = setOf(GridPos(1, 1)) // Obstacle on diagonal
            val encounterState = createEncounterState(actorPos, targetPos, obstacles)
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "fire_bolt",
                targetIds = listOf(targetId)
            )

            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
        }

        test("touch range doesn't check line-of-effect") {
            val actorPos = GridPos(0, 0)
            val targetPos = GridPos(1, 0)
            val obstacles = setOf(GridPos(1, 0)) // Obstacle at target position
            val encounterState = createEncounterState(actorPos, targetPos, obstacles)
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null // Touch range
            )

            // Touch range doesn't check line-of-effect, only distance
            val result = validator.validateRange(action, actorPos, targetPos, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Self-targeted actions") {
        test("always valid") {
            val actorPos = GridPos(0, 0)
            val encounterState = createEncounterState(actorPos)
            val action = GameAction.Dash(actorId = actorId)

            val result = validator.validateRange(action, actorPos, null, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("Dodge action is self-targeted") {
            val actorPos = GridPos(0, 0)
            val encounterState = createEncounterState(actorPos)
            val action = GameAction.Dodge(actorId = actorId)

            val result = validator.validateRange(action, actorPos, null, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }

        test("Disengage action is self-targeted") {
            val actorPos = GridPos(0, 0)
            val encounterState = createEncounterState(actorPos)
            val action = GameAction.Disengage(actorId = actorId)

            val result = validator.validateRange(action, actorPos, null, encounterState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Distance calculation") {
        test("calculates orthogonal distance correctly") {
            val from = GridPos(0, 0)
            val to = GridPos(5, 0)

            val distance = validator.calculateDistance(from, to)

            distance shouldBe 25 // 5 squares * 5 feet
        }

        test("calculates diagonal distance correctly (Chebyshev)") {
            val from = GridPos(0, 0)
            val to = GridPos(3, 3)

            val distance = validator.calculateDistance(from, to)

            // Chebyshev distance: max(dx, dy) = max(3, 3) = 3 squares = 15 feet
            distance shouldBe 15
        }

        test("calculates mixed diagonal distance correctly") {
            val from = GridPos(0, 0)
            val to = GridPos(4, 2)

            val distance = validator.calculateDistance(from, to)

            // Chebyshev distance: max(4, 2) = 4 squares = 20 feet
            distance shouldBe 20
        }

        test("distance to same position is zero") {
            val pos = GridPos(5, 5)

            val distance = validator.calculateDistance(pos, pos)

            distance shouldBe 0
        }
    }

    context("Line-of-effect algorithm") {
        test("hasLineOfEffect returns true for clear path") {
            val from = GridPos(0, 0)
            val to = GridPos(5, 0)
            val obstacles = emptySet<GridPos>()

            val hasLOE = validator.hasLineOfEffect(from, to, obstacles)

            hasLOE shouldBe true
        }

        test("hasLineOfEffect returns false when blocked") {
            val from = GridPos(0, 0)
            val to = GridPos(5, 0)
            val obstacles = setOf(GridPos(3, 0))

            val hasLOE = validator.hasLineOfEffect(from, to, obstacles)

            hasLOE shouldBe false
        }

        test("hasLineOfEffect ignores obstacles at start position") {
            val from = GridPos(0, 0)
            val to = GridPos(5, 0)
            val obstacles = setOf(GridPos(0, 0))

            val hasLOE = validator.hasLineOfEffect(from, to, obstacles)

            hasLOE shouldBe true
        }

        test("hasLineOfEffect ignores obstacles at end position") {
            val from = GridPos(0, 0)
            val to = GridPos(5, 0)
            val obstacles = setOf(GridPos(5, 0))

            val hasLOE = validator.hasLineOfEffect(from, to, obstacles)

            hasLOE shouldBe true
        }

        test("hasLineOfEffect works with diagonal paths") {
            val from = GridPos(0, 0)
            val to = GridPos(4, 4)
            val obstacles = setOf(GridPos(2, 2))

            val hasLOE = validator.hasLineOfEffect(from, to, obstacles)

            hasLOE shouldBe false
        }

        test("hasLineOfEffect returns true with empty obstacles") {
            val from = GridPos(0, 0)
            val to = GridPos(10, 10)
            val obstacles = emptySet<GridPos>()

            val hasLOE = validator.hasLineOfEffect(from, to, obstacles)

            hasLOE shouldBe true
        }
    }
})
