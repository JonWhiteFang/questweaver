package dev.questweaver.domain.entities

import dev.questweaver.domain.values.GridPos
import dev.questweaver.domain.values.TerrainType
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class MapGridTest : FunSpec({
    
    context("MapGrid validation") {
        test("should create MapGrid with valid dimensions") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.width shouldBe 10
            grid.height shouldBe 10
            grid.terrain shouldBe emptyMap()
            grid.creaturePositions shouldBe emptyMap()
        }
        
        test("should create MapGrid with terrain") {
            val terrain = mapOf(
                GridPos(0, 0) to TerrainType.DIFFICULT,
                GridPos(5, 5) to TerrainType.IMPASSABLE
            )
            val grid = MapGrid(width = 10, height = 10, terrain = terrain)
            
            grid.terrain shouldBe terrain
        }
        
        test("should create MapGrid with creature positions") {
            val positions = mapOf(
                1L to GridPos(2, 3),
                2L to GridPos(7, 8)
            )
            val grid = MapGrid(width = 10, height = 10, creaturePositions = positions)
            
            grid.creaturePositions shouldBe positions
        }
        
        test("should throw exception for non-positive width") {
            shouldThrow<IllegalArgumentException> {
                MapGrid(width = 0, height = 10)
            }
        }
        
        test("should throw exception for negative width") {
            shouldThrow<IllegalArgumentException> {
                MapGrid(width = -1, height = 10)
            }
        }
        
        test("should throw exception for non-positive height") {
            shouldThrow<IllegalArgumentException> {
                MapGrid(width = 10, height = 0)
            }
        }
        
        test("should throw exception for negative height") {
            shouldThrow<IllegalArgumentException> {
                MapGrid(width = 10, height = -1)
            }
        }
        
        test("should throw exception for terrain position outside bounds (x too large)") {
            val exception = shouldThrow<IllegalArgumentException> {
                MapGrid(
                    width = 10,
                    height = 10,
                    terrain = mapOf(GridPos(10, 5) to TerrainType.DIFFICULT)
                )
            }
            
            exception.message shouldContain "outside grid bounds"
        }
        
        test("should throw exception for terrain position outside bounds (y too large)") {
            val exception = shouldThrow<IllegalArgumentException> {
                MapGrid(
                    width = 10,
                    height = 10,
                    terrain = mapOf(GridPos(5, 10) to TerrainType.DIFFICULT)
                )
            }
            
            exception.message shouldContain "outside grid bounds"
        }
        
        test("should accept terrain at maximum valid position") {
            shouldNotThrowAny {
                MapGrid(
                    width = 10,
                    height = 10,
                    terrain = mapOf(GridPos(9, 9) to TerrainType.DIFFICULT)
                )
            }
        }
        
        test("should throw exception for creature position outside bounds (x too large)") {
            val exception = shouldThrow<IllegalArgumentException> {
                MapGrid(
                    width = 10,
                    height = 10,
                    creaturePositions = mapOf(1L to GridPos(10, 5))
                )
            }
            
            exception.message shouldContain "outside grid bounds"
        }
        
        test("should throw exception for creature position outside bounds (y too large)") {
            val exception = shouldThrow<IllegalArgumentException> {
                MapGrid(
                    width = 10,
                    height = 10,
                    creaturePositions = mapOf(1L to GridPos(5, 10))
                )
            }
            
            exception.message shouldContain "outside grid bounds"
        }
        
        test("should accept creature at maximum valid position") {
            shouldNotThrowAny {
                MapGrid(
                    width = 10,
                    height = 10,
                    creaturePositions = mapOf(1L to GridPos(9, 9))
                )
            }
        }
    }
    
    context("MapGrid isInBounds method") {
        test("should return true for position at origin") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.isInBounds(GridPos(0, 0)) shouldBe true
        }
        
        test("should return true for position at maximum bounds") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.isInBounds(GridPos(9, 9)) shouldBe true
        }
        
        test("should return true for position in middle") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.isInBounds(GridPos(5, 5)) shouldBe true
        }
        
        test("should return false for position with x at width") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.isInBounds(GridPos(10, 5)) shouldBe false
        }
        
        test("should return false for position with y at height") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.isInBounds(GridPos(5, 10)) shouldBe false
        }
        
        test("should return false for position with x beyond width") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.isInBounds(GridPos(15, 5)) shouldBe false
        }
        
        test("should return false for position with y beyond height") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.isInBounds(GridPos(5, 15)) shouldBe false
        }
    }
    
    context("MapGrid getTerrainAt method") {
        test("should return EMPTY for position with no terrain defined") {
            val grid = MapGrid(width = 10, height = 10)
            
            grid.getTerrainAt(GridPos(5, 5)) shouldBe TerrainType.EMPTY
        }
        
        test("should return correct terrain type for defined position") {
            val grid = MapGrid(
                width = 10,
                height = 10,
                terrain = mapOf(GridPos(5, 5) to TerrainType.DIFFICULT)
            )
            
            grid.getTerrainAt(GridPos(5, 5)) shouldBe TerrainType.DIFFICULT
        }
        
        test("should return EMPTY for adjacent position with no terrain") {
            val grid = MapGrid(
                width = 10,
                height = 10,
                terrain = mapOf(GridPos(5, 5) to TerrainType.DIFFICULT)
            )
            
            grid.getTerrainAt(GridPos(5, 6)) shouldBe TerrainType.EMPTY
        }
        
        test("should return correct terrain for multiple defined positions") {
            val grid = MapGrid(
                width = 10,
                height = 10,
                terrain = mapOf(
                    GridPos(2, 3) to TerrainType.DIFFICULT,
                    GridPos(7, 8) to TerrainType.IMPASSABLE,
                    GridPos(1, 1) to TerrainType.OCCUPIED
                )
            )
            
            grid.getTerrainAt(GridPos(2, 3)) shouldBe TerrainType.DIFFICULT
            grid.getTerrainAt(GridPos(7, 8)) shouldBe TerrainType.IMPASSABLE
            grid.getTerrainAt(GridPos(1, 1)) shouldBe TerrainType.OCCUPIED
            grid.getTerrainAt(GridPos(0, 0)) shouldBe TerrainType.EMPTY
        }
    }
})
