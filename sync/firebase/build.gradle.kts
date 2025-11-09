plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "dev.questweaver.sync.firebase"
  compileSdk = 34
  defaultConfig { minSdk = 26 }
}

dependencies {
  implementation(libs.work.runtime)
}
