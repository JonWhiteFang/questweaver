package dev.questweaver.ai.ondevice.extractor

import dev.questweaver.ai.ondevice.model.CreatureInfo
import dev.questweaver.ai.ondevice.model.EncounterContext
import dev.questweaver.core.domain.map.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Unit tests for EntityExtractor.
 */
class EntityExtractorTest : FunSpec({
    
    context("creature extraction") {
        test("should extract creature by exact name") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin Archer")
                ),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("attack the Goblin Archer", context)
            
            // Assert
            result.creatures shouldHaveSize 1
            result.creatures.first().creatureId shouldBe 1
            result.creatures.first().name shouldBe "Goblin Archer"
        }
        
        test("should extract creature by partial name") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin Archer"),
                    CreatureInfo(id = 2, name = "Goblin Warrior")
                ),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("attack the goblin", context)
            
            // Assert
            // Should match both creatures that contain "goblin"
            result.creatures.size shouldBe 2
        }
        
        test("should handle case-insensitive matching") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin Archer")
                ),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("ATTACK THE GOBLIN ARCHER", context)
            
            // Assert
            result.creatures shouldHaveSize 1
            result.creatures.first().creatureId shouldBe 1
        }
        
        test("should prefer longest match first") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin"),
                    CreatureInfo(id = 2, name = "Goblin Archer")
                ),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("attack the Goblin Archer", context)
            
            // Assert
            // Should match "Goblin Archer" (longer) not just "Goblin"
            result.creatures shouldHaveSize 1
            result.creatures.first().creatureId shouldBe 2
            result.creatures.first().name shouldBe "Goblin Archer"
        }
    }
    
    context("location extraction") {
        test("should extract location from grid notation") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("move to E5", context)
            
            // Assert
            result.locations shouldHaveSize 1
            result.locations.first() shouldBe GridPos(4, 4) // E=4, 5=4 (0-indexed)
        }
        
        test("should extract location from coordinate notation") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("move to (5,5)", context)
            
            // Assert
            result.locations shouldHaveSize 1
            result.locations.first() shouldBe GridPos(5, 5)
        }
        
        test("should handle coordinate notation with spaces") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("move to (10, 3)", context)
            
            // Assert
            result.locations shouldHaveSize 1
            result.locations.first() shouldBe GridPos(10, 3)
        }
        
        test("should extract multiple locations") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("move from A1 to E5", context)
            
            // Assert
            result.locations shouldHaveSize 2
            result.locations shouldContain GridPos(0, 0) // A1
            result.locations shouldContain GridPos(4, 4) // E5
        }
    }
    
    context("spell extraction") {
        test("should extract spell name") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = listOf("Fire Bolt", "Magic Missile"),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("cast fire bolt at the goblin", context)
            
            // Assert
            result.spells shouldHaveSize 1
            result.spells.first() shouldBe "Fire Bolt"
        }
        
        test("should handle case-insensitive spell matching") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = listOf("Fireball"),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("CAST FIREBALL", context)
            
            // Assert
            result.spells shouldHaveSize 1
            result.spells.first() shouldBe "Fireball"
        }
        
        test("should not match partial spell names") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = listOf("Fire Bolt"),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("cast fire", context)
            
            // Assert
            // Should not match "Fire Bolt" from just "fire"
            result.spells shouldHaveSize 0
        }
    }
    
    context("item extraction") {
        test("should extract item name") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = listOf("Potion of Healing", "Rope")
            )
            
            // Act
            val result = extractor.extract("use potion of healing", context)
            
            // Assert
            result.items shouldHaveSize 1
            result.items.first() shouldBe "Potion of Healing"
        }
        
        test("should match item by first word") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = listOf("Potion of Healing")
            )
            
            // Act
            val result = extractor.extract("drink a potion", context)
            
            // Assert
            result.items shouldHaveSize 1
            result.items.first() shouldBe "Potion of Healing"
        }
        
        test("should handle case-insensitive item matching") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = listOf("Rope")
            )
            
            // Act
            val result = extractor.extract("USE THE ROPE", context)
            
            // Assert
            result.items shouldHaveSize 1
            result.items.first() shouldBe "Rope"
        }
    }
    
    context("error handling") {
        test("should return empty result on exception") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act - pass null or invalid input that might cause issues
            val result = extractor.extract("", context)
            
            // Assert - should not throw, returns empty result
            result.creatures shouldHaveSize 0
            result.locations shouldHaveSize 0
            result.spells shouldHaveSize 0
            result.items shouldHaveSize 0
        }
    }
    
    context("combined extraction") {
        test("should extract multiple entity types") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin")
                ),
                playerSpells = listOf("Fire Bolt"),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("cast fire bolt at the goblin at E5", context)
            
            // Assert
            result.creatures shouldHaveSize 1
            result.creatures.first().name shouldBe "Goblin"
            result.spells shouldHaveSize 1
            result.spells.first() shouldBe "Fire Bolt"
            result.locations shouldHaveSize 1
            result.locations.first() shouldBe GridPos(4, 4)
        }
    }
})
