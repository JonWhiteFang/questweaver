package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.core.rules.initiative.models.InitiativeResult
import dev.questweaver.core.rules.initiative.models.RoundState
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.core.rules.initiative.models.TurnState

/**
 * Manages turn order progression and creature lifecycle during combat.
 *
 * Handles initiative order management, turn advancement, creature addition/removal,
 * and delayed turn mechanics. All methods return new immutable RoundState instances.
 *
 * Implements D&D 5e SRD initiative and turn order rules:
 * - Initiative order sorted by total (highest first)
 * - Turns advance sequentially through the order
 * - Round increments when all creatures have acted
 * - Surprise round mechanics for ambushes
 * - Dynamic creature addition/removal mid-combat
 * - Delayed turn support (Ready action)
 */
class InitiativeTracker {
    
    companion object {
        /**
         * Default movement speed for creatures when not specified.
         * Standard humanoid movement speed in D&D 5e is 30 feet.
         */
        private const val DEFAULT_MOVEMENT_SPEED = 30
    }
    
    /**
     * Creates initial turn order from initiative entries.
     *
     * Establishes the starting state for combat:
     * - Round 0 if surprise round, round 1 otherwise
     * - Marks surprise round flag based on surprised creatures
     * - Sets initiative order from sorted entries
     * - Creates initial turn state for first non-surprised creature
     *
     * Validation:
     * - Initiative order must not be empty
     *
     * @param initiativeOrder Sorted list of initiative entries (highest first)
     * @param surprisedCreatures Set of creature IDs that are surprised (default empty)
     * @return Success with initial RoundState, or InvalidState if validation fails
     */
    fun initialize(
        initiativeOrder: List<InitiativeEntry>,
        surprisedCreatures: Set<Long> = emptySet()
    ): InitiativeResult<RoundState> {
        // Validate initiative order is not empty
        if (initiativeOrder.isEmpty()) {
            return InitiativeResult.InvalidState("Initiative order cannot be empty")
        }
        val hasSurpriseRound = surprisedCreatures.isNotEmpty()
        val roundNumber = if (hasSurpriseRound) 0 else 1
        
        // Find first creature that can act (not surprised in surprise round)
        val firstTurnIndex = if (hasSurpriseRound) {
            initiativeOrder.indexOfFirst { it.creatureId !in surprisedCreatures }
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
        
        return InitiativeResult.Success(
            RoundState(
                roundNumber = roundNumber,
                isSurpriseRound = hasSurpriseRound,
                initiativeOrder = initiativeOrder,
                surprisedCreatures = surprisedCreatures,
                delayedCreatures = emptyMap(),
                currentTurn = currentTurn
            )
        )
    }
    
    /**
     * Advances to the next creature in turn order.
     *
     * Progression logic:
     * 1. Increment turn index
     * 2. If index exceeds order length, wrap to 0 and increment round
     * 3. If surprise round ends, clear surprised creatures and set round to 1
     * 4. If creature is surprised in surprise round, skip and recurse
     * 5. Create new TurnState with updated active creature
     *
     * Validation:
     * - Initiative order must not be empty
     * - Turn index must be within bounds
     * - Active creature must exist in order
     *
     * @param currentState Current round and turn state
     * @return Success with updated RoundState, or InvalidState if validation fails
     */
    fun advanceTurn(currentState: RoundState): InitiativeResult<RoundState> {
        val initiativeOrder = currentState.initiativeOrder
        
        // Validate initiative order is not empty
        if (initiativeOrder.isEmpty()) {
            return InitiativeResult.InvalidState("Cannot advance turn with empty initiative order")
        }
        
        // Validate turn index is within bounds
        val currentTurnIndex = currentState.currentTurn?.turnIndex ?: -1
        if (currentTurnIndex < -1 || currentTurnIndex >= initiativeOrder.size) {
            return InitiativeResult.InvalidState("Turn index $currentTurnIndex is out of bounds (0-${initiativeOrder.size - 1})")
        }
        
        // Validate active creature exists in order (if turn is active)
        if (currentState.currentTurn != null) {
            val activeCreatureExists = initiativeOrder.any { 
                it.creatureId == currentState.currentTurn.activeCreatureId 
            }
            if (!activeCreatureExists) {
                return InitiativeResult.InvalidState("Active creature ${currentState.currentTurn.activeCreatureId} not found in initiative order")
            }
        }
        
        var nextIndex = currentTurnIndex + 1
        var newRoundNumber = currentState.roundNumber
        var newIsSurpriseRound = currentState.isSurpriseRound
        var newSurprisedCreatures = currentState.surprisedCreatures
        
        // Check if we've wrapped around to start a new round
        if (nextIndex >= initiativeOrder.size) {
            nextIndex = 0
            newRoundNumber++
            
            // End surprise round if it was active
            if (currentState.isSurpriseRound) {
                newIsSurpriseRound = false
                newSurprisedCreatures = emptySet()
                newRoundNumber = 1 // First normal round after surprise
            }
        }
        
        val nextCreature = initiativeOrder[nextIndex]
        val shouldSkip = newIsSurpriseRound && nextCreature.creatureId in newSurprisedCreatures
        
        // Build result state - either skip to next turn or create new turn state
        return when {
            shouldSkip -> {
                // Skip surprised creatures in surprise round (recursive call)
                val updatedState = currentState.copy(
                    roundNumber = newRoundNumber,
                    isSurpriseRound = newIsSurpriseRound,
                    surprisedCreatures = newSurprisedCreatures,
                    currentTurn = currentState.currentTurn?.copy(turnIndex = nextIndex)
                )
                advanceTurn(updatedState)
            }
            else -> {
                // Create new turn state for next creature
                val newTurnState = TurnState(
                    activeCreatureId = nextCreature.creatureId,
                    turnPhase = TurnPhase(
                        creatureId = nextCreature.creatureId,
                        movementRemaining = DEFAULT_MOVEMENT_SPEED,
                        actionAvailable = true,
                        bonusActionAvailable = true,
                        reactionAvailable = true
                    ),
                    turnIndex = nextIndex
                )
                
                InitiativeResult.Success(
                    currentState.copy(
                        roundNumber = newRoundNumber,
                        isSurpriseRound = newIsSurpriseRound,
                        surprisedCreatures = newSurprisedCreatures,
                        currentTurn = newTurnState
                    )
                )
            }
        }
    }
    
    /**
     * Adds a creature to combat mid-encounter.
     *
     * Inserts the new creature into the initiative order at the correct sorted position.
     * If the insertion point is before the current turn, adjusts the turn index to maintain
     * the correct active creature.
     *
     * Validation:
     * - Initiative order must not be empty
     * - Turn index must be within bounds (if turn is active)
     *
     * @param currentState Current round and turn state
     * @param newEntry Initiative entry for the new creature
     * @return Success with updated RoundState, or InvalidState if validation fails
     */
    fun addCreature(
        currentState: RoundState,
        newEntry: InitiativeEntry
    ): InitiativeResult<RoundState> {
        // Validate initiative order is not empty
        if (currentState.initiativeOrder.isEmpty()) {
            return InitiativeResult.InvalidState("Cannot add creature to empty initiative order")
        }
        
        // Validate turn index is within bounds (if turn is active)
        if (currentState.currentTurn != null) {
            val turnIndex = currentState.currentTurn.turnIndex
            if (turnIndex < 0 || turnIndex >= currentState.initiativeOrder.size) {
                return InitiativeResult.InvalidState("Turn index $turnIndex is out of bounds (0-${currentState.initiativeOrder.size - 1})")
            }
        }
        val newOrder = (currentState.initiativeOrder + newEntry).sorted()
        val newIndex = newOrder.indexOf(newEntry)
        
        // Adjust turn index if insertion is before current turn
        val adjustedTurnIndex = currentState.currentTurn?.let { currentTurn ->
            if (newIndex <= currentTurn.turnIndex) {
                currentTurn.turnIndex + 1
            } else {
                currentTurn.turnIndex
            }
        }
        
        val updatedTurn = currentState.currentTurn?.copy(turnIndex = adjustedTurnIndex ?: 0)
        
        return InitiativeResult.Success(
            currentState.copy(
                initiativeOrder = newOrder,
                currentTurn = updatedTurn
            )
        )
    }
    
    /**
     * Removes a creature from combat (defeated or fled).
     *
     * Removal logic:
     * 1. Find and remove creature from initiative order
     * 2. If creature is before current turn, decrement turn index
     * 3. If removed creature was active, advance to next turn
     *
     * Validation:
     * - Initiative order must not be empty
     * - Turn index must be within bounds (if turn is active)
     *
     * @param currentState Current round and turn state
     * @param creatureId ID of creature to remove
     * @return Success with updated RoundState, or InvalidState if validation fails
     */
    fun removeCreature(
        currentState: RoundState,
        creatureId: Long
    ): InitiativeResult<RoundState> {
        // Validate initiative order is not empty
        if (currentState.initiativeOrder.isEmpty()) {
            return InitiativeResult.InvalidState("Cannot remove creature from empty initiative order")
        }
        
        // Validate turn index is within bounds (if turn is active)
        if (currentState.currentTurn != null) {
            val turnIndex = currentState.currentTurn.turnIndex
            if (turnIndex < 0 || turnIndex >= currentState.initiativeOrder.size) {
                return InitiativeResult.InvalidState("Turn index $turnIndex is out of bounds (0-${currentState.initiativeOrder.size - 1})")
            }
        }
        val creatureIndex = currentState.initiativeOrder.indexOfFirst { it.creatureId == creatureId }
        
        // Creature not found, return error
        if (creatureIndex < 0) {
            return InitiativeResult.InvalidState("Creature $creatureId not found in initiative order")
        }
        
        val newOrder = currentState.initiativeOrder.filterNot { it.creatureId == creatureId }
        val currentTurnIndex = currentState.currentTurn?.turnIndex ?: 0
        val wasActiveCreature = currentState.currentTurn?.activeCreatureId == creatureId
        
        // Handle empty order case
        return when {
            newOrder.isEmpty() -> InitiativeResult.Success(
                currentState.copy(
                    initiativeOrder = newOrder,
                    currentTurn = null
                )
            )
            wasActiveCreature -> {
                // Removed creature was active, advance to next turn
                val stateWithRemovedCreature = currentState.copy(
                    initiativeOrder = newOrder,
                    currentTurn = currentState.currentTurn?.copy(
                        turnIndex = if (creatureIndex < currentTurnIndex) currentTurnIndex - 1 else currentTurnIndex
                    )
                )
                advanceTurn(stateWithRemovedCreature)
            }
            else -> {
                // Adjust turn index if removed creature was before current turn
                val adjustedTurnIndex = if (creatureIndex < currentTurnIndex) {
                    currentTurnIndex - 1
                } else {
                    currentTurnIndex
                }
                
                InitiativeResult.Success(
                    currentState.copy(
                        initiativeOrder = newOrder,
                        currentTurn = currentState.currentTurn?.copy(turnIndex = adjustedTurnIndex)
                    )
                )
            }
        }
    }
    
    /**
     * Delays a creature's turn to later in the initiative order.
     *
     * Implements the Ready action mechanic:
     * 1. Remove creature from current position in initiative order
     * 2. Add creature to delayedCreatures map with original InitiativeEntry
     * 3. Advance turn to next creature
     *
     * Validation:
     * - Initiative order must not be empty
     * - Creature must exist in initiative order
     * - Turn index must be within bounds (if turn is active)
     *
     * @param currentState Current round and turn state
     * @param creatureId ID of creature delaying
     * @return Success with updated RoundState, or InvalidState if validation fails
     */
    fun delayTurn(
        currentState: RoundState,
        creatureId: Long
    ): InitiativeResult<RoundState> {
        // Validate initiative order is not empty
        if (currentState.initiativeOrder.isEmpty()) {
            return InitiativeResult.InvalidState("Cannot delay turn with empty initiative order")
        }
        
        // Validate turn index is within bounds (if turn is active)
        if (currentState.currentTurn != null) {
            val turnIndex = currentState.currentTurn.turnIndex
            if (turnIndex < 0 || turnIndex >= currentState.initiativeOrder.size) {
                return InitiativeResult.InvalidState("Turn index $turnIndex is out of bounds (0-${currentState.initiativeOrder.size - 1})")
            }
        }
        val creatureEntry = currentState.initiativeOrder.find { it.creatureId == creatureId }
        
        // Creature not found, return error
        if (creatureEntry == null) {
            return InitiativeResult.InvalidState("Creature $creatureId not found in initiative order")
        }
        
        val newOrder = currentState.initiativeOrder.filterNot { it.creatureId == creatureId }
        val newDelayedCreatures = currentState.delayedCreatures + (creatureId to creatureEntry)
        
        val stateWithDelay = currentState.copy(
            initiativeOrder = newOrder,
            delayedCreatures = newDelayedCreatures
        )
        
        // If the delayed creature was active, advance to next turn; otherwise return state with delay
        val wasActiveCreature = currentState.currentTurn?.activeCreatureId == creatureId
        return if (wasActiveCreature) {
            advanceTurn(stateWithDelay)
        } else {
            InitiativeResult.Success(stateWithDelay)
        }
    }
    
    /**
     * Inserts a delayed creature at the current initiative position.
     *
     * When a delayed creature chooses to act:
     * 1. Remove creature from delayedCreatures map
     * 2. Create new InitiativeEntry with new initiative score
     * 3. Insert creature at current position in initiative order
     *
     * The creature maintains this new initiative position for the remainder of the encounter.
     *
     * Validation:
     * - Creature must exist in delayed creatures map
     * - Turn index must be within bounds (if turn is active)
     *
     * @param currentState Current round and turn state
     * @param creatureId ID of delayed creature
     * @param newInitiative New initiative score for the creature
     * @return Success with updated RoundState, or InvalidState if validation fails
     */
    fun resumeDelayedTurn(
        currentState: RoundState,
        creatureId: Long,
        newInitiative: Int
    ): InitiativeResult<RoundState> {
        // Validate creature exists in delayed creatures map
        val delayedEntry = currentState.delayedCreatures[creatureId]
            ?: return InitiativeResult.InvalidState("Creature $creatureId not found in delayed creatures")
        
        // Validate turn index is within bounds (if turn is active)
        if (currentState.currentTurn != null) {
            val turnIndex = currentState.currentTurn.turnIndex
            if (turnIndex < 0 || turnIndex >= currentState.initiativeOrder.size) {
                return InitiativeResult.InvalidState("Turn index $turnIndex is out of bounds (0-${currentState.initiativeOrder.size - 1})")
            }
        }
        
        // Create new entry with updated initiative
        val newEntry = delayedEntry.copy(
            total = newInitiative,
            roll = newInitiative - delayedEntry.modifier
        )
        
        // Remove from delayed creatures
        val newDelayedCreatures = currentState.delayedCreatures - creatureId
        
        // Insert at current position (after current turn index)
        val currentTurnIndex = currentState.currentTurn?.turnIndex ?: 0
        val insertPosition = currentTurnIndex + 1
        
        val newOrder = currentState.initiativeOrder.toMutableList().apply {
            add(insertPosition.coerceIn(0, size), newEntry)
        }
        
        return InitiativeResult.Success(
            currentState.copy(
                initiativeOrder = newOrder,
                delayedCreatures = newDelayedCreatures
            )
        )
    }
}
