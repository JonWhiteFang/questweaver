package dev.questweaver.feature.encounter.usecases

import dev.questweaver.core.rules.initiative.InitiativeRoller
import dev.questweaver.core.rules.initiative.SurpriseHandler
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.InitiativeEntryData
import dev.questweaver.feature.encounter.viewmodel.Creature
import dev.questweaver.feature.encounter.viewmodel.MapGrid

/**
 * Use case for initializing a new encounter.
 * Rolls initiative, handles surprise, and generates the EncounterStarted event.
 */
class InitializeEncounter(
    private val initiativeRoller: InitiativeRoller,
    private val surpriseHandler: SurpriseHandler
) {
    /**
     * Initializes an encounter with the given creatures and context.
     *
     * @param sessionId The session ID for this encounter
     * @param creatures List of creatures participating in the encounter
     * @param surprisedCreatures Set of creature IDs that are surprised
     * @param mapGrid The map grid configuration
     * @return EncounterStarted event with all initialization data
     */
    suspend operator fun invoke(
        sessionId: Long,
        creatures: List<Creature>,
        surprisedCreatures: Set<Long>,
        mapGrid: MapGrid
    ): EncounterStarted {
        // Build map of creature ID to Dexterity modifier
        // TODO: Get actual Dexterity modifier from creature stats
        val creatureDexModifiers = creatures.associate { creature ->
            creature.id to 0 // Placeholder: use actual Dex modifier
        }
        
        // Roll initiative for all creatures
        val initiativeEntries = initiativeRoller.rollInitiativeForAll(creatureDexModifiers)
        
        // Convert to serializable format
        val initiativeData = initiativeEntries.map { entry ->
            InitiativeEntryData(
                creatureId = entry.creatureId,
                roll = entry.roll,
                modifier = entry.modifier,
                total = entry.total
            )
        }
        
        // Check for surprise round
        val hasSurpriseRound = surpriseHandler.hasSurpriseRound(surprisedCreatures)
        
        // Generate EncounterStarted event
        return EncounterStarted(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            encounterId = sessionId, // Using sessionId as encounterId for now
            participants = creatures.map { it.id },
            initiativeOrder = initiativeData,
            surprisedCreatures = if (hasSurpriseRound) surprisedCreatures else emptySet()
        )
    }
}
