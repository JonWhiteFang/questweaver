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
@Suppress("TooManyFunctions") // Acceptable: Each function generates a specific action type
class ActionCandidateGenerator {
    // RulesEngine will be used in future for validation
    // private val rulesEngine: RulesEngine
    /**
     * Generates action candidates based on preferred action types.
     * Limits search space for performance by returning top candidates only.
     * Filters by available resources.
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
        
        // Filter by available resources
        val filteredCandidates = filterByResources(candidates, creature, context)
        
        // Limit to top 20 candidates for performance
        return filteredCandidates.take(MAX_CANDIDATES)
    }
    
    /**
     * Generates attack action candidates.
     * Includes melee and ranged weapon attacks.
     */
    private fun generateAttackActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        val enemies = context.getEnemies(creature)
        if (enemies.isEmpty()) return emptyList()
        
        val candidates = mutableListOf<ActionCandidate>()
        
        // Generate melee attack
        candidates.add(
            ActionCandidate(
                action = TacticalAction.Attack("Melee Weapon"),
                targets = enemies,
                positions = emptyList(),
                resourceCost = ResourceCost()
            )
        )
        
        // Generate ranged attack
        candidates.add(
            ActionCandidate(
                action = TacticalAction.Attack("Ranged Weapon"),
                targets = enemies,
                positions = emptyList(),
                resourceCost = ResourceCost()
            )
        )
        
        return candidates
    }
    
    /**
     * Generates spell action candidates.
     * Filters by available spell slots and includes cantrips, damage spells, control spells, and buffs.
     */
    @Suppress("LongMethod") // Acceptable: Generates multiple spell types, each is simple
    private fun generateSpellActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        val enemies = context.getEnemies(creature)
        val allies = context.getAllies(creature)
        val candidates = mutableListOf<ActionCandidate>()
        
        // Cantrips (always available)
        if (enemies.isNotEmpty()) {
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Fire Bolt", 0),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost()
                )
            )
            
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Ray of Frost", 0),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost()
                )
            )
        }
        
        // 1st level spells
        if (enemies.isNotEmpty()) {
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Magic Missile", SPELL_LEVEL_1),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_1)
                )
            )
            
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Burning Hands", SPELL_LEVEL_1),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_1)
                )
            )
        }
        
        // 2nd level spells
        if (enemies.isNotEmpty()) {
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Scorching Ray", SPELL_LEVEL_2),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_2)
                )
            )
            
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Hold Person", SPELL_LEVEL_2),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_2)
                )
            )
        }
        
        // 3rd level spells
        if (enemies.isNotEmpty()) {
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Fireball", SPELL_LEVEL_3),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_3)
                )
            )
            
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Lightning Bolt", SPELL_LEVEL_3),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_3)
                )
            )
        }
        
        // Buff spells (if allies present)
        if (allies.isNotEmpty()) {
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Bless", SPELL_LEVEL_1),
                    targets = allies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_1)
                )
            )
            
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.CastSpell("Haste", SPELL_LEVEL_3),
                    targets = allies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_3)
                )
            )
        }
        
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
     * Includes common class abilities like Second Wind, Action Surge, Rage, etc.
     */
    private fun generateAbilityActions(
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        val candidates = mutableListOf<ActionCandidate>()
        val enemies = context.getEnemies(creature)
        
        // Fighter abilities
        if (creature.hitPointsCurrent < creature.hitPointsMax / 2) {
            // Second Wind (healing)
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.UseAbility(ABILITY_SECOND_WIND),
                    targets = emptyList(),
                    positions = emptyList(),
                    resourceCost = ResourceCost(abilityUse = ABILITY_SECOND_WIND)
                )
            )
        }
        
        if (enemies.size >= 2) {
            // Action Surge (extra action)
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.UseAbility("Action Surge"),
                    targets = emptyList(),
                    positions = emptyList(),
                    resourceCost = ResourceCost(abilityUse = "Action Surge")
                )
            )
        }
        
        // Barbarian abilities
        if (enemies.isNotEmpty()) {
            // Rage (damage resistance and bonus damage)
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.UseAbility("Rage"),
                    targets = emptyList(),
                    positions = emptyList(),
                    resourceCost = ResourceCost(abilityUse = "Rage")
                )
            )
        }
        
        // Rogue abilities
        // Cunning Action is handled by Dash/Disengage as bonus actions
        
        // Paladin abilities
        if (enemies.isNotEmpty()) {
            // Divine Smite (extra damage on hit)
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.UseAbility("Divine Smite"),
                    targets = enemies,
                    positions = emptyList(),
                    resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_1) // Uses spell slot
                )
            )
        }
        
        val allies = context.getAllies(creature)
        if (allies.any { it.hitPointsCurrent < it.hitPointsMax / 2 }) {
            // Lay on Hands (healing)
            candidates.add(
                ActionCandidate(
                    action = TacticalAction.UseAbility(ABILITY_LAY_ON_HANDS),
                    targets = allies.filter { it.hitPointsCurrent < it.hitPointsMax / 2 },
                    positions = emptyList(),
                    resourceCost = ResourceCost(abilityUse = ABILITY_LAY_ON_HANDS)
                )
            )
        }
        
        return candidates
    }
    
    /**
     * Filters action candidates by available resources.
     * Removes candidates that require resources the creature doesn't have.
     * Also performs early pruning of obviously invalid actions.
     *
     * @param candidates The candidates to filter
     * @param creature The creature performing the actions
     * @param context Current tactical situation
     * @return Filtered list of candidates
     */
    @Suppress("CognitiveComplexMethod") // Acceptable: Multiple simple checks, not actually complex
    private fun filterByResources(
        candidates: List<ActionCandidate>,
        creature: Creature,
        context: TacticalContext
    ): List<ActionCandidate> {
        return candidates.filter { candidate ->
            val cost = candidate.resourceCost
            
            // Early pruning: Skip actions with no valid targets
            if (candidate.targets.isEmpty() && candidate.action.actionType in REQUIRES_TARGET) {
                return@filter false
            }
            
            // Early pruning: Skip healing when all targets at full HP
            if (isHealingAction(candidate.action)) {
                val hasWoundedTarget = candidate.targets.any { 
                    it.hitPointsCurrent < it.hitPointsMax 
                }
                if (!hasWoundedTarget) {
                    return@filter false
                }
            }
            
            // Check spell slot availability
            if (cost.spellSlot != null) {
                if (!context.hasSpellSlot(creature.id, cost.spellSlot)) {
                    return@filter false
                }
            }
            
            // Check limited ability availability
            if (cost.abilityUse != null) {
                if (!context.hasAbility(creature.id, cost.abilityUse)) {
                    return@filter false
                }
            }
            
            // Check consumable item availability
            if (cost.consumableItem != null) {
                if (!context.hasItem(creature.id, cost.consumableItem)) {
                    return@filter false
                }
            }
            
            // Candidate has all required resources
            true
        }
    }
    
    /**
     * Checks if an action is a healing action.
     */
    private fun isHealingAction(action: TacticalAction): Boolean {
        return when (action) {
            is TacticalAction.CastSpell -> {
                action.spellName in HEALING_SPELLS
            }
            is TacticalAction.UseAbility -> {
                action.abilityName in HEALING_ABILITIES
            }
            else -> false
        }
    }
    
    companion object {
        private const val MAX_CANDIDATES = 20
        
        // Spell levels
        private const val SPELL_LEVEL_1 = 1
        private const val SPELL_LEVEL_2 = 2
        private const val SPELL_LEVEL_3 = 3
        
        // Ability names
        private const val ABILITY_SECOND_WIND = "Second Wind"
        private const val ABILITY_LAY_ON_HANDS = "Lay on Hands"
        
        // Action types that require a target
        private val REQUIRES_TARGET = setOf(
            ActionType.ATTACK,
            ActionType.CAST_SPELL,
            ActionType.HELP
        )
        
        // Healing spells
        private val HEALING_SPELLS = setOf(
            "Cure Wounds",
            "Healing Word",
            "Mass Cure Wounds",
            "Mass Healing Word",
            "Heal"
        )
        
        // Healing abilities
        private val HEALING_ABILITIES = setOf(
            "Second Wind",
            "Lay on Hands"
        )
    }
}
