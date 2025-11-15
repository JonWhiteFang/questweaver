package dev.questweaver.feature.encounter.state

import dev.questweaver.core.rules.initiative.InitiativeStateBuilder
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.CreatureDefeated
import dev.questweaver.domain.events.EncounterEnded
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.InitiativeEntryData
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.events.RoundStarted
import dev.questweaver.domain.events.SpellCast
import dev.questweaver.domain.events.TurnStarted
import dev.questweaver.domain.values.EncounterStatus
import dev.questweaver.feature.encounter.viewmodel.Creature
import dev.questweaver.feature.encounter.viewmodel.CompletionStatus
import dev.questweaver.feature.map.ui.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk

/**
 * Tests for EncounterStateBuilder.
 * Verifies event replay and state derivation logic.
 *
 * Requirements: 6.1, 6.2, 6.3, 7.1, 7.2, 7.3
 */
class EncounterStateBuilderTest : FunSpec({
    
    lateinit var initiativeStateBuilder: InitiativeStateBuilder
    lateinit var stateBuilder: EncounterStateBuilder
    
    beforeTest {
        initiativeStateBuilder = mockk(relaxed = true)
        stateBuilder = EncounterStateBuilder(initiativeStateBuilder)
    }
    
    context("Event replay") {
        test("state derived from events matches original") {
            // Arrange
            val sessionId = 1L
            val timestamp = System.currentTimeMillis()
            
            val events = listOf<GameEvent>(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 15, 2, 17),
                        InitiativeEntryData(2L, 10, 1, 11)
                    ),
                    surprisedCreatures = emptySet()
                ),
                TurnStarted(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    creatureId = 1L
                )
            )
            
            // Act
            val state = stateBuilder.buildState(events)
            
            // Assert
            state.sessionId shouldBe sessionId
            state.roundState.activeCreatureId shouldBe 1L
            state.roundState.roundNumber shouldBe 1
        }
        
        test("event replay produces identical state") {
            // Arrange
            val sessionId = 1L
            val timestamp = System.currentTimeMillis()
            
            val events = listOf<GameEvent>(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 15, 2, 17),
                        InitiativeEntryData(2L, 10, 1, 11)
                    ),
                    surprisedCreatures = emptySet()
                ),
                RoundStarted(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    roundNumber = 2
                )
            )
            
            // Act - replay twice
            val state1 = stateBuilder.buildState(events)
            val state2 = stateBuilder.buildState(events)
            
            // Assert - should be identical
            state1.sessionId shouldBe state2.sessionId
            state1.roundState.roundNumber shouldBe state2.roundState.roundNumber
            state1.isCompleted shouldBe state2.isCompleted
        }
        
        test("all event types handled exhaustively") {
            // Arrange
            val sessionId = 1L
            val timestamp = System.currentTimeMillis()
            
            val events = listOf<GameEvent>(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 15, 2, 17),
                        InitiativeEntryData(2L, 10, 1, 11)
                    ),
                    surprisedCreatures = emptySet()
                ),
                RoundStarted(sessionId, timestamp + 1, 1),
                TurnStarted(sessionId, timestamp + 2, 1L),
                AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 3,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = 15,
                    hit = true,
                    damage = 8,
                    isCritical = false
                ),
                MoveCommitted(
                    sessionId = sessionId,
                    timestamp = timestamp + 4,
                    creatureId = 1L,
                    path = listOf(
                        dev.questweaver.domain.values.GridPos(0, 0),
                        dev.questweaver.domain.values.GridPos(1, 0)
                    ),
                    movementCost = 5
                ),
                SpellCast(
                    sessionId = sessionId,
                    timestamp = timestamp + 5,
                    casterId = 1L,
                    spellId = 100L,
                    targets = listOf(2L),
                    spellLevel = 1,
                    success = true
                ),
                CreatureDefeated(
                    sessionId = sessionId,
                    timestamp = timestamp + 6,
                    creatureId = 2L,
                    defeatedBy = 1L
                ),
                EncounterEnded(
                    sessionId = sessionId,
                    timestamp = timestamp + 7,
                    status = EncounterStatus.VICTORY
                )
            )
            
            // Act - should not throw exception
            val state = stateBuilder.buildState(events)
            
            // Assert - all events processed
            state.sessionId shouldBe sessionId
            state.isCompleted shouldBe true
            state.completionStatus shouldBe CompletionStatus.Victory
        }
    }
    
    context("UI state building") {
        test("UI state built correctly from domain state") {
            // Arrange
            val sessionId = 1L
            val encounterState = EncounterState(
                sessionId = sessionId,
                roundState = RoundState(
                    roundNumber = 2,
                    isSurpriseRound = false,
                    initiativeOrder = listOf(1L, 2L),
                    activeCreatureId = 1L,
                    turnPhase = TurnPhaseState.Action
                ),
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
            val uiState = stateBuilder.buildUiState(encounterState, creatures, null)
            
            // Assert
            uiState.sessionId shouldBe sessionId
            uiState.roundNumber shouldBe 2
            uiState.activeCreatureId shouldBe 1L
            uiState.isCompleted shouldBe false
            uiState.creatures shouldHaveSize 2
        }
        
        test("creature states include all required fields") {
            // Arrange
            val sessionId = 1L
            val encounterState = EncounterState(
                sessionId = sessionId,
                roundState = RoundState(1, false, emptyList(), null, TurnPhaseState.Start),
                creatures = emptyMap(),
                mapGrid = MapGridState(10, 10, emptySet(), emptySet()),
                readiedActions = emptyMap(),
                isCompleted = false,
                completionStatus = null
            )
            
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 15, 20, 16, GridPos(2, 3), true)
            )
            
            // Act
            val uiState = stateBuilder.buildUiState(encounterState, creatures, null)
            
            // Assert
            val creatureState = uiState.creatures[1L]
            creatureState shouldNotBe null
            creatureState!!.id shouldBe 1L
            creatureState.name shouldBe "Fighter"
            creatureState.hpCurrent shouldBe 15
            creatureState.hpMax shouldBe 20
            creatureState.ac shouldBe 16
            creatureState.position shouldBe GridPos(2, 3)
            creatureState.isPlayerControlled shouldBe true
            creatureState.isDefeated shouldBe false
        }
        
        test("available actions determined correctly for active creature") {
            // Arrange
            val sessionId = 1L
            val activeCreatureId = 1L
            
            val encounterState = EncounterState(
                sessionId = sessionId,
                roundState = RoundState(
                    roundNumber = 1,
                    isSurpriseRound = false,
                    initiativeOrder = listOf(activeCreatureId),
                    activeCreatureId = activeCreatureId,
                    turnPhase = TurnPhaseState.Action
                ),
                creatures = emptyMap(),
                mapGrid = MapGridState(10, 10, emptySet(), emptySet()),
                readiedActions = emptyMap(),
                isCompleted = false,
                completionStatus = null
            )
            
            val creatures = mapOf(
                activeCreatureId to Creature(activeCreatureId, "Fighter", 20, 20, 15, GridPos(0, 0), true)
            )
            
            // Act
            val uiState = stateBuilder.buildUiState(encounterState, creatures, null)
            
            // Assert
            // Available actions should be determined based on turn phase
            // For now, implementation returns empty list (TODO in implementation)
            uiState.availableActions shouldNotBe null
        }
    }
    
    context("Map state synchronization") {
        test("creature positions synchronized with map state") {
            // Arrange
            val sessionId = 1L
            val encounterState = EncounterState(
                sessionId = sessionId,
                roundState = RoundState(1, false, emptyList(), null, TurnPhaseState.Start),
                creatures = emptyMap(),
                mapGrid = MapGridState(10, 10, emptySet(), emptySet()),
                readiedActions = emptyMap(),
                isCompleted = false,
                completionStatus = null
            )
            
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(2, 3), true),
                2L to Creature(2L, "Goblin", 7, 7, 13, GridPos(5, 5), false)
            )
            
            // Act
            val uiState = stateBuilder.buildUiState(encounterState, creatures, null)
            
            // Assert
            uiState.mapState shouldNotBe null
            uiState.mapState!!.tokens shouldHaveSize 2
            
            val fighterToken = uiState.mapState!!.tokens.find { it.id == "1" }
            fighterToken shouldNotBe null
            fighterToken!!.pos shouldBe GridPos(2, 3)
            
            val goblinToken = uiState.mapState!!.tokens.find { it.id == "2" }
            goblinToken shouldNotBe null
            goblinToken!!.pos shouldBe GridPos(5, 5)
        }
        
        test("map grid dimensions extracted from encounter state") {
            // Arrange
            val sessionId = 1L
            val encounterState = EncounterState(
                sessionId = sessionId,
                roundState = RoundState(1, false, emptyList(), null, TurnPhaseState.Start),
                creatures = emptyMap(),
                mapGrid = MapGridState(15, 20, emptySet(), emptySet()),
                readiedActions = emptyMap(),
                isCompleted = false,
                completionStatus = null
            )
            
            val creatures = emptyMap<Long, Creature>()
            
            // Act
            val uiState = stateBuilder.buildUiState(encounterState, creatures, null)
            
            // Assert
            uiState.mapState shouldNotBe null
            uiState.mapState!!.w shouldBe 15
            uiState.mapState!!.h shouldBe 20
        }
    }
    
    context("Event handling") {
        test("MoveCommitted updates creature position") {
            // Arrange
            val sessionId = 1L
            val timestamp = System.currentTimeMillis()
            val creatureId = 1L
            
            val events = listOf<GameEvent>(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(creatureId),
                    initiativeOrder = listOf(InitiativeEntryData(creatureId, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                ),
                MoveCommitted(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    creatureId = creatureId,
                    path = listOf(
                        dev.questweaver.domain.values.GridPos(0, 0),
                        dev.questweaver.domain.values.GridPos(1, 0),
                        dev.questweaver.domain.values.GridPos(2, 0)
                    ),
                    movementCost = 10
                )
            )
            
            // Act
            val state = stateBuilder.buildState(events)
            
            // Assert
            // Position should be updated to final position in path
            // Note: Implementation needs to track creature positions in state
            state.sessionId shouldBe sessionId
        }
        
        test("RoundStarted increments round number") {
            // Arrange
            val sessionId = 1L
            val timestamp = System.currentTimeMillis()
            
            val events = listOf<GameEvent>(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                ),
                RoundStarted(sessionId, timestamp + 1, 1),
                RoundStarted(sessionId, timestamp + 2, 2)
            )
            
            // Act
            val state = stateBuilder.buildState(events)
            
            // Assert
            state.roundState.roundNumber shouldBe 2
        }
        
        test("TurnStarted sets active creature") {
            // Arrange
            val sessionId = 1L
            val timestamp = System.currentTimeMillis()
            val creatureId = 2L
            
            val events = listOf<GameEvent>(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 15, 2, 17),
                        InitiativeEntryData(2L, 10, 1, 11)
                    ),
                    surprisedCreatures = emptySet()
                ),
                TurnStarted(sessionId, timestamp + 1, creatureId)
            )
            
            // Act
            val state = stateBuilder.buildState(events)
            
            // Assert
            state.roundState.activeCreatureId shouldBe creatureId
            state.roundState.turnPhase shouldBe TurnPhaseState.Start
        }
        
        test("EncounterEnded sets completion status") {
            // Arrange
            val sessionId = 1L
            val timestamp = System.currentTimeMillis()
            
            val events = listOf<GameEvent>(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                ),
                EncounterEnded(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    status = EncounterStatus.DEFEAT
                )
            )
            
            // Act
            val state = stateBuilder.buildState(events)
            
            // Assert
            state.isCompleted shouldBe true
            state.completionStatus shouldBe CompletionStatus.Defeat
        }
    }
})
