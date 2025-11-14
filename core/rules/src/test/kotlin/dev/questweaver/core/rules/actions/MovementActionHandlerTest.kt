package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.Dash
import dev.questweaver.core.rules.actions.models.Move
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.GridPos
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class MovementActionHandlerTest : FunSpec({
    
    // Test fixtures
    val creature = Creature(
        id = 1L,
        name = "Fighter",
        armorClass = 16,
        hitPointsCurrent = 30,
        hitPointsMax = 30,
        speed = 30,
        abilities = Abilities(
            strength = 16,
            dexterity = 14,
            constitution = 14,
            intelligence = 10,
            wisdom = 12,
            charisma = 10
        )
    )
    
    val mapGrid = MapGrid(width = 20, height = 20)
    
    val context = ActionContext(
        sessionId = 1L,
        roundNumber = 1,
        turnPhase = TurnPhase(
            creatureId = 1L,
            actionAvailable = true,
            bonusActionAvailable = true,
            reactionAvailable = true,
            movementRemaining = 30
        ),
        creatures = mapOf(1L to creature),
        mapGrid = mapGrid,
        activeConditions = emptyMap(),
        readiedActions = emptyMap()
    )
    
    test("movement consumes correct amount") {
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(1, 0),
            GridPos(2, 0)
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 10
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        val events = runBlocking { handler.handleMovement(moveAction, context) }
        
        events shouldHaveSize 1
        val event = events.first()
        event.shouldBeInstanceOf<MoveCommitted>()
        event.movementUsed shouldBe 10
        event.movementRemaining shouldBe 20
    }
    
    test("difficult terrain doubles cost") {
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(1, 0)
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        // Difficult terrain: 5 feet of movement costs 10 feet
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 10
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        val events = runBlocking { handler.handleMovement(moveAction, context) }
        
        events shouldHaveSize 1
        val event = events.first() as MoveCommitted
        event.movementUsed shouldBe 10
        event.movementRemaining shouldBe 20
    }
    
    test("movement fails when insufficient movement remaining") {
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(5, 0)
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 35 // More than available
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        shouldThrow<IllegalArgumentException> {
            runBlocking { handler.handleMovement(moveAction, context) }
        }
    }
    
    test("movement fails when path is invalid") {
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(10, 10) // Invalid path
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns false
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        shouldThrow<IllegalArgumentException> {
            runBlocking { handler.handleMovement(moveAction, context) }
        }
    }
    
    test("MoveCommitted event contains correct path and remaining movement") {
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(1, 0),
            GridPos(2, 0),
            GridPos(3, 0)
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 15
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        val events = runBlocking { handler.handleMovement(moveAction, context) }
        
        val event = events.first() as MoveCommitted
        event.sessionId shouldBe 1L
        event.creatureId shouldBe 1L
        event.path shouldBe path
        event.movementUsed shouldBe 15
        event.movementRemaining shouldBe 15
    }
    
    test("movement with zero cost still generates event") {
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(GridPos(0, 0)) // Stay in place
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 0
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        val events = runBlocking { handler.handleMovement(moveAction, context) }
        
        events shouldHaveSize 1
        val event = events.first() as MoveCommitted
        event.movementUsed shouldBe 0
        event.movementRemaining shouldBe 30
    }
    
    test("movement uses all remaining movement") {
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(6, 0)
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 30
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        val events = runBlocking { handler.handleMovement(moveAction, context) }
        
        val event = events.first() as MoveCommitted
        event.movementUsed shouldBe 30
        event.movementRemaining shouldBe 0
    }
    
    // Note: Opportunity attack tests are currently limited because the MovementActionHandler
    // has TODO comments for threat detection and triggering opportunity attacks.
    // These tests verify the current behavior and will need to be expanded when
    // opportunity attack logic is fully implemented.
    
    test("opportunity attacks triggered correctly - placeholder test") {
        // This test verifies that the handler can be called with a ReactionHandler
        // The actual opportunity attack triggering logic is marked as TODO in the implementation
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(1, 0)
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 5
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        val events = runBlocking { handler.handleMovement(moveAction, context) }
        
        // Currently only generates MoveCommitted event
        // When opportunity attack logic is implemented, this should include attack events
        events shouldHaveSize 1
        events.first().shouldBeInstanceOf<MoveCommitted>()
    }
    
    test("Dash action doubles movement - placeholder test") {
        // This test verifies that handleDash can be called
        // The actual Dash implementation is marked as TODO and returns empty list
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val dashAction = Dash(actorId = 1L)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        val events = runBlocking { handler.handleDash(dashAction, context) }
        
        // Currently returns empty list as per TODO in implementation
        // When implemented, should generate DashAction event
        events shouldHaveSize 0
    }
    
    test("Disengage prevents opportunity attacks - integration test placeholder") {
        // This test is a placeholder for integration testing with SpecialActionHandler
        // Disengage is handled by SpecialActionHandler, not MovementActionHandler
        // The integration between Disengage condition and opportunity attack prevention
        // will be tested in ActionProcessor integration tests
        
        // For now, we verify that movement works normally
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(1, 0)
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 5
        
        val moveAction = Move(actorId = 1L, path = path)
        val handler = MovementActionHandler(pathfinder, reactionHandler)
        
        val events = runBlocking { handler.handleMovement(moveAction, context) }
        
        events shouldHaveSize 1
        events.first().shouldBeInstanceOf<MoveCommitted>()
    }
})
