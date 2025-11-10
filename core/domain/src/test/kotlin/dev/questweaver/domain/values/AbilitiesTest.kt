package dev.questweaver.domain.values

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class AbilitiesTest : FunSpec({
    
    context("Abilities validation") {
        test("should create Abilities with default values of 10") {
            val abilities = Abilities()
            
            abilities.strength shouldBe 10
            abilities.dexterity shouldBe 10
            abilities.constitution shouldBe 10
            abilities.intelligence shouldBe 10
            abilities.wisdom shouldBe 10
            abilities.charisma shouldBe 10
        }
        
        test("should create Abilities with custom values") {
            val abilities = Abilities(
                strength = 18,
                dexterity = 14,
                constitution = 16,
                intelligence = 12,
                wisdom = 10,
                charisma = 8
            )
            
            abilities.strength shouldBe 18
            abilities.dexterity shouldBe 14
            abilities.constitution shouldBe 16
            abilities.intelligence shouldBe 12
            abilities.wisdom shouldBe 10
            abilities.charisma shouldBe 8
        }
        
        test("should accept minimum valid score of 1") {
            shouldNotThrowAny {
                Abilities(strength = 1, dexterity = 1, constitution = 1, 
                         intelligence = 1, wisdom = 1, charisma = 1)
            }
        }
        
        test("should accept maximum valid score of 30") {
            shouldNotThrowAny {
                Abilities(strength = 30, dexterity = 30, constitution = 30,
                         intelligence = 30, wisdom = 30, charisma = 30)
            }
        }
        
        test("should throw exception for strength below 1") {
            shouldThrow<IllegalArgumentException> {
                Abilities(strength = 0)
            }
        }
        
        test("should throw exception for strength above 30") {
            shouldThrow<IllegalArgumentException> {
                Abilities(strength = 31)
            }
        }
        
        test("should throw exception for dexterity out of range") {
            shouldThrow<IllegalArgumentException> {
                Abilities(dexterity = 0)
            }
        }
        
        test("should throw exception for constitution out of range") {
            shouldThrow<IllegalArgumentException> {
                Abilities(constitution = 31)
            }
        }
        
        test("should throw exception for intelligence out of range") {
            shouldThrow<IllegalArgumentException> {
                Abilities(intelligence = -1)
            }
        }
        
        test("should throw exception for wisdom out of range") {
            shouldThrow<IllegalArgumentException> {
                Abilities(wisdom = 50)
            }
        }
        
        test("should throw exception for charisma out of range") {
            shouldThrow<IllegalArgumentException> {
                Abilities(charisma = 0)
            }
        }
    }
    
    context("ability modifiers following D&D 5e rules") {
        test("score of 10 should have modifier of 0") {
            val abilities = Abilities(strength = 10)
            abilities.strModifier shouldBe 0
        }
        
        test("score of 11 should have modifier of 0") {
            val abilities = Abilities(strength = 11)
            abilities.strModifier shouldBe 0
        }
        
        test("score of 12 should have modifier of +1") {
            val abilities = Abilities(dexterity = 12)
            abilities.dexModifier shouldBe 1
        }
        
        test("score of 20 should have modifier of +5") {
            val abilities = Abilities(constitution = 20)
            abilities.conModifier shouldBe 5
        }
        
        test("score of 8 should have modifier of -1") {
            val abilities = Abilities(intelligence = 8)
            abilities.intModifier shouldBe -1
        }
        
        test("score of 9 should have modifier of -1") {
            val abilities = Abilities(wisdom = 9)
            abilities.wisModifier shouldBe -1
        }
        
        test("score of 1 should have modifier of -5 (floor division)") {
            val abilities = Abilities(charisma = 1)
            // (1 - 10) / 2 = -9 / 2 = -4.5, floor to -5
            abilities.chaModifier shouldBe -5
        }
        
        test("score of 30 should have modifier of +10") {
            val abilities = Abilities(strength = 30)
            abilities.strModifier shouldBe 10
        }
        
        test("all ability modifiers should be calculated correctly") {
            val abilities = Abilities(
                strength = 18,    // +4
                dexterity = 14,   // +2
                constitution = 16, // +3
                intelligence = 12, // +1
                wisdom = 10,      // +0
                charisma = 8      // -1
            )
            
            abilities.strModifier shouldBe 4
            abilities.dexModifier shouldBe 2
            abilities.conModifier shouldBe 3
            abilities.intModifier shouldBe 1
            abilities.wisModifier shouldBe 0
            abilities.chaModifier shouldBe -1
        }
    }
    
    context("property-based tests for ability modifiers") {
        test("modifier formula should work for all valid scores") {
            checkAll(Arb.int(1..30)) { score ->
                val abilities = Abilities(strength = score)
                val expectedModifier = Math.floorDiv(score - 10, 2)
                abilities.strModifier shouldBe expectedModifier
            }
        }
        
        test("all ability modifiers should follow same formula") {
            checkAll(Arb.int(1..30)) { score ->
                val abilities = Abilities(
                    strength = score,
                    dexterity = score,
                    constitution = score,
                    intelligence = score,
                    wisdom = score,
                    charisma = score
                )
                
                val expectedModifier = Math.floorDiv(score - 10, 2)
                abilities.strModifier shouldBe expectedModifier
                abilities.dexModifier shouldBe expectedModifier
                abilities.conModifier shouldBe expectedModifier
                abilities.intModifier shouldBe expectedModifier
                abilities.wisModifier shouldBe expectedModifier
                abilities.chaModifier shouldBe expectedModifier
            }
        }
    }
})
