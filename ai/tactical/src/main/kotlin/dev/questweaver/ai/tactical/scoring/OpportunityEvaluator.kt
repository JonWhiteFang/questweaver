package dev.questweaver.ai.tactical.scoring

import dev.questweaver.ai.tactical.ActionCandidate
import dev.questweaver.ai.tactical.TacticalAction
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.ai.tactical.TacticalOpportunity
import dev.questweaver.domain.values.Condition
import dev.questweaver.domain.values.GridPos
import kotlin.math.abs

/**
 * Identifies tactical opportunities on the battlefield.
 * Opportunities provide bonus scores to actions that exploit them.
 */
class OpportunityEvaluator {
    /**
     * Evaluates tactical opportunities for an action.
     *
     * @param candidate The action candidate being considered
     * @param context Current tactical situation
     * @return List of identified opportunities
     */
    fun evaluateOpportunities(
        candidate: ActionCandidate,
        context: TacticalContext
    ): List<TacticalOpportunity> {
        val opportunities = mutableListOf<TacticalOpportunity>()
        
        // Check for flanking opportunities
        opportunities.addAll(evaluateFlanking(candidate, context))
        
        // Check for condition-based opportunities
        opportunities.addAll(evaluateConditionOpportunities(candidate, context))
        
        // Check for AoE opportunities
        opportunities.addAll(evaluateAoEOpportunities(candidate, context))
        
        // Check for forced movement opportunities
        opportunities.addAll(evaluateForcedMovement(candidate, context))
        
        return opportunities
    }
    
    /**
     * Evaluates flanking opportunities.
     * Flanking provides advantage on melee attacks.
     */
    @Suppress("ReturnCount") // Acceptable: Early returns for clarity
    private fun evaluateFlanking(
        candidate: ActionCandidate,
        context: TacticalContext
    ): List<TacticalOpportunity> {
        // Only melee attacks benefit from flanking
        if (candidate.action !is TacticalAction.Attack) {
            return emptyList()
        }
        
        if (candidate.action.weaponName != "Melee Weapon") {
            return emptyList()
        }
        
        val opportunities = mutableListOf<TacticalOpportunity>()
        
        // Check each target for flanking potential
        for (target in candidate.targets) {
            val targetPos = context.getPosition(target.id) ?: continue
            
            // Check if any ally is on opposite side of target
            val allies = context.getAllies(target)
            @Suppress("LoopWithTooManyJumpStatements") // Acceptable: Simple break for efficiency
            for (ally in allies) {
                val allyPos = context.getPosition(ally.id) ?: continue
                
                if (isFlanking(targetPos, allyPos)) {
                    opportunities.add(TacticalOpportunity.Flanking())
                    break // Only count flanking once per target
                }
            }
        }
        
        return opportunities
    }
    
    /**
     * Checks if two positions are flanking a target.
     * Simplified: positions are flanking if they're on opposite sides.
     */
    private fun isFlanking(targetPos: GridPos, allyPos: GridPos): Boolean {
        // Simplified flanking check: allies are on opposite sides if
        // they're more than 2 squares apart in both x and y
        val dx = abs(targetPos.x - allyPos.x)
        val dy = abs(targetPos.y - allyPos.y)
        return dx >= 2 && dy >= 2
    }
    
    /**
     * Evaluates condition-based opportunities.
     * Includes prone, incapacitated, and concentration targets.
     */
    private fun evaluateConditionOpportunities(
        candidate: ActionCandidate,
        context: TacticalContext
    ): List<TacticalOpportunity> {
        val opportunities = mutableListOf<TacticalOpportunity>()
        
        for (target in candidate.targets) {
            val conditions = context.activeConditions[target.id] ?: emptyList()
            
            // Check for prone targets (advantage on melee attacks)
            if (Condition.PRONE in conditions && candidate.action is TacticalAction.Attack) {
                if ((candidate.action as TacticalAction.Attack).weaponName == "Melee Weapon") {
                    opportunities.add(TacticalOpportunity.ProneTarget())
                }
            }
            
            // Check for incapacitated targets (auto-crit)
            if (Condition.INCAPACITATED in conditions || 
                Condition.PARALYZED in conditions ||
                Condition.UNCONSCIOUS in conditions) {
                opportunities.add(TacticalOpportunity.IncapacitatedTarget())
            }
            
            // Check for concentration targets
            if (context.concentrationSpells.containsKey(target.id)) {
                opportunities.add(TacticalOpportunity.ConcentrationBreak())
            }
        }
        
        return opportunities
    }
    
    /**
     * Evaluates AoE spell opportunities.
     * Multi-target spells get bonus per additional target.
     */
    @Suppress("ReturnCount") // Acceptable: Early returns for clarity
    private fun evaluateAoEOpportunities(
        candidate: ActionCandidate,
        context: TacticalContext
    ): List<TacticalOpportunity> {
        // Only spells can be AoE
        if (candidate.action !is TacticalAction.CastSpell) {
            return emptyList()
        }
        
        val spell = candidate.action as TacticalAction.CastSpell
        
        // Check if spell is AoE
        if (!isAoESpell(spell.spellName)) {
            return emptyList()
        }
        
        // Count potential targets in AoE radius
        val targetCount = countTargetsInAoE(candidate, context)
        
        if (targetCount >= 2) {
            return listOf(TacticalOpportunity.MultiTargetAoE.create(targetCount))
        }
        
        return emptyList()
    }
    
    /**
     * Checks if a spell is an area of effect spell.
     */
    @Suppress("UnusedParameter") // Context will be used for hazard detection
    private fun isAoESpell(spellName: String): Boolean {
        return spellName in AOE_SPELLS
    }
    
    /**
     * Counts targets in AoE radius.
     * Simplified: assumes all enemies in candidate.targets are in range.
     */
    @Suppress("UnusedParameter") // Context will be used for geometry calculations
    private fun countTargetsInAoE(
        candidate: ActionCandidate,
        context: TacticalContext
    ): Int {
        // Simplified: count all targets
        // TODO: Implement proper AoE radius checking with geometry
        return candidate.targets.size
    }
    
    /**
     * Evaluates forced movement opportunities.
     * Pushing enemies into hazards provides bonus.
     */
    @Suppress("UnusedParameter") // Context will be used for hazard detection
    private fun evaluateForcedMovement(
        candidate: ActionCandidate,
        context: TacticalContext
    ): List<TacticalOpportunity> {
        // Only certain spells and abilities can force movement
        val canForceMovement = when (candidate.action) {
            is TacticalAction.CastSpell -> {
                (candidate.action as TacticalAction.CastSpell).spellName in FORCED_MOVEMENT_SPELLS
            }
            is TacticalAction.UseAbility -> {
                (candidate.action as TacticalAction.UseAbility).abilityName in FORCED_MOVEMENT_ABILITIES
            }
            else -> false
        }
        
        if (!canForceMovement) {
            return emptyList()
        }
        
        // Check if any targets are near hazards
        // TODO: Implement hazard detection
        val hazardNearby = false // Placeholder
        
        return listOf(TacticalOpportunity.ForcedMovement.create(hazardNearby))
    }
    
    companion object {
        // AoE spells
        private val AOE_SPELLS = setOf(
            "Fireball",
            "Lightning Bolt",
            "Burning Hands",
            "Cone of Cold",
            "Thunderwave",
            "Shatter",
            "Hypnotic Pattern",
            "Fear",
            "Cloudkill"
        )
        
        // Spells that force movement
        private val FORCED_MOVEMENT_SPELLS = setOf(
            "Thunderwave",
            "Gust of Wind",
            "Telekinesis"
        )
        
        // Abilities that force movement
        private val FORCED_MOVEMENT_ABILITIES = setOf(
            "Shove",
            "Push"
        )
    }
}
