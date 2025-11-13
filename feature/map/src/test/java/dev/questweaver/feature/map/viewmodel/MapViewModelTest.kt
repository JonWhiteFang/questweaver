package dev.questweaver.feature.map.viewmodel

import androidx.compose.ui.geometry.Offset
import dev.questweaver.domain.map.geometry.AoETemplate
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.pathfinding.Pathfinder
import dev.questweaver.domain.map.pathfinding.ReachabilityCalculator
import dev.questweaver.feature.map.ui.MapIntent
import dev.questweaver.feature.map.ui.RangeType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest : FunSpec({
    
    val testDispatcher = StandardTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    context("CellTapped intent") {
        test("updates selectedPosition") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            val position = GridPos(5, 10)
            viewModel.handle(MapIntent.CellTapped(position))
            
            viewModel.state.value.selectedPosition shouldBe position
        }
        
        test("replaces previous selectedPosition") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            viewModel.handle(MapIntent.CellTapped(GridPos(3, 4)))
            viewModel.handle(MapIntent.CellTapped(GridPos(7, 8)))
            
            viewModel.state.value.selectedPosition shouldBe GridPos(7, 8)
        }
    }
    
    context("Pan intent") {
        test("updates cameraOffset") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            val delta = Offset(100f, 200f)
            viewModel.handle(MapIntent.Pan(delta))
            
            viewModel.state.value.cameraOffset shouldBe Offset(100f, 200f)
        }
        
        test("accumulates multiple pan deltas") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            viewModel.handle(MapIntent.Pan(Offset(50f, 100f)))
            viewModel.handle(MapIntent.Pan(Offset(30f, 40f)))
            
            viewModel.state.value.cameraOffset shouldBe Offset(80f, 140f)
        }
        
        test("handles negative pan deltas") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            viewModel.handle(MapIntent.Pan(Offset(100f, 100f)))
            viewModel.handle(MapIntent.Pan(Offset(-50f, -30f)))
            
            viewModel.state.value.cameraOffset shouldBe Offset(50f, 70f)
        }
    }
    
    context("Zoom intent") {
        test("updates zoomLevel") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            viewModel.handle(MapIntent.Zoom(scale = 2f, focus = Offset.Zero))
            
            viewModel.state.value.zoomLevel shouldBe 2f
        }
        
        test("clamps zoomLevel to minimum 0.5x") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            viewModel.handle(MapIntent.Zoom(scale = 0.1f, focus = Offset.Zero))
            
            viewModel.state.value.zoomLevel shouldBe 0.5f
        }
        
        test("clamps zoomLevel to maximum 3.0x") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            viewModel.handle(MapIntent.Zoom(scale = 5f, focus = Offset.Zero))
            
            viewModel.state.value.zoomLevel shouldBe 3f
        }
        
        test("accumulates zoom scale factors") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            viewModel.handle(MapIntent.Zoom(scale = 1.5f, focus = Offset.Zero))
            viewModel.handle(MapIntent.Zoom(scale = 1.5f, focus = Offset.Zero))
            
            viewModel.state.value.zoomLevel shouldBe 2.25f
        }
    }
    
    context("showMovementRange") {
        test("updates rangeOverlay with movement type") {
            runTest {
                val pathfinder = mockk<Pathfinder>()
                val reachabilityCalculator = mockk<ReachabilityCalculator>()
                
                val origin = GridPos(5, 5)
                val reachablePositions = setOf(
                    GridPos(5, 6),
                    GridPos(6, 5),
                    GridPos(4, 5)
                )
                
                coEvery {
                    reachabilityCalculator.findReachablePositions(
                        start = origin,
                        movementBudget = 30,
                        grid = any()
                    )
                } returns reachablePositions
                
                val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
                
                viewModel.showMovementRange(origin, 30)
                advanceUntilIdle()
                
                val overlay = viewModel.state.value.rangeOverlay
                overlay.shouldNotBeNull()
                overlay.origin shouldBe origin
                overlay.rangeType shouldBe RangeType.MOVEMENT
                overlay.positions shouldBe reachablePositions
            }
        }
    }
    
    context("showWeaponRange") {
        test("updates rangeOverlay with weapon type") {
            runTest {
                val pathfinder = mockk<Pathfinder>()
                val reachabilityCalculator = mockk<ReachabilityCalculator>()
                val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
                
                val origin = GridPos(10, 10)
                viewModel.showWeaponRange(origin, 30)
                advanceUntilIdle()
                
                val overlay = viewModel.state.value.rangeOverlay
                overlay.shouldNotBeNull()
                overlay.origin shouldBe origin
                overlay.rangeType shouldBe RangeType.WEAPON
            }
        }
    }
    
    context("showSpellRange") {
        test("updates rangeOverlay with spell type") {
            runTest {
                val pathfinder = mockk<Pathfinder>()
                val reachabilityCalculator = mockk<ReachabilityCalculator>()
                val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
                
                val origin = GridPos(10, 10)
                viewModel.showSpellRange(origin, 60)
                advanceUntilIdle()
                
                val overlay = viewModel.state.value.rangeOverlay
                overlay.shouldNotBeNull()
                overlay.origin shouldBe origin
                overlay.rangeType shouldBe RangeType.SPELL
            }
        }
    }
    
    context("showAoEPreview") {
        test("updates aoeOverlay") {
            runTest {
                val pathfinder = mockk<Pathfinder>()
                val reachabilityCalculator = mockk<ReachabilityCalculator>()
                
                val origin = GridPos(10, 10)
                val affectedPositions = setOf(
                    GridPos(10, 10),
                    GridPos(11, 10),
                    GridPos(10, 11)
                )
                
                val template = mockk<AoETemplate>()
                every {
                    template.affectedPositions(origin, any())
                } returns affectedPositions
                
                val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
                
                viewModel.showAoEPreview(template, origin)
                advanceUntilIdle()
                
                val overlay = viewModel.state.value.aoeOverlay
                overlay.shouldNotBeNull()
                overlay.origin shouldBe origin
                overlay.template shouldBe template
                overlay.affectedPositions shouldBe affectedPositions
            }
        }
    }
    
    context("clearRangeOverlay") {
        test("removes rangeOverlay from state") {
            runTest {
                val pathfinder = mockk<Pathfinder>()
                val reachabilityCalculator = mockk<ReachabilityCalculator>()
                
                coEvery {
                    reachabilityCalculator.findReachablePositions(any(), any(), any())
                } returns setOf(GridPos(5, 5))
                
                val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
                
                viewModel.showMovementRange(GridPos(5, 5), 30)
                advanceUntilIdle()
                viewModel.state.value.rangeOverlay.shouldNotBeNull()
                
                viewModel.clearRangeOverlay()
                viewModel.state.value.rangeOverlay.shouldBeNull()
            }
        }
    }
    
    context("clearAoEOverlay") {
        test("removes aoeOverlay from state") {
            runTest {
                val pathfinder = mockk<Pathfinder>()
                val reachabilityCalculator = mockk<ReachabilityCalculator>()
                
                val template = mockk<AoETemplate>()
                every {
                    template.affectedPositions(any(), any())
                } returns setOf(GridPos(10, 10))
                
                val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
                
                viewModel.showAoEPreview(template, GridPos(10, 10))
                advanceUntilIdle()
                viewModel.state.value.aoeOverlay.shouldNotBeNull()
                
                viewModel.clearAoEOverlay()
                viewModel.state.value.aoeOverlay.shouldBeNull()
            }
        }
    }
    
    context("state immutability") {
        test("state updates create new instances") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            val initialState = viewModel.state.value
            
            viewModel.handle(MapIntent.CellTapped(GridPos(5, 5)))
            val updatedState = viewModel.state.value
            
            initialState shouldBe initialState
            updatedState shouldBe updatedState
            // Verify they are different instances
            (initialState === updatedState) shouldBe false
        }
        
        test("grid reference remains unchanged when not updated") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            val initialGrid = viewModel.state.value.grid
            
            viewModel.handle(MapIntent.Pan(Offset(10f, 10f)))
            val updatedGrid = viewModel.state.value.grid
            
            // Grid should be the same reference since it wasn't updated
            (initialGrid === updatedGrid) shouldBe true
        }
    }
    
    context("updateGrid") {
        test("replaces grid in state") {
            val pathfinder = mockk<Pathfinder>()
            val reachabilityCalculator = mockk<ReachabilityCalculator>()
            val viewModel = MapViewModel(pathfinder, reachabilityCalculator)
            
            val newGrid = MapGrid(width = 30, height = 30)
            viewModel.updateGrid(newGrid)
            
            viewModel.state.value.grid shouldBe newGrid
        }
    }
})
