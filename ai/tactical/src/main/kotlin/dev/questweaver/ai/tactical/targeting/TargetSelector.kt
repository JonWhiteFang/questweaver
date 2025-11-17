package dev.questweaver.ai.tactical.targeting

import dev.questweaver.ai.tactical.TacticalAction
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.domain.entities.Creature
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

/**
 * Selects the best target for an action based on threat, vulnerability, and tactical value.
 * 
 * Selection criteria:
 * - Threat Assessment (40%): Damage output, HP, conditions, healing capability
 * - Vulnerability (30%): HP percentage, AC, resistances, defensive conditions
 * - Tactical Value (30%): Role, concentration, position, range
 */
class TargetSelector(
    private val threatAssessor: ThreatAssessor,
    private val roller: DiceRoller
) {
    /**
     * Selects the optimal target for an action.
     * 
     * @param action The action being performed
     * @param candidates Potential targets
     * @param context Current tactical situation
     * @return Selected target
     */
    suspend fun selectTarget(
        action: TacticalAction,
        candidates: List<Creature>,
        context: TacticalContext
    ): Creature {
        require(candidates.isNotEmpty()) { "Cannot select target from empty list" }
        
        if (candidates.size == 1) {
            return candidates.first()
        }
        
        logger.debug { "Selecting target from ${candidates.size} candidates for action: $action" }
        
        // Score all candidates
        val scoredTargets = candidates.map { target ->
            val threatScore = calculateThreatScore(target, context)
            val vulnerabilityScore = calculateVulnerabilityScore(target, action, context)
            val tacticalScore = calculateTacticalValueScore(target, action, context)
            
            val totalScore = (threatScore * THREAT_WEIGHT) + 
                           (vulnerabilityScore * VULNERABILITY_WEIGHT) + 
                           (tacticalScore * TACTICAL_VALUE_WEIGHT)
            
            ScoredTarget(target, totalScore, threatScore, vulnerabilityScore, tacticalScore)
        }.sortedByDescending { it.totalScore }
        
        // Log top 3 for debugging
        scoredTargets.take(TOP_TARGETS_TO_LOG).forEachIndexed { index, scored ->
            logger.debug { 
                "Target #${index + 1}: ${scored.target.name} " +
                "(total=${scored.totalScore}, threat=${scored.threatScore}, " +
                "vuln=${scored.vulnerabilityScore}, tactical=${scored.tacticalScore})"
            }
        }
        
        // Check for ties
        val topScore = scoredTargets.first().totalScore
        val tiedTargets = scoredTargets.filter { abs(it.totalScore - topScore) < TIE_THRESHOLD }
        
        return if (tiedTargets.size > 1) {
            // Break ties with range preference and seeded randomness
            breakTie(tiedTargets, action, context)
        } else {
            scoredTargets.first().target
        }
    }
    
    /**
     * Task 8.2: Calculate threat-based score (40% weight).
     * Uses ThreatAssessor to evaluate danger level.
     */
    private fun calculateThreatScore(target: Creature, context: TacticalContext): Float {
        return threatAssessor.assessThreat(target, context)
    }
    
    /**
     * Task 8.3: Calculate vulnerability-based score (30% weight).
     * Considers HP percentage, AC, resistances, and defensive conditions.
     */
    @Suppress("MagicNumber", "CyclomaticComplexMethod", "LongMethod") 
    // HP thresholds are D&D conventions, complexity acceptable for scoring, length acceptable for comprehensive scoring
    private fun calculateVulnerabilityScore(
        target: Creature,
        action: TacticalAction,
        context: TacticalContext
    ): Float {
        var score = 0f
        
        // HP percentage (lower HP = more vulnerable)
        val hpPercent = target.hitPointsCurrent.toFloat() / target.hitPointsMax
        score += when {
            hpPercent <= 0.25f -> HP_CRITICAL_BONUS
            hpPercent <= 0.50f -> HP_LOW_BONUS
            hpPercent <= 0.75f -> HP_MEDIUM_BONUS
            else -> HP_HIGH_BONUS
        }
        
        // AC relative to attack bonus (lower AC = more vulnerable)
        val attackBonus = estimateAttackBonus(action)
        val acDifference = target.armorClass - attackBonus
        score += when {
            acDifference <= 5 -> AC_VERY_LOW_BONUS // Easy to hit
            acDifference <= 10 -> AC_LOW_BONUS
            acDifference <= 15 -> AC_MEDIUM_BONUS
            else -> AC_HIGH_BONUS // Hard to hit
        }
        
        // Resistances (simplified - would check actual damage type)
        // In full implementation, would check target.resistances against action damage type
        
        // Active defensive conditions (reduce vulnerability)
        val conditions = context.activeConditions[target.id] ?: emptyList()
        val hasDefensiveConditions = conditions.any { condition ->
            condition.name in listOf("Dodge", "Shield", "Blur", "Mirror Image")
        }
        if (hasDefensiveConditions) {
            score += DEFENSIVE_CONDITION_PENALTY
        }
        
        // Prone targets are vulnerable to melee, resistant to ranged
        val isProne = conditions.any { it.name == "Prone" }
        if (isProne) {
            score += when (action) {
                is TacticalAction.Attack -> {
                    if (action.weaponName.contains("Melee", ignoreCase = true)) {
                        PRONE_MELEE_BONUS
                    } else {
                        PRONE_RANGED_PENALTY
                    }
                }
                else -> 0f
            }
        }
        
        // Incapacitated targets are extremely vulnerable
        val isIncapacitated = conditions.any { 
            it.name in listOf("Incapacitated", "Paralyzed", "Stunned", "Unconscious")
        }
        if (isIncapacitated) {
            score += INCAPACITATED_BONUS
        }
        
        return score
    }
    
    /**
     * Task 8.4: Calculate tactical value score (30% weight).
     * Considers role, concentration, position, and range.
     */
    @Suppress("MagicNumber", "UnusedParameter") 
    // Ability score thresholds are D&D conventions, action parameter reserved for future use
    private fun calculateTacticalValueScore(
        target: Creature,
        action: TacticalAction,
        context: TacticalContext
    ): Float {
        var score = 0f
        
        // Role-based priority
        score += getRolePriority(target, context)
        
        // Concentration spell active (high priority to break)
        if (context.concentrationSpells.containsKey(target.id)) {
            score += CONCENTRATION_BONUS
        }
        
        // Position (isolated targets are higher priority)
        val targetPos = context.getPosition(target.id)
        if (targetPos != null) {
            val nearbyAllies = context.getAllies(target).count { ally ->
                val allyPos = context.getPosition(ally.id)
                allyPos != null && calculateDistance(targetPos, allyPos) <= NEARBY_DISTANCE
            }
            
            score += when (nearbyAllies) {
                0 -> ISOLATED_BONUS // Isolated, easy to focus fire
                1 -> LIGHTLY_SUPPORTED_BONUS
                else -> HEAVILY_SUPPORTED_PENALTY // Well-supported, harder to kill
            }
        }
        
        // Range (prefer closer targets if tied)
        // This is handled in tie-breaking, but we can add a small bonus here
        val actorPos = context.creaturePositions.values.firstOrNull() // Simplified
        if (actorPos != null && targetPos != null) {
            val distance = calculateDistance(actorPos, targetPos)
            score += when {
                distance <= CLOSE_RANGE -> CLOSE_RANGE_BONUS
                distance <= MEDIUM_RANGE -> MEDIUM_RANGE_BONUS
                else -> LONG_RANGE_PENALTY
            }
        }
        
        return score
    }
    
    /**
     * Gets role-based priority score.
     */
    @Suppress("MagicNumber", "UnusedParameter") 
    // Ability score thresholds are D&D conventions, context parameter reserved for future use
    private fun getRolePriority(target: Creature, context: TacticalContext): Float {
        // Determine role based on creature characteristics
        val hasHighWisdom = target.abilities.wisModifier >= HIGH_ABILITY_THRESHOLD
        val hasHighIntelligence = target.abilities.intModifier >= HIGH_ABILITY_THRESHOLD
        val hasHighCharisma = target.abilities.chaModifier >= HIGH_ABILITY_THRESHOLD
        val hasSpellSlots = context.availableSpellSlots[target.id]?.values?.any { it > 0 } == true
        
        return when {
            hasHighWisdom && hasSpellSlots -> HEALER_PRIORITY // Likely a healer
            (hasHighIntelligence || hasHighCharisma) && hasSpellSlots -> SPELLCASTER_PRIORITY
            target.abilities.strModifier >= HIGH_ABILITY_THRESHOLD || 
                target.abilities.dexModifier >= HIGH_ABILITY_THRESHOLD -> STRIKER_PRIORITY
            target.hitPointsMax > HIGH_HP_THRESHOLD -> TANK_PRIORITY
            else -> NORMAL_PRIORITY
        }
    }
    
    /**
     * Estimates attack bonus for hit chance calculations.
     */
    private fun estimateAttackBonus(action: TacticalAction): Int {
        return when (action) {
            is TacticalAction.Attack -> TYPICAL_ATTACK_BONUS
            is TacticalAction.CastSpell -> TYPICAL_SPELL_ATTACK_BONUS
            else -> TYPICAL_ATTACK_BONUS
        }
    }
    
    /**
     * Task 8.5: Break ties between equally-scored targets.
     * Prefers targets within optimal range, then uses seeded randomness.
     */
    private fun breakTie(
        tiedTargets: List<ScoredTarget>,
        action: TacticalAction,
        context: TacticalContext
    ): Creature {
        logger.debug { "Breaking tie between ${tiedTargets.size} targets" }
        
        // Prefer targets within optimal range
        val actorPos = context.creaturePositions.values.firstOrNull()
        if (actorPos != null) {
            val targetsWithRange = tiedTargets.map { scored ->
                val targetPos = context.getPosition(scored.target.id)
                val distance = if (targetPos != null) {
                    calculateDistance(actorPos, targetPos)
                } else {
                    Int.MAX_VALUE
                }
                scored to distance
            }
            
            // Find optimal range based on action type
            val optimalRange = when (action) {
                is TacticalAction.Attack -> {
                    if (action.weaponName.contains("Melee", ignoreCase = true)) {
                        MELEE_OPTIMAL_RANGE // Adjacent for melee
                    } else {
                        RANGED_OPTIMAL_RANGE // Medium range for ranged
                    }
                }
                is TacticalAction.CastSpell -> SPELL_OPTIMAL_RANGE // Long range for spells
                else -> DEFAULT_OPTIMAL_RANGE
            }
            
            // Find targets closest to optimal range
            val closestToOptimal = targetsWithRange.minByOrNull { (_, distance) ->
                abs(distance - optimalRange)
            }
            
            if (closestToOptimal != null) {
                val (scored, distance) = closestToOptimal
                logger.debug { "Selected ${scored.target.name} at distance $distance (optimal: $optimalRange)" }
                return scored.target
            }
        }
        
        // Use seeded randomness for final tie-break
        // Roll d100 and use modulo to select from tied targets
        val roll = roller.d100().result
        val randomIndex = roll % tiedTargets.size
        val selected = tiedTargets[randomIndex].target
        logger.debug { "Random tie-break selected ${selected.name}" }
        return selected
    }
    
    /**
     * Calculates grid distance between two positions.
     */
    private fun calculateDistance(
        pos1: dev.questweaver.domain.values.GridPos,
        pos2: dev.questweaver.domain.values.GridPos
    ): Int {
        return kotlin.math.max(abs(pos1.x - pos2.x), abs(pos1.y - pos2.y))
    }
    
    /**
     * Internal data class for scored targets.
     */
    private data class ScoredTarget(
        val target: Creature,
        val totalScore: Float,
        val threatScore: Float,
        val vulnerabilityScore: Float,
        val tacticalScore: Float
    )
    
    companion object {
        // Selection weights
        private const val THREAT_WEIGHT = 0.40f
        private const val VULNERABILITY_WEIGHT = 0.30f
        private const val TACTICAL_VALUE_WEIGHT = 0.30f
        
        // Tie threshold
        private const val TIE_THRESHOLD = 0.1f
        
        // HP vulnerability bonuses
        private const val HP_CRITICAL_BONUS = 50f
        private const val HP_LOW_BONUS = 30f
        private const val HP_MEDIUM_BONUS = 15f
        private const val HP_HIGH_BONUS = 0f
        
        // AC vulnerability bonuses
        private const val AC_VERY_LOW_BONUS = 20f
        private const val AC_LOW_BONUS = 10f
        private const val AC_MEDIUM_BONUS = 0f
        private const val AC_HIGH_BONUS = -10f
        
        // Condition bonuses/penalties
        private const val DEFENSIVE_CONDITION_PENALTY = -15f
        private const val PRONE_MELEE_BONUS = 15f
        private const val PRONE_RANGED_PENALTY = -15f
        private const val INCAPACITATED_BONUS = 40f
        
        // Role priorities
        private const val HEALER_PRIORITY = 50f
        private const val SPELLCASTER_PRIORITY = 40f
        private const val STRIKER_PRIORITY = 25f
        private const val TANK_PRIORITY = 10f
        private const val NORMAL_PRIORITY = 15f
        
        // Tactical bonuses
        private const val CONCENTRATION_BONUS = 30f
        private const val ISOLATED_BONUS = 20f
        private const val LIGHTLY_SUPPORTED_BONUS = 5f
        private const val HEAVILY_SUPPORTED_PENALTY = -10f
        
        // Range bonuses
        private const val CLOSE_RANGE_BONUS = 5f
        private const val MEDIUM_RANGE_BONUS = 2f
        private const val LONG_RANGE_PENALTY = -5f
        
        // Distance thresholds
        private const val NEARBY_DISTANCE = 2
        private const val CLOSE_RANGE = 3
        private const val MEDIUM_RANGE = 10
        
        // Attack bonus estimates
        private const val TYPICAL_ATTACK_BONUS = 5
        private const val TYPICAL_SPELL_ATTACK_BONUS = 5
        
        // Role detection thresholds
        private const val HIGH_ABILITY_THRESHOLD = 3
        private const val HIGH_HP_THRESHOLD = 50
        
        // Logging constants
        private const val TOP_TARGETS_TO_LOG = 3
        
        // Optimal range constants for tie-breaking
        private const val MELEE_OPTIMAL_RANGE = 1
        private const val RANGED_OPTIMAL_RANGE = 5
        private const val SPELL_OPTIMAL_RANGE = 10
        private const val DEFAULT_OPTIMAL_RANGE = 5
    }
}
