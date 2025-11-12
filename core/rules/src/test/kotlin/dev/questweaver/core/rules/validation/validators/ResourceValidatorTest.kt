package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ActionChoice
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.state.Resource
import dev.questweaver.core.rules.validation.state.ResourcePool
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for ResourceValidator.
 *
 * Tests verify that:
 * - Spell slot validation requires available slot
 * - Upcasting works (higher-level slot for lower-level spell)
 * - Cantrips don't consume spell slots
 * - Class features require remaining uses
 * - Item charges require remaining charges
 * - Resource consumption updates pool correctly
 */
class ResourceValidatorTest : FunSpec({
    val validator = ResourceValidator()
    val actorId = 1L

    context("Spell slot validation") {
        test("requires available spell slot") {
            val resourcePool = ResourcePool(
                spellSlots = mapOf(1 to 0, 2 to 0) // No spell slots available
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                slotLevel = 1
            )

            val result = validator.validateResources(action, resourcePool)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.InsufficientResources>()
            val insufficient = failure as ValidationFailure.InsufficientResources
            insufficient.required shouldBe Resource.SpellSlot(1)
            insufficient.available shouldBe 0
            insufficient.needed shouldBe 1
        }

        test("succeeds with available spell slot") {
            val resourcePool = ResourcePool(
                spellSlots = mapOf(1 to 2, 2 to 1)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                slotLevel = 1
            )

            val result = validator.validateResources(action, resourcePool)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.resources shouldContain Resource.SpellSlot(1)
        }
    }

    context("Upcasting") {
        test("higher-level slot works for lower-level spell") {
            val resourcePool = ResourcePool(
                spellSlots = mapOf(1 to 0, 2 to 1) // No 1st level, but has 2nd level
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                slotLevel = 2 // Upcast to 2nd level
            )

            val result = validator.validateResources(action, resourcePool)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.resources shouldContain Resource.SpellSlot(2)
        }

        test("prompts for slot level choice when multiple available") {
            val resourcePool = ResourcePool(
                spellSlots = mapOf(1 to 1, 2 to 1, 3 to 1) // Multiple levels available
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                slotLevel = null // No slot level specified
            )

            val result = validator.validateResources(action, resourcePool)

            result.shouldBeInstanceOf<ValidationResult.RequiresChoice>()
            val choices = (result as ValidationResult.RequiresChoice).choices
            choices.size shouldBe 1
            choices[0].shouldBeInstanceOf<ActionChoice.SpellSlotLevel>()
            val slotChoice = choices[0] as ActionChoice.SpellSlotLevel
            slotChoice.minLevel shouldBe 1
            slotChoice.availableLevels shouldBe listOf(1, 2, 3)
        }

        test("uses only available slot when single option") {
            val resourcePool = ResourcePool(
                spellSlots = mapOf(1 to 1) // Only one level available
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                slotLevel = null // No slot level specified
            )

            val result = validator.validateResources(action, resourcePool)

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Cantrips") {
        test("cantrips don't consume spell slots") {
            val resourcePool = ResourcePool(
                spellSlots = mapOf(1 to 0) // No spell slots available
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "fire_bolt_cantrip",
                slotLevel = null
            )

            val result = validator.validateResources(action, resourcePool)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.resources.isEmpty() shouldBe true
        }
    }

    context("Class features") {
        test("requires remaining uses") {
            val resourcePool = ResourcePool(
                classFeatures = mapOf("action_surge" to 0) // No uses remaining
            )
            val action = GameAction.UseClassFeature(
                actorId = actorId,
                featureId = "action_surge"
            )

            val result = validator.validateResources(action, resourcePool)

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.InsufficientResources>()
            val insufficient = failure as ValidationFailure.InsufficientResources
            insufficient.required shouldBe Resource.ClassFeature("action_surge", 1)
            insufficient.available shouldBe 0
        }

        test("succeeds with remaining uses") {
            val resourcePool = ResourcePool(
                classFeatures = mapOf("action_surge" to 1)
            )
            val action = GameAction.UseClassFeature(
                actorId = actorId,
                featureId = "action_surge"
            )

            val result = validator.validateResources(action, resourcePool)

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.resources shouldContain Resource.ClassFeature("action_surge", 1)
        }
    }

    context("Item charges") {
        test("requires remaining charges") {
            val itemId = 100L
            val resourcePool = ResourcePool(
                itemCharges = mapOf(itemId to 0) // No charges remaining
            )
            // Simulate using an item (would need actual item action type)
            val resource = Resource.ItemCharge(itemId, 1)

            resourcePool.hasResource(resource) shouldBe false
        }

        test("succeeds with remaining charges") {
            val itemId = 100L
            val resourcePool = ResourcePool(
                itemCharges = mapOf(itemId to 3)
            )
            val resource = Resource.ItemCharge(itemId, 1)

            resourcePool.hasResource(resource) shouldBe true
        }
    }

    context("Resource consumption") {
        test("consuming spell slot updates pool correctly") {
            val resourcePool = ResourcePool(
                spellSlots = mapOf(1 to 2, 2 to 1)
            )
            val resource = Resource.SpellSlot(1)

            val updatedPool = resourcePool.consume(resource)

            updatedPool.spellSlots[1] shouldBe 1
            updatedPool.spellSlots[2] shouldBe 1
        }

        test("consuming class feature updates pool correctly") {
            val resourcePool = ResourcePool(
                classFeatures = mapOf("action_surge" to 2)
            )
            val resource = Resource.ClassFeature("action_surge", 1)

            val updatedPool = resourcePool.consume(resource)

            updatedPool.classFeatures["action_surge"] shouldBe 1
        }

        test("consuming item charge updates pool correctly") {
            val itemId = 100L
            val resourcePool = ResourcePool(
                itemCharges = mapOf(itemId to 5)
            )
            val resource = Resource.ItemCharge(itemId, 2)

            val updatedPool = resourcePool.consume(resource)

            updatedPool.itemCharges[itemId] shouldBe 3
        }

        test("consuming hit dice updates pool correctly") {
            val resourcePool = ResourcePool(
                hitDice = mapOf("d8" to 4)
            )
            val resource = Resource.HitDice("d8", 1)

            val updatedPool = resourcePool.consume(resource)

            updatedPool.hitDice["d8"] shouldBe 3
        }
    }

    context("Resource cost determination") {
        test("getResourceCost for spell includes spell slot") {
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                slotLevel = 1
            )

            val cost = validator.getResourceCost(action)

            cost.resources shouldContain Resource.SpellSlot(1)
        }

        test("getResourceCost for cantrip has no resources") {
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "fire_bolt_cantrip",
                slotLevel = null
            )

            val cost = validator.getResourceCost(action)

            cost.resources.isEmpty() shouldBe true
        }

        test("getResourceCost for class feature includes feature use") {
            val action = GameAction.UseClassFeature(
                actorId = actorId,
                featureId = "action_surge"
            )

            val cost = validator.getResourceCost(action)

            cost.resources shouldContain Resource.ClassFeature("action_surge", 1)
        }

        test("getResourceCost for attack has no resources") {
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = 2L,
                weaponId = null
            )

            val cost = validator.getResourceCost(action)

            cost.resources.isEmpty() shouldBe true
        }
    }
})
