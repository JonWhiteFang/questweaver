# Implementation Plan

- [x] 1. Set up core data structures


  - Create `core/domain/map/geometry` package structure
  - Define `GridPos` data class with x, y coordinates and serialization
  - Define `Direction` enum with 8 cardinal and diagonal directions
  - Define `TerrainType` enum (NORMAL, DIFFICULT, IMPASSABLE)
  - Define `CellProperties` data class with terrain, obstacle, and occupancy fields
  - _Requirements: 1.1, 1.2, 6.1, 6.2, 6.3, 6.4_

- [x] 2. Implement MapGrid data structure


  - Create `MapGrid` data class with width, height, and cells map
  - Add initialization validation for dimensions (10-100 range)
  - Implement `isInBounds()` method for position validation
  - Implement `getCellProperties()` method with default empty properties
  - Implement `withCellProperties()` method for immutable updates
  - Implement `allPositions()` method returning lazy Sequence
  - _Requirements: 1.1, 1.2, 1.3, 1.5, 6.4, 6.5_

- [x] 3. Implement distance calculations


  - Create `DistanceCalculator` object in geometry package
  - Implement `chebyshevDistance()` using max(abs(dx), abs(dy)) formula
  - Implement `distanceInFeet()` multiplying Chebyshev distance by 5
  - Add extension methods to `GridPos` for `distanceTo()` and `distanceToInFeet()`
  - Implement `isWithinRange()` method on GridPos
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 4. Implement neighbor and range queries



  - Implement `neighbors()` method on GridPos returning 8 adjacent positions
  - Implement `neighborsInDirection()` method for directional movement
  - Add Direction extension `toVector()` for coordinate offsets
  - Implement `positionsWithinRange()` in DistanceCalculator
  - Filter out-of-bounds positions from range query results
  - _Requirements: 2.5, 4.1, 4.2, 4.3, 4.5_

- [x] 5. Implement line-of-effect calculations


  - Create `LineOfEffect` object in geometry package
  - Implement `bresenhamLine()` algorithm for path tracing
  - Implement `hasLineOfEffect()` checking obstacles along path
  - Allow line-of-effect through creature-occupied cells
  - Implement `positionsWithinRangeAndLOS()` combining range and LOS checks
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.4_

- [x] 6. Implement AoE template system


  - Create `AoETemplate` sealed interface with `affectedPositions()` method
  - Implement `SphereTemplate` with radius validation and range-based calculation
  - Implement `CubeTemplate` with side length validation and rectangular area
  - Implement `ConeTemplate` with length, direction, and 53-degree approximation
  - Add Direction extension `perpendicular()` for cone width calculation
  - Ensure all templates filter out-of-bounds positions
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 7. Add serialization support



  - Add `@Serializable` annotations to GridPos, MapGrid, CellProperties
  - Add `@SerialName` annotations for stable JSON field names
  - Configure kotlinx-serialization for enum types
  - Test serialization round-trip for all data structures
  - _Requirements: 6.4_

- [x] 8. Write unit tests for core data structures



  - Test GridPos creation, equality, and serialization
  - Test MapGrid initialization with valid and invalid dimensions
  - Test MapGrid bounds checking for various positions
  - Test CellProperties immutability and defaults
  - Test MapGrid cell property get/set operations
  - _Requirements: 1.1, 1.2, 1.3, 1.5_

- [x] 9. Write unit tests for distance calculations





  - Test Chebyshev distance for orthogonal movement (same row/column)
  - Test Chebyshev distance for diagonal movement (45-degree angles)
  - Test Chebyshev distance for mixed movement (knight's move patterns)
  - Test distance-to-feet conversion (multiply by 5)
  - Test isWithinRange for positions at exact range boundary
  - Test neighbor queries for interior, edge, and corner positions
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 10. Write unit tests for line-of-effect







  - Test Bresenham line for horizontal, vertical, and diagonal paths
  - Test Bresenham line for arbitrary angles
  - Test line-of-effect with no obstacles (clear path)
  - Test line-of-effect blocked by single obstacle
  - Test line-of-effect blocked by multiple obstacles
  - Test line-of-effect through creature-occupied cells (allowed)
  - Test positionsWithinRangeAndLOS combining both constraints
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.4_

- [x] 11. Write unit tests for AoE templates




  - Test SphereTemplate with radii 5ft, 10ft, 15ft, 20ft
  - Test SphereTemplate at grid edges and corners
  - Test CubeTemplate with side lengths 5ft, 10ft, 15ft, 20ft
  - Test CubeTemplate centered at various origin positions
  - Test ConeTemplate in all 8 directions (N, NE, E, SE, S, SW, W, NW)
  - Test ConeTemplate with lengths 15ft, 30ft, 60ft
  - Test ConeTemplate width increases with distance
  - Verify all templates exclude out-of-bounds positions
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 12. Write property-based tests




  - Test distance symmetry: distanceTo(a, b) == distanceTo(b, a)
  - Test triangle inequality: distanceTo(a, c) <= distanceTo(a, b) + distanceTo(b, c)
  - Test all positions within range satisfy distance constraint
  - Test Bresenham line always includes start and end points
  - Test AoE templates are deterministic for same inputs
  - _Requirements: 2.1, 2.2, 3.1, 5.6_

- [ ] 13. Write performance tests
  - Benchmark distance calculation (<1μs per operation)
  - Benchmark range query for 30ft on 50x50 grid (<1ms)
  - Benchmark line-of-effect check (<100μs per check)
  - Benchmark AoE template calculation (<1ms for typical sizes)
  - Profile memory allocation for large grid operations
  - _Requirements: 4.5_

- [ ] 14. Integration and validation
  - Verify no Android dependencies in module (check imports)
  - Verify all public APIs are documented with KDoc
  - Run all tests and verify 90%+ coverage
  - Validate serialization works for all data structures
  - Test integration with existing core:domain entities
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
