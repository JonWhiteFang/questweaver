package dev.questweaver.domain.values

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class GridPosTest : FunSpec({
    
    context("GridPos validation") {
        test("should create valid GridPos with non-negative coordinates") {
            val pos = GridPos(5, 10)
            pos.x shouldBe 5
            pos.y shouldBe 10
        }
        
        test("should create GridPos at origin (0, 0)") {
            val pos = GridPos(0, 0)
            pos.x shouldBe 0
            pos.y shouldBe 0
        }
        
        test("should throw exception for negative x coordinate") {
            shouldThrow<IllegalArgumentException> {
                GridPos(-1, 5)
            }
        }
        
        test("should throw exception for negative y coordinate") {
            shouldThrow<IllegalArgumentException> {
                GridPos(5, -1)
            }
        }
        
        test("should throw exception for both negative coordinates") {
            shouldThrow<IllegalArgumentException> {
                GridPos(-1, -1)
            }
        }
    }
    
    context("distanceTo using Chebyshev distance") {
        test("should calculate distance to same position as 0") {
            val pos = GridPos(5, 5)
            pos.distanceTo(pos) shouldBe 0
        }
        
        test("should calculate horizontal distance") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(5, 0)
            pos1.distanceTo(pos2) shouldBe 5
        }
        
        test("should calculate vertical distance") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(0, 5)
            pos1.distanceTo(pos2) shouldBe 5
        }
        
        test("should calculate diagonal distance (Chebyshev uses max of differences)") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(3, 4)
            // Chebyshev distance = max(|3-0|, |4-0|) = max(3, 4) = 4
            pos1.distanceTo(pos2) shouldBe 4
        }
        
        test("should calculate distance symmetrically") {
            val pos1 = GridPos(2, 3)
            val pos2 = GridPos(7, 8)
            pos1.distanceTo(pos2) shouldBe pos2.distanceTo(pos1)
        }
        
        test("should calculate distance for adjacent positions as 1") {
            val center = GridPos(5, 5)
            center.distanceTo(GridPos(6, 5)) shouldBe 1
            center.distanceTo(GridPos(5, 6)) shouldBe 1
            center.distanceTo(GridPos(6, 6)) shouldBe 1
        }
    }
    
    context("neighbors method") {
        test("should return 8 neighbors for position not at edge") {
            val pos = GridPos(5, 5)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            neighbors shouldContainExactlyInAnyOrder listOf(
                GridPos(4, 5), GridPos(6, 5),  // Left, Right
                GridPos(5, 4), GridPos(5, 6),  // Up, Down
                GridPos(4, 4), GridPos(6, 4),  // Diagonals
                GridPos(4, 6), GridPos(6, 6)
            )
        }
        
        test("should return 3 neighbors for position at origin (0, 0)") {
            val pos = GridPos(0, 0)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 3
            neighbors shouldContainExactlyInAnyOrder listOf(
                GridPos(1, 0),  // Right
                GridPos(0, 1),  // Down
                GridPos(1, 1)   // Diagonal down-right
            )
        }
        
        test("should return 5 neighbors for position at x=0 edge") {
            val pos = GridPos(0, 5)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 5
            neighbors shouldContainExactlyInAnyOrder listOf(
                GridPos(1, 5),  // Right
                GridPos(0, 4),  // Up
                GridPos(0, 6),  // Down
                GridPos(1, 4),  // Diagonal up-right
                GridPos(1, 6)   // Diagonal down-right
            )
        }
        
        test("should return 5 neighbors for position at y=0 edge") {
            val pos = GridPos(5, 0)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 5
            neighbors shouldContainExactlyInAnyOrder listOf(
                GridPos(4, 0),  // Left
                GridPos(6, 0),  // Right
                GridPos(5, 1),  // Down
                GridPos(4, 1),  // Diagonal down-left
                GridPos(6, 1)   // Diagonal down-right
            )
        }
        
        test("should not include neighbors with negative coordinates") {
            val pos = GridPos(1, 1)
            val neighbors = pos.neighbors()
            
            neighbors.forEach { neighbor ->
                (neighbor.x >= 0) shouldBe true
                (neighbor.y >= 0) shouldBe true
            }
        }
    }
})
