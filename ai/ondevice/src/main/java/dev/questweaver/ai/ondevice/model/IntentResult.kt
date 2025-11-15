package dev.questweaver.ai.ondevice.model

import dev.questweaver.core.domain.intent.IntentType

/**
 * Result of intent classification.
 * 
 * @property intent The classified intent type
 * @property confidence The confidence score of the classification (0.0-1.0)
 * @property usedFallback Whether keyword fallback was used instead of ML model
 */
data class IntentResult(
    val intent: IntentType,
    val confidence: Float,
    val usedFallback: Boolean
) {
    init {
        require(confidence in 0.0f..1.0f) { "Confidence must be between 0.0 and 1.0" }
    }
}
