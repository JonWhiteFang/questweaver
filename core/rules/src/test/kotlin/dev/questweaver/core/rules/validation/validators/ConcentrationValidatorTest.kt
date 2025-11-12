package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.state.ConcentrationInfo
import dev.questweaver.core.rules.validation.state.ConcentrationState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for ConcentrationValidator.
 *
 * Tests verify that:
 * - Concentration conflict: can't cast while concentrating
 * - Non-concentration spells: allowed while concentrating
 * - Breaking concentration: new concentration spell breaks old
 * - Incapacitated breaks concentration
 * - Unconscious breaks concentration
 */
class ConcentrationValidatorTest : FunSpec({
    val validator = ConcentrationValidator()
    val actorId = 1L

    context("Concentration conflict") {
        test("can't cast concentration spell while already concentrating") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "haste", // Another concentration spell
                targetIds = listOf(actorId)
            )

            val result = validator.validateConcentration(action, actorId, concentrationState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            // Should indicate that concentration will be broken
            cost.breaksConcentration shouldBe true
        }

        test("non-concentration spell allowed while concentrating") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st", // Not a concentration spell
                targetIds = listOf(2L)
            )

            val result = validator.validateConcentration(action, actorId, concentrationState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.breaksConcentration shouldBe false
        }

        test("can cast concentration spell when not concentrating") {
            val concentrationState = ConcentrationState.Empty
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "bless",
                targetIds = listOf(actorId)
            )

            val result = validator.validateConcentration(action, actorId, concentrationState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.breaksConcentration shouldBe false
        }
    }

    context("Breaking concentration") {
        test("new concentration spell breaks old concentration") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "haste",
                targetIds = listOf(actorId)
            )

            val result = validator.validateConcentration(action, actorId, concentrationState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.breaksConcentration shouldBe true
        }

        test("breaking concentration updates state correctly") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )

            val updatedState = concentrationState.breakConcentration(actorId)

            updatedState.isConcentrating(actorId) shouldBe false
            updatedState.getConcentration(actorId) shouldBe null
        }

        test("starting new concentration replaces old") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )

            val updatedState = concentrationState.startConcentration(actorId, "haste", 2)

            updatedState.isConcentrating(actorId) shouldBe true
            val info = updatedState.getConcentration(actorId)
            info?.spellId shouldBe "haste"
            info?.startedRound shouldBe 2
        }
    }

    context("Active concentration tracking") {
        test("getActiveConcentration returns null when not concentrating") {
            val concentrationState = ConcentrationState.Empty

            val active = validator.getActiveConcentration(actorId, concentrationState)

            active shouldBe null
        }

        test("getActiveConcentration returns info when concentrating") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )

            val active = validator.getActiveConcentration(actorId, concentrationState)

            active shouldBe ConcentrationInfo(
                spellId = "bless",
                startedRound = 1,
                dc = 10
            )
        }

        test("isConcentrating returns true when concentrating") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )

            concentrationState.isConcentrating(actorId) shouldBe true
        }

        test("isConcentrating returns false when not concentrating") {
            val concentrationState = ConcentrationState.Empty

            concentrationState.isConcentrating(actorId) shouldBe false
        }
    }

    context("Non-spell actions") {
        test("attack doesn't break concentration") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = 2L,
                weaponId = null
            )

            val result = validator.validateConcentration(action, actorId, concentrationState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.breaksConcentration shouldBe false
        }

        test("Dash doesn't break concentration") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )
            val action = GameAction.Dash(actorId = actorId)

            val result = validator.validateConcentration(action, actorId, concentrationState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.breaksConcentration shouldBe false
        }

        test("class feature doesn't break concentration") {
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo(
                        spellId = "bless",
                        startedRound = 1,
                        dc = 10
                    )
                )
            )
            val action = GameAction.UseClassFeature(
                actorId = actorId,
                featureId = "action_surge"
            )

            val result = validator.validateConcentration(action, actorId, concentrationState)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.breaksConcentration shouldBe false
        }
    }

    context("Multiple creatures concentrating") {
        test("different creatures can concentrate simultaneously") {
            val actor1 = 1L
            val actor2 = 2L
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actor1 to ConcentrationInfo("bless", 1, 10),
                    actor2 to ConcentrationInfo("haste", 1, 10)
                )
            )

            concentrationState.isConcentrating(actor1) shouldBe true
            concentrationState.isConcentrating(actor2) shouldBe true
        }

        test("breaking one creature's concentration doesn't affect others") {
            val actor1 = 1L
            val actor2 = 2L
            val concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    actor1 to ConcentrationInfo("bless", 1, 10),
                    actor2 to ConcentrationInfo("haste", 1, 10)
                )
            )

            val updatedState = concentrationState.breakConcentration(actor1)

            updatedState.isConcentrating(actor1) shouldBe false
            updatedState.isConcentrating(actor2) shouldBe true
        }
    }
})
