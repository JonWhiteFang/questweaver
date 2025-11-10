package dev.questweaver.domain.entities

import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.Condition
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CreatureTest : FunSpec({
    
    context("Creature validation") {
        test("should create Creature with valid values") {
            val creature = Creature(
                id = 1,
                name = "Goblin",
                armorClass = 15,
                hitPointsCurrent = 20,
                hitPointsMax = 20,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.id shouldBe 1
            creature.name shouldBe "Goblin"
            creature.armorClass shouldBe 15
            creature.hitPointsCurrent shouldBe 20
            creature.hitPointsMax shouldBe 20
            creature.speed shouldBe 30
        }
        
        test("should create Creature with default proficiency bonus of 2") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 10,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.proficiencyBonus shouldBe 2
        }
        
        test("should create Creature with custom proficiency bonus") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 10,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities(),
                proficiencyBonus = 5
            )
            
            creature.proficiencyBonus shouldBe 5
        }
        
        test("should create Creature with empty conditions by default") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 10,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.conditions shouldBe emptySet()
        }
        
        test("should create Creature with conditions") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 10,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities(),
                conditions = setOf(Condition.POISONED, Condition.PRONE)
            )
            
            creature.conditions shouldBe setOf(Condition.POISONED, Condition.PRONE)
        }
        
        test("should throw exception for non-positive id") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 0,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for negative id") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = -1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for blank name") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 1,
                    name = "",
                    armorClass = 10,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for whitespace-only name") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 1,
                    name = "   ",
                    armorClass = 10,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for non-positive armor class") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 0,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for non-positive max HP") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 0,
                    hitPointsMax = 0,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for current HP below 0") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = -1,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for current HP above max HP") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 11,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should accept current HP equal to max HP") {
            shouldNotThrowAny {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should accept current HP of 0") {
            shouldNotThrowAny {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 0,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for negative speed") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = -1,
                    abilities = Abilities()
                )
            }
        }
        
        test("should accept speed of 0") {
            shouldNotThrowAny {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = 0,
                    abilities = Abilities()
                )
            }
        }
        
        test("should throw exception for negative proficiency bonus") {
            shouldThrow<IllegalArgumentException> {
                Creature(
                    id = 1,
                    name = "Test",
                    armorClass = 10,
                    hitPointsCurrent = 10,
                    hitPointsMax = 10,
                    speed = 30,
                    abilities = Abilities(),
                    proficiencyBonus = -1
                )
            }
        }
    }
    
    context("Creature computed properties") {
        test("isAlive should be true when HP is above 0") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 5,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.isAlive shouldBe true
        }
        
        test("isAlive should be false when HP is 0") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 0,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.isAlive shouldBe false
        }
        
        test("isBloodied should be true when HP is at half max") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 5,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.isBloodied shouldBe true
        }
        
        test("isBloodied should be true when HP is below half max") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 4,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.isBloodied shouldBe true
        }
        
        test("isBloodied should be false when HP is above half max") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 6,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.isBloodied shouldBe false
        }
        
        test("isBloodied should be false when HP is at max") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 10,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            creature.isBloodied shouldBe false
        }
    }
    
    context("Creature copy operations") {
        test("should create modified copy with different HP") {
            val original = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 10,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            val modified = original.copy(hitPointsCurrent = 5)
            
            modified.hitPointsCurrent shouldBe 5
            modified.id shouldBe original.id
            modified.name shouldBe original.name
        }
        
        test("should create modified copy with different conditions") {
            val original = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 10,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            val modified = original.copy(conditions = setOf(Condition.STUNNED))
            
            modified.conditions shouldBe setOf(Condition.STUNNED)
            original.conditions shouldBe emptySet()
        }
    }
})
