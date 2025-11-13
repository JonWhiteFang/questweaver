package dev.questweaver.feature.map.ui

import dev.questweaver.domain.map.geometry.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TokenRenderDataTest : FunSpec({
    
    context("showHP property") {
        test("is true for friendly creatures") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 20,
                maxHP = 30
            )
            
            token.showHP shouldBe true
        }
        
        test("is false for enemy creatures") {
            val token = TokenRenderData(
                creatureId = 2L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 20,
                maxHP = 30
            )
            
            token.showHP shouldBe false
        }
        
        test("is false for neutral creatures") {
            val token = TokenRenderData(
                creatureId = 3L,
                position = GridPos(5, 5),
                allegiance = Allegiance.NEUTRAL,
                currentHP = 20,
                maxHP = 30
            )
            
            token.showHP shouldBe false
        }
    }
    
    context("isBloodied property") {
        test("is true when HP is less than half") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 14,
                maxHP = 30
            )
            
            token.isBloodied shouldBe true
        }
        
        test("is false when HP is exactly half") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 15,
                maxHP = 30
            )
            
            token.isBloodied shouldBe false
        }
        
        test("is false when HP is more than half") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 20,
                maxHP = 30
            )
            
            token.isBloodied shouldBe false
        }
        
        test("is false when HP is at maximum") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 30,
                maxHP = 30
            )
            
            token.isBloodied shouldBe false
        }
        
        test("is true when HP is 1 and maxHP is 30") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 1,
                maxHP = 30
            )
            
            token.isBloodied shouldBe true
        }
        
        test("handles odd maxHP correctly (49 of 99)") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 49,
                maxHP = 99
            )
            
            // 49 < 99/2 (integer division = 49), so 49 < 49 is false, not bloodied
            token.isBloodied shouldBe false
        }
        
        test("handles odd maxHP correctly (50 of 99)") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 50,
                maxHP = 99
            )
            
            // 50 >= 99/2 (49.5 truncated to 49), so not bloodied
            token.isBloodied shouldBe false
        }
    }
    
    context("hpPercentage property") {
        test("calculates percentage correctly for full HP") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 30,
                maxHP = 30
            )
            
            token.hpPercentage shouldBe 1.0f
        }
        
        test("calculates percentage correctly for half HP") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 15,
                maxHP = 30
            )
            
            token.hpPercentage shouldBe 0.5f
        }
        
        test("calculates percentage correctly for quarter HP") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 10,
                maxHP = 40
            )
            
            token.hpPercentage shouldBe 0.25f
        }
        
        test("calculates percentage correctly for zero HP") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 0,
                maxHP = 30
            )
            
            token.hpPercentage shouldBe 0.0f
        }
        
        test("calculates percentage correctly for 75% HP") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 75,
                maxHP = 100
            )
            
            token.hpPercentage shouldBe 0.75f
        }
        
        test("calculates percentage correctly for odd numbers") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 17,
                maxHP = 23
            )
            
            // 17/23 ≈ 0.7391304
            token.hpPercentage shouldBe (17f / 23f)
        }
    }
    
    context("allegiance color mapping") {
        test("friendly allegiance is distinct") {
            val friendly = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 20,
                maxHP = 30
            )
            
            friendly.allegiance shouldBe Allegiance.FRIENDLY
        }
        
        test("enemy allegiance is distinct") {
            val enemy = TokenRenderData(
                creatureId = 2L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 20,
                maxHP = 30
            )
            
            enemy.allegiance shouldBe Allegiance.ENEMY
        }
        
        test("neutral allegiance is distinct") {
            val neutral = TokenRenderData(
                creatureId = 3L,
                position = GridPos(5, 5),
                allegiance = Allegiance.NEUTRAL,
                currentHP = 20,
                maxHP = 30
            )
            
            neutral.allegiance shouldBe Allegiance.NEUTRAL
        }
    }
    
    context("combined properties") {
        test("friendly creature at full HP shows HP and is not bloodied") {
            val token = TokenRenderData(
                creatureId = 1L,
                position = GridPos(5, 5),
                allegiance = Allegiance.FRIENDLY,
                currentHP = 30,
                maxHP = 30
            )
            
            token.showHP shouldBe true
            token.isBloodied shouldBe false
            token.hpPercentage shouldBe 1.0f
        }
        
        test("enemy creature below half HP does not show HP but is bloodied") {
            val token = TokenRenderData(
                creatureId = 2L,
                position = GridPos(5, 5),
                allegiance = Allegiance.ENEMY,
                currentHP = 10,
                maxHP = 30
            )
            
            token.showHP shouldBe false
            token.isBloodied shouldBe true
            token.hpPercentage shouldBe (10f / 30f)
        }
        
        test("neutral creature at half HP does not show HP and is not bloodied") {
            val token = TokenRenderData(
                creatureId = 3L,
                position = GridPos(5, 5),
                allegiance = Allegiance.NEUTRAL,
                currentHP = 15,
                maxHP = 30
            )
            
            token.showHP shouldBe false
            token.isBloodied shouldBe false
            token.hpPercentage shouldBe 0.5f
        }
    }
})
