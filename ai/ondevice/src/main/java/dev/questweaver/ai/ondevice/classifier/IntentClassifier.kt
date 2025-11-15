package dev.questweaver.ai.ondevice.classifier

import dev.questweaver.ai.ondevice.model.IntentResult

/**
 * Interface for intent classification.
 *
 * Implementations classify player text input into structured intent types
 * (e.g., ATTACK, MOVE, CAST_SPELL) with confidence scores.
 */
interface IntentClassifier {
    /**
     * Classifies player text input into an intent type.
     *
     * @param text Player input text
     * @return IntentResult with intent type, confidence, and fallback flag
     */
    suspend fun classify(text: String): IntentResult
}
