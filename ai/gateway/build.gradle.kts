plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "dev.questweaver.ai.gateway"
  compileSdk = 34
  defaultConfig { minSdk = 26 }
}

dependencies {
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.kotlinx)
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.logging)
  implementation(libs.serialization.json)
}
