package dev.questweaver.ai.ondevice.usecase

import dev.questweaver.ai.ondevice.classifier.IntentClassifier
import dev.questweaver.ai.ondevice.extractor.EntityExtractor
import dev.questweaver.ai.ondevice.model.EncounterContext
import dev.questweaver.core.domain.action.ActionOption
import dev.questweaver.core.domain.action.ActionResult
import dev.questweaver.core.domain.action.NLAction
import dev.questweaver.core.domain.intent.IntentType
import org.slf4j.LoggerFactory

/**
 * Use case for classifying player natural language input into structured actions.
 *
 * This orchestrates the full pipeline:
 * 1. Validates input text
 * 2. Classifies intent using IntentClassifier
 * 3. Extracts entities using EntityExtractor
 * 4. Constructs NLAction from intent + entities
 * 5. Handles disambiguation when entities are missing or ambiguous
 *
 * @param intentClassifier Intent classification component
 * @param entityExtractor Entity extraction component
 */
@Suppress("TooManyFunctions") // Use case requires multiple helper functions for clarity
class IntentClassificationUseCase(
    private val intentClassifier: IntentClassifier,
    private val entityExtractor: EntityExtractor
) {
    private val logger = LoggerFactory.getLogger(IntentClassificationUseCase::class.java)
    
    companion object {
        private const val MAX_INPUT_LENGTH = 500
    }
    
    /**
     * Classifies player input and extracts entities.
     *
     * @param input Player text input
     * @param context Current encounter context
     * @return ActionResult with NLAction, failure, or disambiguation request
     */
    suspend operator fun invoke(
        input: String,
        context: EncounterContext
    ): ActionResult {
        // Validate input
        val validationResult = validateInput(input)
        if (validationResult != null) {
            return validationResult
        }
        
        // Sanitize and truncate input if needed
        val sanitizedInput = sanitizeInput(input)
        
        return classifyAndExtract(sanitizedInput, context)
    }
    
    /**
     * Performs classification and entity extraction.
     */
    private suspend fun classifyAndExtract(
        sanitizedInput: String,
        context: EncounterContext
    ): ActionResult {
        // Classify intent
        val intentResult = intentClassifier.classify(sanitizedInput)
        logger.info { "Classified intent: ${intentResult.intent} (confidence=${intentResult.confidence})" }
        
        // Extract entities
        val entities = entityExtractor.extract(sanitizedInput, context)
        logger.debug { 
            "Extracted entities: ${entities.creatures.size} creatures, " +
            "${entities.locations.size} locations, ${entities.spells.size} spells, " +
            "${entities.items.size} items" 
        }
        
        // Check if required entities are present
        val disambiguationResult = checkRequiredEntities(intentResult.intent, entities, context)
        if (disambiguationResult != null) {
            return disambiguationResult
        }
        
        // Construct and return NLAction
        return createSuccessResult(sanitizedInput, intentResult, entities)
    }
    
    /**
     * Creates a successful ActionResult with NLAction.
     */
    private fun createSuccessResult(
        sanitizedInput: String,
        intentResult: dev.questweaver.ai.ondevice.model.IntentResult,
        entities: dev.questweaver.ai.ondevice.model.EntityExtractionResult
    ): ActionResult.Success {
        val action = NLAction(
            intent = intentResult.intent,
            originalText = sanitizedInput,
            targetCreatureId = entities.creatures.firstOrNull()?.creatureId,
            targetLocation = entities.locations.firstOrNull(),
            spellName = entities.spells.firstOrNull(),
            itemName = entities.items.firstOrNull(),
            confidence = intentResult.confidence
        )
        
        logger.info { "Successfully created NLAction: $action" }
        return ActionResult.Success(action)
    }
    
    /**
     * Validates input text.
     *
     * @return ActionResult.Failure if invalid, null if valid
     */
    private fun validateInput(input: String): ActionResult.Failure? {
        if (input.isBlank()) {
            return ActionResult.Failure("Input cannot be empty")
        }
        
        return null
    }
    
    /**
     * Sanitizes input text by removing invalid characters and truncating if too long.
     */
    private fun sanitizeInput(input: String): String {
        var sanitized = input.trim()
        
        // Truncate if too long
        if (sanitized.length > MAX_INPUT_LENGTH) {
            logger.warn { "Input too long (${sanitized.length} chars), truncating to $MAX_INPUT_LENGTH" }
            sanitized = sanitized.take(MAX_INPUT_LENGTH)
        }
        
        // Remove control characters (keep newlines and tabs)
        sanitized = sanitized.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,!?;:()[]{}\"'-" }
        
        return sanitized
    }
    
    /**
     * Checks if required entities are present for the given intent.
     *
     * @return ActionResult.RequiresChoice if disambiguation needed, null if all required entities present
     */
    private fun checkRequiredEntities(
        intent: IntentType,
        entities: dev.questweaver.ai.ondevice.model.EntityExtractionResult,
        context: EncounterContext
    ): ActionResult.RequiresChoice? {
        return when (intent) {
            IntentType.ATTACK -> checkAttackEntities(entities, context)
            IntentType.MOVE -> checkMoveEntities(entities)
            IntentType.CAST_SPELL -> checkSpellEntities(entities, context)
            IntentType.USE_ITEM -> checkItemEntities(entities, context)
            else -> null
        }
    }
    
    /**
     * Checks if attack intent has required target.
     */
    private fun checkAttackEntities(
        entities: dev.questweaver.ai.ondevice.model.EntityExtractionResult,
        context: EncounterContext
    ): ActionResult.RequiresChoice? {
        if (entities.creatures.isEmpty() && context.creatures.isNotEmpty()) {
            val options = context.creatures.map { creature ->
                ActionOption(
                    description = "Attack ${creature.name}",
                    action = NLAction(
                        intent = IntentType.ATTACK,
                        originalText = "attack ${creature.name}",
                        targetCreatureId = creature.id,
                        targetLocation = null,
                        spellName = null,
                        itemName = null,
                        confidence = 0.5f
                    )
                )
            }
            return ActionResult.RequiresChoice(
                options = options,
                prompt = "Which creature do you want to attack?"
            )
        }
        return null
    }
    
    /**
     * Checks if move intent has required location.
     */
    private fun checkMoveEntities(
        entities: dev.questweaver.ai.ondevice.model.EntityExtractionResult
    ): ActionResult.RequiresChoice? {
        if (entities.locations.isEmpty()) {
            return ActionResult.RequiresChoice(
                options = emptyList(),
                prompt = "Where do you want to move? (e.g., 'E5' or '(5,5)')"
            )
        }
        return null
    }
    
    /**
     * Checks if spell intent has required spell name.
     */
    private fun checkSpellEntities(
        entities: dev.questweaver.ai.ondevice.model.EntityExtractionResult,
        context: EncounterContext
    ): ActionResult.RequiresChoice? {
        return when {
            entities.spells.isEmpty() && context.playerSpells.isNotEmpty() -> {
                createSpellOptions(context.playerSpells, entities)
            }
            entities.spells.size > 1 -> {
                createSpellOptions(entities.spells, entities)
            }
            else -> null
        }
    }
    
    /**
     * Creates spell selection options.
     */
    private fun createSpellOptions(
        spells: List<String>,
        entities: dev.questweaver.ai.ondevice.model.EntityExtractionResult
    ): ActionResult.RequiresChoice {
        val options = spells.map { spell ->
            ActionOption(
                description = "Cast $spell",
                action = NLAction(
                    intent = IntentType.CAST_SPELL,
                    originalText = "cast $spell",
                    targetCreatureId = entities.creatures.firstOrNull()?.creatureId,
                    targetLocation = entities.locations.firstOrNull(),
                    spellName = spell,
                    itemName = null,
                    confidence = 0.5f
                )
            )
        }
        val prompt = if (entities.spells.isEmpty()) {
            "Which spell do you want to cast?"
        } else {
            "Which spell did you mean?"
        }
        return ActionResult.RequiresChoice(options = options, prompt = prompt)
    }
    
    /**
     * Checks if item intent has required item name.
     */
    private fun checkItemEntities(
        entities: dev.questweaver.ai.ondevice.model.EntityExtractionResult,
        context: EncounterContext
    ): ActionResult.RequiresChoice? {
        return when {
            entities.items.isEmpty() && context.playerInventory.isNotEmpty() -> {
                createItemOptions(context.playerInventory, entities)
            }
            entities.items.size > 1 -> {
                createItemOptions(entities.items, entities)
            }
            else -> null
        }
    }
    
    /**
     * Creates item selection options.
     */
    private fun createItemOptions(
        items: List<String>,
        entities: dev.questweaver.ai.ondevice.model.EntityExtractionResult
    ): ActionResult.RequiresChoice {
        val options = items.map { item ->
            ActionOption(
                description = "Use $item",
                action = NLAction(
                    intent = IntentType.USE_ITEM,
                    originalText = "use $item",
                    targetCreatureId = entities.creatures.firstOrNull()?.creatureId,
                    targetLocation = entities.locations.firstOrNull(),
                    spellName = null,
                    itemName = item,
                    confidence = 0.5f
                )
            )
        }
        val prompt = if (entities.items.isEmpty()) {
            "Which item do you want to use?"
        } else {
            "Which item did you mean?"
        }
        return ActionResult.RequiresChoice(options = options, prompt = prompt)
    }
}
