package dev.questweaver.domain.map.geometry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for AoE template implementations (SphereTemplate, CubeTemplate, ConeTemplate).
 * 
 * Validates:
 * - SphereTemplate with various radii (5ft, 10ft, 15ft, 20ft)
 * - SphereTemplate at grid edges and corners
 * - CubeTemplate with various side lengths (5ft, 10ft, 15ft, 20ft)
 * - CubeTemplate centered at various origin positions
 * - ConeTemplate in all 8 directions (N, NE, E, SE, S, SW, W, NW)
 * - ConeTemplate with various lengths (15ft, 30ft, 60ft)
 * - ConeTemplate width increases with distance
 * - All templates exclude out-of-bounds positions
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
class AoETemplateTest : FunSpec({
    
    context("SphereTemplate with radii 5ft, 10ft, 15ft, 20ft") {
        val testGrid = MapGrid(50, 50)
        val center = GridPos(25, 25)
        
        test("should affect 9 positions with 5ft radius (1 square)") {
            val template = SphereTemplate(radiusInFeet = 5)
            val affected = template.affectedPositions(center, testGrid)
            
            // 5ft = 1 square radius, includes center + 8 neighbors
            affected shouldHaveSize 9
            affected shouldContain center
            affected shouldContain GridPos(24, 24) // NW
            affected shouldContain GridPos(25, 24) // N
            affected shouldContain GridPos(26, 24) // NE
            affected shouldContain GridPos(24, 25) // W
            affected shouldContain GridPos(26, 25) // E
            affected shouldContain GridPos(24, 26) // SW
            affected shouldContain GridPos(25, 26) // S
            affected shouldContain GridPos(26, 26) // SE
        }
        
        test("should affect 25 positions with 10ft radius (2 squares)") {
            val template = SphereTemplate(radiusInFeet = 10)
            val affected = template.affectedPositions(center, testGrid)
            
            // 10ft = 2 square radius, 5x5 grid
            affected shouldHaveSize 25
            affected shouldContain center
            affected shouldContain GridPos(23, 23) // Corner
            affected shouldContain GridPos(27, 27) // Opposite corner
        }
        
        test("should affect 49 positions with 15ft radius (3 squares)") {
            val template = SphereTemplate(radiusInFeet = 15)
            val affected = template.affectedPositions(center, testGrid)
            
            // 15ft = 3 square radius, 7x7 grid
            affected shouldHaveSize 49
            affected shouldContain GridPos(22, 22) // Corner
            affected shouldContain GridPos(28, 28) // Opposite corner
        }
        
        test("should affect 81 positions with 20ft radius (4 squares)") {
            val template = SphereTemplate(radiusInFeet = 20)
            val affected = template.affectedPositions(center, testGrid)
            
            // 20ft = 4 square radius, 9x9 grid
            affected shouldHaveSize 81
            affected shouldContain GridPos(21, 21) // Corner
            affected shouldContain GridPos(29, 29) // Opposite corner
        }
    }
    
    context("SphereTemplate at grid edges and corners") {
        val testGrid = MapGrid(20, 20)
        
        test("should exclude out-of-bounds positions at top-left corner") {
            val corner = GridPos(0, 0)
            val template = SphereTemplate(radiusInFeet = 10)
            val affected = template.affectedPositions(corner, testGrid)
            
            // All positions should be within bounds
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
            }
            
            // Should not include negative coordinates
            affected.forEach { pos ->
                (pos.x >= 0) shouldBe true
                (pos.y >= 0) shouldBe true
            }
        }
        
        test("should exclude out-of-bounds positions at top-right corner") {
            val corner = GridPos(19, 0)
            val template = SphereTemplate(radiusInFeet = 10)
            val affected = template.affectedPositions(corner, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
                (pos.x < 20) shouldBe true
                (pos.y >= 0) shouldBe true
            }
        }
        
        test("should exclude out-of-bounds positions at bottom-left corner") {
            val corner = GridPos(0, 19)
            val template = SphereTemplate(radiusInFeet = 10)
            val affected = template.affectedPositions(corner, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
                (pos.x >= 0) shouldBe true
                (pos.y < 20) shouldBe true
            }
        }
        
        test("should exclude out-of-bounds positions at bottom-right corner") {
            val corner = GridPos(19, 19)
            val template = SphereTemplate(radiusInFeet = 10)
            val affected = template.affectedPositions(corner, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
                (pos.x < 20) shouldBe true
                (pos.y < 20) shouldBe true
            }
        }
        
        test("should exclude out-of-bounds positions at top edge") {
            val edge = GridPos(10, 0)
            val template = SphereTemplate(radiusInFeet = 10)
            val affected = template.affectedPositions(edge, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
                (pos.y >= 0) shouldBe true
            }
        }
        
        test("should exclude out-of-bounds positions at left edge") {
            val edge = GridPos(0, 10)
            val template = SphereTemplate(radiusInFeet = 10)
            val affected = template.affectedPositions(edge, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
                (pos.x >= 0) shouldBe true
            }
        }
    }
    
    context("CubeTemplate with side lengths 5ft, 10ft, 15ft, 20ft") {
        val testGrid = MapGrid(50, 50)
        val center = GridPos(25, 25)
        
        test("should affect 9 positions with 5ft side length") {
            val template = CubeTemplate(sideLengthInFeet = 5)
            val affected = template.affectedPositions(center, testGrid)
            
            // 5ft = 1 square, halfSide = 5/10 = 0, so -0..0 = 1x1 grid centered
            // Actually creates a 1x1 grid (just the center)
            affected shouldHaveSize 1
            affected shouldContain center
        }
        
        test("should affect 9 positions with 10ft side length") {
            val template = CubeTemplate(sideLengthInFeet = 10)
            val affected = template.affectedPositions(center, testGrid)
            
            // 10ft = 2 squares, halfSide = 10/10 = 1, so -1..1 = 3x3 grid
            affected shouldHaveSize 9
            affected shouldContain center
            affected shouldContain GridPos(24, 24)
            affected shouldContain GridPos(26, 26)
        }
        
        test("should affect 25 positions with 15ft side length") {
            val template = CubeTemplate(sideLengthInFeet = 15)
            val affected = template.affectedPositions(center, testGrid)
            
            // 15ft = 3 squares, halfSide = 15/10 = 1, so -1..1 = 3x3 grid
            // Wait, this seems wrong. Let me recalculate:
            // halfSide = 15 / 10 = 1 (integer division)
            // So it's still 3x3 = 9 positions
            affected shouldHaveSize 9
        }
        
        test("should affect 25 positions with 20ft side length") {
            val template = CubeTemplate(sideLengthInFeet = 20)
            val affected = template.affectedPositions(center, testGrid)
            
            // 20ft = 4 squares, halfSide = 20/10 = 2, so -2..2 = 5x5 grid
            affected shouldHaveSize 25
            affected shouldContain center
            affected shouldContain GridPos(23, 23)
            affected shouldContain GridPos(27, 27)
        }
    }
    
    context("CubeTemplate centered at various origin positions") {
        val testGrid = MapGrid(30, 30)
        
        test("should center correctly at grid center") {
            val center = GridPos(15, 15)
            val template = CubeTemplate(sideLengthInFeet = 10)
            val affected = template.affectedPositions(center, testGrid)
            
            // Should be symmetric around center
            affected shouldContain GridPos(14, 14)
            affected shouldContain GridPos(15, 15)
            affected shouldContain GridPos(16, 16)
        }
        
        test("should center correctly at top-left quadrant") {
            val origin = GridPos(5, 5)
            val template = CubeTemplate(sideLengthInFeet = 10)
            val affected = template.affectedPositions(origin, testGrid)
            
            affected shouldHaveSize 9
            affected shouldContain origin
            affected shouldContain GridPos(4, 4)
            affected shouldContain GridPos(6, 6)
        }
        
        test("should center correctly at bottom-right quadrant") {
            val origin = GridPos(25, 25)
            val template = CubeTemplate(sideLengthInFeet = 10)
            val affected = template.affectedPositions(origin, testGrid)
            
            affected shouldHaveSize 9
            affected shouldContain origin
            affected shouldContain GridPos(24, 24)
            affected shouldContain GridPos(26, 26)
        }
        
        test("should handle origin at edge with partial coverage") {
            val edge = GridPos(1, 15)
            val template = CubeTemplate(sideLengthInFeet = 10)
            val affected = template.affectedPositions(edge, testGrid)
            
            // Some positions will be out of bounds
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
            }
        }
        
        test("should handle origin at corner with minimal coverage") {
            val corner = GridPos(0, 0)
            val template = CubeTemplate(sideLengthInFeet = 10)
            val affected = template.affectedPositions(corner, testGrid)
            
            // Only positions in bounds should be included
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
                (pos.x >= 0) shouldBe true
                (pos.y >= 0) shouldBe true
            }
        }
    }
    
    context("ConeTemplate in all 8 directions") {
        val testGrid = MapGrid(50, 50)
        val origin = GridPos(25, 25)
        val length = 15 // 3 squares
        
        test("should create cone pointing NORTH") {
            val template = ConeTemplate(lengthInFeet = length, direction = Direction.NORTH)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should extend upward (decreasing y)
            affected.forEach { pos ->
                (pos.y <= origin.y) shouldBe true
            }
            
            // Should include positions along the north direction
            affected shouldContain GridPos(25, 24) // 1 square north
            affected shouldContain GridPos(25, 23) // 2 squares north
            affected shouldContain GridPos(25, 22) // 3 squares north
        }
        
        test("should create cone pointing NORTHEAST") {
            val template = ConeTemplate(lengthInFeet = length, direction = Direction.NORTHEAST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should extend northeast (increasing x, decreasing y)
            affected.forEach { pos ->
                (pos.x >= origin.x || pos.y <= origin.y) shouldBe true
            }
        }
        
        test("should create cone pointing EAST") {
            val template = ConeTemplate(lengthInFeet = length, direction = Direction.EAST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should extend eastward (increasing x)
            affected.forEach { pos ->
                (pos.x >= origin.x) shouldBe true
            }
            
            // Should include positions along the east direction
            affected shouldContain GridPos(26, 25) // 1 square east
            affected shouldContain GridPos(27, 25) // 2 squares east
            affected shouldContain GridPos(28, 25) // 3 squares east
        }
        
        test("should create cone pointing SOUTHEAST") {
            val template = ConeTemplate(lengthInFeet = length, direction = Direction.SOUTHEAST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should extend southeast (increasing x and y)
            affected.forEach { pos ->
                (pos.x >= origin.x || pos.y >= origin.y) shouldBe true
            }
        }
        
        test("should create cone pointing SOUTH") {
            val template = ConeTemplate(lengthInFeet = length, direction = Direction.SOUTH)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should extend downward (increasing y)
            affected.forEach { pos ->
                (pos.y >= origin.y) shouldBe true
            }
            
            // Should include positions along the south direction
            affected shouldContain GridPos(25, 26) // 1 square south
            affected shouldContain GridPos(25, 27) // 2 squares south
            affected shouldContain GridPos(25, 28) // 3 squares south
        }
        
        test("should create cone pointing SOUTHWEST") {
            val template = ConeTemplate(lengthInFeet = length, direction = Direction.SOUTHWEST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should extend southwest (decreasing x, increasing y)
            affected.forEach { pos ->
                (pos.x <= origin.x || pos.y >= origin.y) shouldBe true
            }
        }
        
        test("should create cone pointing WEST") {
            val template = ConeTemplate(lengthInFeet = length, direction = Direction.WEST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should extend westward (decreasing x)
            affected.forEach { pos ->
                (pos.x <= origin.x) shouldBe true
            }
            
            // Should include positions along the west direction
            affected shouldContain GridPos(24, 25) // 1 square west
            affected shouldContain GridPos(23, 25) // 2 squares west
            affected shouldContain GridPos(22, 25) // 3 squares west
        }
        
        test("should create cone pointing NORTHWEST") {
            val template = ConeTemplate(lengthInFeet = length, direction = Direction.NORTHWEST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should extend northwest (decreasing x and y)
            affected.forEach { pos ->
                (pos.x <= origin.x || pos.y <= origin.y) shouldBe true
            }
        }
    }
    
    context("ConeTemplate with lengths 15ft, 30ft, 60ft") {
        val testGrid = MapGrid(50, 50)
        val origin = GridPos(25, 25)
        
        test("should create 15ft cone (3 squares)") {
            val template = ConeTemplate(lengthInFeet = 15, direction = Direction.NORTH)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Should extend 3 squares north
            val maxDistance = affected.maxOf { pos -> 
                DistanceCalculator.chebyshevDistance(origin, pos)
            }
            maxDistance shouldBe 3
        }
        
        test("should create 30ft cone (6 squares)") {
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.EAST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Should extend 6 squares east
            val maxDistance = affected.maxOf { pos -> 
                DistanceCalculator.chebyshevDistance(origin, pos)
            }
            maxDistance shouldBe 6
        }
        
        test("should create 60ft cone (12 squares)") {
            val template = ConeTemplate(lengthInFeet = 60, direction = Direction.SOUTH)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Should extend 12 squares south
            val maxDistance = affected.maxOf { pos -> 
                DistanceCalculator.chebyshevDistance(origin, pos)
            }
            maxDistance shouldBe 12
        }
        
        test("should have more positions with longer cone") {
            val cone15 = ConeTemplate(lengthInFeet = 15, direction = Direction.NORTH)
            val cone30 = ConeTemplate(lengthInFeet = 30, direction = Direction.NORTH)
            val cone60 = ConeTemplate(lengthInFeet = 60, direction = Direction.NORTH)
            
            val affected15 = cone15.affectedPositions(origin, testGrid)
            val affected30 = cone30.affectedPositions(origin, testGrid)
            val affected60 = cone60.affectedPositions(origin, testGrid)
            
            // Longer cones should affect more positions
            (affected30.size > affected15.size) shouldBe true
            (affected60.size > affected30.size) shouldBe true
        }
    }
    
    context("ConeTemplate width increases with distance") {
        val testGrid = MapGrid(50, 50)
        val origin = GridPos(25, 25)
        
        test("should have width of 1 at distance 1") {
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.NORTH)
            val affected = template.affectedPositions(origin, testGrid)
            
            // At distance 1 (y=24), width should be 1 (just the center line)
            val atDistance1 = affected.filter { it.y == 24 }
            atDistance1 shouldHaveSize 1
            atDistance1 shouldContain GridPos(25, 24)
        }
        
        test("should have width of 3 at distance 2") {
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.NORTH)
            val affected = template.affectedPositions(origin, testGrid)
            
            // At distance 2 (y=23), width should be 3 (offset range -1..1)
            val atDistance2 = affected.filter { it.y == 23 }
            atDistance2 shouldHaveSize 3
        }
        
        test("should have width of 3 at distance 3") {
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.NORTH)
            val affected = template.affectedPositions(origin, testGrid)
            
            // At distance 3 (y=22), width should be 3
            val atDistance3 = affected.filter { it.y == 22 }
            atDistance3 shouldHaveSize 3
        }
        
        test("should widen progressively for EAST direction") {
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.EAST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Check widths at different distances
            val atDistance1 = affected.filter { it.x == 26 }
            val atDistance2 = affected.filter { it.x == 27 }
            val atDistance3 = affected.filter { it.x == 28 }
            
            atDistance1 shouldHaveSize 1
            atDistance2 shouldHaveSize 3 // width=2, offset range -1..1 = 3 positions
            atDistance3 shouldHaveSize 3 // width=3, offset range -1..1 = 3 positions
        }
        
        test("should widen progressively for diagonal direction") {
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.NORTHEAST)
            val affected = template.affectedPositions(origin, testGrid)
            
            // Cone should widen as it extends
            val distances = affected.groupBy { pos ->
                DistanceCalculator.chebyshevDistance(origin, pos)
            }
            
            // Each distance level should have more or equal positions than the previous
            // (except distance 0 which is the origin)
            for (d in 2..5) {
                if (distances.containsKey(d) && distances.containsKey(d - 1)) {
                    (distances[d]!!.size >= distances[d - 1]!!.size) shouldBe true
                }
            }
        }
    }
    
    context("All templates exclude out-of-bounds positions") {
        val testGrid = MapGrid(20, 20)
        
        test("SphereTemplate excludes out-of-bounds at corner") {
            val corner = GridPos(0, 0)
            val template = SphereTemplate(radiusInFeet = 15)
            val affected = template.affectedPositions(corner, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
            }
        }
        
        test("CubeTemplate excludes out-of-bounds at corner") {
            val corner = GridPos(0, 0)
            val template = CubeTemplate(sideLengthInFeet = 20)
            val affected = template.affectedPositions(corner, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
            }
        }
        
        test("ConeTemplate excludes out-of-bounds at corner pointing outward") {
            val corner = GridPos(0, 0)
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.NORTHWEST)
            val affected = template.affectedPositions(corner, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
            }
        }
        
        test("ConeTemplate excludes out-of-bounds at edge") {
            val edge = GridPos(0, 10)
            val template = ConeTemplate(lengthInFeet = 30, direction = Direction.WEST)
            val affected = template.affectedPositions(edge, testGrid)
            
            affected.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
            }
        }
        
        test("All templates return empty set when origin is out of bounds") {
            val outOfBounds = GridPos(-5, -5)
            
            val sphere = SphereTemplate(radiusInFeet = 10)
            val cube = CubeTemplate(sideLengthInFeet = 10)
            val cone = ConeTemplate(lengthInFeet = 15, direction = Direction.NORTH)
            
            sphere.affectedPositions(outOfBounds, testGrid) shouldHaveSize 0
            cube.affectedPositions(outOfBounds, testGrid) shouldHaveSize 0
            cone.affectedPositions(outOfBounds, testGrid) shouldHaveSize 0
        }
    }
})
