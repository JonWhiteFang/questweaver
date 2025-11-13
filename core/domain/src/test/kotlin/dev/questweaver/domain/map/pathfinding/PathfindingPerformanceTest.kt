package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.TerrainType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.system.measureTimeMillis

/**
 * Performance tests for pathfinding system.
 *
 * Tests verify that pathfinding meets performance budgets:
 * - Pathfinding on 50x50 grid: <50ms (CI-friendly threshold, 5x local target)
 * - Reachability calculation with 30ft movement: <25ms (CI-friendly)
 * - Path validation: <5ms (CI-friendly)
 * - Performance scales appropriately with grid size
 *
 * Note: Thresholds are set conservatively for CI environments where:
 * - Shared resources and variable hardware affect performance
 * - Cold JVM and less optimized JIT compilation
 * - Background processes consume resources
 *
 * Local development typically sees 5-10x better performance.
 */
class PathfindingPerformanceTest : FunSpec({
    
    val pathfinder = AStarPathfinder()
    
    // CI-friendly thresholds (5x local targets to account for shared resources)
    val pathfindingThreshold = 50L  // Local target: 10ms
    val reachabilityThreshold = 25L  // Local target: 5ms
    val validationThreshold = 5L     // Local target: 1ms
    
    context("pathfinding performance") {
        
        test("pathfinding on 50x50 grid completes within threshold") {
            val grid = createEmptyGrid(50, 50)
            val start = GridPos(0, 0)
            val destination = GridPos(49, 49)
            
            // Warm up
            repeat(10) {
                pathfinder.findPath(start, destination, grid)
            }
            
            // Measure performance (average of 3 runs for stability)
            val durations = (1..3).map {
                measureTimeMillis {
                    pathfinder.findPath(start, destination, grid)
                }
            }
            val avgDuration = durations.average().toLong()
            
            avgDuration shouldBeLessThan pathfindingThreshold
        }
        
        test("pathfinding with obstacles on 50x50 grid completes within threshold") {
            var grid = createEmptyGrid(50, 50)
            val obstacleProps = dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
            
            // Add some obstacles to make pathfinding more complex
            for (i in 10..40 step 5) {
                for (j in 10..40 step 5) {
                    grid = grid.withCellProperties(GridPos(i, j), obstacleProps)
                }
            }
            
            val start = GridPos(0, 0)
            val destination = GridPos(49, 49)
            
            // Warm up
            repeat(10) {
                pathfinder.findPath(start, destination, grid)
            }
            
            // Measure performance (average of 3 runs)
            val durations = (1..3).map {
                measureTimeMillis {
                    pathfinder.findPath(start, destination, grid)
                }
            }
            val avgDuration = durations.average().toLong()
            
            avgDuration shouldBeLessThan pathfindingThreshold
        }
        
        test("pathfinding with difficult terrain on 50x50 grid completes within threshold") {
            var grid = createEmptyGrid(50, 50)
            val difficultProps = dev.questweaver.domain.map.geometry.CellProperties(
                terrainType = TerrainType.DIFFICULT
            )
            
            // Add difficult terrain patches
            for (x in 15..35) {
                for (y in 15..35) {
                    grid = grid.withCellProperties(GridPos(x, y), difficultProps)
                }
            }
            
            val start = GridPos(0, 0)
            val destination = GridPos(49, 49)
            
            // Warm up more thoroughly
            repeat(10) {
                pathfinder.findPath(start, destination, grid)
            }
            
            // Measure performance (average of 3 runs)
            val durations = (1..3).map {
                measureTimeMillis {
                    pathfinder.findPath(start, destination, grid)
                }
            }
            val avgDuration = durations.average().toLong()
            
            avgDuration shouldBeLessThan pathfindingThreshold
        }
    }
    
    context("reachability calculation performance") {
        
        test("reachability calculation with 30ft movement completes within threshold") {
            val grid = createEmptyGrid(50, 50)
            val start = GridPos(25, 25)
            val movementBudget = 6 // 30ft = 6 squares in D&D 5e
            val calculator = ReachabilityCalculator(pathfinder)
            
            // Warm up
            repeat(10) {
                calculator.findReachablePositions(start, movementBudget, grid)
            }
            
            // Measure performance (average of 3 runs)
            val durations = (1..3).map {
                measureTimeMillis {
                    calculator.findReachablePositions(start, movementBudget, grid)
                }
            }
            val avgDuration = durations.average().toLong()
            
            avgDuration shouldBeLessThan reachabilityThreshold
        }
        
        test("reachability calculation with obstacles completes within threshold") {
            var grid = createEmptyGrid(50, 50)
            val obstacleProps = dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
            
            // Add some obstacles
            for (i in 20..30 step 2) {
                grid = grid.withCellProperties(GridPos(i, 25), obstacleProps)
            }
            
            val start = GridPos(25, 25)
            val movementBudget = 6
            val calculator = ReachabilityCalculator(pathfinder)
            
            // Warm up
            repeat(10) {
                calculator.findReachablePositions(start, movementBudget, grid)
            }
            
            // Measure performance (average of 3 runs)
            val durations = (1..3).map {
                measureTimeMillis {
                    calculator.findReachablePositions(start, movementBudget, grid)
                }
            }
            val avgDuration = durations.average().toLong()
            
            avgDuration shouldBeLessThan reachabilityThreshold
        }
    }
    
    context("path validation performance") {
        
        test("path validation completes within threshold") {
            val grid = createEmptyGrid(50, 50)
            val start = GridPos(0, 0)
            val destination = GridPos(20, 20)
            
            val result = pathfinder.findPath(start, destination, grid)
            result.shouldBeInstanceOf<PathResult.Success>()
            val path = (result as PathResult.Success).path
            
            // Warm up
            repeat(20) {
                PathValidator.isValidPath(path, grid)
            }
            
            // Measure performance (average of 5 runs for very fast operations)
            val durations = (1..5).map {
                measureTimeMillis {
                    repeat(10) { PathValidator.isValidPath(path, grid) }
                }
            }
            val avgDuration = (durations.average() / 10).toLong()
            
            avgDuration shouldBeLessThan validationThreshold
        }
        
        test("path cost calculation completes within threshold") {
            val grid = createEmptyGrid(50, 50)
            val start = GridPos(0, 0)
            val destination = GridPos(20, 20)
            
            val result = pathfinder.findPath(start, destination, grid)
            result.shouldBeInstanceOf<PathResult.Success>()
            val path = (result as PathResult.Success).path
            
            // Warm up
            repeat(20) {
                PathValidator.calculatePathCost(path, grid)
            }
            
            // Measure performance (average of 5 runs for very fast operations)
            val durations = (1..5).map {
                measureTimeMillis {
                    repeat(10) { PathValidator.calculatePathCost(path, grid) }
                }
            }
            val avgDuration = (durations.average() / 10).toLong()
            
            avgDuration shouldBeLessThan validationThreshold
        }
        
        test("budget validation completes within threshold") {
            val grid = createEmptyGrid(50, 50)
            val start = GridPos(0, 0)
            val destination = GridPos(20, 20)
            
            val result = pathfinder.findPath(start, destination, grid)
            result.shouldBeInstanceOf<PathResult.Success>()
            val path = (result as PathResult.Success).path
            
            // Warm up
            repeat(20) {
                PathValidator.isWithinBudget(path, 100, grid)
            }
            
            // Measure performance (average of 5 runs for very fast operations)
            val durations = (1..5).map {
                measureTimeMillis {
                    repeat(10) { PathValidator.isWithinBudget(path, 100, grid) }
                }
            }
            val avgDuration = (durations.average() / 10).toLong()
            
            avgDuration shouldBeLessThan validationThreshold
        }
    }
    
    context("performance scaling") {
        
        test("performance scales appropriately with grid size") {
            val gridSizes = listOf(10, 20, 30, 40, 50)
            val timings = mutableListOf<Long>()
            
            for (size in gridSizes) {
                val grid = createEmptyGrid(size, size)
                val start = GridPos(0, 0)
                val destination = GridPos(size - 1, size - 1)
                
                // Warm up
                repeat(3) {
                    pathfinder.findPath(start, destination, grid)
                }
                
                // Measure
                val duration = measureTimeMillis {
                    repeat(5) {
                        pathfinder.findPath(start, destination, grid)
                    }
                }
                
                timings.add(duration / 5)
            }
            
            // Verify 50x50 is still under budget
            timings.last() shouldBeLessThan pathfindingThreshold
            
            // Log timings for analysis (optional)
            println("Performance scaling:")
            gridSizes.zip(timings).forEach { (size, time) ->
                println("  ${size}x${size}: ${time}ms")
            }
        }
        
        test("performance with varying obstacle density") {
            val grid = createEmptyGrid(50, 50)
            val obstacleDensities = listOf(0.0, 0.1, 0.2, 0.3)
            val timings = mutableListOf<Long>()
            
            for (density in obstacleDensities) {
                var testGrid = grid
                val obstacleProps = dev.questweaver.domain.map.geometry.CellProperties(hasObstacle = true)
                
                // Add obstacles based on density
                val totalCells = 50 * 50
                val obstacleCells = (totalCells * density).toInt()
                var added = 0
                
                for (x in 0 until 50) {
                    for (y in 0 until 50) {
                        if (added >= obstacleCells) break
                        if ((x + y) % (1.0 / density).toInt() == 0) {
                            testGrid = testGrid.withCellProperties(GridPos(x, y), obstacleProps)
                            added++
                        }
                    }
                    if (added >= obstacleCells) break
                }
                
                val start = GridPos(0, 0)
                val destination = GridPos(49, 49)
                
                // Warm up
                repeat(3) {
                    pathfinder.findPath(start, destination, testGrid)
                }
                
                // Measure
                val duration = measureTimeMillis {
                    repeat(5) {
                        pathfinder.findPath(start, destination, testGrid)
                    }
                }
                
                timings.add(duration / 5)
            }
            
            // All should be under budget
            timings.forEach { time ->
                time shouldBeLessThan pathfindingThreshold
            }
            
            // Log timings for analysis (optional)
            println("Performance with obstacle density:")
            obstacleDensities.zip(timings).forEach { (density, time) ->
                println("  ${(density * 100).toInt()}% obstacles: ${time}ms")
            }
        }
    }
})

/**
 * Creates an empty grid with all normal terrain and no obstacles.
 */
private fun createEmptyGrid(width: Int, height: Int): MapGrid {
    return MapGrid(width, height)
}
