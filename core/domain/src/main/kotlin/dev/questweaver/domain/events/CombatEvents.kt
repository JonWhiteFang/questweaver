package dev.questweaver.domain.events

import dev.questweaver.domain.values.Condition
import dev.questweaver.domain.values.DiceRoll
import kotlinx.serialization.Serializable

/**
 * Event emitted when an attack roll is resolved.
 * Captures both the intent (attack attempt) and outcome (hit/miss/critical).
 */
@Serializable
data class AttackResolved(
    override val sessionId: Long,
    override val timestamp: Long,
    val attackerId: Long,
    val targetId: Long,
    val attackRoll: DiceRoll,
    val targetAC: Int,
    val hit: Boolean,
    val critical: Boolean = false
) : GameEvent

/**
 * Event emitted when damage is applied to a creature.
 * Captures the damage roll, amount, and HP state before and after.
 */
@Serializable
data class DamageApplied(
    override val sessionId: Long,
    override val timestamp: Long,
    val targetId: Long,
    val damageRoll: DiceRoll,
    val damageAmount: Int,
    val hpBefore: Int,
    val hpAfter: Int
) : GameEvent

/**
 * Event emitted when a condition is applied to a creature.
 */
@Serializable
data class ConditionApplied(
    override val sessionId: Long,
    override val timestamp: Long,
    val targetId: Long,
    val condition: Condition,
    val duration: Int?
) : GameEvent

/**
 * Event emitted when a condition is removed from a creature.
 */
@Serializable
data class ConditionRemoved(
    override val sessionId: Long,
    override val timestamp: Long,
    val targetId: Long,
    val condition: Condition
) : GameEvent

/**
 * Event emitted when a creature is defeated (HP reaches 0).
 *
 * @property sessionId The session this event belongs to
 * @property timestamp Unix timestamp in milliseconds when the event occurred
 * @property creatureId The ID of the creature that was defeated
 * @property defeatedBy The ID of the creature that dealt the defeating blow,
 *                      or null if defeated by environmental damage
 */
@Serializable
data class CreatureDefeated(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val defeatedBy: Long?
) : GameEvent
