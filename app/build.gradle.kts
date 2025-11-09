plugins {
  alias(libs.plugins.android.app)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  // Crashlytics / Google services omitted in scaffold for build simplicity.
}

android {
  namespace = "dev.questweaver.app"
  compileSdk = 34

  defaultConfig {
    applicationId = "dev.questweaver"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug { isMinifyEnabled = false }
  }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
  packaging { resources.excludes += "META-INF/**" }
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

  implementation(project(":core:domain"))
  implementation(project(":core:data"))
  implementation(project(":core:rules"))
  implementation(project(":feature:map"))
  implementation(project(":feature:encounter"))
  implementation(project(":feature:character"))
  implementation(project(":ai:ondevice"))
  implementation(project(":ai:gateway"))
  implementation(project(":sync:firebase"))
}
