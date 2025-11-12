package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ActionChoice
import dev.questweaver.core.rules.validation.results.Resource
import dev.questweaver.core.rules.validation.results.ResourceCost
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.state.ResourcePool

/**
 * Validates resource availability for actions.
 *
 * This validator checks whether the actor has sufficient resources (spell slots,
 * class features, item charges) to perform the requested action.
 */
class ResourceValidator {
    /**
     * Validates whether the actor has required resources for the action.
     *
     * @param action The action requiring resources
     * @param resourcePool Current resource availability
     * @return ValidationResult indicating success, failure, or required choices
     */
    fun validateResources(
        action: GameAction,
        resourcePool: ResourcePool
    ): ValidationResult {
        val resourceCost = getResourceCost(action)
        
        // Check if all required resources are available
        for (resource in resourceCost.resources) {
            if (!resourcePool.hasResource(resource)) {
                return ValidationResult.Failure(
                    createInsufficientResourcesFailure(resource, resourcePool)
                )
            }
        }
        
        // For spell casting, check if we need to prompt for slot level choice
        return if (action is GameAction.CastSpell && action.slotLevel == null) {
            validateSpellSlotChoice(action.spellId, resourcePool, resourceCost)
        } else {
            ValidationResult.Success(resourceCost)
        }
    }
    
    /**
     * Validates spell slot selection and prompts for choice if needed.
     */
    private fun validateSpellSlotChoice(
        spellId: String,
        resourcePool: ResourcePool,
        resourceCost: ResourceCost
    ): ValidationResult {
        val spellLevel = getSpellLevel(spellId)
        if (spellLevel == 0) { // Cantrip
            return ValidationResult.Success(resourceCost)
        }
        
        val availableLevels = resourcePool.getAvailableSpellSlotLevels()
            .filter { it >= spellLevel }
        
        return when {
            availableLevels.isEmpty() -> ValidationResult.Failure(
                ValidationFailure.InsufficientResources(
                    required = Resource.SpellSlot(spellLevel),
                    available = 0,
                    needed = 1
                )
            )
            availableLevels.size > 1 -> ValidationResult.RequiresChoice(
                listOf(
                    ActionChoice.SpellSlotLevel(
                        minLevel = spellLevel,
                        availableLevels = availableLevels
                    )
                )
            )
            else -> ValidationResult.Success(resourceCost)
        }
    }
    
    /**
     * Determines which resources would be consumed by the action.
     *
     * @param action The action being validated
     * @return ResourceCost with specific resources consumed
     */
    fun getResourceCost(action: GameAction): ResourceCost {
        val resources = mutableSetOf<Resource>()
        
        when (action) {
            is GameAction.CastSpell -> {
                val spellLevel = getSpellLevel(action.spellId)
                if (spellLevel > 0) { // Not a cantrip
                    val slotLevel = action.slotLevel ?: spellLevel
                    resources.add(Resource.SpellSlot(slotLevel))
                }
            }
            is GameAction.UseClassFeature -> {
                // Class features typically consume 1 use
                // In a real implementation, this would look up feature data
                resources.add(Resource.ClassFeature(action.featureId, 1))
            }
            else -> {
                // Other actions don't consume resources
            }
        }
        
        return ResourceCost(
            actionEconomy = emptySet(), // Action economy handled by ActionEconomyValidator
            resources = resources,
            movementCost = 0, // Movement cost handled by ActionEconomyValidator
            breaksConcentration = false // Concentration handled by ConcentrationValidator
        )
    }
    
    /**
     * Creates an InsufficientResources failure with appropriate context.
     */
    private fun createInsufficientResourcesFailure(
        resource: Resource,
        resourcePool: ResourcePool
    ): ValidationFailure.InsufficientResources {
        return when (resource) {
            is Resource.SpellSlot -> {
                val available = resourcePool.spellSlots[resource.level] ?: 0
                ValidationFailure.InsufficientResources(
                    required = resource,
                    available = available,
                    needed = 1
                )
            }
            is Resource.ClassFeature -> {
                val available = resourcePool.classFeatures[resource.featureId] ?: 0
                ValidationFailure.InsufficientResources(
                    required = resource,
                    available = available,
                    needed = resource.uses
                )
            }
            is Resource.ItemCharge -> {
                val available = resourcePool.itemCharges[resource.itemId] ?: 0
                ValidationFailure.InsufficientResources(
                    required = resource,
                    available = available,
                    needed = resource.charges
                )
            }
            is Resource.HitDice -> {
                val available = resourcePool.hitDice[resource.diceType] ?: 0
                ValidationFailure.InsufficientResources(
                    required = resource,
                    available = available,
                    needed = resource.count
                )
            }
        }
    }
    
    /**
     * Gets the spell level for a given spell ID.
     * 
     * TODO: This should look up spell data from a spell registry.
     * For now, returns a placeholder value.
     */
    private fun getSpellLevel(spellId: String): Int {
        // Placeholder implementation
        // In a real implementation, this would look up the spell in a registry
        return when {
            spellId.contains("cantrip", ignoreCase = true) -> CANTRIP_LEVEL
            spellId.contains("1st", ignoreCase = true) -> FIRST_LEVEL
            spellId.contains("2nd", ignoreCase = true) -> SECOND_LEVEL
            spellId.contains("3rd", ignoreCase = true) -> THIRD_LEVEL
            else -> FIRST_LEVEL // Default to 1st level
        }
    }
    
    companion object {
        private const val CANTRIP_LEVEL = 0
        private const val FIRST_LEVEL = 1
        private const val SECOND_LEVEL = 2
        private const val THIRD_LEVEL = 3
    }
}
