package dev.questweaver.core.rules.di

import dev.questweaver.core.rules.initiative.InitiativeRoller
import dev.questweaver.core.rules.initiative.InitiativeStateBuilder
import dev.questweaver.core.rules.initiative.InitiativeTracker
import dev.questweaver.core.rules.initiative.SurpriseHandler
import dev.questweaver.core.rules.initiative.TurnPhaseManager
import dev.questweaver.domain.dice.DiceRoller
import org.koin.dsl.module

/**
 * Koin dependency injection module for initiative and turn management components.
 *
 * Provides factory bindings for all initiative system components:
 * - InitiativeRoller: Rolls initiative for creatures (requires DiceRoller)
 * - InitiativeTracker: Manages turn order and progression
 * - TurnPhaseManager: Tracks action economy within turns
 * - SurpriseHandler: Handles surprise round mechanics
 * - InitiativeStateBuilder: Rebuilds state from events for event sourcing
 *
 * All components are stateless and use factory scope for new instances per injection.
 * InitiativeRoller requires a seeded DiceRoller for deterministic initiative rolls.
 *
 * ## Usage
 *
 * ### In Application Setup
 * ```kotlin
 * startKoin {
 *     modules(
 *         domainModule,
 *         initiativeModule,
 *         // other modules
 *     )
 * }
 * ```
 *
 * ### In ViewModels or Use Cases
 * ```kotlin
 * class EncounterViewModel(
 *     private val diceRoller: DiceRoller,
 *     private val initiativeRoller: InitiativeRoller,
 *     private val initiativeTracker: InitiativeTracker
 * ) : ViewModel() {
 *     // Use injected dependencies
 * }
 * ```
 *
 * ### With Parameterized DiceRoller
 * ```kotlin
 * class StartEncounter(
 *     private val sessionSeed: Long
 * ) {
 *     private val diceRoller: DiceRoller by inject { parametersOf(sessionSeed) }
 *     private val initiativeRoller: InitiativeRoller = InitiativeRoller(diceRoller)
 * }
 * ```
 */
val initiativeModule = module {
    /**
     * InitiativeRoller factory binding.
     *
     * Requires a DiceRoller dependency for deterministic initiative rolls.
     * Use factory scope since each encounter may need a new roller with
     * a different seed for deterministic replay.
     */
    factory { InitiativeRoller(get()) }
    
    /**
     * InitiativeTracker factory binding.
     *
     * Stateless component for managing turn order and progression.
     * Use factory scope for new instances per injection.
     */
    factory { InitiativeTracker() }
    
    /**
     * TurnPhaseManager factory binding.
     *
     * Stateless component for tracking action economy within turns.
     * Use factory scope for new instances per injection.
     */
    factory { TurnPhaseManager() }
    
    /**
     * SurpriseHandler factory binding.
     *
     * Stateless component for handling surprise round mechanics.
     * Use factory scope for new instances per injection.
     */
    factory { SurpriseHandler() }
    
    /**
     * InitiativeStateBuilder factory binding.
     *
     * Stateless component for rebuilding initiative state from events.
     * Use factory scope for new instances per injection.
     */
    factory { InitiativeStateBuilder() }
}
