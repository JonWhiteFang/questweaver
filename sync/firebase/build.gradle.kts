plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "dev.questweaver.sync.firebase"
  compileSdk = 34
  
  defaultConfig {
    minSdk = 26
  }
  
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  
  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  // Module dependencies
  implementation(project(":core:domain"))
  implementation(project(":core:data"))
  
  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.bundles.firebase)
  
  // WorkManager
  implementation(libs.work.runtime)
  
  // Kotlin
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines)
  
  // Testing
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.mockk)
}
