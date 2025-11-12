# Requirements Document

## Introduction

This specification defines the tactical map rendering system for QuestWeaver's combat interface. The system uses Jetpack Compose Canvas to render the grid-based tactical map with creature tokens, movement paths, range overlays, and area-of-effect visualizations. The design prioritizes performance (60fps target), clarity, and integration with the grid geometry and pathfinding systems.

## Glossary

- **Tactical Map**: The visual representation of the combat grid with creatures and terrain
- **Canvas**: Jetpack Compose's low-level drawing API for custom graphics
- **Token**: A visual representation of a creature on the map (circle with HP indicator)
- **Movement Path**: A visual line showing the planned movement route
- **Range Overlay**: A visual highlight showing positions within range of an ability
- **AoE Visualization**: A visual representation of area-of-effect templates
- **System**: The map rendering system
- **Render Frame**: A single drawing pass of the map (target: ≤4ms)

## Requirements

### Requirement 1

**User Story:** As a player, I want to see a clear grid-based map, so that I can understand the tactical layout of the battlefield

#### Acceptance Criteria

1. THE System SHALL render a square grid with visible cell boundaries
2. THE System SHALL support grid dimensions from 10x10 to 100x100 cells
3. THE System SHALL render each cell as a square with consistent size
4. THE System SHALL use distinct colors for different terrain types (normal, difficult, impassable)
5. THE System SHALL render the grid using Jetpack Compose Canvas API

### Requirement 2

**User Story:** As a player, I want to see creature tokens on the map, so that I can identify where all combatants are positioned

#### Acceptance Criteria

1. THE System SHALL render each creature as a circular token at its GridPos
2. WHEN a creature is friendly, THE System SHALL display the creature's current HP as a number on the token
3. WHEN a creature is not friendly, THE System SHALL not display HP numbers on the token
4. WHEN a creature is not friendly and has less than half HP remaining, THE System SHALL indicate bloodied status with a visual indicator
5. THE System SHALL use color coding to indicate creature allegiance (friendly, enemy, neutral)
6. THE System SHALL scale token size appropriately to fit within grid cells
7. THE System SHALL render tokens above the grid layer for visibility

### Requirement 3

**User Story:** As a player, I want to see movement paths, so that I can plan where my character will move

#### Acceptance Criteria

1. WHEN a movement path is provided, THE System SHALL render a line connecting the path positions
2. THE System SHALL render the path line above the grid but below tokens
3. THE System SHALL use a distinct color for the movement path (e.g., blue or yellow)
4. THE System SHALL render path segments between adjacent GridPos instances
5. THE System SHALL indicate the path direction with visual cues if needed

### Requirement 4

**User Story:** As a player, I want to see range overlays, so that I can identify which positions are within range of my abilities

#### Acceptance Criteria

1. WHEN a range overlay is requested, THE System SHALL highlight all positions within the specified range
2. THE System SHALL use semi-transparent overlay color to show range without obscuring the grid
3. THE System SHALL support multiple range types (movement, weapon, spell)
4. THE System SHALL render range overlays above the grid but below paths and tokens
5. THE System SHALL update range overlay when the selected creature or ability changes

### Requirement 5

**User Story:** As a game master, I want to see area-of-effect visualizations, so that I can understand which creatures will be affected by spells

#### Acceptance Criteria

1. WHEN an AoE template is provided, THE System SHALL render the affected area on the map
2. THE System SHALL support sphere, cube, and cone AoE visualizations
3. THE System SHALL use semi-transparent overlay with distinct color for AoE areas
4. THE System SHALL render AoE visualizations above range overlays but below tokens
5. THE System SHALL show AoE direction for cone templates

### Requirement 6

**User Story:** As a player, I want smooth and responsive map rendering, so that the UI does not lag during combat

#### Acceptance Criteria

1. THE System SHALL render each frame in 4 milliseconds or less for typical grids (50x50)
2. THE System SHALL maintain 60 frames per second during map interactions
3. THE System SHALL use efficient drawing techniques to minimize overdraw
4. THE System SHALL batch draw calls where possible to reduce rendering overhead
5. THE System SHALL profile and optimize rendering performance to meet targets

### Requirement 7

**User Story:** As a player, I want to interact with the map, so that I can select positions and creatures

#### Acceptance Criteria

1. WHEN the user taps a grid cell, THE System SHALL convert screen coordinates to GridPos
2. THE System SHALL provide callbacks for tap events with the selected GridPos
3. THE System SHALL support pan and zoom gestures for map navigation
4. THE System SHALL maintain grid alignment during zoom operations
5. THE System SHALL provide visual feedback for selected cells or tokens

### Requirement 8

**User Story:** As a developer, I want the rendering system to integrate with MVI architecture, so that it follows project patterns

#### Acceptance Criteria

1. THE System SHALL be implemented in the feature:map module
2. THE System SHALL receive rendering state through a MapRenderState data class
3. THE System SHALL emit user interactions as MapIntent sealed interface instances
4. THE System SHALL use StateFlow for reactive state updates
5. THE System SHALL keep the Composable stateless with state hoisting to ViewModel
