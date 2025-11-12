package dev.questweaver.domain.map.geometry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class CellPropertiesTest : FunSpec({
    
    context("CellProperties defaults") {
        test("should have NORMAL terrain type by default") {
            val props = CellProperties()
            props.terrainType shouldBe TerrainType.NORMAL
        }
        
        test("should have no obstacle by default") {
            val props = CellProperties()
            props.hasObstacle shouldBe false
        }
        
        test("should have no occupant by default") {
            val props = CellProperties()
            props.occupiedBy shouldBe null
        }
    }
    
    context("CellProperties creation") {
        test("should create with custom terrain type") {
            val props = CellProperties(terrainType = TerrainType.DIFFICULT)
            props.terrainType shouldBe TerrainType.DIFFICULT
        }
        
        test("should create with obstacle") {
            val props = CellProperties(hasObstacle = true)
            props.hasObstacle shouldBe true
        }
        
        test("should create with occupant") {
            val props = CellProperties(occupiedBy = 123L)
            props.occupiedBy shouldBe 123L
        }
        
        test("should create with all properties set") {
            val props = CellProperties(
                terrainType = TerrainType.IMPASSABLE,
                hasObstacle = true,
                occupiedBy = 456L
            )
            
            props.terrainType shouldBe TerrainType.IMPASSABLE
            props.hasObstacle shouldBe true
            props.occupiedBy shouldBe 456L
        }
    }
    
    context("CellProperties immutability") {
        test("should be immutable data class") {
            val props1 = CellProperties(hasObstacle = true)
            val props2 = props1.copy(terrainType = TerrainType.DIFFICULT)
            
            props1.terrainType shouldBe TerrainType.NORMAL
            props2.terrainType shouldBe TerrainType.DIFFICULT
            props2.hasObstacle shouldBe true
        }
    }
    
    context("CellProperties serialization") {
        val json = Json { 
            prettyPrint = false
            encodeDefaults = false
        }
        
        test("should serialize default properties") {
            val props = CellProperties()
            val jsonString = json.encodeToString(props)
            
            // With encodeDefaults = false, only non-default values are serialized
            jsonString shouldBe "{}"
        }
        
        test("should serialize with obstacle") {
            val props = CellProperties(hasObstacle = true)
            val jsonString = json.encodeToString(props)
            
            jsonString shouldBe """{"has_obstacle":true}"""
        }
        
        test("should serialize with occupant") {
            val props = CellProperties(occupiedBy = 123L)
            val jsonString = json.encodeToString(props)
            
            jsonString shouldBe """{"occupied_by":123}"""
        }
        
        test("should deserialize from JSON") {
            val jsonString = """{"terrain_type":"difficult","has_obstacle":true,"occupied_by":456}"""
            val props = json.decodeFromString<CellProperties>(jsonString)
            
            props.terrainType shouldBe TerrainType.DIFFICULT
            props.hasObstacle shouldBe true
            props.occupiedBy shouldBe 456L
        }
        
        test("should round-trip serialize and deserialize") {
            val original = CellProperties(
                terrainType = TerrainType.IMPASSABLE,
                hasObstacle = true,
                occupiedBy = 789L
            )
            
            val jsonString = json.encodeToString(original)
            val deserialized = json.decodeFromString<CellProperties>(jsonString)
            
            deserialized shouldBe original
        }
    }
})
