package dev.questweaver.domain.values

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DiceRollTest : FunSpec({
    
    context("DiceRoll validation") {
        test("should create valid DiceRoll for d20") {
            val roll = DiceRoll(diceType = 20, count = 1, modifier = 0, result = 15)
            
            roll.diceType shouldBe 20
            roll.count shouldBe 1
            roll.modifier shouldBe 0
            roll.result shouldBe 15
        }
        
        test("should create valid DiceRoll with positive modifier") {
            val roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20)
            
            roll.modifier shouldBe 5
            roll.result shouldBe 20
        }
        
        test("should create valid DiceRoll with negative modifier") {
            val roll = DiceRoll(diceType = 20, count = 1, modifier = -2, result = 10)
            
            roll.modifier shouldBe -2
            roll.result shouldBe 10
        }
        
        test("should create valid DiceRoll with multiple dice") {
            val roll = DiceRoll(diceType = 6, count = 3, modifier = 2, result = 15)
            
            roll.count shouldBe 3
            roll.result shouldBe 15
        }
        
        test("should accept all valid dice types") {
            DiceRoll.VALID_DICE_TYPES.forEach { diceType ->
                val roll = DiceRoll(diceType = diceType, count = 1, modifier = 0, result = diceType)
                roll.diceType shouldBe diceType
            }
        }
        
        test("should throw exception for invalid dice type") {
            shouldThrow<IllegalArgumentException> {
                DiceRoll(diceType = 7, count = 1, modifier = 0, result = 5)
            }
        }
        
        test("should throw exception for zero count") {
            shouldThrow<IllegalArgumentException> {
                DiceRoll(diceType = 20, count = 0, modifier = 0, result = 10)
            }
        }
        
        test("should throw exception for negative count") {
            shouldThrow<IllegalArgumentException> {
                DiceRoll(diceType = 20, count = -1, modifier = 0, result = 10)
            }
        }
        
        test("should throw exception for result below minimum possible") {
            // 1d20+5 minimum is 6 (1+5)
            shouldThrow<IllegalArgumentException> {
                DiceRoll(diceType = 20, count = 1, modifier = 5, result = 5)
            }
        }
        
        test("should throw exception for result above maximum possible") {
            // 1d20+5 maximum is 25 (20+5)
            shouldThrow<IllegalArgumentException> {
                DiceRoll(diceType = 20, count = 1, modifier = 5, result = 26)
            }
        }
        
        test("should accept result at minimum possible value") {
            // 2d6+3 minimum is 5 (2+3)
            val roll = DiceRoll(diceType = 6, count = 2, modifier = 3, result = 5)
            roll.result shouldBe 5
        }
        
        test("should accept result at maximum possible value") {
            // 2d6+3 maximum is 15 (12+3)
            val roll = DiceRoll(diceType = 6, count = 2, modifier = 3, result = 15)
            roll.result shouldBe 15
        }
        
        test("should validate range with negative modifier") {
            // 1d20-2 minimum is -1 (1-2), maximum is 18 (20-2)
            val roll = DiceRoll(diceType = 20, count = 1, modifier = -2, result = 10)
            roll.result shouldBe 10
        }
        
        test("should throw exception for result below minimum with negative modifier") {
            // 1d20-2 minimum is -1
            shouldThrow<IllegalArgumentException> {
                DiceRoll(diceType = 20, count = 1, modifier = -2, result = -2)
            }
        }
    }
    
    context("toString method") {
        test("should format roll with positive modifier") {
            val roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20)
            roll.toString() shouldBe "1d20+5 = 20"
        }
        
        test("should format roll with negative modifier") {
            val roll = DiceRoll(diceType = 20, count = 1, modifier = -2, result = 10)
            roll.toString() shouldBe "1d20-2 = 10"
        }
        
        test("should format roll with no modifier") {
            val roll = DiceRoll(diceType = 20, count = 1, modifier = 0, result = 15)
            roll.toString() shouldBe "1d20 = 15"
        }
        
        test("should format roll with multiple dice") {
            val roll = DiceRoll(diceType = 6, count = 3, modifier = 2, result = 15)
            roll.toString() shouldBe "3d6+2 = 15"
        }
        
        test("should format d4 roll") {
            val roll = DiceRoll(diceType = 4, count = 2, modifier = 1, result = 7)
            roll.toString() shouldBe "2d4+1 = 7"
        }
        
        test("should format d100 roll") {
            val roll = DiceRoll(diceType = 100, count = 1, modifier = 0, result = 50)
            roll.toString() shouldBe "1d100 = 50"
        }
        
        test("toString should contain dice type") {
            val roll = DiceRoll(diceType = 12, count = 1, modifier = 0, result = 8)
            roll.toString() shouldContain "d12"
        }
        
        test("toString should contain result") {
            val roll = DiceRoll(diceType = 20, count = 1, modifier = 0, result = 15)
            roll.toString() shouldContain "15"
        }
    }
    
    context("valid dice types") {
        test("VALID_DICE_TYPES should contain all D&D dice") {
            DiceRoll.VALID_DICE_TYPES shouldBe setOf(4, 6, 8, 10, 12, 20, 100)
        }
        
        test("should accept d4") {
            val roll = DiceRoll(diceType = 4, count = 1, modifier = 0, result = 3)
            roll.diceType shouldBe 4
        }
        
        test("should accept d6") {
            val roll = DiceRoll(diceType = 6, count = 1, modifier = 0, result = 4)
            roll.diceType shouldBe 6
        }
        
        test("should accept d8") {
            val roll = DiceRoll(diceType = 8, count = 1, modifier = 0, result = 5)
            roll.diceType shouldBe 8
        }
        
        test("should accept d10") {
            val roll = DiceRoll(diceType = 10, count = 1, modifier = 0, result = 7)
            roll.diceType shouldBe 10
        }
        
        test("should accept d12") {
            val roll = DiceRoll(diceType = 12, count = 1, modifier = 0, result = 9)
            roll.diceType shouldBe 12
        }
        
        test("should accept d20") {
            val roll = DiceRoll(diceType = 20, count = 1, modifier = 0, result = 15)
            roll.diceType shouldBe 20
        }
        
        test("should accept d100") {
            val roll = DiceRoll(diceType = 100, count = 1, modifier = 0, result = 50)
            roll.diceType shouldBe 100
        }
    }
})
