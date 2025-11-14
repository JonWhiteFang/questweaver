package dev.questweaver.core.rules.actions.models

import dev.questweaver.domain.values.GridPos
import kotlinx.serialization.Serializable

/**
 * Sealed interface representing all possible combat actions.
 * Each action type captures the intent and parameters needed for execution.
 */
@Serializable
sealed interface CombatAction {
    val actorId: Long
}

/**
 * Enum representing the type of help being provided.
 */
enum class HelpType {
    Attack,
    AbilityCheck
}

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
 * Attack action - make a weapon or spell attack against a target.
 */
@Serializable
data class Attack(
    override val actorId: Long,
    val targetId: Long,
    val weaponId: Long?,
    val attackBonus: Int,
    val damageDice: String,
    val damageModifier: Int,
    val damageType: DamageType
) : CombatAction

/**
 * Enum representing damage types in D&D 5e.
 */
enum class DamageType {
    Slashing,
    Piercing,
    Bludgeoning,
    Fire,
    Cold,
    Lightning,
    Thunder,
    Acid,
    Poison,
    Necrotic,
    Radiant,
    Force,
    Psychic
}

/**
 * Move action - change position on the tactical map.
 */
@Serializable
data class Move(
    override val actorId: Long,
    val path: List<GridPos>,
    val isDash: Boolean = false
) : CombatAction

/**
 * Sealed interface representing spell effects.
 */
@Serializable
sealed interface SpellEffect {
    @Serializable
    data class Attack(val targets: List<Long>) : SpellEffect
    
    @Serializable
    data class Save(
        val dc: Int,
        val abilityType: AbilityType,
        val targets: List<Long>
    ) : SpellEffect
    
    @Serializable
    data class Utility(val effect: String) : SpellEffect
}

/**
 * Enum representing ability types for saving throws.
 */
enum class AbilityType {
    Strength,
    Dexterity,
    Constitution,
    Intelligence,
    Wisdom,
    Charisma
}

/**
 * Cast spell action - cast a spell with various effects.
 */
@Serializable
data class CastSpell(
    override val actorId: Long,
    val spellId: Long,
    val spellLevel: Int,
    val targets: List<Long>,
    val spellEffect: SpellEffect,
    val isBonusAction: Boolean
) : CombatAction

/**
 * Dodge action - gain defensive benefits until next turn.
 */
@Serializable
data class Dodge(
    override val actorId: Long
) : CombatAction

/**
 * Disengage action - move without provoking opportunity attacks.
 */
@Serializable
data class Disengage(
    override val actorId: Long
) : CombatAction

/**
 * Help action - grant advantage to an ally.
 */
@Serializable
data class Help(
    override val actorId: Long,
    val targetId: Long,
    val helpType: HelpType
) : CombatAction

/**
 * Ready action - prepare an action to trigger on a condition.
 */
@Serializable
data class Ready(
    override val actorId: Long,
    val preparedAction: CombatAction,
    val trigger: String
) : CombatAction

/**
 * Reaction action - respond to a trigger outside the creature's turn.
 */
@Serializable
data class Reaction(
    override val actorId: Long,
    val reactionType: ReactionType,
    val targetId: Long?
) : CombatAction
