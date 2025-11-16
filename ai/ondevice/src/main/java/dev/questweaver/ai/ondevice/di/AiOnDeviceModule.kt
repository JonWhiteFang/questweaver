package dev.questweaver.ai.ondevice.di

import dev.questweaver.ai.ondevice.classifier.IntentClassifier
import dev.questweaver.ai.ondevice.classifier.OnnxIntentClassifier
import dev.questweaver.ai.ondevice.extractor.EntityExtractor
import dev.questweaver.ai.ondevice.fallback.KeywordFallback
import dev.questweaver.ai.ondevice.inference.OnnxSessionManager
import dev.questweaver.ai.ondevice.tokenizer.SimpleTokenizer
import dev.questweaver.ai.ondevice.tokenizer.Tokenizer
import dev.questweaver.ai.ondevice.tokenizer.VocabularyLoader
import dev.questweaver.ai.ondevice.usecase.IntentClassificationUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin dependency injection module for ai/ondevice components.
 *
 * This module provides:
 * - OnnxSessionManager (singleton) - ONNX Runtime session management
 * - Tokenizer (singleton) - Text tokenization with vocabulary
 * - KeywordFallback (singleton) - Rule-based intent classification
 * - IntentClassifier (singleton) - ONNX-based intent classification
 * - EntityExtractor (factory) - Entity extraction from text
 * - IntentClassificationUseCase (factory) - Full classification pipeline
 */
val aiOnDeviceModule = module {
    
    // OnnxSessionManager - singleton for shared ONNX Runtime session
    single {
        OnnxSessionManager(
            context = androidContext(),
            modelPath = "models/intent_classifier.onnx"
        )
    }
    
    // VocabularyLoader - singleton for loading vocabulary
    single {
        VocabularyLoader(context = androidContext())
    }
    
    // Tokenizer - singleton with loaded vocabulary
    single<Tokenizer> {
        val vocabularyLoader: VocabularyLoader = get()
        
        // Try to load vocabulary from assets, fall back to default if missing
        val vocabulary = try {
            vocabularyLoader.loadVocabulary("models/vocabulary.json")
        } catch (e: java.io.IOException) {
            // Log warning and use default vocabulary
            org.slf4j.LoggerFactory.getLogger("AiOnDeviceModule")
                .warn("Failed to load vocabulary from assets, using default", e)
            vocabularyLoader.createDefaultVocabulary()
        } catch (e: IllegalArgumentException) {
            // Log warning and use default vocabulary
            org.slf4j.LoggerFactory.getLogger("AiOnDeviceModule")
                .warn("Invalid vocabulary format, using default", e)
            vocabularyLoader.createDefaultVocabulary()
        }
        
        SimpleTokenizer(
            vocabulary = vocabulary,
            maxLength = 128,
            unknownTokenId = 0,
            paddingTokenId = 1
        )
    }
    
    // KeywordFallback - singleton for rule-based classification
    single {
        KeywordFallback()
    }
    
    // IntentClassifier - singleton ONNX-based classifier
    single<IntentClassifier> {
        OnnxIntentClassifier(
            sessionManager = get(),
            tokenizer = get(),
            keywordFallback = get(),
            confidenceThreshold = 0.6f,
            timeoutMs = 300L
        )
    }
    
    // EntityExtractor - factory (new instance per request)
    factory {
        EntityExtractor()
    }
    
    // IntentClassificationUseCase - factory (new instance per request)
    factory {
        IntentClassificationUseCase(
            intentClassifier = get(),
            entityExtractor = get()
        )
    }
}
