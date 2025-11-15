# Requirements Document

## Introduction

The Intent Classification system enables QuestWeaver to understand natural language player input and convert it into structured game actions. This on-device system uses ONNX Runtime to classify player intent (attack, move, cast spell, etc.) and extract relevant entities (targets, locations, spell names) from text input. The system must work offline, provide fast responses (≤300ms), and gracefully fall back to keyword matching when the ML model is unavailable or uncertain.

## Glossary

- **Intent Classifier**: An ONNX-based machine learning model that categorizes player text input into predefined intent types
- **Entity Extractor**: A component that identifies and extracts specific game entities (creature names, locations, spell names) from player input
- **ONNX Runtime**: A cross-platform inference engine for running machine learning models
- **Tokenizer**: A component that converts text into numerical tokens for model input
- **Keyword Fallback**: A rule-based system that matches input text against predefined patterns when ML classification fails or is unavailable
- **Intent Confidence**: A numerical score (0.0-1.0) indicating the model's certainty about the classified intent
- **NLAction**: A domain model representing a structured action derived from natural language input

## Requirements

### Requirement 1

**User Story:** As a player, I want to type natural language commands like "attack the goblin" so that I can control my character intuitively without memorizing specific syntax

#### Acceptance Criteria

1. WHEN the player submits text input, THE Intent Classifier SHALL tokenize the input text using the configured Tokenizer
2. WHEN tokenized input is provided, THE Intent Classifier SHALL execute the ONNX model and return intent probabilities within 300 milliseconds
3. WHEN intent probabilities are returned, THE Intent Classifier SHALL select the intent with the highest confidence score above 0.6 threshold
4. IF no intent exceeds the confidence threshold, THEN THE Intent Classifier SHALL invoke the Keyword Fallback system
5. WHEN an intent is classified, THE Intent Classifier SHALL return an IntentResult containing the intent type and confidence score

### Requirement 2

**User Story:** As a player, I want the system to understand who or what I'm referring to in my commands so that my actions target the correct creatures or objects

#### Acceptance Criteria

1. WHEN the Intent Classifier identifies an intent, THE Entity Extractor SHALL scan the input text for creature names present in the current encounter
2. WHEN creature names are found, THE Entity Extractor SHALL match them to Creature entities using case-insensitive comparison
3. WHEN location references are found (e.g., "move to E5"), THE Entity Extractor SHALL parse them into GridPos coordinates
4. WHEN spell names are found, THE Entity Extractor SHALL match them against the player character's known spell list
5. WHEN entities are extracted, THE Entity Extractor SHALL return an EntityExtractionResult containing matched entities with their types and positions in the text

### Requirement 3

**User Story:** As a player, I want the game to work even when the AI model fails or is unavailable so that I can always play without interruption

#### Acceptance Criteria

1. WHEN the ONNX model fails to load, THE Keyword Fallback system SHALL activate automatically
2. WHEN the Keyword Fallback system is active, THE system SHALL match input text against predefined regex patterns for common intents
3. WHEN a keyword pattern matches, THE Keyword Fallback system SHALL return the corresponding intent type with confidence score 0.5
4. IF no keyword pattern matches, THEN THE Keyword Fallback system SHALL return IntentResult with intent type UNKNOWN and confidence score 0.0
5. WHEN the Keyword Fallback system is used, THE system SHALL log a warning indicating fallback mode is active

### Requirement 4

**User Story:** As a developer, I want the ONNX Runtime to be initialized efficiently so that the app starts quickly and doesn't block the main thread

#### Acceptance Criteria

1. WHEN the application starts, THE OnnxSessionManager SHALL initialize the ONNX Runtime on a background thread
2. WHEN the ONNX model file is loaded, THE OnnxSessionManager SHALL create an InferenceSession with the model from app assets
3. WHEN the InferenceSession is created, THE OnnxSessionManager SHALL warm up the model by running a dummy inference
4. WHILE the model is initializing, THE OnnxSessionManager SHALL queue incoming classification requests
5. WHEN initialization completes, THE OnnxSessionManager SHALL process queued requests in order

### Requirement 5

**User Story:** As a developer, I want the tokenizer to convert text into model-compatible input so that the ONNX model can process player commands

#### Acceptance Criteria

1. WHEN text input is provided, THE Tokenizer SHALL convert the text to lowercase
2. WHEN text is lowercased, THE Tokenizer SHALL split the text into tokens using whitespace and punctuation delimiters
3. WHEN tokens are generated, THE Tokenizer SHALL map each token to its corresponding vocabulary index
4. IF a token is not in the vocabulary, THEN THE Tokenizer SHALL map it to the unknown token index
5. WHEN token indices are generated, THE Tokenizer SHALL pad or truncate the sequence to the model's maximum input length (128 tokens)

### Requirement 6

**User Story:** As a developer, I want the intent classification system to integrate with the domain layer so that classified intents can be validated and executed by the rules engine

#### Acceptance Criteria

1. WHEN an intent is classified and entities are extracted, THE IntentClassificationUseCase SHALL construct an NLAction domain object
2. WHEN an NLAction is constructed, THE IntentClassificationUseCase SHALL include the intent type, extracted entities, and original text
3. WHEN an NLAction is returned, THE system SHALL pass it to the action validation system for rules engine verification
4. IF entity extraction fails to find required entities, THEN THE IntentClassificationUseCase SHALL return ActionResult.RequiresChoice with disambiguation options
5. WHEN disambiguation is required, THE system SHALL present the player with a list of possible interpretations

### Requirement 7

**User Story:** As a developer, I want comprehensive error handling so that model failures don't crash the app

#### Acceptance Criteria

1. IF the ONNX model file is missing from assets, THEN THE OnnxSessionManager SHALL log an error and activate Keyword Fallback mode
2. IF the ONNX Runtime throws an exception during inference, THEN THE Intent Classifier SHALL catch the exception, log it, and fall back to keyword matching
3. IF the Tokenizer encounters invalid input, THEN THE Tokenizer SHALL sanitize the input and log a warning
4. IF entity extraction fails, THEN THE Entity Extractor SHALL return an empty EntityExtractionResult rather than throwing an exception
5. WHEN any component fails, THE system SHALL continue operating in degraded mode using keyword fallback

### Requirement 8

**User Story:** As a developer, I want the intent classification system to be testable so that I can verify its accuracy and reliability

#### Acceptance Criteria

1. THE Intent Classifier SHALL provide a test mode that uses a mock ONNX session for unit testing
2. THE Tokenizer SHALL be testable independently with known input-output pairs
3. THE Entity Extractor SHALL be testable with mock encounter state containing known creatures
4. THE Keyword Fallback system SHALL be testable with a comprehensive set of test phrases
5. THE system SHALL achieve 85% or higher accuracy on a test dataset of 500 common player commands

---

**Last Updated:** 2025-11-15
