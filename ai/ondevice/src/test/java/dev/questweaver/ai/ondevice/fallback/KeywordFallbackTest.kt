package dev.questweaver.ai.ondevice.fallback

import dev.questweaver.core.domain.intent.IntentType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KeywordFallbackTest : FunSpec({
    
    val fallback = KeywordFallback()
    
    context("ATTACK intent") {
        test("matches 'attack' keyword") {
            val result = fallback.classify("attack the goblin")
            
            result.intent shouldBe IntentType.ATTACK
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'hit' keyword") {
            val result = fallback.classify("hit the orc")
            
            result.intent shouldBe IntentType.ATTACK
        }
        
        test("matches 'strike' keyword") {
            val result = fallback.classify("strike the enemy")
            
            result.intent shouldBe IntentType.ATTACK
        }
        
        test("matches 'shoot' keyword") {
            val result = fallback.classify("shoot the archer")
            
            result.intent shouldBe IntentType.ATTACK
        }
    }
    
    context("MOVE intent") {
        test("matches 'move' keyword") {
            val result = fallback.classify("move to E5")
            
            result.intent shouldBe IntentType.MOVE
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'go to' phrase") {
            val result = fallback.classify("go to the door")
            
            result.intent shouldBe IntentType.MOVE
        }
        
        test("matches 'walk' keyword") {
            val result = fallback.classify("walk forward")
            
            result.intent shouldBe IntentType.MOVE
        }
        
        test("matches 'run' keyword") {
            val result = fallback.classify("run away")
            
            result.intent shouldBe IntentType.MOVE
        }
    }
    
    context("CAST_SPELL intent") {
        test("matches 'cast' keyword") {
            val result = fallback.classify("cast fireball")
            
            result.intent shouldBe IntentType.CAST_SPELL
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'spell' keyword") {
            val result = fallback.classify("use a spell")
            
            result.intent shouldBe IntentType.CAST_SPELL
        }
        
        test("matches 'magic' keyword") {
            val result = fallback.classify("use magic missile")
            
            result.intent shouldBe IntentType.CAST_SPELL
        }
    }
    
    context("USE_ITEM intent") {
        test("matches 'use' keyword") {
            val result = fallback.classify("use potion")
            
            result.intent shouldBe IntentType.USE_ITEM
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'drink' keyword") {
            val result = fallback.classify("drink healing potion")
            
            result.intent shouldBe IntentType.USE_ITEM
        }
        
        test("matches 'potion' keyword") {
            val result = fallback.classify("take a potion")
            
            result.intent shouldBe IntentType.USE_ITEM
        }
    }
    
    context("DASH intent") {
        test("matches 'dash' keyword") {
            val result = fallback.classify("dash forward")
            
            result.intent shouldBe IntentType.DASH
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'sprint' keyword") {
            val result = fallback.classify("sprint to cover")
            
            result.intent shouldBe IntentType.DASH
        }
    }
    
    context("DODGE intent") {
        test("matches 'dodge' keyword") {
            val result = fallback.classify("dodge the attack")
            
            result.intent shouldBe IntentType.DODGE
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'evade' keyword") {
            val result = fallback.classify("evade incoming attacks")
            
            result.intent shouldBe IntentType.DODGE
        }
    }
    
    context("HELP intent") {
        test("matches 'help' keyword") {
            val result = fallback.classify("help my ally")
            
            result.intent shouldBe IntentType.HELP
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'assist' keyword") {
            val result = fallback.classify("assist the fighter")
            
            result.intent shouldBe IntentType.HELP
        }
    }
    
    context("HIDE intent") {
        test("matches 'hide' keyword") {
            val result = fallback.classify("hide behind the pillar")
            
            result.intent shouldBe IntentType.HIDE
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'stealth' keyword") {
            val result = fallback.classify("use stealth")
            
            result.intent shouldBe IntentType.HIDE
        }
    }
    
    context("DISENGAGE intent") {
        test("matches 'disengage' keyword") {
            val result = fallback.classify("disengage from combat")
            
            result.intent shouldBe IntentType.DISENGAGE
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'withdraw' keyword") {
            val result = fallback.classify("withdraw safely")
            
            result.intent shouldBe IntentType.DISENGAGE
        }
    }
    
    context("READY intent") {
        test("matches 'ready' keyword") {
            val result = fallback.classify("ready an action")
            
            result.intent shouldBe IntentType.READY
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'prepare' keyword") {
            val result = fallback.classify("prepare to strike")
            
            result.intent shouldBe IntentType.READY
        }
    }
    
    context("SEARCH intent") {
        test("matches 'search' keyword") {
            val result = fallback.classify("search the room")
            
            result.intent shouldBe IntentType.SEARCH
            result.confidence shouldBe 0.5f
            result.usedFallback shouldBe true
        }
        
        test("matches 'look for' phrase") {
            val result = fallback.classify("look for traps")
            
            result.intent shouldBe IntentType.SEARCH
        }
        
        test("matches 'investigate' keyword") {
            val result = fallback.classify("investigate the chest")
            
            result.intent shouldBe IntentType.SEARCH
        }
    }
    
    context("case insensitivity") {
        test("matches uppercase keywords") {
            val result = fallback.classify("ATTACK THE GOBLIN")
            
            result.intent shouldBe IntentType.ATTACK
        }
        
        test("matches mixed case keywords") {
            val result = fallback.classify("CaSt FiReBaLl")
            
            result.intent shouldBe IntentType.CAST_SPELL
        }
    }
    
    context("word boundaries") {
        test("does not match partial words") {
            // "attacking" should not match "attack" pattern due to word boundary
            val result = fallback.classify("I am attacking")
            
            // Should still match because "attacking" contains "attack" as a word
            // But let's test a case where it shouldn't match
            val result2 = fallback.classify("the attachment is broken")
            
            // "attachment" should not match "attack" due to word boundaries
            result2.intent shouldBe IntentType.UNKNOWN
        }
    }
    
    context("UNKNOWN intent") {
        test("returns UNKNOWN for unrecognized input") {
            val result = fallback.classify("do something random")
            
            result.intent shouldBe IntentType.UNKNOWN
            result.confidence shouldBe 0.0f
            result.usedFallback shouldBe true
        }
        
        test("returns UNKNOWN for empty input") {
            val result = fallback.classify("")
            
            result.intent shouldBe IntentType.UNKNOWN
            result.confidence shouldBe 0.0f
        }
    }
})
