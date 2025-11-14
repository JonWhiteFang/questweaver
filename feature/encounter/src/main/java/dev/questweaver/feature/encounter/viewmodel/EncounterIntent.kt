package dev.questweaver.feature.encounter.viewmodel

import dev.questweaver.feature.map.ui.GridPos
import dev.questweaver.feature.map.ui.MapState

/**
 * Sealed interface representing all user intents in the encounter.
 * Follows MVI pattern for unidirectional data flow.
 */
sealed interface EncounterIntent {
    
    // ========== Encounter Lifecycle ==========
    
    /**
     * Start a new encounter with the given creatures and context.
     */
    data class StartEncounter(
        val creatures: List<Creature>,
        val surprisedCreatures: Set<Long>,
        val mapGrid: MapGrid
    ) : EncounterIntent
    
    /**
     * End the current creature's turn and advance to the next.
     */
    data object EndTurn : EncounterIntent
    
    // ========== Combat Actions ==========
    
    /**
     * Attack a target creature.
     */
    data class Attack(
        val targetId: Long,
        val weaponId: Long? = null
    ) : EncounterIntent
    
    /**
     * Move to a position on the map.
     */
    data class MoveTo(
        val path: List<GridPos>
    ) : EncounterIntent
    
    /**
     * Cast a spell.
     */
    data class CastSpell(
        val spellId: Long,
        val targets: List<Long>,
        val spellLevel: Int
    ) : EncounterIntent
    
    /**
     * Take the Dodge action.
     */
    data object Dodge : EncounterIntent
    
    /**
     * Take the Disengage action.
     */
    data object Disengage : EncounterIntent
    
    /**
     * Help another creature.
     */
    data class Help(
        val targetId: Long,
        val helpType: HelpType
    ) : EncounterIntent
    
    /**
     * Ready an action with a trigger.
     */
    data class Ready(
        val action: CombatAction,
        val trigger: String
    ) : EncounterIntent
    
    // ========== Undo/Redo ==========
    
    /**
     * Undo the last action.
     */
    data object Undo : EncounterIntent
    
    /**
     * Redo the last undone action.
     */
    data object Redo : EncounterIntent
    
    // ========== Action Choice Resolution ==========
    
    /**
     * Resolve a pending choice by selecting an option.
     */
    data class ResolveChoice(
        val selectedOption: ActionOption
    ) : EncounterIntent
}

/**
 * Type of help being provided.
 */
enum class HelpType {
    AttackRoll,
    AbilityCheck,
    SavingThrow
}

/**
 * Represents a combat action that can be taken.
 * Placeholder for actual combat action implementation.
 */
data class CombatAction(
    val type: String,
    val targetId: Long? = null
)

/**
 * Represents a creature in the encounter.
 * Placeholder for actual creature entity from core:domain.
 */
data class Creature(
    val id: Long,
    val name: String,
    val hpCurrent: Int,
    val hpMax: Int,
    val ac: Int,
    val position: GridPos,
    val isPlayerControlled: Boolean
)

/**
 * Represents the map grid configuration.
 * Placeholder for actual map grid from feature:map.
 */
data class MapGrid(
    val width: Int,
    val height: Int,
    val blockedPositions: Set<GridPos> = emptySet(),
    val difficultTerrain: Set<GridPos> = emptySet()
)
