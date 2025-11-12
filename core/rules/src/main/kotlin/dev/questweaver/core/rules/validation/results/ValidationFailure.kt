package dev.questweaver.core.rules.validation.results

import dev.questweaver.rules.conditions.Condition
import dev.questweaver.core.rules.validation.state.GridPos
import dev.questweaver.core.rules.validation.state.Resource

/**
 * Sealed interface representing specific reasons why action validation failed.
 *
 * Each failure type includes relevant context to help the UI display meaningful
 * error messages to the user.
 */
sealed interface ValidationFailure {
    /**
     * The action cannot be performed because the required action economy resource
     * has already been used this turn.
     *
     * @property required The action economy resource that was required
     * @property alreadyUsed Whether this resource has already been used
     */
    data class ActionEconomyExhausted(
        val required: ActionEconomyResource,
        val alreadyUsed: Boolean
    ) : ValidationFailure

    /**
     * The action cannot be performed because the actor lacks required resources.
     *
     * @property required The resource that was required
     * @property available The amount of this resource currently available
     * @property needed The amount of this resource needed
     */
    data class InsufficientResources(
        val required: Resource,
        val available: Int,
        val needed: Int
    ) : ValidationFailure

    /**
     * The action cannot be performed because the target is out of range.
     *
     * @property actualDistance The actual distance to the target in feet
     * @property maxRange The maximum range of the action in feet
     * @property rangeType The type of range requirement
     */
    data class OutOfRange(
        val actualDistance: Int,
        val maxRange: Int,
        val rangeType: Range
    ) : ValidationFailure

    /**
     * The action cannot be performed because line-of-effect is blocked.
     *
     * @property blockingObstacle The position of the obstacle blocking line-of-effect
     * @property obstacleType A description of what is blocking the path
     */
    data class LineOfEffectBlocked(
        val blockingObstacle: GridPos,
        val obstacleType: String
    ) : ValidationFailure

    /**
     * The action cannot be performed because it would conflict with existing concentration.
     *
     * @property activeSpell The spell currently being concentrated on
     * @property newSpell The new concentration spell being attempted
     */
    data class ConcentrationConflict(
        val activeSpell: String, // Spell ID or name
        val newSpell: String      // Spell ID or name
    ) : ValidationFailure

    /**
     * The action cannot be performed because a condition prevents it.
     *
     * @property condition The condition that prevents the action
     * @property reason A human-readable explanation of why the condition prevents the action
     */
    data class ConditionPreventsAction(
        val condition: Condition,
        val reason: String
    ) : ValidationFailure

    /**
     * The action cannot be performed because the target is invalid.
     *
     * @property reason A human-readable explanation of why the target is invalid
     */
    data class InvalidTarget(
        val reason: String
    ) : ValidationFailure
}

/**
 * Enum representing action economy resources in D&D 5e.
 */
enum class ActionEconomyResource {
    Action,
    BonusAction,
    Reaction,
    Movement,
    FreeAction
}

/**
 * Sealed interface representing range requirements for actions.
 */
sealed interface Range {
    /**
     * Touch range (5 feet or less).
     */
    object Touch : Range

    /**
     * A specific distance in feet.
     *
     * @property distance The maximum distance in feet
     */
    data class Feet(val distance: Int) : Range

    /**
     * Line of sight (no specific distance limit).
     */
    object Sight : Range

    /**
     * Self-targeted only.
     */
    object Self : Range

    /**
     * A radius around the caster.
     *
     * @property feet The radius in feet
     */
    data class Radius(val feet: Int) : Range
}
