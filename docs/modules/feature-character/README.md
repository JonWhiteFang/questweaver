# feature:character

**Character sheets and party management**

## Purpose

The `feature:character` module provides UI for viewing and managing player characters and party members. It displays character sheets with stats, abilities, inventory, and spells, and allows players to manage their party composition.

## Responsibilities

- Display character sheets with full stats
- Show ability scores, skills, and modifiers
- Display inventory and equipment
- Show spell lists and spell slots
- Manage party composition
- Import/export character data
- Character creation and leveling (future)

## Key Classes and Interfaces

### UI Components (Placeholder)

- `CharacterSheetScreen`: Main character sheet display
- `PartyScreen`: Party overview and management
- `AbilityScoresPanel`: Display ability scores and modifiers
- `InventoryPanel`: Equipment and inventory display
- `SpellsPanel`: Spell list and spell slot tracking

### ViewModels (Placeholder)

- `CharacterViewModel`: MVI ViewModel for character state
- `PartyViewModel`: MVI ViewModel for party management
- `CharacterState`: Immutable character display state
- `CharacterIntent`: Sealed interface for character actions

## Dependencies

### Production

- `core:domain`: Domain entities (Creature, Character, Party)
- `compose-ui`: Jetpack Compose UI
- `compose-material3`: Material3 components
- `kotlinx-coroutines-android`: Coroutines for Android

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `mockk`: Mocking library

## Module Rules

### ✅ Allowed

- Compose UI for character screens
- Dependencies on `core:domain` only
- Character display and management
- Import/export functionality

### ❌ Forbidden

- Dependencies on other feature modules
- Business logic (belongs in `core:domain` or `core:rules`)
- Direct database access (use repositories)
- Combat logic (belongs in `feature:encounter`)

## Architecture Patterns

### MVI Pattern

```kotlin
data class CharacterState(
    val character: Creature,
    val abilityScores: Abilities,
    val skills: Map<String, Int>,
    val inventory: List<Item>,
    val spells: List<Spell>,
    val spellSlots: Map<Int, SpellSlotInfo>
)

sealed interface CharacterIntent {
    data class LoadCharacter(val characterId: Long) : CharacterIntent
    data class UpdateAbilityScore(val ability: String, val value: Int) : CharacterIntent
    data class AddItem(val item: Item) : CharacterIntent
    data class RemoveItem(val itemId: Long) : CharacterIntent
    data class PrepareSpell(val spellId: String) : CharacterIntent
}

class CharacterViewModel(
    private val characterRepo: CharacterRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CharacterState())
    val state: StateFlow<CharacterState> = _state.asStateFlow()
    
    fun handle(intent: CharacterIntent) {
        viewModelScope.launch {
            when (intent) {
                is CharacterIntent.LoadCharacter -> loadCharacter(intent.characterId)
                is CharacterIntent.UpdateAbilityScore -> updateAbility(intent.ability, intent.value)
                is CharacterIntent.AddItem -> addItem(intent.item)
                is CharacterIntent.RemoveItem -> removeItem(intent.itemId)
                is CharacterIntent.PrepareSpell -> prepareSpell(intent.spellId)
            }
        }
    }
}
```

### State Hoisting

Keep Composables stateless:

```kotlin
@Composable
fun CharacterSheetScreen(
    state: CharacterState,
    onIntent: (CharacterIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        CharacterHeader(state.character)
        AbilityScoresPanel(state.abilityScores)
        SkillsPanel(state.skills)
        InventoryPanel(state.inventory, onIntent)
        SpellsPanel(state.spells, state.spellSlots, onIntent)
    }
}
```

## Testing Approach

### Unit Tests

- Test ViewModel state transitions
- Test character data loading
- Test inventory management
- Test spell preparation logic

### Coverage Target

**60%+** code coverage (focus on logic, not UI rendering)

### Example Test

```kotlin
class CharacterViewModelTest : FunSpec({
    test("load character updates state") {
        val repo = mockk<CharacterRepository>()
        val character = Fixtures.mockCreature(id = 1, name = "Gandalf")
        coEvery { repo.getById(1) } returns character
        
        val viewModel = CharacterViewModel(repo)
        viewModel.handle(CharacterIntent.LoadCharacter(1))
        
        viewModel.state.value.character shouldBe character
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :feature:character:build

# Run tests
./gradlew :feature:character:test

# Run tests with coverage
./gradlew :feature:character:test koverHtmlReport
```

## Package Structure

```
dev.questweaver.feature.character/
├── ui/
│   ├── CharacterSheetScreen.kt
│   ├── PartyScreen.kt
│   ├── AbilityScoresPanel.kt
│   ├── InventoryPanel.kt
│   └── SpellsPanel.kt
├── viewmodel/
│   ├── CharacterViewModel.kt
│   └── PartyViewModel.kt
└── di/
    └── CharacterModule.kt
```

## Integration Points

### Consumed By

- `app` (navigation to character screens)

### Depends On

- `core:domain` (Character, Creature, Party entities)

## Features

### Current (Placeholder)

- Character sheet display
- Party overview

### Planned

- Full character sheet with all D&D 5e stats
- Ability score display with modifiers
- Skills with proficiency indicators
- Inventory and equipment management
- Spell list with prepared spells
- Spell slot tracking
- Character creation wizard
- Leveling up
- Import/export character data (JSON)
- Character portraits

## UI/UX Considerations

- **Tabs**: Use tabs for different sections (Stats, Inventory, Spells)
- **Scrolling**: Long character sheets should scroll smoothly
- **Editing**: Clear distinction between view and edit modes
- **Calculations**: Auto-calculate modifiers and derived stats
- **Validation**: Validate character data on input

## Character Sheet Sections

### Header
- Name, race, class, level
- Current/max HP
- Armor class
- Initiative modifier

### Ability Scores
- STR, DEX, CON, INT, WIS, CHA
- Modifiers calculated automatically
- Saving throw proficiencies

### Skills
- All D&D 5e skills
- Proficiency indicators
- Total modifiers

### Combat Stats
- Attack bonus
- Damage dice
- Spell save DC
- Spell attack bonus

### Inventory
- Equipment slots (armor, weapons, accessories)
- Carried items
- Weight tracking
- Currency

### Spells
- Spell list by level
- Prepared spells
- Spell slots (current/max)
- Spell descriptions

## Notes

- This module is display-focused - no combat logic
- All calculations should use `core:rules` if needed
- Keep character data in `core:domain` entities
- Use repository pattern for data access
- Support import/export for character portability

---

**Last Updated**: 2025-11-10
