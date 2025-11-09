---
inclusion: fileMatch
fileMatchPattern: '**/*.kt'
---

# Jetpack Compose Performance Guidelines

## State Reading Phases

Compose has three phases where state can be read, each with different performance implications:

1. **Composition**: Building the UI tree (most expensive)
2. **Layout**: Measuring and positioning elements
3. **Drawing**: Rendering to canvas (least expensive)

**Rule**: Read state in the latest phase possible to minimize recomposition overhead.

## Lambda Modifiers for Frequently Changing State

### Problem: Direct State Reads Trigger Full Recomposition
```kotlin
// ❌ Bad: Triggers recomposition on every scroll
Box {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.offset(y = scroll.value.dp)
    ) {
        // Content recomposes on every scroll event
    }
}
```

### Solution: Lambda Modifiers Read State During Layout
```kotlin
// ✅ Good: Skips composition, goes straight to layout
Box {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.offset { 
            IntOffset(0, scroll.value) 
        }
    ) {
        // Content doesn't recompose on scroll
    }
}
```

**When to use**: Scroll state, animation values, drag positions, or any rapidly changing state.

## derivedStateOf for Computed State

### Problem: Recomposes on Every Source Change
```kotlin
// ❌ Bad: Recomposes on every scroll event
val listState = rememberLazyListState()
val showButton = listState.firstVisibleItemIndex > 0

AnimatedVisibility(visible = showButton) {
    ScrollToTopButton()
}
```

### Solution: Only Recomposes When Derived Value Changes
```kotlin
// ✅ Good: Only recomposes when boolean changes
val listState = rememberLazyListState()
val showButton by remember {
    derivedStateOf {
        listState.firstVisibleItemIndex > 0
    }
}

AnimatedVisibility(visible = showButton) {
    ScrollToTopButton()
}
```

**When to use**: Computed values that change less frequently than their source state.

## Canvas Drawing Optimization

### Read State During Draw Phase
```kotlin
@Composable
fun TacticalMap(state: MapState, modifier: Modifier = Modifier) {
    // State changes only trigger redraw, not recomposition
    Canvas(modifier) {
        // Draw grid
        for (x in 0..state.w) {
            drawLine(
                start = Offset(x * state.tileSize, 0f),
                end = Offset(x * state.tileSize, size.height),
                color = Color.Gray,
                strokeWidth = 1f
            )
        }
        
        // Draw tokens
        state.tokens.forEach { token ->
            val center = Offset(
                token.pos.x * state.tileSize + state.tileSize / 2,
                token.pos.y * state.tileSize + state.tileSize / 2
            )
            drawCircle(
                color = if (token.isEnemy) Color.Red else Color.Blue,
                radius = state.tileSize * 0.35f,
                center = center
            )
        }
    }
}
```

### Batch Draw Operations
```kotlin
// ✅ Good: Single drawPath call
val path = Path().apply {
    for (x in 0..gridWidth) {
        moveTo(x * tileSize, 0f)
        lineTo(x * tileSize, height)
    }
}
drawPath(path, color = Color.Gray, style = Stroke(width = 1f))

// ❌ Bad: Multiple drawLine calls
for (x in 0..gridWidth) {
    drawLine(/* ... */)
}
```

## Map Rendering Best Practices

### Minimize State Objects
```kotlin
// ✅ Good: Flat structure
data class MapState(
    val w: Int,
    val h: Int,
    val tileSize: Float,
    val tokens: List<Token>,
    val blocked: Set<GridPos>
)

// ❌ Bad: Nested mutable state
data class MapState(
    val grid: Grid, // Contains mutable state
    val entities: MutableList<Entity> // Mutable
)
```

### Use Immutable Collections
```kotlin
// ✅ Good: Immutable list
data class MapState(
    val tokens: List<Token> = emptyList()
)

// ❌ Bad: Mutable list
data class MapState(
    val tokens: MutableList<Token> = mutableListOf()
)
```

### Key for List Items
```kotlin
LazyColumn {
    items(
        items = creatures,
        key = { it.id } // Stable key prevents unnecessary recomposition
    ) { creature ->
        CreatureCard(creature)
    }
}
```

## Performance Checklist

### For Map/Canvas Components
- [ ] State reads happen in Canvas block (draw phase)
- [ ] Draw operations are batched where possible
- [ ] No allocations in draw lambda
- [ ] Target <4ms per frame (60fps)

### For Scrolling/Animation
- [ ] Use lambda modifiers (`Modifier.offset { }`)
- [ ] Avoid reading scroll state in composition
- [ ] Use `derivedStateOf` for computed scroll-based values

### For Lists
- [ ] Use `LazyColumn/Row` for large lists
- [ ] Provide stable `key` for items
- [ ] Keep item composables small and focused
- [ ] Avoid heavy computation in item composables

### General
- [ ] Use `remember` for expensive calculations
- [ ] Use `derivedStateOf` for computed state
- [ ] Minimize state object nesting
- [ ] Use immutable collections
- [ ] Profile with Compose Layout Inspector

## Profiling Tools

### Layout Inspector
```
Android Studio → View → Tool Windows → Layout Inspector
```
- Shows recomposition counts
- Highlights slow composables
- Displays composition skips

### Compose Compiler Metrics
```kotlin
// build.gradle.kts
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$projectDir/compose-metrics"
        )
    }
}
```

### Systrace/Perfetto
```bash
# Capture trace
adb shell am start -n com.android.traceur/.MainActivity

# Analyze with Perfetto
# https://ui.perfetto.dev/
```

## Common Pitfalls

### ❌ Reading State in Wrong Phase
```kotlin
// Bad: Reads scroll state during composition
val offset = with(LocalDensity.current) { scroll.value.toDp() }
Column(Modifier.offset(y = offset)) { }
```

### ❌ Unnecessary Recomposition
```kotlin
// Bad: Recomposes on every frame
var time by remember { mutableStateOf(System.currentTimeMillis()) }
LaunchedEffect(Unit) {
    while (true) {
        time = System.currentTimeMillis()
        delay(16)
    }
}
```

### ❌ Heavy Computation in Composition
```kotlin
// Bad: Expensive calculation on every recomposition
@Composable
fun MyScreen(data: List<Item>) {
    val processed = data.map { expensiveTransform(it) } // Recalculates every time
}

// Good: Memoize with remember
@Composable
fun MyScreen(data: List<Item>) {
    val processed = remember(data) { 
        data.map { expensiveTransform(it) } 
    }
}
```

## QuestWeaver-Specific Guidelines

### Tactical Map
- Read `MapState` in Canvas draw block
- Use `Modifier.offset { }` for camera panning
- Batch grid line drawing into single Path
- Cache token positions between frames

### Turn Order UI
- Use `LazyColumn` with stable keys (creature ID)
- Use `derivedStateOf` for "is active creature" checks
- Animate active creature indicator in draw phase

### Dice Roller Animation
- Use `animateFloatAsState` for rotation
- Read animated value in `Modifier.graphicsLayer { }`
- Avoid recomposing dice result text on every frame

### Combat Log
- Use `LazyColumn` with `reverseLayout = true`
- Provide stable keys (event ID)
- Limit visible items with `maxItemsInViewport`
