package dev.questweaver.core.rules.di

import dev.questweaver.core.rules.RulesEngine
import dev.questweaver.core.rules.validation.ActionValidator
import dev.questweaver.core.rules.validation.validators.ActionEconomyValidator
import dev.questweaver.core.rules.validation.validators.ConditionValidator
import dev.questweaver.core.rules.validation.validators.ConcentrationValidator
import dev.questweaver.core.rules.validation.validators.RangeValidator
import dev.questweaver.core.rules.validation.validators.ResourceValidator
import dev.questweaver.rules.combat.AbilityCheckResolver
import dev.questweaver.rules.combat.AttackResolver
import dev.questweaver.rules.combat.DamageCalculator
import dev.questweaver.rules.combat.SavingThrowResolver
import dev.questweaver.rules.conditions.ConditionRegistry
import org.koin.dsl.module

/**
 * Koin dependency injection module for the rules engine.
 *
 * Provides:
 * - RulesEngine (singleton)
 * - Combat resolvers (factories that accept DiceRoller)
 * - Action validators (factories)
 *
 * Note: DiceRoller is not provided by DI because it requires a seed parameter
 * that varies per session/encounter. Resolvers should be created with a
 * session-specific DiceRoller instance.
 */
val rulesModule = module {
    // Core rules engine
    single { RulesEngine() }
    
    // Combat resolvers - factory bindings that accept DiceRoller
    // These are created on-demand with a provided DiceRoller instance
    factory { (diceRoller: dev.questweaver.domain.dice.DiceRoller) ->
        AttackResolver(diceRoller)
    }
    
    factory { (diceRoller: dev.questweaver.domain.dice.DiceRoller) ->
        DamageCalculator(diceRoller)
    }
    
    factory { (diceRoller: dev.questweaver.domain.dice.DiceRoller) ->
        SavingThrowResolver(diceRoller)
    }
    
    factory { (diceRoller: dev.questweaver.domain.dice.DiceRoller) ->
        AbilityCheckResolver(diceRoller)
    }
    
    // Action validation - validators are stateless and can be singletons
    single { ActionEconomyValidator() }
    single { ResourceValidator() }
    single { RangeValidator() }
    single { ConcentrationValidator() }
    single { ConditionValidator(ConditionRegistry) }
    
    // Main action validator orchestrator
    factory {
        ActionValidator(
            actionEconomyValidator = get(),
            resourceValidator = get(),
            rangeValidator = get(),
            concentrationValidator = get(),
            conditionValidator = get()
        )
    }
}
