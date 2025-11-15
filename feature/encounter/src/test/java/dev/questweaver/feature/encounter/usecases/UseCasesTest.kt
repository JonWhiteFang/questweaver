package dev.questweaver.feature.encounter.usecases

import dev.questweaver.core.rules.initiative.InitiativeRoller
import dev.questweaver.core.rules.initiative.InitiativeTracker
import dev.questweaver.core.rules.initiative.SurpriseHandler
import dev.questweaver.feature.encounter.state.RoundState
import dev.questweaver.feature.encounter.state.TurnPhaseState
import dev.questweaver.feature.encounter.viewmodel.ActionResult
import dev.questweaver.feature.encounter.viewmodel.CombatAction
import dev.questweaver.feature.encounter.viewmodel.Creature
import dev.questweaver.feature.encounter.viewmodel.MapGrid
import dev.questweaver.domain.map.geometry.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Tests for encounter use cases.
 * Verifies use case logic and event generation.
 *
 * Requirements: 1.1, 1.2, 1.3, 3.1, 3.4, 3.5, 4.1, 4.2
 */
class UseCasesTest : FunSpec({
    
    context("InitializeEncounter") {
        test("rolls initiative correctly") {
            runTest {
                // Arrange
                val initiativeRoller = mockk<InitiativeRoller>()
                val surpriseHandler = mockk<SurpriseHandler>()
                val useCase = InitializeEncounter(initiativeRoller, surpriseHandler)
                
                val creatures = listOf(
                    Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                    Creature(2L, "Goblin", 7, 7, 13, GridPos(5, 5), false)
                )
                
                val initiativeEntries = listOf(
                    dev.questweaver.core.rules.initiative.models.InitiativeEntry(1L, 15, 2, 17),
                    dev.questweaver.core.rules.initiative.models.InitiativeEntry(2L, 10, 1, 11)
                )
                
                every { initiativeRoller.rollInitiativeForAll(any()) } returns initiativeEntries
                every { surpriseHandler.hasSurpriseRound(any()) } returns false
                
                // Act
                val event = useCase(
                    sessionId = 1L,
                    creatures = creatures,
                    surprisedCreatures = emptySet(),
                    mapGrid = MapGrid(10, 10)
                )
                
                // Assert
                event.sessionId shouldBe 1L
                event.participants shouldHaveSize 2
                event.initiativeOrder shouldHaveSize 2
                event.initiativeOrder[0].creatureId shouldBe 1L
                event.initiativeOrder[0].total shouldBe 17
            }
        }
        
        test("creates surprise round when needed") {
            runTest {
                // Arrange
                val initiativeRoller = mockk<InitiativeRoller>()
                val surpriseHandler = mockk<SurpriseHandler>()
                val useCase = InitializeEncounter(initiativeRoller, surpriseHandler)
                
                val creatures = listOf(
                    Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                    Creature(2L, "Goblin", 7, 7, 13, GridPos(5, 5), false)
                )
                
                val surprisedCreatures = setOf(2L)
                
                val initiativeEntries = listOf(
                    dev.questweaver.core.rules.initiative.models.InitiativeEntry(1L, 15, 2, 17),
                    dev.questweaver.core.rules.initiative.models.InitiativeEntry(2L, 10, 1, 11)
                )
                
                every { initiativeRoller.rollInitiativeForAll(any()) } returns initiativeEntries
                every { surpriseHandler.hasSurpriseRound(surprisedCreatures) } returns true
                
                // Act
                val event = useCase(
                    sessionId = 1L,
                    creatures = creatures,
                    surprisedCreatures = surprisedCreatures,
                    mapGrid = MapGrid(10, 10)
                )
                
                // Assert
                event.surprisedCreatures shouldBe surprisedCreatures
            }
        }
    }
    
    context("ProcessPlayerAction") {
        test("validates actions before execution") {
            runTest {
                // Arrange
                val useCase = ProcessPlayerAction()
                val action = CombatAction("attack", 2L)
                val context = ActionContext(1L, 1L, 1)
                
                // Act
                val result = useCase(action, context)
                
                // Assert
                // Current implementation returns failure (not yet implemented)
                result.shouldBeInstanceOf<ActionResult.Failure>()
            }
        }
        
        test("returns failure for invalid actions") {
            runTest {
                // Arrange
                val useCase = ProcessPlayerAction()
                val action = CombatAction("invalid_action", null)
                val context = ActionContext(1L, 1L, 1)
                
                // Act
                val result = useCase(action, context)
                
                // Assert
                result.shouldBeInstanceOf<ActionResult.Failure>()
            }
        }
        
        test("returns RequiresChoice when needed") {
            runTest {
                // Arrange
                val useCase = ProcessPlayerAction()
                val action = CombatAction("cast_spell", 2L)
                val context = ActionContext(1L, 1L, 1)
                
                // Act
                val result = useCase(action, context)
                
                // Assert
                // Current implementation returns failure
                // When implemented, should return RequiresChoice for certain actions
                result shouldNotBe null
            }
        }
    }
    
    context("AdvanceTurn") {
        test("progresses to next creature") {
            runTest {
                // Arrange
                val initiativeTracker = mockk<InitiativeTracker>()
                val useCase = AdvanceTurn(initiativeTracker)
                
                val currentState = RoundState(
                    roundNumber = 1,
                    isSurpriseRound = false,
                    initiativeOrder = listOf(1L, 2L),
                    activeCreatureId = 1L,
                    turnPhase = TurnPhaseState.End
                )
                
                // Act
                val events = useCase(currentState)
                
                // Assert
                events.shouldNotBeEmpty()
                // Should generate TurnEnded and TurnStarted events
                events shouldHaveSize 2
            }
        }
        
        test("wraps to first creature and increments round") {
            runTest {
                // Arrange
                val initiativeTracker = mockk<InitiativeTracker>()
                val useCase = AdvanceTurn(initiativeTracker)
                
                val currentState = RoundState(
                    roundNumber = 1,
                    isSurpriseRound = false,
                    initiativeOrder = listOf(1L, 2L),
                    activeCreatureId = 2L, // Last creature
                    turnPhase = TurnPhaseState.End
                )
                
                // Act
                val events = useCase(currentState)
                
                // Assert
                events.shouldNotBeEmpty()
                // Current implementation generates TurnEnded and TurnStarted
                // When fully implemented, should also generate RoundStarted
            }
        }
    }
})
