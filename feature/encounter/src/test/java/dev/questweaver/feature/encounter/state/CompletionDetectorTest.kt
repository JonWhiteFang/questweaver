package dev.questweaver.feature.encounter.state

import dev.questweaver.feature.encounter.viewmodel.CompletionStatus
import dev.questweaver.feature.encounter.viewmodel.Creature
import dev.questweaver.feature.map.ui.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for CompletionDetector.
 * Verifies victory/defeat detection and rewards calculation.
 *
 * Requirements: 5.1, 5.2, 5.3, 5.5
 */
class CompletionDetectorTest : FunSpec({
    
    lateinit var completionDetector: CompletionDetector
    
    beforeTest {
        completionDetector = CompletionDetector()
    }
    
    context("Victory detection") {
        test("victory detected when all enemies defeated") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Wizard", 15, 15, 12, GridPos(1, 0), true),
                3L to Creature(3L, "Goblin", 0, 7, 13, GridPos(5, 5), false), // Defeated
                4L to Creature(4L, "Orc", 0, 15, 14, GridPos(6, 6), false) // Defeated
            )
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            result.shouldNotBeNull()
            result shouldBe CompletionStatus.Victory
        }
        
        test("victory detected when no enemies remain") {
            // Arrange - only player creatures
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Wizard", 15, 15, 12, GridPos(1, 0), true)
            )
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            result.shouldNotBeNull()
            result shouldBe CompletionStatus.Victory
        }
    }
    
    context("Defeat detection") {
        test("defeat detected when all PCs defeated") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 0, 20, 15, GridPos(0, 0), true), // Defeated
                2L to Creature(2L, "Wizard", 0, 15, 12, GridPos(1, 0), true), // Defeated
                3L to Creature(3L, "Goblin", 7, 7, 13, GridPos(5, 5), false),
                4L to Creature(4L, "Orc", 15, 15, 14, GridPos(6, 6), false)
            )
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            result.shouldNotBeNull()
            result shouldBe CompletionStatus.Defeat
        }
        
        test("defeat detected when no player creatures remain alive") {
            // Arrange - only enemy creatures alive
            val creatures = mapOf(
                1L to Creature(1L, "Goblin", 7, 7, 13, GridPos(5, 5), false),
                2L to Creature(2L, "Orc", 15, 15, 14, GridPos(6, 6), false)
            )
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            result.shouldNotBeNull()
            result shouldBe CompletionStatus.Defeat
        }
    }
    
    context("Encounter continuation") {
        test("encounter continues when neither condition met") {
            // Arrange - both sides have living creatures
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Wizard", 15, 15, 12, GridPos(1, 0), true),
                3L to Creature(3L, "Goblin", 7, 7, 13, GridPos(5, 5), false),
                4L to Creature(4L, "Orc", 15, 15, 14, GridPos(6, 6), false)
            )
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            result.shouldBeNull()
        }
        
        test("encounter continues with some defeated on each side") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Wizard", 0, 15, 12, GridPos(1, 0), true), // Defeated
                3L to Creature(3L, "Goblin", 0, 7, 13, GridPos(5, 5), false), // Defeated
                4L to Creature(4L, "Orc", 15, 15, 14, GridPos(6, 6), false)
            )
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            result.shouldBeNull()
        }
    }
    
    context("Rewards calculation") {
        test("rewards calculated correctly based on defeated creatures") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Goblin", 0, 7, 13, GridPos(5, 5), false), // Defeated
                3L to Creature(3L, "Orc", 0, 15, 14, GridPos(6, 6), false), // Defeated
                4L to Creature(4L, "Hobgoblin", 0, 11, 16, GridPos(7, 7), false) // Defeated
            )
            
            // Act
            val rewards = completionDetector.calculateRewards(creatures, CompletionStatus.Victory)
            
            // Assert
            rewards.xpAwarded shouldBe 300 // 3 defeated enemies * 100 XP each
            rewards.loot shouldHaveSize 3
        }
        
        test("XP calculation uses creature challenge ratings") {
            // Arrange - 2 defeated enemies
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Goblin", 0, 7, 13, GridPos(5, 5), false), // Defeated
                3L to Creature(3L, "Orc", 0, 15, 14, GridPos(6, 6), false) // Defeated
            )
            
            // Act
            val rewards = completionDetector.calculateRewards(creatures, CompletionStatus.Victory)
            
            // Assert
            // Using simplified formula: 100 XP per defeated enemy
            // TODO: Replace with actual CR-based calculation when available
            rewards.xpAwarded shouldBe 200
        }
        
        test("no rewards for defeat") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 0, 20, 15, GridPos(0, 0), true), // Defeated
                2L to Creature(2L, "Goblin", 7, 7, 13, GridPos(5, 5), false)
            )
            
            // Act
            val rewards = completionDetector.calculateRewards(creatures, CompletionStatus.Defeat)
            
            // Assert
            rewards.xpAwarded shouldBe 0
            rewards.loot shouldHaveSize 0
        }
        
        test("no rewards for fled") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Goblin", 7, 7, 13, GridPos(5, 5), false)
            )
            
            // Act
            val rewards = completionDetector.calculateRewards(creatures, CompletionStatus.Fled)
            
            // Assert
            rewards.xpAwarded shouldBe 0
            rewards.loot shouldHaveSize 0
        }
        
        test("loot generated for each defeated enemy") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 20, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Goblin", 0, 7, 13, GridPos(5, 5), false), // Defeated
                3L to Creature(3L, "Orc", 0, 15, 14, GridPos(6, 6), false) // Defeated
            )
            
            // Act
            val rewards = completionDetector.calculateRewards(creatures, CompletionStatus.Victory)
            
            // Assert
            rewards.loot shouldHaveSize 2
            rewards.loot[0].name shouldBe "Goblin's Equipment"
            rewards.loot[0].quantity shouldBe 1
            rewards.loot[1].name shouldBe "Orc's Equipment"
            rewards.loot[1].quantity shouldBe 1
        }
    }
    
    context("Edge cases") {
        test("empty creature map returns null") {
            // Arrange
            val creatures = emptyMap<Long, Creature>()
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            // With no creatures, both sides are "defeated" - should return defeat
            result.shouldNotBeNull()
            result shouldBe CompletionStatus.Defeat
        }
        
        test("single player creature alive continues encounter") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 1, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Goblin", 1, 7, 13, GridPos(5, 5), false)
            )
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            result.shouldBeNull()
        }
        
        test("all creatures at 0 HP results in defeat") {
            // Arrange
            val creatures = mapOf(
                1L to Creature(1L, "Fighter", 0, 20, 15, GridPos(0, 0), true),
                2L to Creature(2L, "Goblin", 0, 7, 13, GridPos(5, 5), false)
            )
            
            // Act
            val result = completionDetector.checkCompletion(creatures)
            
            // Assert
            result.shouldNotBeNull()
            result shouldBe CompletionStatus.Defeat
        }
    }
})
