# Requirements Document

## Introduction

This specification defines the grid system and geometry calculations for QuestWeaver's tactical combat map. The system provides a foundation for positioning creatures, calculating distances, determining line-of-effect, and defining area-of-effect templates on a square grid. All calculations must be deterministic and support D&D 5e SRD distance rules.

## Glossary

- **MapGrid**: The data structure representing the tactical combat grid with dimensions and cell properties
- **GridPos**: A position on the grid defined by x and y coordinates
- **Line-of-Effect**: An unobstructed path between two grid positions
- **Range**: The distance between two positions measured in feet (5ft per square)
- **Area-of-Effect (AoE)**: A template defining multiple grid cells affected by an ability (cone, sphere, cube)
- **System**: The grid geometry calculation system

## Requirements

### Requirement 1

**User Story:** As a game master, I want a grid-based map system, so that I can position creatures and terrain for tactical combat

#### Acceptance Criteria

1. THE System SHALL provide a MapGrid data structure with configurable width and height dimensions
2. THE System SHALL store grid cell properties including terrain type and occupancy status
3. THE System SHALL support grid dimensions from 10x10 to 100x100 cells
4. THE System SHALL represent each grid cell as a 5-foot square per D&D 5e SRD rules
5. THE System SHALL validate that grid positions are within bounds before operations

### Requirement 2

**User Story:** As a player, I want to know the distance between positions, so that I can determine if my character can reach a target

#### Acceptance Criteria

1. WHEN calculating distance between two GridPos instances, THE System SHALL return the distance in feet
2. THE System SHALL use Chebyshev distance (diagonal movement costs same as orthogonal) per D&D 5e rules
3. THE System SHALL calculate distance as max(abs(x2-x1), abs(y2-y1)) multiplied by 5 feet
4. THE System SHALL provide a method to check if two positions are within a specified range in feet
5. THE System SHALL return adjacent neighbors for a GridPos (up to 8 neighbors including diagonals)

### Requirement 3

**User Story:** As a game master, I want to determine line-of-effect between positions, so that I can validate ranged attacks and spell targeting

#### Acceptance Criteria

1. WHEN checking line-of-effect between two GridPos instances, THE System SHALL return whether an unobstructed path exists
2. THE System SHALL use Bresenham's line algorithm to trace the path between positions
3. WHEN a grid cell contains an obstacle, THE System SHALL block line-of-effect through that cell
4. THE System SHALL allow line-of-effect through cells occupied by creatures
5. THE System SHALL consider corner cases where diagonal movement passes between two obstacles

### Requirement 4

**User Story:** As a player, I want to see which squares are within range of my abilities, so that I can select valid targets

#### Acceptance Criteria

1. WHEN given a GridPos and range in feet, THE System SHALL return all positions within that range
2. THE System SHALL support common D&D ranges including 5ft, 10ft, 15ft, 30ft, 60ft, and 120ft
3. THE System SHALL exclude positions outside the grid boundaries from range calculations
4. THE System SHALL provide separate methods for range with and without line-of-effect requirements
5. THE System SHALL calculate range efficiently for performance targets (sub-millisecond for typical grids)

### Requirement 5

**User Story:** As a game master, I want to apply area-of-effect templates, so that I can determine which creatures are affected by spells and abilities

#### Acceptance Criteria

1. THE System SHALL provide a cone template with configurable length (15ft, 30ft, 60ft) and 53-degree angle
2. THE System SHALL provide a sphere template with configurable radius (5ft, 10ft, 15ft, 20ft)
3. THE System SHALL provide a cube template with configurable side length (5ft, 10ft, 15ft, 20ft)
4. WHEN applying an AoE template, THE System SHALL return all GridPos instances within the affected area
5. THE System SHALL support directional orientation for cone templates (8 cardinal and diagonal directions)
6. THE System SHALL calculate AoE templates deterministically with consistent results for the same inputs

### Requirement 6

**User Story:** As a developer, I want the grid system to be pure Kotlin, so that it can be used in the core:domain module without Android dependencies

#### Acceptance Criteria

1. THE System SHALL be implemented in the core:domain module
2. THE System SHALL use only Kotlin standard library dependencies
3. THE System SHALL not import any Android framework classes
4. THE System SHALL use immutable data classes for GridPos and MapGrid
5. THE System SHALL provide pure functions for all geometry calculations
