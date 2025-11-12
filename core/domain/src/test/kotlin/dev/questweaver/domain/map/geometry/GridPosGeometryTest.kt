package dev.questweaver.domain.map.geometry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class GridPosGeometryTest : FunSpec({
    
    context("GridPos creation and equality") {
        test("should create GridPos with positive coordinates") {
            val pos = GridPos(5, 10)
            pos.x shouldBe 5
            pos.y shouldBe 10
        }
        
        test("should create GridPos at origin") {
            val pos = GridPos(0, 0)
            pos.x shouldBe 0
            pos.y shouldBe 0
        }
        
        test("should support negative coordinates") {
            val pos = GridPos(-5, -10)
            pos.x shouldBe -5
            pos.y shouldBe -10
        }
        
        test("should be equal when coordinates match") {
            val pos1 = GridPos(5, 10)
            val pos2 = GridPos(5, 10)
            pos1 shouldBe pos2
        }
        
        test("should have consistent hashCode for equal positions") {
            val pos1 = GridPos(5, 10)
            val pos2 = GridPos(5, 10)
            pos1.hashCode() shouldBe pos2.hashCode()
        }
    }
    
    context("GridPos serialization") {
        val json = Json { prettyPrint = false }
        
        test("should serialize to JSON") {
            val pos = GridPos(5, 10)
            val jsonString = json.encodeToString(pos)
            jsonString shouldBe """{"x":5,"y":10}"""
        }
        
        test("should deserialize from JSON") {
            val jsonString = """{"x":5,"y":10}"""
            val pos = json.decodeFromString<GridPos>(jsonString)
            pos shouldBe GridPos(5, 10)
        }
        
        test("should round-trip serialize and deserialize") {
            val original = GridPos(15, 25)
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<GridPos>(jsonString)
            deserialized shouldBe original
        }
    }
    
    context("distanceTo calculations") {
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
        
        test("should calculate diagonal distance using Chebyshev") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(3, 4)
            // Chebyshev: max(|3-0|, |4-0|) = max(3, 4) = 4
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
    
    context("distanceToInFeet calculations") {
        test("should convert distance to feet (5ft per square)") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(3, 0)
            pos1.distanceToInFeet(pos2) shouldBe 15
        }
        
        test("should return 0 feet for same position") {
            val pos = GridPos(5, 5)
            pos.distanceToInFeet(pos) shouldBe 0
        }
        
        test("should calculate diagonal distance in feet") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(3, 4)
            // Chebyshev distance = 4, so 4 * 5 = 20 feet
            pos1.distanceToInFeet(pos2) shouldBe 20
        }
    }
    
    context("isWithinRange checks") {
        test("should return true when exactly at range") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(3, 0)
            pos1.isWithinRange(pos2, 15) shouldBe true
        }
        
        test("should return true when within range") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(2, 0)
            pos1.isWithinRange(pos2, 15) shouldBe true
        }
        
        test("should return false when outside range") {
            val pos1 = GridPos(0, 0)
            val pos2 = GridPos(4, 0)
            pos1.isWithinRange(pos2, 15) shouldBe false
        }
        
        test("should return true for same position with any range") {
            val pos = GridPos(5, 5)
            pos.isWithinRange(pos, 5) shouldBe true
        }
    }
    
    context("neighbors method") {
        test("should return 8 neighbors for interior position") {
            val pos = GridPos(5, 5)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            neighbors shouldContainExactlyInAnyOrder listOf(
                GridPos(4, 4), GridPos(5, 4), GridPos(6, 4),
                GridPos(4, 5),                GridPos(6, 5),
                GridPos(4, 6), GridPos(5, 6), GridPos(6, 6)
            )
        }
        
        test("should return 8 neighbors including negative coordinates") {
            val pos = GridPos(0, 0)
            val neighbors = pos.neighbors()
            
            neighbors shouldHaveSize 8
            neighbors shouldContainExactlyInAnyOrder listOf(
                GridPos(-1, -1), GridPos(0, -1), GridPos(1, -1),
                GridPos(-1, 0),                   GridPos(1, 0),
                GridPos(-1, 1),  GridPos(0, 1),  GridPos(1, 1)
            )
        }
    }
    
    context("neighborsInDirection method") {
        test("should return correct neighbor for NORTH") {
            val pos = GridPos(5, 5)
            pos.neighborsInDirection(Direction.NORTH) shouldBe GridPos(5, 4)
        }
        
        test("should return correct neighbor for SOUTH") {
            val pos = GridPos(5, 5)
            pos.neighborsInDirection(Direction.SOUTH) shouldBe GridPos(5, 6)
        }
        
        test("should return correct neighbor for EAST") {
            val pos = GridPos(5, 5)
            pos.neighborsInDirection(Direction.EAST) shouldBe GridPos(6, 5)
        }
        
        test("should return correct neighbor for WEST") {
            val pos = GridPos(5, 5)
            pos.neighborsInDirection(Direction.WEST) shouldBe GridPos(4, 5)
        }
        
        test("should return correct neighbor for NORTHEAST") {
            val pos = GridPos(5, 5)
            pos.neighborsInDirection(Direction.NORTHEAST) shouldBe GridPos(6, 4)
        }
        
        test("should return correct neighbor for NORTHWEST") {
            val pos = GridPos(5, 5)
            pos.neighborsInDirection(Direction.NORTHWEST) shouldBe GridPos(4, 4)
        }
        
        test("should return correct neighbor for SOUTHEAST") {
            val pos = GridPos(5, 5)
            pos.neighborsInDirection(Direction.SOUTHEAST) shouldBe GridPos(6, 6)
        }
        
        test("should return correct neighbor for SOUTHWEST") {
            val pos = GridPos(5, 5)
            pos.neighborsInDirection(Direction.SOUTHWEST) shouldBe GridPos(4, 6)
        }
    }
})
