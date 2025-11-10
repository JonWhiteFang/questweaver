package dev.questweaver.ai.ondevice

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class IntentClassifier {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        // In a real app, load a small .onnx model from assets
        // For scaffold purposes this won't run until you add the model.
        val bytes = ByteArray(0) // placeholder
        session = env.createSession(bytes) // will throw until replaced with real model
    }

    fun classify(): String {
        // Stub implementation - will be replaced with actual ONNX inference
        return DEFAULT_INTENT
    }

    companion object {
        private const val DEFAULT_INTENT = "attack"
    }
}
