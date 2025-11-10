package dev.questweaver.domain.entities

import dev.questweaver.domain.values.ContentRating
import dev.questweaver.domain.values.Difficulty
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CampaignTest : FunSpec({
    
    context("Campaign validation") {
        test("should create Campaign with valid values") {
            val campaign = Campaign(
                id = 1,
                name = "Test Campaign",
                createdTimestamp = 1000,
                lastPlayedTimestamp = 2000,
                playerCharacterId = 5,
                settings = CampaignSettings()
            )
            
            campaign.id shouldBe 1
            campaign.name shouldBe "Test Campaign"
            campaign.createdTimestamp shouldBe 1000
            campaign.lastPlayedTimestamp shouldBe 2000
            campaign.playerCharacterId shouldBe 5
        }
        
        test("should throw exception for non-positive id") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = 0,
                    name = "Test",
                    createdTimestamp = 1000,
                    lastPlayedTimestamp = 2000,
                    playerCharacterId = 1,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should throw exception for negative id") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = -1,
                    name = "Test",
                    createdTimestamp = 1000,
                    lastPlayedTimestamp = 2000,
                    playerCharacterId = 1,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should throw exception for blank name") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = 1,
                    name = "",
                    createdTimestamp = 1000,
                    lastPlayedTimestamp = 2000,
                    playerCharacterId = 1,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should throw exception for whitespace-only name") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = 1,
                    name = "   ",
                    createdTimestamp = 1000,
                    lastPlayedTimestamp = 2000,
                    playerCharacterId = 1,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should throw exception for non-positive created timestamp") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = 1,
                    name = "Test",
                    createdTimestamp = 0,
                    lastPlayedTimestamp = 2000,
                    playerCharacterId = 1,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should throw exception for negative created timestamp") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = 1,
                    name = "Test",
                    createdTimestamp = -1,
                    lastPlayedTimestamp = 2000,
                    playerCharacterId = 1,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should throw exception when last played is before created") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = 1,
                    name = "Test",
                    createdTimestamp = 2000,
                    lastPlayedTimestamp = 1000,
                    playerCharacterId = 1,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should accept last played equal to created") {
            shouldNotThrowAny {
                Campaign(
                    id = 1,
                    name = "Test",
                    createdTimestamp = 1000,
                    lastPlayedTimestamp = 1000,
                    playerCharacterId = 1,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should throw exception for non-positive player character id") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = 1,
                    name = "Test",
                    createdTimestamp = 1000,
                    lastPlayedTimestamp = 2000,
                    playerCharacterId = 0,
                    settings = CampaignSettings()
                )
            }
        }
        
        test("should throw exception for negative player character id") {
            shouldThrow<IllegalArgumentException> {
                Campaign(
                    id = 1,
                    name = "Test",
                    createdTimestamp = 1000,
                    lastPlayedTimestamp = 2000,
                    playerCharacterId = -1,
                    settings = CampaignSettings()
                )
            }
        }
    }
    
    context("CampaignSettings") {
        test("should create CampaignSettings with default values") {
            val settings = CampaignSettings()
            
            settings.difficulty shouldBe Difficulty.NORMAL
            settings.contentRating shouldBe ContentRating.TEEN
        }
        
        test("should create CampaignSettings with custom difficulty") {
            val settings = CampaignSettings(difficulty = Difficulty.HARD)
            
            settings.difficulty shouldBe Difficulty.HARD
            settings.contentRating shouldBe ContentRating.TEEN
        }
        
        test("should create CampaignSettings with custom content rating") {
            val settings = CampaignSettings(contentRating = ContentRating.MATURE)
            
            settings.difficulty shouldBe Difficulty.NORMAL
            settings.contentRating shouldBe ContentRating.MATURE
        }
        
        test("should create CampaignSettings with all custom values") {
            val settings = CampaignSettings(
                difficulty = Difficulty.EASY,
                contentRating = ContentRating.EVERYONE
            )
            
            settings.difficulty shouldBe Difficulty.EASY
            settings.contentRating shouldBe ContentRating.EVERYONE
        }
    }
    
    context("Campaign with settings") {
        test("should create Campaign with custom settings") {
            val campaign = Campaign(
                id = 1,
                name = "Test",
                createdTimestamp = 1000,
                lastPlayedTimestamp = 2000,
                playerCharacterId = 1,
                settings = CampaignSettings(
                    difficulty = Difficulty.DEADLY,
                    contentRating = ContentRating.MATURE
                )
            )
            
            campaign.settings.difficulty shouldBe Difficulty.DEADLY
            campaign.settings.contentRating shouldBe ContentRating.MATURE
        }
    }
})
