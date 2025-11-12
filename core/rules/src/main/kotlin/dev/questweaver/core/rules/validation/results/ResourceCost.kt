package dev.questweaver.core.rules.validation.results

import dev.questweaver.core.rules.validation.state.Resource

/**
 * Data class representing the resources that would be consumed by executing an action.
 *
 * This includes action economy resources (action, bonus action, reaction, movement),
 * consumable resources (spell slots, class features, item charges), and whether
 * the action would break concentration.
 */
data class ResourceCost(
    /**
     * The action economy resources consumed by this action.
     */
    val actionEconomy: Set<ActionEconomyResource>,

    /**
     * The consumable resources consumed by this action.
     */
    val resources: Set<Resource>,

    /**
     * The amount of movement consumed in feet.
     */
    val movementCost: Int,

    /**
     * Whether this action would break existing concentration.
     */
    val breaksConcentration: Boolean
) {
    companion object {
        /**
         * A resource cost representing no resources consumed.
         */
        val None = ResourceCost(
            actionEconomy = emptySet(),
            resources = emptySet(),
            movementCost = 0,
            breaksConcentration = false
        )
    }
}
