package dev.questweaver.rules

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.rules.combat.AbilityCheckResolver
import dev.questweaver.rules.combat.AttackResolver
import dev.questweaver.rules.combat.DamageCalculator
import dev.questweaver.rules.combat.SavingThrowResolver
import dev.questweaver.rules.modifiers.AbilityType
import dev.questweaver.rules.modifiers.DamageType
import dev.questweaver.rules.modifiers.ProficiencyLevel
import dev.questweaver.rules.modifiers.RollModifier
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

/**
 * Property-based tests for deterministic behavior of combat rules.
 *
 * These tests verify that the same seed produces the same outcomes across
 * all resolvers, which is critical for event sourcing and replay capabilities.
 */
class DeterminismPropertyTest : FunSpec({

    context("Attack resolution determinism") {
        test("same seed produces same attack outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller1 = DiceRoller(seed)
                val resolver1 = AttackResolver(roller1)
                val outcome1 = resolver1.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC
                )
                
                val roller2 = DiceRoller(seed)
                val resolver2 = AttackResolver(roller2)
                val outcome2 = resolver2.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC
                )
                
                outcome1.d20Roll shouldBe outcome2.d20Roll
                outcome1.totalRoll shouldBe outcome2.totalRoll
                outcome1.hit shouldBe outcome2.hit
                outcome1.isCritical shouldBe outcome2.isCritical
                outcome1.isAutoMiss shouldBe outcome2.isAutoMiss
            }
        }

        test("same seed with advantage produces same outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller1 = DiceRoller(seed)
                val resolver1 = AttackResolver(roller1)
                val outcome1 = resolver1.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Advantage
                )
                
                val roller2 = DiceRoller(seed)
                val resolver2 = AttackResolver(roller2)
                val outcome2 = resolver2.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Advantage
                )
                
                outcome1.d20Roll shouldBe outcome2.d20Roll
                outcome1.hit shouldBe outcome2.hit
            }
        }

        test("same seed with disadvantage produces same outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(10..20)
            ) { seed, attackBonus, targetAC ->
                val roller1 = DiceRoller(seed)
                val resolver1 = AttackResolver(roller1)
                val outcome1 = resolver1.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Disadvantage
                )
                
                val roller2 = DiceRoller(seed)
                val resolver2 = AttackResolver(roller2)
                val outcome2 = resolver2.resolveAttack(
                    attackBonus = attackBonus,
                    targetAC = targetAC,
                    rollModifier = RollModifier.Disadvantage
                )
                
                outcome1.d20Roll shouldBe outcome2.d20Roll
                outcome1.hit shouldBe outcome2.hit
            }
        }
    }

    context("Damage calculation determinism") {
        test("same seed produces same damage outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller1 = DiceRoller(seed)
                val calculator1 = DamageCalculator(roller1)
                val outcome1 = calculator1.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
                
                val roller2 = DiceRoller(seed)
                val calculator2 = DamageCalculator(roller2)
                val outcome2 = calculator2.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = false,
                    targetModifiers = emptySet()
                )
                
                outcome1.diceRolls shouldBe outcome2.diceRolls
                outcome1.diceTotal shouldBe outcome2.diceTotal
                outcome1.baseDamage shouldBe outcome2.baseDamage
                outcome1.finalDamage shouldBe outcome2.finalDamage
            }
        }

        test("same seed produces same critical damage outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(0..10)
            ) { seed, damageModifier ->
                val roller1 = DiceRoller(seed)
                val calculator1 = DamageCalculator(roller1)
                val outcome1 = calculator1.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = true,
                    targetModifiers = emptySet()
                )
                
                val roller2 = DiceRoller(seed)
                val calculator2 = DamageCalculator(roller2)
                val outcome2 = calculator2.calculateDamage(
                    damageDice = "2d6",
                    damageModifier = damageModifier,
                    damageType = DamageType.Slashing,
                    isCritical = true,
                    targetModifiers = emptySet()
                )
                
                outcome1.diceRolls shouldBe outcome2.diceRolls
                outcome1.finalDamage shouldBe outcome2.finalDamage
            }
        }

        test("same seed produces same outcomes with different dice types") {
            checkAll(Arb.long()) { seed ->
                val diceExpressions = listOf("1d4", "1d6", "1d8", "1d10", "1d12", "2d6", "3d4")
                
                diceExpressions.forEach { dice ->
                    val roller1 = DiceRoller(seed)
                    val calculator1 = DamageCalculator(roller1)
                    val outcome1 = calculator1.calculateDamage(
                        damageDice = dice,
                        damageModifier = 0,
                        damageType = DamageType.Fire,
                        isCritical = false,
                        targetModifiers = emptySet()
                    )
                    
                    val roller2 = DiceRoller(seed)
                    val calculator2 = DamageCalculator(roller2)
                    val outcome2 = calculator2.calculateDamage(
                        damageDice = dice,
                        damageModifier = 0,
                        damageType = DamageType.Fire,
                        isCritical = false,
                        targetModifiers = emptySet()
                    )
                    
                    outcome1.diceRolls shouldBe outcome2.diceRolls
                }
            }
        }
    }

    context("Saving throw determinism") {
        test("same seed produces same saving throw outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(0..6),
                Arb.int(5..20)
            ) { seed, abilityModifier, proficiencyBonus, dc ->
                val roller1 = DiceRoller(seed)
                val resolver1 = SavingThrowResolver(roller1)
                val outcome1 = resolver1.resolveSavingThrow(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    isProficient = true,
                    abilityType = AbilityType.Dexterity
                )
                
                val roller2 = DiceRoller(seed)
                val resolver2 = SavingThrowResolver(roller2)
                val outcome2 = resolver2.resolveSavingThrow(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    isProficient = true,
                    abilityType = AbilityType.Dexterity
                )
                
                outcome1.d20Roll shouldBe outcome2.d20Roll
                outcome1.totalRoll shouldBe outcome2.totalRoll
                outcome1.success shouldBe outcome2.success
                outcome1.isAutoSuccess shouldBe outcome2.isAutoSuccess
            }
        }

        test("same seed with advantage produces same outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(0..6),
                Arb.int(5..20)
            ) { seed, abilityModifier, proficiencyBonus, dc ->
                val roller1 = DiceRoller(seed)
                val resolver1 = SavingThrowResolver(roller1)
                val outcome1 = resolver1.resolveSavingThrow(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    rollModifier = RollModifier.Advantage,
                    isProficient = true,
                    abilityType = AbilityType.Wisdom
                )
                
                val roller2 = DiceRoller(seed)
                val resolver2 = SavingThrowResolver(roller2)
                val outcome2 = resolver2.resolveSavingThrow(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    rollModifier = RollModifier.Advantage,
                    isProficient = true,
                    abilityType = AbilityType.Wisdom
                )
                
                outcome1.d20Roll shouldBe outcome2.d20Roll
                outcome1.success shouldBe outcome2.success
            }
        }
    }

    context("Ability check determinism") {
        test("same seed produces same ability check outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(0..6),
                Arb.int(5..20)
            ) { seed, abilityModifier, proficiencyBonus, dc ->
                val roller1 = DiceRoller(seed)
                val resolver1 = AbilityCheckResolver(roller1)
                val outcome1 = resolver1.resolveAbilityCheck(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    proficiencyLevel = ProficiencyLevel.Proficient
                )
                
                val roller2 = DiceRoller(seed)
                val resolver2 = AbilityCheckResolver(roller2)
                val outcome2 = resolver2.resolveAbilityCheck(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    proficiencyLevel = ProficiencyLevel.Proficient
                )
                
                outcome1.d20Roll shouldBe outcome2.d20Roll
                outcome1.totalRoll shouldBe outcome2.totalRoll
                outcome1.success shouldBe outcome2.success
                outcome1.proficiencyBonus shouldBe outcome2.proficiencyBonus
            }
        }

        test("same seed with expertise produces same outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(0..6),
                Arb.int(5..20)
            ) { seed, abilityModifier, proficiencyBonus, dc ->
                val roller1 = DiceRoller(seed)
                val resolver1 = AbilityCheckResolver(roller1)
                val outcome1 = resolver1.resolveAbilityCheck(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    proficiencyLevel = ProficiencyLevel.Expertise
                )
                
                val roller2 = DiceRoller(seed)
                val resolver2 = AbilityCheckResolver(roller2)
                val outcome2 = resolver2.resolveAbilityCheck(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    proficiencyLevel = ProficiencyLevel.Expertise
                )
                
                outcome1.d20Roll shouldBe outcome2.d20Roll
                outcome1.proficiencyBonus shouldBe outcome2.proficiencyBonus
                outcome1.success shouldBe outcome2.success
            }
        }

        test("same seed with advantage produces same outcomes") {
            checkAll(
                Arb.long(),
                Arb.int(-5..10),
                Arb.int(0..6),
                Arb.int(5..20)
            ) { seed, abilityModifier, proficiencyBonus, dc ->
                val roller1 = DiceRoller(seed)
                val resolver1 = AbilityCheckResolver(roller1)
                val outcome1 = resolver1.resolveAbilityCheck(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    rollModifier = RollModifier.Advantage,
                    proficiencyLevel = ProficiencyLevel.None
                )
                
                val roller2 = DiceRoller(seed)
                val resolver2 = AbilityCheckResolver(roller2)
                val outcome2 = resolver2.resolveAbilityCheck(
                    abilityModifier = abilityModifier,
                    proficiencyBonus = proficiencyBonus,
                    dc = dc,
                    rollModifier = RollModifier.Advantage,
                    proficiencyLevel = ProficiencyLevel.None
                )
                
                outcome1.d20Roll shouldBe outcome2.d20Roll
                outcome1.success shouldBe outcome2.success
            }
        }
    }

    context("Cross-resolver determinism") {
        test("same seed produces consistent sequence across different resolvers") {
            checkAll(Arb.long()) { seed ->
                // First sequence: attack, damage, save, check
                val roller1 = DiceRoller(seed)
                val attackResolver1 = AttackResolver(roller1)
                val damageCalculator1 = DamageCalculator(roller1)
                val saveResolver1 = SavingThrowResolver(roller1)
                val checkResolver1 = AbilityCheckResolver(roller1)
                
                val attack1 = attackResolver1.resolveAttack(5, 15)
                val damage1 = damageCalculator1.calculateDamage(
                    "2d6", 3, DamageType.Slashing, false, emptySet()
                )
                val save1 = saveResolver1.resolveSavingThrow(
                    2, 2, 15, RollModifier.Normal, false, AbilityType.Dexterity
                )
                val check1 = checkResolver1.resolveAbilityCheck(
                    3, 2, 15, RollModifier.Normal, ProficiencyLevel.Proficient
                )
                
                // Second sequence: same operations with same seed
                val roller2 = DiceRoller(seed)
                val attackResolver2 = AttackResolver(roller2)
                val damageCalculator2 = DamageCalculator(roller2)
                val saveResolver2 = SavingThrowResolver(roller2)
                val checkResolver2 = AbilityCheckResolver(roller2)
                
                val attack2 = attackResolver2.resolveAttack(5, 15)
                val damage2 = damageCalculator2.calculateDamage(
                    "2d6", 3, DamageType.Slashing, false, emptySet()
                )
                val save2 = saveResolver2.resolveSavingThrow(
                    2, 2, 15, RollModifier.Normal, false, AbilityType.Dexterity
                )
                val check2 = checkResolver2.resolveAbilityCheck(
                    3, 2, 15, RollModifier.Normal, ProficiencyLevel.Proficient
                )
                
                // All outcomes should match
                attack1.d20Roll shouldBe attack2.d20Roll
                damage1.diceRolls shouldBe damage2.diceRolls
                save1.d20Roll shouldBe save2.d20Roll
                check1.d20Roll shouldBe check2.d20Roll
            }
        }
    }
})
