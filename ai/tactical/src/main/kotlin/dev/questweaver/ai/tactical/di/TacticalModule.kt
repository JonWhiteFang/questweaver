package dev.questweaver.ai.tactical.di

import dev.questweaver.ai.tactical.resources.ResourceManager
import org.koin.dsl.module

/**
 * Koin DI module for Tactical AI components.
 */
val tacticalModule = module {
    // Resource Management
    single { ResourceManager() }
    
    // Other components will be added as they are implemented
}
