package dev.questweaver.ai.ondevice.usecase

import dev.questweaver.ai.ondevice.classifier.IntentClassifier
import dev.questweaver.ai.ondevice.extractor.EntityExtractor
import dev.questweaver.ai.ondevice.model.CreatureInfo
import dev.questweaver.ai.ondevice.model.EncounterContext
import dev.questweaver.ai.ondevice.model.EntityExtractionResult
import dev.questweaver.ai.ondevice.model.ExtractedCreature
import dev.questweaver.ai.ondevice.model.IntentResult
import dev.questweaver.core.domain.action.ActionResult
import dev.questweaver.core.domain.intent.IntentType
import dev.questweaver.core.domain.map.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for IntentClassificationUseCase.
 */
class IntentClassificationUseCaseTest : FunSpec({
    
    context("successful classification") {
        test("should create NLAction with all entities") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            coEvery { intentClassifier.classify(any()) } returns IntentResult(
                intent = IntentType.ATTACK,
                confidence = 0.95f,
                usedFallback = false
            )
            
            every { entityExtractor.extract(any(), any()) } returns EntityExtractionResult(
                creatures = listOf(
                    ExtractedCreature(
                        creatureId = 1,
                        name = "Goblin",
                        matchedText = "goblin",
                        startIndex = 11,
                        endIndex = 17
                    )
                ),
                locations = emptyList(),
                spells = emptyList(),
                items = emptyList()
            )
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = listOf(CreatureInfo(id = 1, name = "Goblin")),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("attack the goblin", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Success>()
            val action = (result as ActionResult.Success).action
            action.intent shouldBe IntentType.ATTACK
            action.targetCreatureId shouldBe 1
            action.confidence shouldBe 0.95f
        }
    }
    
    context("disambiguation") {
        test("should request disambiguation when target missing") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            coEvery { intentClassifier.classify(any()) } returns IntentResult(
                intent = IntentType.ATTACK,
                confidence = 0.95f,
                usedFallback = false
            )
            
            every { entityExtractor.extract(any(), any()) } returns EntityExtractionResult(
                creatures = emptyList(), // No creatures extracted
                locations = emptyList(),
                spells = emptyList(),
                items = emptyList()
            )
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin"),
                    CreatureInfo(id = 2, name = "Orc")
                ),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("attack", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.RequiresChoice>()
            val choice = result as ActionResult.RequiresChoice
            choice.options.size shouldBe 2
            choice.prompt shouldBe "Which creature do you want to attack?"
        }
        
        test("should request disambiguation when spell missing") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            coEvery { intentClassifier.classify(any()) } returns IntentResult(
                intent = IntentType.CAST_SPELL,
                confidence = 0.95f,
                usedFallback = false
            )
            
            every { entityExtractor.extract(any(), any()) } returns EntityExtractionResult(
                creatures = emptyList(),
                locations = emptyList(),
                spells = emptyList(), // No spells extracted
                items = emptyList()
            )
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = listOf("Fire Bolt", "Magic Missile"),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("cast spell", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.RequiresChoice>()
            val choice = result as ActionResult.RequiresChoice
            choice.options.size shouldBe 2
            choice.prompt shouldBe "Which spell do you want to cast?"
        }
        
        test("should request disambiguation when location missing") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            coEvery { intentClassifier.classify(any()) } returns IntentResult(
                intent = IntentType.MOVE,
                confidence = 0.95f,
                usedFallback = false
            )
            
            every { entityExtractor.extract(any(), any()) } returns EntityExtractionResult(
                creatures = emptyList(),
                locations = emptyList(), // No locations extracted
                spells = emptyList(),
                items = emptyList()
            )
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("move", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.RequiresChoice>()
            val choice = result as ActionResult.RequiresChoice
            choice.prompt shouldBe "Where do you want to move? (e.g., 'E5' or '(5,5)')"
        }
    }
    
    context("input validation") {
        test("should reject empty input") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Failure>()
            (result as ActionResult.Failure).reason shouldBe "Input cannot be empty"
        }
        
        test("should reject blank input") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("   ", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Failure>()
            (result as ActionResult.Failure).reason shouldBe "Input cannot be empty"
        }
    }
    
    context("error handling") {
        test("should handle classification exception") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            coEvery { intentClassifier.classify(any()) } throws RuntimeException("Classification error")
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("attack", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Failure>()
        }
    }
})
