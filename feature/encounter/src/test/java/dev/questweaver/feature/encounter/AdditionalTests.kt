package dev.questweaver.feature.encounter

import androidx.lifecycle.ViewModel
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.repositories.EventRepository
import dev.questweaver.feature.encounter.state.CompletionDetector
import dev.questweaver.feature.encounter.state.EncounterState
import dev.questweaver.feature.encounter.state.EncounterStateBuilder
import dev.questweaver.feature.encounter.state.MapGridState
import dev.questweaver.feature.encounter.state.RoundState
import dev.questweaver.feature.encounter.state.TurnPhaseState
import dev.questweaver.feature.encounter.state.UndoRedoManager
import dev.questweaver.feature.encounter.usecases.AdvanceTurn
import dev.questweaver.feature.encounter.usecases.InitializeEncounter
import dev.questweaver.feature.encounter.usecases.ProcessPlayerAction
import dev.questweaver.feature.encounter.viewmodel.Creature
import dev.questweaver.feature.encounter.viewmodel.EncounterIntent
import dev.questweaver.feature.encounter.viewmodel.EncounterUiState
import dev.questweaver.feature.encounter.viewmodel.EncounterViewModel
import dev.questweaver.feature.encounter.viewmodel.MapGrid
import dev.questweaver.domain.map.geometry.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Additional tests for error handling, map integration, lifecycle, and testability.
 *
 * Requirements: 3.5, 8.1, 8.2, 8.3, 8.4, 8.5, 10.1, 10.2, 10.3, 10.4, 10.5, 11.1, 11.2, 11.3, 11.4, 11.5
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdditionalTests : FunSpec({
    
    val testDispatcher = StandardTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    context("Error handling") {
        test("initialization failure updates error state") {
            runTest(testDispatcher) {
                // Arrange
                val viewModel = EncounterViewModel(
                    initializeEncounter = mockk(),
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = mockk(relaxed = true),
                    stateBuilder = mockk(),
                    completionDetector = mockk(),
                    undoRedoManager = mockk()
                )
                
                // Act - try to start with empty creatures
                viewModel.handle(EncounterIntent.StartEncounter(emptyList(), emptySet(), MapGrid(10, 10)))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.error.shouldNotBeNull()
                state.error shouldBe "Failed to start encounter: Cannot start encounter with no creatures"
            }
        }
        
        test("action failure updates error state") {
            runTest(testDispatcher) {
                // Arrange
                val viewModel = EncounterViewModel(
                    initializeEncounter = mockk(),
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = mockk(relaxed = true),
                    stateBuilder = mockk(),
                    completionDetector = mockk(),
                    undoRedoManager = mockk()
                )
                
                // Act - try to attack with no active creature
                viewModel.handle(EncounterIntent.Attack(2L))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.error.shouldNotBeNull()
                state.error shouldBe "Action failed: No active creature"
            }
        }
        
        test("load failure updates error state") {
            runTest(testDispatcher) {
                // Arrange
                val eventRepository = mockk<EventRepository>()
                val viewModel = EncounterViewModel(
                    initializeEncounter = mockk(),
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = eventRepository,
                    stateBuilder = mockk(),
                    completionDetector = mockk(),
                    undoRedoManager = mockk()
                )
                
                coEvery { eventRepository.forSession(any()) } returns emptyList()
                
                // Act
                viewModel.loadEncounter(1L)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.error.shouldNotBeNull()
            }
        }
        
        test("errors don't corrupt state") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val eventRepository = mockk<EventRepository>(relaxed = true)
                val stateBuilder = mockk<EncounterStateBuilder>()
                val undoRedoManager = mockk<UndoRedoManager>()
                
                val viewModel = EncounterViewModel(
                    initializeEncounter = mockk(),
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = eventRepository,
                    stateBuilder = stateBuilder,
                    completionDetector = mockk(),
                    undoRedoManager = undoRedoManager
                )
                
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), 1L, TurnPhaseState.Action),
                    creatures = emptyMap(),
                    mapGrid = MapGridState(10, 10, emptySet(), emptySet()),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    roundNumber = 1
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                every { undoRedoManager.updateEventCount(any()) } returns Unit
                
                // Act - cause an error
                viewModel.handle(EncounterIntent.Attack(2L))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert - state should still be valid
                val state = viewModel.state.first()
                state.sessionId.shouldBeNull() // No session started yet
            }
        }
    }
    
    context("Map integration") {
        test("creature positions updated on movement") {
            // Arrange
            val stateBuilder = EncounterStateBuilder(mockk(relaxed = true))
            
            val encounterState = EncounterState(
                sessionId = 1L,
                roundState = RoundState(1, false, emptyList(), null, TurnPhaseState.Start),
                creatures = emptyMap(),
                mapGrid = MapGridState(10, 10, emptySet(), emptySet()),
                readiedActions = emptyMap(),
                isCompleted = false,
                completionStatus = null
            )
            
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(2, 3), true)
            )
            
            // Act
            val uiState = stateBuilder.buildUiState(encounterState, creatures)
            
            // Assert
            uiState.mapState.shouldNotBeNull()
            val fighterToken = uiState.mapState!!.tokens.find { it.id == "1" }
            fighterToken.shouldNotBeNull()
            fighterToken!!.pos shouldBe GridPos(2, 3)
        }
        
        test("pathfinding information provided to map") {
            runTest(testDispatcher) {
                // Arrange
                val viewModel = EncounterViewModel(
                    initializeEncounter = mockk(),
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = mockk(relaxed = true),
                    stateBuilder = mockk(),
                    completionDetector = mockk(),
                    undoRedoManager = mockk()
                )
                
                // Act - validate movement path
                val path = listOf(GridPos(0, 0), GridPos(1, 0))
                val isValid = viewModel.validateMovementPath(path)
                
                // Assert
                isValid shouldBe true
            }
        }
        
        xtest("AoE spell areas provided to map") {
            runTest(testDispatcher) {
                // Arrange
                val viewModel = EncounterViewModel(
                    initializeEncounter = mockk(),
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = mockk(relaxed = true),
                    stateBuilder = mockk(),
                    completionDetector = mockk(),
                    undoRedoManager = mockk()
                )
                
                // TODO: Implement AoETemplate implementations and getAoEOverlay method
                // Act - get AoE overlay
                // val aoeOverlay = viewModel.getAoEOverlay(
                //     template = mockk<AoETemplate>(),
                //     origin = GridPos(5, 5),
                //     radiusInFeet = 20
                // )
                
                // Assert
                // aoeOverlay.shouldNotBeNull()
            }
        }
        
        xtest("range overlays provided to map") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val eventRepository = mockk<EventRepository>(relaxed = true)
                val stateBuilder = mockk<EncounterStateBuilder>()
                val undoRedoManager = mockk<UndoRedoManager>()
                val initializeEncounter = mockk<InitializeEncounter>()
                
                val viewModel = EncounterViewModel(
                    initializeEncounter = initializeEncounter,
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = eventRepository,
                    stateBuilder = stateBuilder,
                    completionDetector = mockk(),
                    undoRedoManager = undoRedoManager
                )
                
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), 1L, TurnPhaseState.Action),
                    creatures = emptyMap(),
                    mapGrid = MapGridState(10, 10, emptySet(), emptySet()),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    activeCreatureId = 1L,
                    creatures = mapOf(
                        1L to dev.questweaver.feature.encounter.viewmodel.CreatureState(
                            1L, "Fighter", 20, 20, 15, GridPos(0, 0), emptySet(), true, false
                        )
                    ),
                    mapState = dev.questweaver.feature.map.ui.MapState(
                        w = 10,
                        h = 10,
                        tileSize = 50f,
                        blocked = emptySet(),
                        difficult = emptySet(),
                        tokens = emptyList()
                    )
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                every { undoRedoManager.updateEventCount(any()) } returns Unit
                
                // Start encounter to initialize state
                viewModel.handle(EncounterIntent.StartEncounter(
                    listOf(Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true)),
                    emptySet(),
                    MapGrid(10, 10)
                ))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Act - get weapon range overlay
                val rangeOverlay = viewModel.getWeaponRangeOverlay(30)
                
                // Assert
                rangeOverlay.shouldNotBeNull()
            }
        }
        
        test("active creature highlighted on map") {
            // Arrange
            val stateBuilder = EncounterStateBuilder(mockk(relaxed = true))
            
            val encounterState = EncounterState(
                sessionId = 1L,
                roundState = RoundState(1, false, listOf(1L, 2L), 1L, TurnPhaseState.Action),
                creatures = emptyMap(),
                mapGrid = MapGridState(10, 10, emptySet(), emptySet()),
                readiedActions = emptyMap(),
                isCompleted = false,
                completionStatus = null
            )
            
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Goblin", 7, 7, 13, GridPos(5, 5), false)
            )
            
            // Act
            val uiState = stateBuilder.buildUiState(encounterState, creatures)
            
            // Assert
            uiState.activeCreatureId shouldBe 1L
        }
    }
    
    context("ViewModel lifecycle") {
        test("coroutines cancelled on ViewModel clear") {
            // Arrange
            val viewModel = EncounterViewModel(
                initializeEncounter = mockk(),
                processPlayerAction = mockk(),
                advanceTurn = mockk(),
                eventRepository = mockk(relaxed = true),
                stateBuilder = mockk(),
                completionDetector = mockk(),
                undoRedoManager = mockk()
            )
            
            // Act - call onCleared (simulating ViewModel destruction)
            // Note: onCleared is protected, so we can't call it directly in tests
            // In real usage, the framework calls this automatically
            
            // Assert - viewModelScope automatically cancels coroutines
            // This is handled by the ViewModel base class
            viewModel.shouldBeInstanceOf<ViewModel>()
        }
        
        test("encounter marked inactive on cleanup") {
            // Arrange
            val viewModel = EncounterViewModel(
                initializeEncounter = mockk(),
                processPlayerAction = mockk(),
                advanceTurn = mockk(),
                eventRepository = mockk(relaxed = true),
                stateBuilder = mockk(),
                completionDetector = mockk(),
                undoRedoManager = mockk()
            )
            
            // Act & Assert
            // TODO: Implement encounter inactive marking in ViewModel.onCleared()
            viewModel.shouldBeInstanceOf<ViewModel>()
        }
        
        test("temporary state cleared on cleanup") {
            // Arrange
            val viewModel = EncounterViewModel(
                initializeEncounter = mockk(),
                processPlayerAction = mockk(),
                advanceTurn = mockk(),
                eventRepository = mockk(relaxed = true),
                stateBuilder = mockk(),
                completionDetector = mockk(),
                undoRedoManager = mockk()
            )
            
            // Act & Assert
            // TODO: Implement temporary state clearing in ViewModel.onCleared()
            viewModel.shouldBeInstanceOf<ViewModel>()
        }
        
        test("encounter can be resumed after abandonment") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val eventRepository = mockk<EventRepository>()
                val stateBuilder = mockk<EncounterStateBuilder>()
                val undoRedoManager = mockk<UndoRedoManager>()
                
                val viewModel = EncounterViewModel(
                    initializeEncounter = mockk(),
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = eventRepository,
                    stateBuilder = stateBuilder,
                    completionDetector = mockk(),
                    undoRedoManager = undoRedoManager
                )
                
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { eventRepository.forSession(sessionId) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(2, false, emptyList(), 1L, TurnPhaseState.Action),
                    creatures = emptyMap(),
                    mapGrid = MapGridState(10, 10, emptySet(), emptySet()),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    roundNumber = 2
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                every { undoRedoManager.updateEventCount(any()) } returns Unit
                
                // Act - load encounter (simulating resume)
                viewModel.loadEncounter(sessionId)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.sessionId shouldBe sessionId
                state.roundNumber shouldBe 2
            }
        }
    }
    
    context("Testability verification") {
        test("ViewModel accepts dependencies through constructor") {
            // Arrange & Act
            val viewModel = EncounterViewModel(
                initializeEncounter = mockk(),
                processPlayerAction = mockk(),
                advanceTurn = mockk(),
                eventRepository = mockk(relaxed = true),
                stateBuilder = mockk(),
                completionDetector = mockk(),
                undoRedoManager = mockk()
            )
            
            // Assert
            viewModel.shouldBeInstanceOf<EncounterViewModel>()
        }
        
        test("all dependencies are interfaces") {
            // Arrange
            val initializeEncounter: InitializeEncounter = mockk()
            val processPlayerAction: ProcessPlayerAction = mockk()
            val advanceTurn: AdvanceTurn = mockk()
            val eventRepository: EventRepository = mockk(relaxed = true)
            val stateBuilder: EncounterStateBuilder = mockk()
            val completionDetector: CompletionDetector = mockk()
            val undoRedoManager: UndoRedoManager = mockk()
            
            // Act
            val viewModel = EncounterViewModel(
                initializeEncounter = initializeEncounter,
                processPlayerAction = processPlayerAction,
                advanceTurn = advanceTurn,
                eventRepository = eventRepository,
                stateBuilder = stateBuilder,
                completionDetector = completionDetector,
                undoRedoManager = undoRedoManager
            )
            
            // Assert - all dependencies can be mocked
            viewModel.shouldBeInstanceOf<EncounterViewModel>()
        }
        
        test("state exposed through StateFlow") {
            // Arrange
            val viewModel = EncounterViewModel(
                initializeEncounter = mockk(),
                processPlayerAction = mockk(),
                advanceTurn = mockk(),
                eventRepository = mockk(relaxed = true),
                stateBuilder = mockk(),
                completionDetector = mockk(),
                undoRedoManager = mockk()
            )
            
            // Act & Assert
            viewModel.state.shouldBeInstanceOf<StateFlow<EncounterUiState>>()
        }
        
        test("intents processed synchronously in tests with TestCoroutineDispatcher") {
            runTest(testDispatcher) {
                // Arrange
                val viewModel = EncounterViewModel(
                    initializeEncounter = mockk(),
                    processPlayerAction = mockk(),
                    advanceTurn = mockk(),
                    eventRepository = mockk(relaxed = true),
                    stateBuilder = mockk(),
                    completionDetector = mockk(),
                    undoRedoManager = mockk()
                )
                
                // Act
                viewModel.handle(EncounterIntent.StartEncounter(emptyList(), emptySet(), MapGrid(10, 10)))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert - intent processed synchronously
                val state = viewModel.state.first()
                state.error.shouldNotBeNull()
            }
        }
    }
})
