package dev.questweaver.feature.map.util

import androidx.compose.ui.geometry.Offset
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.feature.map.ui.MapRenderState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class CoordinateConverterTest : FunSpec({
    
    context("gridToScreen conversion") {
        test("converts grid position to screen coordinates with zoom 1x") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset.Zero
            val zoomLevel = 1f
            
            val result = CoordinateConverter.gridToScreen(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(300f, 600f)
        }
        
        test("converts grid position to screen coordinates with zoom 2x") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset.Zero
            val zoomLevel = 2f
            
            val result = CoordinateConverter.gridToScreen(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(600f, 1200f)
        }
        
        test("converts grid position to screen coordinates with zoom 0.5x") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset.Zero
            val zoomLevel = 0.5f
            
            val result = CoordinateConverter.gridToScreen(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(150f, 300f)
        }
        
        test("converts grid position to screen coordinates with zoom 3x") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset.Zero
            val zoomLevel = 3f
            
            val result = CoordinateConverter.gridToScreen(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(900f, 1800f)
        }
        
        test("converts grid position with camera offset") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset(100f, 200f)
            val zoomLevel = 1f
            
            val result = CoordinateConverter.gridToScreen(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(400f, 800f)
        }
        
        test("converts grid position with negative camera offset") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset(-50f, -100f)
            val zoomLevel = 1f
            
            val result = CoordinateConverter.gridToScreen(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(250f, 500f)
        }
        
        test("converts origin position (0,0)") {
            val gridPos = GridPos(0, 0)
            val cellSize = 60f
            val cameraOffset = Offset.Zero
            val zoomLevel = 1f
            
            val result = CoordinateConverter.gridToScreen(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset.Zero
        }
    }
    
    context("screenToGrid conversion") {
        test("converts screen position to grid coordinates with valid position") {
            val screenPos = Offset(300f, 600f)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset.Zero,
                zoomLevel = 1f
            )
            
            val result = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            result.shouldNotBeNull()
            result shouldBe GridPos(5, 10)
        }
        
        test("converts screen position with camera offset") {
            val screenPos = Offset(400f, 800f)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset(100f, 200f),
                zoomLevel = 1f
            )
            
            val result = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            result.shouldNotBeNull()
            result shouldBe GridPos(5, 10)
        }
        
        test("converts screen position with zoom 2x") {
            val screenPos = Offset(600f, 1200f)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset.Zero,
                zoomLevel = 2f
            )
            
            val result = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            result.shouldNotBeNull()
            result shouldBe GridPos(5, 10)
        }
        
        test("returns null for out-of-bounds position (negative x)") {
            val screenPos = Offset(-100f, 300f)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset.Zero,
                zoomLevel = 1f
            )
            
            val result = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            result.shouldBeNull()
        }
        
        test("returns null for out-of-bounds position (negative y)") {
            val screenPos = Offset(300f, -100f)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset.Zero,
                zoomLevel = 1f
            )
            
            val result = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            result.shouldBeNull()
        }
        
        test("returns null for out-of-bounds position (x too large)") {
            val screenPos = Offset(1300f, 600f)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset.Zero,
                zoomLevel = 1f
            )
            
            val result = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            result.shouldBeNull()
        }
        
        test("returns null for out-of-bounds position (y too large)") {
            val screenPos = Offset(300f, 1300f)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset.Zero,
                zoomLevel = 1f
            )
            
            val result = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            result.shouldBeNull()
        }
    }
    
    context("round-trip conversion") {
        test("grid to screen to grid returns original position") {
            val originalPos = GridPos(7, 13)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset.Zero,
                zoomLevel = 1f
            )
            
            val screenPos = CoordinateConverter.gridToScreen(
                originalPos, cellSize, state.cameraOffset, state.zoomLevel
            )
            val resultPos = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            resultPos.shouldNotBeNull()
            resultPos shouldBe originalPos
        }
        
        test("round-trip with zoom 2x") {
            val originalPos = GridPos(7, 13)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset.Zero,
                zoomLevel = 2f
            )
            
            val screenPos = CoordinateConverter.gridToScreen(
                originalPos, cellSize, state.cameraOffset, state.zoomLevel
            )
            val resultPos = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            resultPos.shouldNotBeNull()
            resultPos shouldBe originalPos
        }
        
        test("round-trip with camera offset") {
            val originalPos = GridPos(7, 13)
            val cellSize = 60f
            val state = MapRenderState(
                grid = MapGrid(width = 20, height = 20),
                cameraOffset = Offset(150f, 250f),
                zoomLevel = 1f
            )
            
            val screenPos = CoordinateConverter.gridToScreen(
                originalPos, cellSize, state.cameraOffset, state.zoomLevel
            )
            val resultPos = CoordinateConverter.screenToGrid(screenPos, state, cellSize)
            
            resultPos.shouldNotBeNull()
            resultPos shouldBe originalPos
        }
    }
    
    context("gridToScreenCenter conversion") {
        test("calculates center point of cell") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset.Zero
            val zoomLevel = 1f
            
            val result = CoordinateConverter.gridToScreenCenter(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(330f, 630f)
        }
        
        test("calculates center point with zoom 2x") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset.Zero
            val zoomLevel = 2f
            
            val result = CoordinateConverter.gridToScreenCenter(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(660f, 1260f)
        }
        
        test("calculates center point with camera offset") {
            val gridPos = GridPos(5, 10)
            val cellSize = 60f
            val cameraOffset = Offset(100f, 200f)
            val zoomLevel = 1f
            
            val result = CoordinateConverter.gridToScreenCenter(
                gridPos, cellSize, cameraOffset, zoomLevel
            )
            
            result shouldBe Offset(430f, 830f)
        }
    }
})
