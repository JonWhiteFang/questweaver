package dev.questweaver.core.rules.di

import dev.questweaver.core.rules.actions.ActionProcessor
import dev.questweaver.core.rules.actions.AttackActionHandler
import dev.questweaver.core.rules.actions.AttackResolver
import dev.questweaver.core.rules.actions.DamageCalculator
import dev.questweaver.core.rules.actions.MovementActionHandler
import dev.questweaver.core.rules.actions.Pathfinder
import dev.questweaver.core.rules.actions.ReactionHandler
import dev.questweaver.core.rules.actions.SavingThrowResolver
import dev.questweaver.core.rules.actions.SpecialActionHandler
import dev.questweaver.core.rules.actions.SpellActionHandler
import dev.questweaver.core.rules.actions.validation.ActionValidationSystem
import dev.questweaver.core.rules.actions.validation.ActionValidator
import org.koin.dsl.module

/**
 * Koin dependency injection module for combat action processing components.
 *
 * Provides factory bindings for all action system components:
 * - ActionValidator: Pre-execution validation using Action Validation System
 * - AttackActionHandler: Processes attack actions with Combat Rules Engine
 * - MovementActionHandler: Processes movement with pathfinding and opportunity attacks
 * - SpellActionHandler: Processes spell casting with slot consumption and effects
 * - SpecialActionHandler: Processes special actions (Dodge, Disengage, Help, Ready)
 * - ReactionHandler: Processes reactions triggered by combat events
 * - ActionProcessor: Main coordinator that routes actions to appropriate handlers
 *
 * All components are stateless and use factory scope for new instances per injection.
 * Handlers depend on placeholder interfaces (AttackResolver, DamageCalculator, etc.)
 * that will be implemented as part of the 05-combat-rules and 06-action-validation specs.
 *
 * ## Usage
 *
 * ### In Application Setup
 * ```kotlin
 * startKoin {
 *     modules(
 *         domainModule,
 *         rulesModule,
 *         actionsModule,
 *         // other modules
 *     )
 * }
 * ```
 *
 * ### In ViewModels or Use Cases
 * ```kotlin
 * class EncounterViewModel(
 *     private val actionProcessor: ActionProcessor
 * ) : ViewModel() {
 *     suspend fun processPlayerAction(action: CombatAction, context: ActionContext) {
 *         val result = actionProcessor.processAction(action, context)
 *         when (result) {
 *             is ActionResult.Success -> applyEvents(result.events)
 *             is ActionResult.Failure -> showError(result.reason)
 *             is ActionResult.RequiresChoice -> promptUser(result.options)
 *         }
 *     }
 * }
 * ```
 *
 * ### Processing Reactions
 * ```kotlin
 * class TurnEngine(
 *     private val reactionHandler: ReactionHandler
 * ) {
 *     suspend fun handleMovement(move: Move, context: ActionContext) {
 *         // Identify creatures that can react
 *         val reactors = reactionHandler.identifyReactors(
 *             ReactionTrigger.CreatureMoved(move.actorId, fromPos, toPos),
 *             context
 *         )
 *         
 *         // Process opportunity attacks
 *         for (reactorId in reactors) {
 *             val reaction = Reaction(reactorId, ReactionType.OpportunityAttack, move.actorId)
 *             val events = reactionHandler.handleReaction(reaction, trigger, context)
 *             applyEvents(events)
 *         }
 *     }
 * }
 * ```
 *
 * ## Dependencies
 *
 * This module requires placeholder implementations for:
 * - ActionValidationSystem (from 06-action-validation spec)
 * - AttackResolver (from 05-combat-rules spec)
 * - DamageCalculator (from 05-combat-rules spec)
 * - SavingThrowResolver (from 05-combat-rules spec)
 * - Pathfinder (from feature:map module)
 *
 * These dependencies must be provided by other modules or mock implementations
 * must be used for testing until the actual implementations are available.
 */
val actionsModule = module {
    /**
     * ActionValidator factory binding.
     *
     * Requires ActionValidationSystem dependency for pre-execution validation.
     * Use factory scope since validation is stateless.
     */
    factory { ActionValidator(get()) }
    
    /**
     * AttackActionHandler factory binding.
     *
     * Requires AttackResolver and DamageCalculator dependencies from Combat Rules Engine.
     * Use factory scope for new instances per injection.
     */
    factory { AttackActionHandler(get(), get()) }
    
    /**
     * MovementActionHandler factory binding.
     *
     * Requires Pathfinder for path validation and ReactionHandler for opportunity attacks.
     * Use factory scope for new instances per injection.
     */
    factory { MovementActionHandler(get(), get()) }
    
    /**
     * SpellActionHandler factory binding.
     *
     * Requires AttackResolver, SavingThrowResolver, and DamageCalculator for spell effects.
     * Use factory scope for new instances per injection.
     */
    factory { SpellActionHandler(get(), get(), get()) }
    
    /**
     * SpecialActionHandler factory binding.
     *
     * Stateless component for processing special actions (Dodge, Disengage, Help, Ready).
     * Use factory scope for new instances per injection.
     */
    factory { SpecialActionHandler() }
    
    /**
     * ReactionHandler factory binding.
     *
     * Requires AttackActionHandler dependency for processing opportunity attacks.
     * Use factory scope for new instances per injection.
     */
    factory { ReactionHandler(get()) }
    
    /**
     * ActionProcessor factory binding.
     *
     * Main coordinator that requires all handler dependencies for routing actions.
     * Use factory scope for new instances per injection.
     */
    factory {
        ActionProcessor(
            attackHandler = get(),
            movementHandler = get(),
            spellHandler = get(),
            specialHandler = get(),
            validator = get()
        )
    }
}
