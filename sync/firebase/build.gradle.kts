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

tasks.withType<Test> {
  useJUnitPlatform()
  
  // Enable parallel test execution
  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
  
  // Configure test reports
  reports {
    html.required.set(true)
    junitXml.required.set(true)
  }
  
  // Configure test logging
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showStandardStreams = false
  }
}
