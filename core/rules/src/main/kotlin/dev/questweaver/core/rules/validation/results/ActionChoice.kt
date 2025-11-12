package dev.questweaver.core.rules.validation.results

/**
 * Sealed interface representing choices that must be made before an action can be validated.
 *
 * When validation returns RequiresChoice, the user must select from the provided options
 * before the action can be fully validated and executed.
 */
sealed interface ActionChoice {
    /**
     * The user must choose which spell slot level to use for casting a spell.
     *
     * This occurs when a spell can be upcast using higher-level spell slots.
     *
     * @property minLevel The minimum spell slot level required
     * @property availableLevels The list of available spell slot levels that can be used
     */
    data class SpellSlotLevel(
        val minLevel: Int,
        val availableLevels: List<Int>
    ) : ActionChoice

    /**
     * The user must select which targets to affect with the action.
     *
     * This occurs for area-of-effect spells or abilities that can target multiple creatures.
     *
     * @property validTargets The list of valid target IDs that can be selected
     * @property minTargets The minimum number of targets that must be selected
     * @property maxTargets The maximum number of targets that can be selected
     */
    data class TargetSelection(
        val validTargets: List<Long>,
        val minTargets: Int,
        val maxTargets: Int
    ) : ActionChoice

    /**
     * The user must choose which option to use for a class feature.
     *
     * This occurs when a class feature has multiple modes or options.
     *
     * @property featureId The unique identifier of the class feature
     * @property options The list of available option identifiers
     */
    data class FeatureOption(
        val featureId: String,
        val options: List<String>
    ) : ActionChoice
}
