package dev.questweaver.ai.ondevice.fallback

import dev.questweaver.ai.ondevice.model.IntentResult
import dev.questweaver.core.domain.intent.IntentType

/**
 * Provides rule-based intent classification using keyword patterns.
 * 
 * This is used as a fallback when the ONNX model is unavailable or
 * produces low-confidence results.
 */
class KeywordFallback {
    
    private val patterns = mapOf(
        IntentType.ATTACK to listOf(
            Regex("\\battack\\b", RegexOption.IGNORE_CASE),
            Regex("\\bhit\\b", RegexOption.IGNORE_CASE),
            Regex("\\bstrike\\b", RegexOption.IGNORE_CASE),
            Regex("\\bshoot\\b", RegexOption.IGNORE_CASE),
            Regex("\\bfire at\\b", RegexOption.IGNORE_CASE),
            Regex("\\bswing at\\b", RegexOption.IGNORE_CASE),
            Regex("\\bslash\\b", RegexOption.IGNORE_CASE),
            Regex("\\bstab\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.MOVE to listOf(
            Regex("\\bmove\\b", RegexOption.IGNORE_CASE),
            Regex("\\bgo to\\b", RegexOption.IGNORE_CASE),
            Regex("\\bwalk\\b", RegexOption.IGNORE_CASE),
            Regex("\\brun\\b", RegexOption.IGNORE_CASE),
            Regex("\\bstep\\b", RegexOption.IGNORE_CASE),
            Regex("\\badvance\\b", RegexOption.IGNORE_CASE),
            Regex("\\bretreat\\b", RegexOption.IGNORE_CASE),
            Regex("\\bapproach\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.CAST_SPELL to listOf(
            Regex("\\bcast\\b", RegexOption.IGNORE_CASE),
            Regex("\\bspell\\b", RegexOption.IGNORE_CASE),
            Regex("\\bmagic\\b", RegexOption.IGNORE_CASE),
            Regex("\\bconjure\\b", RegexOption.IGNORE_CASE),
            Regex("\\binvoke\\b", RegexOption.IGNORE_CASE),
            Regex("\\bsummon\\b", RegexOption.IGNORE_CASE),
            Regex("\\bchant\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.USE_ITEM to listOf(
            Regex("\\buse\\b", RegexOption.IGNORE_CASE),
            Regex("\\bdrink\\b", RegexOption.IGNORE_CASE),
            Regex("\\bpotion\\b", RegexOption.IGNORE_CASE),
            Regex("\\bapply\\b", RegexOption.IGNORE_CASE),
            Regex("\\bactivate\\b", RegexOption.IGNORE_CASE),
            Regex("\\bconsume\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.DASH to listOf(
            Regex("\\bdash\\b", RegexOption.IGNORE_CASE),
            Regex("\\bsprint\\b", RegexOption.IGNORE_CASE),
            Regex("\\bdouble move\\b", RegexOption.IGNORE_CASE),
            Regex("\\brun fast\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.DODGE to listOf(
            Regex("\\bdodge\\b", RegexOption.IGNORE_CASE),
            Regex("\\bevade\\b", RegexOption.IGNORE_CASE),
            Regex("\\bdefend\\b", RegexOption.IGNORE_CASE),
            Regex("\\btake cover\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.HELP to listOf(
            Regex("\\bhelp\\b", RegexOption.IGNORE_CASE),
            Regex("\\bassist\\b", RegexOption.IGNORE_CASE),
            Regex("\\baid\\b", RegexOption.IGNORE_CASE),
            Regex("\\bsupport\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.HIDE to listOf(
            Regex("\\bhide\\b", RegexOption.IGNORE_CASE),
            Regex("\\bstealth\\b", RegexOption.IGNORE_CASE),
            Regex("\\bsneak\\b", RegexOption.IGNORE_CASE),
            Regex("\\bconceal\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.DISENGAGE to listOf(
            Regex("\\bdisengage\\b", RegexOption.IGNORE_CASE),
            Regex("\\bwithdraw\\b", RegexOption.IGNORE_CASE),
            Regex("\\bfall back\\b", RegexOption.IGNORE_CASE),
            Regex("\\bretreat safely\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.READY to listOf(
            Regex("\\bready\\b", RegexOption.IGNORE_CASE),
            Regex("\\bprepare\\b", RegexOption.IGNORE_CASE),
            Regex("\\bwait for\\b", RegexOption.IGNORE_CASE),
            Regex("\\bhold action\\b", RegexOption.IGNORE_CASE)
        ),
        IntentType.SEARCH to listOf(
            Regex("\\bsearch\\b", RegexOption.IGNORE_CASE),
            Regex("\\blook for\\b", RegexOption.IGNORE_CASE),
            Regex("\\binvestigate\\b", RegexOption.IGNORE_CASE),
            Regex("\\bexamine\\b", RegexOption.IGNORE_CASE),
            Regex("\\bcheck\\b", RegexOption.IGNORE_CASE)
        )
    )
    
    /**
     * Classifies text using keyword pattern matching.
     * 
     * @param text The input text to classify
     * @return IntentResult with matched intent or UNKNOWN if no match
     */
    fun classify(text: String): IntentResult {
        for ((intent, regexList) in patterns) {
            if (regexList.any { it.containsMatchIn(text) }) {
                return IntentResult(
                    intent = intent,
                    confidence = 0.5f,  // Fixed confidence for keyword matches
                    usedFallback = true
                )
            }
        }
        
        return IntentResult(
            intent = IntentType.UNKNOWN,
            confidence = 0.0f,
            usedFallback = true
        )
    }
}
