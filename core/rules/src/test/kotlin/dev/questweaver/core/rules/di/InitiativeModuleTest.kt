package dev.questweaver.core.rules.di

import dev.questweaver.core.rules.initiative.InitiativeRoller
import dev.questweaver.core.rules.initiative.InitiativeStateBuilder
import dev.questweaver.core.rules.initiative.InitiativeTracker
import dev.questweaver.core.rules.initiative.SurpriseHandler
import dev.questweaver.core.rules.initiative.TurnPhaseManager
import dev.questweaver.domain.dice.DiceRoller
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Tests for InitiativeModule Koin dependency injection configuration.
 *
 * Verifies that all initiative system components are correctly configured
 * and can be resolved by the Koin DI container.
 */
class InitiativeModuleTest : FunSpec({
    
    beforeTest {
        // Start Koin with initiative module and a DiceRoller
        startKoin {
            modules(
                module {
                    factory { DiceRoller(seed = 42L) }
                },
                initiativeModule
            )
        }
    }
    
    afterTest {
        stopKoin()
    }
    
    test("InitiativeRoller can be resolved from Koin") {
        val koin = org.koin.core.context.GlobalContext.get()
        val initiativeRoller = koin.get<InitiativeRoller>()
        
        initiativeRoller shouldNotBe null
        initiativeRoller.shouldBeInstanceOf<InitiativeRoller>()
    }
    
    test("InitiativeTracker can be resolved from Koin") {
        val koin = org.koin.core.context.GlobalContext.get()
        val initiativeTracker = koin.get<InitiativeTracker>()
        
        initiativeTracker shouldNotBe null
        initiativeTracker.shouldBeInstanceOf<InitiativeTracker>()
    }
    
    test("TurnPhaseManager can be resolved from Koin") {
        val koin = org.koin.core.context.GlobalContext.get()
        val turnPhaseManager = koin.get<TurnPhaseManager>()
        
        turnPhaseManager shouldNotBe null
        turnPhaseManager.shouldBeInstanceOf<TurnPhaseManager>()
    }
    
    test("SurpriseHandler can be resolved from Koin") {
        val koin = org.koin.core.context.GlobalContext.get()
        val surpriseHandler = koin.get<SurpriseHandler>()
        
        surpriseHandler shouldNotBe null
        surpriseHandler.shouldBeInstanceOf<SurpriseHandler>()
    }
    
    test("InitiativeStateBuilder can be resolved from Koin") {
        val koin = org.koin.core.context.GlobalContext.get()
        val initiativeStateBuilder = koin.get<InitiativeStateBuilder>()
        
        initiativeStateBuilder shouldNotBe null
        initiativeStateBuilder.shouldBeInstanceOf<InitiativeStateBuilder>()
    }
    
    test("InitiativeRoller uses injected DiceRoller") {
        val koin = org.koin.core.context.GlobalContext.get()
        val initiativeRoller = koin.get<InitiativeRoller>()
        
        // Roll initiative for a creature
        val entry = initiativeRoller.rollInitiative(
            creatureId = 1L,
            dexterityModifier = 3
        )
        
        // Verify the entry is created correctly
        entry.creatureId shouldBe 1L
        entry.modifier shouldBe 3
        entry.total shouldBe entry.roll + entry.modifier
    }
    
    test("factory scope creates new instances") {
        val koin = org.koin.core.context.GlobalContext.get()
        val tracker1 = koin.get<InitiativeTracker>()
        val tracker2 = koin.get<InitiativeTracker>()
        
        // Factory scope should create different instances
        (tracker1 !== tracker2) shouldBe true
    }
})
