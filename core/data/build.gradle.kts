plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
  namespace = "dev.questweaver.core.data"
  compileSdk = 34
  defaultConfig { minSdk = 26 }
}

dependencies {
  implementation(libs.serialization.json)
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)
  implementation(libs.sqlcipher)
  implementation(libs.sqlite.ktx)
}
