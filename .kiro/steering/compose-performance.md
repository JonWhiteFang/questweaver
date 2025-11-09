---
inclusion: fileMatch
fileMatchPattern: ['**/*.kt', '**/*.kts']
---

# Compose Performance Guidelines

## Core Principle

**Read state in the latest phase possible**: Composition → Layout → Drawing (cheapest)

Compose has three phases:
1. **Composition** (most expensive): Building UI tree
2. **Layout**: Measuring and positioning
3. **Drawing** (cheapest): Canvas rendering

Delaying state reads reduces recomposition overhead.

## Lambda Modifiers (Defer to Layout Phase)

**Use lambda modifiers for rapidly changing state** (scroll, animation, drag):

```kotlin
// ❌ Triggers recomposition on every scroll
Column(Modifier.offset(y = scroll.value.dp)) { }

// ✅ Skips composition, reads during layout
Column(Modifier.offset { IntOffset(0, scroll.value) }) { }
```

**When to use**: Scroll positions, animation values, drag offsets, camera panning

## derivedStateOf (Reduce Recomposition Frequency)

**Use when computed value changes less frequently than source state**:

```kotlin
// ❌ Recomposes on every scroll event
val showButton = listState.firstVisibleItemIndex > 0

// ✅ Only recomposes when boolean changes
val showButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

**When to use**: Boolean flags from scroll state, filtered lists, computed visibility

## Canvas Drawing (Defer to Draw Phase)

**Read state in Canvas block to skip composition and layout**:

```kotlin
@Composable
fun TacticalMap(state: MapState, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // State reads only trigger redraw, not recomposition
        
        // Draw grid lines (batch into single Path)
        val gridPath = Path().apply {
            for (x in 0..state.w) {
                moveTo(x * state.tileSize, 0f)
                lineTo(x * state.tileSize, size.height)
            }
        }
        drawPath(gridPath, Color.Gray, style = Stroke(1f))
        
        // Draw tokens
        state.tokens.forEach { token ->
            drawCircle(
                color = if (token.isEnemy) Color.Red else Color.Blue,
                radius = state.tileSize * 0.35f,
                center = Offset(
                    token.pos.x * state.tileSize + state.tileSize / 2,
                    token.pos.y * state.tileSize + state.tileSize / 2
                )
            )
        }
    }
}
```

**Critical rules**:
- Batch draw operations (use Path for multiple lines)
- No allocations in draw lambda
- Target <4ms per frame (60fps)
- Cache computed values between frames when possible

## State Design Best Practices

**Use flat, immutable structures**:

```kotlin
// ✅ Flat, immutable
data class MapState(
    val w: Int,
    val h: Int,
    val tileSize: Float,
    val tokens: List<Token>,
    val blocked: Set<GridPos>
)

// ❌ Nested, mutable
data class MapState(
    val grid: Grid, // Contains mutable state
    val entities: MutableList<Entity>
)
```

**Always use stable keys for lists**:

```kotlin
LazyColumn {
    items(
        items = creatures,
        key = { it.id } // Prevents unnecessary recomposition
    ) { creature ->
        CreatureCard(creature)
    }
}
```

## Common Mistakes to Avoid

```kotlin
// ❌ Reading state in wrong phase
val offset = with(LocalDensity.current) { scroll.value.toDp() }
Column(Modifier.offset(y = offset)) { }

// ❌ Heavy computation in composition
val processed = data.map { expensiveTransform(it) } // Recalculates every time

// ✅ Memoize with remember
val processed = remember(data) { data.map { expensiveTransform(it) } }

// ❌ Unnecessary recomposition from animation
var time by remember { mutableStateOf(System.currentTimeMillis()) }
LaunchedEffect(Unit) {
    while (true) {
        time = System.currentTimeMillis() // Triggers recomposition every frame
        delay(16)
    }
}
```

## Profiling & Debugging

**Layout Inspector** (Android Studio → View → Tool Windows → Layout Inspector):
- Shows recomposition counts
- Highlights slow composables
- Displays composition skips

**Compose Compiler Metrics**:
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

**Systrace/Perfetto**: Capture with `adb shell am start -n com.android.traceur/.MainActivity`, analyze at https://ui.perfetto.dev/

## QuestWeaver-Specific Patterns

**Tactical Map**:
- Read `MapState` in Canvas draw block
- Use `Modifier.offset { }` for camera panning
- Batch grid lines into single Path
- Cache token positions between frames

**Turn Order UI**:
- Use `LazyColumn` with stable keys (creature.id)
- Use `derivedStateOf` for "is active creature" checks
- Animate active indicator in draw phase with `graphicsLayer`

**Dice Roller Animation**:
- Use `animateFloatAsState` for rotation
- Read animated value in `Modifier.graphicsLayer { rotationZ = animatedValue }`
- Avoid recomposing result text on every frame

**Combat Log**:
- Use `LazyColumn` with `reverseLayout = true`
- Provide stable keys (event.id)
- Limit visible items for performance
