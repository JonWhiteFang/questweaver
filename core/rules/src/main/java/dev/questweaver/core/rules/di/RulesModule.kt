package dev.questweaver.core.rules.di

import dev.questweaver.core.rules.RulesEngine
import dev.questweaver.rules.combat.AbilityCheckResolver
import dev.questweaver.rules.combat.AttackResolver
import dev.questweaver.rules.combat.DamageCalculator
import dev.questweaver.rules.combat.SavingThrowResolver
import org.koin.dsl.module

/**
 * Koin dependency injection module for the rules engine.
 *
 * Provides:
 * - RulesEngine (singleton)
 * - Combat resolvers (factories that accept DiceRoller)
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
}
