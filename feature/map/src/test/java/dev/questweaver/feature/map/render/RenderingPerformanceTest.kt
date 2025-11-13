package dev.questweaver.feature.map.render

import androidx.compose.ui.geometry.Offset
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.SphereTemplate
import dev.questweaver.feature.map.ui.AoEOverlayData
import dev.questweaver.feature.map.ui.Allegiance
import dev.questweaver.feature.map.ui.MapRenderState
import dev.questweaver.feature.map.ui.RangeOverlayData
import dev.questweaver.feature.map.ui.RangeType
import dev.questweaver.feature.map.ui.TokenRenderData
import dev.questweaver.feature.map.util.CoordinateConverter
import io.kotest.core.Tag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.system.measureNanoTime

/**
 * Performance tests for map rendering components.
 * 
 * These tests benchmark the performance of rendering operations to ensure
 * they meet the target of ≤4ms per frame (60fps).
 * 
 * Note: These are unit tests that benchmark the data structures and logic
 * that support rendering. Full Canvas rendering benchmarks require instrumented
 * tests with the Android framework.
 * 
 * Performance targets (local development):
 * - Grid rendering for 50x50 grid: ≤4ms
 * - Full frame render with tokens and overlays: ≤4ms
 * - Coordinate conversions: <0.1ms for batch operations
 * - State updates: <0.5ms
 * 
 * CI Environment:
 * - Thresholds are 3x more lenient to account for variable CI hardware
 * - Warmup phases ensure JIT compilation doesn't affect results
 * - Tagged with "performance" for optional exclusion in CI
 * 
 * To skip in CI: gradle test -Dkotest.tags.exclude=performance
 */
class RenderingPerformanceTest : FunSpec({
    
    tags(Tag("performance"))
    
    // Detect CI environment and adjust thresholds accordingly
    val isCI = System.getenv("CI")?.toBoolean() ?: false
    val ciMultiplier = if (isCI) 3 else 1
    
    /**
     * Warmup function to ensure JIT compilation doesn't affect benchmark results.
     * Runs the operation multiple times before measuring.
     */
    fun <T> warmup(iterations: Int = 50, operation: () -> T) {
        repeat(iterations) { operation() }
    }
    
    context("Grid rendering performance") {
        
        test("50x50 grid state creation should be fast") {
            // Warmup JIT
            warmup {
                MapRenderState(grid = MapGrid(width = 50, height = 50))
            }
            
            val iterations = 100
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    MapRenderState(
                        grid = MapGrid(width = 50, height = 50)
                    )
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (1L * ciMultiplier) // Local: 1ms, CI: 3ms
        }
        
        test("100x100 grid state creation should be reasonable") {
            // Warmup JIT
            warmup {
                MapRenderState(grid = MapGrid(width = 100, height = 100))
            }
            
            val iterations = 100
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    MapRenderState(
                        grid = MapGrid(width = 100, height = 100)
                    )
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (5L * ciMultiplier) // Local: 5ms, CI: 15ms
        }
        
        test("viewport culling calculation should be fast") {
            val grid = MapGrid(width = 50, height = 50)
            val cellSize = 60f
            val zoomLevel = 1f
            val cameraOffset = Offset.Zero
            val viewportWidth = 1080f
            val viewportHeight = 1920f
            
            // Warmup JIT
            warmup {
                val scaledCellSize = cellSize * zoomLevel
                val visibleStartX = ((-cameraOffset.x / scaledCellSize).toInt() - 1).coerceAtLeast(0)
                val visibleEndX = (((viewportWidth - cameraOffset.x) / scaledCellSize).toInt() + 1)
                    .coerceAtMost(grid.width)
                val visibleStartY = ((-cameraOffset.y / scaledCellSize).toInt() - 1).coerceAtLeast(0)
                val visibleEndY = (((viewportHeight - cameraOffset.y) / scaledCellSize).toInt() + 1)
                    .coerceAtMost(grid.height)
                (visibleEndX - visibleStartX) * (visibleEndY - visibleStartY)
            }
            
            val iterations = 10000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    val scaledCellSize = cellSize * zoomLevel
                    
                    // Calculate visible range (viewport culling logic)
                    val visibleStartX = ((-cameraOffset.x / scaledCellSize).toInt() - 1).coerceAtLeast(0)
                    val visibleEndX = (((viewportWidth - cameraOffset.x) / scaledCellSize).toInt() + 1)
                        .coerceAtMost(grid.width)
                    val visibleStartY = ((-cameraOffset.y / scaledCellSize).toInt() - 1).coerceAtLeast(0)
                    val visibleEndY = (((viewportHeight - cameraOffset.y) / scaledCellSize).toInt() + 1)
                        .coerceAtMost(grid.height)
                    
                    // Use the calculated values to prevent optimization
                    @Suppress("UNUSED_VARIABLE")
                    val visibleCells = (visibleEndX - visibleStartX) * (visibleEndY - visibleStartY)
                }
            }
            
            val avgDurationNs = duration / iterations
            avgDurationNs shouldBeLessThan (1000L * ciMultiplier) // Local: 1μs, CI: 3μs
        }
        
        test("terrain type lookup should be fast for large grids") {
            val grid = MapGrid(width = 50, height = 50)
            
            // Warmup JIT
            warmup {
                for (x in 0 until 50) {
                    for (y in 0 until 50) {
                        val pos = GridPos(x, y)
                        val cell = grid.getCellProperties(pos)
                        cell.terrainType
                    }
                }
            }
            
            val iterations = 10000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    for (x in 0 until 50) {
                        for (y in 0 until 50) {
                            val pos = GridPos(x, y)
                            val cell = grid.getCellProperties(pos)
                            // Access terrain type
                            cell.terrainType
                        }
                    }
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (4L * ciMultiplier) // Local: 4ms, CI: 12ms
        }
    }
    
    context("Token rendering performance") {
        
        test("rendering 10 tokens should be fast") {
            val tokens = (1..10).map { id ->
                TokenRenderData(
                    creatureId = id.toLong(),
                    position = GridPos(id % 10, id / 10),
                    allegiance = if (id % 2 == 0) Allegiance.FRIENDLY else Allegiance.ENEMY,
                    currentHP = 20,
                    maxHP = 20
                )
            }
            
            // Warmup JIT
            warmup {
                MapRenderState(grid = MapGrid(width = 50, height = 50), tokens = tokens)
            }
            
            val iterations = 1000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    MapRenderState(
                        grid = MapGrid(width = 50, height = 50),
                        tokens = tokens
                    )
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (1L * ciMultiplier) // Local: 1ms, CI: 3ms
        }
        
        test("rendering 50 tokens should be reasonable") {
            val tokens = (1..50).map { id ->
                TokenRenderData(
                    creatureId = id.toLong(),
                    position = GridPos(id % 10, id / 10),
                    allegiance = when (id % 3) {
                        0 -> Allegiance.FRIENDLY
                        1 -> Allegiance.ENEMY
                        else -> Allegiance.NEUTRAL
                    },
                    currentHP = 15,
                    maxHP = 30
                )
            }
            
            // Warmup JIT
            warmup {
                MapRenderState(grid = MapGrid(width = 50, height = 50), tokens = tokens)
            }
            
            val iterations = 1000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    MapRenderState(
                        grid = MapGrid(width = 50, height = 50),
                        tokens = tokens
                    )
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (2L * ciMultiplier) // Local: 2ms, CI: 6ms
        }
        
        test("token HP calculations should be fast") {
            val tokens = (1..50).map { id ->
                TokenRenderData(
                    creatureId = id.toLong(),
                    position = GridPos(id % 10, id / 10),
                    allegiance = if (id % 2 == 0) Allegiance.FRIENDLY else Allegiance.ENEMY,
                    currentHP = id,
                    maxHP = 30
                )
            }
            
            // Warmup JIT
            warmup {
                tokens.forEach { token ->
                    token.showHP
                    token.isBloodied
                    token.hpPercentage
                }
            }
            
            val iterations = 10000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    tokens.forEach { token ->
                        // Access computed properties
                        token.showHP
                        token.isBloodied
                        token.hpPercentage
                    }
                }
            }
            
            val avgDurationNs = duration / iterations
            avgDurationNs shouldBeLessThan (10000L * ciMultiplier) // Local: 10μs, CI: 30μs
        }
    }
    
    context("Full frame rendering performance") {
        
        test("full state with all layers should meet 4ms target") {
            val grid = MapGrid(width = 50, height = 50)
            
            val tokens = (1..20).map { id ->
                TokenRenderData(
                    creatureId = id.toLong(),
                    position = GridPos(id % 10, id / 10),
                    allegiance = if (id % 2 == 0) Allegiance.FRIENDLY else Allegiance.ENEMY,
                    currentHP = 20,
                    maxHP = 20
                )
            }
            
            val movementPath = (0..10).map { GridPos(it, 5) }
            
            val rangeOverlay = RangeOverlayData(
                origin = GridPos(5, 5),
                positions = (0..9).flatMap { x ->
                    (0..9).map { y -> GridPos(x, y) }
                }.toSet(),
                rangeType = RangeType.MOVEMENT
            )
            
            val aoeOverlay = AoEOverlayData(
                template = SphereTemplate(radiusInFeet = 20),
                origin = GridPos(10, 10),
                affectedPositions = (8..12).flatMap { x ->
                    (8..12).map { y -> GridPos(x, y) }
                }.toSet()
            )
            
            // Warmup JIT
            warmup {
                MapRenderState(
                    grid = grid,
                    tokens = tokens,
                    movementPath = movementPath,
                    rangeOverlay = rangeOverlay,
                    aoeOverlay = aoeOverlay,
                    selectedPosition = GridPos(5, 5),
                    cameraOffset = Offset(100f, 100f),
                    zoomLevel = 1.5f
                )
            }
            
            val iterations = 100
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    MapRenderState(
                        grid = grid,
                        tokens = tokens,
                        movementPath = movementPath,
                        rangeOverlay = rangeOverlay,
                        aoeOverlay = aoeOverlay,
                        selectedPosition = GridPos(5, 5),
                        cameraOffset = Offset(100f, 100f),
                        zoomLevel = 1.5f
                    )
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (4L * ciMultiplier) // Local: 4ms, CI: 12ms
        }
        
        test("state updates should be fast") {
            val original = MapRenderState(
                grid = MapGrid(width = 50, height = 50),
                tokens = (1..20).map { id ->
                    TokenRenderData(
                        creatureId = id.toLong(),
                        position = GridPos(id % 10, id / 10),
                        allegiance = if (id % 2 == 0) Allegiance.FRIENDLY else Allegiance.ENEMY,
                        currentHP = 20,
                        maxHP = 20
                    )
                }
            )
            
            // Warmup JIT
            warmup {
                original.copy(
                    cameraOffset = Offset(100f, 100f),
                    zoomLevel = 2f,
                    selectedPosition = GridPos(10, 10)
                )
            }
            
            val iterations = 1000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    original.copy(
                        cameraOffset = Offset(100f, 100f),
                        zoomLevel = 2f,
                        selectedPosition = GridPos(10, 10)
                    )
                }
            }
            
            val avgDurationNs = duration / iterations
            avgDurationNs shouldBeLessThan (500000L * ciMultiplier) // Local: 0.5ms, CI: 1.5ms
        }
    }
    
    context("Coordinate conversion performance") {
        
        test("grid to screen conversion should be fast") {
            val cellSize = 60f
            val cameraOffset = Offset(100f, 100f)
            val zoomLevel = 1.5f
            
            // Warmup JIT
            warmup {
                for (x in 0 until 50) {
                    for (y in 0 until 50) {
                        CoordinateConverter.gridToScreen(
                            GridPos(x, y),
                            cellSize,
                            cameraOffset,
                            zoomLevel
                        )
                    }
                }
            }
            
            val iterations = 10000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    for (x in 0 until 50) {
                        for (y in 0 until 50) {
                            CoordinateConverter.gridToScreen(
                                GridPos(x, y),
                                cellSize,
                                cameraOffset,
                                zoomLevel
                            )
                        }
                    }
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (4L * ciMultiplier) // Local: 4ms, CI: 12ms
        }
        
        test("screen to grid conversion should be fast") {
            val state = MapRenderState(
                grid = MapGrid(width = 50, height = 50),
                cameraOffset = Offset(100f, 100f),
                zoomLevel = 1.5f
            )
            val cellSize = 60f
            
            // Warmup JIT
            warmup {
                for (x in 0 until 1080 step 60) {
                    for (y in 0 until 1920 step 60) {
                        CoordinateConverter.screenToGrid(
                            Offset(x.toFloat(), y.toFloat()),
                            state,
                            cellSize
                        )
                    }
                }
            }
            
            val iterations = 10000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    for (x in 0 until 1080 step 60) {
                        for (y in 0 until 1920 step 60) {
                            CoordinateConverter.screenToGrid(
                                Offset(x.toFloat(), y.toFloat()),
                                state,
                                cellSize
                            )
                        }
                    }
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (4L * ciMultiplier) // Local: 4ms, CI: 12ms
        }
        
        test("batch coordinate conversions should be efficient") {
            val positions = (0 until 50).flatMap { x ->
                (0 until 50).map { y -> GridPos(x, y) }
            }
            
            val cellSize = 60f
            val cameraOffset = Offset.Zero
            val zoomLevel = 1f
            
            // Warmup JIT
            warmup {
                positions.map { pos ->
                    CoordinateConverter.gridToScreen(pos, cellSize, cameraOffset, zoomLevel)
                }
            }
            
            val iterations = 100
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    positions.map { pos ->
                        CoordinateConverter.gridToScreen(pos, cellSize, cameraOffset, zoomLevel)
                    }
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (4L * ciMultiplier) // Local: 4ms, CI: 12ms
        }
    }
    
    context("Pan and zoom gesture performance") {
        
        test("camera offset updates should be fast") {
            val state = MapRenderState(
                grid = MapGrid(width = 50, height = 50),
                cameraOffset = Offset.Zero
            )
            
            // Warmup JIT
            warmup {
                state.copy(cameraOffset = state.cameraOffset + Offset(10f, 10f))
            }
            
            val iterations = 10000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    state.copy(cameraOffset = state.cameraOffset + Offset(10f, 10f))
                }
            }
            
            val avgDurationNs = duration / iterations
            avgDurationNs shouldBeLessThan (100000L * ciMultiplier) // Local: 0.1ms, CI: 0.3ms
        }
        
        test("zoom level updates should be fast") {
            val state = MapRenderState(
                grid = MapGrid(width = 50, height = 50),
                zoomLevel = 1f
            )
            
            // Warmup JIT
            warmup {
                val newZoom = (state.zoomLevel * 1.1f).coerceIn(0.5f, 3.0f)
                state.copy(zoomLevel = newZoom)
            }
            
            val iterations = 10000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    val newZoom = (state.zoomLevel * 1.1f).coerceIn(0.5f, 3.0f)
                    state.copy(zoomLevel = newZoom)
                }
            }
            
            val avgDurationNs = duration / iterations
            avgDurationNs shouldBeLessThan (100000L * ciMultiplier) // Local: 0.1ms, CI: 0.3ms
        }
        
        test("simulated pan gesture sequence should be smooth") {
            var state = MapRenderState(
                grid = MapGrid(width = 50, height = 50),
                tokens = (1..20).map { id ->
                    TokenRenderData(
                        creatureId = id.toLong(),
                        position = GridPos(id % 10, id / 10),
                        allegiance = if (id % 2 == 0) Allegiance.FRIENDLY else Allegiance.ENEMY,
                        currentHP = 20,
                        maxHP = 20
                    )
                }
            )
            
            // Warmup JIT
            warmup(10) {
                state = state.copy(cameraOffset = state.cameraOffset + Offset(5f, 5f))
            }
            
            // Simulate 60 frames of panning (1 second at 60fps)
            val frames = 60
            
            val duration = measureNanoTime {
                repeat(frames) {
                    state = state.copy(
                        cameraOffset = state.cameraOffset + Offset(5f, 5f)
                    )
                }
            }
            
            val avgFrameTimeMs = duration / frames / 1_000_000
            avgFrameTimeMs shouldBeLessThan (4L * ciMultiplier) // Local: 4ms, CI: 12ms
        }
        
        test("simulated zoom gesture sequence should be smooth") {
            var state = MapRenderState(
                grid = MapGrid(width = 50, height = 50),
                tokens = (1..20).map { id ->
                    TokenRenderData(
                        creatureId = id.toLong(),
                        position = GridPos(id % 10, id / 10),
                        allegiance = if (id % 2 == 0) Allegiance.FRIENDLY else Allegiance.ENEMY,
                        currentHP = 20,
                        maxHP = 20
                    )
                },
                zoomLevel = 1f
            )
            
            // Warmup JIT
            warmup(10) {
                val newZoom = (state.zoomLevel * 1.02f).coerceIn(0.5f, 3.0f)
                state = state.copy(zoomLevel = newZoom)
            }
            
            // Simulate 60 frames of zooming (1 second at 60fps)
            val frames = 60
            
            val duration = measureNanoTime {
                repeat(frames) {
                    val newZoom = (state.zoomLevel * 1.02f).coerceIn(0.5f, 3.0f)
                    state = state.copy(zoomLevel = newZoom)
                }
            }
            
            val avgFrameTimeMs = duration / frames / 1_000_000
            avgFrameTimeMs shouldBeLessThan (4L * ciMultiplier) // Local: 4ms, CI: 12ms
        }
    }
    
    context("Memory allocation performance") {
        
        test("state creation should not allocate excessive memory").config(enabled = !isCI) {
            // Memory tests are unreliable in CI due to GC timing - skip in CI
            val grid = MapGrid(width = 50, height = 50)
            val tokens = (1..20).map { id ->
                TokenRenderData(
                    creatureId = id.toLong(),
                    position = GridPos(id % 10, id / 10),
                    allegiance = if (id % 2 == 0) Allegiance.FRIENDLY else Allegiance.ENEMY,
                    currentHP = 20,
                    maxHP = 20
                )
            }
            
            // Warm up and trigger GC
            repeat(100) {
                MapRenderState(grid = grid, tokens = tokens)
            }
            System.gc()
            Thread.sleep(100)
            
            // Measure allocations
            val iterations = 1000
            val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            
            repeat(iterations) {
                MapRenderState(grid = grid, tokens = tokens)
            }
            
            val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memoryPerIteration = (endMemory - startMemory) / iterations
            
            // Should allocate less than 10KB per state creation
            memoryPerIteration shouldBeLessThan 10240L
        }
        
        test("overlay data should be memory efficient") {
            val positions = (0..9).flatMap { x ->
                (0..9).map { y -> GridPos(x, y) }
            }.toSet()
            
            // Warmup JIT
            warmup {
                RangeOverlayData(
                    origin = GridPos(5, 5),
                    positions = positions,
                    rangeType = RangeType.MOVEMENT
                )
            }
            
            val iterations = 1000
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    RangeOverlayData(
                        origin = GridPos(5, 5),
                        positions = positions,
                        rangeType = RangeType.MOVEMENT
                    )
                }
            }
            
            val avgDurationNs = duration / iterations
            avgDurationNs shouldBeLessThan (50000L * ciMultiplier) // Local: 0.05ms, CI: 0.15ms
        }
    }
    
    context("Large grid stress tests") {
        
        test("100x100 grid with 50 tokens should be reasonable") {
            val grid = MapGrid(width = 100, height = 100)
            val tokens = (1..50).map { id ->
                TokenRenderData(
                    creatureId = id.toLong(),
                    position = GridPos(id % 20, id / 20),
                    allegiance = when (id % 3) {
                        0 -> Allegiance.FRIENDLY
                        1 -> Allegiance.ENEMY
                        else -> Allegiance.NEUTRAL
                    },
                    currentHP = 20,
                    maxHP = 20
                )
            }
            
            // Warmup JIT
            warmup {
                MapRenderState(grid = grid, tokens = tokens)
            }
            
            val iterations = 100
            
            val duration = measureNanoTime {
                repeat(iterations) {
                    MapRenderState(
                        grid = grid,
                        tokens = tokens
                    )
                }
            }
            
            val avgDurationMs = duration / iterations / 1_000_000
            avgDurationMs shouldBeLessThan (10L * ciMultiplier) // Local: 10ms, CI: 30ms
        }
        
        test("viewport culling should reduce work for large grids") {
            val grid = MapGrid(width = 100, height = 100)
            val cellSize = 60f
            val zoomLevel = 1f
            val cameraOffset = Offset.Zero
            val viewportWidth = 1080f
            val viewportHeight = 1920f
            
            val scaledCellSize = cellSize * zoomLevel
            
            // Calculate visible range
            val visibleStartX = ((-cameraOffset.x / scaledCellSize).toInt() - 1).coerceAtLeast(0)
            val visibleEndX = (((viewportWidth - cameraOffset.x) / scaledCellSize).toInt() + 1)
                .coerceAtMost(grid.width)
            val visibleStartY = ((-cameraOffset.y / scaledCellSize).toInt() - 1).coerceAtLeast(0)
            val visibleEndY = (((viewportHeight - cameraOffset.y) / scaledCellSize).toInt() + 1)
                .coerceAtMost(grid.height)
            
            val visibleCells = (visibleEndX - visibleStartX) * (visibleEndY - visibleStartY)
            val totalCells = grid.width * grid.height
            
            // Viewport culling should significantly reduce the number of cells to render
            visibleCells.toLong() shouldBeLessThan (totalCells / 2).toLong()
        }
    }
})

