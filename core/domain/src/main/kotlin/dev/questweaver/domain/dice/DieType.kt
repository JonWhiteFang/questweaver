package dev.questweaver.domain.dice

import kotlin.random.Random

/**
 * Standard D&D dice types.
 *
 * Represents the common polyhedral dice used in D&D 5e SRD mechanics.
 * Each die type knows its number of sides and can roll itself using a
 * provided random number generator.
 */
@Suppress("MagicNumber") // Die sides are domain constants
enum class DieType(val sides: Int) {
    D4(4),
    D6(6),
    D8(8),
    D10(10),
    D12(12),
    D20(20),
    D100(100);

    /**
     * Roll this die type using the provided random generator.
     *
     * @param random The seeded random number generator to use
     * @return A value between 1 and [sides] inclusive
     */
    internal fun roll(random: Random): Int = random.nextInt(1, sides + 1)
}
