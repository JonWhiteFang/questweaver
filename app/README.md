# app

**Android application module - DI assembly, navigation, and theme**

## Purpose

The `app` module is the main Android application module that assembles all other modules together. It handles dependency injection setup with Koin, navigation between screens, Material3 theming, and application-level configuration.

## Responsibilities

- Application entry point (MainActivity)
- Dependency injection assembly with Koin
- Navigation graph and routing
- Material3 theme configuration
- Application-level configuration
- Splash screen and initialization
- Deep linking (future)
- App-level error handling

## Key Classes and Interfaces

### Application (Placeholder)

- `QuestWeaverApplication`: Application class with Koin setup
- `MainActivity`: Main activity with Compose setContent

### Navigation (Placeholder)

- `NavGraph`: Navigation graph definition
- `Screen`: Sealed class for screen destinations
- `Navigator`: Navigation helper

### Theme (Placeholder)

- `QuestWeaverTheme`: Material3 theme Composable
- `Color`: Color palette definition
- `Typography`: Typography scale
- `Shapes`: Shape definitions

### DI (Placeholder)

- `AppModule`: Koin module for app-level dependencies
- Module aggregation from all feature modules

## Dependencies

### Production

- All project modules:
  - `core:domain`
  - `core:data`
  - `core:rules`
  - `feature:map`
  - `feature:encounter`
  - `feature:character`
  - `ai:ondevice`
  - `ai:gateway`
  - `sync:firebase`

- Android & Compose:
  - `compose-bom`: Compose Bill of Materials
  - `compose-ui`: Compose UI
  - `compose-material3`: Material3 components
  - `compose-ui-tooling-preview`: Preview support
  - `activity-compose`: Compose Activity integration
  - `navigation-compose`: Compose Navigation

- DI:
  - `koin-android`: Koin for Android
  - `koin-androidx-compose`: Koin Compose integration

### Debug

- `compose-ui-tooling`: Compose tooling for debug builds

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `mockk`: Mocking library

## Module Rules

### ✅ Allowed

- Dependencies on ALL other modules
- Application-level configuration
- Navigation setup
- Theme definition
- DI assembly

### ❌ Forbidden

- Business logic (belongs in `core:domain` or `core:rules`)
- Feature-specific UI (belongs in `feature:*`)
- Data persistence (belongs in `core:data`)

## Architecture Patterns

### Application Setup

```kotlin
class QuestWeaverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@QuestWeaverApplication)
            modules(
                appModule,
                domainModule,
                dataModule,
                rulesModule,
                mapModule,
                encounterModule,
                characterModule,
                onDeviceAIModule,
                gatewayModule,
                syncModule
            )
        }
    }
}
```

### MainActivity

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            QuestWeaverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }
}
```

### Navigation

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CharacterSheet : Screen("character/{characterId}") {
        fun createRoute(characterId: Long) = "character/$characterId"
    }
    object Encounter : Screen("encounter/{encounterId}") {
        fun createRoute(encounterId: Long) = "encounter/$encounterId"
    }
    object TacticalMap : Screen("map/{encounterId}") {
        fun createRoute(encounterId: Long) = "map/$encounterId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCharacter = { id ->
                    navController.navigate(Screen.CharacterSheet.createRoute(id))
                },
                onNavigateToEncounter = { id ->
                    navController.navigate(Screen.Encounter.createRoute(id))
                }
            )
        }
        
        composable(
            route = Screen.CharacterSheet.route,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getLong("characterId") ?: return@composable
            CharacterSheetScreen(characterId = characterId)
        }
        
        // More screens...
    }
}
```

### Theme

```kotlin
@Composable
fun QuestWeaverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5)
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

## Testing Approach

### Unit Tests

- Test navigation logic
- Test DI module configuration
- Test ViewModel integration

### UI Tests

- Test navigation flows
- Test theme application
- Test critical user journeys

### Coverage Target

**60%+** code coverage

### Example Test

```kotlin
class NavigationTest : FunSpec({
    test("navigates to character sheet") {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        
        composeTestRule.setContent {
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            NavGraph(navController = navController)
        }
        
        navController.navigate(Screen.CharacterSheet.createRoute(1))
        
        navController.currentBackStackEntry?.destination?.route shouldBe Screen.CharacterSheet.route
    }
})
```

## Building and Testing

```bash
# Build app
./gradlew :app:assembleDebug

# Install on device
./gradlew :app:installDebug

# Run tests
./gradlew :app:test

# Run UI tests
./gradlew :app:connectedAndroidTest
```

## Package Structure

```
dev.questweaver/
├── QuestWeaverApplication.kt
├── MainActivity.kt
├── ui/
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       ├── Type.kt
│       └── Shape.kt
└── di/
    └── AppModule.kt
```

## Integration Points

### Consumes

- ALL other modules in the project

### Provides

- Application context
- Navigation controller
- Theme configuration
- DI container

## Configuration

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.INTERNET" />
    
    <application
        android:name=".QuestWeaverApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.QuestWeaver">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.QuestWeaver">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### build.gradle.kts

Key configurations:
- `applicationId`: "dev.questweaver"
- `compileSdk`: 34
- `minSdk`: 26
- `targetSdk`: 34
- `versionCode`: 1
- `versionName`: "1.0.0"

## Dependency Injection

### Module Assembly

```kotlin
val appModule = module {
    single { AndroidContext(androidContext()) }
    single { Navigator() }
}

// In Application.onCreate()
startKoin {
    androidLogger()
    androidContext(this@QuestWeaverApplication)
    modules(
        appModule,
        domainModule,      // from core:domain
        dataModule,        // from core:data
        rulesModule,       // from core:rules
        mapModule,         // from feature:map
        encounterModule,   // from feature:encounter
        characterModule,   // from feature:character
        onDeviceAIModule,  // from ai:ondevice
        gatewayModule,     // from ai:gateway
        syncModule         // from sync:firebase
    )
}
```

## ProGuard Rules

Located in `app/proguard-rules.pro`:

```proguard
# Keep kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# Keep Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Koin modules
-keep class org.koin.** { *; }
```

## Assets

### Models (ai:ondevice)

Place ONNX models in `app/src/main/assets/models/`:
- `intent.onnx` - Intent classification model
- `tokenizer.json` - Tokenizer configuration

### Resources

- `res/values/strings.xml` - UI strings
- `res/values/themes.xml` - Material3 theme
- `res/drawable/` - Vector drawables
- `res/mipmap/` - App icons

## Performance Considerations

- **Startup Time**: Minimize work in Application.onCreate()
- **DI**: Use lazy injection where possible
- **Navigation**: Use single activity architecture
- **Theme**: Use Material3 dynamic colors on Android 12+

## Notes

- This module assembles all other modules
- Keep business logic out of this module
- Use Koin for dependency injection
- Follow single activity architecture with Compose Navigation
- Configure ProGuard rules for release builds
- Place ONNX models in assets directory

---

**Last Updated**: 2025-11-10
