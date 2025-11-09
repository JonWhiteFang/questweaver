plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "dev.questweaver.ai.ondevice"
  compileSdk = 34
  defaultConfig { minSdk = 26 }
}

dependencies {
  implementation(libs.onnx.runtime)
}
