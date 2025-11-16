plugins {
  alias(libs.plugins.android.app)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  // Crashlytics / Google services omitted in scaffold for build simplicity.
}

android {
  namespace = "dev.questweaver"
  compileSdk = 34

  defaultConfig {
    applicationId = "dev.questweaver"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0.0"
  }
  
  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {
      isMinifyEnabled = false
      isDebuggable = true
    }
  }
  
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
  packaging { resources.excludes += "META-INF/**" }
  
  lint {
    // Use custom lint configuration
    lintConfig = file("lint.xml")
    
    // Fail build on errors
    abortOnError = true
    
    // Don't fail on warnings
    warningsAsErrors = false
    
    // Generate reports
    htmlReport = true
    htmlOutput = file("${layout.buildDirectory.get()}/reports/lint/lint-results.html")
    
    xmlReport = true
    xmlOutput = file("${layout.buildDirectory.get()}/reports/lint/lint-results.xml")
    
    // Check all warnings
    checkAllWarnings = true
    
    // Ignore test sources
    ignoreTestSources = true
    
    // Baseline file for existing issues
    baseline = file("lint-baseline.xml")
    
    // Check dependencies
    checkDependencies = true
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.activity)
  debugImplementation(libs.compose.ui.tooling)

  implementation(libs.kotlinx.coroutines)
  implementation(libs.serialization.json)

  implementation(libs.koin.android)

  implementation(libs.retrofit.core)
  implementation(libs.retrofit.kotlinx)
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.logging)

  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)
  implementation(libs.sqlcipher)
  implementation(libs.sqlite.ktx)

  implementation(libs.work.runtime)

  // Logging implementation
  implementation(libs.slf4j.simple)

  implementation(project(":core:domain"))
  implementation(project(":core:data"))
  implementation(project(":core:rules"))
  implementation(project(":feature:map"))
  implementation(project(":feature:encounter"))
  implementation(project(":feature:character"))
  implementation(project(":ai:ondevice"))
  implementation(project(":ai:gateway"))
  implementation(project(":sync:firebase"))
  
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
