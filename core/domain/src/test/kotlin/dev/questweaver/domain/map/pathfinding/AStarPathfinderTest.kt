package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.TerrainType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for basic pathfinding functionality.
 *
 * Tests cover:
 * - Straight line paths (horizontal, vertical, diagonal)
 * - Paths around single obstacle
 * - Paths around multiple obstacles
 * - No path exists when completely blocked
 * - Start equals destination (trivial path)
 */
class AStarPathfinderTest : FunSpec({
    
    val pathfinder = AStarPathfinder()
    
    context("straight line paths") {
        
        test("finds horizontal path from left to right") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(0, 5)
            val destination = GridPos(5, 5)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            success.path.first() shouldBe start
            success.path.last() shouldBe destination
            success.path shouldHaveSize 6 // 0,5 -> 1,5 -> 2,5 -> 3,5 -> 4,5 -> 5,5
            success.totalCost shouldBe 5
        }
        
        test("finds vertical path from top to bottom") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(5, 0)
            val destination = GridPos(5, 5)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            success.path.first() shouldBe start
            success.path.last() shouldBe destination
            success.path shouldHaveSize 6
            success.totalCost shouldBe 5
        }
        
        test("finds diagonal path") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(0, 0)
            val destination = GridPos(5, 5)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            success.path.first() shouldBe start
            success.path.last() shouldBe destination
            // Diagonal movement in D&D 5e: Chebyshev distance = max(dx, dy) = 5
            success.totalCost shouldBe 5
        }
    }
    
    context("paths around obstacles") {
        
        test("finds path around single obstacle") {
            // Place obstacle at (2, 2)
            val grid = createEmptyGrid(10, 10)
                .withCellProperties(
                    GridPos(2, 2),
                    dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
                )
            
            val start = GridPos(1, 2)
            val destination = GridPos(3, 2)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            success.path.first() shouldBe start
            success.path.last() shouldBe destination
            // Path should go around the obstacle
            success.path shouldContain GridPos(1, 2)
            success.path shouldContain GridPos(3, 2)
            // Should NOT contain the obstacle
            success.path.none { it == GridPos(2, 2) } shouldBe true
        }
        
        test("finds path around multiple obstacles") {
            // Create a wall of obstacles
            var grid = createEmptyGrid(10, 10)
            val obstacleProps = dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
            grid = grid.withCellProperties(GridPos(2, 0), obstacleProps)
            grid = grid.withCellProperties(GridPos(2, 1), obstacleProps)
            grid = grid.withCellProperties(GridPos(2, 2), obstacleProps)
            grid = grid.withCellProperties(GridPos(2, 3), obstacleProps)
            
            val start = GridPos(0, 2)
            val destination = GridPos(4, 2)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            success.path.first() shouldBe start
            success.path.last() shouldBe destination
            // Path should go around the wall (either above or below)
            success.path.none { it.x == 2 && it.y in 0..3 } shouldBe true
        }
    }
    
    context("no path exists") {
        
        test("returns NoPathFound when destination is completely blocked") {
            val destination = GridPos(5, 5)
            
            // Surround destination with obstacles
            var grid = createEmptyGrid(10, 10)
            val obstacleProps = dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
            for (neighbor in destination.neighbors()) {
                grid = grid.withCellProperties(neighbor, obstacleProps)
            }
            
            val start = GridPos(0, 0)
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.NoPathFound>()
        }
        
        test("returns NoPathFound when start is surrounded by obstacles") {
            val start = GridPos(5, 5)
            
            // Surround start with obstacles
            var grid = createEmptyGrid(10, 10)
            val obstacleProps = dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
            for (neighbor in start.neighbors()) {
                grid = grid.withCellProperties(neighbor, obstacleProps)
            }
            
            val destination = GridPos(0, 0)
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.NoPathFound>()
        }
    }
    
    context("trivial paths") {
        
        test("returns single-element path when start equals destination") {
            val grid = createEmptyGrid(10, 10)
            val position = GridPos(5, 5)
            
            val result = pathfinder.findPath(position, position, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            success.path shouldHaveSize 1
            success.path.first() shouldBe position
            success.totalCost shouldBe 0
        }
    }
})

/**
 * Creates an empty grid with all normal terrain and no obstacles.
 */
private fun createEmptyGrid(width: Int, height: Int): MapGrid {
    return MapGrid(width, height)
}
