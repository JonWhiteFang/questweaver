package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.core.rules.initiative.models.RoundState
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.core.rules.initiative.models.TurnState
import dev.questweaver.domain.events.CreatureAddedToCombat
import dev.questweaver.domain.events.CreatureRemovedFromCombat
import dev.questweaver.domain.events.DelayedTurnResumed
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.ReactionUsed
import dev.questweaver.domain.events.RoundStarted
import dev.questweaver.domain.events.TurnDelayed
import dev.questweaver.domain.events.TurnEnded
import dev.questweaver.domain.events.TurnStarted

/**
 * Rebuilds initiative state from event sequence for event sourcing.
 *
 * Implements event sourcing by deriving all initiative state from a sequence of
 * immutable GameEvent instances. This enables:
 * - Full combat replay from event log
 * - Deterministic state reconstruction
 * - Time-travel debugging
 * - Audit trail of all initiative changes
 *
 * The builder processes events in order, applying each event's state changes
 * to produce the final RoundState. All state is derived, never mutated directly.
 */
class InitiativeStateBuilder {
    
    companion object {
        /**
         * Default movement speed for creatures when not specified.
         * Standard humanoid movement speed in D&D 5e is 30 feet.
         */
        private const val DEFAULT_MOVEMENT_SPEED = 30
    }
    
    /**
     * Rebuilds initiative state from event sequence.
     *
     * Processes events in order to derive the current RoundState:
     * 1. Starts with empty initial state
     * 2. Applies each event's state changes sequentially
     * 3. Returns final derived state
     *
     * Handles all initiative-related events exhaustively:
     * - EncounterStarted: Establishes initial initiative order
     * - RoundStarted: Increments round number
     * - TurnStarted: Updates current turn
     * - TurnEnded: Clears current turn
     * - ReactionUsed: Updates turn phase
     * - TurnDelayed: Moves creature to delayed map
     * - DelayedTurnResumed: Inserts creature back into order
     * - CreatureAddedToCombat: Inserts creature into order
     * - CreatureRemovedFromCombat: Removes creature from order
     *
     * Non-initiative events (e.g., AttackResolved, DamageApplied) are ignored
     * and return the state unchanged.
     *
     * @param events Sequence of initiative-related events
     * @return Current RoundState derived from events
     */
    fun buildState(events: List<GameEvent>): RoundState {
        var state = RoundState(
            roundNumber = 0,
            isSurpriseRound = false,
            initiativeOrder = emptyList(),
            surprisedCreatures = emptySet(),
            delayedCreatures = emptyMap(),
            currentTurn = null
        )
        
        events.forEach { event ->
            state = when (event) {
                is EncounterStarted -> handleEncounterStarted(event)
                is RoundStarted -> handleRoundStarted(state, event)
                is TurnStarted -> handleTurnStarted(state, event)
                is TurnEnded -> handleTurnEnded(state)
                is ReactionUsed -> handleReactionUsed(state, event)
                is TurnDelayed -> handleTurnDelayed(state, event)
                is DelayedTurnResumed -> handleDelayedTurnResumed(state, event)
                is CreatureAddedToCombat -> handleCreatureAdded(state, event)
                is CreatureRemovedFromCombat -> handleCreatureRemoved(state, event)
                else -> state // Non-initiative events don't affect state
            }
        }
        
        return state
    }
    
    /**
     * Handles EncounterStarted event to set initial state.
     *
     * Establishes the starting state for combat:
     * - Converts InitiativeEntryData to InitiativeEntry
     * - Sets round number (0 for surprise round, 1 otherwise)
     * - Marks surprise round flag based on surprised creatures
     * - Creates initial turn state for first non-surprised creature
     *
     * @param event EncounterStarted event with initiative order
     * @return Updated RoundState with initial combat state
     */
    private fun handleEncounterStarted(event: EncounterStarted): RoundState {
        val initiativeOrder = event.initiativeOrder.map { data ->
            InitiativeEntry(
                creatureId = data.creatureId,
                roll = data.roll,
                modifier = data.modifier,
                total = data.total
            )
        }.sorted()
        
        val hasSurpriseRound = event.surprisedCreatures.isNotEmpty()
        val roundNumber = if (hasSurpriseRound) 0 else 1
        
        // Find first creature that can act (not surprised in surprise round)
        val firstTurnIndex = if (hasSurpriseRound) {
            initiativeOrder.indexOfFirst { it.creatureId !in event.surprisedCreatures }
        } else {
            0
        }
        
        val currentTurn = if (firstTurnIndex >= 0 && initiativeOrder.isNotEmpty()) {
            val firstCreature = initiativeOrder[firstTurnIndex]
            TurnState(
                activeCreatureId = firstCreature.creatureId,
                turnPhase = TurnPhase(
                    creatureId = firstCreature.creatureId,
                    movementRemaining = DEFAULT_MOVEMENT_SPEED,
                    actionAvailable = true,
                    bonusActionAvailable = true,
                    reactionAvailable = true
                ),
                turnIndex = firstTurnIndex
            )
        } else {
            null
        }
        
        return RoundState(
            roundNumber = roundNumber,
            isSurpriseRound = hasSurpriseRound,
            initiativeOrder = initiativeOrder,
            surprisedCreatures = event.surprisedCreatures,
            delayedCreatures = emptyMap(),
            currentTurn = currentTurn
        )
    }
    
    /**
     * Handles RoundStarted event to increment round number.
     *
     * Updates the round counter when a new round begins.
     * If transitioning from surprise round (round 0) to normal rounds,
     * also clears the surprise round flag and surprised creatures.
     *
     * @param state Current state
     * @param event RoundStarted event with new round number
     * @return Updated RoundState with incremented round
     */
    private fun handleRoundStarted(state: RoundState, event: RoundStarted): RoundState {
        val isEndingSurpriseRound = state.isSurpriseRound && event.roundNumber == 1
        
        return state.copy(
            roundNumber = event.roundNumber,
            isSurpriseRound = if (isEndingSurpriseRound) false else state.isSurpriseRound,
            surprisedCreatures = if (isEndingSurpriseRound) emptySet() else state.surprisedCreatures
        )
    }
    
    /**
     * Handles TurnStarted event to update current turn.
     *
     * Sets the active creature and creates a new TurnPhase with all actions available.
     * Finds the creature's position in the initiative order to set the turn index.
     *
     * @param state Current state
     * @param event TurnStarted event with creature ID and turn info
     * @return Updated RoundState with new active turn
     */
    private fun handleTurnStarted(state: RoundState, event: TurnStarted): RoundState {
        val turnIndex = state.initiativeOrder.indexOfFirst { it.creatureId == event.creatureId }
        
        // If creature not found in order, return unchanged state
        if (turnIndex < 0) {
            return state
        }
        
        val turnState = TurnState(
            activeCreatureId = event.creatureId,
            turnPhase = TurnPhase(
                creatureId = event.creatureId,
                movementRemaining = DEFAULT_MOVEMENT_SPEED,
                actionAvailable = true,
                bonusActionAvailable = true,
                reactionAvailable = true
            ),
            turnIndex = turnIndex
        )
        
        return state.copy(currentTurn = turnState)
    }
    
    /**
     * Handles TurnEnded event to clear current turn.
     *
     * Clears the active turn state when a creature's turn ends.
     * The next TurnStarted event will establish the new active creature.
     *
     * @param state Current state
     * @return Updated RoundState with cleared turn
     */
    private fun handleTurnEnded(state: RoundState): RoundState {
        return state.copy(currentTurn = null)
    }
    
    /**
     * Handles ReactionUsed event to update turn phase.
     *
     * Marks the reaction as consumed in the current turn phase.
     * If no current turn exists or reaction doesn't belong to active creature,
     * returns state unchanged.
     *
     * @param state Current state
     * @param event ReactionUsed event
     * @return Updated RoundState with reaction consumed
     */
    private fun handleReactionUsed(state: RoundState, event: ReactionUsed): RoundState {
        val currentTurn = state.currentTurn
        
        return when {
            currentTurn == null -> state
            currentTurn.activeCreatureId != event.creatureId -> state
            else -> {
                val updatedPhase = currentTurn.turnPhase.copy(reactionAvailable = false)
                val updatedTurn = currentTurn.copy(turnPhase = updatedPhase)
                state.copy(currentTurn = updatedTurn)
            }
        }
    }
    
    /**
     * Handles TurnDelayed event to move creature to delayed map.
     *
     * Removes the creature from the initiative order and adds it to the
     * delayedCreatures map with its original initiative entry.
     *
     * @param state Current state
     * @param event TurnDelayed event with creature ID
     * @return Updated RoundState with creature delayed
     */
    private fun handleTurnDelayed(state: RoundState, event: TurnDelayed): RoundState {
        val creatureEntry = state.initiativeOrder.find { it.creatureId == event.creatureId }
            ?: return state
        
        val newOrder = state.initiativeOrder.filterNot { it.creatureId == event.creatureId }
        val newDelayedCreatures = state.delayedCreatures + (event.creatureId to creatureEntry)
        
        return state.copy(
            initiativeOrder = newOrder,
            delayedCreatures = newDelayedCreatures,
            currentTurn = null // Clear turn since creature delayed
        )
    }
    
    /**
     * Handles DelayedTurnResumed event to insert creature back into order.
     *
     * Removes the creature from the delayedCreatures map and inserts it
     * back into the initiative order with the new initiative score.
     *
     * @param state Current state
     * @param event DelayedTurnResumed event with new initiative
     * @return Updated RoundState with creature resumed
     */
    private fun handleDelayedTurnResumed(state: RoundState, event: DelayedTurnResumed): RoundState {
        val delayedEntry = state.delayedCreatures[event.creatureId]
            ?: return state
        
        // Create new entry with updated initiative
        val newEntry = delayedEntry.copy(
            total = event.newInitiative,
            roll = event.newInitiative - delayedEntry.modifier
        )
        
        // Remove from delayed creatures
        val newDelayedCreatures = state.delayedCreatures - event.creatureId
        
        // Insert into sorted order
        val newOrder = (state.initiativeOrder + newEntry).sorted()
        
        return state.copy(
            initiativeOrder = newOrder,
            delayedCreatures = newDelayedCreatures
        )
    }
    
    /**
     * Handles CreatureAddedToCombat event to insert creature into order.
     *
     * Adds a new creature to the initiative order mid-combat.
     * The creature is inserted at the correct sorted position.
     *
     * @param state Current state
     * @param event CreatureAddedToCombat event with initiative entry
     * @return Updated RoundState with creature added
     */
    private fun handleCreatureAdded(state: RoundState, event: CreatureAddedToCombat): RoundState {
        val newEntry = InitiativeEntry(
            creatureId = event.initiativeEntry.creatureId,
            roll = event.initiativeEntry.roll,
            modifier = event.initiativeEntry.modifier,
            total = event.initiativeEntry.total
        )
        
        val newOrder = (state.initiativeOrder + newEntry).sorted()
        
        // Adjust turn index if insertion is before current turn
        val updatedTurn = state.currentTurn?.let { currentTurn ->
            val newIndex = newOrder.indexOf(newEntry)
            if (newIndex <= currentTurn.turnIndex) {
                currentTurn.copy(turnIndex = currentTurn.turnIndex + 1)
            } else {
                currentTurn
            }
        }
        
        return state.copy(
            initiativeOrder = newOrder,
            currentTurn = updatedTurn
        )
    }
    
    /**
     * Handles CreatureRemovedFromCombat event to remove creature from order.
     *
     * Removes a creature from the initiative order (defeated or fled).
     * Adjusts turn index if the removed creature was before the current turn.
     *
     * @param state Current state
     * @param event CreatureRemovedFromCombat event with creature ID
     * @return Updated RoundState with creature removed
     */
    private fun handleCreatureRemoved(state: RoundState, event: CreatureRemovedFromCombat): RoundState {
        val creatureIndex = state.initiativeOrder.indexOfFirst { it.creatureId == event.creatureId }
        
        // Creature not found, return unchanged state
        if (creatureIndex < 0) {
            return state
        }
        
        val newOrder = state.initiativeOrder.filterNot { it.creatureId == event.creatureId }
        val currentTurnIndex = state.currentTurn?.turnIndex ?: 0
        val wasActiveCreature = state.currentTurn?.activeCreatureId == event.creatureId
        
        return when {
            newOrder.isEmpty() -> state.copy(
                initiativeOrder = newOrder,
                currentTurn = null
            )
            wasActiveCreature -> state.copy(
                initiativeOrder = newOrder,
                currentTurn = null // Clear turn, next TurnStarted will set new active
            )
            else -> {
                // Adjust turn index if removed creature was before current turn
                val adjustedTurnIndex = if (creatureIndex < currentTurnIndex) {
                    currentTurnIndex - 1
                } else {
                    currentTurnIndex
                }
                
                state.copy(
                    initiativeOrder = newOrder,
                    currentTurn = state.currentTurn?.copy(turnIndex = adjustedTurnIndex)
                )
            }
        }
    }
}
