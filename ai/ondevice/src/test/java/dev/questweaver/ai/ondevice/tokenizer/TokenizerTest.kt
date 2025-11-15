package dev.questweaver.ai.ondevice.tokenizer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class TokenizerTest : FunSpec({
    
    context("SimpleTokenizer") {
        val vocabulary = mapOf(
            "attack" to 10,
            "the" to 20,
            "goblin" to 30,
            "with" to 40,
            "my" to 50,
            "sword" to 60
        )
        
        test("tokenizes simple text") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 128)
            
            val tokens = tokenizer.tokenize("attack the goblin").toList()
            
            tokens shouldContain 10  // attack
            tokens shouldContain 20  // the
            tokens shouldContain 30  // goblin
        }
        
        test("converts text to lowercase") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 128)
            
            val tokens = tokenizer.tokenize("ATTACK THE GOBLIN").toList()
            
            tokens shouldContain 10  // attack
            tokens shouldContain 20  // the
            tokens shouldContain 30  // goblin
        }
        
        test("pads short sequences") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 128, paddingTokenId = 1)
            
            val tokens = tokenizer.tokenize("attack")
            
            tokens shouldHaveSize 128
            tokens.count { it == 1 } shouldBe 127  // 127 padding tokens
        }
        
        test("truncates long sequences") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 5)
            
            val tokens = tokenizer.tokenize("attack the goblin with my sword")
            
            tokens shouldHaveSize 5
        }
        
        test("handles unknown tokens") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 128, unknownTokenId = 0)
            
            val tokens = tokenizer.tokenize("attack the dragon").toList()  // "dragon" not in vocab
            
            tokens shouldContain 10  // attack
            tokens shouldContain 20  // the
            tokens shouldContain 0   // unknown token for "dragon"
        }
        
        test("splits on punctuation") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 128)
            
            val tokens = tokenizer.tokenize("attack, the goblin!").toList()
            
            tokens shouldContain 10  // attack
            tokens shouldContain 20  // the
            tokens shouldContain 30  // goblin
        }
        
        test("filters blank tokens") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 128)
            
            val tokens = tokenizer.tokenize("attack   the    goblin")  // Multiple spaces
            
            // Should only have 3 tokens + padding, not extra tokens for spaces
            val nonPaddingTokens = tokens.filter { it != 1 }
            nonPaddingTokens shouldHaveSize 3
        }
        
        test("handles empty input") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 128, paddingTokenId = 1)
            
            val tokens = tokenizer.tokenize("")
            
            tokens shouldHaveSize 128
            tokens.all { it == 1 } shouldBe true  // All padding
        }
        
        test("handles exact max length") {
            val tokenizer = SimpleTokenizer(vocabulary, maxLength = 3)
            
            val tokens = tokenizer.tokenize("attack the goblin")
            
            tokens shouldHaveSize 3
            tokens[0] shouldBe 10  // attack
            tokens[1] shouldBe 20  // the
            tokens[2] shouldBe 30  // goblin
        }
    }
})
