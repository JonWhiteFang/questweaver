package dev.questweaver.core.rules

import kotlin.random.Random

data class AttackOutcome(val total: Int, val hit: Boolean)

class RulesEngine(private val rng: Random = Random(DEFAULT_SEED)) {
    fun rollD20(): Int = rng.nextInt(MIN_D20, MAX_D20_PLUS_ONE)
    fun attack(toHitBonus: Int, targetAC: Int, advantage: Int = 0): AttackOutcome {
        val roll = when {
            advantage > 0 -> maxOf(rollD20(), rollD20())
            advantage < 0 -> minOf(rollD20(), rollD20())
            else -> rollD20()
        }
        val total = roll + toHitBonus
        return AttackOutcome(total, total >= targetAC)
    }

    companion object {
        private const val DEFAULT_SEED = 42L
        private const val MIN_D20 = 1
        private const val MAX_D20_PLUS_ONE = 21
    }
}
