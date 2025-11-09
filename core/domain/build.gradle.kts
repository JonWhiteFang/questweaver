plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "dev.questweaver.core.domain"
  compileSdk = 34
  defaultConfig { minSdk = 26 }
}

dependencies {
  implementation(libs.serialization.json)
}
