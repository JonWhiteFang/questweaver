package dev.questweaver.core.rules.validation.actions

import dev.questweaver.core.rules.validation.state.GridPos

/**
 * Sealed interface representing all possible actions a creature can take during combat.
 *
 * Each action type specifies:
 * - The actor performing the action
 * - The type of action economy resource consumed
 * - Action-specific parameters (targets, positions, etc.)
 */
sealed interface GameAction {
    /**
     * The unique identifier of the creature performing this action.
     */
    val actorId: Long

    /**
     * The type of action economy resource consumed by this action.
     */
    val actionType: ActionType

    /**
     * Attack action - make a weapon or unarmed attack against a target.
     *
     * @property actorId The creature making the attack
     * @property targetId The creature being attacked
     * @property weaponId The weapon being used (null for unarmed strike)
     */
    data class Attack(
        override val actorId: Long,
        val targetId: Long,
        val weaponId: Long?
    ) : GameAction {
        override val actionType = ActionType.Action
    }

    /**
     * Cast Spell action - cast a spell with specified targets and slot level.
     *
     * @property actorId The creature casting the spell
     * @property spellId The unique identifier of the spell being cast
     * @property targetIds The list of creature IDs being targeted (empty for area effects or self)
     * @property targetPos The grid position being targeted (for area effects)
     * @property slotLevel The spell slot level being used (null for cantrips)
     */
    data class CastSpell(
        override val actorId: Long,
        val spellId: String,
        val targetIds: List<Long> = emptyList(),
        val targetPos: GridPos? = null,
        val slotLevel: Int? = null
    ) : GameAction {
        // Action type determined by spell (most spells use Action, some use Bonus Action or Reaction)
        // For now, default to Action - will be determined by spell data in actual implementation
        override val actionType: ActionType
            get() = ActionType.Action // TODO: Determine from spell data
    }

    /**
     * Move action - move along a path on the tactical grid.
     *
     * @property actorId The creature moving
     * @property path The sequence of grid positions to move through
     */
    data class Move(
        override val actorId: Long,
        val path: List<GridPos>
    ) : GameAction {
        override val actionType = ActionType.Movement
    }

    /**
     * Dash action - double movement speed for this turn.
     *
     * @property actorId The creature taking the Dash action
     */
    data class Dash(
        override val actorId: Long
    ) : GameAction {
        override val actionType = ActionType.Action
    }

    /**
     * Disengage action - movement doesn't provoke opportunity attacks this turn.
     *
     * @property actorId The creature taking the Disengage action
     */
    data class Disengage(
        override val actorId: Long
    ) : GameAction {
        override val actionType = ActionType.Action
    }

    /**
     * Dodge action - attacks against you have disadvantage, you have advantage on Dex saves.
     *
     * @property actorId The creature taking the Dodge action
     */
    data class Dodge(
        override val actorId: Long
    ) : GameAction {
        override val actionType = ActionType.Action
    }

    /**
     * Use Class Feature action - activate a limited-use class feature.
     *
     * @property actorId The creature using the feature
     * @property featureId The unique identifier of the class feature
     * @property targetId The target of the feature (null if self-targeted or no target)
     */
    data class UseClassFeature(
        override val actorId: Long,
        val featureId: String,
        val targetId: Long? = null
    ) : GameAction {
        // Action type determined by feature (varies by class feature)
        override val actionType: ActionType
            get() = ActionType.Action // TODO: Determine from feature data
    }

    /**
     * Opportunity Attack - reaction attack when a creature leaves your reach.
     *
     * @property actorId The creature making the opportunity attack
     * @property targetId The creature that provoked the opportunity attack
     */
    data class OpportunityAttack(
        override val actorId: Long,
        val targetId: Long
    ) : GameAction {
        override val actionType = ActionType.Reaction
    }
}
