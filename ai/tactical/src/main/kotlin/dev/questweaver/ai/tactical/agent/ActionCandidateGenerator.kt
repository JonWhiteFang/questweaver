package dev.questweaver.ai.tactical.agent

import dev.questweaver.ai.tactical.ActionCandidate
import dev.questweaver.ai.tactical.ActionType
import dev.questweaver.ai.tactical.ResourceCost
import dev.questweaver.ai.tactical.TacticalAction
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.core.rules.RulesEngine
import dev.questweaver.domain.entities.Creature

/**
 * Generates valid action candidates for a creature based on available resources and tactical context.
 */
class ActionCandidateGenerator {
    // RulesEngine will be used in future for validation
    // private val rulesEngine: RulesEngine
    /**
     * Generates action candidates based on preferred action types.
     * Limits search space for performance by returning top candidates only.
     *
     * @param creature The creature generating actions
     * @param context Current tactical situation
     * @param actionTypes Preferred action types from behavior tree
     * @return List of valid action candidates (max 20)
     */
    suspend fun generate(
        creature: Creature,
        context: TacticalContext,
        actionTypes: List<ActionType>
    ): List<ActionCandidate> {
        val candidates = mutableListOf<ActionCandidate>()
        
        for (actionType in actionTypes) {
            when (actionType) {
                ActionType.ATTACK -> candidates.addAll(generateAttackActions(creature, context))
                ActionType.CAST_SPELL -> candidates.addAll(generateSpellActions(creature, context))
                ActionType.MOVE -> candidates.addAll(generateMovementActions(creature, context))
                ActionType.DASH -> candidates.addAll(generateDashActions(creature, context))
                ActionType.DISENGAGE -> candidates.addAll(generateDisengageActions(creature, context))
                ActionType.DODGE -> candidates.addAll(generateDodgeActions(creature, context))
                ActionType.HELP -> candidates.addAll(generateHelpActions(creature, context))
                ActionType.USE_ABILITY -> candidates.addAll(generateAbilityActions(creature, context))
            }
        }
        
        // Limit to top 20 candidates for performance
        return candidates.take(MAX_CANDIDATES)
    }
    
    /**
     * Generates attack action candidates.
     */
    private fun generateAttackActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        val enemies = context.getEnemies(creature)
        if (enemies.isEmpty()) return emptyList()
        
        // For now, generate a single melee attack candidate
        // TODO: Support multiple weapons, ranged attacks
        return listOf(
            ActionCandidate(
                action = TacticalAction.Attack("Melee Weapon"),
                targets = enemies,
                positions = emptyList(),
                resourceCost = ResourceCost()
            )
        )
    }
    
    /**
     * Generates spell action candidates.
     * Filters by available spell slots.
     */
    private fun generateSpellActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        val enemies = context.getEnemies(creature)
        if (enemies.isEmpty()) return emptyList()
        
        val candidates = mutableListOf<ActionCandidate>()
        
        // Cantrip (always available)
        candidates.add(
            ActionCandidate(
                action = TacticalAction.CastSpell("Fire Bolt", 0),
                targets = enemies,
                positions = emptyList(),
                resourceCost = ResourceCost()
            )
        )
        
        // TODO: Check available spell slots and generate spell candidates
        // For now, add a simple 1st level spell
        candidates.add(
            ActionCandidate(
                action = TacticalAction.CastSpell("Magic Missile", 1),
                targets = enemies,
                positions = emptyList(),
                resourceCost = ResourceCost(spellSlot = 1)
            )
        )
        
        return candidates
    }
    
    /**
     * Generates movement action candidates.
     */
    private fun generateMovementActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        val enemies = context.getEnemies(creature)
        
        return listOf(
            ActionCandidate(
                action = TacticalAction.Move(creature.speed),
                targets = enemies,
                positions = emptyList(), // Will be filled by positioning strategy
                resourceCost = ResourceCost()
            )
        )
    }
    
    /**
     * Generates dash action candidates.
     */
    @Suppress("UnusedParameter")
    private fun generateDashActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        return listOf(
            ActionCandidate(
                action = TacticalAction.Dash,
                targets = context.getEnemies(creature),
                positions = emptyList(),
                resourceCost = ResourceCost()
            )
        )
    }
    
    /**
     * Generates disengage action candidates.
     */
    @Suppress("UnusedParameter")
    private fun generateDisengageActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        return listOf(
            ActionCandidate(
                action = TacticalAction.Disengage,
                targets = emptyList(),
                positions = emptyList(),
                resourceCost = ResourceCost()
            )
        )
    }
    
    /**
     * Generates dodge action candidates.
     */
    @Suppress("UnusedParameter")
    private fun generateDodgeActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        return listOf(
            ActionCandidate(
                action = TacticalAction.Dodge,
                targets = emptyList(),
                positions = emptyList(),
                resourceCost = ResourceCost()
            )
        )
    }
    
    /**
     * Generates help action candidates.
     */
    private fun generateHelpActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        val allies = context.getAllies(creature)
        if (allies.isEmpty()) return emptyList()
        
        return listOf(
            ActionCandidate(
                action = TacticalAction.Help,
                targets = allies,
                positions = emptyList(),
                resourceCost = ResourceCost()
            )
        )
    }
    
    /**
     * Generates ability action candidates.
     * TODO: Implement based on creature abilities.
     */
    @Suppress("UnusedParameter")
    private fun generateAbilityActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        // TODO: Check creature's available abilities
        return emptyList()
    }
    
    companion object {
        private const val MAX_CANDIDATES = 20
    }
}
