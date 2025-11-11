# Implementation Plan

- [ ] 1. Set up module structure and core data models
  - Create `core/rules` module with `build.gradle.kts` configured as pure Kotlin library
  - Add module to `settings.gradle.kts`
  - Create package structure: `combat/`, `conditions/`, `modifiers/`, `outcomes/`
  - Add dependency on `core:domain` and `04-dice-system`
  - _Requirements: 6.5_

- [ ] 2. Implement sealed types and enums for combat mechanics
- [ ] 2.1 Create RollModifier sealed interface
  - Define `Normal`, `Advantage`, `Disadvantage` objects
  - _Requirements: 1.4, 1.5_

- [ ] 2.2 Create ProficiencyLevel enum
  - Define `None`, `Proficient`, `Expertise` values
  - _Requirements: 4.4, 4.5_

- [ ] 2.3 Create DamageType enum
  - Define all SRD damage types (Slashing, Piercing, Bludgeoning, Fire, Cold, etc.)
  - _Requirements: 2.1_

- [ ] 2.4 Create DamageModifier sealed interface
  - Define `Resistance`, `Vulnerability`, `Immunity` data classes with damageType parameter
  - _Requirements: 2.3, 2.4, 2.5_

- [ ] 2.5 Create Condition sealed interface
  - Define condition objects: `Prone`, `Stunned`, `Poisoned`, `Blinded`, `Restrained`, `Incapacitated`, `Paralyzed`, `Unconscious`
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 2.6 Create AbilityType enum and SaveEffect sealed interface
  - Define ability types (Strength, Dexterity, Constitution, Intelligence, Wisdom, Charisma)
  - Define save effects: `AutoFail`, `Disadvantage`, `Normal`
  - _Requirements: 3.1, 3.5_

- [ ] 3. Implement outcome data classes
- [ ] 3.1 Create AttackOutcome data class
  - Include fields: d20Roll, attackBonus, totalRoll, targetAC, hit, isCritical, isAutoMiss, rollModifier, appliedConditions
  - _Requirements: 7.1_

- [ ] 3.2 Create DamageOutcome data class
  - Include fields: diceRolls, diceTotal, damageModifier, baseDamage, damageType, isCritical, appliedModifiers, finalDamage
  - _Requirements: 7.4_

- [ ] 3.3 Create SavingThrowOutcome data class
  - Include fields: d20Roll, abilityModifier, proficiencyBonus, totalRoll, dc, success, isAutoSuccess, rollModifier, appliedConditions
  - _Requirements: 7.2_

- [ ] 3.4 Create AbilityCheckOutcome data class
  - Include fields: d20Roll, abilityModifier, proficiencyBonus, totalRoll, dc, success, rollModifier, proficiencyLevel, appliedConditions
  - _Requirements: 7.3_

- [ ] 4. Implement ConditionRegistry
- [ ] 4.1 Create ConditionRegistry object with condition effect lookup methods
  - Implement `getAttackRollEffect(condition, isAttacker)` returning RollModifier
  - Implement `getSavingThrowEffect(condition, abilityType)` returning SaveEffect
  - Implement `getAbilityCheckEffect(condition)` returning RollModifier
  - Implement `preventsActions(condition)` returning Boolean
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 7.5_

- [ ] 4.2 Implement condition effect logic for each condition
  - Prone: disadvantage on attacks, advantage for melee attacks against
  - Stunned: auto-fail STR/DEX saves, prevents actions
  - Poisoned: disadvantage on attacks and ability checks
  - Blinded: disadvantage on attacks, advantage for attacks against
  - Restrained: disadvantage on attacks and DEX saves, advantage for attacks against
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 5. Implement AttackResolver
- [ ] 5.1 Create AttackResolver class with DiceRoller dependency
  - Define `resolveAttack()` method with parameters: attackBonus, targetAC, rollModifier, attackerConditions, targetConditions
  - _Requirements: 1.1, 6.1_

- [ ] 5.2 Implement attack roll logic with advantage/disadvantage
  - Roll d20 (or 2d20 for advantage/disadvantage) using DiceRoller
  - Apply advantage: take higher of two rolls
  - Apply disadvantage: take lower of two rolls
  - _Requirements: 1.1, 1.4, 1.5_

- [ ] 5.3 Implement critical hit and auto-miss detection
  - Check for natural 20 (critical hit, always hits)
  - Check for natural 1 (auto-miss, always misses)
  - _Requirements: 1.2, 1.3_

- [ ] 5.4 Implement condition effects on attack rolls
  - Query ConditionRegistry for attacker and target conditions
  - Combine roll modifiers (multiple sources of advantage/disadvantage)
  - _Requirements: 5.1, 5.3, 5.4, 7.5_

- [ ] 5.5 Calculate final attack result and return AttackOutcome
  - Calculate total: d20 + attackBonus
  - Determine hit: total >= targetAC (or critical hit)
  - Return AttackOutcome with all details
  - _Requirements: 1.1, 7.1_

- [ ] 6. Implement DamageCalculator
- [ ] 6.1 Create DamageCalculator class with DiceRoller dependency
  - Define `calculateDamage()` method with parameters: damageDice, damageModifier, damageType, isCritical, targetModifiers
  - _Requirements: 2.1, 6.1_

- [ ] 6.2 Implement damage dice parsing and rolling
  - Parse damage dice expression (e.g., "2d6", "1d8+3")
  - Roll damage dice using DiceRoller
  - Add flat damage modifier
  - _Requirements: 2.1_

- [ ] 6.3 Implement critical hit damage doubling
  - If critical, double the number of dice rolled (not the modifier)
  - _Requirements: 2.2_

- [ ] 6.4 Implement damage modifiers (resistance/vulnerability/immunity)
  - Apply resistance: halve damage (rounded down)
  - Apply vulnerability: double damage
  - Apply immunity: reduce damage to zero
  - Handle multiple modifiers in correct order
  - _Requirements: 2.3, 2.4, 2.5_

- [ ] 6.5 Return DamageOutcome with breakdown
  - Include individual die rolls, totals, modifiers applied, and final damage
  - _Requirements: 7.4_

- [ ] 7. Implement SavingThrowResolver
- [ ] 7.1 Create SavingThrowResolver class with DiceRoller dependency
  - Define `resolveSavingThrow()` method with parameters: abilityModifier, proficiencyBonus, dc, rollModifier, isProficient, conditions
  - _Requirements: 3.1, 6.1_

- [ ] 7.2 Implement saving throw roll logic with advantage/disadvantage
  - Roll d20 (or 2d20 for advantage/disadvantage) using DiceRoller
  - Apply advantage/disadvantage logic
  - _Requirements: 3.1, 3.2, 3.3_

- [ ] 7.3 Implement proficiency and natural 20 handling
  - Add proficiency bonus if isProficient is true
  - Check for natural 20 (automatic success)
  - _Requirements: 3.4, 3.5_

- [ ] 7.4 Implement condition effects on saving throws
  - Query ConditionRegistry for condition effects
  - Handle auto-fail for Stunned on STR/DEX saves
  - Apply disadvantage from conditions
  - _Requirements: 5.2, 7.5_

- [ ] 7.5 Calculate final result and return SavingThrowOutcome
  - Calculate total: d20 + abilityModifier + (proficiencyBonus if proficient)
  - Determine success: total >= dc (or natural 20)
  - Return SavingThrowOutcome with all details
  - _Requirements: 3.1, 7.2_

- [ ] 8. Implement AbilityCheckResolver
- [ ] 8.1 Create AbilityCheckResolver class with DiceRoller dependency
  - Define `resolveAbilityCheck()` method with parameters: abilityModifier, proficiencyBonus, dc, rollModifier, proficiencyLevel, conditions
  - _Requirements: 4.1, 6.1_

- [ ] 8.2 Implement ability check roll logic with advantage/disadvantage
  - Roll d20 (or 2d20 for advantage/disadvantage) using DiceRoller
  - Apply advantage/disadvantage logic
  - _Requirements: 4.1, 4.2, 4.3_

- [ ] 8.3 Implement proficiency and expertise handling
  - Calculate proficiency multiplier: 0x for None, 1x for Proficient, 2x for Expertise
  - Add (proficiencyBonus × multiplier) to roll
  - _Requirements: 4.4, 4.5_

- [ ] 8.4 Implement condition effects on ability checks
  - Query ConditionRegistry for condition effects
  - Apply disadvantage from Poisoned condition
  - _Requirements: 5.3, 7.5_

- [ ] 8.5 Calculate final result and return AbilityCheckOutcome
  - Calculate total: d20 + abilityModifier + (proficiencyBonus × multiplier)
  - Determine success: total >= dc
  - Return AbilityCheckOutcome with all details
  - _Requirements: 4.1, 7.3_

- [ ] 9. Add Koin dependency injection module
  - Create `RulesModule.kt` in `di/` package
  - Define factory bindings for AttackResolver, DamageCalculator, SavingThrowResolver, AbilityCheckResolver
  - Each resolver should receive DiceRoller as dependency
  - _Requirements: 6.1_

- [ ] 10. Write comprehensive unit tests
- [ ] 10.1 Write AttackResolver tests
  - Test normal attacks hit/miss based on AC
  - Test natural 20 always hits (critical)
  - Test natural 1 always misses
  - Test advantage takes higher of two rolls
  - Test disadvantage takes lower of two rolls
  - Test condition effects apply correctly
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 10.2 Write DamageCalculator tests
  - Test basic damage calculation (dice + modifier)
  - Test critical hits double dice (not modifier)
  - Test resistance halves damage (rounded down)
  - Test vulnerability doubles damage
  - Test immunity reduces damage to zero
  - Test multiple modifiers apply correctly
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 10.3 Write SavingThrowResolver tests
  - Test success/failure based on DC
  - Test natural 20 always succeeds
  - Test proficiency adds bonus
  - Test advantage/disadvantage work correctly
  - Test Stunned auto-fails STR/DEX saves
  - Test condition effects apply correctly
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 10.4 Write AbilityCheckResolver tests
  - Test success/failure based on DC
  - Test proficiency adds 1x bonus
  - Test expertise adds 2x bonus
  - Test advantage/disadvantage work correctly
  - Test Poisoned applies disadvantage
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 10.5 Write ConditionRegistry tests
  - Test each condition applies correct modifiers
  - Test multiple conditions stack appropriately
  - Test all condition effects are covered
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 10.6 Write property-based determinism tests
  - Test same seed produces same attack outcomes
  - Test same seed produces same damage outcomes
  - Test same seed produces same saving throw outcomes
  - Test same seed produces same ability check outcomes
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 10.7 Write property-based invariant tests
  - Test damage with resistance is always half or less
  - Test damage with vulnerability is always double or more
  - Test damage with immunity is always zero
  - Test d20 rolls are always in range 1-20
  - Test advantage always >= normal roll
  - Test disadvantage always <= normal roll
  - _Requirements: 2.3, 2.4, 2.5_
