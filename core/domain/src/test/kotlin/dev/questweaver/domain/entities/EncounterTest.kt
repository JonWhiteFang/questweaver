package dev.questweaver.domain.entities

import dev.questweaver.domain.values.EncounterStatus
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class EncounterTest : FunSpec({
    
    context("Encounter validation") {
        test("should create Encounter with valid values") {
            val encounter = Encounter(
                id = 1,
                campaignId = 5,
                createdTimestamp = 1000,
                currentRound = 1,
                activeCreatureId = 10,
                participants = listOf(10, 20, 30),
                initiativeOrder = listOf(
                    InitiativeEntryData(10, 18, 0, 18),
                    InitiativeEntryData(20, 15, 0, 15),
                    InitiativeEntryData(30, 12, 0, 12)
                )
            )
            
            encounter.id shouldBe 1
            encounter.campaignId shouldBe 5
            encounter.createdTimestamp shouldBe 1000
            encounter.currentRound shouldBe 1
            encounter.activeCreatureId shouldBe 10
            encounter.participants shouldBe listOf(10, 20, 30)
            encounter.status shouldBe EncounterStatus.IN_PROGRESS
        }
        
        test("should create Encounter with null active creature") {
            shouldNotThrowAny {
                Encounter(
                    id = 1,
                    campaignId = 5,
                    createdTimestamp = 1000,
                    currentRound = 1,
                    activeCreatureId = null,
                    participants = listOf(10, 20),
                    initiativeOrder = listOf(
                        InitiativeEntryData(10, 18, 0, 18),
                        InitiativeEntryData(20, 15, 0, 15)
                    )
                )
            }
        }
        
        test("should create Encounter with custom status") {
            val encounter = Encounter(
                id = 1,
                campaignId = 5,
                createdTimestamp = 1000,
                currentRound = 5,
                activeCreatureId = null,
                participants = listOf(10),
                initiativeOrder = listOf(InitiativeEntryData(10, 18, 0, 18)),
                status = EncounterStatus.VICTORY
            )
            
            encounter.status shouldBe EncounterStatus.VICTORY
        }
        
        test("should throw exception for non-positive id") {
            shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 0,
                    campaignId = 5,
                    createdTimestamp = 1000,
                    currentRound = 1,
                    activeCreatureId = 10,
                    participants = listOf(10),
                    initiativeOrder = listOf(InitiativeEntryData(10, 18, 0, 18))
                )
            }
        }
        
        test("should throw exception for non-positive campaign id") {
            shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 1,
                    campaignId = 0,
                    createdTimestamp = 1000,
                    currentRound = 1,
                    activeCreatureId = 10,
                    participants = listOf(10),
                    initiativeOrder = listOf(InitiativeEntryData(10, 18, 0, 18))
                )
            }
        }
        
        test("should throw exception for non-positive created timestamp") {
            shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 1,
                    campaignId = 5,
                    createdTimestamp = 0,
                    currentRound = 1,
                    activeCreatureId = 10,
                    participants = listOf(10),
                    initiativeOrder = listOf(InitiativeEntryData(10, 18, 0, 18))
                )
            }
        }
        
        test("should throw exception for round less than 1") {
            shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 1,
                    campaignId = 5,
                    createdTimestamp = 1000,
                    currentRound = 0,
                    activeCreatureId = 10,
                    participants = listOf(10),
                    initiativeOrder = listOf(InitiativeEntryData(10, 18, 0, 18))
                )
            }
        }
        
        test("should throw exception for empty participants") {
            shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 1,
                    campaignId = 5,
                    createdTimestamp = 1000,
                    currentRound = 1,
                    activeCreatureId = null,
                    participants = emptyList(),
                    initiativeOrder = emptyList()
                )
            }
        }
        
        test("should throw exception when active creature is not a participant") {
            val exception = shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 1,
                    campaignId = 5,
                    createdTimestamp = 1000,
                    currentRound = 1,
                    activeCreatureId = 99,
                    participants = listOf(10, 20),
                    initiativeOrder = listOf(
                        InitiativeEntryData(10, 18, 0, 18),
                        InitiativeEntryData(20, 15, 0, 15)
                    )
                )
            }
            
            exception.message shouldContain "Active creature"
            exception.message shouldContain "must be a participant"
        }
        
        test("should throw exception when initiative order is missing a participant") {
            val exception = shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 1,
                    campaignId = 5,
                    createdTimestamp = 1000,
                    currentRound = 1,
                    activeCreatureId = 10,
                    participants = listOf(10, 20, 30),
                    initiativeOrder = listOf(
                        InitiativeEntryData(10, 18, 0, 18),
                        InitiativeEntryData(20, 15, 0, 15)
                    )
                )
            }
            
            exception.message shouldContain "Initiative order must include all participants"
        }
        
        test("should throw exception when initiative order has extra creatures") {
            val exception = shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 1,
                    campaignId = 5,
                    createdTimestamp = 1000,
                    currentRound = 1,
                    activeCreatureId = 10,
                    participants = listOf(10, 20),
                    initiativeOrder = listOf(
                        InitiativeEntryData(10, 18, 0, 18),
                        InitiativeEntryData(20, 15, 0, 15),
                        InitiativeEntryData(30, 12, 0, 12)
                    )
                )
            }
            
            exception.message shouldContain "Initiative order must include all participants"
        }
        
        test("should throw exception when initiative order has duplicate creatures") {
            val exception = shouldThrow<IllegalArgumentException> {
                Encounter(
                    id = 1,
                    campaignId = 5,
                    createdTimestamp = 1000,
                    currentRound = 1,
                    activeCreatureId = 10,
                    participants = listOf(10, 20),
                    initiativeOrder = listOf(
                        InitiativeEntryData(10, 18, 0, 18),
                        InitiativeEntryData(10, 15, 0, 15)
                    )
                )
            }
            
            exception.message shouldContain "Initiative order must include all participants"
        }
    }
    
    context("InitiativeEntry") {
        test("should create InitiativeEntry with valid values") {
            val entry = InitiativeEntryData(creatureId = 10, roll = 18, modifier = 0, total = 18)
            
            entry.creatureId shouldBe 10
            entry.initiative shouldBe 18
        }
        
        test("should create InitiativeEntry with negative initiative") {
            val entry = InitiativeEntryData(creatureId = 10, roll = -5, modifier = 0, total = -5)
            
            entry.initiative shouldBe -5
        }
    }
    
    context("Encounter with multiple rounds") {
        test("should create Encounter in later round") {
            val encounter = Encounter(
                id = 1,
                campaignId = 5,
                createdTimestamp = 1000,
                currentRound = 10,
                activeCreatureId = 20,
                participants = listOf(10, 20),
                initiativeOrder = listOf(
                    InitiativeEntryData(10, 18, 0, 18),
                    InitiativeEntryData(20, 15, 0, 15)
                )
            )
            
            encounter.currentRound shouldBe 10
        }
    }
})
