package dev.questweaver.ai.tactical.scoring

import dev.questweaver.ai.tactical.ActionCandidate
import dev.questweaver.ai.tactical.Difficulty
import dev.questweaver.ai.tactical.ResourceCost
import dev.questweaver.ai.tactical.ScoreBreakdown
import dev.questweaver.ai.tactical.ScoredAction
import dev.questweaver.ai.tactical.TacticalAction
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.ai.tactical.targeting.ThreatAssessor
import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.values.Condition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Scores action candidates using utility-based evaluation.
 * 
 * Formula: (Damage × HitProb × TargetPriority) + TacticalValue + PositioningValue - ResourceCost + RandomVariance
 */
@Suppress("TooManyFunctions") // Acceptable: Each function calculates a specific score component
class ActionScorer(
    private val threatAssessor: ThreatAssessor,
    private val opportunityEvaluator: OpportunityEvaluator,
    private val roller: DiceRoller
) {
    suspend fun scoreAll(
        candidates: List<ActionCandidate>,
        context: TacticalContext,
        difficulty: Difficulty
    ): List<ScoredAction> {
        logger.debug { "Scoring ${candidates.size} candidates at $difficulty" }
        
        return candidates.map { candidate ->
            val breakdown = calculateBreakdown(candidate, context)
            val variance = applyVariance(breakdown.totalScore(), difficulty)
            
            ScoredAction(candidate, breakdown.totalScore() + variance, breakdown)
        }.sortedByDescending { it.score }
    }
    
    private suspend fun calculateBreakdown(
        candidate: ActionCandidate,
        context: TacticalContext
    ): ScoreBreakdown {
        val damage = calculateDamageScore(candidate, context)
        val hitProb = calculateHitProbability(candidate, context)
        val targetPriority = calculateTargetPriority(candidate, context)
        val resourceCost = calculateResourceCost(candidate)
        val tacticalValue = calculateTacticalValue(candidate, context)
        val positioning = calculatePositioningScore(candidate, context)
        
        return ScoreBreakdown(damage, hitProb, targetPriority, resourceCost, tacticalValue, positioning)
    }
    
    /**
     * Task 7.2: Calculate expected damage score.
     * Considers base damage, hit probability, critical hits, and resistances.
     */
    private fun calculateDamageScore(candidate: ActionCandidate, context: TacticalContext): Float {
        if (candidate.targets.isEmpty()) return 0f
        
        val target = candidate.targets.first()
        val baseDamage = estimateBaseDamage(candidate.action)
        
        // Apply critical hit multiplier if target is incapacitated (auto-crit on melee)
        val critMultiplier = if (hasAdvantageAgainst(target, context)) {
            CRIT_MULTIPLIER_WITH_ADVANTAGE
        } else {
            CRIT_MULTIPLIER_NORMAL
        }
        
        // Apply resistance/vulnerability/immunity
        val damageType = getDamageType(candidate.action)
        val resistanceMultiplier = getResistanceMultiplier(target, damageType, context)
        
        return baseDamage * critMultiplier * resistanceMultiplier
    }
    
    /**
     * Estimates base damage for an action.
     */
    @Suppress("MagicNumber") // Spell levels are D&D conventions
    private fun estimateBaseDamage(action: TacticalAction): Float {
        return when (action) {
            is TacticalAction.Attack -> {
                // Assume average weapon damage (1d8 + modifier = 4.5 + 3 = 7.5)
                AVERAGE_WEAPON_DAMAGE
            }
            is TacticalAction.CastSpell -> {
                when (action.spellLevel) {
                    0 -> CANTRIP_DAMAGE // Cantrip (e.g., Fire Bolt: 1d10 = 5.5)
                    1 -> SPELL_LEVEL_1_DAMAGE // 1st level (e.g., Magic Missile: 3d4+3 = 10.5)
                    2 -> SPELL_LEVEL_2_DAMAGE // 2nd level (e.g., Scorching Ray: 6d6 = 21)
                    3 -> SPELL_LEVEL_3_DAMAGE // 3rd level (e.g., Fireball: 8d6 = 28)
                    else -> action.spellLevel * DAMAGE_PER_SPELL_LEVEL
                }
            }
            is TacticalAction.UseAbility -> {
                // Assume moderate ability damage
                ABILITY_DAMAGE
            }
            else -> 0f
        }
    }
    
    /**
     * Gets damage type for resistance calculations.
     */
    private fun getDamageType(action: TacticalAction): String {
        return when (action) {
            is TacticalAction.Attack -> "physical"
            is TacticalAction.CastSpell -> {
                // Simplified: assume fire for most damage spells
                when (action.spellName) {
                    "Fire Bolt", "Fireball", "Burning Hands", "Scorching Ray" -> "fire"
                    "Ray of Frost", "Cone of Cold" -> "cold"
                    "Lightning Bolt" -> "lightning"
                    "Magic Missile" -> "force"
                    else -> "fire"
                }
            }
            else -> "physical"
        }
    }
    
    /**
     * Gets resistance multiplier for damage type.
     */
    @Suppress("UnusedParameter") // damageType will be used when creature resistance data is available
    private fun getResistanceMultiplier(target: Creature, damageType: String, context: TacticalContext): Float {
        // Simplified: check for common resistances
        // In full implementation, would check creature's resistance/vulnerability/immunity lists
        
        // Check if target is concentrating (more vulnerable)
        val isConcentrating = context.concentrationSpells.containsKey(target.id)
        
        return when {
            // Immunity (would check creature data)
            false -> IMMUNITY_MULTIPLIER
            // Resistance (would check creature data)
            false -> RESISTANCE_MULTIPLIER
            // Vulnerability (would check creature data or concentrating)
            isConcentrating -> VULNERABILITY_MULTIPLIER
            else -> NORMAL_MULTIPLIER
        }
    }
    
    /**
     * Task 7.3: Calculate hit probability.
     * Considers attack bonus vs AC, advantage/disadvantage, and saving throws.
     */
    private fun calculateHitProbability(candidate: ActionCandidate, context: TacticalContext): Float {
        if (candidate.targets.isEmpty()) return 1.0f
        
        val target = candidate.targets.first()
        
        return when (candidate.action) {
            is TacticalAction.Attack -> {
                // Attack roll: need to roll >= (target AC - attack bonus)
                val attackBonus = estimateAttackBonus(candidate.action)
                val targetAC = target.armorClass
                val baseHitChance = calculateAttackHitChance(attackBonus, targetAC)
                
                // Adjust for advantage/disadvantage
                if (hasAdvantageAgainst(target, context)) {
                    // Advantage: 1 - (1 - p)^2
                    1f - ((1f - baseHitChance) * (1f - baseHitChance))
                } else if (hasDisadvantageAgainst(target, context)) {
                    // Disadvantage: p^2
                    baseHitChance * baseHitChance
                } else {
                    baseHitChance
                }
            }
            is TacticalAction.CastSpell -> {
                if (isAttackSpell(candidate.action.spellName)) {
                    // Spell attack roll
                    val spellAttackBonus = estimateSpellAttackBonus()
                    val targetAC = target.armorClass
                    calculateAttackHitChance(spellAttackBonus, targetAC)
                } else if (isSaveSpell(candidate.action.spellName)) {
                    // Saving throw spell
                    val saveDC = estimateSaveDC()
                    val saveBonus = target.getSavingThrowModifier(getSaveAbility(candidate.action.spellName))
                    calculateSaveFailChance(saveDC, saveBonus)
                } else {
                    // Auto-hit (e.g., Magic Missile, buffs)
                    1.0f
                }
            }
            else -> 1.0f // Non-attack actions always "hit"
        }
    }
    
    /**
     * Calculates hit chance for attack roll.
     * D20 + attackBonus >= targetAC
     */
    @Suppress("MagicNumber") // D20 mechanics constants are self-documenting
    private fun calculateAttackHitChance(attackBonus: Int, targetAC: Int): Float {
        val neededRoll = targetAC - attackBonus
        return when {
            neededRoll <= 1 -> 0.95f // Natural 1 always misses
            neededRoll >= 20 -> 0.05f // Natural 20 always hits
            else -> (21 - neededRoll) / 20f
        }
    }
    
    /**
     * Calculates chance of target failing save.
     */
    @Suppress("MagicNumber") // D20 mechanics constants are self-documenting
    private fun calculateSaveFailChance(saveDC: Int, saveBonus: Int): Float {
        val neededRoll = saveDC - saveBonus
        return when {
            neededRoll <= 1 -> 0.05f // Natural 1 always fails
            neededRoll >= 20 -> 0.95f // Natural 20 always succeeds
            else -> (neededRoll - 1) / 20f
        }
    }
    
    /**
     * Estimates attack bonus for weapon attacks.
     */
    @Suppress("UnusedParameter") // action will be used when weapon-specific bonuses are implemented
    private fun estimateAttackBonus(action: TacticalAction.Attack): Int {
        // Simplified: assume +5 attack bonus (proficiency + ability modifier)
        return TYPICAL_ATTACK_BONUS
    }
    
    /**
     * Estimates spell attack bonus.
     */
    private fun estimateSpellAttackBonus(): Int {
        // Simplified: assume +5 spell attack bonus
        return TYPICAL_SPELL_ATTACK_BONUS
    }
    
    /**
     * Estimates spell save DC.
     */
    private fun estimateSaveDC(): Int {
        // Simplified: assume DC 13 (8 + proficiency + ability)
        return TYPICAL_SAVE_DC
    }
    
    /**
     * Checks if spell requires attack roll.
     */
    private fun isAttackSpell(spellName: String): Boolean {
        return spellName in listOf("Fire Bolt", "Ray of Frost", "Scorching Ray")
    }
    
    /**
     * Checks if spell requires saving throw.
     */
    private fun isSaveSpell(spellName: String): Boolean {
        return spellName in listOf("Fireball", "Lightning Bolt", "Burning Hands", "Hold Person")
    }
    
    /**
     * Gets saving throw ability for spell.
     */
    private fun getSaveAbility(spellName: String): dev.questweaver.domain.values.AbilityType {
        return when (spellName) {
            "Hold Person" -> dev.questweaver.domain.values.AbilityType.Wisdom
            else -> dev.questweaver.domain.values.AbilityType.Dexterity
        }
    }
    
    /**
     * Checks if attacker has advantage against target.
     */
    private fun hasAdvantageAgainst(target: Creature, context: TacticalContext): Boolean {
        val conditions = context.activeConditions[target.id] ?: emptyList()
        return conditions.any { it in listOf(Condition.PRONE, Condition.INCAPACITATED, Condition.PARALYZED) }
    }
    
    /**
     * Checks if attacker has disadvantage against target.
     */
    @Suppress("UnusedParameter", "FunctionOnlyReturningConstant") // Will be implemented with obscurement/prone checks
    private fun hasDisadvantageAgainst(target: Creature, context: TacticalContext): Boolean {
        // Simplified: would check for obscurement, prone (for ranged), etc.
        return false
    }
    
    /**
     * Task 7.4: Calculate target priority score.
     * Uses ThreatAssessor and applies role/HP multipliers.
     */
    @Suppress("MagicNumber") // HP thresholds are D&D conventions
    private fun calculateTargetPriority(candidate: ActionCandidate, context: TacticalContext): Float {
        if (candidate.targets.isEmpty()) return 1.0f
        
        val target = candidate.targets.first()
        val baseThreat = threatAssessor.assessThreat(target, context)
        
        // Apply role multipliers
        val roleMultiplier = getRoleMultiplier(target, context)
        
        // Apply HP percentage multiplier (prioritize low HP targets)
        val hpPercent = target.hitPointsCurrent.toFloat() / target.hitPointsMax
        val hpMultiplier = when {
            hpPercent <= 0.25f -> HP_CRITICAL_MULTIPLIER
            hpPercent <= 0.50f -> HP_LOW_MULTIPLIER
            else -> HP_NORMAL_MULTIPLIER
        }
        
        // Apply concentration multiplier
        val concentrationMultiplier = if (context.concentrationSpells.containsKey(target.id)) {
            CONCENTRATION_MULTIPLIER
        } else {
            1.0f
        }
        
        return baseThreat * roleMultiplier * hpMultiplier * concentrationMultiplier
    }
    
    /**
     * Gets role-based priority multiplier.
     */
    @Suppress("UnusedParameter", "MagicNumber") // context will be used when creature class/role data is available
    private fun getRoleMultiplier(target: Creature, context: TacticalContext): Float {
        // Simplified: would check creature class/role
        // For now, use heuristics based on spellcasting ability
        
        val hasHighWisdom = target.abilities.wisModifier >= 3
        val hasHighIntelligence = target.abilities.intModifier >= 3
        val hasHighCharisma = target.abilities.chaModifier >= 3
        
        return when {
            hasHighWisdom -> HEALER_MULTIPLIER // Likely a healer
            hasHighIntelligence || hasHighCharisma -> SPELLCASTER_MULTIPLIER // Likely a spellcaster
            else -> NORMAL_MULTIPLIER
        }
    }
    
    /**
     * Task 7.5: Calculate resource cost penalty.
     */
    private fun calculateResourceCost(candidate: ActionCandidate): Float {
        var penalty = 0f
        candidate.resourceCost.spellSlot?.let { penalty += it * SPELL_SLOT_PENALTY_PER_LEVEL }
        candidate.resourceCost.abilityUse?.let { penalty += ABILITY_USE_PENALTY }
        candidate.resourceCost.consumableItem?.let { penalty += CONSUMABLE_ITEM_PENALTY }
        return penalty
    }
    
    /**
     * Task 7.6: Calculate tactical value from opportunities.
     */
    private suspend fun calculateTacticalValue(candidate: ActionCandidate, context: TacticalContext): Float {
        val opportunities = opportunityEvaluator.evaluateOpportunities(candidate, context)
        return opportunities.sumOf { it.bonusScore.toDouble() }.toFloat()
    }
    
    /**
     * Task 7.7: Calculate positioning score.
     * Considers cover, optimal range, and opportunity attacks.
     */
    @Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod", "NestedBlockDepth", "ReturnCount", "MagicNumber")
    // Acceptable: Positioning logic requires checking multiple action types and ranges
    // Numbers are D&D range conventions
    private fun calculatePositioningScore(candidate: ActionCandidate, context: TacticalContext): Float {
        if (candidate.positions.isEmpty()) return 0f
        
        // For now, simplified scoring
        // In full implementation, would check:
        // - Cover availability at position
        // - Distance to enemies (optimal range)
        // - Number of opportunity attacks provoked
        
        var score = 0f
        
        // Prefer positions that maintain distance from enemies
        val position = candidate.positions.firstOrNull() ?: return 0f
        val enemies = context.enemies
        
        enemies.forEach { enemy ->
            val enemyPos = context.getPosition(enemy.id)
            if (enemyPos != null) {
                val distance = calculateDistance(position, enemyPos)
                
                // Melee: prefer adjacent positions
                if (candidate.action is TacticalAction.Attack && 
                    (candidate.action as TacticalAction.Attack).weaponName.contains("Melee", ignoreCase = true)) {
                    score += if (distance <= 1) MELEE_OPTIMAL_RANGE_BONUS else MELEE_SUBOPTIMAL_RANGE_PENALTY
                }
                
                // Ranged: prefer medium distance (not too close, not too far)
                if (candidate.action is TacticalAction.Attack && 
                    (candidate.action as TacticalAction.Attack).weaponName.contains("Ranged", ignoreCase = true)) {
                    score += when {
                        distance <= 1 -> RANGED_TOO_CLOSE_PENALTY // Disadvantage
                        distance in 2..10 -> RANGED_OPTIMAL_RANGE_BONUS
                        else -> RANGED_TOO_FAR_PENALTY
                    }
                }
                
                // Spellcaster: prefer maximum distance
                if (candidate.action is TacticalAction.CastSpell) {
                    score += when {
                        distance <= 2 -> SPELLCASTER_TOO_CLOSE_PENALTY
                        distance >= 5 -> SPELLCASTER_OPTIMAL_RANGE_BONUS
                        else -> 0f
                    }
                }
            }
        }
        
        return score
    }
    
    /**
     * Calculates grid distance between two positions.
     */
    private fun calculateDistance(pos1: dev.questweaver.domain.values.GridPos, 
                                   pos2: dev.questweaver.domain.values.GridPos): Int {
        return max(kotlin.math.abs(pos1.x - pos2.x), kotlin.math.abs(pos1.y - pos2.y))
    }
    
    /**
     * Task 7.8: Apply difficulty-based random variance.
     * Uses seeded DiceRoller for determinism.
     */
    private fun applyVariance(baseScore: Float, difficulty: Difficulty): Float {
        val variancePercent = when (difficulty) {
            Difficulty.EASY -> EASY_VARIANCE
            Difficulty.NORMAL -> NORMAL_VARIANCE
            Difficulty.HARD -> HARD_VARIANCE
        }
        
        // Roll d100 and normalize to [-1, 1] range
        val roll = roller.d100().result
        val normalized = (roll.toFloat() - DICE_MIDPOINT) / DICE_MIDPOINT
        
        return baseScore * variancePercent * normalized
    }
    
    companion object {
        // Damage estimation constants
        private const val AVERAGE_WEAPON_DAMAGE = 7.5f
        private const val CANTRIP_DAMAGE = 5.5f
        private const val SPELL_LEVEL_1_DAMAGE = 10.5f
        private const val SPELL_LEVEL_2_DAMAGE = 21f
        private const val SPELL_LEVEL_3_DAMAGE = 28f
        private const val DAMAGE_PER_SPELL_LEVEL = 10f
        private const val ABILITY_DAMAGE = 12f
        
        // Critical hit multipliers
        private const val CRIT_MULTIPLIER_NORMAL = 1.05f // 5% crit chance
        private const val CRIT_MULTIPLIER_WITH_ADVANTAGE = 1.0975f // ~9.75% crit chance with advantage
        
        // Resistance multipliers
        private const val IMMUNITY_MULTIPLIER = 0f
        private const val RESISTANCE_MULTIPLIER = 0.5f
        private const val NORMAL_MULTIPLIER = 1.0f
        private const val VULNERABILITY_MULTIPLIER = 2.0f
        
        // Attack bonus estimates
        private const val TYPICAL_ATTACK_BONUS = 5
        private const val TYPICAL_SPELL_ATTACK_BONUS = 5
        private const val TYPICAL_SAVE_DC = 13
        
        // Target priority multipliers
        private const val HEALER_MULTIPLIER = 2.0f
        private const val SPELLCASTER_MULTIPLIER = 1.8f
        private const val CONCENTRATION_MULTIPLIER = 1.5f
        private const val HP_CRITICAL_MULTIPLIER = 1.3f
        private const val HP_LOW_MULTIPLIER = 1.2f
        private const val HP_NORMAL_MULTIPLIER = 1.0f
        
        // Resource cost penalties
        private const val SPELL_SLOT_PENALTY_PER_LEVEL = -10f
        private const val ABILITY_USE_PENALTY = -15f
        private const val CONSUMABLE_ITEM_PENALTY = -20f
        
        // Positioning score bonuses/penalties
        private const val MELEE_OPTIMAL_RANGE_BONUS = 5f
        private const val MELEE_SUBOPTIMAL_RANGE_PENALTY = -10f
        private const val RANGED_OPTIMAL_RANGE_BONUS = 5f
        private const val RANGED_TOO_CLOSE_PENALTY = -15f
        private const val RANGED_TOO_FAR_PENALTY = -5f
        private const val SPELLCASTER_OPTIMAL_RANGE_BONUS = 10f
        private const val SPELLCASTER_TOO_CLOSE_PENALTY = -20f
        
        // Variance constants
        private const val EASY_VARIANCE = 0.30f
        private const val NORMAL_VARIANCE = 0.15f
        private const val HARD_VARIANCE = 0.05f
        private const val DICE_MIDPOINT = 50.5f
    }
}

/**
 * Extension function to calculate total score from breakdown.
 */
fun ScoreBreakdown.totalScore(): Float {
    return (damageScore * hitProbabilityScore * targetPriorityScore) + 
           tacticalValueScore + 
           positioningScore + 
           resourceCostScore
}
