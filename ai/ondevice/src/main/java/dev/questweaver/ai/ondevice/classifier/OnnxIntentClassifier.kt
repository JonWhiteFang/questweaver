package dev.questweaver.ai.ondevice.classifier

import dev.questweaver.ai.ondevice.fallback.KeywordFallback
import dev.questweaver.ai.ondevice.inference.OnnxSessionManager
import dev.questweaver.ai.ondevice.model.IntentResult
import dev.questweaver.ai.ondevice.tokenizer.Tokenizer
import dev.questweaver.core.domain.intent.IntentType
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

/**
 * ONNX-based intent classifier with keyword fallback.
 *
 * This implementation:
 * 1. Tokenizes input text
 * 2. Runs ONNX model inference
 * 3. Checks confidence threshold
 * 4. Falls back to keyword matching if confidence too low or inference fails
 *
 * @param sessionManager ONNX Runtime session manager
 * @param tokenizer Text tokenizer
 * @param keywordFallback Keyword-based fallback classifier
 * @param confidenceThreshold Minimum confidence for ONNX classification (default 0.6)
 * @param timeoutMs Maximum time for inference in milliseconds (default 300)
 */
class OnnxIntentClassifier(
    private val sessionManager: OnnxSessionManager,
    private val tokenizer: Tokenizer,
    private val keywordFallback: KeywordFallback,
    private val confidenceThreshold: Float = 0.6f,
    private val timeoutMs: Long = 300L
) : IntentClassifier {
    
    private val logger = LoggerFactory.getLogger(OnnxIntentClassifier::class.java)
    
    companion object {
        private const val EXPECTED_PROBABILITY_COUNT = 12
        private const val INTENT_INDEX_ATTACK = 0
        private const val INTENT_INDEX_MOVE = 1
        private const val INTENT_INDEX_CAST_SPELL = 2
        private const val INTENT_INDEX_USE_ITEM = 3
        private const val INTENT_INDEX_DASH = 4
        private const val INTENT_INDEX_DODGE = 5
        private const val INTENT_INDEX_HELP = 6
        private const val INTENT_INDEX_HIDE = 7
        private const val INTENT_INDEX_DISENGAGE = 8
        private const val INTENT_INDEX_READY = 9
        private const val INTENT_INDEX_SEARCH = 10
        private const val INTENT_INDEX_UNKNOWN = 11
    }
    
    /**
     * Classifies player text input into an intent type.
     *
     * @param text Player input text
     * @return IntentResult with intent type, confidence, and fallback flag
     */
    override suspend fun classify(text: String): IntentResult {
        // Check if ONNX session is ready
        if (!sessionManager.isReady()) {
            logger.warn { "ONNX session not ready, using keyword fallback" }
            return keywordFallback.classify(text)
        }
        
        return try {
            // Apply timeout to prevent blocking
            withTimeout(timeoutMs) {
                classifyWithOnnx(text)
            }
        } catch (e: IllegalStateException) {
            logger.warn(e) { "ONNX classification failed due to invalid state, using keyword fallback" }
            keywordFallback.classify(text)
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "ONNX classification failed due to invalid argument, using keyword fallback" }
            keywordFallback.classify(text)
        }
    }
    
    /**
     * Performs ONNX-based classification.
     */
    private suspend fun classifyWithOnnx(text: String): IntentResult {
        // Tokenize input
        val tokens = tokenizer.tokenize(text)
        logger.debug { "Tokenized input: ${tokens.size} tokens" }
        
        // Run ONNX inference
        val probabilities = sessionManager.infer(tokens)
        logger.debug { "Inference complete: ${probabilities.size} probabilities" }
        
        // Find highest confidence intent
        val (intent, confidence) = findBestIntent(probabilities)
        logger.debug { "Best intent: $intent (confidence=$confidence)" }
        
        // Check confidence threshold
        if (confidence >= confidenceThreshold) {
            return IntentResult(
                intent = intent,
                confidence = confidence,
                usedFallback = false
            )
        } else {
            logger.debug { "Confidence $confidence below threshold $confidenceThreshold, using fallback" }
            return keywordFallback.classify(text)
        }
    }
    
    /**
     * Finds the intent with the highest probability.
     *
     * @param probabilities Array of intent probabilities (length 12)
     * @return Pair of (IntentType, confidence)
     */
    private fun findBestIntent(probabilities: FloatArray): Pair<IntentType, Float> {
        require(probabilities.size == EXPECTED_PROBABILITY_COUNT) { 
            "Expected $EXPECTED_PROBABILITY_COUNT probabilities, got ${probabilities.size}" 
        }
        
        // Find index of maximum probability
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val maxConfidence = probabilities[maxIndex]
        
        // Map index to IntentType
        val intent = indexToIntent(maxIndex)
        
        return Pair(intent, maxConfidence)
    }
    
    /**
     * Maps probability array index to IntentType.
     *
     * Index mapping (must match model training):
     * 0 = ATTACK
     * 1 = MOVE
     * 2 = CAST_SPELL
     * 3 = USE_ITEM
     * 4 = DASH
     * 5 = DODGE
     * 6 = HELP
     * 7 = HIDE
     * 8 = DISENGAGE
     * 9 = READY
     * 10 = SEARCH
     * 11 = UNKNOWN
     */
    private fun indexToIntent(index: Int): IntentType = when (index) {
        INTENT_INDEX_ATTACK -> IntentType.ATTACK
        INTENT_INDEX_MOVE -> IntentType.MOVE
        INTENT_INDEX_CAST_SPELL -> IntentType.CAST_SPELL
        INTENT_INDEX_USE_ITEM -> IntentType.USE_ITEM
        INTENT_INDEX_DASH -> IntentType.DASH
        INTENT_INDEX_DODGE -> IntentType.DODGE
        INTENT_INDEX_HELP -> IntentType.HELP
        INTENT_INDEX_HIDE -> IntentType.HIDE
        INTENT_INDEX_DISENGAGE -> IntentType.DISENGAGE
        INTENT_INDEX_READY -> IntentType.READY
        INTENT_INDEX_SEARCH -> IntentType.SEARCH
        INTENT_INDEX_UNKNOWN -> IntentType.UNKNOWN
        else -> {
            logger.warn { "Invalid intent index: $index, defaulting to UNKNOWN" }
            IntentType.UNKNOWN
        }
    }
}
