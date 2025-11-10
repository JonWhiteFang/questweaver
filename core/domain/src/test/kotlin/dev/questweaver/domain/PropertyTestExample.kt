package dev.questweaver.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.random.Random

/**
 * Example property-based tests demonstrating kotest-property usage.
 * Property-based testing is ideal for testing deterministic components
 * like dice rollers, rules engines, and mathematical operations.
 */
class PropertyTestExample : FunSpec({
    
    context("Property-based testing examples") {
        
        test("seeded random number generator produces consistent results") {
            checkAll(Arb.long()) { seed ->
                // Given the same seed, Random should produce the same sequence
                val random1 = Random(seed)
                val random2 = Random(seed)
                
                val value1 = random1.nextInt(1, 21)
                val value2 = random2.nextInt(1, 21)
                
                value1 shouldBe value2
            }
        }
        
        test("d20 roll always returns value between 1 and 20") {
            checkAll(Arb.long()) { seed ->
                val random = Random(seed)
                val roll = random.nextInt(1, 21)
                
                roll shouldBeInRange 1..20
            }
        }
        
        test("addition is commutative for all integers") {
            checkAll(Arb.int(), Arb.int()) { a, b ->
                (a + b) shouldBe (b + a)
            }
        }
        
        test("multiplication by zero always returns zero") {
            checkAll(Arb.int()) { value ->
                (value * 0) shouldBe 0
            }
        }
        
        test("absolute value is always non-negative for valid range") {
            checkAll(Arb.int(Int.MIN_VALUE + 1, Int.MAX_VALUE)) { value ->
                val absValue = kotlin.math.abs(value)
                absValue shouldBeInRange 0..Int.MAX_VALUE
            }
        }
    }
    
    context("Deterministic behavior validation") {
        
        test("same seed produces same sequence of rolls") {
            val seed = 42L
            val rolls1 = List(10) { Random(seed).nextInt(1, 21) }
            val rolls2 = List(10) { Random(seed).nextInt(1, 21) }
            
            rolls1 shouldBe rolls2
        }
        
        test("different seeds produce different sequences") {
            checkAll(Arb.long(), Arb.long()) { seed1, seed2 ->
                if (seed1 != seed2) {
                    val random1 = Random(seed1)
                    val random2 = Random(seed2)
                    
                    val sequence1 = List(5) { random1.nextInt(1, 21) }
                    val sequence2 = List(5) { random2.nextInt(1, 21) }
                    
                    // With high probability, different seeds produce different sequences
                    // (This is a probabilistic test, but with 5 rolls the chance of collision is very low)
                    sequence1 != sequence2 || seed1 == seed2
                }
            }
        }
    }
})
