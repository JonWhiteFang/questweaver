package dev.questweaver.domain.events

import dev.questweaver.domain.values.DiceRoll
import kotlinx.serialization.Serializable

/**
 * Represents the outcome of a spell effect on a single target.
 */
@Serializable
data class SpellOutcome(
    val targetId: Long,
    val attackRoll: DiceRoll?,
    val saveRoll: DiceRoll?,
    val success: Boolean,
    val damage: Int?,
    val damageType: String?
)

/**
 * Event emitted when a spell is cast.
 * Captures spell details, slot consumption, and outcomes for all targets.
 */
@Serializable
data class SpellCast(
    override val sessionId: Long,
    override val timestamp: Long,
    val casterId: Long,
    val spellId: Long,
    val spellLevel: Int,
    val slotConsumed: Int,
    val targets: List<Long>,
    val outcomes: List<SpellOutcome>
) : GameEvent
