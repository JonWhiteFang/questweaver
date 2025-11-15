package dev.questweaver.feature.encounter.state

import dev.questweaver.feature.map.ui.GridPos
import dev.questweaver.feature.map.ui.RangeOverlayData
import dev.questweaver.feature.map.ui.RangeType
import dev.questweaver.feature.map.ui.AoEOverlayData
import dev.questweaver.domain.map.geometry.AoETemplate

/**
 * Helper class for map integration functionality.
 * Provides methods to build map overlays for pathfinding, range, and AoE visualization.
 */
object MapIntegration {
    
    /**
     * Builds range overlay data for movement range visualization.
     *
     * @param origin The origin position (active creature)
     * @param movementRemaining Remaining movement in feet
     * @param blockedPositions Set of blocked positions
     * @return RangeOverlayData for movement range
     */
    fun buildMovementRangeOverlay(
        origin: GridPos,
        movementRemaining: Int,
        blockedPositions: Set<GridPos>
    ): RangeOverlayData {
        // Calculate reachable positions within movement range
        // Each grid square is 5 feet in D&D 5e
        val rangeInSquares = movementRemaining / 5
        
        val reachablePositions = mutableSetOf<GridPos>()
        
        // Simple flood fill to find reachable positions
        // This is a simplified version - actual pathfinding would be more complex
        for (dx in -rangeInSquares..rangeInSquares) {
            for (dy in -rangeInSquares..rangeInSquares) {
                val pos = GridPos(origin.x + dx, origin.y + dy)
                
                // Check if within range (Chebyshev distance)
                val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                if (distance <= rangeInSquares && !blockedPositions.contains(pos)) {
                    reachablePositions.add(pos)
                }
            }
        }
        
        return RangeOverlayData(
            origin = origin,
            positions = reachablePositions,
            rangeType = RangeType.MOVEMENT
        )
    }
    
    /**
     * Builds range overlay data for weapon range visualization.
     *
     * @param origin The origin position (attacker)
     * @param rangeInFeet Weapon range in feet
     * @return RangeOverlayData for weapon range
     */
    fun buildWeaponRangeOverlay(
        origin: GridPos,
        rangeInFeet: Int
    ): RangeOverlayData {
        // Calculate positions within weapon range
        val rangeInSquares = rangeInFeet / 5
        
        val positionsInRange = mutableSetOf<GridPos>()
        
        for (dx in -rangeInSquares..rangeInSquares) {
            for (dy in -rangeInSquares..rangeInSquares) {
                val pos = GridPos(origin.x + dx, origin.y + dy)
                
                // Check if within range (Chebyshev distance)
                val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                if (distance <= rangeInSquares) {
                    positionsInRange.add(pos)
                }
            }
        }
        
        return RangeOverlayData(
            origin = origin,
            positions = positionsInRange,
            rangeType = RangeType.WEAPON
        )
    }
    
    /**
     * Builds range overlay data for spell range visualization.
     *
     * @param origin The origin position (caster)
     * @param rangeInFeet Spell range in feet
     * @return RangeOverlayData for spell range
     */
    fun buildSpellRangeOverlay(
        origin: GridPos,
        rangeInFeet: Int
    ): RangeOverlayData {
        // Calculate positions within spell range
        val rangeInSquares = rangeInFeet / 5
        
        val positionsInRange = mutableSetOf<GridPos>()
        
        for (dx in -rangeInSquares..rangeInSquares) {
            for (dy in -rangeInSquares..rangeInSquares) {
                val pos = GridPos(origin.x + dx, origin.y + dy)
                
                // Check if within range (Chebyshev distance)
                val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                if (distance <= rangeInSquares) {
                    positionsInRange.add(pos)
                }
            }
        }
        
        return RangeOverlayData(
            origin = origin,
            positions = positionsInRange,
            rangeType = RangeType.SPELL
        )
    }
    
    /**
     * Builds AoE overlay data for area-of-effect spell visualization.
     *
     * @param template The AoE template (sphere, cube, cone, etc.)
     * @param origin The origin position of the AoE
     * @param radiusInFeet The radius or size of the AoE in feet
     * @return AoEOverlayData for AoE visualization
     */
    fun buildAoEOverlay(
        template: AoETemplate,
        origin: GridPos,
        radiusInFeet: Int
    ): AoEOverlayData {
        val affectedPositions = when (template) {
            AoETemplate.SPHERE -> calculateSphereAoE(origin, radiusInFeet)
            AoETemplate.CUBE -> calculateCubeAoE(origin, radiusInFeet)
            AoETemplate.CONE -> calculateConeAoE(origin, radiusInFeet)
            AoETemplate.LINE -> calculateLineAoE(origin, radiusInFeet)
            AoETemplate.CYLINDER -> calculateCylinderAoE(origin, radiusInFeet)
        }
        
        return AoEOverlayData(
            template = template,
            origin = origin,
            affectedPositions = affectedPositions
        )
    }
    
    /**
     * Calculates affected positions for a sphere AoE.
     */
    private fun calculateSphereAoE(origin: GridPos, radiusInFeet: Int): Set<GridPos> {
        val radiusInSquares = radiusInFeet / 5
        val affected = mutableSetOf<GridPos>()
        
        for (dx in -radiusInSquares..radiusInSquares) {
            for (dy in -radiusInSquares..radiusInSquares) {
                val pos = GridPos(origin.x + dx, origin.y + dy)
                
                // Check if within radius (Chebyshev distance for D&D 5e)
                val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                if (distance <= radiusInSquares) {
                    affected.add(pos)
                }
            }
        }
        
        return affected
    }
    
    /**
     * Calculates affected positions for a cube AoE.
     */
    private fun calculateCubeAoE(origin: GridPos, sizeInFeet: Int): Set<GridPos> {
        val sizeInSquares = sizeInFeet / 5
        val affected = mutableSetOf<GridPos>()
        
        for (dx in 0 until sizeInSquares) {
            for (dy in 0 until sizeInSquares) {
                affected.add(GridPos(origin.x + dx, origin.y + dy))
            }
        }
        
        return affected
    }
    
    /**
     * Calculates affected positions for a cone AoE.
     * Simplified implementation - actual cone calculation would be more complex.
     */
    private fun calculateConeAoE(origin: GridPos, lengthInFeet: Int): Set<GridPos> {
        val lengthInSquares = lengthInFeet / 5
        val affected = mutableSetOf<GridPos>()
        
        // Simplified cone pointing north
        for (dy in 0 until lengthInSquares) {
            val width = dy + 1
            for (dx in -width..width) {
                affected.add(GridPos(origin.x + dx, origin.y + dy))
            }
        }
        
        return affected
    }
    
    /**
     * Calculates affected positions for a line AoE.
     */
    private fun calculateLineAoE(origin: GridPos, lengthInFeet: Int): Set<GridPos> {
        val lengthInSquares = lengthInFeet / 5
        val affected = mutableSetOf<GridPos>()
        
        // Simplified line pointing north
        for (dy in 0 until lengthInSquares) {
            affected.add(GridPos(origin.x, origin.y + dy))
        }
        
        return affected
    }
    
    /**
     * Calculates affected positions for a cylinder AoE.
     */
    private fun calculateCylinderAoE(origin: GridPos, radiusInFeet: Int): Set<GridPos> {
        // Cylinder is similar to sphere for 2D grid
        return calculateSphereAoE(origin, radiusInFeet)
    }
    
    /**
     * Validates a movement path against blocked positions.
     *
     * @param path The proposed movement path
     * @param blockedPositions Set of blocked positions
     * @return true if the path is valid, false otherwise
     */
    fun validateMovementPath(
        path: List<GridPos>,
        blockedPositions: Set<GridPos>
    ): Boolean {
        // Check if any position in the path is blocked
        return path.none { blockedPositions.contains(it) }
    }
    
    /**
     * Calculates the movement cost for a path considering difficult terrain.
     *
     * @param path The movement path
     * @param difficultTerrain Set of difficult terrain positions
     * @return Total movement cost in feet
     */
    fun calculateMovementCost(
        path: List<GridPos>,
        difficultTerrain: Set<GridPos>
    ): Int {
        var cost = 0
        
        for (i in 0 until path.size - 1) {
            val current = path[i]
            val next = path[i + 1]
            
            // Base cost is 5 feet per square
            var stepCost = 5
            
            // Diagonal movement costs the same in D&D 5e
            // (simplified rules, not using alternating 5/10 feet)
            
            // Difficult terrain doubles the cost
            if (difficultTerrain.contains(next)) {
                stepCost *= 2
            }
            
            cost += stepCost
        }
        
        return cost
    }
}
