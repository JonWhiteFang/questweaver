package dev.questweaver.core.rules

import kotlin.random.Random

data class AttackOutcome(val total: Int, val hit: Boolean)

class RulesEngine(private val rng: Random = Random(42)) {
    fun rollD20(): Int = rng.nextInt(1, 21)
    fun attack(toHitBonus: Int, targetAC: Int, advantage: Int = 0): AttackOutcome {
        val roll = when {
            advantage > 0 -> maxOf(rollD20(), rollD20())
            advantage < 0 -> minOf(rollD20(), rollD20())
            else -> rollD20()
        }
        val total = roll + toHitBonus
        return AttackOutcome(total, total >= targetAC)
    }
}
