package dev.questweaver.ai.tactical

/**
 * AI difficulty levels that affect decision-making quality.
 *
 * @property variancePercent Percentage of random variance in action scoring
 * @property suboptimalChance Chance of making a suboptimal decision (0.0 to 1.0)
 */
enum class Difficulty(
    val variancePercent: Float,
    val suboptimalChance: Float
) {
    /**
     * Easy difficulty: AI makes suboptimal decisions 30% of the time.
     * Higher variance makes behavior more unpredictable.
     */
    EASY(variancePercent = 0.30f, suboptimalChance = 0.30f),
    
    /**
     * Normal difficulty: AI makes optimal decisions with occasional tactical errors.
     * Moderate variance for some unpredictability.
     */
    NORMAL(variancePercent = 0.15f, suboptimalChance = 0.10f),
    
    /**
     * Hard difficulty: AI makes optimal decisions consistently.
     * Minimal variance for predictable, optimal play.
     */
    HARD(variancePercent = 0.05f, suboptimalChance = 0.0f)
}
