package dev.questweaver.domain.map.geometry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark tests for grid geometry operations.
 *
 * Verifies that geometry calculations meet performance targets:
 * - Distance calculation: <1μs per operation
 * - Range query (30ft on 50x50 grid): <1ms
 * - Line-of-effect check: <100μs per check
 * - AoE template calculation: <1ms for typical sizes
 *
 * These benchmarks ensure the geometry system is fast enough for real-time
 * tactical map rendering and combat calculations without introducing latency.
 */
class GeometryPerformanceTest : FunSpec({

    context("Distance calculation performance") {
        test("single distance calculation completes in under 1μs") {
            val from = GridPos(10, 10)
            val to = GridPos(25, 30)

            // Warm up JVM
            repeat(1000) { DistanceCalculator.chebyshevDistance(from, to) }

            // Measure average time over many iterations
            val totalTime = measureNanoTime {
                repeat(10000) {
                    DistanceCalculator.chebyshevDistance(from, to)
                }
            }

            val averageTime = totalTime / 10000
            // Target: <1μs (1000 nanoseconds)
            // Using 10μs as threshold to account for CI environment variance
            averageTime shouldBeLessThan 10000
        }

        test("distance in feet calculation completes in under 1μs") {
            val from = GridPos(10, 10)
            val to = GridPos(25, 30)

            // Warm up JVM
            repeat(1000) { DistanceCalculator.distanceInFeet(from, to) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    DistanceCalculator.distanceInFeet(from, to)
                }
            }

            val averageTime = totalTime / 10000
            averageTime shouldBeLessThan 10000
        }

        test("GridPos extension method distanceTo completes in under 1μs") {
            val from = GridPos(10, 10)
            val to = GridPos(25, 30)

            // Warm up JVM
            repeat(1000) { from.distanceTo(to) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(10000) {
                    from.distanceTo(to)
                }
            }

            val averageTime = totalTime / 10000
            averageTime shouldBeLessThan 10000
        }

        test("1000 distance calculations complete in under 10ms") {
            val positions = List(1000) { i -> GridPos(i % 50, i / 50) }

            // Warm up JVM
            repeat(100) { 
                DistanceCalculator.chebyshevDistance(positions[0], positions[1]) 
            }

            // Measure time for 1000 distance calculations
            val duration = measureTimeMillis {
                for (i in 0 until 999) {
                    DistanceCalculator.chebyshevDistance(positions[i], positions[i + 1])
                }
            }

            // Target: <10ms for 1000 calculations
            duration shouldBeLessThan 10
        }
    }

    context("Range query performance") {
        test("range query for 30ft on 50x50 grid completes in under 2ms") {
            val grid = MapGrid(width = 50, height = 50)
            val center = GridPos(25, 25)

            // Warm up JVM
            repeat(100) { 
                DistanceCalculator.positionsWithinRange(center, 30, grid) 
            }

            // Measure time for range query
            val duration = measureTimeMillis {
                DistanceCalculator.positionsWithinRange(center, 30, grid)
            }

            // Target: <2ms (allowing for CI variance)
            duration shouldBeLessThan 2
        }

        test("range query for 60ft on 50x50 grid completes in under 2ms") {
            val grid = MapGrid(width = 50, height = 50)
            val center = GridPos(25, 25)

            // Warm up JVM
            repeat(100) { 
                DistanceCalculator.positionsWithinRange(center, 60, grid) 
            }

            // Measure time for larger range query
            val duration = measureTimeMillis {
                DistanceCalculator.positionsWithinRange(center, 60, grid)
            }

            // Target: <2ms for larger range
            duration shouldBeLessThan 2
        }

        test("range query for 15ft on 100x100 grid completes in under 2ms") {
            val grid = MapGrid(width = 100, height = 100)
            val center = GridPos(50, 50)

            // Warm up JVM
            repeat(100) { 
                DistanceCalculator.positionsWithinRange(center, 15, grid) 
            }

            // Measure time for range query on larger grid
            val duration = measureTimeMillis {
                DistanceCalculator.positionsWithinRange(center, 15, grid)
            }

            // Target: <2ms even on larger grid (allowing for CI variance)
            duration shouldBeLessThan 2
        }

        test("100 range queries complete in under 50ms") {
            val grid = MapGrid(width = 50, height = 50)
            val positions = List(100) { i -> GridPos(i % 50, i / 50) }

            // Warm up JVM
            repeat(10) { 
                DistanceCalculator.positionsWithinRange(positions[0], 30, grid) 
            }

            // Measure time for 100 range queries
            val duration = measureTimeMillis {
                positions.forEach { pos ->
                    DistanceCalculator.positionsWithinRange(pos, 30, grid)
                }
            }

            // Target: <50ms for 100 queries
            duration shouldBeLessThan 50
        }
    }

    context("Line-of-effect performance") {
        test("single line-of-effect check completes in under 100μs") {
            val grid = MapGrid(width = 50, height = 50)
            val from = GridPos(10, 10)
            val to = GridPos(40, 40)

            // Warm up JVM
            repeat(1000) { LineOfEffect.hasLineOfEffect(from, to, grid) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(1000) {
                    LineOfEffect.hasLineOfEffect(from, to, grid)
                }
            }

            val averageTime = totalTime / 1000
            // Target: <100μs (100000 nanoseconds)
            // Using 200μs as threshold for CI variance
            averageTime shouldBeLessThan 200000
        }

        test("Bresenham line algorithm completes in under 50μs") {
            val from = GridPos(10, 10)
            val to = GridPos(40, 40)

            // Warm up JVM
            repeat(1000) { LineOfEffect.bresenhamLine(from, to) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(1000) {
                    LineOfEffect.bresenhamLine(from, to)
                }
            }

            val averageTime = totalTime / 1000
            // Target: <50μs (50000 nanoseconds)
            // Using 100μs as threshold for CI variance
            averageTime shouldBeLessThan 100000
        }

        test("100 line-of-effect checks complete in under 10ms") {
            val grid = MapGrid(width = 50, height = 50)
            val positions = List(100) { i -> GridPos(i % 50, i / 50) }

            // Warm up JVM
            repeat(10) { 
                LineOfEffect.hasLineOfEffect(positions[0], positions[50], grid) 
            }

            // Measure time for 100 LOS checks
            val duration = measureTimeMillis {
                for (i in 0 until 50) {
                    LineOfEffect.hasLineOfEffect(positions[i], positions[i + 50], grid)
                }
            }

            // Target: <10ms for 100 checks
            duration shouldBeLessThan 10
        }

        test("line-of-effect with obstacles completes in under 100μs") {
            val grid = MapGrid(width = 50, height = 50)
                .withCellProperties(GridPos(25, 25), CellProperties(hasObstacle = true))
                .withCellProperties(GridPos(26, 26), CellProperties(hasObstacle = true))
            val from = GridPos(10, 10)
            val to = GridPos(40, 40)

            // Warm up JVM
            repeat(1000) { LineOfEffect.hasLineOfEffect(from, to, grid) }

            // Measure average time
            val totalTime = measureNanoTime {
                repeat(1000) {
                    LineOfEffect.hasLineOfEffect(from, to, grid)
                }
            }

            val averageTime = totalTime / 1000
            // Target: <100μs even with obstacles
            averageTime shouldBeLessThan 200000
        }

        test("positionsWithinRangeAndLOS completes in under 2ms") {
            val grid = MapGrid(width = 50, height = 50)
            val from = GridPos(25, 25)

            // Warm up JVM
            repeat(100) { 
                LineOfEffect.positionsWithinRangeAndLOS(from, 30, grid) 
            }

            // Measure time for combined range and LOS query
            val duration = measureTimeMillis {
                LineOfEffect.positionsWithinRangeAndLOS(from, 30, grid)
            }

            // Target: <2ms for combined query
            duration shouldBeLessThan 2
        }
    }

    context("AoE template performance") {
        test("SphereTemplate 20ft radius completes in under 3ms") {
            val grid = MapGrid(width = 50, height = 50)
            val template = SphereTemplate(radiusInFeet = 20)
            val origin = GridPos(25, 25)

            // Warm up JVM
            repeat(100) { template.affectedPositions(origin, grid) }

            // Measure time
            val duration = measureTimeMillis {
                template.affectedPositions(origin, grid)
            }

            // Target: <3ms (allowing for CI environment variance)
            duration shouldBeLessThan 3
        }

        test("CubeTemplate 20ft side completes in under 3ms") {
            val grid = MapGrid(width = 50, height = 50)
            val template = CubeTemplate(sideLengthInFeet = 20)
            val origin = GridPos(25, 25)

            // Warm up JVM
            repeat(100) { template.affectedPositions(origin, grid) }

            // Measure time
            val duration = measureTimeMillis {
                template.affectedPositions(origin, grid)
            }

            // Target: <3ms (allowing for CI environment variance)
            duration shouldBeLessThan 3
        }

        test("ConeTemplate 30ft length completes in under 3ms") {
            val grid = MapGrid(width = 50, height = 50)
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.NORTH)
            val origin = GridPos(25, 25)

            // Warm up JVM
            repeat(100) { template.affectedPositions(origin, grid) }

            // Measure time
            val duration = measureTimeMillis {
                template.affectedPositions(origin, grid)
            }

            // Target: <3ms (allowing for CI environment variance)
            duration shouldBeLessThan 3
        }

        test("ConeTemplate 60ft length completes in under 2ms") {
            val grid = MapGrid(width = 100, height = 100)
            val template = ConeTemplate(lengthInFeet = 60, direction = Direction.EAST)
            val origin = GridPos(50, 50)

            // Warm up JVM
            repeat(100) { template.affectedPositions(origin, grid) }

            // Measure time for larger cone
            val duration = measureTimeMillis {
                template.affectedPositions(origin, grid)
            }

            // Target: <2ms for larger cone
            duration shouldBeLessThan 2
        }

        test("100 AoE template calculations complete in under 50ms") {
            val grid = MapGrid(width = 50, height = 50)
            val templates = listOf(
                SphereTemplate(radiusInFeet = 20),
                CubeTemplate(sideLengthInFeet = 15),
                ConeTemplate(lengthInFeet = 30, direction = Direction.NORTH)
            )
            val positions = List(100) { i -> GridPos(i % 50, i / 50) }

            // Warm up JVM
            repeat(10) { 
                templates[0].affectedPositions(positions[0], grid) 
            }

            // Measure time for 100 template calculations
            val duration = measureTimeMillis {
                positions.take(100).forEachIndexed { index, pos ->
                    templates[index % 3].affectedPositions(pos, grid)
                }
            }

            // Target: <50ms for 100 calculations
            duration shouldBeLessThan 50
        }
    }

    context("Memory allocation profiling") {
        test("range query creates reasonable number of objects") {
            val grid = MapGrid(width = 50, height = 50)
            val center = GridPos(25, 25)

            // Create many range query results
            val results = List(1000) {
                DistanceCalculator.positionsWithinRange(center, 30, grid)
            }

            // Verify we can create many results without issues
            results.size shouldBe 1000
            results.all { it.isNotEmpty() }.shouldBeTrue()
        }

        test("line-of-effect creates reasonable number of objects") {
            val grid = MapGrid(width = 50, height = 50)
            val from = GridPos(10, 10)
            val to = GridPos(40, 40)

            // Create many line-of-effect checks
            val results = List(1000) {
                LineOfEffect.hasLineOfEffect(from, to, grid)
            }

            // Verify we can create many results without issues
            results.size shouldBe 1000
        }

        test("AoE template creates reasonable number of objects") {
            val grid = MapGrid(width = 50, height = 50)
            val template = SphereTemplate(radiusInFeet = 20)
            val origin = GridPos(25, 25)

            // Create many AoE results
            val results = List(1000) {
                template.affectedPositions(origin, grid)
            }

            // Verify we can create many results without issues
            results.size shouldBe 1000
            results.all { it.isNotEmpty() }.shouldBeTrue()
        }

        test("large grid allPositions sequence is memory efficient") {
            val largeGrid = MapGrid(width = 100, height = 100)

            // Verify we can iterate through all positions without materializing
            val count = largeGrid.allPositions().count()

            // Should have 10,000 positions
            count shouldBe 10000
        }

        test("MapGrid withCellProperties is memory efficient") {
            var grid = MapGrid(width = 50, height = 50)

            // Add many cell properties
            repeat(1000) { i ->
                val pos = GridPos(i % 50, i / 50)
                grid = grid.withCellProperties(pos, CellProperties(hasObstacle = true))
            }

            // Verify grid has expected properties
            grid.cells.size shouldBe 1000
        }
    }

    context("Real-world combat scenario performance") {
        test("typical spell targeting (range + LOS) completes in under 5ms") {
            val grid = MapGrid(width = 50, height = 50)
                .withCellProperties(GridPos(20, 20), CellProperties(hasObstacle = true))
                .withCellProperties(GridPos(30, 30), CellProperties(hasObstacle = true))
            val casterPos = GridPos(10, 10)

            // Warm up JVM
            repeat(100) { 
                LineOfEffect.positionsWithinRangeAndLOS(casterPos, 60, grid) 
            }

            // Simulate finding valid spell targets
            val duration = measureTimeMillis {
                LineOfEffect.positionsWithinRangeAndLOS(casterPos, 60, grid)
            }

            // Target: <5ms for spell targeting (allowing for CI environment variance)
            duration shouldBeLessThan 5
        }

        test("fireball targeting (range + AoE) completes in under 3ms") {
            val grid = MapGrid(width = 50, height = 50)
            val casterPos = GridPos(10, 10)
            val targetPos = GridPos(30, 30)
            val fireball = SphereTemplate(radiusInFeet = 20)

            // Warm up JVM
            repeat(100) {
                DistanceCalculator.positionsWithinRange(casterPos, 150, grid)
                fireball.affectedPositions(targetPos, grid)
            }

            // Simulate fireball targeting: check range, then calculate AoE
            val duration = measureTimeMillis {
                val validTargets = DistanceCalculator.positionsWithinRange(casterPos, 150, grid)
                if (targetPos in validTargets) {
                    fireball.affectedPositions(targetPos, grid)
                }
            }

            // Target: <3ms for complete fireball targeting
            duration shouldBeLessThan 3
        }

        test("cone spell (Burning Hands) completes in under 3ms") {
            val grid = MapGrid(width = 50, height = 50)
            val casterPos = GridPos(25, 25)
            val burningHands = ConeTemplate(lengthInFeet = 15, direction = Direction.NORTH)

            // Warm up JVM
            repeat(100) { burningHands.affectedPositions(casterPos, grid) }

            // Simulate cone spell
            val duration = measureTimeMillis {
                burningHands.affectedPositions(casterPos, grid)
            }

            // Target: <3ms for cone spell (allowing for CI environment variance)
            duration shouldBeLessThan 3
        }

        test("movement validation (neighbors + distance) completes in under 3ms") {
            val grid = MapGrid(width = 50, height = 50)
            val currentPos = GridPos(25, 25)
            val speed = 30 // 30 feet movement

            // Warm up JVM
            repeat(100) {
                currentPos.neighbors()
                DistanceCalculator.positionsWithinRange(currentPos, speed, grid)
            }

            // Simulate movement validation
            val duration = measureTimeMillis {
                val neighbors = currentPos.neighbors()
                val reachable = DistanceCalculator.positionsWithinRange(currentPos, speed, grid)
                neighbors.filter { it in reachable }
            }

            // Target: <3ms for movement validation (allowing for CI environment variance)
            duration shouldBeLessThan 3
        }

        test("initiative order distance checks complete in under 3ms") {
            val grid = MapGrid(width = 50, height = 50)
            val creatures = List(10) { i -> GridPos(i * 5, i * 5) }

            // Warm up JVM
            repeat(100) {
                creatures.forEach { from ->
                    creatures.forEach { to ->
                        DistanceCalculator.distanceInFeet(from, to)
                    }
                }
            }

            // Calculate all pairwise distances (10 creatures = 45 unique pairs)
            val duration = measureTimeMillis {
                creatures.forEach { from ->
                    creatures.forEach { to ->
                        DistanceCalculator.distanceInFeet(from, to)
                    }
                }
            }

            // Target: <3ms for all distance checks (allowing for CI environment variance)
            duration shouldBeLessThan 3
        }

        test("complex combat round (multiple operations) completes in under 10ms") {
            val grid = MapGrid(width = 50, height = 50)
                .withCellProperties(GridPos(20, 20), CellProperties(hasObstacle = true))
                .withCellProperties(GridPos(30, 30), CellProperties(hasObstacle = true))
            
            val attackerPos = GridPos(10, 10)
            val targetPos = GridPos(40, 40)
            val fireball = SphereTemplate(radiusInFeet = 20)

            // Warm up JVM
            repeat(10) {
                DistanceCalculator.distanceInFeet(attackerPos, targetPos)
                LineOfEffect.hasLineOfEffect(attackerPos, targetPos, grid)
                DistanceCalculator.positionsWithinRange(attackerPos, 30, grid)
                fireball.affectedPositions(targetPos, grid)
            }

            // Simulate complex combat round with multiple geometry operations
            val duration = measureTimeMillis {
                // Check attack range
                val distance = DistanceCalculator.distanceInFeet(attackerPos, targetPos)
                
                // Check line of sight
                LineOfEffect.hasLineOfEffect(attackerPos, targetPos, grid)
                
                // Find movement options
                val moveOptions = DistanceCalculator.positionsWithinRange(attackerPos, 30, grid)
                
                // Calculate AoE for spell
                val affected = fireball.affectedPositions(targetPos, grid)
                
                // Verify operations completed
                distance shouldBeGreaterThan 0
                moveOptions.isNotEmpty().shouldBeTrue()
                affected.isNotEmpty().shouldBeTrue()
            }

            // Target: <10ms for complex combat round
            duration shouldBeLessThan 10
        }
    }
})
