# Implementation Plan

- [x] 1. Set up ai/ondevice module structure and dependencies
  - Create `ai/ondevice` module directory with Gradle build file
  - Add ONNX Runtime 1.16.3 dependency
  - Add kotlinx-serialization for JSON parsing
  - Configure module to depend only on `core:domain`
  - Add to root `settings.gradle.kts`
  - _Requirements: 1.1, 4.1_

- [x] 2. Implement domain models for intent classification
  - [x] 2.1 Create IntentType enum in core:domain
    - Define 12 intent types (ATTACK, MOVE, CAST_SPELL, USE_ITEM, DASH, DODGE, HELP, HIDE, DISENGAGE, READY, SEARCH, UNKNOWN)
    - _Requirements: 1.3, 1.5_
  
  - [x] 2.2 Create NLAction data class in core:domain
    - Include intent, originalText, targetCreatureId, targetLocation, spellName, itemName, confidence fields
    - Make all fields except intent and originalText nullable
    - _Requirements: 6.1, 6.2_
  
  - [x] 2.3 Create IntentResult data class in ai:ondevice
    - Include intent, confidence, usedFallback fields
    - _Requirements: 1.5_
  
  - [x] 2.4 Create EntityExtractionResult and related data classes
    - Define ExtractedCreature with creatureId, name, matchedText, startIndex, endIndex
    - Define EntityExtractionResult with creatures, locations, spells, items lists
    - Define EncounterContext with creatures, playerSpells, playerInventory
    - _Requirements: 2.2, 2.5_

- [x] 3. Implement Tokenizer component
  - [x] 3.1 Create Tokenizer interface and SimpleTokenizer implementation
    - Implement text lowercasing and splitting on whitespace/punctuation
    - Implement vocabulary mapping with unknown token handling
    - Implement padding/truncation to 128 tokens
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [x] 3.2 Create vocabulary loader from JSON assets
    - Implement AssetLoader class to read vocabulary.json
    - Parse JSON into Map<String, Int>
    - Handle missing or corrupted vocabulary file
    - _Requirements: 5.3, 7.3_
  
  - [x] 3.3 Write unit tests for Tokenizer
    - Test tokenization of simple text
    - Test padding of short sequences
    - Test truncation of long sequences
    - Test unknown token handling
    - Test special characters and punctuation
    - _Requirements: 8.2_

- [x] 4. Implement KeywordFallback system
  - [x] 4.1 Create KeywordFallback class with regex patterns
    - Define regex patterns for each intent type (5-10 patterns per intent)
    - Implement pattern matching with case-insensitive comparison
    - Return IntentResult with confidence 0.5 for matches
    - Return UNKNOWN intent with confidence 0.0 for no matches
    - _Requirements: 3.2, 3.3, 3.4_
  
  - [x] 4.2 Write unit tests for KeywordFallback
    - Test pattern matching for each intent type
    - Test case-insensitive matching
    - Test word boundary handling
    - Test unknown input handling
    - Test with comprehensive phrase dataset
    - _Requirements: 8.4_

- [x] 5. Implement OnnxSessionManager
  - [x] 5.1 Create OnnxSessionManager class with initialization logic
    - Implement background thread initialization using IO dispatcher
    - Load ONNX model from assets
    - Create InferenceSession with model bytes
    - Implement request queuing during initialization
    - _Requirements: 4.1, 4.2, 4.4, 4.5_
  
  - [x] 5.2 Implement model warmup and inference methods
    - Run dummy inference on initialization to warm up model
    - Implement infer() method that takes IntArray tokens and returns FloatArray probabilities
    - Add thread safety with Mutex for session access
    - Implement isReady() check and close() cleanup
    - _Requirements: 4.3_
  
  - [x] 5.3 Add error handling for model loading failures
    - Handle missing model file gracefully
    - Handle corrupted model file
    - Handle insufficient memory errors
    - Log errors and signal fallback mode activation
    - _Requirements: 7.1_
  
  - [x] 5.4 Write unit tests for OnnxSessionManager
    - Test initialization with mock ONNX session
    - Test request queuing during initialization
    - Test inference with valid input
    - Test error handling for missing model
    - _Requirements: 8.1_

- [x] 6. Implement IntentClassifier
  - [x] 6.1 Create IntentClassifier interface and OnnxIntentClassifier implementation
    - Implement classify() method that tokenizes input
    - Call OnnxSessionManager.infer() with tokens
    - Find highest confidence intent from probabilities
    - Check confidence against threshold (0.6)
    - Fall back to KeywordFallback if confidence too low
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [x] 6.2 Add error handling and fallback logic
    - Catch ONNX Runtime exceptions and fall back to keywords
    - Handle timeout scenarios (>300ms)
    - Log warnings when using fallback
    - _Requirements: 3.5, 7.2_
  
  - [x] 6.3 Write unit tests for IntentClassifier
    - Test ONNX classification with high confidence
    - Test fallback to keywords when confidence low
    - Test fallback on ONNX exception
    - Test with mock OnnxSessionManager
    - _Requirements: 8.1_

- [x] 7. Implement EntityExtractor
  - [x] 7.1 Create EntityExtractor class with creature matching
    - Build case-insensitive name map from EncounterContext
    - Scan text for creature names using longest-match-first strategy
    - Handle partial name matches
    - Return ExtractedCreature with position information
    - _Requirements: 2.1, 2.2_
  
  - [x] 7.2 Implement location parsing
    - Add regex patterns for grid notation (e.g., "E5")
    - Add regex patterns for coordinate notation (e.g., "(5,5)")
    - Convert matches to GridPos objects
    - Validate coordinates are within map bounds
    - _Requirements: 2.3_
  
  - [x] 7.3 Implement spell and item matching
    - Match spell names against playerSpells list (case-insensitive)
    - Match item names against playerInventory list (case-insensitive)
    - Handle partial matches and common misspellings
    - _Requirements: 2.4_
  
  - [x] 7.4 Add error handling for entity extraction
    - Return empty EntityExtractionResult on failure instead of throwing
    - Log warnings for ambiguous references
    - _Requirements: 7.4_
  
  - [x] 7.5 Write unit tests for EntityExtractor
    - Test creature extraction by exact name
    - Test creature extraction by partial name
    - Test location parsing from grid notation
    - Test location parsing from coordinate notation
    - Test spell name matching
    - Test item name matching
    - Test ambiguous reference handling
    - _Requirements: 8.3_

- [x] 8. Implement IntentClassificationUseCase
  - [x] 8.1 Create IntentClassificationUseCase orchestration logic
    - Validate input text (non-empty, reasonable length)
    - Call IntentClassifier.classify()
    - Call EntityExtractor.extract() with EncounterContext
    - Construct NLAction from intent and extracted entities
    - Return ActionResult.Success with NLAction
    - _Requirements: 6.1, 6.2, 6.3_
  
  - [x] 8.2 Add disambiguation logic for missing entities
    - Check if required entities are present for intent type
    - Generate ActionOption list for disambiguation
    - Return ActionResult.RequiresChoice when entities missing or ambiguous
    - _Requirements: 6.4, 6.5_
  
  - [x] 8.3 Add input validation and error handling
    - Validate input is not empty
    - Truncate input if too long (>500 chars)
    - Sanitize invalid characters
    - Return ActionResult.Failure for invalid input
    - _Requirements: 7.3_
  
  - [x] 8.4 Write unit tests for IntentClassificationUseCase
    - Test successful classification with all entities
    - Test disambiguation when entities missing
    - Test error handling for empty input
    - Test error handling for invalid input
    - _Requirements: 8.1_

- [x] 9. Create model asset files and configuration
  - [x] 9.1 Add placeholder ONNX model to assets
    - Create `app/src/main/assets/models/` directory
    - Add placeholder `intent_classifier.onnx` file (or real model if available)
    - Add `vocabulary.json` with common D&D terms
    - Add `model_config.json` with model metadata
    - _Requirements: 4.2_
  
  - [x] 9.2 Implement model config loader
    - Parse model_config.json to get model parameters
    - Use config values for confidence threshold, max sequence length, etc.
    - _Requirements: 4.2_

- [x] 10. Add Koin dependency injection module
  - Create `ai/ondevice/di/OnDeviceModule.kt`
  - Register OnnxSessionManager as singleton
  - Register Tokenizer as singleton
  - Register KeywordFallback as singleton
  - Register IntentClassifier as singleton
  - Register EntityExtractor as factory
  - Register IntentClassificationUseCase as factory
  - Wire up dependencies
  - _Requirements: 6.1_

- [ ] 11. Write integration tests
  - [ ] 11.1 Create end-to-end classification test
    - Test full pipeline from text input to NLAction
    - Test with real EncounterContext containing creatures and spells
    - Verify intent, entities, and confidence are correct
    - _Requirements: 8.1_
  
  - [ ] 11.2 Create performance benchmark tests
    - Test classification completes within 300ms budget
    - Test tokenization completes within 20ms
    - Test entity extraction completes within 100ms
    - _Requirements: 1.2_
  
  - [ ] 11.3 Create accuracy test with test dataset
    - Create dataset of 500 common player commands
    - Run classification on entire dataset
    - Verify 85%+ accuracy on intent classification
    - _Requirements: 8.5_

- [ ] 12. Add comprehensive error handling tests
  - Test model loading failure scenarios
  - Test ONNX Runtime exception handling
  - Test timeout scenarios
  - Test invalid input handling
  - Test entity extraction failures
  - Verify graceful degradation to keyword fallback
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 13. Create documentation
  - Add KDoc comments to all public APIs
  - Document model requirements and specifications
  - Document vocabulary format and special tokens
  - Add usage examples to IntentClassificationUseCase
  - Document performance characteristics and budgets

---

**Last Updated:** 2025-11-15
