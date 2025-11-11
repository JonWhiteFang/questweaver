package dev.questweaver.app

import android.app.Application
import dev.questweaver.data.di.dataModule
import dev.questweaver.core.rules.di.rulesModule
import dev.questweaver.feature.map.di.mapModule
import dev.questweaver.feature.encounter.di.encounterModule
import dev.questweaver.ai.ondevice.di.aiOnDeviceModule
import dev.questweaver.ai.gateway.di.aiGatewayModule
import dev.questweaver.sync.firebase.di.syncModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class QuestWeaverApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QuestWeaverApp)
            modules(listOf(
                dataModule,
                rulesModule,
                mapModule,
                encounterModule,
                aiOnDeviceModule,
                aiGatewayModule,
                syncModule
            ))
        }
    }
}
