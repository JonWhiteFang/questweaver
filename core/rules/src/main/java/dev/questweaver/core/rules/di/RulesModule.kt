package dev.questweaver.core.rules.di

import dev.questweaver.core.rules.RulesEngine
import org.koin.dsl.module

val rulesModule = module {
    single { RulesEngine() }
}
