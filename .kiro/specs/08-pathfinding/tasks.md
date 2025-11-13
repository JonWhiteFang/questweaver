# Implementation Plan

- [x] 1. Set up pathfinding module structure




  - Create `core/domain/map/pathfinding` package
  - Define `PathResult` sealed interface with Success, NoPathFound, ExceedsMovementBudget variants
  - Define `Pathfinder` interface with `findPath()` method signature
  - Define `MovementCostCalculator` interface with `calculateCost()` method
  - _Requirements: 1.1, 1.3, 7.1, 7.2, 7.3, 7.5_

- [x] 2. Implement movement cost calculator





  - Create `DefaultMovementCostCalculator` implementing MovementCostCalculator
  - Implement cost calculation: NORMAL terrain = 1, DIFFICULT = 2, IMPASSABLE = Int.MAX_VALUE
  - Add validation for grid bounds and cell properties
  - Ensure pure function with no side effects
  - _Requirements: 3.1, 3.2, 3.3, 5.5_

- [x] 3. Implement A* pathfinding core algorithm


  - Create `AStarPathfinder` class implementing Pathfinder interface
  - Implement priority queue for open set using PathNode with fScore ordering
  - Implement closed set using HashSet for visited positions
  - Implement gScore map for tracking cost from start to each position
  - Implement cameFrom map for path reconstruction
  - _Requirements: 1.2, 5.1, 5.5, 6.2_


- [x] 4. Implement A* heuristic and node comparison

  - Create `PathNode` data class with position, gScore, fScore
  - Implement Comparable interface with deterministic tie-breaking (fScore, then gScore, then position)
  - Implement heuristic function using Chebyshev distance from grid geometry
  - Ensure consistent ordering for deterministic behavior
  - _Requirements: 1.2, 5.1, 5.2, 5.3, 5.4_


- [x] 5. Implement A* main search loop

  - Implement input validation (bounds checking for start and destination)
  - Initialize open set with start node
  - Implement main loop: poll lowest fScore node, check if destination reached
  - Implement neighbor exploration with cost calculation
  - Implement early termination when destination is reached
  - _Requirements: 1.1, 1.2, 1.5, 6.1, 6.3_


- [x] 6. Implement obstacle detection and traversability


  - Create `isTraversable()` helper method checking cell properties
  - Block movement through IMPASSABLE terrain and obstacles
  - Block movement through occupied cells (except destination)
  - Allow destination to be occupied for attack movement
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 7. Implement path reconstruction


  - Create `reconstructPath()` method using cameFrom map
  - Build path from destination back to start
  - Reverse path to get start-to-destination order
  - Include both start and destination in returned path
  - _Requirements: 1.1, 1.4_

- [x] 8. Implement movement budget validation


  - Add maxCost parameter to findPath() method
  - Check tentative gScore against maxCost during search
  - Skip neighbors that would exceed budget
  - Return ExceedsMovementBudget result when path found but too expensive
  - _Requirements: 4.1, 4.2, 4.4_

- [x] 9. Implement path validator


  - Create `PathValidator` object with validation methods
  - Implement `isValidPath()` checking bounds and adjacency
  - Implement `calculatePathCost()` summing cell costs along path
  - Implement `isWithinBudget()` comparing path cost to budget
  - _Requirements: 1.5, 4.1, 4.2_

- [x] 10. Implement reachability calculator


  - Create `ReachabilityCalculator` class with BFS-based algorithm
  - Implement `findReachablePositions()` using movement budget
  - Use queue to explore positions within budget
  - Track visited positions to avoid duplicates
  - Account for terrain costs when calculating reachability
  - _Requirements: 4.3, 4.4, 4.5_

- [x] 11. Implement exact cost position finder


  - Add `findPositionsAtCost()` method to ReachabilityCalculator
  - Filter reachable positions to those with exact cost match
  - Use pathfinder to verify actual path cost
  - Return set of positions at exact cost
  - _Requirements: 4.3, 4.5_

- [x] 12. Write unit tests for basic pathfinding



  - Test straight line paths (horizontal, vertical, diagonal)
  - Test path around single obstacle
  - Test path around multiple obstacles
  - Test no path exists when completely blocked
  - Test start equals destination (trivial path)
  - _Requirements: 1.1, 1.2, 1.3, 2.3_

- [x] 13. Write unit tests for movement costs


  - Test path through normal terrain (cost 1 per cell)
  - Test path through difficult terrain (cost 2 per cell)
  - Test path through mixed terrain
  - Test pathfinder prefers lower cost path when alternatives exist
  - Test diagonal movement cost equals orthogonal per D&D 5e
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 14. Write unit tests for obstacles and occupancy


  - Test obstacle blocks path
  - Test IMPASSABLE terrain blocks path
  - Test occupied cell blocks intermediate path
  - Test occupied destination is allowed
  - Test pathfinding around occupied cells
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 15. Write unit tests for movement budget


  - Test path within budget returns Success
  - Test path exceeding budget returns ExceedsMovementBudget
  - Test path at exact budget boundary
  - Test reachable positions all within budget
  - Test reachable positions exclude those beyond budget
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 16. Write unit tests for path validation

  - Test isValidPath with connected path
  - Test isValidPath rejects non-adjacent steps
  - Test isValidPath rejects out-of-bounds positions
  - Test calculatePathCost sums terrain costs correctly
  - Test isWithinBudget compares cost to budget
  - _Requirements: 1.4, 1.5, 4.1, 4.2_

- [x] 17. Write unit tests for edge cases


  - Test pathfinding on grid boundaries
  - Test pathfinding on maximum grid size (100x100)
  - Test empty path when start is blocked
  - Test path includes start and destination
  - Test each step moves to adjacent cell
  - _Requirements: 1.1, 1.4, 1.5_

- [x] 18. Write property-based tests for determinism


  - Test same inputs produce identical paths across multiple runs
  - Test path is always connected (adjacent steps)
  - Test path cost matches sum of cell costs
  - Test all reachable positions are within budget
  - Test PathNode comparison is transitive and consistent
  - _Requirements: 5.1, 5.2, 5.3, 5.4_


- [x] 19. Write performance tests


  - Benchmark pathfinding on 50x50 grid (<10ms)
  - Benchmark reachability calculation with 30ft movement (<5ms)
  - Benchmark path validation (<1ms)
  - Test performance scales appropriately with grid size
  - Profile memory allocation and optimize if needed
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 20. Integration and validation
  - Verify no Android dependencies (check imports)
  - Verify all public APIs have KDoc documentation
  - Run all tests and verify 90%+ coverage
  - Test integration with grid geometry from spec 07
  - Validate PathResult serialization if needed for events
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
