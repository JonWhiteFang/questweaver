package dev.questweaver.ai.ondevice

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class IntentClassifier(context: Context) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        // In a real app, load a small .onnx model from assets
        // For scaffold purposes this won't run until you add the model.
        val bytes = ByteArray(0) // placeholder
        session = env.createSession(bytes) // will throw until replaced with real model
    }

    fun classify(text: String): String = "attack" // stub
}
