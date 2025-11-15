package dev.questweaver.feature.encounter.state

import dev.questweaver.feature.encounter.viewmodel.CompletionStatus
import dev.questweaver.feature.encounter.viewmodel.Creature

/**
 * Detects encounter completion conditions (victory, defeat, fled).
 * Implements D&D 5e SRD rules for encounter resolution.
 */
class CompletionDetector {
    
    companion object {
        private const val XP_PER_DEFEATED_ENEMY = 100
    }
    
    /**
     * Checks if the encounter is complete based on creature states.
     *
     * @param creatures Current creature states mapped by ID
     * @return CompletionStatus if encounter is complete, null if ongoing
     */
    fun checkCompletion(creatures: Map<Long, Creature>): CompletionStatus? {
        // Handle empty creature map - no creatures means defeat
        if (creatures.isEmpty()) {
            return CompletionStatus.Defeat
        }
        
        // Separate creatures into player-controlled and enemies
        val playerCreatures = creatures.values.filter { it.isPlayerControlled && it.hpCurrent > 0 }
        val enemyCreatures = creatures.values.filter { !it.isPlayerControlled && it.hpCurrent > 0 }
        
        // Determine completion status based on alive creatures
        return when {
            playerCreatures.isEmpty() -> CompletionStatus.Defeat
            enemyCreatures.isEmpty() -> CompletionStatus.Victory
            else -> null
        }
    }
    
    /**
     * Calculates rewards for encounter completion.
     *
     * @param creatures All creatures in the encounter
     * @param completionStatus How the encounter ended
     * @return EncounterRewards with XP and loot
     */
    fun calculateRewards(
        creatures: Map<Long, Creature>,
        completionStatus: CompletionStatus
    ): EncounterRewards {
        // Only award rewards for victory
        if (completionStatus != CompletionStatus.Victory) {
            return EncounterRewards(xpAwarded = 0, loot = emptyList())
        }
        
        // Calculate XP based on defeated enemy creatures
        // For now, using a simple formula: 100 XP per defeated enemy
        // TODO: Replace with actual CR-based XP calculation when creature CR is available
        val defeatedEnemies = creatures.values.filter { 
            !it.isPlayerControlled && it.hpCurrent <= 0 
        }
        val xpAwarded = defeatedEnemies.size * XP_PER_DEFEATED_ENEMY
        
        // Generate loot based on defeated creatures
        // TODO: Replace with actual loot table system when available
        val loot = defeatedEnemies.mapIndexed { index, creature ->
            LootItem(
                id = index.toLong(),
                name = "${creature.name}'s Equipment",
                quantity = 1
            )
        }
        
        return EncounterRewards(
            xpAwarded = xpAwarded,
            loot = loot
        )
    }
}
