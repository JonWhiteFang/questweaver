package dev.questweaver.domain.values

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EnumSerializationTest : FunSpec({
    
    val json = Json { prettyPrint = false }
    
    context("TerrainType serialization") {
        test("should serialize EMPTY") {
            val serialized = json.encodeToString(TerrainType.EMPTY)
            serialized shouldBe "\"EMPTY\""
        }
        
        test("should serialize DIFFICULT") {
            val serialized = json.encodeToString(TerrainType.DIFFICULT)
            serialized shouldBe "\"DIFFICULT\""
        }
        
        test("should serialize IMPASSABLE") {
            val serialized = json.encodeToString(TerrainType.IMPASSABLE)
            serialized shouldBe "\"IMPASSABLE\""
        }
        
        test("should serialize OCCUPIED") {
            val serialized = json.encodeToString(TerrainType.OCCUPIED)
            serialized shouldBe "\"OCCUPIED\""
        }
        
        test("should deserialize EMPTY") {
            val deserialized = json.decodeFromString<TerrainType>("\"EMPTY\"")
            deserialized shouldBe TerrainType.EMPTY
        }
        
        test("should round-trip all TerrainType values") {
            TerrainType.entries.forEach { terrainType ->
                val serialized = json.encodeToString(terrainType)
                val deserialized = json.decodeFromString<TerrainType>(serialized)
                deserialized shouldBe terrainType
            }
        }
    }
    
    context("Condition serialization") {
        test("should serialize BLINDED") {
            val serialized = json.encodeToString(Condition.BLINDED)
            serialized shouldBe "\"BLINDED\""
        }
        
        test("should serialize PARALYZED") {
            val serialized = json.encodeToString(Condition.PARALYZED)
            serialized shouldBe "\"PARALYZED\""
        }
        
        test("should serialize UNCONSCIOUS") {
            val serialized = json.encodeToString(Condition.UNCONSCIOUS)
            serialized shouldBe "\"UNCONSCIOUS\""
        }
        
        test("should deserialize POISONED") {
            val deserialized = json.decodeFromString<Condition>("\"POISONED\"")
            deserialized shouldBe Condition.POISONED
        }
        
        test("should round-trip all Condition values") {
            Condition.entries.forEach { condition ->
                val serialized = json.encodeToString(condition)
                val deserialized = json.decodeFromString<Condition>(serialized)
                deserialized shouldBe condition
            }
        }
        
        test("Condition should have all 14 D&D 5e conditions") {
            Condition.entries.size shouldBe 14
        }
    }
    
    context("Difficulty serialization") {
        test("should serialize EASY") {
            val serialized = json.encodeToString(Difficulty.EASY)
            serialized shouldBe "\"EASY\""
        }
        
        test("should serialize NORMAL") {
            val serialized = json.encodeToString(Difficulty.NORMAL)
            serialized shouldBe "\"NORMAL\""
        }
        
        test("should serialize HARD") {
            val serialized = json.encodeToString(Difficulty.HARD)
            serialized shouldBe "\"HARD\""
        }
        
        test("should serialize DEADLY") {
            val serialized = json.encodeToString(Difficulty.DEADLY)
            serialized shouldBe "\"DEADLY\""
        }
        
        test("should round-trip all Difficulty values") {
            Difficulty.entries.forEach { difficulty ->
                val serialized = json.encodeToString(difficulty)
                val deserialized = json.decodeFromString<Difficulty>(serialized)
                deserialized shouldBe difficulty
            }
        }
    }
    
    context("ContentRating serialization") {
        test("should serialize EVERYONE") {
            val serialized = json.encodeToString(ContentRating.EVERYONE)
            serialized shouldBe "\"EVERYONE\""
        }
        
        test("should serialize TEEN") {
            val serialized = json.encodeToString(ContentRating.TEEN)
            serialized shouldBe "\"TEEN\""
        }
        
        test("should serialize MATURE") {
            val serialized = json.encodeToString(ContentRating.MATURE)
            serialized shouldBe "\"MATURE\""
        }
        
        test("should round-trip all ContentRating values") {
            ContentRating.entries.forEach { rating ->
                val serialized = json.encodeToString(rating)
                val deserialized = json.decodeFromString<ContentRating>(serialized)
                deserialized shouldBe rating
            }
        }
    }
    
    context("EncounterStatus serialization") {
        test("should serialize IN_PROGRESS") {
            val serialized = json.encodeToString(EncounterStatus.IN_PROGRESS)
            serialized shouldBe "\"IN_PROGRESS\""
        }
        
        test("should serialize VICTORY") {
            val serialized = json.encodeToString(EncounterStatus.VICTORY)
            serialized shouldBe "\"VICTORY\""
        }
        
        test("should serialize DEFEAT") {
            val serialized = json.encodeToString(EncounterStatus.DEFEAT)
            serialized shouldBe "\"DEFEAT\""
        }
        
        test("should serialize FLED") {
            val serialized = json.encodeToString(EncounterStatus.FLED)
            serialized shouldBe "\"FLED\""
        }
        
        test("should round-trip all EncounterStatus values") {
            EncounterStatus.entries.forEach { status ->
                val serialized = json.encodeToString(status)
                val deserialized = json.decodeFromString<EncounterStatus>(serialized)
                deserialized shouldBe status
            }
        }
    }
})
