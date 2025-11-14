package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.ActionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Unit tests for TurnPhaseManager.
 *
 * Tests verify that:
 * - All actions available at turn start
 * - Actions consumed correctly
 * - Cannot consume action twice
 * - Reaction restored at turn start
 * - Movement tracks remaining distance
 */
class TurnPhaseManagerTest : FunSpec({

    context("Turn start initialization") {
        test("all actions available at turn start") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(
                creatureId = 1L,
                movementSpeed = 30
            )
            
            phase.creatureId shouldBe 1L
            phase.movementRemaining shouldBe 30
            phase.actionAvailable.shouldBeTrue()
            phase.bonusActionAvailable.shouldBeTrue()
            phase.reactionAvailable.shouldBeTrue()
        }

        test("turn start with different movement speeds") {
            val manager = TurnPhaseManager()
            
            val phase1 = manager.startTurn(1L, 25)
            phase1.movementRemaining shouldBe 25
            
            val phase2 = manager.startTurn(2L, 40)
            phase2.movementRemaining shouldBe 40
            
            val phase3 = manager.startTurn(3L, 0)
            phase3.movementRemaining shouldBe 0
        }

        test("turn start for different creatures") {
            val manager = TurnPhaseManager()
            
            val phase1 = manager.startTurn(5L, 30)
            phase1.creatureId shouldBe 5L
            
            val phase2 = manager.startTurn(10L, 30)
            phase2.creatureId shouldBe 10L
        }
    }

    context("Movement consumption") {
        test("movement tracks remaining distance") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            phase.movementRemaining shouldBe 30
            
            val phase2 = manager.consumeMovement(phase, 10)
            phase2.movementRemaining shouldBe 20
            
            val phase3 = manager.consumeMovement(phase2, 15)
            phase3.movementRemaining shouldBe 5
        }

        test("consume all movement") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeMovement(phase, 30)
            
            phase2.movementRemaining shouldBe 0
        }

        test("consume movement in small increments") {
            val manager = TurnPhaseManager()
            
            var phase = manager.startTurn(1L, 30)
            
            phase = manager.consumeMovement(phase, 5)
            phase.movementRemaining shouldBe 25
            
            phase = manager.consumeMovement(phase, 5)
            phase.movementRemaining shouldBe 20
            
            phase = manager.consumeMovement(phase, 5)
            phase.movementRemaining shouldBe 15
        }

        test("movement clamped to zero (cannot go negative)") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeMovement(phase, 40)
            
            phase2.movementRemaining shouldBe 0
        }
    }

    context("Action consumption") {
        test("action consumed correctly") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            phase.actionAvailable.shouldBeTrue()
            
            val phase2 = manager.consumeAction(phase)
            phase2.actionAvailable.shouldBeFalse()
        }

        test("cannot consume action twice") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeAction(phase)
            val phase3 = manager.consumeAction(phase2)
            
            phase3.actionAvailable.shouldBeFalse()
        }

        test("action consumption doesn't affect other actions") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeAction(phase)
            
            phase2.actionAvailable.shouldBeFalse()
            phase2.bonusActionAvailable.shouldBeTrue()
            phase2.reactionAvailable.shouldBeTrue()
            phase2.movementRemaining shouldBe 30
        }
    }

    context("Bonus action consumption") {
        test("bonus action consumed correctly") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            phase.bonusActionAvailable.shouldBeTrue()
            
            val phase2 = manager.consumeBonusAction(phase)
            phase2.bonusActionAvailable.shouldBeFalse()
        }

        test("cannot consume bonus action twice") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeBonusAction(phase)
            val phase3 = manager.consumeBonusAction(phase2)
            
            phase3.bonusActionAvailable.shouldBeFalse()
        }

        test("bonus action consumption doesn't affect other actions") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeBonusAction(phase)
            
            phase2.bonusActionAvailable.shouldBeFalse()
            phase2.actionAvailable.shouldBeTrue()
            phase2.reactionAvailable.shouldBeTrue()
            phase2.movementRemaining shouldBe 30
        }
    }

    context("Reaction consumption") {
        test("reaction consumed correctly") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            phase.reactionAvailable.shouldBeTrue()
            
            val phase2 = manager.consumeReaction(phase)
            phase2.reactionAvailable.shouldBeFalse()
        }

        test("cannot consume reaction twice") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeReaction(phase)
            val phase3 = manager.consumeReaction(phase2)
            
            phase3.reactionAvailable.shouldBeFalse()
        }

        test("reaction consumption doesn't affect other actions") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeReaction(phase)
            
            phase2.reactionAvailable.shouldBeFalse()
            phase2.actionAvailable.shouldBeTrue()
            phase2.bonusActionAvailable.shouldBeTrue()
            phase2.movementRemaining shouldBe 30
        }

        test("reaction restored at turn start") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeReaction(phase)
            phase2.reactionAvailable.shouldBeFalse()
            
            val phase3 = manager.restoreReaction(phase2)
            phase3.reactionAvailable.shouldBeTrue()
        }

        test("restore reaction when already available") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            phase.reactionAvailable.shouldBeTrue()
            
            val phase2 = manager.restoreReaction(phase)
            phase2.reactionAvailable.shouldBeTrue()
        }
    }

    context("Action availability checking") {
        test("check action availability") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            
            manager.isActionAvailable(phase, ActionType.Action).shouldBeTrue()
            
            val phase2 = manager.consumeAction(phase)
            manager.isActionAvailable(phase2, ActionType.Action).shouldBeFalse()
        }

        test("check bonus action availability") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            
            manager.isActionAvailable(phase, ActionType.BonusAction).shouldBeTrue()
            
            val phase2 = manager.consumeBonusAction(phase)
            manager.isActionAvailable(phase2, ActionType.BonusAction).shouldBeFalse()
        }

        test("check reaction availability") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            
            manager.isActionAvailable(phase, ActionType.Reaction).shouldBeTrue()
            
            val phase2 = manager.consumeReaction(phase)
            manager.isActionAvailable(phase2, ActionType.Reaction).shouldBeFalse()
        }

        test("check movement availability") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            
            manager.isActionAvailable(phase, ActionType.Movement).shouldBeTrue()
            
            val phase2 = manager.consumeMovement(phase, 30)
            manager.isActionAvailable(phase2, ActionType.Movement).shouldBeFalse()
        }

        test("free action always available") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            
            manager.isActionAvailable(phase, ActionType.FreeAction).shouldBeTrue()
            
            // Even after consuming everything
            val phase2 = manager.consumeAction(phase)
            val phase3 = manager.consumeBonusAction(phase2)
            val phase4 = manager.consumeReaction(phase3)
            val phase5 = manager.consumeMovement(phase4, 30)
            
            manager.isActionAvailable(phase5, ActionType.FreeAction).shouldBeTrue()
        }
    }

    context("Complex action sequences") {
        test("consume all actions in sequence") {
            val manager = TurnPhaseManager()
            
            var phase = manager.startTurn(1L, 30)
            
            manager.isActionAvailable(phase, ActionType.Action).shouldBeTrue()
            manager.isActionAvailable(phase, ActionType.BonusAction).shouldBeTrue()
            manager.isActionAvailable(phase, ActionType.Reaction).shouldBeTrue()
            manager.isActionAvailable(phase, ActionType.Movement).shouldBeTrue()
            
            phase = manager.consumeMovement(phase, 15)
            phase = manager.consumeAction(phase)
            phase = manager.consumeBonusAction(phase)
            phase = manager.consumeReaction(phase)
            
            manager.isActionAvailable(phase, ActionType.Action).shouldBeFalse()
            manager.isActionAvailable(phase, ActionType.BonusAction).shouldBeFalse()
            manager.isActionAvailable(phase, ActionType.Reaction).shouldBeFalse()
            phase.movementRemaining shouldBe 15
        }

        test("typical turn sequence: move, action, bonus action") {
            val manager = TurnPhaseManager()
            
            var phase = manager.startTurn(1L, 30)
            
            // Move 20 feet
            phase = manager.consumeMovement(phase, 20)
            phase.movementRemaining shouldBe 10
            
            // Take action
            phase = manager.consumeAction(phase)
            phase.actionAvailable.shouldBeFalse()
            
            // Take bonus action
            phase = manager.consumeBonusAction(phase)
            phase.bonusActionAvailable.shouldBeFalse()
            
            // Reaction still available
            phase.reactionAvailable.shouldBeTrue()
        }

        test("reaction used outside turn, restored at turn start") {
            val manager = TurnPhaseManager()
            
            // Creature uses reaction on someone else's turn
            var phase = manager.startTurn(1L, 30)
            phase = manager.consumeReaction(phase)
            phase.reactionAvailable.shouldBeFalse()
            
            // New turn starts, reaction restored
            val newTurnPhase = manager.startTurn(1L, 30)
            newTurnPhase.reactionAvailable.shouldBeTrue()
        }
    }

    context("Edge cases") {
        test("zero movement speed") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 0)
            
            phase.movementRemaining shouldBe 0
            manager.isActionAvailable(phase, ActionType.Movement).shouldBeFalse()
        }

        test("very high movement speed") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 120)
            
            phase.movementRemaining shouldBe 120
            manager.isActionAvailable(phase, ActionType.Movement).shouldBeTrue()
        }

        test("consume zero movement") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeMovement(phase, 0)
            
            phase2.movementRemaining shouldBe 30
        }

        test("immutability - original phase unchanged after consumption") {
            val manager = TurnPhaseManager()
            
            val phase = manager.startTurn(1L, 30)
            val phase2 = manager.consumeAction(phase)
            
            // Original phase should be unchanged
            phase.actionAvailable.shouldBeTrue()
            phase2.actionAvailable.shouldBeFalse()
        }
    }
})
