package dev.questweaver.domain.map.geometry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class DistanceCalculatorTest : FunSpec({
    
    context("Chebyshev distance for orthogonal movement (same row/column)") {
        test("should calculate distance for horizontal movement (same row)") {
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 10
        }
        
        test("should calculate distance for vertical movement (same column)") {
            val from = GridPos(5, 0)
            val to = GridPos(5, 10)
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 10
        }
        
        test("should calculate distance for horizontal movement in negative direction") {
            val from = GridPos(10, 5)
            val to = GridPos(3, 5)
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 7
        }
        
        test("should calculate distance for vertical movement in negative direction") {
            val from = GridPos(5, 10)
            val to = GridPos(5, 2)
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 8
        }
        
        test("should return 0 for same position") {
            val pos = GridPos(5, 5)
            DistanceCalculator.chebyshevDistance(pos, pos) shouldBe 0
        }
    }
    
    context("Chebyshev distance for diagonal movement (45-degree angles)") {
        test("should calculate distance for pure diagonal movement northeast") {
            val from = GridPos(0, 0)
            val to = GridPos(5, -5)
            // Pure diagonal: max(5, 5) = 5
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 5
        }
        
        test("should calculate distance for pure diagonal movement southeast") {
            val from = GridPos(0, 0)
            val to = GridPos(5, 5)
            // Pure diagonal: max(5, 5) = 5
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 5
        }
        
        test("should calculate distance for pure diagonal movement southwest") {
            val from = GridPos(10, 10)
            val to = GridPos(5, 15)
            // Pure diagonal: max(5, 5) = 5
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 5
        }
        
        test("should calculate distance for pure diagonal movement northwest") {
            val from = GridPos(10, 10)
            val to = GridPos(5, 5)
            // Pure diagonal: max(5, 5) = 5
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 5
        }
        
        test("should treat diagonal movement same as orthogonal (D&D 5e rule)") {
            val from = GridPos(0, 0)
            val diagonal = GridPos(3, 3)
            val orthogonal = GridPos(3, 0)
            
            // Both should have same distance in Chebyshev
            DistanceCalculator.chebyshevDistance(from, diagonal) shouldBe 3
            DistanceCalculator.chebyshevDistance(from, orthogonal) shouldBe 3
        }
    }
    
    context("Chebyshev distance for mixed movement (knight's move patterns)") {
        test("should calculate distance for knight's move (2 horizontal, 1 vertical)") {
            val from = GridPos(0, 0)
            val to = GridPos(2, 1)
            // max(2, 1) = 2
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 2
        }
        
        test("should calculate distance for knight's move (1 horizontal, 2 vertical)") {
            val from = GridPos(0, 0)
            val to = GridPos(1, 2)
            // max(1, 2) = 2
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 2
        }
        
        test("should calculate distance for L-shaped movement (3 horizontal, 4 vertical)") {
            val from = GridPos(0, 0)
            val to = GridPos(3, 4)
            // max(3, 4) = 4
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 4
        }
        
        test("should calculate distance for L-shaped movement (5 horizontal, 2 vertical)") {
            val from = GridPos(0, 0)
            val to = GridPos(5, 2)
            // max(5, 2) = 5
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 5
        }
        
        test("should calculate distance for asymmetric movement") {
            val from = GridPos(2, 3)
            val to = GridPos(8, 5)
            // max(|8-2|, |5-3|) = max(6, 2) = 6
            DistanceCalculator.chebyshevDistance(from, to) shouldBe 6
        }
    }
    
    context("distance-to-feet conversion (multiply by 5)") {
        test("should convert 1 square to 5 feet") {
            val from = GridPos(0, 0)
            val to = GridPos(1, 0)
            DistanceCalculator.distanceInFeet(from, to) shouldBe 5
        }
        
        test("should convert 3 squares to 15 feet") {
            val from = GridPos(0, 0)
            val to = GridPos(3, 0)
            DistanceCalculator.distanceInFeet(from, to) shouldBe 15
        }
        
        test("should convert 6 squares to 30 feet") {
            val from = GridPos(0, 0)
            val to = GridPos(6, 0)
            DistanceCalculator.distanceInFeet(from, to) shouldBe 30
        }
        
        test("should convert 12 squares to 60 feet") {
            val from = GridPos(0, 0)
            val to = GridPos(12, 0)
            DistanceCalculator.distanceInFeet(from, to) shouldBe 60
        }
        
        test("should convert 24 squares to 120 feet") {
            val from = GridPos(0, 0)
            val to = GridPos(24, 0)
            DistanceCalculator.distanceInFeet(from, to) shouldBe 120
        }
        
        test("should convert diagonal distance to feet") {
            val from = GridPos(0, 0)
            val to = GridPos(4, 4)
            // Chebyshev distance = 4, so 4 * 5 = 20 feet
            DistanceCalculator.distanceInFeet(from, to) shouldBe 20
        }
        
        test("should return 0 feet for same position") {
            val pos = GridPos(5, 5)
            DistanceCalculator.distanceInFeet(pos, pos) shouldBe 0
        }
    }
    
    context("isWithinRange for positions at exact range boundary") {
        val testGrid = MapGrid(50, 50)
        
        test("should return true when exactly at 5ft range") {
            val from = GridPos(10, 10)
            val to = GridPos(11, 10)
            from.isWithinRange(to, 5) shouldBe true
        }
        
        test("should return true when exactly at 10ft range") {
            val from = GridPos(10, 10)
            val to = GridPos(12, 10)
            from.isWithinRange(to, 10) shouldBe true
        }
        
        test("should return true when exactly at 15ft range") {
            val from = GridPos(10, 10)
            val to = GridPos(13, 10)
            from.isWithinRange(to, 15) shouldBe true
        }
        
        test("should return true when exactly at 30ft range") {
            val from = GridPos(10, 10)
            val to = GridPos(16, 10)
            from.isWithinRange(to, 30) shouldBe true
        }
        
        test("should return true when exactly at 60ft range") {
            val from = GridPos(10, 10)
            val to = GridPos(22, 10)
            from.isWithinRange(to, 60) shouldBe true
        }
        
        test("should return false when just outside 5ft range") {
            val from = GridPos(10, 10)
            val to = GridPos(12, 10)
            from.isWithinRange(to, 5) shouldBe false
        }
        
        test("should return false when just outside 30ft range") {
            val from = GridPos(10, 10)
            val to = GridPos(17, 10)
            from.isWithinRange(to, 30) shouldBe false
        }
        
        test("should handle diagonal positions at exact range boundary") {
            val from = GridPos(10, 10)
            val to = GridPos(13, 13)
            // Chebyshev distance = 3, so 15 feet
            from.isWithinRange(to, 15) shouldBe true
            from.isWithinRange(to, 10) shouldBe false
        }
    }
    
    context("neighbor queries for interior positions") {
        test("should return all 8 neighbors for interior position") {
            val pos = GridPos(25, 25)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            neighbors shouldContain GridPos(24, 24) // NW
            neighbors shouldContain GridPos(25, 24) // N
            neighbors shouldContain GridPos(26, 24) // NE
            neighbors shouldContain GridPos(24, 25) // W
            neighbors shouldContain GridPos(26, 25) // E
            neighbors shouldContain GridPos(24, 26) // SW
            neighbors shouldContain GridPos(25, 26) // S
            neighbors shouldContain GridPos(26, 26) // SE
        }
        
        test("should calculate correct distance to all neighbors") {
            val pos = GridPos(25, 25)
            val neighbors = pos.neighbors()
            
            neighbors.forEach { neighbor ->
                pos.distanceTo(neighbor) shouldBe 1
            }
        }
    }
    
    context("neighbor queries for edge positions") {
        test("should return 8 neighbors for top edge position (may include out-of-bounds)") {
            val pos = GridPos(25, 0)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            // Top edge will have neighbors with y = -1 (out of bounds)
            neighbors shouldContain GridPos(24, -1) // NW
            neighbors shouldContain GridPos(25, -1) // N
            neighbors shouldContain GridPos(26, -1) // NE
        }
        
        test("should return 8 neighbors for bottom edge position") {
            val pos = GridPos(25, 49)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            // Bottom edge will have neighbors with y = 50 (out of bounds)
            neighbors shouldContain GridPos(24, 50) // SW
            neighbors shouldContain GridPos(25, 50) // S
            neighbors shouldContain GridPos(26, 50) // SE
        }
        
        test("should return 8 neighbors for left edge position") {
            val pos = GridPos(0, 25)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            // Left edge will have neighbors with x = -1 (out of bounds)
            neighbors shouldContain GridPos(-1, 24) // NW
            neighbors shouldContain GridPos(-1, 25) // W
            neighbors shouldContain GridPos(-1, 26) // SW
        }
        
        test("should return 8 neighbors for right edge position") {
            val pos = GridPos(49, 25)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            // Right edge will have neighbors with x = 50 (out of bounds)
            neighbors shouldContain GridPos(50, 24) // NE
            neighbors shouldContain GridPos(50, 25) // E
            neighbors shouldContain GridPos(50, 26) // SE
        }
    }
    
    context("neighbor queries for corner positions") {
        test("should return 8 neighbors for top-left corner") {
            val pos = GridPos(0, 0)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            // Corner will have 3 out-of-bounds neighbors
            neighbors shouldContain GridPos(-1, -1) // NW
            neighbors shouldContain GridPos(0, -1)  // N
            neighbors shouldContain GridPos(1, -1)  // NE
            neighbors shouldContain GridPos(-1, 0)  // W
            neighbors shouldContain GridPos(1, 0)   // E
            neighbors shouldContain GridPos(-1, 1)  // SW
            neighbors shouldContain GridPos(0, 1)   // S
            neighbors shouldContain GridPos(1, 1)   // SE
        }
        
        test("should return 8 neighbors for top-right corner") {
            val pos = GridPos(49, 0)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            neighbors shouldContain GridPos(48, -1) // NW
            neighbors shouldContain GridPos(49, -1) // N
            neighbors shouldContain GridPos(50, -1) // NE
        }
        
        test("should return 8 neighbors for bottom-left corner") {
            val pos = GridPos(0, 49)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            neighbors shouldContain GridPos(-1, 48) // NW
            neighbors shouldContain GridPos(-1, 49) // W
            neighbors shouldContain GridPos(-1, 50) // SW
        }
        
        test("should return 8 neighbors for bottom-right corner") {
            val pos = GridPos(49, 49)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            neighbors shouldContain GridPos(50, 50) // SE
            neighbors shouldContain GridPos(50, 49) // E
            neighbors shouldContain GridPos(49, 50) // S
        }
    }
    
    context("positionsWithinRange filtering") {
        val testGrid = MapGrid(20, 20)
        
        test("should return only positions within 5ft range") {
            val center = GridPos(10, 10)
            val positions = DistanceCalculator.positionsWithinRange(center, 5, testGrid)
            
            // 5ft = 1 square, so should include center + 8 neighbors = 9 positions
            positions shouldHaveSize 9
            positions shouldContain center
            positions shouldContain GridPos(9, 9)
            positions shouldContain GridPos(10, 9)
            positions shouldContain GridPos(11, 9)
        }
        
        test("should return only positions within 10ft range") {
            val center = GridPos(10, 10)
            val positions = DistanceCalculator.positionsWithinRange(center, 10, testGrid)
            
            // 10ft = 2 squares, should include 5x5 grid = 25 positions
            positions shouldHaveSize 25
            positions shouldContain GridPos(8, 8)
            positions shouldContain GridPos(12, 12)
        }
        
        test("should exclude out-of-bounds positions from range query") {
            val center = GridPos(1, 1)
            val positions = DistanceCalculator.positionsWithinRange(center, 10, testGrid)
            
            // Should not include positions with negative coordinates
            positions.forEach { pos ->
                (pos.x >= 0) shouldBe true
                (pos.y >= 0) shouldBe true
            }
        }
        
        test("should handle range query at grid edge") {
            val center = GridPos(0, 0)
            val positions = DistanceCalculator.positionsWithinRange(center, 5, testGrid)
            
            // Should only include valid positions
            positions.forEach { pos ->
                testGrid.isInBounds(pos) shouldBe true
            }
        }
    }
})
