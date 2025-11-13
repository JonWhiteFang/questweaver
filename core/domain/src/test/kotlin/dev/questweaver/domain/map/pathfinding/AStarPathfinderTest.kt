package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.TerrainType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
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
    
    context("movement costs") {
        
        test("path through normal terrain costs 1 per cell") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(0, 0)
            val destination = GridPos(3, 0)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // 3 cells to traverse (not counting start)
            success.totalCost shouldBe 3
        }
        
        test("path through difficult terrain costs 2 per cell") {
            var grid = createEmptyGrid(10, 10)
            val difficultProps = dev.questweaver.domain.map.geometry.CellProperties(
                terrainType = TerrainType.DIFFICULT
            )
            
            // Make entire grid difficult terrain to force path through it
            for (x in 0..9) {
                for (y in 0..9) {
                    grid = grid.withCellProperties(GridPos(x, y), difficultProps)
                }
            }
            
            val start = GridPos(0, 0)
            val destination = GridPos(2, 0)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // 2 cells of difficult terrain = 2 * 2 = 4
            success.totalCost shouldBe 4
        }
        
        test("path through mixed terrain calculates correct cost") {
            var grid = createEmptyGrid(10, 10)
            val difficultProps = dev.questweaver.domain.map.geometry.CellProperties(
                terrainType = TerrainType.DIFFICULT
            )
            
            // Create a corridor forcing path through mixed terrain
            // Block all cells except the path we want
            val obstacleProps = dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
            for (y in 1..9) {
                grid = grid.withCellProperties(GridPos(0, y), obstacleProps)
                grid = grid.withCellProperties(GridPos(1, y), obstacleProps)
                grid = grid.withCellProperties(GridPos(2, y), obstacleProps)
                grid = grid.withCellProperties(GridPos(3, y), obstacleProps)
            }
            
            // Make cells (1,0) and (2,0) difficult, (3,0) normal
            grid = grid.withCellProperties(GridPos(1, 0), difficultProps)
            grid = grid.withCellProperties(GridPos(2, 0), difficultProps)
            
            val start = GridPos(0, 0)
            val destination = GridPos(3, 0)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // 2 difficult (cost 2 each) + 1 normal (cost 1) = 5
            success.totalCost shouldBe 5
        }
        
        test("pathfinder prefers lower cost path when alternatives exist") {
            var grid = createEmptyGrid(10, 10)
            val difficultProps = dev.questweaver.domain.map.geometry.CellProperties(
                terrainType = TerrainType.DIFFICULT
            )
            
            // Create two paths: one through difficult terrain (straight), one through normal (around)
            // Difficult path: (0,0) -> (1,0) -> (2,0) -> (3,0) would cost 5 (2+2+1)
            grid = grid.withCellProperties(GridPos(1, 0), difficultProps)
            grid = grid.withCellProperties(GridPos(2, 0), difficultProps)
            
            // Normal path available via diagonals through (1,1) and (2,1)
            val start = GridPos(0, 0)
            val destination = GridPos(3, 0)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // Should prefer the path that goes around difficult terrain via diagonals
            // Diagonal to (1,1), diagonal to (2,1), diagonal to (3,0) = 3 moves, cost 3
            success.totalCost shouldBe 3
        }
        
        test("diagonal movement cost equals orthogonal per D&D 5e") {
            val grid = createEmptyGrid(10, 10)
            
            // Test orthogonal movement
            val orthogonalStart = GridPos(0, 0)
            val orthogonalDest = GridPos(3, 0)
            val orthogonalResult = pathfinder.findPath(orthogonalStart, orthogonalDest, grid)
            
            // Test diagonal movement (same distance using Chebyshev)
            val diagonalStart = GridPos(0, 0)
            val diagonalDest = GridPos(3, 3)
            val diagonalResult = pathfinder.findPath(diagonalStart, diagonalDest, grid)
            
            orthogonalResult.shouldBeInstanceOf<PathResult.Success>()
            diagonalResult.shouldBeInstanceOf<PathResult.Success>()
            
            val orthogonalSuccess = orthogonalResult as PathResult.Success
            val diagonalSuccess = diagonalResult as PathResult.Success
            
            // Both should cost 3 (Chebyshev distance)
            orthogonalSuccess.totalCost shouldBe 3
            diagonalSuccess.totalCost shouldBe 3
        }
    }
    
    context("obstacles and occupancy") {
        
        test("obstacle blocks path") {
            var grid = createEmptyGrid(10, 10)
            val obstacleProps = dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
            
            // Place obstacle directly in the path
            grid = grid.withCellProperties(GridPos(2, 0), obstacleProps)
            
            val start = GridPos(0, 0)
            val destination = GridPos(4, 0)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // Path should go around the obstacle
            success.path.none { it == GridPos(2, 0) } shouldBe true
        }
        
        test("IMPASSABLE terrain blocks path") {
            var grid = createEmptyGrid(10, 10)
            val impassableProps = dev.questweaver.domain.map.geometry.CellProperties(
                terrainType = TerrainType.IMPASSABLE
            )
            
            // Place impassable terrain in the path
            grid = grid.withCellProperties(GridPos(2, 0), impassableProps)
            
            val start = GridPos(0, 0)
            val destination = GridPos(4, 0)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // Path should go around the impassable terrain
            success.path.none { it == GridPos(2, 0) } shouldBe true
        }
        
        test("occupied cell blocks intermediate path") {
            var grid = createEmptyGrid(10, 10)
            val occupiedProps = dev.questweaver.domain.map.geometry.CellProperties(occupiedBy = 123L)
            
            // Place occupied cell in the path
            grid = grid.withCellProperties(GridPos(2, 0), occupiedProps)
            
            val start = GridPos(0, 0)
            val destination = GridPos(4, 0)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // Path should go around the occupied cell
            success.path.none { it == GridPos(2, 0) } shouldBe true
        }
        
        test("occupied destination is allowed") {
            var grid = createEmptyGrid(10, 10)
            val occupiedProps = dev.questweaver.domain.map.geometry.CellProperties(occupiedBy = 123L)
            
            // Place occupied cell at destination (for attack movement)
            val destination = GridPos(3, 0)
            grid = grid.withCellProperties(destination, occupiedProps)
            
            val start = GridPos(0, 0)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // Path should reach the occupied destination
            success.path.last() shouldBe destination
        }
        
        test("pathfinding around occupied cells") {
            var grid = createEmptyGrid(10, 10)
            val occupiedProps = dev.questweaver.domain.map.geometry.CellProperties(occupiedBy = 123L)
            
            // Create a line of occupied cells forcing path to go around
            for (y in 0..3) {
                grid = grid.withCellProperties(GridPos(2, y), occupiedProps)
            }
            
            val start = GridPos(0, 2)
            val destination = GridPos(4, 2)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            // Path should go around the occupied cells
            success.path.none { it.x == 2 && it.y in 0..3 } shouldBe true
        }
    }
    
    context("movement budget") {
        
        test("path within budget returns Success") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(0, 0)
            val destination = GridPos(3, 0)
            val budget = 10 // More than enough
            
            val result = pathfinder.findPath(start, destination, grid, maxCost = budget)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            success.totalCost shouldBe 3
        }
        
        test("path exceeding budget returns NoPathFound when budget too restrictive") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(0, 0)
            val destination = GridPos(5, 0)
            val budget = 2 // Way too low (needs 5)
            
            val result = pathfinder.findPath(start, destination, grid, maxCost = budget)
            
            // When budget is too restrictive, pathfinder can't find any path
            result.shouldBeInstanceOf<PathResult.NoPathFound>()
        }
        
        test("path at exact budget boundary") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(0, 0)
            val destination = GridPos(3, 0)
            val budget = 3 // Exactly enough
            
            val result = pathfinder.findPath(start, destination, grid, maxCost = budget)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            success.totalCost shouldBe 3
        }
        
        test("reachable positions all within budget") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(5, 5)
            val budget = 3
            val calculator = ReachabilityCalculator(pathfinder)
            
            val reachable = calculator.findReachablePositions(start, budget, grid)
            
            // Verify all reachable positions are actually within budget
            reachable.forEach { pos ->
                val result = pathfinder.findPath(start, pos, grid)
                result.shouldBeInstanceOf<PathResult.Success>()
                val success = result as PathResult.Success
                success.totalCost shouldBeLessThanOrEqualTo budget
            }
        }
        
        test("reachable positions exclude those beyond budget") {
            val grid = createEmptyGrid(10, 10)
            val start = GridPos(5, 5)
            val budget = 2
            val calculator = ReachabilityCalculator(pathfinder)
            
            val reachable = calculator.findReachablePositions(start, budget, grid)
            
            // Position (8, 5) is 3 cells away, should not be reachable with budget 2
            reachable shouldContain start
            reachable.contains(GridPos(8, 5)) shouldBe false
        }
    }
    
    context("property-based tests for determinism") {
        
        test("same inputs produce identical paths across multiple runs") {
            val grid = createEmptyGrid(20, 20)
            val start = GridPos(0, 0)
            val destination = GridPos(15, 15)
            
            // Run pathfinding multiple times
            val results = (1..10).map {
                pathfinder.findPath(start, destination, grid)
            }
            
            // All results should be identical
            results.forEach { result ->
                result.shouldBeInstanceOf<PathResult.Success>()
            }
            
            val firstPath = (results[0] as PathResult.Success).path
            val firstCost = (results[0] as PathResult.Success).totalCost
            
            results.drop(1).forEach { result ->
                val success = result as PathResult.Success
                success.path shouldBe firstPath
                success.totalCost shouldBe firstCost
            }
        }
        
        test("path is always connected with adjacent steps") {
            val grid = createEmptyGrid(20, 20)
            val testCases = listOf(
                GridPos(0, 0) to GridPos(10, 10),
                GridPos(5, 5) to GridPos(15, 2),
                GridPos(0, 19) to GridPos(19, 0),
                GridPos(10, 10) to GridPos(10, 15)
            )
            
            testCases.forEach { (start, destination) ->
                val result = pathfinder.findPath(start, destination, grid)
                
                if (result is PathResult.Success) {
                    val path = result.path
                    
                    // Verify each step is adjacent to the next
                    for (i in 0 until path.size - 1) {
                        val current = path[i]
                        val next = path[i + 1]
                        
                        // Check that next is in current's neighbors
                        current.neighbors() shouldContain next
                    }
                }
            }
        }
        
        test("path cost matches sum of cell costs") {
            var grid = createEmptyGrid(20, 20)
            val difficultProps = dev.questweaver.domain.map.geometry.CellProperties(
                terrainType = TerrainType.DIFFICULT
            )
            
            // Add some difficult terrain
            for (x in 5..10) {
                for (y in 5..10) {
                    grid = grid.withCellProperties(GridPos(x, y), difficultProps)
                }
            }
            
            val testCases = listOf(
                GridPos(0, 0) to GridPos(15, 15),
                GridPos(3, 3) to GridPos(12, 12),
                GridPos(0, 10) to GridPos(19, 10)
            )
            
            testCases.forEach { (start, destination) ->
                val result = pathfinder.findPath(start, destination, grid)
                
                if (result is PathResult.Success) {
                    val path = result.path
                    val reportedCost = result.totalCost
                    
                    // Calculate cost manually
                    val calculatedCost = PathValidator.calculatePathCost(path, grid)
                    
                    calculatedCost shouldBe reportedCost
                }
            }
        }
        
        test("all reachable positions are within budget") {
            val grid = createEmptyGrid(20, 20)
            val calculator = ReachabilityCalculator(pathfinder)
            
            val testCases = listOf(
                GridPos(10, 10) to 5,
                GridPos(0, 0) to 10,
                GridPos(15, 15) to 8
            )
            
            testCases.forEach { (start, budget) ->
                val reachable = calculator.findReachablePositions(start, budget, grid)
                
                // Verify every reachable position is actually within budget
                reachable.forEach { pos ->
                    val result = pathfinder.findPath(start, pos, grid)
                    result.shouldBeInstanceOf<PathResult.Success>()
                    val success = result as PathResult.Success
                    success.totalCost shouldBeLessThanOrEqualTo budget
                }
            }
        }
        
        test("PathNode comparison is transitive and consistent") {
            // Create test nodes with various scores
            val node1 = PathNode(GridPos(0, 0), gScore = 5, fScore = 10)
            val node2 = PathNode(GridPos(1, 1), gScore = 5, fScore = 10)
            
            // Test consistency: comparing same nodes multiple times
            (node1.compareTo(node2) == node1.compareTo(node2)) shouldBe true
            
            // Test transitivity: if a < b and b < c, then a < c
            val a = PathNode(GridPos(0, 0), gScore = 5, fScore = 10)
            val b = PathNode(GridPos(1, 1), gScore = 5, fScore = 11)
            val c = PathNode(GridPos(2, 2), gScore = 5, fScore = 12)
            
            (a < b) shouldBe true
            (b < c) shouldBe true
            (a < c) shouldBe true
            
            // Test tie-breaking by gScore
            val sameF1 = PathNode(GridPos(0, 0), gScore = 5, fScore = 10)
            val sameF2 = PathNode(GridPos(1, 1), gScore = 6, fScore = 10)
            (sameF1 < sameF2) shouldBe true
            
            // Test tie-breaking by position
            val sameG1 = PathNode(GridPos(0, 0), gScore = 5, fScore = 10)
            val sameG2 = PathNode(GridPos(1, 0), gScore = 5, fScore = 10)
            (sameG1 < sameG2) shouldBe true
        }
    }
})

/**
 * Creates an empty grid with all normal terrain and no obstacles.
 */
private fun createEmptyGrid(width: Int, height: Int): MapGrid {
    return MapGrid(width, height)
}
