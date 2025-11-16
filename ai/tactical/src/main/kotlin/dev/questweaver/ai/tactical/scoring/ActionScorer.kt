package dev.questweaver.ai.tactical.scoring

import dev.questweaver.ai.tactical.model.*
import dev.questweaver.ai.tactical.resources.ThreatAssessor
import dev.questweaver.core.rules.dice.DiceRoller
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Scores action candidates using utility-based evaluation.
 * 
 * Formula: (Damage × HitProb × TargetPriority) + TacticalValue + PositioningValue - ResourceCost + RandomVariance
 */
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
        val damage = 0f // TODO: Calculate expected damage
        val hitProb = 0f // TODO: Calculate hit probability  
        val targetPriority = calculateTargetPriority(candidate, context)
        val resourceCost = calculateResourceCost(candidate)
        val tacticalValue = calculateTacticalValue(candidate, context)
        val positioning = 0f // TODO: Calculate positioning score
        
        return ScoreBreakdown(damage, hitProb, targetPriority, resourceCost, tacticalValue, positioning)
    }
    
    private fun calculateTargetPriority(candidate: ActionCandidate, context: TacticalContext): Float {
        if (candidate.targets.isEmpty()) return 1.0f
        return threatAssessor.assessThreat(candidate.targets.first(), context)
    }
    
    private fun calculateResourceCost(candidate: ActionCandidate): Float {
        var penalty = 0f
        candidate.resourceCost.spellSlot?.let { penalty += it * -10f }
        candidate.resourceCost.abilityUse?.let { penalty += -15f }
        candidate.resourceCost.consumableItem?.let { penalty += -20f }
        return penalty
    }
    
    private suspend fun calculateTacticalValue(candidate: ActionCandidate, context: TacticalContext): Float {
        val opportunities = opportunityEvaluator.evaluateOpportunities(candidate, context)
        return opportunities.sumOf { it.bonusScore.toDouble() }.toFloat()
    }
    
    private fun applyVariance(baseScore: Float, difficulty: Difficulty): Float {
        val variancePercent = when (difficulty) {
            Difficulty.EASY -> 0.30f
            Difficulty.NORMAL -> 0.15f
            Difficulty.HARD -> 0.05f
        }
        
        val roll = roller.d100().value
        val normalized = (roll - 50.5f) / 50.5f
        return baseScore * variancePercent * normalized
    }
}
