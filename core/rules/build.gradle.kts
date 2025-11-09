plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "dev.questweaver.core.rules"
  compileSdk = 34
  defaultConfig { minSdk = 26 }
}
