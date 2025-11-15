package dev.questweaver.feature.encounter.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import dev.questweaver.feature.encounter.viewmodel.EncounterViewModel
import dev.questweaver.feature.encounter.usecases.InitializeEncounter
import dev.questweaver.feature.encounter.usecases.ProcessPlayerAction
import dev.questweaver.feature.encounter.usecases.AdvanceTurn
import dev.questweaver.feature.encounter.state.EncounterStateBuilder
import dev.questweaver.feature.encounter.state.CompletionDetector
import dev.questweaver.feature.encounter.state.UndoRedoManager

/**
 * Koin dependency injection module for the encounter feature.
 * Provides bindings for ViewModel, use cases, and state management components.
 */
val encounterModule = module {
    
    // ViewModel binding with all dependencies
    viewModel {
        EncounterViewModel(
            initializeEncounter = get(),
            processPlayerAction = get(),
            advanceTurn = get(),
            eventRepository = get(),
            stateBuilder = get(),
            completionDetector = get(),
            undoRedoManager = get()
        )
    }
    
    // Use case factory bindings (new instance per injection)
    factory {
        InitializeEncounter(
            initiativeRoller = get(),
            surpriseHandler = get()
        )
    }
    
    factory {
        ProcessPlayerAction()
    }
    
    factory {
        AdvanceTurn(
            initiativeTracker = get()
        )
    }
    
    // Single instance bindings for state management
    single {
        EncounterStateBuilder(
            initiativeStateBuilder = get()
        )
    }
    
    single {
        CompletionDetector()
    }
    
    // Factory binding for UndoRedoManager (new instance per injection)
    factory {
        UndoRedoManager(
            eventRepository = get()
        )
    }
}
