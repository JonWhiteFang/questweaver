package dev.questweaver.core.rules.validation.results

/**
 * Sealed interface representing choices that must be made before an action can be validated.
 *
 * When validation returns RequiresChoice, the user must select from the available options
 * before the action can be fully validated and executed.
 */
sealed interface ActionChoice {
    /**
     * The user must choose which spell slot level to use for casting a spell.
     *
     * @property minLevel The minimum spell slot level required
     * @property availableLevels The spell slot levels currently available
     */
    data class SpellSlotLevel(
        val minLevel: Int,
        val availableLevels: List<Int>
    ) : ActionChoice

    /**
     * The user must select which targets to affect with the action.
     *
     * @property validTargets The list of valid target IDs
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
     * @property featureId The unique identifier of the class feature
     * @property options The list of available options for this feature
     */
    data class FeatureOption(
        val featureId: String,
        val options: List<String>
    ) : ActionChoice
}
