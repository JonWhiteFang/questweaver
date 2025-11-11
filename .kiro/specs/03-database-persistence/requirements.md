# Requirements Document

## Introduction

This specification defines the database and persistence layer for QuestWeaver, implementing event-sourced storage with SQLCipher encryption. The system provides secure, offline-first data persistence for all game state through immutable event logs, enabling full campaign replay and deterministic state reconstruction.

## Glossary

- **Event Sourcing System**: The persistence architecture where all state mutations are captured as immutable events stored in a database, allowing state to be derived through event replay
- **SQLCipher**: An encrypted SQLite database extension that provides transparent 256-bit AES encryption of database files
- **Room Database**: Android's persistence library providing an abstraction layer over SQLite with compile-time verification of SQL queries
- **Android Keystore**: A secure system-level credential storage that protects cryptographic keys from extraction
- **EventDao**: Data Access Object interface defining database operations for GameEvent entities
- **Repository**: An abstraction layer implementing the repository pattern, providing a clean API for data access while hiding implementation details
- **Database Migration**: A versioned schema change that transforms the database structure from one version to another while preserving data integrity

## Requirements

### Requirement 1

**User Story:** As a developer, I want a Room database configured with SQLCipher encryption, so that all game data is stored securely on the device

#### Acceptance Criteria

1. THE Event Sourcing System SHALL create a Room database instance with SQLCipher encryption enabled
2. THE Event Sourcing System SHALL configure the database with version number 1 and include all required entity classes
3. THE Event Sourcing System SHALL provide a singleton database instance accessible through dependency injection
4. THE Event Sourcing System SHALL initialize the database with proper type converters for custom data types
5. WHERE the database file does not exist, THE Event Sourcing System SHALL create a new encrypted database file on first access

### Requirement 2

**User Story:** As a developer, I want encryption keys managed through Android Keystore, so that database encryption keys are protected from extraction and persist across app restarts

#### Acceptance Criteria

1. THE Event Sourcing System SHALL generate a 256-bit AES encryption key using Android Keystore on first launch
2. THE Event Sourcing System SHALL store the encryption key in Android Keystore with hardware-backed security where available
3. THE Event Sourcing System SHALL retrieve the encryption key from Android Keystore when opening the database
4. IF the encryption key is not found in Android Keystore, THEN THE Event Sourcing System SHALL generate a new key and initialize a new database
5. THE Event Sourcing System SHALL configure the key with user authentication requirements disabled to allow background access

### Requirement 3

**User Story:** As a developer, I want EventDao and EventEntity implementations, so that GameEvent domain objects can be persisted and queried efficiently

#### Acceptance Criteria

1. THE Event Sourcing System SHALL define an EventEntity Room entity class with columns for event type, session ID, timestamp, and serialized event data
2. THE Event Sourcing System SHALL define an EventDao interface with methods to insert events, query events by session, and observe events reactively
3. THE Event Sourcing System SHALL implement automatic mapping between GameEvent domain objects and EventEntity database objects
4. THE Event Sourcing System SHALL use kotlinx-serialization to serialize GameEvent polymorphic types to JSON for storage
5. THE Event Sourcing System SHALL create database indices on session_id and timestamp columns for query performance

### Requirement 4

**User Story:** As a developer, I want EventRepository implementation, so that the domain layer can persist and retrieve events without knowing database implementation details

#### Acceptance Criteria

1. THE Event Sourcing System SHALL implement EventRepository interface defined in core:domain module
2. THE Event Sourcing System SHALL provide a suspend function to append a single GameEvent to the database
3. THE Event Sourcing System SHALL provide a suspend function to retrieve all events for a given session ID ordered by timestamp
4. THE Event Sourcing System SHALL provide a Flow-based function to observe events for a session with automatic updates
5. THE Event Sourcing System SHALL handle database exceptions and convert them to domain-appropriate error types

### Requirement 5

**User Story:** As a developer, I want database migration support, so that future schema changes can be applied safely without data loss

#### Acceptance Criteria

1. THE Event Sourcing System SHALL define a migration strategy for version 1 to version 2 schema changes
2. THE Event Sourcing System SHALL configure Room to execute migrations automatically on database version mismatch
3. WHERE a migration fails, THE Event Sourcing System SHALL provide fallback behavior to prevent data corruption
4. THE Event Sourcing System SHALL validate database integrity after migration completion
5. THE Event Sourcing System SHALL log migration execution for debugging purposes

### Requirement 6

**User Story:** As a developer, I want type converters for custom domain types, so that complex objects can be stored in Room database columns

#### Acceptance Criteria

1. THE Event Sourcing System SHALL provide a type converter for Long timestamp values to ensure consistent storage
2. THE Event Sourcing System SHALL provide a type converter for serializing GameEvent polymorphic types to JSON strings
3. THE Event Sourcing System SHALL provide a type converter for deserializing JSON strings back to GameEvent instances
4. THE Event Sourcing System SHALL register all type converters with the Room database configuration
5. THE Event Sourcing System SHALL handle serialization errors gracefully with appropriate error messages

### Requirement 7

**User Story:** As a developer, I want Koin dependency injection modules for the data layer, so that database and repository instances can be injected throughout the application

#### Acceptance Criteria

1. THE Event Sourcing System SHALL define a Koin module providing the Room database as a singleton
2. THE Event Sourcing System SHALL define a Koin module providing EventDao instances from the database
3. THE Event Sourcing System SHALL define a Koin module providing EventRepository implementation as a singleton
4. THE Event Sourcing System SHALL define a Koin module providing encryption key management utilities
5. THE Event Sourcing System SHALL ensure all data layer dependencies are properly scoped and lifecycle-aware

### Requirement 8

**User Story:** As a developer, I want database operations to be tested with in-memory databases, so that repository behavior can be verified without affecting production data

#### Acceptance Criteria

1. THE Event Sourcing System SHALL provide test utilities for creating in-memory Room databases
2. THE Event Sourcing System SHALL verify that events can be inserted and retrieved correctly through EventRepository
3. THE Event Sourcing System SHALL verify that event ordering by timestamp is maintained in query results
4. THE Event Sourcing System SHALL verify that Flow-based observation emits updates when new events are inserted
5. THE Event Sourcing System SHALL verify that serialization and deserialization of all GameEvent types works correctly
