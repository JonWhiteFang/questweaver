package dev.questweaver.data.di

import dev.questweaver.data.db.AppDatabase
import dev.questweaver.data.db.buildDatabase
import dev.questweaver.data.repositories.EventRepositoryImpl
import dev.questweaver.data.security.KeystoreManager
import dev.questweaver.domain.repositories.EventRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin dependency injection module for the data layer.
 * 
 * Provides singleton instances of:
 * - KeystoreManager for encryption key management
 * - AppDatabase with SQLCipher encryption
 * - EventDao from the database instance
 * - EventRepository implementation
 * 
 * All dependencies are properly scoped and lifecycle-aware, with the database
 * instance being a singleton shared across the application.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
val dataModule = module {
    
    /**
     * Provides KeystoreManager as a singleton.
     * 
     * Uses androidContext() to access the Android application context
     * for Keystore operations.
     */
    single { KeystoreManager(androidContext()) }
    
    /**
     * Provides AppDatabase as a singleton with SQLCipher encryption.
     * 
     * Retrieves the encryption key from KeystoreManager and uses it
     * to build the encrypted database instance. The database is shared
     * across the application lifecycle.
     */
    single {
        val keystoreManager: KeystoreManager = get()
        val passphrase = keystoreManager.getOrCreateDatabaseKey()
        buildDatabase(androidContext(), passphrase)
    }
    
    /**
     * Provides EventDao from the database instance.
     * 
     * The DAO is obtained from the singleton AppDatabase and provides
     * access to event persistence operations.
     */
    single { get<AppDatabase>().eventDao() }
    
    /**
     * Provides EventRepository implementation as a singleton.
     * 
     * Binds the EventRepositoryImpl to the EventRepository interface,
     * allowing domain layer to depend on the interface while the data
     * layer provides the implementation.
     */
    single<EventRepository> { EventRepositoryImpl(get()) }
}
