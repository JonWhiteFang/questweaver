package dev.questweaver.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DomainErrorTest : FunSpec({
    context("InvalidEntity") {
        test("creates error with entity type and reason") {
            val error = DomainError.InvalidEntity(
                entityType = "Creature",
                reason = "HP cannot be negative"
            )

            error.entityType shouldBe "Creature"
            error.reason shouldBe "HP cannot be negative"
            error.message shouldBe "Invalid Creature: HP cannot be negative"
        }

        test("is a DomainError") {
            val error = DomainError.InvalidEntity("Campaign", "Name cannot be blank")
            error.shouldBeInstanceOf<DomainError>()
        }

        test("is an Exception") {
            val error = DomainError.InvalidEntity("Encounter", "No participants")
            error.shouldBeInstanceOf<Exception>()
        }
    }

    context("InvalidOperation") {
        test("creates error with operation name and reason") {
            val error = DomainError.InvalidOperation(
                operation = "AttackCreature",
                reason = "Target is out of range"
            )

            error.operation shouldBe "AttackCreature"
            error.reason shouldBe "Target is out of range"
            error.message shouldBe "Invalid operation 'AttackCreature': Target is out of range"
        }

        test("is a DomainError") {
            val error = DomainError.InvalidOperation("MoveToPosition", "Path is blocked")
            error.shouldBeInstanceOf<DomainError>()
        }

        test("is an Exception") {
            val error = DomainError.InvalidOperation("CastSpell", "No spell slots remaining")
            error.shouldBeInstanceOf<Exception>()
        }
    }

    context("NotFound") {
        test("creates error with entity type and id") {
            val error = DomainError.NotFound(
                entityType = "Creature",
                id = 42L
            )

            error.entityType shouldBe "Creature"
            error.id shouldBe 42L
            error.message shouldBe "Creature with id 42 not found"
        }

        test("is a DomainError") {
            val error = DomainError.NotFound("Campaign", 123L)
            error.shouldBeInstanceOf<DomainError>()
        }

        test("is an Exception") {
            val error = DomainError.NotFound("Encounter", 999L)
            error.shouldBeInstanceOf<Exception>()
        }
    }

    context("exhaustive when expressions") {
        test("handles all DomainError subtypes") {
            val errors = listOf(
                DomainError.InvalidEntity("Creature", "Invalid HP"),
                DomainError.InvalidOperation("Attack", "Out of range"),
                DomainError.NotFound("Campaign", 1L)
            )

            errors.forEach { error ->
                val result = when (error) {
                    is DomainError.InvalidEntity -> "invalid-entity"
                    is DomainError.InvalidOperation -> "invalid-operation"
                    is DomainError.NotFound -> "not-found"
                }
                result shouldBe when (error) {
                    is DomainError.InvalidEntity -> "invalid-entity"
                    is DomainError.InvalidOperation -> "invalid-operation"
                    is DomainError.NotFound -> "not-found"
                }
            }
        }
    }
})
