package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.Range
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.results.ResourceCost
import dev.questweaver.core.rules.validation.state.EncounterState
import dev.questweaver.core.rules.validation.state.GridPos
import kotlin.math.abs
import kotlin.math.max

/**
 * Validates range and line-of-effect requirements for actions.
 *
 * This validator checks whether targets are within range and whether there is
 * an unobstructed path (line-of-effect) between the actor and target.
 */
class RangeValidator {
    /**
     * Validates whether the target is within range and line-of-effect.
     *
     * @param action The action with range requirements
     * @param actorPos The actor's position
     * @param targetPos The target's position (null for self-targeted actions)
     * @param encounterState Current encounter state with obstacles
     * @return ValidationResult indicating success or failure with distance/obstacle info
     */
    fun validateRange(
        action: GameAction,
        actorPos: GridPos,
        targetPos: GridPos?,
        encounterState: EncounterState
    ): ValidationResult {
        val range = getActionRange(action)
        
        // Self-targeted actions or actions without targets don't need range validation
        return when {
            range is Range.Self || targetPos == null -> ValidationResult.Success(ResourceCost.None)
            else -> validateDistanceAndLineOfEffect(actorPos, targetPos, range, encounterState)
        }
    }
    
    /**
     * Validates both distance and line-of-effect in a single check.
     */
    private fun validateDistanceAndLineOfEffect(
        actorPos: GridPos,
        targetPos: GridPos,
        range: Range,
        encounterState: EncounterState
    ): ValidationResult {
        // First check distance
        val distanceValidation = validateDistance(actorPos, targetPos, range)
        if (distanceValidation is ValidationResult.Failure) {
            return distanceValidation
        }
        
        // Then check line-of-effect
        return validateLineOfEffect(actorPos, targetPos, range, encounterState)
    }
    
    /**
     * Validates that the target is within the action's range.
     */
    private fun validateDistance(
        actorPos: GridPos,
        targetPos: GridPos,
        range: Range
    ): ValidationResult {
        val distance = calculateDistance(actorPos, targetPos)
        val maxRange = when (range) {
            is Range.Touch -> TOUCH_RANGE_FEET
            is Range.Feet -> range.distance
            is Range.Sight -> Int.MAX_VALUE
            is Range.Self -> 0
            is Range.Radius -> range.feet
        }
        
        return if (distance > maxRange) {
            ValidationResult.Failure(
                ValidationFailure.OutOfRange(
                    actualDistance = distance,
                    maxRange = maxRange,
                    rangeType = range
                )
            )
        } else {
            ValidationResult.Success(ResourceCost.None)
        }
    }
    
    /**
     * Validates that line-of-effect exists to the target.
     */
    private fun validateLineOfEffect(
        actorPos: GridPos,
        targetPos: GridPos,
        range: Range,
        encounterState: EncounterState
    ): ValidationResult {
        // Touch range doesn't require line-of-effect check
        if (range is Range.Touch || range is Range.Self) {
            return ValidationResult.Success(ResourceCost.None)
        }
        
        return if (!hasLineOfEffect(actorPos, targetPos, encounterState.obstacles)) {
            val blockingObstacle = findBlockingObstacle(actorPos, targetPos, encounterState.obstacles)
            ValidationResult.Failure(
                ValidationFailure.LineOfEffectBlocked(
                    blockingObstacle = blockingObstacle,
                    obstacleType = "obstacle"
                )
            )
        } else {
            ValidationResult.Success(ResourceCost.None)
        }
    }
    
    /**
     * Calculates distance between two positions using D&D 5e rules.
     *
     * D&D 5e uses a simplified diagonal movement rule where each square
     * (including diagonals) costs 5 feet. This is equivalent to Chebyshev distance.
     *
     * @param from Source position
     * @param to Target position
     * @return Distance in feet (5ft per square)
     */
    fun calculateDistance(from: GridPos, to: GridPos): Int {
        val dx = abs(to.x - from.x)
        val dy = abs(to.y - from.y)
        // Chebyshev distance: max of dx and dy, then multiply by feet per square
        return max(dx, dy) * FEET_PER_SQUARE
    }
    
    companion object {
        private const val FEET_PER_SQUARE = 5
        private const val TOUCH_RANGE_FEET = 5
        private const val DEFAULT_SPELL_RANGE_FEET = 60
        private const val DEFAULT_FEATURE_RANGE_FEET = 30
    }
    
    /**
     * Checks if line-of-effect exists between two positions.
     *
     * Uses Bresenham's line algorithm to check if any obstacles intersect
     * the path between source and target.
     *
     * @param from Source position
     * @param to Target position
     * @param obstacles Set of obstacle positions
     * @return True if unobstructed path exists
     */
    fun hasLineOfEffect(
        from: GridPos,
        to: GridPos,
        obstacles: Set<GridPos>
    ): Boolean {
        if (obstacles.isEmpty()) return true
        
        val line = bresenhamLine(from, to)
        // Exclude the start and end positions from obstacle check
        val pathPositions = line.drop(1).dropLast(1)
        
        return pathPositions.none { it in obstacles }
    }
    
    /**
     * Finds the blocking obstacle closest to the actor.
     */
    private fun findBlockingObstacle(
        from: GridPos,
        to: GridPos,
        obstacles: Set<GridPos>
    ): GridPos {
        val line = bresenhamLine(from, to)
        val pathPositions = line.drop(1).dropLast(1)
        
        return pathPositions.firstOrNull { it in obstacles } ?: from
    }
    
    /**
     * Generates a line of grid positions using Bresenham's line algorithm.
     *
     * @param from Start position
     * @param to End position
     * @return List of grid positions along the line (including start and end)
     */
    private fun bresenhamLine(from: GridPos, to: GridPos): List<GridPos> {
        val positions = mutableListOf<GridPos>()
        
        var x0 = from.x
        var y0 = from.y
        val x1 = to.x
        val y1 = to.y
        
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        
        while (true) {
            positions.add(GridPos(x0, y0))
            
            if (x0 == x1 && y0 == y1) break
            
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }
        
        return positions
    }
    
    /**
     * Gets the range requirement for an action.
     *
     * TODO: This should look up action/spell/weapon data from registries.
     * For now, returns placeholder values.
     */
    private fun getActionRange(action: GameAction): Range {
        return when (action) {
            is GameAction.Attack -> {
                // TODO: Look up weapon range
                // Melee weapons are touch, ranged weapons have specific ranges
                Range.Touch
            }
            is GameAction.CastSpell -> {
                // TODO: Look up spell range
                // For now, assume default range for most spells
                Range.Feet(DEFAULT_SPELL_RANGE_FEET)
            }
            is GameAction.OpportunityAttack -> {
                // Opportunity attacks are always melee (touch range)
                Range.Touch
            }
            is GameAction.UseClassFeature -> {
                // TODO: Look up feature range
                Range.Feet(DEFAULT_FEATURE_RANGE_FEET)
            }
            is GameAction.Move,
            is GameAction.Dash,
            is GameAction.Disengage,
            is GameAction.Dodge -> {
                // These actions don't have range requirements
                Range.Self
            }
        }
    }
}
