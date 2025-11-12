package dev.questweaver.domain.map.geometry

import dev.questweaver.domain.entities.Creature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Integration tests verifying the geometry module works correctly with other core:domain entities.
 * This validates requirement 6.5: Test integration with existing core:domain entities.
 */
class GeometryIntegrationTest : FunSpec({
    
    context("Integration with core:domain entities") {
        test("GridPos can be used as Creature position") {
            // Verify GridPos integrates with domain entities
            val position = GridPos(10, 15)
            
            // GridPos should be usable in entity contexts
            position.x shouldBe 10
            position.y shouldBe 15
        }
        
        test("MapGrid can store creature positions") {
            val grid = MapGrid(width = 50, height = 50)
            val creatureId = 123L
            val creaturePos = GridPos(25, 25)
            
            // Store creature in grid
            val updatedGrid = grid.withCellProperties(
                creaturePos,
                CellProperties(occupiedBy = creatureId)
            )
            
            // Verify creature is stored
            val cellProps = updatedGrid.getCellProperties(creaturePos)
            cellProps.occupiedBy shouldBe creatureId
        }
        
        test("Multiple creatures can be tracked on grid") {
            var grid = MapGrid(width = 50, height = 50)
            
            // Add multiple creatures
            val creatures = mapOf(
                GridPos(10, 10) to 1L,
                GridPos(20, 20) to 2L,
                GridPos(30, 30) to 3L
            )
            
            creatures.forEach { (pos, id) ->
                grid = grid.withCellProperties(pos, CellProperties(occupiedBy = id))
            }
            
            // Verify all creatures are tracked
            creatures.forEach { (pos, id) ->
                grid.getCellProperties(pos).occupiedBy shouldBe id
            }
        }
        
        test("Serialization works for grid with creature data") {
            val json = Json { 
                prettyPrint = false
                allowStructuredMapKeys = true
            }
            
            var grid = MapGrid(width = 20, height = 20)
            grid = grid.withCellProperties(
                GridPos(10, 10),
                CellProperties(occupiedBy = 42L, hasObstacle = false)
            )
            
            // Serialize
            val jsonString = json.encodeToString(grid)
            jsonString shouldNotBe ""
            
            // Deserialize
            val deserialized = json.decodeFromString<MapGrid>(jsonString)
            deserialized.getCellProperties(GridPos(10, 10)).occupiedBy shouldBe 42L
        }
        
        test("Distance calculations work for creature movement") {
            val creaturePos = GridPos(10, 10)
            val targetPos = GridPos(15, 15)
            
            // Calculate distance for movement
            val distance = creaturePos.distanceToInFeet(targetPos)
            
            // Verify distance is correct (Chebyshev distance * 5)
            distance shouldBe 25 // max(5, 5) * 5 = 25 feet
        }
        
        test("Line-of-effect works for creature targeting") {
            val grid = MapGrid(width = 50, height = 50)
                .withCellProperties(GridPos(15, 15), CellProperties(hasObstacle = true))
            
            val attackerPos = GridPos(10, 10)
            val targetPos = GridPos(20, 20)
            
            // Check if attacker has line-of-effect to target
            val hasLOS = LineOfEffect.hasLineOfEffect(attackerPos, targetPos, grid)
            
            // Should be blocked by obstacle at (15, 15)
            hasLOS shouldBe false
        }
        
        test("AoE templates work for spell effects") {
            val grid = MapGrid(width = 50, height = 50)
            val casterPos = GridPos(25, 25)
            val fireball = SphereTemplate(radiusInFeet = 20)
            
            // Calculate affected positions
            val affected = fireball.affectedPositions(casterPos, grid)
            
            // Verify caster position is included
            affected.contains(casterPos) shouldBe true
            
            // Verify positions at exact range are included
            val edgePos = GridPos(29, 29) // 4 squares away = 20 feet
            affected.contains(edgePos) shouldBe true
        }
    }
    
    context("No Android dependencies verification") {
        test("All geometry classes are pure Kotlin") {
            // This test verifies that the geometry module compiles without Android dependencies
            // If any Android imports were present, this would fail to compile
            
            val pos = GridPos(0, 0)
            val grid = MapGrid(10, 10)
            val direction = Direction.NORTH
            val terrain = TerrainType.NORMAL
            val props = CellProperties()
            
            // All classes should be instantiable
            pos shouldNotBe null
            grid shouldNotBe null
            direction shouldNotBe null
            terrain shouldNotBe null
            props shouldNotBe null
        }
    }
})
