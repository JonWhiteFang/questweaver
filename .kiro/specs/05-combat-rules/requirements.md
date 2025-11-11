# Requirements Document

## Introduction

The Combat Rules Engine is a deterministic, pure Kotlin module that implements D&D 5e SRD-compatible combat mechanics. It provides attack resolution, saving throws, ability checks, and condition effects without any Android dependencies or AI integration. The engine must be 100% deterministic using seeded random number generation to ensure reproducible outcomes for event sourcing and replay capabilities.

## Glossary

- **Combat Rules Engine**: The system responsible for validating and resolving combat-related actions according to D&D 5e SRD rules
- **Attack Roll**: A d20 roll plus modifiers to determine if an attack hits a target
- **Armor Class (AC)**: The target number an attack roll must meet or exceed to hit
- **Saving Throw**: A d20 roll plus modifiers to resist or reduce an effect
- **Difficulty Class (DC)**: The target number a saving throw or ability check must meet or exceed to succeed
- **Ability Check**: A d20 roll plus ability modifier to determine success at a task
- **Condition**: A status effect that modifies a creature's capabilities (e.g., prone, stunned, poisoned)
- **Advantage/Disadvantage**: Rolling two d20s and taking the higher/lower result
- **Critical Hit**: Rolling a natural 20 on an attack roll, doubling damage dice
- **Critical Miss**: Rolling a natural 1 on an attack roll, automatically missing
- **Proficiency Bonus**: A bonus added to rolls for skills, saves, or attacks the creature is proficient in
- **Damage Type**: The category of damage (slashing, piercing, bludgeoning, fire, etc.)
- **Resistance**: Taking half damage from a specific damage type
- **Vulnerability**: Taking double damage from a specific damage type
- **Immunity**: Taking no damage from a specific damage type

## Requirements

### Requirement 1

**User Story:** As a game developer, I want the Combat Rules Engine to resolve attack rolls against targets, so that combat outcomes are determined according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN an attack is made with an attack bonus and target AC, THE Combat Rules Engine SHALL calculate the d20 roll plus attack bonus and determine if the result meets or exceeds the target AC
2. WHEN a natural 20 is rolled on an attack roll, THE Combat Rules Engine SHALL treat the attack as a critical hit regardless of the target AC
3. WHEN a natural 1 is rolled on an attack roll, THE Combat Rules Engine SHALL treat the attack as an automatic miss regardless of the attack bonus
4. WHEN an attack roll is made with advantage, THE Combat Rules Engine SHALL roll two d20s and use the higher result for the attack calculation
5. WHEN an attack roll is made with disadvantage, THE Combat Rules Engine SHALL roll two d20s and use the lower result for the attack calculation

### Requirement 2

**User Story:** As a game developer, I want the Combat Rules Engine to calculate damage for successful attacks, so that hit point changes are accurate and support various damage types and modifiers.

#### Acceptance Criteria

1. WHEN an attack hits, THE Combat Rules Engine SHALL roll the specified damage dice and add the damage modifier to determine total damage
2. WHEN a critical hit occurs, THE Combat Rules Engine SHALL double all damage dice (but not modifiers) before calculating total damage
3. WHEN a target has resistance to the damage type, THE Combat Rules Engine SHALL reduce the final damage by half (rounded down)
4. WHEN a target has vulnerability to the damage type, THE Combat Rules Engine SHALL double the final damage
5. WHEN a target has immunity to the damage type, THE Combat Rules Engine SHALL reduce the damage to zero

### Requirement 3

**User Story:** As a game developer, I want the Combat Rules Engine to resolve saving throws, so that creatures can resist or reduce effects according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a saving throw is required with a DC and ability modifier, THE Combat Rules Engine SHALL calculate the d20 roll plus ability modifier and determine if the result meets or exceeds the DC
2. WHEN a saving throw is made with advantage, THE Combat Rules Engine SHALL roll two d20s and use the higher result for the calculation
3. WHEN a saving throw is made with disadvantage, THE Combat Rules Engine SHALL roll two d20s and use the lower result for the calculation
4. WHEN a creature is proficient in the saving throw ability, THE Combat Rules Engine SHALL add the proficiency bonus to the saving throw calculation
5. WHEN a natural 20 is rolled on a saving throw, THE Combat Rules Engine SHALL treat the save as a success regardless of modifiers

### Requirement 4

**User Story:** As a game developer, I want the Combat Rules Engine to resolve ability checks, so that non-combat actions can be adjudicated according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN an ability check is required with a DC and ability modifier, THE Combat Rules Engine SHALL calculate the d20 roll plus ability modifier and determine if the result meets or exceeds the DC
2. WHEN an ability check is made with advantage, THE Combat Rules Engine SHALL roll two d20s and use the higher result for the calculation
3. WHEN an ability check is made with disadvantage, THE Combat Rules Engine SHALL roll two d20s and use the lower result for the calculation
4. WHEN a creature is proficient in the skill being checked, THE Combat Rules Engine SHALL add the proficiency bonus to the ability check calculation
5. WHEN a creature has expertise in the skill being checked, THE Combat Rules Engine SHALL add double the proficiency bonus to the ability check calculation

### Requirement 5

**User Story:** As a game developer, I want the Combat Rules Engine to manage condition effects, so that status effects modify creature capabilities according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature has the Prone condition, THE Combat Rules Engine SHALL apply disadvantage to the creature's attack rolls and grant advantage to melee attacks against the creature
2. WHEN a creature has the Stunned condition, THE Combat Rules Engine SHALL prevent the creature from taking actions or reactions and automatically fail Strength and Dexterity saving throws
3. WHEN a creature has the Poisoned condition, THE Combat Rules Engine SHALL apply disadvantage to the creature's attack rolls and ability checks
4. WHEN a creature has the Blinded condition, THE Combat Rules Engine SHALL apply disadvantage to the creature's attack rolls and grant advantage to attacks against the creature
5. WHEN a creature has the Restrained condition, THE Combat Rules Engine SHALL apply disadvantage to the creature's attack rolls and Dexterity saving throws, grant advantage to attacks against the creature, and reduce the creature's speed to zero

### Requirement 6

**User Story:** As a game developer, I want the Combat Rules Engine to be deterministic, so that the same inputs always produce the same outputs for event sourcing and replay.

#### Acceptance Criteria

1. THE Combat Rules Engine SHALL use only seeded random number generation from the DiceRoller component
2. THE Combat Rules Engine SHALL produce identical results when given the same seed, inputs, and sequence of operations
3. THE Combat Rules Engine SHALL NOT use any unseeded random number generation
4. THE Combat Rules Engine SHALL NOT depend on system time, network state, or other non-deterministic inputs
5. THE Combat Rules Engine SHALL be implemented as pure Kotlin with no Android dependencies

### Requirement 7

**User Story:** As a game developer, I want the Combat Rules Engine to provide detailed resolution outcomes, so that the UI can display what happened and events can be properly logged.

#### Acceptance Criteria

1. WHEN an attack is resolved, THE Combat Rules Engine SHALL return an outcome containing the attack roll, whether it hit, damage dealt, and whether it was a critical hit
2. WHEN a saving throw is resolved, THE Combat Rules Engine SHALL return an outcome containing the roll result, whether it succeeded, and any modifiers applied
3. WHEN an ability check is resolved, THE Combat Rules Engine SHALL return an outcome containing the roll result, whether it succeeded, and any modifiers applied
4. WHEN damage is calculated, THE Combat Rules Engine SHALL return an outcome containing the damage roll, modifiers, damage type, and final damage after resistances
5. WHEN conditions affect a roll, THE Combat Rules Engine SHALL return an outcome indicating which conditions were applied and how they modified the result
