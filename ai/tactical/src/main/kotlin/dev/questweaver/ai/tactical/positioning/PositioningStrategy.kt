package dev.questweaver.ai.tactical.positioning

import dev.questweaver.ai.tactical.ActionType
import dev.questweaver.ai.tactical.TacticalAction
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.core.rules.DiceRoller
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.map.geometry.DistanceCalculator
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.LineOfEffect
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.pathfinding.PathResult
import dev.questweaver.domain.map.pathfinding.Pathfinder
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Determines optimal positioning for creatures during combat.
 * 
 * Evaluates positions based on:
 * - Role-specific preferences (melee, ranged, spellcaster)
 * - Cover availability
 * - Flanking opportunities
 * - Threat avoidance
 * - Opportunity attack risk
 * 
 * Performance target: ≤40ms per decision
 */
@Suppress("TooManyFunctions")
class PositioningStrategy(
    private val pathfinder: Pathfinder,
    @Suppress("UnusedPrivateProperty") private val roller: DiceRoller
) {
    /**
     * Selects the best position for a creature to perform an action.
     * 
     * @param creature The creature moving
     * @param action The action being performed
     * @param target The target of the action (if any)
     * @param context Current tactical situation
     * @param mapGrid The tactical map grid
     * @return Optimal position decision with path and reasoning
     */
    @Suppress("ReturnCount")
    suspend fun selectPosition(
        creature: Creature,
        action: TacticalAction,
        target: Creature?,
        context: TacticalContext,
        mapGrid: MapGrid
    ): PositionDecision {
        val currentPos = context.getPosition(creature.id) 
            ?: return PositionDecision(
                position = GridPos(0, 0),
                path = emptyList(),
                reasoning = "No current position found"
            )
        
        // If action doesn't require movement, stay in place
        if (!requiresMovement(action)) {
            return PositionDecision(
                position = currentPos,
                path = listOf(currentPos),
                reasoning = "Action does not require movement"
            )
        }
        
        // Generate candidate positions based on creature role and HP
        val candidates = generateCandidatePositions(
            creature = creature,
            currentPos = currentPos,
            action = action,
            target = target,
            context = context,
            mapGrid = mapGrid
        )
        
        if (candidates.isEmpty()) {
            return PositionDecision(
                position = currentPos,
                path = listOf(currentPos),
                reasoning = "No valid positions available, holding position"
            )
        }
        
        // Score each candidate position
        val scoredPositions = candidates.map { pos ->
            val score = scorePosition(
                position = pos,
                creature = creature,
                target = target,
                context = context,
                mapGrid = mapGrid
            )
            ScoredPosition(pos, score)
        }.sortedByDescending { it.score }
        
        // Select best position
        val bestPosition = scoredPositions.first()
        
        // Calculate path to best position
        val pathResult = pathfinder.findPath(
            start = currentPos,
            destination = bestPosition.position,
            grid = mapGrid
        )
        
        val path = when (pathResult) {
            is PathResult.Success -> pathResult.path
            else -> listOf(currentPos)
        }
        
        // Generate reasoning
        val reasoning = generateReasoning(
            creature = creature,
            score = bestPosition.score,
            action = action,
            target = target
        )
        
        logger.debug { "Selected position ${bestPosition.position} for ${creature.name} (score: ${bestPosition.score})" }
        
        return PositionDecision(
            position = bestPosition.position,
            path = path,
            reasoning = reasoning
        )
    }
    
    /**
     * Determines if an action requires movement to a new position.
     */
    private fun requiresMovement(action: TacticalAction): Boolean {
        return when (action.actionType) {
            ActionType.MOVE, ActionType.DASH, ActionType.DISENGAGE -> true
            ActionType.ATTACK, ActionType.CAST_SPELL, ActionType.DODGE, 
            ActionType.HELP, ActionType.USE_ABILITY -> false
        }
    }
    
    /**
     * Generates candidate positions based on creature role and situation.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun generateCandidatePositions(
        creature: Creature,
        currentPos: GridPos,
        @Suppress("UnusedParameter") action: TacticalAction,
        target: Creature?,
        context: TacticalContext,
        mapGrid: MapGrid
    ): List<GridPos> {
        val movementRange = creature.speed / FEET_PER_SQUARE
        val reachablePositions = getReachablePositions(currentPos, movementRange, context, mapGrid)
        
        // Determine role-based positioning strategy
        val hpPercentage = creature.hpCurrent.toFloat() / creature.hpMax
        
        return when {
            // Defensive positioning when low HP
            hpPercentage < LOW_HP_THRESHOLD -> {
                generateDefensivePositions(creature, reachablePositions, context)
            }
            
            // Role-based positioning
            isMeleeRole(creature) -> {
                generateMeleePositions(target, reachablePositions, context, mapGrid)
            }
            
            isRangedRole(creature) -> {
                generateRangedPositions(creature, target, reachablePositions, context, mapGrid)
            }
            
            isSpellcasterRole(creature) -> {
                generateSpellcasterPositions(creature, reachablePositions, context)
            }
            
            else -> {
                // Default: positions closer to target
                target?.let { t ->
                    val targetPos = context.getPosition(t.id)
                    targetPos?.let { pos ->
                        reachablePositions.sortedBy { 
                            DistanceCalculator.chebyshevDistance(it, pos) 
                        }.take(MAX_CANDIDATE_POSITIONS)
                    }
                } ?: reachablePositions.take(MAX_CANDIDATE_POSITIONS)
            }
        }
    }
    
    /**
     * Generates defensive positions for low HP creatures.
     * Prioritizes: distance from threats, cover, proximity to allies.
     */
    private fun generateDefensivePositions(
        creature: Creature,
        reachablePositions: List<GridPos>,
        context: TacticalContext
    ): List<GridPos> {
        val enemies = context.getEnemies(creature)
        
        return reachablePositions
            .sortedByDescending { pos ->
                // Maximize distance from all threats
                val minDistanceToEnemy = enemies.minOfOrNull { enemy ->
                    context.getPosition(enemy.id)?.let { enemyPos ->
                        DistanceCalculator.chebyshevDistance(pos, enemyPos).toFloat()
                    } ?: Float.MAX_VALUE
                } ?: Float.MAX_VALUE
                
                minDistanceToEnemy
            }
            .take(MAX_CANDIDATE_POSITIONS)
    }
    
    /**
     * Generates melee positions.
     * Prioritizes: flanking, adjacency to target, avoiding opportunity attacks.
     */
    @Suppress("ReturnCount")
    private fun generateMeleePositions(
        target: Creature?,
        reachablePositions: List<GridPos>,
        context: TacticalContext,
        mapGrid: MapGrid
    ): List<GridPos> {
        val targetPos = target?.let { context.getPosition(it.id) } ?: return emptyList()
        
        // Get positions adjacent to target
        val adjacentPositions = reachablePositions.filter { pos ->
            DistanceCalculator.chebyshevDistance(pos, targetPos) <= MELEE_RANGE &&
            LineOfEffect.hasLineOfEffect(pos, targetPos, mapGrid)
        }
        
        if (adjacentPositions.isEmpty()) {
            // If can't reach target, move closer
            return reachablePositions
                .sortedBy { DistanceCalculator.chebyshevDistance(it, targetPos) }
                .take(MAX_CANDIDATE_POSITIONS)
        }
        
        return adjacentPositions.take(MAX_CANDIDATE_POSITIONS)
    }
    
    /**
     * Generates ranged positions.
     * Prioritizes: cover, optimal range, distance from melee threats.
     */
    private fun generateRangedPositions(
        creature: Creature,
        target: Creature?,
        reachablePositions: List<GridPos>,
        context: TacticalContext,
        mapGrid: MapGrid
    ): List<GridPos> {
        val targetPos = target?.let { context.getPosition(it.id) } ?: return emptyList()
        val enemies = context.getEnemies(creature)
        
        return reachablePositions
            .filter { pos ->
                // Must have line of sight to target
                LineOfEffect.hasLineOfEffect(pos, targetPos, mapGrid)
            }
            .sortedByDescending { pos ->
                val distanceToTarget = DistanceCalculator.chebyshevDistance(pos, targetPos)
                val inOptimalRange = distanceToTarget in RANGED_MIN_RANGE..RANGED_MAX_RANGE
                
                // Maximize distance from melee threats
                val minDistanceToEnemy = enemies.minOfOrNull { enemy ->
                    context.getPosition(enemy.id)?.let { enemyPos ->
                        DistanceCalculator.chebyshevDistance(pos, enemyPos).toFloat()
                    } ?: Float.MAX_VALUE
                } ?: Float.MAX_VALUE
                
                // Prefer optimal range and distance from threats
                (if (inOptimalRange) OPTIMAL_RANGE_BONUS else 0f) + minDistanceToEnemy
            }
            .take(MAX_CANDIDATE_POSITIONS)
    }
    
    /**
     * Generates spellcaster positions.
     * Prioritizes: AoE coverage, avoiding friendly fire, distance from threats.
     */
    private fun generateSpellcasterPositions(
        creature: Creature,
        reachablePositions: List<GridPos>,
        context: TacticalContext
    ): List<GridPos> {
        val enemies = context.getEnemies(creature)
        
        return reachablePositions
            .sortedByDescending { pos ->
                // Maximize distance from all threats
                val minDistanceToEnemy = enemies.minOfOrNull { enemy ->
                    context.getPosition(enemy.id)?.let { enemyPos ->
                        DistanceCalculator.chebyshevDistance(pos, enemyPos).toFloat()
                    } ?: Float.MAX_VALUE
                } ?: Float.MAX_VALUE
                
                // Prefer positions far from threats
                minDistanceToEnemy
            }
            .take(MAX_CANDIDATE_POSITIONS)
    }
    
    /**
     * Scores a position based on tactical value.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun scorePosition(
        position: GridPos,
        creature: Creature,
        target: Creature?,
        context: TacticalContext,
        mapGrid: MapGrid
    ): Float {
        var score = 0f
        
        val currentPos = context.getPosition(creature.id) ?: return 0f
        val hpPercentage = creature.hpCurrent.toFloat() / creature.hpMax
        
        // Cover bonus (placeholder - would need actual cover detection)
        if (hasCover(mapGrid)) {
            score += COVER_BONUS
        }
        
        // Range scoring
        target?.let { t ->
            val targetPos = context.getPosition(t.id)
            targetPos?.let { tPos ->
                val distance = DistanceCalculator.chebyshevDistance(position, tPos)
                
                when {
                    isMeleeRole(creature) -> {
                        // Melee prefers adjacent positions
                        if (distance <= MELEE_RANGE) {
                            score += MELEE_ADJACENT_BONUS
                            
                            // Flanking bonus (placeholder)
                            if (isFlanking()) {
                                score += FLANKING_BONUS
                            }
                        }
                    }
                    
                    isRangedRole(creature) -> {
                        // Ranged prefers optimal range
                        if (distance in RANGED_MIN_RANGE..RANGED_MAX_RANGE) {
                            score += OPTIMAL_RANGE_BONUS
                        }
                    }
                    
                    isSpellcasterRole(creature) -> {
                        // Spellcasters prefer distance
                        if (distance >= SPELLCASTER_MIN_RANGE) {
                            score += SPELLCASTER_DISTANCE_BONUS
                        }
                    }
                }
            }
        }
        
        // Threat avoidance (especially when low HP)
        val enemies = context.getEnemies(creature)
        val minDistanceToEnemy = enemies.minOfOrNull { enemy ->
            context.getPosition(enemy.id)?.let { enemyPos ->
                DistanceCalculator.chebyshevDistance(position, enemyPos).toFloat()
            } ?: Float.MAX_VALUE
        } ?: Float.MAX_VALUE
        
        if (hpPercentage < LOW_HP_THRESHOLD) {
            // Low HP: heavily prioritize distance from threats
            score += minDistanceToEnemy * LOW_HP_DISTANCE_MULTIPLIER
        }
        
        // Opportunity attack penalty (placeholder)
        val opportunityAttacks = countOpportunityAttacks()
        score -= opportunityAttacks * OPPORTUNITY_ATTACK_PENALTY
        
        // Ally proximity bonus (for support/defensive positioning)
        val allies = context.getAllies(creature)
        val nearbyAllies = allies.count { ally ->
            context.getPosition(ally.id)?.let { allyPos ->
                DistanceCalculator.chebyshevDistance(position, allyPos) <= ALLY_PROXIMITY_RANGE
            } ?: false
        }
        
        if (hpPercentage < LOW_HP_THRESHOLD && nearbyAllies > 0) {
            score += ALLY_PROXIMITY_BONUS * nearbyAllies
        }
        
        return score
    }
    
    /**
     * Gets all positions reachable within movement range.
     */
    private fun getReachablePositions(
        start: GridPos,
        range: Int,
        context: TacticalContext,
        mapGrid: MapGrid
    ): List<GridPos> {
        val positions = mutableListOf<GridPos>()
        
        for (dx in -range..range) {
            for (dy in -range..range) {
                val pos = GridPos(start.x + dx, start.y + dy)
                
                // Check if position is valid and unoccupied
                if (isValidPosition(pos, context, mapGrid)) {
                    positions.add(pos)
                }
            }
        }
        
        return positions
    }
    
    /**
     * Checks if a position is valid and unoccupied.
     */
    private fun isValidPosition(pos: GridPos, context: TacticalContext, mapGrid: MapGrid): Boolean {
        // Check if position is within map bounds
        if (!mapGrid.isInBounds(pos)) {
            return false
        }
        
        // Check if position is occupied by another creature
        val occupied = context.creatures.any { creature ->
            context.getPosition(creature.id) == pos
        }
        
        return !occupied
    }
    
    /**
     * Checks if a position provides cover.
     * Placeholder implementation - would need actual cover mechanics.
     */
    @Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
    private fun hasCover(mapGrid: MapGrid): Boolean {
        return false
    }
    
    /**
     * Checks if a position enables flanking.
     * Placeholder implementation - would need actual flanking detection.
     */
    @Suppress("FunctionOnlyReturningConstant")
    private fun isFlanking(): Boolean {
        return false
    }
    
    /**
     * Counts opportunity attacks provoked by moving to a position.
     * Placeholder implementation - would need actual opportunity attack detection.
     */
    @Suppress("FunctionOnlyReturningConstant")
    private fun countOpportunityAttacks(): Int {
        return 0
    }
    
    /**
     * Determines if creature is a melee role.
     */
    private fun isMeleeRole(creature: Creature): Boolean {
        // Simplified: check if creature has melee weapons
        // In full implementation, would check weapon properties
        return true // Placeholder
    }
    
    /**
     * Determines if creature is a ranged role.
     */
    private fun isRangedRole(creature: Creature): Boolean {
        // Simplified: check if creature has ranged weapons
        // In full implementation, would check weapon properties
        return false // Placeholder
    }
    
    /**
     * Determines if creature is a spellcaster role.
     */
    private fun isSpellcasterRole(creature: Creature): Boolean {
        // Check if creature has spell slots
        return creature.spellSlots.values.sum() > 0
    }
    
    /**
     * Generates reasoning for position selection.
     */
    @Suppress("UnusedParameter")
    private fun generateReasoning(
        creature: Creature,
        score: Float,
        action: TacticalAction,
        target: Creature?
    ): String {
        val hpPercentage = creature.hpCurrent.toFloat() / creature.hpMax
        
        return when {
            hpPercentage < LOW_HP_THRESHOLD -> {
                "Defensive positioning: Low HP (${(hpPercentage * PERCENTAGE_MULTIPLIER).toInt()}%), maximizing distance from threats"
            }
            
            isMeleeRole(creature) -> {
                target?.let { "Melee positioning: Moving to engage ${it.name}" }
                    ?: "Melee positioning: Advancing toward enemies"
            }
            
            isRangedRole(creature) -> {
                "Ranged positioning: Maintaining optimal range with cover"
            }
            
            isSpellcasterRole(creature) -> {
                "Spellcaster positioning: Maintaining distance from threats"
            }
            
            else -> {
                "Tactical positioning (score: $score)"
            }
        }
    }
    
    private data class ScoredPosition(
        val position: GridPos,
        val score: Float
    )
    
    companion object {
        private const val FEET_PER_SQUARE = 5
        private const val MELEE_RANGE = 1
        private const val RANGED_MIN_RANGE = 2
        private const val RANGED_MAX_RANGE = 10
        private const val SPELLCASTER_MIN_RANGE = 5
        private const val LOW_HP_THRESHOLD = 0.3f
        private const val ALLY_PROXIMITY_RANGE = 2
        private const val PERCENTAGE_MULTIPLIER = 100f
        
        private const val MAX_CANDIDATE_POSITIONS = 10
        
        // Score bonuses
        private const val COVER_BONUS = 15f
        private const val MELEE_ADJACENT_BONUS = 20f
        private const val FLANKING_BONUS = 15f
        private const val OPTIMAL_RANGE_BONUS = 10f
        private const val SPELLCASTER_DISTANCE_BONUS = 10f
        private const val ALLY_PROXIMITY_BONUS = 5f
        private const val LOW_HP_DISTANCE_MULTIPLIER = 2f
        
        // Score penalties
        private const val OPPORTUNITY_ATTACK_PENALTY = 15f
    }
}

/**
 * Result of position selection with path and reasoning.
 */
data class PositionDecision(
    val position: GridPos,
    val path: List<GridPos>,
    val reasoning: String
)
