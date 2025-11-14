package dev.questweaver.domain.events

import kotlinx.serialization.Serializable

/**
 * Event emitted when a bonus action is taken.
 */
@Serializable
data class BonusActionTaken(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val actionType: String
) : GameEvent

/**
 * Event emitted when a creature takes the Dodge action.
 */
@Serializable
data class DodgeAction(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long
) : GameEvent

/**
 * Event emitted when a creature takes the Disengage action.
 */
@Serializable
data class DisengageAction(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long
) : GameEvent

/**
 * Enum representing the type of help being provided.
 */
enum class HelpType {
    Attack,
    AbilityCheck
}

/**
 * Event emitted when a creature takes the Help action.
 */
@Serializable
data class HelpAction(
    override val sessionId: Long,
    override val timestamp: Long,
    val helperId: Long,
    val targetId: Long,
    val helpType: HelpType
) : GameEvent

/**
 * Event emitted when a creature readies an action.
 * The prepared action will be executed when the trigger condition is met.
 */
@Serializable
data class ReadyAction(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val preparedActionDescription: String,
    val trigger: String
) : GameEvent

/**
 * Enum representing different types of reactions.
 */
enum class ReactionType {
    OpportunityAttack,
    ReadiedAction,
    Shield,
    Counterspell,
    Other
}

/**
 * Event emitted when a creature uses a reaction.
 */
@Serializable
data class ReactionUsed(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val reactionType: ReactionType,
    val triggerId: Long?
) : GameEvent
