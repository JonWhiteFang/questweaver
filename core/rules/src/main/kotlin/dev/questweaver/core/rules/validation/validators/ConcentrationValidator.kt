package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ResourceCost
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.state.ConcentrationInfo
import dev.questweaver.core.rules.validation.state.ConcentrationState

/**
 * Validates concentration requirements for spells.
 *
 * In D&D 5e, a creature can only concentrate on one spell at a time.
 * Casting a new concentration spell breaks the existing concentration.
 */
class ConcentrationValidator {
    /**
     * Validates whether a concentration spell can be cast.
     *
     * @param action The action being validated (must be CastSpell)
     * @param actorId The creature casting the spell
     * @param concentrationState Current concentration tracking
     * @return ValidationResult indicating success or warning about breaking concentration
     */
    fun validateConcentration(
        action: GameAction,
        actorId: Long,
        concentrationState: ConcentrationState
    ): ValidationResult {
        val breaksConcentration = when {
            action !is GameAction.CastSpell -> false
            !spellRequiresConcentration(action.spellId) -> false
            else -> getActiveConcentration(actorId, concentrationState) != null
        }
        
        return ValidationResult.Success(
            ResourceCost(
                actionEconomy = emptySet(),
                resources = emptySet(),
                movementCost = 0,
                breaksConcentration = breaksConcentration
            )
        )
    }
    
    /**
     * Checks if the actor is currently concentrating.
     *
     * @param actorId The creature ID
     * @param concentrationState Current concentration tracking
     * @return Active concentration spell ID, if any
     */
    fun getActiveConcentration(
        actorId: Long,
        concentrationState: ConcentrationState
    ): ConcentrationInfo? {
        return concentrationState.getConcentration(actorId)
    }
    
    /**
     * Determines if a spell requires concentration.
     *
     * TODO: This should look up spell data from a spell registry.
     * For now, uses a simple heuristic based on spell name.
     */
    private fun spellRequiresConcentration(spellId: String): Boolean {
        // Placeholder implementation
        // In a real implementation, this would look up the spell in a registry
        
        // Common concentration spells
        val concentrationSpells = setOf(
            "bless",
            "bane",
            "hunter's mark",
            "hex",
            "haste",
            "slow",
            "polymorph",
            "banishment",
            "hold person",
            "hold monster",
            "invisibility",
            "greater invisibility",
            "fly",
            "levitate",
            "fog cloud",
            "darkness",
            "moonbeam",
            "flaming sphere",
            "spiritual weapon", // Actually doesn't require concentration
            "spirit guardians"
        )
        
        // Check if spell name contains concentration keywords
        val spellName = spellId.lowercase()
        return concentrationSpells.any { spellName.contains(it) } ||
               spellName.contains("concentration")
    }
}
