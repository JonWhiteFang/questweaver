package dev.questweaver.domain.map.geometry

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class MapGridTest : FunSpec({
    
    context("MapGrid initialization") {
        test("should create grid with valid dimensions") {
            val grid = MapGrid(width = 20, height = 30)
            grid.width shouldBe 20
            grid.height shouldBe 30
        }
        
        test("should create grid with minimum dimensions") {
            val grid = MapGrid(width = 10, height = 10)
            grid.width shouldBe 10
            grid.height shouldBe 10
        }
        
        test("should create grid with maximum dimensions") {
            val grid = MapGrid(width = 100, height = 100)
            grid.width shouldBe 100
            grid.height shouldBe 100
        }
        
        test("should throw exception for width below minimum") {
            shouldThrow<IllegalArgumentException> {
                MapGrid(width = 9, height = 20)
            }
        }
        
        test("should throw exception for height below minimum") {
            shouldThrow<IllegalArgumentException> {
                MapGrid(width = 20, height = 9)
            }
        }
        
        test("should throw exception for width above maximum") {
            shouldThrow<IllegalArgumentException> {
                MapGrid(width = 101, height = 20)
            }
        }
        
        test("should throw exception for height above maximum") {
            shouldThrow<IllegalArgumentException> {
                MapGrid(width = 20, height = 101)
            }
        }
        
        test("should initialize with empty cells map by default") {
            val grid = MapGrid(width = 20, height = 20)
            grid.cells shouldBe emptyMap()
        }
    }
    
    context("isInBounds checks") {
        val grid = MapGrid(width = 20, height = 30)
        
        test("should return true for position at origin") {
            grid.isInBounds(GridPos(0, 0)) shouldBe true
        }
        
        test("should return true for position at max coordinates") {
            grid.isInBounds(GridPos(19, 29)) shouldBe true
        }
        
        test("should return true for interior position") {
            grid.isInBounds(GridPos(10, 15)) shouldBe true
        }
        
        test("should return false for negative x") {
            grid.isInBounds(GridPos(-1, 10)) shouldBe false
        }
        
        test("should return false for negative y") {
            grid.isInBounds(GridPos(10, -1)) shouldBe false
        }
        
        test("should return false for x at width") {
            grid.isInBounds(GridPos(20, 10)) shouldBe false
        }
        
        test("should return false for y at height") {
            grid.isInBounds(GridPos(10, 30)) shouldBe false
        }
        
        test("should return false for x beyond width") {
            grid.isInBounds(GridPos(25, 10)) shouldBe false
        }
        
        test("should return false for y beyond height") {
            grid.isInBounds(GridPos(10, 35)) shouldBe false
        }
    }
    
    context("getCellProperties") {
        test("should return default properties for unset cell") {
            val grid = MapGrid(width = 20, height = 20)
            val props = grid.getCellProperties(GridPos(5, 5))
            
            props.terrainType shouldBe TerrainType.NORMAL
            props.hasObstacle shouldBe false
            props.occupiedBy shouldBe null
        }
        
        test("should return set properties for configured cell") {
            val pos = GridPos(5, 5)
            val properties = CellProperties(
                terrainType = TerrainType.DIFFICULT,
                hasObstacle = true,
                occupiedBy = 123L
            )
            val grid = MapGrid(
                width = 20,
                height = 20,
                cells = mapOf(pos to properties)
            )
            
            grid.getCellProperties(pos) shouldBe properties
        }
    }
    
    context("withCellProperties immutability") {
        test("should return new grid with updated cell") {
            val original = MapGrid(width = 20, height = 20)
            val pos = GridPos(5, 5)
            val properties = CellProperties(hasObstacle = true)
            
            val updated = original.withCellProperties(pos, properties)
            
            updated.getCellProperties(pos) shouldBe properties
            original.getCellProperties(pos) shouldBe CellProperties()
        }
        
        test("should preserve other cells when updating one") {
            val pos1 = GridPos(5, 5)
            val pos2 = GridPos(10, 10)
            val props1 = CellProperties(hasObstacle = true)
            val props2 = CellProperties(terrainType = TerrainType.DIFFICULT)
            
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(pos1, props1)
                .withCellProperties(pos2, props2)
            
            grid.getCellProperties(pos1) shouldBe props1
            grid.getCellProperties(pos2) shouldBe props2
        }
        
        test("should overwrite existing cell properties") {
            val pos = GridPos(5, 5)
            val props1 = CellProperties(hasObstacle = true)
            val props2 = CellProperties(terrainType = TerrainType.IMPASSABLE)
            
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(pos, props1)
                .withCellProperties(pos, props2)
            
            grid.getCellProperties(pos) shouldBe props2
        }
    }
    
    context("allPositions sequence") {
        test("should generate all positions for small grid") {
            val grid = MapGrid(width = 10, height = 10)
            val positions = grid.allPositions().take(6).toList()
            
            positions shouldBe listOf(
                GridPos(0, 0), GridPos(1, 0), GridPos(2, 0),
                GridPos(3, 0), GridPos(4, 0), GridPos(5, 0)
            )
        }
        
        test("should generate correct count of positions") {
            val grid = MapGrid(width = 20, height = 30)
            val count = grid.allPositions().count()
            
            count shouldBe 600
        }
        
        test("should generate positions lazily") {
            val grid = MapGrid(width = 100, height = 100)
            val first5 = grid.allPositions().take(5).toList()
            
            first5 shouldBe listOf(
                GridPos(0, 0), GridPos(1, 0), GridPos(2, 0),
                GridPos(3, 0), GridPos(4, 0)
            )
        }
        
        test("should generate all positions within bounds") {
            val grid = MapGrid(width = 20, height = 20)
            val allInBounds = grid.allPositions().all { pos ->
                grid.isInBounds(pos)
            }
            
            allInBounds shouldBe true
        }
    }
    
    context("MapGrid serialization") {
        val json = Json { 
            prettyPrint = false
            allowStructuredMapKeys = true
        }
        
        test("should serialize empty grid") {
            val grid = MapGrid(width = 20, height = 30)
            val jsonString = json.encodeToString(grid)
            
            // Empty cells map is omitted in serialization (default value)
            jsonString shouldBe """{"width":20,"height":30}"""
        }
        
        test("should deserialize empty grid") {
            val jsonString = """{"width":20,"height":30,"cells":[]}"""
            val grid = json.decodeFromString<MapGrid>(jsonString)
            
            grid.width shouldBe 20
            grid.height shouldBe 30
            grid.cells shouldBe emptyMap()
        }
        
        test("should round-trip serialize grid with cells") {
            val pos = GridPos(5, 5)
            val properties = CellProperties(hasObstacle = true)
            val original = MapGrid(
                width = 20,
                height = 20,
                cells = mapOf(pos to properties)
            )
            
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<MapGrid>(jsonString)
            
            deserialized.width shouldBe original.width
            deserialized.height shouldBe original.height
            deserialized.getCellProperties(pos) shouldBe properties
        }
    }
})
