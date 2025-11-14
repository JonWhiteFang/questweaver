package dev.questweaver.data.db.converters

import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.ConditionApplied
import dev.questweaver.domain.events.ConditionRemoved
import dev.questweaver.domain.events.DamageApplied
import dev.questweaver.domain.events.EncounterEnded
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.events.RoundStarted
import dev.questweaver.domain.events.TurnEnded
import dev.questweaver.domain.events.TurnStarted
import dev.questweaver.domain.values.Condition
import dev.questweaver.domain.values.DiceRoll
import dev.questweaver.domain.values.EncounterStatus
import dev.questweaver.domain.values.GridPos
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationException

/**
 * Tests for GameEventConverters type converter.
 * 
 * Verifies serialization and deserialization of all GameEvent subtypes,
 * forward compatibility with unknown keys, and polymorphic type discrimination.
 * 
 * Requirements: 8.5
 */
class GameEventConvertersTest : FunSpec({
    
    val converters = GameEventConverters()
    
    context("AttackResolved serialization") {
        test("should serialize AttackResolved event") {
            val event = AttackResolved(
                sessionId = 1L,
                timestamp = 1000L,
                attackerId = 10L,
                targetId = 20L,
                attackRoll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                targetAC = 18,
                hit = true,
                critical = false
            )
            
            val json = converters.fromGameEvent(event)
            
            json shouldContain "AttackResolved"
            json shouldContain "\"sessionId\":1"
            json shouldContain "\"timestamp\":1000"
            json shouldContain "\"attackerId\":10"
            json shouldContain "\"targetId\":20"
        }
        
        test("should deserialize AttackResolved event") {
            val event = AttackResolved(
                sessionId = 1L,
                timestamp = 1000L,
                attackerId = 10L,
                targetId = 20L,
                attackRoll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                targetAC = 18,
                hit = true,
                critical = false
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
        
        test("should handle critical hit flag") {
            val event = AttackResolved(
                sessionId = 1L,
                timestamp = 1000L,
                attackerId = 10L,
                targetId = 20L,
                attackRoll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 25),
                targetAC = 18,
                hit = true,
                critical = true
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json) as AttackResolved
            
            deserialized.critical shouldBe true
        }
    }
    
    context("DamageApplied serialization") {
        test("should serialize and deserialize DamageApplied event") {
            val event = DamageApplied(
                sessionId = 1L,
                timestamp = 1000L,
                targetId = 20L,
                damageRoll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 11),
                damageAmount = 11,
                hpBefore = 30,
                hpAfter = 19
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
    }
    
    context("ConditionApplied serialization") {
        test("should serialize and deserialize ConditionApplied event") {
            val event = ConditionApplied(
                sessionId = 1L,
                timestamp = 1000L,
                targetId = 20L,
                condition = Condition.PRONE,
                duration = null
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
        
        test("should handle condition with duration") {
            val event = ConditionApplied(
                sessionId = 1L,
                timestamp = 1000L,
                targetId = 20L,
                condition = Condition.STUNNED,
                duration = 3
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json) as ConditionApplied
            
            deserialized.duration shouldBe 3
        }
    }
    
    context("ConditionRemoved serialization") {
        test("should serialize and deserialize ConditionRemoved event") {
            val event = ConditionRemoved(
                sessionId = 1L,
                timestamp = 1000L,
                targetId = 20L,
                condition = Condition.PRONE
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
    }
    
    context("MoveCommitted serialization") {
        test("should serialize and deserialize MoveCommitted event") {
            val event = MoveCommitted(
                sessionId = 1L,
                timestamp = 1000L,
                creatureId = 10L,
                path = listOf(GridPos(0, 0), GridPos(1, 1), GridPos(5, 5)),
                movementUsed = 5,
                movementRemaining = 25
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
        
        test("should preserve path order") {
            val event = MoveCommitted(
                sessionId = 1L,
                timestamp = 1000L,
                creatureId = 10L,
                path = listOf(GridPos(0, 0), GridPos(1, 1), GridPos(2, 2), GridPos(3, 3)),
                movementUsed = 3,
                movementRemaining = 27
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json) as MoveCommitted
            
            deserialized.path shouldBe event.path
        }
    }
    
    context("EncounterStarted serialization") {
        test("should serialize and deserialize EncounterStarted event") {
            val event = EncounterStarted(
                sessionId = 1L,
                timestamp = 1000L,
                encounterId = 100L,
                participants = listOf(10L, 20L, 30L),
                initiativeOrder = emptyList()
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
        
        test("should preserve participant order") {
            val event = EncounterStarted(
                sessionId = 1L,
                timestamp = 1000L,
                encounterId = 100L,
                participants = listOf(30L, 10L, 20L),
                initiativeOrder = emptyList()
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json) as EncounterStarted
            
            deserialized.participants shouldBe event.participants
        }
    }
    
    context("RoundStarted serialization") {
        test("should serialize and deserialize RoundStarted event") {
            val event = RoundStarted(
                sessionId = 1L,
                timestamp = 1000L,
                encounterId = 100L,
                roundNumber = 5
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
    }
    
    context("TurnStarted serialization") {
        test("should serialize and deserialize TurnStarted event") {
            val event = TurnStarted(
                sessionId = 1L,
                timestamp = 1000L,
                encounterId = 100L,
                creatureId = 10L
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
    }
    
    context("TurnEnded serialization") {
        test("should serialize and deserialize TurnEnded event") {
            val event = TurnEnded(
                sessionId = 1L,
                timestamp = 1000L,
                encounterId = 100L,
                creatureId = 10L
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
    }
    
    context("EncounterEnded serialization") {
        test("should serialize and deserialize EncounterEnded event") {
            val event = EncounterEnded(
                sessionId = 1L,
                timestamp = 1000L,
                encounterId = 100L,
                status = EncounterStatus.VICTORY
            )
            
            val json = converters.fromGameEvent(event)
            val deserialized = converters.toGameEvent(json)
            
            deserialized shouldBe event
        }
        
        test("should handle different encounter statuses") {
            val statuses = listOf(
                EncounterStatus.VICTORY,
                EncounterStatus.DEFEAT,
                EncounterStatus.FLED
            )
            
            statuses.forEach { status ->
                val event = EncounterEnded(
                    sessionId = 1L,
                    timestamp = 1000L,
                    encounterId = 100L,
                    status = status
                )
                
                val json = converters.fromGameEvent(event)
                val deserialized = converters.toGameEvent(json) as EncounterEnded
                
                deserialized.status shouldBe status
            }
        }
    }
    
    context("polymorphic type discrimination") {
        test("should correctly identify event type after deserialization") {
            val attackEvent: GameEvent = AttackResolved(
                sessionId = 1L,
                timestamp = 1000L,
                attackerId = 10L,
                targetId = 20L,
                attackRoll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                targetAC = 18,
                hit = true,
                critical = false
            )
            
            val json = converters.fromGameEvent(attackEvent)
            val deserialized = converters.toGameEvent(json)
            
            deserialized.shouldBeInstanceOf<AttackResolved>()
        }
        
        test("should distinguish between different event types") {
            val events: List<GameEvent> = listOf(
                AttackResolved(
                    1L, 1000L, 10L, 20L,
                    DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                    18, true, false
                ),
                MoveCommitted(1L, 2000L, 10L, listOf(GridPos(0, 0), GridPos(5, 5)), 5, 25),
                EncounterStarted(1L, 3000L, 100L, emptyList(), emptyList())
            )
            
            events.forEach { event ->
                val json = converters.fromGameEvent(event)
                val deserialized = converters.toGameEvent(json)
                
                deserialized::class shouldBe event::class
            }
        }
    }
    
    context("forward compatibility with unknown keys") {
        test("should ignore unknown JSON keys during deserialization") {
            // Simulate future version with additional field
            val jsonWithExtraField = """
                {
                    "type":"dev.questweaver.domain.events.AttackResolved",
                    "sessionId":1,
                    "timestamp":1000,
                    "attackerId":10,
                    "targetId":20,
                    "attackRoll":{
                        "diceType":20,"count":1,"modifier":5,"result":20
                    },
                    "targetAC":18,
                    "hit":true,
                    "critical":false,
                    "futureField":"someValue"
                }
            """.trimIndent()
            
            val deserialized = converters.toGameEvent(jsonWithExtraField)
            
            deserialized.shouldBeInstanceOf<AttackResolved>()
            deserialized.sessionId shouldBe 1L
            deserialized.timestamp shouldBe 1000L
        }
        
        test("should handle missing optional fields") {
            // JSON without critical field (defaults to false)
            val jsonWithoutOptional = """
                {
                    "type":"dev.questweaver.domain.events.AttackResolved",
                    "sessionId":1,
                    "timestamp":1000,
                    "attackerId":10,
                    "targetId":20,
                    "attackRoll":{"diceType":20,"count":1,"modifier":5,"result":20},
                    "targetAC":18,
                    "hit":true
                }
            """.trimIndent()
            
            val deserialized = converters.toGameEvent(jsonWithoutOptional) as AttackResolved
            
            deserialized.critical shouldBe false
        }
    }
    
    context("error handling") {
        test("should throw SerializationException for invalid JSON") {
            val invalidJson = """{"invalid": "json", "missing": "type"}"""
            
            shouldThrow<SerializationException> {
                converters.toGameEvent(invalidJson)
            }
        }
        
        test("should throw SerializationException for malformed JSON") {
            val malformedJson = """{"type":"AttackResolved", "sessionId":}"""
            
            shouldThrow<SerializationException> {
                converters.toGameEvent(malformedJson)
            }
        }
        
        test("should provide helpful error message on serialization failure") {
            val invalidJson = """{"invalid": "structure"}"""
            
            val exception = shouldThrow<SerializationException> {
                converters.toGameEvent(invalidJson)
            }
            
            exception.message shouldContain "Failed to deserialize GameEvent"
        }
    }
    
    context("round-trip serialization") {
        test("should maintain data integrity through multiple serialization cycles") {
            val originalEvent = AttackResolved(
                sessionId = 1L,
                timestamp = 1000L,
                attackerId = 10L,
                targetId = 20L,
                attackRoll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                targetAC = 18,
                hit = true,
                critical = false
            )
            
            // Serialize and deserialize multiple times
            var event: GameEvent = originalEvent
            repeat(3) {
                val json = converters.fromGameEvent(event)
                event = converters.toGameEvent(json)
            }
            
            event shouldBe originalEvent
        }
        
        test("should handle all event types in round-trip") {
            val events: List<GameEvent> = listOf(
                AttackResolved(
                    1L, 1000L, 10L, 20L,
                    DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                    18, true, false
                ),
                DamageApplied(
                    1L, 1000L, 20L,
                    DiceRoll(diceType = 8, count = 1, modifier = 3, result = 11),
                    11, 30, 19
                ),
                ConditionApplied(1L, 1000L, 20L, Condition.PRONE, null),
                ConditionRemoved(1L, 1000L, 20L, Condition.PRONE),
                MoveCommitted(1L, 1000L, 10L, listOf(GridPos(0, 0), GridPos(5, 5)), 5, 25),
                EncounterStarted(1L, 1000L, 100L, emptyList(), emptyList()),
                RoundStarted(1L, 1000L, 100L, 1),
                TurnStarted(1L, 1000L, 100L, 10L),
                TurnEnded(1L, 1000L, 100L, 10L),
                EncounterEnded(1L, 1000L, 100L, EncounterStatus.VICTORY)
            )
            
            events.forEach { originalEvent ->
                val json = converters.fromGameEvent(originalEvent)
                val deserialized = converters.toGameEvent(json)
                
                deserialized shouldBe originalEvent
            }
        }
    }
})
