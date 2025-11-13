package dev.questweaver.feature.map.di

import dev.questweaver.domain.map.pathfinding.AStarPathfinder
import dev.questweaver.domain.map.pathfinding.Pathfinder
import dev.questweaver.domain.map.pathfinding.ReachabilityCalculator
import dev.questweaver.feature.map.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin DI module for feature:map.
 *
 * Provides:
 * - MapViewModel with MVI pattern
 * - Pathfinder for movement path calculations
 * - ReachabilityCalculator for range overlay calculations
 */
val mapModule = module {
    // Pathfinding dependencies
    single<Pathfinder> { AStarPathfinder() }
    single { ReachabilityCalculator(pathfinder = get()) }
    
    // ViewModel
    viewModel { 
        MapViewModel(
            pathfinder = get(),
            reachabilityCalculator = get()
        )
    }
}
