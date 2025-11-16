package dev.questweaver.ai.tactical

import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.values.GridPos

/**
 * Represents a potential action with its targets and positions.
 *
 * @property action The tactical action to perform
 * @property targets Potential target creatures
 * @property positions Potential positions to move to
 * @property resourceCost Resources required for this action
 */
data class ActionCandidate(
    val action: TacticalAction,
    val targets: List<Creature>,
    val positions: List<GridPos>,
    val resourceCost: ResourceCost
)

/**
 * Represents the resource cost of an action.
 *
 * @property spellSlot Spell slot level consumed (null if not a spell)
 * @property abilityUse Name of limited ability used (null if not an ability)
 * @property consumableItem Name of consumable item used (null if not an item)
 */
data class ResourceCost(
    val spellSlot: Int? = null,
    val abilityUse: String? = null,
    val consumableItem: String? = null
) {
    /**
     * Returns true if this action has no resource cost.
     */
    val isFree: Boolean
        get() = spellSlot == null && abilityUse == null && consumableItem == null
}
