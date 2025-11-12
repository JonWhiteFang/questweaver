package dev.questweaver.domain.map.geometry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

/**
 * Property-based tests for grid geometry calculations.
 * 
 * These tests verify mathematical properties that should hold for all inputs:
 * - Distance symmetry
 * - Triangle inequality
 * - Range constraint satisfaction
 * - Bresenham line endpoints
 * - AoE template determinism
 * 
 * Requirements: 2.1, 2.2, 3.1, 5.6
 */
class GeometryPropertyTest : FunSpec({
    
    // Arbitrary generators for grid positions
    val gridPosArb = Arb.bind(
        Arb.int(-50..50),
        Arb.int(-50..50)
    ) { x, y -> GridPos(x, y) }
    
    val validGridPosArb = Arb.bind(
        Arb.int(0..49),
        Arb.int(0..49)
    ) { x, y -> GridPos(x, y) }
    
    context("Distance symmetry: distanceTo(a, b) == distanceTo(b, a)") {
        test("Chebyshev distance should be symmetric for all position pairs") {
            checkAll(gridPosArb, gridPosArb) { pos1, pos2 ->
                val distance1to2 = DistanceCalculator.chebyshevDistance(pos1, pos2)
                val distance2to1 = DistanceCalculator.chebyshevDistance(pos2, pos1)
                
                distance1to2 shouldBe distance2to1
            }
        }
        
        test("Distance in feet should be symmetric for all position pairs") {
            checkAll(gridPosArb, gridPosArb) { pos1, pos2 ->
                val distance1to2 = DistanceCalculator.distanceInFeet(pos1, pos2)
                val distance2to1 = DistanceCalculator.distanceInFeet(pos2, pos1)
                
                distance1to2 shouldBe distance2to1
            }
        }
        
        test("GridPos.distanceTo should be symmetric") {
            checkAll(gridPosArb, gridPosArb) { pos1, pos2 ->
                pos1.distanceTo(pos2) shouldBe pos2.distanceTo(pos1)
            }
        }
        
        test("GridPos.distanceToInFeet should be symmetric") {
            checkAll(gridPosArb, gridPosArb) { pos1, pos2 ->
                pos1.distanceToInFeet(pos2) shouldBe pos2.distanceToInFeet(pos1)
            }
        }
    }
    
    context("Triangle inequality: distanceTo(a, c) <= distanceTo(a, b) + distanceTo(b, c)") {
        test("Chebyshev distance should satisfy triangle inequality") {
            checkAll(gridPosArb, gridPosArb, gridPosArb) { a, b, c ->
                val distanceAC = DistanceCalculator.chebyshevDistance(a, c)
                val distanceAB = DistanceCalculator.chebyshevDistance(a, b)
                val distanceBC = DistanceCalculator.chebyshevDistance(b, c)
                
                distanceAC shouldBeLessThanOrEqual (distanceAB + distanceBC)
            }
        }
        
        test("Distance in feet should satisfy triangle inequality") {
            checkAll(gridPosArb, gridPosArb, gridPosArb) { a, b, c ->
                val distanceAC = DistanceCalculator.distanceInFeet(a, c)
                val distanceAB = DistanceCalculator.distanceInFeet(a, b)
                val distanceBC = DistanceCalculator.distanceInFeet(b, c)
                
                distanceAC shouldBeLessThanOrEqual (distanceAB + distanceBC)
            }
        }
        
        test("GridPos.distanceTo should satisfy triangle inequality") {
            checkAll(gridPosArb, gridPosArb, gridPosArb) { a, b, c ->
                val distanceAC = a.distanceTo(c)
                val distanceAB = a.distanceTo(b)
                val distanceBC = b.distanceTo(c)
                
                distanceAC shouldBeLessThanOrEqual (distanceAB + distanceBC)
            }
        }
    }
    
    context("All positions within range satisfy distance constraint") {
        val testGrid = MapGrid(50, 50)
        val rangeArb = Arb.int(1..12).map { it * 5 } // Multiples of 5 from 5 to 60
        
        test("All positions returned by positionsWithinRange should be within the specified range") {
            checkAll(validGridPosArb, rangeArb) { center, range ->
                val positions = DistanceCalculator.positionsWithinRange(center, range, testGrid)
                
                positions.forEach { pos ->
                    val distance = DistanceCalculator.distanceInFeet(center, pos)
                    distance shouldBeLessThanOrEqual range
                }
            }
        }
        
        test("All positions returned should be within grid bounds") {
            checkAll(validGridPosArb, rangeArb) { center, range ->
                val positions = DistanceCalculator.positionsWithinRange(center, range, testGrid)
                
                positions.forEach { pos ->
                    testGrid.isInBounds(pos) shouldBe true
                }
            }
        }
        
        test("Center position should always be included in range query") {
            checkAll(validGridPosArb, rangeArb) { center, range ->
                val positions = DistanceCalculator.positionsWithinRange(center, range, testGrid)
                
                positions shouldContain center
            }
        }
        
        test("GridPos.isWithinRange should be consistent with positionsWithinRange") {
            checkAll(validGridPosArb, validGridPosArb, rangeArb) { pos1, pos2, range ->
                val isWithinRange = pos1.isWithinRange(pos2, range)
                val distance = pos1.distanceToInFeet(pos2)
                
                isWithinRange shouldBe (distance <= range)
            }
        }
    }
    
    context("Bresenham line always includes start and end points") {
        test("Line should always start with the from position") {
            checkAll(gridPosArb, gridPosArb) { from, to ->
                val line = LineOfEffect.bresenhamLine(from, to)
                
                line.first() shouldBe from
            }
        }
        
        test("Line should always end with the to position") {
            checkAll(gridPosArb, gridPosArb) { from, to ->
                val line = LineOfEffect.bresenhamLine(from, to)
                
                line.last() shouldBe to
            }
        }
        
        test("Line should contain at least 1 position (when from == to)") {
            checkAll(gridPosArb) { pos ->
                val line = LineOfEffect.bresenhamLine(pos, pos)
                
                line.size shouldBe 1
                line.first() shouldBe pos
            }
        }
        
        test("Line should be continuous (each step moves by at most 1 in each direction)") {
            checkAll(gridPosArb, gridPosArb) { from, to ->
                val line = LineOfEffect.bresenhamLine(from, to)
                
                // Check each consecutive pair
                for (i in 0 until line.size - 1) {
                    val current = line[i]
                    val next = line[i + 1]
                    
                    val dx = kotlin.math.abs(next.x - current.x)
                    val dy = kotlin.math.abs(next.y - current.y)
                    
                    // Each step should move by at most 1 in each direction
                    dx shouldBeLessThanOrEqual 1
                    dy shouldBeLessThanOrEqual 1
                }
            }
        }
    }
    
    context("AoE templates are deterministic for same inputs") {
        val testGrid = MapGrid(50, 50)
        
        test("SphereTemplate should return identical results for same inputs") {
            checkAll(validGridPosArb, Arb.int(1..4)) { origin, radiusMultiplier ->
                val radius = radiusMultiplier * 5 // 5, 10, 15, or 20 feet
                val template = SphereTemplate(radiusInFeet = radius)
                
                val result1 = template.affectedPositions(origin, testGrid)
                val result2 = template.affectedPositions(origin, testGrid)
                
                result1 shouldBe result2
            }
        }
        
        test("CubeTemplate should return identical results for same inputs") {
            checkAll(validGridPosArb, Arb.int(1..4)) { origin, sideMultiplier ->
                val sideLength = sideMultiplier * 5 // 5, 10, 15, or 20 feet
                val template = CubeTemplate(sideLengthInFeet = sideLength)
                
                val result1 = template.affectedPositions(origin, testGrid)
                val result2 = template.affectedPositions(origin, testGrid)
                
                result1 shouldBe result2
            }
        }
        
        test("ConeTemplate should return identical results for same inputs") {
            checkAll(validGridPosArb, Arb.int(1..4)) { origin, lengthMultiplier ->
                val length = lengthMultiplier * 15 // 15, 30, 45, or 60 feet
                val direction = Direction.entries[origin.x % Direction.entries.size]
                val template = ConeTemplate(lengthInFeet = length, direction = direction)
                
                val result1 = template.affectedPositions(origin, testGrid)
                val result2 = template.affectedPositions(origin, testGrid)
                
                result1 shouldBe result2
            }
        }
        
        test("All AoE templates should only return in-bounds positions") {
            checkAll(validGridPosArb) { origin ->
                val sphere = SphereTemplate(radiusInFeet = 15)
                val cube = CubeTemplate(sideLengthInFeet = 15)
                val cone = ConeTemplate(lengthInFeet = 30, direction = Direction.NORTH)
                
                val spherePositions = sphere.affectedPositions(origin, testGrid)
                val cubePositions = cube.affectedPositions(origin, testGrid)
                val conePositions = cone.affectedPositions(origin, testGrid)
                
                spherePositions.forEach { pos -> testGrid.isInBounds(pos) shouldBe true }
                cubePositions.forEach { pos -> testGrid.isInBounds(pos) shouldBe true }
                conePositions.forEach { pos -> testGrid.isInBounds(pos) shouldBe true }
            }
        }
        
        test("SphereTemplate should be symmetric around origin") {
            checkAll(validGridPosArb, Arb.int(1..3)) { origin, radiusMultiplier ->
                val radius = radiusMultiplier * 5
                val template = SphereTemplate(radiusInFeet = radius)
                val affected = template.affectedPositions(origin, testGrid)
                
                // For each position in the sphere, check if its mirror is also included
                affected.forEach { pos ->
                    val dx = pos.x - origin.x
                    val dy = pos.y - origin.y
                    
                    // Check all 4 quadrants
                    val mirrors = listOf(
                        GridPos(origin.x + dx, origin.y + dy),
                        GridPos(origin.x - dx, origin.y + dy),
                        GridPos(origin.x + dx, origin.y - dy),
                        GridPos(origin.x - dx, origin.y - dy)
                    )
                    
                    mirrors.forEach { mirror ->
                        if (testGrid.isInBounds(mirror)) {
                            val distanceToMirror = DistanceCalculator.distanceInFeet(origin, mirror)
                            val distanceToPos = DistanceCalculator.distanceInFeet(origin, pos)
                            
                            // If distances are equal, both should be in or out
                            if (distanceToMirror == distanceToPos) {
                                (mirror in affected) shouldBe (pos in affected)
                            }
                        }
                    }
                }
            }
        }
    }
})
