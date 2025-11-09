package dev.questweaver.core.data.di

import android.content.Context
import androidx.room.Room
import dev.questweaver.core.data.db.AppDatabase
import net.sqlcipher.database.SupportFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { provideDatabase(androidContext()) }
}

private fun provideDatabase(ctx: Context): AppDatabase {
    // Demo key — replace with a key from Android Keystore in production
    val passphrase = "demo-demo-demo-demo".toByteArray()
    val factory = SupportFactory(passphrase)
    return Room.databaseBuilder(ctx, AppDatabase::class.java, "questweaver.db")
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
}
