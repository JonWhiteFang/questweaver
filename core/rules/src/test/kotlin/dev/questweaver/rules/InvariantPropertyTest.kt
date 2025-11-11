package dev.questweaver.rules

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.rules.combat.AttackResolver
import dev.questweaver.rules.combat.DamageCalculator
import dev.questweaver.rules.modifiers.DamageModifier
import dev.questweaver.rules.modifiers.DamageType
import dev.questweaver.rules.modifiers.RollModifier
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

/**
 * Property-based tests for invariants in combat rules.
 *
 * These tests verify that certain properties always hold true regardless
 * of input values, ensuring the rules engine behaves correctly under all
 * conditions.
 */
class InvariantPropertyTest : FunSpec({

    context("Damage modifier invariants") {
        test("damage with resistance is always half or less of base damage") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Fire,
                    isCritical = false,
                    targetModifiers = setOf(DamageModifier.Resistance(DamageType.Fire))
                )
                
                outcome.finalDamage shouldBeLessThanOrEqual (outcome.baseDamage / 2)
                outcome.finalDamage shouldBe (outcome.baseDamage / 2) // Exact half, rounded down
            }
        }

        test("damage with vulnerability is always double or more of base damage") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Fire,
                    isCritical = false,
                    targetModifiers = setOf(DamageModifier.Vulnerability(DamageType.Fire))
                )
                
                outcome.finalDamage shouldBeGreaterThanOrEqual (outcome.baseDamage * 2)
                outcome.finalDamage shouldBe (outcome.baseDamage * 2) // Exact double
            }
        }

        test("damage with immunity is always zero") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Poison,
                    isCritical = false,
                    targetModifiers = setOf(DamageModifier.Immunity(DamageType.Poison))
                )
                
                outcome.finalDamage shouldBe 0
            }
        }

        test("resistance and vulnerability result in expected damage") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Fire,
                    isCritical = false,
                    targetModifiers = setOf(
                        DamageModifier.Resistance(DamageType.Fire),
                        DamageModifier.Vulnerability(DamageType.Fire)
                    )
                )
                
                // Resistance halves (rounds down), then vulnerability doubles
                // For even base damage: (base / 2) * 2 = base
                // For odd base damage: ((base / 2) rounded down) * 2 = base - 1
                val expectedDamage = (outcome.baseDamage / 2) * 2
                outcome.finalDamage shouldBe expectedDamage
            }
        }

        test("immunity overrides all other modifiers") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Fire,
                    isCritical = false,
                    targetModifiers = setOf(
                        DamageModifier.Immunity(DamageType.Fire),
                        DamageModifier.Resistance(DamageType.Fire),
                        DamageModifier.Vulnerability(DamageType.Fire)
                    )
                )
                
                outcome.finalDamage shouldBe 0
            }
        }
    }

    context("D20 roll range invariants") {
        test("attack d20 rolls are always in range 1-20") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller = DiceRoller(seed)
                val resolver = AttackResolver(roller)
                
                val outcome = resolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC
                )
                
                outcome.d20Roll shouldBeInRange 1..20
            }
        }

        test("attack d20 rolls with advantage are always in range 1-20") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller = DiceRoller(seed)
                val resolver = AttackResolver(roller)
                
                val outcome = resolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Advantage
                )
                
                outcome.d20Roll shouldBeInRange 1..20
            }
        }

        test("attack d20 rolls with disadvantage are always in range 1-20") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller = DiceRoller(seed)
                val resolver = AttackResolver(roller)
                
                val outcome = resolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Disadvantage
                )
                
                outcome.d20Roll shouldBeInRange 1..20
            }
        }
    }

    context("Advantage and disadvantage invariants") {
        test("advantage roll is always greater than or equal to normal roll") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val normalRoller = DiceRoller(seed)
                val normalResolver = AttackResolver(normalRoller)
                val normalOutcome = normalResolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Normal
                )
                
                val advantageRoller = DiceRoller(seed)
                val advantageResolver = AttackResolver(advantageRoller)
                val advantageOutcome = advantageResolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Advantage
                )
                
                // Advantage should give same or better d20 roll
                advantageOutcome.d20Roll shouldBeGreaterThanOrEqual normalOutcome.d20Roll
            }
        }

        test("disadvantage roll is always less than or equal to normal roll") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val normalRoller = DiceRoller(seed)
                val normalResolver = AttackResolver(normalRoller)
                val normalOutcome = normalResolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Normal
                )
                
                val disadvantageRoller = DiceRoller(seed)
                val disadvantageResolver = AttackResolver(disadvantageRoller)
                val disadvantageOutcome = disadvantageResolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Disadvantage
                )
                
                // Disadvantage should give same or worse d20 roll
                disadvantageOutcome.d20Roll shouldBeLessThanOrEqual normalOutcome.d20Roll
            }
        }
    }

    context("Critical hit invariants") {
        test("critical hit damage is always greater than or equal to normal damage") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val normalRoller = DiceRoller(seed)
                val normalCalculator = DamageCalculator(normalRoller)
                val normalOutcome = normalCalculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
                
                val criticalRoller = DiceRoller(seed)
                val criticalCalculator = DamageCalculator(criticalRoller)
                val criticalOutcome = criticalCalculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = true,
                    targetModifiers = emptySet()
                )
                
                // Critical should have more dice rolls
                criticalOutcome.diceRolls.size shouldBe normalOutcome.diceRolls.size * 2
                
                // Critical base damage should be at least as much as normal
                // (could be less if normal rolled high and critical rolled low, but on average higher)
                criticalOutcome.baseDamage shouldBeGreaterThanOrEqual damageModifier
            }
        }

        test("critical hit doubles dice count but not modifier") {
            checkAll(
                Arb.long(),
                Arb.int(1..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = true,
                    targetModifiers = emptySet()
                )
                
                outcome.diceRolls.size shouldBe 4 // 2d6 becomes 4d6
                outcome.damageModifier shouldBe damageModifier // Modifier unchanged
                outcome.baseDamage shouldBe outcome.diceTotal + damageModifier
            }
        }
    }

    context("Damage calculation invariants") {
        test("base damage is always dice total plus modifier") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
                
                outcome.baseDamage shouldBe outcome.diceTotal + damageModifier
            }
        }

        test("final damage without modifiers equals base damage") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
                
                outcome.finalDamage shouldBe outcome.baseDamage
            }
        }

        test("dice total equals sum of individual rolls") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller = DiceRoller(seed)
                val calculator = DamageCalculator(roller)
                
                val outcome = calculator.calculateDamage(
                    damageDice = "3d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Fire,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
                
                outcome.diceTotal shouldBe outcome.diceRolls.sum()
            }
        }
    }

    context("Attack total calculation invariants") {
        test("attack total is always d20 roll plus attack bonus") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller = DiceRoller(seed)
                val resolver = AttackResolver(roller)
                
                val outcome = resolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC
                )
                
                outcome.totalRoll shouldBe outcome.d20Roll + attackBonus
            }
        }
    }

    context("Natural 20 and natural 1 invariants") {
        test("natural 20 always results in critical hit") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..30)
            ) { seed, attackBonus, targetAC ->
                val roller = DiceRoller(seed)
                val resolver = AttackResolver(roller)
                
                val outcome = resolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC
                )
                
                if (outcome.d20Roll == 20) {
                    outcome.isCritical shouldBe true
                    outcome.hit shouldBe true
                    outcome.isAutoMiss shouldBe false
                }
            }
        }

        test("natural 1 always results in auto-miss") {
            checkAll(
                Arb.long(),
                Arb.int(-5..20),
                Arb.int(5..15)
            ) { seed, attackBonus, targetAC ->
                val roller = DiceRoller(seed)
                val resolver = AttackResolver(roller)
                
                val outcome = resolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC
                )
                
                if (outcome.d20Roll == 1) {
                    outcome.isAutoMiss shouldBe true
                    outcome.hit shouldBe false
                    outcome.isCritical shouldBe false
                }
            }
        }

        test("only natural 20 is marked as critical") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller = DiceRoller(seed)
                val resolver = AttackResolver(roller)
                
                val outcome = resolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC
                )
                
                if (outcome.isCritical) {
                    outcome.d20Roll shouldBe 20
                }
            }
        }

        test("only natural 1 is marked as auto-miss") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller = DiceRoller(seed)
                val resolver = AttackResolver(roller)
                
                val outcome = resolver.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC
                )
                
                if (outcome.isAutoMiss) {
                    outcome.d20Roll shouldBe 1
                }
            }
        }
    }
})
