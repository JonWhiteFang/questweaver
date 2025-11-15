package dev.questweaver.feature.encounter.viewmodel

import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.repositories.EventRepository
import dev.questweaver.feature.encounter.state.CompletionDetector
import dev.questweaver.feature.encounter.state.EncounterState
import dev.questweaver.feature.encounter.state.EncounterStateBuilder
import dev.questweaver.feature.encounter.state.RoundState
import dev.questweaver.feature.encounter.state.TurnPhaseState
import dev.questweaver.feature.encounter.state.UndoRedoManager
import dev.questweaver.feature.encounter.usecases.AdvanceTurn
import dev.questweaver.feature.encounter.usecases.InitializeEncounter
import dev.questweaver.feature.encounter.usecases.ProcessPlayerAction
import dev.questweaver.domain.map.geometry.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Tests for EncounterViewModel intent handling.
 * Verifies MVI pattern implementation and state flow emissions.
 *
 * Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 4.1, 9.1, 9.2
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EncounterViewModelTest : FunSpec({
    
    val testDispatcher = StandardTestDispatcher()
    
    // Mocked dependencies
    lateinit var initializeEncounter: InitializeEncounter
    lateinit var processPlayerAction: ProcessPlayerAction
    lateinit var advanceTurn: AdvanceTurn
    lateinit var eventRepository: EventRepository
    lateinit var stateBuilder: EncounterStateBuilder
    lateinit var completionDetector: CompletionDetector
    lateinit var undoRedoManager: UndoRedoManager
    lateinit var viewModel: EncounterViewModel
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    beforeTest {
        // Initialize mocks
        initializeEncounter = mockk()
        processPlayerAction = mockk()
        advanceTurn = mockk()
        eventRepository = mockk(relaxed = true)
        stateBuilder = mockk()
        completionDetector = mockk()
        undoRedoManager = mockk()
        
        // Create ViewModel with mocked dependencies
        viewModel = EncounterViewModel(
            initializeEncounter = initializeEncounter,
            processPlayerAction = processPlayerAction,
            advanceTurn = advanceTurn,
            eventRepository = eventRepository,
            stateBuilder = stateBuilder,
            completionDetector = completionDetector,
            undoRedoManager = undoRedoManager
        )
    }
    
    context("StartEncounter intent") {
        test("initializes encounter correctly") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val creatures = listOf(
                    Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                    Creature(2L, "Goblin", 7, 7, 13, GridPos(5, 5), false)
                )
                val mapGrid = MapGrid(10, 10)
                val intent = EncounterIntent.StartEncounter(creatures, emptySet(), mapGrid)
                
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), null, TurnPhaseState.Start),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    roundNumber = 1,
                    isLoading = false
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                
                // Act
                viewModel.handle(intent)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.sessionId shouldBe sessionId
                state.roundNumber shouldBe 1
                state.isLoading shouldBe false
                state.error.shouldBeNull()
                
                coVerify { initializeEncounter(any(), creatures, emptySet(), mapGrid) }
                coVerify { eventRepository.append(encounterStartedEvent) }
            }
        }
        
        test("rejects empty creature list with error") {
            runTest(testDispatcher) {
                // Arrange
                val intent = EncounterIntent.StartEncounter(emptyList(), emptySet(), MapGrid(10, 10))
                
                // Act
                viewModel.handle(intent)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.error.shouldNotBeNull()
                state.error shouldBe "Failed to start encounter: Cannot start encounter with no creatures"
                state.isLoading shouldBe false
            }
        }
    }
    
    context("Attack intent") {
        test("processes attack and updates state") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val activeCreatureId = 1L
                val targetId = 2L
                
                // Set up initial state
                viewModel.handle(EncounterIntent.StartEncounter(
                    listOf(
                        Creature(activeCreatureId, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                        Creature(targetId, "Goblin", 7, 7, 13, GridPos(1, 0), false)
                    ),
                    emptySet(),
                    MapGrid(10, 10)
                ))
                
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), activeCreatureId, TurnPhaseState.Action),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    activeCreatureId = activeCreatureId,
                    roundNumber = 1
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Now test attack
                val attackIntent = EncounterIntent.Attack(targetId)
                coEvery { processPlayerAction(any(), any()) } returns ActionResult.Success("Attack hit!")
                
                // Act
                viewModel.handle(attackIntent)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.lastActionResult.shouldBeInstanceOf<ActionResult.Success>()
                state.error.shouldBeNull()
                
                coVerify { processPlayerAction(any(), any()) }
            }
        }
    }
    
    context("MoveTo intent") {
        test("processes movement and updates state") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val activeCreatureId = 1L
                val path = listOf(GridPos(0, 0), GridPos(1, 0), GridPos(2, 0))
                
                // Set up initial state with active creature
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), activeCreatureId, TurnPhaseState.Movement),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    activeCreatureId = activeCreatureId,
                    roundNumber = 1,
                    mapState = mockk {
                        every { blocked } returns emptySet()
                        every { difficult } returns emptySet()
                    }
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                
                viewModel.handle(EncounterIntent.StartEncounter(
                    listOf(Creature(activeCreatureId, "Fighter", 20, 20, 15, GridPos(0, 0), true)),
                    emptySet(),
                    MapGrid(10, 10)
                ))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Test movement
                val moveIntent = EncounterIntent.MoveTo(path)
                coEvery { processPlayerAction(any(), any()) } returns ActionResult.Success("Moved successfully")
                
                // Act
                viewModel.handle(moveIntent)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.lastActionResult.shouldBeInstanceOf<ActionResult.Success>()
                state.movementPath.shouldBeNull() // Should be cleared after processing
                
                coVerify { processPlayerAction(any(), any()) }
            }
        }
    }
    
    context("CastSpell intent") {
        test("processes spell and updates state") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val activeCreatureId = 1L
                val targetId = 2L
                val spellId = 100L
                
                // Set up initial state
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), activeCreatureId, TurnPhaseState.Action),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    activeCreatureId = activeCreatureId,
                    roundNumber = 1
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                
                viewModel.handle(EncounterIntent.StartEncounter(
                    listOf(
                        Creature(activeCreatureId, "Wizard", 15, 15, 12, GridPos(0, 0), true),
                        Creature(targetId, "Goblin", 7, 7, 13, GridPos(3, 3), false)
                    ),
                    emptySet(),
                    MapGrid(10, 10)
                ))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Test spell casting
                val spellIntent = EncounterIntent.CastSpell(spellId, listOf(targetId), 1)
                coEvery { processPlayerAction(any(), any()) } returns ActionResult.Success("Spell cast!")
                
                // Act
                viewModel.handle(spellIntent)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.lastActionResult.shouldBeInstanceOf<ActionResult.Success>()
                
                coVerify { processPlayerAction(any(), any()) }
            }
        }
    }
    
    context("EndTurn intent") {
        test("advances turn correctly") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val activeCreatureId = 1L
                
                // Set up initial state
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                coEvery { eventRepository.appendAll(any()) } returns Unit
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), activeCreatureId, TurnPhaseState.End),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    activeCreatureId = activeCreatureId,
                    roundNumber = 1,
                    creatures = mapOf(
                        1L to CreatureState(1L, "Fighter", 20, 20, 15, GridPos(0, 0), emptySet(), true, false),
                        2L to CreatureState(2L, "Goblin", 7, 7, 13, GridPos(5, 5), emptySet(), false, false)
                    )
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                every { completionDetector.checkCompletion(any()) } returns null
                
                viewModel.handle(EncounterIntent.StartEncounter(
                    listOf(
                        Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                        Creature(2L, "Goblin", 7, 7, 13, GridPos(5, 5), false)
                    ),
                    emptySet(),
                    MapGrid(10, 10)
                ))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Test end turn
                coEvery { advanceTurn(any()) } returns emptyList()
                
                // Act
                viewModel.handle(EncounterIntent.EndTurn)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                coVerify { advanceTurn(any()) }
                coVerify { eventRepository.appendAll(any()) }
            }
        }
    }
    
    context("Undo intent") {
        test("removes last event and rebuilds state") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                
                // Set up initial state
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), 1L, TurnPhaseState.Action),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    roundNumber = 1
                )
                every { undoRedoManager.canUndo() } returns true andThen false
                every { undoRedoManager.canRedo() } returns false andThen true
                
                viewModel.handle(EncounterIntent.StartEncounter(
                    listOf(Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true)),
                    emptySet(),
                    MapGrid(10, 10)
                ))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Test undo
                coEvery { undoRedoManager.undo(sessionId) } returns listOf(encounterStartedEvent)
                
                // Act
                viewModel.handle(EncounterIntent.Undo)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.canUndo shouldBe false
                state.canRedo shouldBe true
                
                coVerify { undoRedoManager.undo(sessionId) }
            }
        }
    }
    
    context("Redo intent") {
        test("restores undone event") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                
                // Set up initial state
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), 1L, TurnPhaseState.Action),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    roundNumber = 1
                )
                every { undoRedoManager.canUndo() } returns false andThen true
                every { undoRedoManager.canRedo() } returns true andThen false
                
                viewModel.handle(EncounterIntent.StartEncounter(
                    listOf(Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true)),
                    emptySet(),
                    MapGrid(10, 10)
                ))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Test redo
                coEvery { undoRedoManager.redo(sessionId) } returns listOf(encounterStartedEvent)
                
                // Act
                viewModel.handle(EncounterIntent.Redo)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.canUndo shouldBe true
                state.canRedo shouldBe false
                
                coVerify { undoRedoManager.redo(sessionId) }
            }
        }
    }
    
    context("Invalid intents") {
        test("rejects attack with no active creature") {
            runTest(testDispatcher) {
                // Arrange - no active creature in state
                val encounterState = EncounterState(
                    sessionId = 1L,
                    roundState = RoundState(1, false, emptyList(), null, TurnPhaseState.Start),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = 1L,
                    activeCreatureId = null
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                
                // Act
                viewModel.handle(EncounterIntent.Attack(2L))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val state = viewModel.state.first()
                state.error.shouldNotBeNull()
                state.error shouldBe "Action failed: No active creature"
            }
        }
    }
    
    context("State flow emissions") {
        test("emits updated state after each intent") {
            runTest(testDispatcher) {
                // Arrange
                val sessionId = 1L
                val encounterStartedEvent = mockk<EncounterStarted> {
                    every { this@mockk.sessionId } returns sessionId
                }
                
                coEvery { initializeEncounter(any(), any(), any(), any()) } returns encounterStartedEvent
                coEvery { eventRepository.forSession(any()) } returns listOf(encounterStartedEvent)
                
                val encounterState = EncounterState(
                    sessionId = sessionId,
                    roundState = RoundState(1, false, emptyList(), null, TurnPhaseState.Start),
                    creatures = emptyMap(),
                    mapGrid = mockk(),
                    readiedActions = emptyMap(),
                    isCompleted = false,
                    completionStatus = null
                )
                
                every { stateBuilder.buildState(any()) } returns encounterState
                every { stateBuilder.buildUiState(any(), any()) } returns EncounterUiState(
                    sessionId = sessionId,
                    roundNumber = 1,
                    isLoading = false
                )
                every { undoRedoManager.canUndo() } returns false
                every { undoRedoManager.canRedo() } returns false
                
                // Act
                val initialState = viewModel.state.first()
                initialState.sessionId.shouldBeNull()
                
                viewModel.handle(EncounterIntent.StartEncounter(
                    listOf(Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true)),
                    emptySet(),
                    MapGrid(10, 10)
                ))
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                val updatedState = viewModel.state.first()
                updatedState.sessionId shouldNotBe null
                updatedState.sessionId shouldBe sessionId
                updatedState.roundNumber shouldBe 1
            }
        }
    }
})

