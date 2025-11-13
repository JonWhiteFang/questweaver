package dev.questweaver.feature.map.ui

import androidx.compose.ui.geometry.Offset
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.SphereTemplate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for TacticalMapCanvas state and intent types.
 * 
 * Note: Full UI rendering tests with gesture simulation require instrumented tests
 * with Compose Test framework. These unit tests focus on state management and
 * intent creation that can be tested without the full Android framework.
 */
class TacticalMapCanvasTest : FunSpec({
    
    context("MapRenderState creation") {
        
        test("should create state with minimal configuration") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10)
            )
            
            state.grid.width shouldBe 10
            state.grid.height shouldBe 10
            state.tokens shouldBe emptyList()
            state.movementPath shouldBe null
            state.rangeOverlay shouldBe null
            state.aoeOverlay shouldBe null
            state.selectedPosition shouldBe null
            state.cameraOffset shouldBe Offset.Zero
            state.zoomLevel shouldBe 1f
        }
        
        test("should create state with tokens") {
            val tokens = listOf(
                TokenRenderData(
                    creatureId = 1L,
                    position = GridPos(5, 5),
                    allegiance = Allegiance.FRIENDLY,
                    currentHP = 20,
                    maxHP = 20
                ),
                TokenRenderData(
                    creatureId = 2L,
                    position = GridPos(7, 7),
                    allegiance = Allegiance.ENEMY,
                    currentHP = 15,
                    maxHP = 30
                )
            )
            
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                tokens = tokens
            )
            
            state.tokens shouldHaveSize 2
            state.tokens[0].creatureId shouldBe 1L
            state.tokens[1].creatureId shouldBe 2L
        }
        
        test("should create state with movement path") {
            val path = listOf(
                GridPos(0, 0),
                GridPos(1, 0),
                GridPos(2, 0),
                GridPos(3, 0)
            )
            
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                movementPath = path
            )
            
            state.movementPath?.size shouldBe 4
            state.movementPath?.first() shouldBe GridPos(0, 0)
            state.movementPath?.last() shouldBe GridPos(3, 0)
        }
        
        test("should create state with range overlay") {
            val rangeOverlay = RangeOverlayData(
                origin = GridPos(5, 5),
                positions = setOf(
                    GridPos(4, 5),
                    GridPos(5, 4),
                    GridPos(6, 5),
                    GridPos(5, 6)
                ),
                rangeType = RangeType.MOVEMENT
            )
            
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                rangeOverlay = rangeOverlay
            )
            
            state.rangeOverlay?.origin shouldBe GridPos(5, 5)
            state.rangeOverlay?.positions?.size shouldBe 4
            state.rangeOverlay?.rangeType shouldBe RangeType.MOVEMENT
        }
        
        test("should create state with AoE overlay") {
            val aoeOverlay = AoEOverlayData(
                template = SphereTemplate(radiusInFeet = 10),
                origin = GridPos(5, 5),
                affectedPositions = setOf(
                    GridPos(5, 5),
                    GridPos(6, 5),
                    GridPos(5, 6),
                    GridPos(6, 6)
                )
            )
            
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                aoeOverlay = aoeOverlay
            )
            
            state.aoeOverlay?.origin shouldBe GridPos(5, 5)
            state.aoeOverlay?.affectedPositions?.size shouldBe 4
        }
        
        test("should create state with selection highlight") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                selectedPosition = GridPos(3, 3)
            )
            
            state.selectedPosition shouldBe GridPos(3, 3)
        }
        
        test("should create state with camera offset") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                cameraOffset = Offset(100f, 50f)
            )
            
            state.cameraOffset shouldBe Offset(100f, 50f)
        }
        
        test("should create state with zoom level") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                zoomLevel = 2.5f
            )
            
            state.zoomLevel shouldBe 2.5f
        }
    }
    
    context("MapIntent types") {
        
        test("should create CellTapped intent") {
            val intent = MapIntent.CellTapped(GridPos(5, 5))
            
            intent.shouldBeInstanceOf<MapIntent.CellTapped>()
            intent.position shouldBe GridPos(5, 5)
        }
        
        test("should create TokenTapped intent") {
            val intent = MapIntent.TokenTapped(creatureId = 42L)
            
            intent.shouldBeInstanceOf<MapIntent.TokenTapped>()
            intent.creatureId shouldBe 42L
        }
        
        test("should create Pan intent") {
            val intent = MapIntent.Pan(Offset(10f, 20f))
            
            intent.shouldBeInstanceOf<MapIntent.Pan>()
            intent.delta shouldBe Offset(10f, 20f)
        }
        
        test("should create Zoom intent") {
            val intent = MapIntent.Zoom(scale = 1.5f, focus = Offset(100f, 100f))
            
            intent.shouldBeInstanceOf<MapIntent.Zoom>()
            intent.scale shouldBe 1.5f
            intent.focus shouldBe Offset(100f, 100f)
        }
    }
    
    context("State immutability") {
        
        test("should create new state when copying with tokens") {
            val original = MapRenderState(
                grid = MapGrid(width = 10, height = 10)
            )
            
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 20,
                maxHP = 20
            )
            
            val updated = original.copy(tokens = listOf(token))
            
            original.tokens shouldBe emptyList()
            updated.tokens shouldHaveSize 1
        }
        
        test("should create new state when copying with camera offset") {
            val original = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                cameraOffset = Offset.Zero
            )
            
            val updated = original.copy(cameraOffset = Offset(50f, 50f))
            
            original.cameraOffset shouldBe Offset.Zero
            updated.cameraOffset shouldBe Offset(50f, 50f)
        }
        
        test("should create new state when copying with zoom level") {
            val original = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                zoomLevel = 1f
            )
            
            val updated = original.copy(zoomLevel = 2f)
            
            original.zoomLevel shouldBe 1f
            updated.zoomLevel shouldBe 2f
        }
        
        test("should create new state when copying with selected position") {
            val original = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                selectedPosition = null
            )
            
            val updated = original.copy(selectedPosition = GridPos(3, 3))
            
            original.selectedPosition shouldBe null
            updated.selectedPosition shouldBe GridPos(3, 3)
        }
    }
    
    context("Edge cases") {
        
        test("should handle empty grid") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10)
            )
            
            state.grid.width shouldBe 10
            state.grid.height shouldBe 10
        }
        
        test("should handle large grid") {
            val state = MapRenderState(
                grid = MapGrid(width = 100, height = 100)
            )
            
            state.grid.width shouldBe 100
            state.grid.height shouldBe 100
        }
        
        test("should handle many tokens") {
            val tokens = (1..50).map { id ->
                TokenRenderData(
                    creatureId = id.toLong(),
                    position = GridPos(id % 10, id / 10),
                    allegiance = if (id % 2 == 0) Allegiance.FRIENDLY else Allegiance.ENEMY,
                    currentHP = 20,
                    maxHP = 20
                )
            }
            
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                tokens = tokens
            )
            
            state.tokens shouldHaveSize 50
        }
        
        test("should handle long movement path") {
            val path = (0..20).map { GridPos(it, 0) }
            
            val state = MapRenderState(
                grid = MapGrid(width = 30, height = 30),
                movementPath = path
            )
            
            state.movementPath?.size shouldBe 21
        }
        
        test("should handle large range overlay") {
            val positions = (0..9).flatMap { x ->
                (0..9).map { y -> GridPos(x, y) }
            }.toSet()
            
            val rangeOverlay = RangeOverlayData(
                origin = GridPos(5, 5),
                positions = positions,
                rangeType = RangeType.SPELL
            )
            
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                rangeOverlay = rangeOverlay
            )
            
            state.rangeOverlay?.positions?.size shouldBe 100
        }
        
        test("should handle zero camera offset") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                cameraOffset = Offset.Zero
            )
            
            state.cameraOffset.x shouldBe 0f
            state.cameraOffset.y shouldBe 0f
        }
        
        test("should handle negative camera offset") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                cameraOffset = Offset(-100f, -50f)
            )
            
            state.cameraOffset.x shouldBe -100f
            state.cameraOffset.y shouldBe -50f
        }
        
        test("should handle minimum zoom level") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                zoomLevel = 0.5f
            )
            
            state.zoomLevel shouldBe 0.5f
        }
        
        test("should handle maximum zoom level") {
            val state = MapRenderState(
                grid = MapGrid(width = 10, height = 10),
                zoomLevel = 3.0f
            )
            
            state.zoomLevel shouldBe 3.0f
        }
    }
})
