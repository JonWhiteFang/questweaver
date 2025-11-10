package dev.questweaver.domain.entities

import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.Condition
import dev.questweaver.domain.values.ContentRating
import dev.questweaver.domain.values.Difficulty
import dev.questweaver.domain.values.EncounterStatus
import dev.questweaver.domain.values.GridPos
import dev.questweaver.domain.values.TerrainType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EntitySerializationTest : FunSpec({
    
    val json = Json { 
        prettyPrint = true
        allowStructuredMapKeys = true
    }
    
    context("Creature serialization") {
        test("should serialize and deserialize Creature") {
            val creature = Creature(
                id = 1,
                name = "Goblin",
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
                conditions = setOf(Condition.POISONED)
            )
            
            val serialized = json.encodeToString(creature)
            val deserialized = json.decodeFromString<Creature>(serialized)
            
            deserialized shouldBe creature
        }
        
        test("should serialize Creature with empty conditions") {
            val creature = Creature(
                id = 1,
                name = "Test",
                armorClass = 10,
                hitPointsCurrent = 10,
                hitPointsMax = 10,
                speed = 30,
                abilities = Abilities()
            )
            
            val serialized = json.encodeToString(creature)
            val deserialized = json.decodeFromString<Creature>(serialized)
            
            deserialized shouldBe creature
            deserialized.conditions shouldBe emptySet()
        }
    }
    
    context("Campaign serialization") {
        test("should serialize and deserialize Campaign") {
            val campaign = Campaign(
                id = 1,
                name = "Test Campaign",
                createdTimestamp = 1000,
                lastPlayedTimestamp = 2000,
                playerCharacterId = 5,
                settings = CampaignSettings(
                    difficulty = Difficulty.HARD,
                    contentRating = ContentRating.MATURE
                )
            )
            
            val serialized = json.encodeToString(campaign)
            val deserialized = json.decodeFromString<Campaign>(serialized)
            
            deserialized shouldBe campaign
        }
        
        test("should serialize Campaign with default settings") {
            val campaign = Campaign(
                id = 1,
                name = "Test",
                createdTimestamp = 1000,
                lastPlayedTimestamp = 1000,
                playerCharacterId = 1,
                settings = CampaignSettings()
            )
            
            val serialized = json.encodeToString(campaign)
            val deserialized = json.decodeFromString<Campaign>(serialized)
            
            deserialized shouldBe campaign
            deserialized.settings.difficulty shouldBe Difficulty.NORMAL
            deserialized.settings.contentRating shouldBe ContentRating.TEEN
        }
    }
    
    context("Encounter serialization") {
        test("should serialize and deserialize Encounter") {
            val encounter = Encounter(
                id = 1,
                campaignId = 5,
                createdTimestamp = 1000,
                currentRound = 3,
                activeCreatureId = 10,
                participants = listOf(10, 20, 30),
                initiativeOrder = listOf(
                    InitiativeEntry(10, 18),
                    InitiativeEntry(20, 15),
                    InitiativeEntry(30, 12)
                ),
                status = EncounterStatus.IN_PROGRESS
            )
            
            val serialized = json.encodeToString(encounter)
            val deserialized = json.decodeFromString<Encounter>(serialized)
            
            deserialized shouldBe encounter
        }
        
        test("should serialize Encounter with null active creature") {
            val encounter = Encounter(
                id = 1,
                campaignId = 5,
                createdTimestamp = 1000,
                currentRound = 1,
                activeCreatureId = null,
                participants = listOf(10),
                initiativeOrder = listOf(InitiativeEntry(10, 18)),
                status = EncounterStatus.VICTORY
            )
            
            val serialized = json.encodeToString(encounter)
            val deserialized = json.decodeFromString<Encounter>(serialized)
            
            deserialized shouldBe encounter
            deserialized.activeCreatureId shouldBe null
        }
    }
    
    context("MapGrid serialization") {
        test("should serialize and deserialize MapGrid") {
            val grid = MapGrid(
                width = 10,
                height = 10,
                terrain = mapOf(
                    GridPos(0, 0) to TerrainType.DIFFICULT,
                    GridPos(5, 5) to TerrainType.IMPASSABLE
                ),
                creaturePositions = mapOf(
                    1L to GridPos(2, 3),
                    2L to GridPos(7, 8)
                )
            )
            
            val serialized = json.encodeToString(grid)
            val deserialized = json.decodeFromString<MapGrid>(serialized)
            
            deserialized shouldBe grid
        }
        
        test("should serialize MapGrid with empty terrain and positions") {
            val grid = MapGrid(width = 5, height = 5)
            
            val serialized = json.encodeToString(grid)
            val deserialized = json.decodeFromString<MapGrid>(serialized)
            
            deserialized shouldBe grid
            deserialized.terrain shouldBe emptyMap()
            deserialized.creaturePositions shouldBe emptyMap()
        }
    }
    
    context("InitiativeEntry serialization") {
        test("should serialize and deserialize InitiativeEntry") {
            val entry = InitiativeEntry(creatureId = 10, initiative = 18)
            
            val serialized = json.encodeToString(entry)
            val deserialized = json.decodeFromString<InitiativeEntry>(serialized)
            
            deserialized shouldBe entry
        }
    }
    
    context("CampaignSettings serialization") {
        test("should serialize and deserialize CampaignSettings") {
            val settings = CampaignSettings(
                difficulty = Difficulty.DEADLY,
                contentRating = ContentRating.EVERYONE
            )
            
            val serialized = json.encodeToString(settings)
            val deserialized = json.decodeFromString<CampaignSettings>(serialized)
            
            deserialized shouldBe settings
        }
    }
})
