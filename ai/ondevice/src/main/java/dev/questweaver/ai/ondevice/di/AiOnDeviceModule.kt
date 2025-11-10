package dev.questweaver.ai.ondevice.di

import dev.questweaver.ai.ondevice.IntentClassifier
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val aiOnDeviceModule = module {
    single { IntentClassifier() }
}
