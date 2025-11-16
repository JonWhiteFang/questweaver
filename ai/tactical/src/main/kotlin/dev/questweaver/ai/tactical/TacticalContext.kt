package dev.questweaver.ai.tactical

import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.values.Condition
import dev.questweaver.domain.values.GridPos

/**
 * Represents the current tactical situation during an encounter.
 * Contains all information needed for AI decision-making.
 *
 * @property encounterId Unique identifier for the encounter
 * @property round Current round number
 * @property creatures All creatures in the encounter
 * @property allies Creatures allied with the decision-maker
 * @property enemies Creatures hostile to the decision-maker
 * @property creaturePositions Map of creature IDs to their grid positions
 * @property activeConditions Map of creature IDs to their active conditions
 * @property concentrationSpells Map of creature IDs to spell names they're concentrating on
 * @property recentDamage Map of creature IDs to damage dealt in last 2 rounds
 * @property seed Seed for deterministic randomness
 */
data class TacticalContext(
    val encounterId: Long,
    val round: Int,
    val creatures: List<Creature>,
    val allies: List<Creature>,
    val enemies: List<Creature>,
    val creaturePositions: Map<Long, GridPos>,
    val activeConditions: Map<Long, List<Condition>>,
    val concentrationSpells: Map<Long, String>,
    val recentDamage: Map<Long, Int>,
    val seed: Long
) {
    /**
     * Returns all allies of the given creature.
     *
     * @param creature The creature to find allies for
     * @return List of allied creatures
     */
    fun getAllies(creature: Creature): List<Creature> {
        return if (allies.any { it.id == creature.id }) {
            allies.filter { it.id != creature.id }
        } else {
            enemies.filter { it.id != creature.id }
        }
    }
    
    /**
     * Returns all enemies of the given creature.
     *
     * @param creature The creature to find enemies for
     * @return List of enemy creatures
     */
    fun getEnemies(creature: Creature): List<Creature> {
        return if (allies.any { it.id == creature.id }) {
            enemies
        } else {
            allies
        }
    }
    
    /**
     * Checks if two creatures are allies.
     *
     * @param creature1 First creature
     * @param creature2 Second creature
     * @return True if creatures are allies
     */
    fun isAlly(creature1: Creature, creature2: Creature): Boolean {
        val creature1IsAlly = allies.any { it.id == creature1.id }
        val creature2IsAlly = allies.any { it.id == creature2.id }
        return creature1IsAlly == creature2IsAlly
    }
    
    /**
     * Gets the position of a creature.
     *
     * @param creatureId The creature's ID
     * @return The creature's position, or null if not found
     */
    fun getPosition(creatureId: Long): GridPos? {
        return creaturePositions[creatureId]
    }
    
    /**
     * Gets a creature by ID.
     *
     * @param creatureId The creature's ID
     * @return The creature, or null if not found
     */
    fun getCreature(creatureId: Long): Creature? {
        return creatures.firstOrNull { it.id == creatureId }
    }
}
