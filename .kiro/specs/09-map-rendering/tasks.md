# Implementation Plan

- [x] 1. Set up feature:map module structure


  - Create `feature/map` module with Android library configuration
  - Add dependencies: core:domain, Compose UI, Compose Canvas
  - Create package structure: ui/, render/, viewmodel/, util/
  - Add module to settings.gradle.kts and app dependencies
  - _Requirements: 1.5, 8.1_


- [x] 2. Define MVI state and intent types

  - Create `MapRenderState` data class with grid, tokens, overlays, camera, zoom
  - Create `TokenRenderData` data class with creature data and HP visibility logic
  - Create `Allegiance` enum (FRIENDLY, ENEMY, NEUTRAL)
  - Create `RangeOverlayData` and `RangeType` for range visualization
  - Create `AoEOverlayData` for AoE visualization
  - Create `MapIntent` sealed interface with CellTapped, TokenTapped, Pan, Zoom
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 8.2, 8.3_


- [x] 3. Implement coordinate conversion utilities

  - Create `CoordinateConverter` object in util package
  - Implement `gridToScreen()` converting GridPos to screen Offset with zoom and camera
  - Implement `screenToGrid()` converting screen Offset to GridPos with bounds checking
  - Add helper functions for cell center calculations
  - Test coordinate conversion with various zoom levels and camera offsets
  - _Requirements: 7.1, 7.4_

- [x] 4. Implement grid rendering



  - Create `GridRenderer.kt` with DrawScope extension functions
  - Implement `drawGrid()` rendering all cells with terrain colors
  - Use distinct colors: normal (light gray), difficult (brown), impassable (black), obstacle (dark gray)
  - Draw cell borders with 1px stroke
  - Support configurable cell size (default 60dp)
  - Implement viewport culling to only render visible cells
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 6.3, 6.4_

- [x] 5. Implement token rendering


  - Create `TokenRenderer.kt` with DrawScope extension functions
  - Implement `drawTokens()` rendering circular tokens at creature positions
  - Use allegiance-based colors: friendly (blue), enemy (red), neutral (yellow)
  - Render HP numbers for friendly creatures only
  - Render bloodied indicator (small red circle) for non-friendly creatures under half HP
  - Draw HP ring around token showing health percentage
  - Scale token size to 80% of cell size
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

- [x] 6. Implement movement path rendering


  - Create `PathRenderer.kt` with DrawScope extension functions
  - Implement `drawMovementPath()` connecting path positions with lines
  - Use semi-transparent yellow color for path visibility
  - Draw lines between cell centers along the path
  - Use 4px stroke width for path lines
  - Render path above grid but below tokens
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 7. Implement range overlay rendering


  - Create `OverlayRenderer.kt` with DrawScope extension functions
  - Implement `drawRangeOverlay()` highlighting positions within range
  - Use semi-transparent colors: movement (blue), weapon (red), spell (magenta)
  - Set alpha to 0.3 for overlay transparency
  - Render overlay above grid but below paths
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 8. Implement AoE overlay rendering



  - Add `drawAoEOverlay()` to OverlayRenderer
  - Render affected positions with semi-transparent red (alpha 0.4)
  - Support sphere, cube, and cone template visualizations
  - Render AoE above range overlays but below tokens
  - Add directional indicator for cone templates if needed
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 9. Implement selection highlight rendering


  - Add `drawSelectionHighlight()` function
  - Draw white border around selected cell with 3px stroke
  - Render selection above all other layers
  - Update highlight when selected position changes
  - _Requirements: 7.5_

- [x] 10. Create main TacticalMapCanvas Composable


  - Create `TacticalMapCanvas.kt` in ui package
  - Implement Canvas Composable with fillMaxSize modifier
  - Call rendering functions in correct layer order: grid, range, AoE, path, tokens, selection
  - Pass state and onIntent callback as parameters
  - Keep Composable stateless with state hoisting
  - _Requirements: 1.5, 8.2, 8.5_

- [x] 11. Implement tap gesture handling


  - Add pointerInput modifier with detectTapGestures
  - Convert tap screen coordinates to GridPos using screenToGrid()
  - Emit MapIntent.CellTapped with selected position
  - Check if tap is on a token and emit MapIntent.TokenTapped if so
  - Provide visual feedback for tapped cells
  - _Requirements: 7.1, 7.2, 7.5_

- [x] 12. Implement pan and zoom gestures



  - Add pointerInput modifier with detectTransformGestures
  - Emit MapIntent.Pan with delta offset for camera movement
  - Emit MapIntent.Zoom with scale factor for zoom changes
  - Maintain grid alignment during zoom operations
  - Clamp zoom level between 0.5x and 3.0x
  - _Requirements: 7.3, 7.4_

- [x] 13. Create MapViewModel with MVI pattern


  - Create `MapViewModel` class extending ViewModel
  - Define private MutableStateFlow for MapRenderState
  - Expose public StateFlow for state observation
  - Implement `handle()` method for MapIntent processing
  - Implement handlers for CellTapped, TokenTapped, Pan, Zoom intents
  - Update state immutably using copy() and update()
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 14. Implement range overlay logic in ViewModel

  - Add `showMovementRange()` method using ReachabilityCalculator
  - Add `showWeaponRange()` method using DistanceCalculator
  - Add `showSpellRange()` method with line-of-effect checking
  - Update MapRenderState with RangeOverlayData when range requested
  - Clear overlay when selection changes
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 15. Implement AoE preview logic in ViewModel

  - Add `showAoEPreview()` method accepting AoETemplate and origin
  - Calculate affected positions using template.affectedPositions()
  - Update MapRenderState with AoEOverlayData
  - Support sphere, cube, and cone templates
  - Clear AoE overlay when preview dismissed
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 16. Add Koin DI module for feature:map



  - Create `MapModule.kt` with Koin module definition
  - Register MapViewModel as viewModel scope
  - Inject Pathfinder and ReachabilityCalculator dependencies
  - Register module in app-level Koin configuration
  - _Requirements: 8.1_

- [x] 17. Write unit tests for coordinate conversion


  - Test gridToScreen with various zoom levels (0.5x, 1x, 2x, 3x)
  - Test gridToScreen with camera offsets
  - Test screenToGrid with valid positions
  - Test screenToGrid returns null for out-of-bounds positions
  - Test round-trip conversion (grid → screen → grid)
  - _Requirements: 7.1, 7.4_


- [x] 18. Write unit tests for MapViewModel

  - Test CellTapped intent updates selectedPosition
  - Test Pan intent updates cameraOffset
  - Test Zoom intent updates zoomLevel with clamping
  - Test showMovementRange updates rangeOverlay
  - Test showAoEPreview updates aoeOverlay
  - Test state updates are immutable
  - _Requirements: 8.2, 8.3, 8.4_


- [x] 19. Write unit tests for token rendering logic


  - Test TokenRenderData.showHP is true for friendly creatures
  - Test TokenRenderData.showHP is false for non-friendly creatures
  - Test isBloodied is true when HP < 50%
  - Test hpPercentage calculation
  - Test allegiance color mapping
  - _Requirements: 2.2, 2.3, 2.4, 2.5_

- [x] 20. Write Compose UI tests


  - Test TacticalMapCanvas renders without crashing
  - Test tap gesture emits MapIntent.CellTapped
  - Test pan gesture emits MapIntent.Pan
  - Test state changes trigger recomposition
  - Test tokens render at correct positions
  - _Requirements: 7.1, 7.2, 7.3, 8.5_

- [x] 21. Write performance tests






  - Benchmark grid rendering for 50x50 grid (target ≤4ms)
  - Benchmark full frame render with tokens and overlays (target ≤4ms)
  - Test frame rate during pan/zoom gestures (target 60fps)
  - Profile memory allocation per frame
  - Use Android Profiler to analyze overdraw
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 22. Optimize rendering performance
  - Implement viewport culling to skip off-screen cells
  - Batch similar draw calls (all cells, all tokens)
  - Use remember() for expensive calculations
  - Use derivedStateOf for computed rendering data
  - Profile and optimize hot paths identified in performance tests
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 23. Integration and validation
  - Verify module dependencies follow architecture rules (feature:map → core:domain only)
  - Test integration with grid geometry from spec 07
  - Test integration with pathfinding from spec 08
  - Verify all public APIs have KDoc documentation
  - Run all tests and verify 60%+ coverage
  - Test on physical device for performance validation
  - _Requirements: 1.1, 1.2, 1.3, 6.1, 6.2, 8.1_
