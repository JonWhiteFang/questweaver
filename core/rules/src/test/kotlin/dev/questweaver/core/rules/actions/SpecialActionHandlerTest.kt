package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.Attack
import dev.questweaver.core.rules.actions.models.DamageType
import dev.questweaver.core.rules.actions.models.Disengage
import dev.questweaver.core.rules.actions.models.Dodge
import dev.questweaver.core.rules.actions.models.Help
import dev.questweaver.core.rules.actions.models.HelpType
import dev.questweaver.core.rules.actions.models.Ready
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.DisengageAction
import dev.questweaver.domain.events.DodgeAction
import dev.questweaver.domain.events.HelpAction
import dev.questweaver.domain.events.ReadyAction
import dev.questweaver.domain.values.Abilities
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking

class SpecialActionHandlerTest : FunSpec({
    
    // Test fixtures
    val actor = Creature(
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
    
    val ally = Creature(
        id = 2L,
        name = "Rogue",
        armorClass = 15,
        hitPointsCurrent = 25,
        hitPointsMax = 25,
        speed = 30,
        abilities = Abilities(
            strength = 10,
            dexterity = 16,
            constitution = 12,
            intelligence = 12,
            wisdom = 10,
            charisma = 14
        )
    )
    
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
        creatures = mapOf(1L to actor, 2L to ally),
        mapGrid = MapGrid(width = 10, height = 10),
        activeConditions = emptyMap(),
        readiedActions = emptyMap()
    )
    
    context("Dodge action") {
        test("generates DodgeAction event") {
            val handler = SpecialActionHandler()
            val dodgeAction = Dodge(actorId = 1L)
            
            val events = runBlocking { handler.handleDodge(dodgeAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<DodgeAction>()
        }
        
        test("DodgeAction event contains correct creature ID") {
            val handler = SpecialActionHandler()
            val dodgeAction = Dodge(actorId = 1L)
            
            val events = runBlocking { handler.handleDodge(dodgeAction, context) }
            
            val event = events.first() as DodgeAction
            event.creatureId shouldBe 1L
            event.sessionId shouldBe 1L
        }
        
        test("Dodge applies condition until next turn - verified by event generation") {
            // The Dodge action generates an event that will be used by the encounter state
            // to apply the Dodging condition until the start of the creature's next turn
            val handler = SpecialActionHandler()
            val dodgeAction = Dodge(actorId = 1L)
            
            val events = runBlocking { handler.handleDodge(dodgeAction, context) }
            
            // Event generation confirms the action was processed
            // The actual condition application is handled by the encounter state manager
            events shouldHaveSize 1
            events.first().shouldBeInstanceOf<DodgeAction>()
        }
        
        test("Dodge action consumes action phase - verified by event generation") {
            // The event generation indicates the action was taken
            // The actual action phase consumption is handled by the turn phase manager
            val handler = SpecialActionHandler()
            val dodgeAction = Dodge(actorId = 1L)
            
            val events = runBlocking { handler.handleDodge(dodgeAction, context) }
            
            events shouldHaveSize 1
            events.first().shouldBeInstanceOf<DodgeAction>()
        }
    }
    
    context("Disengage action") {
        test("generates DisengageAction event") {
            val handler = SpecialActionHandler()
            val disengageAction = Disengage(actorId = 1L)
            
            val events = runBlocking { handler.handleDisengage(disengageAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<DisengageAction>()
        }
        
        test("DisengageAction event contains correct creature ID") {
            val handler = SpecialActionHandler()
            val disengageAction = Disengage(actorId = 1L)
            
            val events = runBlocking { handler.handleDisengage(disengageAction, context) }
            
            val event = events.first() as DisengageAction
            event.creatureId shouldBe 1L
            event.sessionId shouldBe 1L
        }
        
        test("Disengage prevents opportunity attacks - verified by event generation") {
            // The Disengage action generates an event that will be used by the encounter state
            // to apply the Disengaged condition for the remainder of the turn
            // This condition prevents opportunity attacks when the creature moves
            val handler = SpecialActionHandler()
            val disengageAction = Disengage(actorId = 1L)
            
            val events = runBlocking { handler.handleDisengage(disengageAction, context) }
            
            // Event generation confirms the action was processed
            // The actual opportunity attack prevention is handled by the reaction handler
            // checking for the Disengaged condition
            events shouldHaveSize 1
            events.first().shouldBeInstanceOf<DisengageAction>()
        }
        
        test("Disengage action consumes action phase - verified by event generation") {
            // The event generation indicates the action was taken
            // The actual action phase consumption is handled by the turn phase manager
            val handler = SpecialActionHandler()
            val disengageAction = Disengage(actorId = 1L)
            
            val events = runBlocking { handler.handleDisengage(disengageAction, context) }
            
            events shouldHaveSize 1
            events.first().shouldBeInstanceOf<DisengageAction>()
        }
    }
    
    context("Help action") {
        test("generates HelpAction event") {
            val handler = SpecialActionHandler()
            val helpAction = Help(
                actorId = 1L,
                targetId = 2L,
                helpType = HelpType.Attack
            )
            
            val events = runBlocking { handler.handleHelp(helpAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<HelpAction>()
        }
        
        test("HelpAction event contains correct helper and target IDs") {
            val handler = SpecialActionHandler()
            val helpAction = Help(
                actorId = 1L,
                targetId = 2L,
                helpType = HelpType.Attack
            )
            
            val events = runBlocking { handler.handleHelp(helpAction, context) }
            
            val event = events.first() as HelpAction
            event.helperId shouldBe 1L
            event.targetId shouldBe 2L
            event.sessionId shouldBe 1L
        }
        
        test("Help action for attack grants advantage on next attack roll") {
            val handler = SpecialActionHandler()
            val helpAction = Help(
                actorId = 1L,
                targetId = 2L,
                helpType = HelpType.Attack
            )
            
            val events = runBlocking { handler.handleHelp(helpAction, context) }
            
            val event = events.first() as HelpAction
            event.helpType shouldBe dev.questweaver.domain.events.HelpType.Attack
        }
        
        test("Help action for ability check grants advantage on next ability check") {
            val handler = SpecialActionHandler()
            val helpAction = Help(
                actorId = 1L,
                targetId = 2L,
                helpType = HelpType.AbilityCheck
            )
            
            val events = runBlocking { handler.handleHelp(helpAction, context) }
            
            val event = events.first() as HelpAction
            event.helpType shouldBe dev.questweaver.domain.events.HelpType.AbilityCheck
        }
        
        test("Help action fails when target does not exist") {
            val handler = SpecialActionHandler()
            val helpAction = Help(
                actorId = 1L,
                targetId = 999L, // Non-existent target
                helpType = HelpType.Attack
            )
            
            shouldThrow<IllegalArgumentException> {
                runBlocking { handler.handleHelp(helpAction, context) }
            }
        }
        
        test("Help action consumes action phase - verified by event generation") {
            // The event generation indicates the action was taken
            // The actual action phase consumption is handled by the turn phase manager
            val handler = SpecialActionHandler()
            val helpAction = Help(
                actorId = 1L,
                targetId = 2L,
                helpType = HelpType.Attack
            )
            
            val events = runBlocking { handler.handleHelp(helpAction, context) }
            
            events shouldHaveSize 1
            events.first().shouldBeInstanceOf<HelpAction>()
        }
    }
    
    context("Ready action") {
        test("generates ReadyAction event") {
            val handler = SpecialActionHandler()
            val preparedAction = Attack(
                actorId = 1L,
                targetId = 2L,
                weaponId = null,
                attackBonus = 5,
                damageDice = "1d8",
                damageModifier = 3,
                damageType = DamageType.Slashing
            )
            val readyAction = Ready(
                actorId = 1L,
                preparedAction = preparedAction,
                trigger = "when the goblin moves within 5 feet"
            )
            
            val events = runBlocking { handler.handleReady(readyAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<ReadyAction>()
        }
        
        test("ReadyAction event contains correct creature ID and trigger") {
            val handler = SpecialActionHandler()
            val preparedAction = Attack(
                actorId = 1L,
                targetId = 2L,
                weaponId = null,
                attackBonus = 5,
                damageDice = "1d8",
                damageModifier = 3,
                damageType = DamageType.Slashing
            )
            val readyAction = Ready(
                actorId = 1L,
                preparedAction = preparedAction,
                trigger = "when the goblin moves within 5 feet"
            )
            
            val events = runBlocking { handler.handleReady(readyAction, context) }
            
            val event = events.first() as ReadyAction
            event.creatureId shouldBe 1L
            event.trigger shouldBe "when the goblin moves within 5 feet"
            event.sessionId shouldBe 1L
        }
        
        test("Ready action stores prepared action description") {
            val handler = SpecialActionHandler()
            val preparedAction = Attack(
                actorId = 1L,
                targetId = 2L,
                weaponId = null,
                attackBonus = 5,
                damageDice = "1d8",
                damageModifier = 3,
                damageType = DamageType.Slashing
            )
            val readyAction = Ready(
                actorId = 1L,
                preparedAction = preparedAction,
                trigger = "when the goblin moves within 5 feet"
            )
            
            val events = runBlocking { handler.handleReady(readyAction, context) }
            
            val event = events.first() as ReadyAction
            // The prepared action is stored as a string description
            event.preparedActionDescription shouldBe preparedAction.toString()
        }
        
        test("Ready action stores and triggers correctly - verified by event generation") {
            // The Ready action generates an event that stores the prepared action and trigger
            // The actual trigger detection and action execution is handled by the reaction handler
            // when the trigger condition is met
            val handler = SpecialActionHandler()
            val preparedAction = Dodge(actorId = 1L)
            val readyAction = Ready(
                actorId = 1L,
                preparedAction = preparedAction,
                trigger = "when an enemy attacks me"
            )
            
            val events = runBlocking { handler.handleReady(readyAction, context) }
            
            // Event generation confirms the action was stored
            // The trigger detection and execution is handled by the encounter state
            events shouldHaveSize 1
            val event = events.first() as ReadyAction
            event.trigger shouldBe "when an enemy attacks me"
        }
        
        test("Ready action consumes action phase - verified by event generation") {
            // The event generation indicates the action was taken
            // The actual action phase consumption is handled by the turn phase manager
            val handler = SpecialActionHandler()
            val preparedAction = Attack(
                actorId = 1L,
                targetId = 2L,
                weaponId = null,
                attackBonus = 5,
                damageDice = "1d8",
                damageModifier = 3,
                damageType = DamageType.Slashing
            )
            val readyAction = Ready(
                actorId = 1L,
                preparedAction = preparedAction,
                trigger = "when the goblin moves"
            )
            
            val events = runBlocking { handler.handleReady(readyAction, context) }
            
            events shouldHaveSize 1
            events.first().shouldBeInstanceOf<ReadyAction>()
        }
    }
    
    context("All special actions") {
        test("all special actions consume action phase - verified by event generation") {
            // All special actions generate events that indicate the action was taken
            // The actual action phase consumption is handled by the turn phase manager
            // This test verifies that all special action handlers generate events
            val handler = SpecialActionHandler()
            
            val dodgeEvents = runBlocking { handler.handleDodge(Dodge(actorId = 1L), context) }
            val disengageEvents = runBlocking { handler.handleDisengage(Disengage(actorId = 1L), context) }
            val helpEvents = runBlocking { 
                handler.handleHelp(
                    Help(actorId = 1L, targetId = 2L, helpType = HelpType.Attack),
                    context
                )
            }
            val readyEvents = runBlocking {
                handler.handleReady(
                    Ready(
                        actorId = 1L,
                        preparedAction = Dodge(actorId = 1L),
                        trigger = "when attacked"
                    ),
                    context
                )
            }
            
            // All actions generate exactly one event
            dodgeEvents shouldHaveSize 1
            disengageEvents shouldHaveSize 1
            helpEvents shouldHaveSize 1
            readyEvents shouldHaveSize 1
            
            // All events are of the correct type
            dodgeEvents.first().shouldBeInstanceOf<DodgeAction>()
            disengageEvents.first().shouldBeInstanceOf<DisengageAction>()
            helpEvents.first().shouldBeInstanceOf<HelpAction>()
            readyEvents.first().shouldBeInstanceOf<ReadyAction>()
        }
    }
})
