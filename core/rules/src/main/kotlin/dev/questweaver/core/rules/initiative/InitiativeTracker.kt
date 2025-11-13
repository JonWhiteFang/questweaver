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
@Suppress("TooManyFunctions")
class InitiativeTracker {
    
    companion object {
        /**
         * Default movement speed for creatures when not specified.
         * Standard humanoid movement speed in D&D 5e is 30 feet.
         */
        private const val DEFAULT_MOVEMENT_SPEED = 30
        
        private const val ERROR_EMPTY_ORDER = "Initiative order cannot be empty"
        private const val ERROR_CANNOT_ADVANCE = "Cannot advance turn with empty initiative order"
        private const val ERROR_CANNOT_ADD = "Cannot add creature to empty initiative order"
        private const val ERROR_CANNOT_REMOVE = "Cannot remove creature from empty initiative order"
        private const val ERROR_CANNOT_DELAY = "Cannot delay turn with empty initiative order"
        
        private fun outOfBoundsError(index: Int, max: Int): String =
            "Turn index $index is out of bounds (0-$max)"
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
            return InitiativeResult.InvalidState(ERROR_EMPTY_ORDER)
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
        val validationResult = validateAdvanceTurn(currentState)
        if (validationResult != null) return validationResult
        
        val nextTurnData = calculateNextTurn(currentState)
        val nextCreature = currentState.initiativeOrder[nextTurnData.index]
        val shouldSkip = nextTurnData.isSurpriseRound && 
            nextCreature.creatureId in nextTurnData.surprisedCreatures
        
        return if (shouldSkip) {
            advanceTurnSkippingSurprised(currentState, nextTurnData)
        } else {
            createNextTurnState(currentState, nextTurnData, nextCreature)
        }
    }
    
    private fun validateAdvanceTurn(state: RoundState): InitiativeResult.InvalidState? =
        when {
            state.initiativeOrder.isEmpty() -> 
                InitiativeResult.InvalidState(ERROR_CANNOT_ADVANCE)
            else -> validateTurnIndex(state, allowNegativeOne = true) 
                ?: validateActiveCreatureExists(state)
        }
    
    private fun validateActiveCreatureExists(
        state: RoundState
    ): InitiativeResult.InvalidState? {
        if (state.currentTurn == null) return null
        
        val activeExists = state.initiativeOrder.any { 
            it.creatureId == state.currentTurn.activeCreatureId 
        }
        
        return if (!activeExists) {
            val creatureId = state.currentTurn.activeCreatureId
            InitiativeResult.InvalidState(
                "Active creature $creatureId not found in initiative order"
            )
        } else {
            null
        }
    }
    
    private fun validateTurnIndex(
        state: RoundState,
        allowNegativeOne: Boolean = false
    ): InitiativeResult.InvalidState? {
        val currentTurn = state.currentTurn ?: return null
        val turnIndex = currentTurn.turnIndex
        val maxIndex = state.initiativeOrder.size - 1
        val minIndex = if (allowNegativeOne) -1 else 0
        
        return if (turnIndex < minIndex || turnIndex > maxIndex) {
            InitiativeResult.InvalidState(outOfBoundsError(turnIndex, maxIndex))
        } else {
            null
        }
    }
    
    private data class NextTurnData(
        val index: Int,
        val roundNumber: Int,
        val isSurpriseRound: Boolean,
        val surprisedCreatures: Set<Long>
    )
    
    private fun calculateNextTurn(state: RoundState): NextTurnData {
        val currentIndex = state.currentTurn?.turnIndex ?: -1
        var nextIndex = currentIndex + 1
        var newRound = state.roundNumber
        var newIsSurprise = state.isSurpriseRound
        var newSurprised = state.surprisedCreatures
        
        if (nextIndex >= state.initiativeOrder.size) {
            nextIndex = 0
            newRound++
            
            if (state.isSurpriseRound) {
                newIsSurprise = false
                newSurprised = emptySet()
                newRound = 1
            }
        }
        
        return NextTurnData(nextIndex, newRound, newIsSurprise, newSurprised)
    }
    
    private fun advanceTurnSkippingSurprised(
        state: RoundState,
        turnData: NextTurnData
    ): InitiativeResult<RoundState> {
        val updatedState = state.copy(
            roundNumber = turnData.roundNumber,
            isSurpriseRound = turnData.isSurpriseRound,
            surprisedCreatures = turnData.surprisedCreatures,
            currentTurn = state.currentTurn?.copy(turnIndex = turnData.index)
        )
        return advanceTurn(updatedState)
    }
    
    private fun createNextTurnState(
        state: RoundState,
        turnData: NextTurnData,
        nextCreature: InitiativeEntry
    ): InitiativeResult<RoundState> {
        val newTurnState = TurnState(
            activeCreatureId = nextCreature.creatureId,
            turnPhase = TurnPhase(
                creatureId = nextCreature.creatureId,
                movementRemaining = DEFAULT_MOVEMENT_SPEED,
                actionAvailable = true,
                bonusActionAvailable = true,
                reactionAvailable = true
            ),
            turnIndex = turnData.index
        )
        
        return InitiativeResult.Success(
            state.copy(
                roundNumber = turnData.roundNumber,
                isSurpriseRound = turnData.isSurpriseRound,
                surprisedCreatures = turnData.surprisedCreatures,
                currentTurn = newTurnState
            )
        )
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
        val validationResult = validateAddCreature(currentState)
        if (validationResult != null) return validationResult
        
        val newOrder = (currentState.initiativeOrder + newEntry).sorted()
        val newIndex = newOrder.indexOf(newEntry)
        
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
    
    private fun validateAddCreature(state: RoundState): InitiativeResult.InvalidState? {
        if (state.initiativeOrder.isEmpty()) {
            return InitiativeResult.InvalidState(ERROR_CANNOT_ADD)
        }
        
        return validateTurnIndex(state)
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
        val validationResult = validateRemoveCreature(currentState, creatureId)
        if (validationResult != null) return validationResult
        
        val creatureIndex = currentState.initiativeOrder.indexOfFirst { 
            it.creatureId == creatureId 
        }
        val newOrder = currentState.initiativeOrder.filterNot { it.creatureId == creatureId }
        val currentTurnIndex = currentState.currentTurn?.turnIndex ?: 0
        val wasActiveCreature = currentState.currentTurn?.activeCreatureId == creatureId
        
        return handleCreatureRemoval(
            currentState,
            newOrder,
            creatureIndex,
            currentTurnIndex,
            wasActiveCreature
        )
    }
    
    private fun validateRemoveCreature(
        state: RoundState,
        creatureId: Long
    ): InitiativeResult.InvalidState? =
        when {
            state.initiativeOrder.isEmpty() -> 
                InitiativeResult.InvalidState(ERROR_CANNOT_REMOVE)
            else -> validateTurnIndex(state) ?: validateCreatureExists(state, creatureId)
        }
    
    private fun validateCreatureExists(
        state: RoundState,
        creatureId: Long
    ): InitiativeResult.InvalidState? {
        val exists = state.initiativeOrder.any { it.creatureId == creatureId }
        return if (!exists) {
            InitiativeResult.InvalidState(
                "Creature $creatureId not found in initiative order"
            )
        } else {
            null
        }
    }
    
    private fun handleCreatureRemoval(
        state: RoundState,
        newOrder: List<InitiativeEntry>,
        creatureIndex: Int,
        currentTurnIndex: Int,
        wasActiveCreature: Boolean
    ): InitiativeResult<RoundState> {
        return when {
            newOrder.isEmpty() -> InitiativeResult.Success(
                state.copy(initiativeOrder = newOrder, currentTurn = null)
            )
            wasActiveCreature -> {
                val adjustedIndex = if (creatureIndex < currentTurnIndex) {
                    currentTurnIndex - 1
                } else {
                    currentTurnIndex
                }
                val stateWithRemoved = state.copy(
                    initiativeOrder = newOrder,
                    currentTurn = state.currentTurn?.copy(turnIndex = adjustedIndex)
                )
                advanceTurn(stateWithRemoved)
            }
            else -> {
                val adjustedIndex = if (creatureIndex < currentTurnIndex) {
                    currentTurnIndex - 1
                } else {
                    currentTurnIndex
                }
                InitiativeResult.Success(
                    state.copy(
                        initiativeOrder = newOrder,
                        currentTurn = state.currentTurn?.copy(turnIndex = adjustedIndex)
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
        val validationResult = validateDelayTurn(currentState, creatureId)
        if (validationResult != null) return validationResult
        
        val creatureEntry = currentState.initiativeOrder.find { it.creatureId == creatureId }!!
        val newOrder = currentState.initiativeOrder.filterNot { it.creatureId == creatureId }
        val newDelayedCreatures = currentState.delayedCreatures + (creatureId to creatureEntry)
        
        val stateWithDelay = currentState.copy(
            initiativeOrder = newOrder,
            delayedCreatures = newDelayedCreatures
        )
        
        val wasActiveCreature = currentState.currentTurn?.activeCreatureId == creatureId
        return if (wasActiveCreature) {
            advanceTurn(stateWithDelay)
        } else {
            InitiativeResult.Success(stateWithDelay)
        }
    }
    
    private fun validateDelayTurn(
        state: RoundState,
        creatureId: Long
    ): InitiativeResult.InvalidState? =
        when {
            state.initiativeOrder.isEmpty() -> 
                InitiativeResult.InvalidState(ERROR_CANNOT_DELAY)
            else -> validateTurnIndex(state) ?: validateCreatureExists(state, creatureId)
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
        val validationResult = validateResumeDelayedTurn(currentState, creatureId)
        if (validationResult != null) return validationResult
        
        val delayedEntry = currentState.delayedCreatures[creatureId]!!
        val newEntry = delayedEntry.copy(
            total = newInitiative,
            roll = newInitiative - delayedEntry.modifier
        )
        
        val newDelayedCreatures = currentState.delayedCreatures - creatureId
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
    
    private fun validateResumeDelayedTurn(
        state: RoundState,
        creatureId: Long
    ): InitiativeResult.InvalidState? {
        if (creatureId !in state.delayedCreatures) {
            return InitiativeResult.InvalidState(
                "Creature $creatureId not found in delayed creatures"
            )
        }
        
        return validateTurnIndex(state)
    }
}
