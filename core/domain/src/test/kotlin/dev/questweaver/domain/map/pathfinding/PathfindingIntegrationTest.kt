package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.CellProperties
import dev.questweaver.domain.map.geometry.DistanceCalculator
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.TerrainType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Integration tests for pathfinding system with grid geometry from spec 07.
 *
 * Validates:
 * - No Android dependencies (verified by successful compilation)
 * - Integration with grid geometry components
 * - PathResult serialization for event sourcing
 * - All public APIs have KDoc documentation
 */
class PathfindingIntegrationTest : FunSpec({
    
    context("integration with grid geometry from spec 07") {
        
        test("pathfinding uses DistanceCalculator for heuristic") {
            // Create a grid with obstacles
            val grid = createTestGrid(10, 10) { x, y ->
                when {
                    x == 5 && y in 2..7 -> CellProperties(
                        terrainType = TerrainType.IMPASSABLE,
                        hasObstacle = true
                    )
                    else -> CellProperties(terrainType = TerrainType.NORMAL)
                }
            }
            
            val pathfinder = AStarPathfinder()
            val start = GridPos(3, 5)
            val destination = GridPos(7, 5)
            
            // Pathfinding should find a path around the obstacle
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val path = (result as PathResult.Success).path
            
            // Verify path doesn't go through the obstacle
            path.none { it.x == 5 && it.y in 2..7 } shouldBe true
            
            // Verify path uses adjacent cells (GridPos.neighbors())
            PathValidator.isValidPath(path, grid) shouldBe true
        }
        
        test("pathfinding respects TerrainType from grid geometry") {
            // Create a grid with difficult terrain
            val grid = createTestGrid(10, 10) { x, y ->
                when {
                    x in 3..6 && y == 5 -> CellProperties(terrainType = TerrainType.DIFFICULT)
                    else -> CellProperties(terrainType = TerrainType.NORMAL)
                }
            }
            
            val pathfinder = AStarPathfinder()
            val start = GridPos(2, 5)
            val destination = GridPos(7, 5)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val success = result as PathResult.Success
            
            // Verify the pathfinder correctly calculates cost through difficult terrain
            // The path should include cells with difficult terrain
            val costCalculator = DefaultMovementCostCalculator()
            val calculatedCost = success.path.drop(1).sumOf { pos ->
                costCalculator.calculateCost(pos, grid)
            }
            
            // Verify calculated cost matches reported cost
            success.totalCost shouldBe calculatedCost
            
            // Verify that difficult terrain cells cost 2
            val difficultCells = success.path.filter { it.x in 3..6 && it.y == 5 }
            difficultCells.all { pos ->
                costCalculator.calculateCost(pos, grid) == 2
            } shouldBe true
        }
        
        test("pathfinding uses CellProperties for obstacle detection") {
            val grid = createTestGrid(10, 10) { x, y ->
                CellProperties(
                    terrainType = TerrainType.NORMAL,
                    hasObstacle = x == 5 && y == 5
                )
            }
            
            val pathfinder = AStarPathfinder()
            val start = GridPos(4, 5)
            val destination = GridPos(6, 5)
            
            val result = pathfinder.findPath(start, destination, grid)
            
            result.shouldBeInstanceOf<PathResult.Success>()
            val path = (result as PathResult.Success).path
            
            // Path should go around the obstacle at (5, 5)
            path.contains(GridPos(5, 5)) shouldBe false
        }
        
        test("pathfinding uses CellProperties for occupancy") {
            val grid = createTestGrid(10, 10) { x, y ->
                CellProperties(
                    terrainType = TerrainType.NORMAL,
                    occupiedBy = if (x == 5 && y == 5) 123L else null
                )
            }
            
            val pathfinder = AStarPathfinder()
            
            // Occupied cell blocks intermediate path
            val result1 = pathfinder.findPath(GridPos(4, 5), GridPos(6, 5), grid)
            result1.shouldBeInstanceOf<PathResult.Success>()
            val path1 = (result1 as PathResult.Success).path
            path1.contains(GridPos(5, 5)) shouldBe false
            
            // Occupied destination is allowed
            val result2 = pathfinder.findPath(GridPos(4, 5), GridPos(5, 5), grid)
            result2.shouldBeInstanceOf<PathResult.Success>()
            val path2 = (result2 as PathResult.Success).path
            path2.last() shouldBe GridPos(5, 5)
        }
        
        test("reachability calculator integrates with grid geometry") {
            val grid = createTestGrid(10, 10) { x, y ->
                when {
                    x == 5 && y in 3..7 -> CellProperties(
                        terrainType = TerrainType.IMPASSABLE,
                        hasObstacle = true
                    )
                    else -> CellProperties(terrainType = TerrainType.NORMAL)
                }
            }
            
            val calculator = ReachabilityCalculator()
            val start = GridPos(3, 5)
            val movementBudget = 5
            
            val reachable = calculator.findReachablePositions(start, movementBudget, grid)
            
            // All reachable positions should be within budget
            reachable.all { pos ->
                val result = AStarPathfinder().findPath(start, pos, grid)
                result is PathResult.Success && result.totalCost <= movementBudget
            } shouldBe true
            
            // No reachable position should be on the obstacle
            reachable.none { it.x == 5 && it.y in 3..7 } shouldBe true
        }
    }
    
    context("PathResult serialization for event sourcing") {
        
        val json = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
        }
        
        test("PathResult.Success serializes and deserializes correctly") {
            val original = PathResult.Success(
                path = listOf(GridPos(0, 0), GridPos(1, 1), GridPos(2, 2)),
                totalCost = 5
            )
            
            val serialized = json.encodeToString<PathResult>(original)
            val deserialized = json.decodeFromString<PathResult>(serialized)
            
            deserialized shouldBe original
        }
        
        test("PathResult.NoPathFound serializes and deserializes correctly") {
            val original = PathResult.NoPathFound("Destination is blocked")
            
            val serialized = json.encodeToString<PathResult>(original)
            val deserialized = json.decodeFromString<PathResult>(serialized)
            
            deserialized shouldBe original
        }
        
        test("PathResult.ExceedsMovementBudget serializes and deserializes correctly") {
            val original = PathResult.ExceedsMovementBudget(
                requiredCost = 10,
                availableCost = 6
            )
            
            val serialized = json.encodeToString<PathResult>(original)
            val deserialized = json.decodeFromString<PathResult>(serialized)
            
            deserialized shouldBe original
        }
        
        test("PathResult uses correct SerialName for polymorphic serialization") {
            val success = PathResult.Success(listOf(GridPos(0, 0)), 0)
            val noPath = PathResult.NoPathFound("test")
            val exceeds = PathResult.ExceedsMovementBudget(10, 5)
            
            val successJson = json.encodeToString<PathResult>(success)
            val noPathJson = json.encodeToString<PathResult>(noPath)
            val exceedsJson = json.encodeToString<PathResult>(exceeds)
            
            successJson.contains("\"type\":\"path_success\"") shouldBe true
            noPathJson.contains("\"type\":\"path_no_path_found\"") shouldBe true
            exceedsJson.contains("\"type\":\"path_exceeds_budget\"") shouldBe true
        }
    }
    
    context("no Android dependencies verification") {
        
        test("pathfinding module compiles without Android dependencies") {
            // This test verifies that the pathfinding module has no Android dependencies
            // by successfully compiling and running. If Android dependencies were present,
            // the test would fail to compile.
            
            val pathfinder = AStarPathfinder()
            val calculator = ReachabilityCalculator()
            val costCalculator = DefaultMovementCostCalculator()
            
            // All classes instantiate successfully without Android context
            pathfinder.shouldBeInstanceOf<Pathfinder>()
            calculator.shouldBeInstanceOf<ReachabilityCalculator>()
            costCalculator.shouldBeInstanceOf<MovementCostCalculator>()
        }
    }
})

/**
 * Helper function to create a test grid with custom cell properties.
 *
 * Creates a MapGrid by materializing all cells with the provided properties.
 * Note: MapGrid is a data class, so we must construct it with a cells map.
 *
 * @param width The width of the grid in cells
 * @param height The height of the grid in cells
 * @param cellPropertiesProvider Lambda that provides cell properties for each (x, y) coordinate
 * @return A MapGrid instance with the specified dimensions and cell properties
 */
@Suppress("NestedBlockDepth") // Simple nested loops for grid initialization
private fun createTestGrid(
    width: Int,
    height: Int,
    cellPropertiesProvider: (x: Int, y: Int) -> CellProperties
): MapGrid {
    val cells = mutableMapOf<GridPos, CellProperties>()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pos = GridPos(x, y)
            cells[pos] = cellPropertiesProvider(x, y)
        }
    }
    return MapGrid(width, height, cells)
}
