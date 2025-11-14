package dev.questweaver.core.rules.initiative

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Unit tests for SurpriseHandler.
 *
 * Tests verify that:
 * - Surprise round occurs when creatures are surprised
 * - Surprised creatures cannot act in surprise round
 * - Non-surprised creatures can act in surprise round
 * - Surprise condition removed after surprise round
 */
class SurpriseHandlerTest : FunSpec({

    context("Surprise round detection") {
        test("surprise round occurs when creatures are surprised") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(1L, 2L)
            
            handler.hasSurpriseRound(surprisedCreatures).shouldBeTrue()
        }

        test("no surprise round when no creatures are surprised") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = emptySet<Long>()
            
            handler.hasSurpriseRound(surprisedCreatures).shouldBeFalse()
        }

        test("surprise round occurs with single surprised creature") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(5L)
            
            handler.hasSurpriseRound(surprisedCreatures).shouldBeTrue()
        }

        test("surprise round occurs with many surprised creatures") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(1L, 2L, 3L, 4L, 5L)
            
            handler.hasSurpriseRound(surprisedCreatures).shouldBeTrue()
        }
    }

    context("Creature action eligibility in surprise round") {
        test("surprised creature cannot act in surprise round") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(1L, 2L, 3L)
            
            handler.canActInSurpriseRound(1L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(2L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(3L, surprisedCreatures).shouldBeFalse()
        }

        test("non-surprised creature can act in surprise round") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(1L, 2L)
            
            handler.canActInSurpriseRound(3L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(4L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(5L, surprisedCreatures).shouldBeTrue()
        }

        test("mixed group - only non-surprised can act") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(2L, 4L)
            
            // Surprised creatures cannot act
            handler.canActInSurpriseRound(2L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(4L, surprisedCreatures).shouldBeFalse()
            
            // Non-surprised creatures can act
            handler.canActInSurpriseRound(1L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(3L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(5L, surprisedCreatures).shouldBeTrue()
        }

        test("all creatures can act when none are surprised") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = emptySet<Long>()
            
            handler.canActInSurpriseRound(1L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(2L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(3L, surprisedCreatures).shouldBeTrue()
        }
    }

    context("Ending surprise round") {
        test("surprise condition removed after surprise round") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(1L, 2L, 3L)
            
            val afterSurprise = handler.endSurpriseRound()
            
            afterSurprise.shouldBeEmpty()
        }

        test("ending surprise round with empty set returns empty set") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = emptySet<Long>()
            
            val afterSurprise = handler.endSurpriseRound()
            
            afterSurprise.shouldBeEmpty()
        }

        test("ending surprise round with single creature") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(5L)
            
            val afterSurprise = handler.endSurpriseRound()
            
            afterSurprise.shouldBeEmpty()
        }

        test("all creatures can act after surprise round ends") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(1L, 2L, 3L)
            
            // Before ending surprise round
            handler.canActInSurpriseRound(1L, surprisedCreatures).shouldBeFalse()
            
            // After ending surprise round
            val afterSurprise = handler.endSurpriseRound()
            handler.canActInSurpriseRound(1L, afterSurprise).shouldBeTrue()
            handler.canActInSurpriseRound(2L, afterSurprise).shouldBeTrue()
            handler.canActInSurpriseRound(3L, afterSurprise).shouldBeTrue()
        }
    }

    context("Complete surprise round workflow") {
        test("full surprise round lifecycle") {
            val handler = SurpriseHandler()
            
            // Setup: Some creatures are surprised
            val surprisedCreatures = setOf(2L, 4L)
            
            // Phase 1: Detect surprise round
            handler.hasSurpriseRound(surprisedCreatures).shouldBeTrue()
            
            // Phase 2: Check who can act
            handler.canActInSurpriseRound(1L, surprisedCreatures).shouldBeTrue()  // Not surprised
            handler.canActInSurpriseRound(2L, surprisedCreatures).shouldBeFalse() // Surprised
            handler.canActInSurpriseRound(3L, surprisedCreatures).shouldBeTrue()  // Not surprised
            handler.canActInSurpriseRound(4L, surprisedCreatures).shouldBeFalse() // Surprised
            
            // Phase 3: End surprise round
            val afterSurprise = handler.endSurpriseRound()
            
            // Phase 4: Verify no more surprise
            handler.hasSurpriseRound(afterSurprise).shouldBeFalse()
            handler.canActInSurpriseRound(1L, afterSurprise).shouldBeTrue()
            handler.canActInSurpriseRound(2L, afterSurprise).shouldBeTrue()
            handler.canActInSurpriseRound(3L, afterSurprise).shouldBeTrue()
            handler.canActInSurpriseRound(4L, afterSurprise).shouldBeTrue()
        }

        test("ambush scenario - half the party surprised") {
            val handler = SurpriseHandler()
            
            // Party of 4, enemies of 4
            // Party members 1-4, enemies 5-8
            // Party members 1 and 2 are surprised
            val surprisedCreatures = setOf(1L, 2L)
            
            // Surprise round exists
            handler.hasSurpriseRound(surprisedCreatures).shouldBeTrue()
            
            // Surprised party members cannot act
            handler.canActInSurpriseRound(1L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(2L, surprisedCreatures).shouldBeFalse()
            
            // Alert party members can act
            handler.canActInSurpriseRound(3L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(4L, surprisedCreatures).shouldBeTrue()
            
            // All enemies can act (they initiated ambush)
            handler.canActInSurpriseRound(5L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(6L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(7L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(8L, surprisedCreatures).shouldBeTrue()
            
            // After surprise round, everyone can act
            val afterSurprise = handler.endSurpriseRound()
            (1L..8L).forEach { creatureId ->
                handler.canActInSurpriseRound(creatureId, afterSurprise).shouldBeTrue()
            }
        }

        test("total surprise - all enemies surprised") {
            val handler = SurpriseHandler()
            
            // Party successfully surprises all enemies
            val surprisedCreatures = setOf(5L, 6L, 7L, 8L)
            
            handler.hasSurpriseRound(surprisedCreatures).shouldBeTrue()
            
            // Party can act
            handler.canActInSurpriseRound(1L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(2L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(3L, surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(4L, surprisedCreatures).shouldBeTrue()
            
            // Enemies cannot act
            handler.canActInSurpriseRound(5L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(6L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(7L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(8L, surprisedCreatures).shouldBeFalse()
        }
    }

    context("Edge cases") {
        test("creature ID 0 can be surprised") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(0L)
            
            handler.hasSurpriseRound(surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(0L, surprisedCreatures).shouldBeFalse()
        }

        test("large creature IDs work correctly") {
            val handler = SurpriseHandler()
            
            val surprisedCreatures = setOf(999999L, 1000000L)
            
            handler.hasSurpriseRound(surprisedCreatures).shouldBeTrue()
            handler.canActInSurpriseRound(999999L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(1000000L, surprisedCreatures).shouldBeFalse()
            handler.canActInSurpriseRound(1000001L, surprisedCreatures).shouldBeTrue()
        }
    }
})
