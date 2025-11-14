package dev.questweaver.domain

import dev.questweaver.domain.entities.Campaign
import dev.questweaver.domain.entities.CampaignSettings
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.Encounter
import dev.questweaver.domain.events.InitiativeEntryData
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.ConditionApplied
import dev.questweaver.domain.events.ConditionRemoved
import dev.questweaver.domain.events.CreatureAddedToCombat
import dev.questweaver.domain.events.CreatureRemovedFromCombat
import dev.questweaver.domain.events.DamageApplied
import dev.questweaver.domain.events.DelayedTurnResumed
import dev.questweaver.domain.events.EncounterEnded
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.events.ReactionUsed
import dev.questweaver.domain.events.RoundStarted
import dev.questweaver.domain.events.TurnDelayed
import dev.questweaver.domain.events.TurnEnded
import dev.questweaver.domain.events.TurnStarted
import dev.questweaver.domain.repositories.CampaignRepository
import dev.questweaver.domain.repositories.CreatureRepository
import dev.questweaver.domain.repositories.EncounterRepository
import dev.questweaver.domain.repositories.EventRepository
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.Condition
import dev.questweaver.domain.values.ContentRating
import dev.questweaver.domain.values.DiceRoll
import dev.questweaver.domain.values.Difficulty
import dev.questweaver.domain.values.EncounterStatus
import dev.questweaver.domain.values.GridPos
import dev.questweaver.domain.values.TerrainType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Integration verification tests for core domain module.
 * Tests creating sample domain objects, copy operations, serialization,
 * event sourcing patterns, and repository contracts.
 */
@Suppress("LargeClass") // Integration test covering multiple verification scenarios
class IntegrationVerificationTest : FunSpec({
    
    val json = Json { 
        prettyPrint = true
        allowStructuredMapKeys = true
    }
    
    context("9.1 Create sample domain objects") {
        
        test("should create all entity instances successfully") {
            // Create Creature
            val creature = Creature(
                id = 1,
                name = "Goblin Warrior",
                armorClass = 15,
                hitPointsCurrent = 7,
                hitPointsMax = 7,
                speed = 30,
                abilities = Abilities(
                    strength = 8,
                    dexterity = 14,
                    constitution = 10,
                    intelligence = 10,
                    wisdom = 8,
                    charisma = 8
                ),
                proficiencyBonus = 2,
                conditions = setOf(Condition.POISONED, Condition.PRONE)
            )
            
            creature.id shouldBe 1
            creature.name shouldBe "Goblin Warrior"
            creature.isAlive shouldBe true
            creature.isBloodied shouldBe false
            
            // Create Campaign
            val campaign = Campaign(
                id = 1,
                name = "Lost Mine of Phandelver",
                createdTimestamp = 1000000,
                lastPlayedTimestamp = 2000000,
                playerCharacterId = 10,
                settings = CampaignSettings(
                    difficulty = Difficulty.NORMAL,
                    contentRating = ContentRating.TEEN
                )
            )
            
            campaign.id shouldBe 1
            campaign.name shouldBe "Lost Mine of Phandelver"
            campaign.settings.difficulty shouldBe Difficulty.NORMAL
            
            // Create Encounter
            val encounter = Encounter(
                id = 1,
                campaignId = 1,
                createdTimestamp = 2000000,
                currentRound = 1,
                activeCreatureId = 10,
                participants = listOf(10, 1, 2),
                initiativeOrder = listOf(
                    InitiativeEntryData(10, 18, 4, 22),
                    InitiativeEntryData(1, 15, 2, 17),
                    InitiativeEntryData(2, 12, 1, 13)
                ),
                status = EncounterStatus.IN_PROGRESS
            )
            
            encounter.id shouldBe 1
            encounter.currentRound shouldBe 1
            encounter.status shouldBe EncounterStatus.IN_PROGRESS
            
            // Create MapGrid
            val mapGrid = MapGrid(
                width = 20,
                height = 20,
                terrain = mapOf(
                    GridPos(5, 5) to TerrainType.DIFFICULT,
                    GridPos(10, 10) to TerrainType.IMPASSABLE
                ),
                creaturePositions = mapOf(
                    10L to GridPos(0, 0),
                    1L to GridPos(5, 0),
                    2L to GridPos(10, 0)
                )
            )
            
            mapGrid.width shouldBe 20
            mapGrid.height shouldBe 20
            mapGrid.isInBounds(GridPos(19, 19)) shouldBe true
            mapGrid.isInBounds(GridPos(20, 20)) shouldBe false
        }
        
        test("should verify copy operations work correctly for Creature") {
            val original = Creature(
                id = 1,
                name = "Goblin",
                armorClass = 15,
                hitPointsCurrent = 7,
                hitPointsMax = 7,
                speed = 30,
                abilities = Abilities(strength = 10)
            )
            
            // Copy with modified HP
            val damaged = original.copy(hitPointsCurrent = 3)
            
            damaged.id shouldBe original.id
            damaged.name shouldBe original.name
            damaged.hitPointsCurrent shouldBe 3
            damaged.hitPointsMax shouldBe 7
            damaged.isBloodied shouldBe true
            
            // Copy with added condition
            val poisoned = original.copy(conditions = setOf(Condition.POISONED))
            
            poisoned.conditions shouldBe setOf(Condition.POISONED)
            original.conditions shouldBe emptySet()
        }
        
        test("should verify copy operations work correctly for Campaign") {
            val original = Campaign(
                id = 1,
                name = "Test Campaign",
                createdTimestamp = 1000,
                lastPlayedTimestamp = 1000,
                playerCharacterId = 1,
                settings = CampaignSettings()
            )
            
            // Update last played timestamp
            val updated = original.copy(lastPlayedTimestamp = 2000)
            
            updated.lastPlayedTimestamp shouldBe 2000
            updated.createdTimestamp shouldBe 1000
            original.lastPlayedTimestamp shouldBe 1000
            
            // Change settings
            val harder = original.copy(
                settings = CampaignSettings(difficulty = Difficulty.HARD)
            )
            
            harder.settings.difficulty shouldBe Difficulty.HARD
            original.settings.difficulty shouldBe Difficulty.NORMAL
        }
        
        test("should verify copy operations work correctly for Encounter") {
            val original = Encounter(
                id = 1,
                campaignId = 1,
                createdTimestamp = 1000,
                currentRound = 1,
                activeCreatureId = 10,
                participants = listOf(10, 20),
                initiativeOrder = listOf(
                    InitiativeEntryData(10, 18, 4, 22),
                    InitiativeEntryData(20, 15, 2, 17)
                )
            )
            
            // Advance round
            val nextRound = original.copy(
                currentRound = 2,
                activeCreatureId = 20
            )
            
            nextRound.currentRound shouldBe 2
            nextRound.activeCreatureId shouldBe 20
            original.currentRound shouldBe 1
            
            // End encounter
            val ended = original.copy(
                status = EncounterStatus.VICTORY,
                activeCreatureId = null
            )
            
            ended.status shouldBe EncounterStatus.VICTORY
            ended.activeCreatureId shouldBe null
            original.status shouldBe EncounterStatus.IN_PROGRESS
        }
        
        test("should verify copy operations work correctly for MapGrid") {
            val original = MapGrid(
                width = 10,
                height = 10,
                terrain = mapOf(GridPos(5, 5) to TerrainType.DIFFICULT),
                creaturePositions = mapOf(1L to GridPos(0, 0))
            )
            
            // Move creature
            val moved = original.copy(
                creaturePositions = mapOf(1L to GridPos(5, 5))
            )
            
            moved.creaturePositions[1L] shouldBe GridPos(5, 5)
            original.creaturePositions[1L] shouldBe GridPos(0, 0)
            
            // Add terrain
            val withTerrain = original.copy(
                terrain = original.terrain + (GridPos(3, 3) to TerrainType.IMPASSABLE)
            )
            
            withTerrain.terrain.size shouldBe 2
            original.terrain.size shouldBe 1
        }
        
        test("should verify serialization round-trips successfully for all entities") {
            // Creature
            val creature = Creature(
                id = 1,
                name = "Test Creature",
                armorClass = 15,
                hitPointsCurrent = 20,
                hitPointsMax = 20,
                speed = 30,
                abilities = Abilities(strength = 16, dexterity = 14),
                proficiencyBonus = 3,
                conditions = setOf(Condition.BLINDED, Condition.DEAFENED)
            )
            
            val creatureSerialized = json.encodeToString(creature)
            val creatureDeserialized = json.decodeFromString<Creature>(creatureSerialized)
            creatureDeserialized shouldBe creature
            
            // Campaign
            val campaign = Campaign(
                id = 2,
                name = "Epic Quest",
                createdTimestamp = 5000,
                lastPlayedTimestamp = 10000,
                playerCharacterId = 5,
                settings = CampaignSettings(
                    difficulty = Difficulty.DEADLY,
                    contentRating = ContentRating.MATURE
                )
            )
            
            val campaignSerialized = json.encodeToString(campaign)
            val campaignDeserialized = json.decodeFromString<Campaign>(campaignSerialized)
            campaignDeserialized shouldBe campaign
            
            // Encounter
            val encounter = Encounter(
                id = 3,
                campaignId = 2,
                createdTimestamp = 10000,
                currentRound = 5,
                activeCreatureId = 5,
                participants = listOf(5, 1, 2, 3),
                initiativeOrder = listOf(
                    InitiativeEntryData(5, 20, 3, 23),
                    InitiativeEntryData(1, 18, 2, 20),
                    InitiativeEntryData(2, 15, 2, 17),
                    InitiativeEntryData(3, 10, 1, 11)
                ),
                status = EncounterStatus.IN_PROGRESS
            )
            
            val encounterSerialized = json.encodeToString(encounter)
            val encounterDeserialized = json.decodeFromString<Encounter>(encounterSerialized)
            encounterDeserialized shouldBe encounter
            
            // MapGrid
            val mapGrid = MapGrid(
                width = 15,
                height = 15,
                terrain = mapOf(
                    GridPos(0, 0) to TerrainType.EMPTY,
                    GridPos(5, 5) to TerrainType.DIFFICULT,
                    GridPos(10, 10) to TerrainType.IMPASSABLE
                ),
                creaturePositions = mapOf(
                    5L to GridPos(1, 1),
                    1L to GridPos(2, 2),
                    2L to GridPos(3, 3)
                )
            )
            
            val mapGridSerialized = json.encodeToString(mapGrid)
            val mapGridDeserialized = json.decodeFromString<MapGrid>(mapGridSerialized)
            mapGridDeserialized shouldBe mapGrid
        }
        
        test("should create all value objects successfully") {
            // GridPos
            val pos = GridPos(5, 10)
            pos.x shouldBe 5
            pos.y shouldBe 10
            pos.distanceTo(GridPos(8, 14)) shouldBe 4
            
            // Abilities
            val abilities = Abilities(
                strength = 16,
                dexterity = 14,
                constitution = 15,
                intelligence = 10,
                wisdom = 12,
                charisma = 8
            )
            abilities.strModifier shouldBe 3
            abilities.dexModifier shouldBe 2
            abilities.conModifier shouldBe 2
            abilities.intModifier shouldBe 0
            abilities.wisModifier shouldBe 1
            abilities.chaModifier shouldBe -1
            
            // DiceRoll
            val roll = DiceRoll(
                diceType = 20,
                count = 1,
                modifier = 5,
                result = 18
            )
            roll.diceType shouldBe 20
            roll.result shouldBe 18
            roll.toString() shouldBe "1d20+5 = 18"
        }
    }
    
    context("9.2 Verify event sourcing pattern") {
        
        test("should generate events for a sample encounter") {
            val sessionId = 1L
            val timestamp = System.currentTimeMillis()
            
            // Start encounter
            val encounterStarted = EncounterStarted(
                sessionId = sessionId,
                timestamp = timestamp,
                encounterId = 1,
                participants = listOf(10, 1, 2),
                initiativeOrder = listOf(
                    InitiativeEntryData(10, 18, 4, 22),
                    InitiativeEntryData(1, 15, 2, 17),
                    InitiativeEntryData(2, 12, 1, 13)
                )
            )
            
            encounterStarted.sessionId shouldBe sessionId
            encounterStarted.encounterId shouldBe 1
            
            // Start round
            val roundStarted = RoundStarted(
                sessionId = sessionId,
                timestamp = timestamp + 1,
                encounterId = 1,
                roundNumber = 1
            )
            
            roundStarted.roundNumber shouldBe 1
            
            // Start turn
            val turnStarted = TurnStarted(
                sessionId = sessionId,
                timestamp = timestamp + 2,
                encounterId = 1,
                creatureId = 10
            )
            
            turnStarted.creatureId shouldBe 10
            
            // Move
            val moveCommitted = MoveCommitted(
                sessionId = sessionId,
                timestamp = timestamp + 3,
                creatureId = 10,
                fromPos = GridPos(0, 0),
                toPos = GridPos(5, 0),
                path = listOf(
                    GridPos(0, 0),
                    GridPos(1, 0),
                    GridPos(2, 0),
                    GridPos(3, 0),
                    GridPos(4, 0),
                    GridPos(5, 0)
                ),
                movementCost = 5
            )
            
            moveCommitted.fromPos shouldBe GridPos(0, 0)
            moveCommitted.toPos shouldBe GridPos(5, 0)
            moveCommitted.movementCost shouldBe 5
            
            // Attack
            val attackResolved = AttackResolved(
                sessionId = sessionId,
                timestamp = timestamp + 4,
                attackerId = 10,
                targetId = 1,
                attackRoll = DiceRoll(20, 1, 5, 18),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            attackResolved.hit shouldBe true
            attackResolved.critical shouldBe false
            
            // Damage
            val damageApplied = DamageApplied(
                sessionId = sessionId,
                timestamp = timestamp + 5,
                targetId = 1,
                damageRoll = DiceRoll(8, 1, 3, 8),
                damageAmount = 8,
                hpBefore = 7,
                hpAfter = 0
            )
            
            damageApplied.damageAmount shouldBe 8
            damageApplied.hpAfter shouldBe 0
            
            // Apply condition
            val conditionApplied = ConditionApplied(
                sessionId = sessionId,
                timestamp = timestamp + 6,
                targetId = 2,
                condition = Condition.FRIGHTENED,
                duration = 1
            )
            
            conditionApplied.condition shouldBe Condition.FRIGHTENED
            conditionApplied.duration shouldBe 1
            
            // Remove condition
            val conditionRemoved = ConditionRemoved(
                sessionId = sessionId,
                timestamp = timestamp + 7,
                targetId = 2,
                condition = Condition.FRIGHTENED
            )
            
            conditionRemoved.condition shouldBe Condition.FRIGHTENED
            
            // End turn
            val turnEnded = TurnEnded(
                sessionId = sessionId,
                timestamp = timestamp + 8,
                encounterId = 1,
                creatureId = 10
            )
            
            turnEnded.creatureId shouldBe 10
            
            // End encounter
            val encounterEnded = EncounterEnded(
                sessionId = sessionId,
                timestamp = timestamp + 9,
                encounterId = 1,
                status = EncounterStatus.VICTORY
            )
            
            encounterEnded.status shouldBe EncounterStatus.VICTORY
            
            // Verify all events are GameEvent instances
            val allEvents: List<GameEvent> = listOf(
                encounterStarted,
                roundStarted,
                turnStarted,
                moveCommitted,
                attackResolved,
                damageApplied,
                conditionApplied,
                conditionRemoved,
                turnEnded,
                encounterEnded
            )
            
            allEvents.size shouldBe 10
            allEvents.forEach { event ->
                event.sessionId shouldBe sessionId
                event.shouldBeInstanceOf<GameEvent>()
            }
        }
        
        test("should verify events are immutable") {
            val event = AttackResolved(
                sessionId = 1,
                timestamp = 1000,
                attackerId = 10,
                targetId = 20,
                attackRoll = DiceRoll(20, 1, 5, 18),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            // Events are data classes with val properties - immutable by design
            event.sessionId shouldBe 1
            event.attackerId shouldBe 10
            event.hit shouldBe true
            
            // Can create modified copy, but original is unchanged
            val modified = event.copy(hit = false)
            modified.hit shouldBe false
            event.hit shouldBe true
        }
        
        test("should verify exhaustive when expressions compile") {
            val events: List<GameEvent> = listOf(
                EncounterStarted(1, 1000, 1, listOf(1), listOf(InitiativeEntryData(1, 10, 0, 10))),
                RoundStarted(1, 1001, 1, 1),
                TurnStarted(1, 1002, 1, 1),
                TurnEnded(1, 1003, 1, 1),
                EncounterEnded(1, 1004, 1, EncounterStatus.VICTORY),
                AttackResolved(1, 1005, 1, 2, DiceRoll(20, 1, 0, 15), 15, true, false),
                DamageApplied(1, 1006, 2, DiceRoll(8, 1, 0, 5), 5, 10, 5),
                ConditionApplied(1, 1007, 2, Condition.PRONE, null),
                ConditionRemoved(1, 1008, 2, Condition.PRONE),
                MoveCommitted(1, 1009, 1, GridPos(0, 0), GridPos(5, 0), listOf(GridPos(0, 0), GridPos(5, 0)), 5)
            )
            
            // Exhaustive when expression - compiler enforces all cases
            val eventTypes = events.map { event ->
                when (event) {
                    is EncounterStarted -> "EncounterStarted"
                    is RoundStarted -> "RoundStarted"
                    is TurnStarted -> "TurnStarted"
                    is TurnEnded -> "TurnEnded"
                    is EncounterEnded -> "EncounterEnded"
                    is AttackResolved -> "AttackResolved"
                    is DamageApplied -> "DamageApplied"
                    is ConditionApplied -> "ConditionApplied"
                    is ConditionRemoved -> "ConditionRemoved"
                    is MoveCommitted -> "MoveCommitted"
                    is ReactionUsed -> "ReactionUsed"
                    is TurnDelayed -> "TurnDelayed"
                    is DelayedTurnResumed -> "DelayedTurnResumed"
                    is CreatureAddedToCombat -> "CreatureAddedToCombat"
                    is CreatureRemovedFromCombat -> "CreatureRemovedFromCombat"
                    // No else branch needed - sealed interface ensures exhaustiveness
                }
            }
            
            eventTypes.size shouldBe 10
            eventTypes shouldBe listOf(
                "EncounterStarted",
                "RoundStarted",
                "TurnStarted",
                "TurnEnded",
                "EncounterEnded",
                "AttackResolved",
                "DamageApplied",
                "ConditionApplied",
                "ConditionRemoved",
                "MoveCommitted"
            )
        }
        
        test("should serialize and deserialize all event types") {
            val events: List<GameEvent> = listOf(
                EncounterStarted(1, 1000, 1, listOf(1, 2), listOf(InitiativeEntryData(1, 18, 4, 22), InitiativeEntryData(2, 15, 2, 17))),
                RoundStarted(1, 1001, 1, 1),
                TurnStarted(1, 1002, 1, 1),
                MoveCommitted(1, 1003, 1, GridPos(0, 0), GridPos(5, 5), listOf(GridPos(0, 0), GridPos(5, 5)), 5),
                AttackResolved(1, 1004, 1, 2, DiceRoll(20, 1, 5, 20), 15, true, true),
                DamageApplied(1, 1005, 2, DiceRoll(8, 2, 3, 15), 15, 20, 5),
                ConditionApplied(1, 1006, 2, Condition.STUNNED, 1),
                ConditionRemoved(1, 1007, 2, Condition.STUNNED),
                TurnEnded(1, 1008, 1, 1),
                EncounterEnded(1, 1009, 1, EncounterStatus.VICTORY)
            )
            
            events.forEach { event ->
                val serialized = json.encodeToString<GameEvent>(event)
                val deserialized = json.decodeFromString<GameEvent>(serialized)
                deserialized shouldBe event
            }
        }
    }
    
    context("9.3 Verify repository contracts") {
        
        test("should create mock EventRepository and verify Flow types") {
            // Mock implementation for testing
            val mockEventRepository = object : EventRepository {
                private val events = mutableListOf<GameEvent>()
                
                override suspend fun append(event: GameEvent) {
                    events.add(event)
                }
                
                override suspend fun appendAll(events: List<GameEvent>) {
                    this.events.addAll(events)
                }
                
                override suspend fun forSession(sessionId: Long): List<GameEvent> {
                    return events.filter { it.sessionId == sessionId }
                }
                
                override fun observeSession(sessionId: Long): Flow<List<GameEvent>> {
                    return flowOf(events.filter { it.sessionId == sessionId })
                }
            }
            
            // Test append
            val event1 = AttackResolved(
                sessionId = 1,
                timestamp = 1000,
                attackerId = 10,
                targetId = 20,
                attackRoll = DiceRoll(20, 1, 5, 18),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            mockEventRepository.append(event1)
            
            // Test forSession
            val sessionEvents = mockEventRepository.forSession(1)
            sessionEvents.size shouldBe 1
            sessionEvents.first() shouldBe event1
            
            // Test appendAll
            val event2 = DamageApplied(
                sessionId = 1,
                timestamp = 1001,
                targetId = 20,
                damageRoll = DiceRoll(8, 1, 3, 8),
                damageAmount = 8,
                hpBefore = 20,
                hpAfter = 12
            )
            
            val event3 = TurnEnded(
                sessionId = 1,
                timestamp = 1002,
                encounterId = 1,
                creatureId = 10
            )
            
            mockEventRepository.appendAll(listOf(event2, event3))
            
            val allEvents = mockEventRepository.forSession(1)
            allEvents.size shouldBe 3
            
            // Test observeSession Flow
            val observedEvents = mockEventRepository.observeSession(1).first()
            observedEvents.size shouldBe 3
            observedEvents shouldBe allEvents
            
            // Verify Flow type works correctly
            val flow = mockEventRepository.observeSession(1)
            flow.shouldBeInstanceOf<Flow<List<GameEvent>>>()
        }
        
        test("should create mock CreatureRepository and verify suspend functions") {
            val mockCreatureRepository = object : CreatureRepository {
                private val creatures = mutableMapOf<Long, Creature>()
                private var nextId = 1L
                
                override suspend fun getById(id: Long): Creature? {
                    return creatures[id]
                }
                
                override suspend fun getAll(): List<Creature> {
                    return creatures.values.toList()
                }
                
                override suspend fun insert(creature: Creature): Long {
                    val id = if (creature.id > 0) creature.id else nextId++
                    creatures[id] = creature.copy(id = id)
                    if (creature.id <= 0) nextId = maxOf(nextId, id + 1)
                    return id
                }
                
                override suspend fun update(creature: Creature) {
                    creatures[creature.id] = creature
                }
                
                override suspend fun delete(id: Long) {
                    creatures.remove(id)
                }
                
                override fun observe(id: Long): Flow<Creature?> {
                    return flowOf(creatures[id])
                }
            }
            
            // Test insert
            val creature = Creature(
                id = 1, // Will be assigned by repository
                name = "Goblin",
                armorClass = 15,
                hitPointsCurrent = 7,
                hitPointsMax = 7,
                speed = 30,
                abilities = Abilities()
            )
            
            val insertedId = mockCreatureRepository.insert(creature)
            insertedId shouldBe 1
            
            // Test getById
            val retrieved = mockCreatureRepository.getById(insertedId)
            retrieved shouldNotBe null
            retrieved?.name shouldBe "Goblin"
            
            // Test update
            val damaged = retrieved!!.copy(hitPointsCurrent = 3)
            mockCreatureRepository.update(damaged)
            
            val updated = mockCreatureRepository.getById(insertedId)
            updated?.hitPointsCurrent shouldBe 3
            
            // Test getAll
            val creature2 = Creature(
                id = 2,
                name = "Orc",
                armorClass = 13,
                hitPointsCurrent = 15,
                hitPointsMax = 15,
                speed = 30,
                abilities = Abilities(strength = 16)
            )
            mockCreatureRepository.insert(creature2)
            
            val allCreatures = mockCreatureRepository.getAll()
            allCreatures.size shouldBe 2
            
            // Test observe Flow
            val observed = mockCreatureRepository.observe(insertedId).first()
            observed shouldNotBe null
            observed?.hitPointsCurrent shouldBe 3
            
            // Test delete
            mockCreatureRepository.delete(insertedId)
            val deleted = mockCreatureRepository.getById(insertedId)
            deleted shouldBe null
        }
        
        test("should create mock CampaignRepository") {
            val mockCampaignRepository = object : CampaignRepository {
                private val campaigns = mutableMapOf<Long, Campaign>()
                private var nextId = 1L
                
                override suspend fun getById(id: Long): Campaign? {
                    return campaigns[id]
                }
                
                override suspend fun getAll(): List<Campaign> {
                    return campaigns.values.toList()
                }
                
                override suspend fun insert(campaign: Campaign): Long {
                    val id = if (campaign.id > 0) campaign.id else nextId++
                    campaigns[id] = campaign.copy(id = id)
                    if (campaign.id <= 0) nextId = maxOf(nextId, id + 1)
                    return id
                }
                
                override suspend fun update(campaign: Campaign) {
                    campaigns[campaign.id] = campaign
                }
                
                override suspend fun delete(id: Long) {
                    campaigns.remove(id)
                }
                
                override fun observeAll(): Flow<List<Campaign>> {
                    return flowOf(campaigns.values.toList())
                }
            }
            
            // Test insert
            val campaign = Campaign(
                id = 1,
                name = "Test Campaign",
                createdTimestamp = 1000,
                lastPlayedTimestamp = 1000,
                playerCharacterId = 1,
                settings = CampaignSettings()
            )
            
            val insertedId = mockCampaignRepository.insert(campaign)
            insertedId shouldBe 1
            
            // Test getById
            val retrieved = mockCampaignRepository.getById(insertedId)
            retrieved shouldNotBe null
            retrieved?.name shouldBe "Test Campaign"
            
            // Test update
            val updated = retrieved!!.copy(lastPlayedTimestamp = 2000)
            mockCampaignRepository.update(updated)
            
            val afterUpdate = mockCampaignRepository.getById(insertedId)
            afterUpdate?.lastPlayedTimestamp shouldBe 2000
            
            // Test observeAll Flow
            val observed = mockCampaignRepository.observeAll().first()
            observed.size shouldBe 1
            observed.first().lastPlayedTimestamp shouldBe 2000
            
            // Test delete
            mockCampaignRepository.delete(insertedId)
            val deleted = mockCampaignRepository.getById(insertedId)
            deleted shouldBe null
        }
        
        test("should create mock EncounterRepository") {
            val mockEncounterRepository = object : EncounterRepository {
                private val encounters = mutableMapOf<Long, Encounter>()
                private var nextId = 1L
                
                override suspend fun getById(id: Long): Encounter? {
                    return encounters[id]
                }
                
                override suspend fun getByCampaign(campaignId: Long): List<Encounter> {
                    return encounters.values.filter { it.campaignId == campaignId }
                }
                
                override suspend fun insert(encounter: Encounter): Long {
                    val id = if (encounter.id > 0) encounter.id else nextId++
                    encounters[id] = encounter.copy(id = id)
                    if (encounter.id <= 0) nextId = maxOf(nextId, id + 1)
                    return id
                }
                
                override suspend fun update(encounter: Encounter) {
                    encounters[encounter.id] = encounter
                }
                
                override suspend fun delete(id: Long) {
                    encounters.remove(id)
                }
                
                override fun observe(id: Long): Flow<Encounter?> {
                    return flowOf(encounters[id])
                }
            }
            
            // Test insert
            val encounter = Encounter(
                id = 1,
                campaignId = 1,
                createdTimestamp = 1000,
                currentRound = 1,
                activeCreatureId = 10,
                participants = listOf(10, 20),
                initiativeOrder = listOf(
                    InitiativeEntryData(10, 18, 4, 22),
                    InitiativeEntryData(20, 15, 2, 17)
                )
            )
            
            val insertedId = mockEncounterRepository.insert(encounter)
            insertedId shouldBe 1
            
            // Test getById
            val retrieved = mockEncounterRepository.getById(insertedId)
            retrieved shouldNotBe null
            retrieved?.currentRound shouldBe 1
            
            // Test update
            val nextRound = retrieved!!.copy(currentRound = 2, activeCreatureId = 20)
            mockEncounterRepository.update(nextRound)
            
            val afterUpdate = mockEncounterRepository.getById(insertedId)
            afterUpdate?.currentRound shouldBe 2
            afterUpdate?.activeCreatureId shouldBe 20
            
            // Test getByCampaign
            val encounter2 = Encounter(
                id = 2,
                campaignId = 1,
                createdTimestamp = 2000,
                currentRound = 1,
                activeCreatureId = 30,
                participants = listOf(30),
                initiativeOrder = listOf(InitiativeEntryData(30, 15, 2, 17))
            )
            mockEncounterRepository.insert(encounter2)
            
            val campaignEncounters = mockEncounterRepository.getByCampaign(1)
            campaignEncounters.size shouldBe 2
            
            // Test observe Flow
            val observed = mockEncounterRepository.observe(insertedId).first()
            observed shouldNotBe null
            observed?.currentRound shouldBe 2
            
            // Test delete
            mockEncounterRepository.delete(insertedId)
            val deleted = mockEncounterRepository.getById(insertedId)
            deleted shouldBe null
        }
    }
})
