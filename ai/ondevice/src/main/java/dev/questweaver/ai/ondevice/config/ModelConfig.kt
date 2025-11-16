package dev.questweaver.ai.ondevice.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for the ONNX intent classification model.
 * Loaded from model_config.json in assets.
 */
@Serializable
data class ModelConfig(
    @SerialName("model_version")
    val modelVersion: String,
    
    @SerialName("input_shape")
    val inputShape: List<Int>,
    
    @SerialName("output_shape")
    val outputShape: List<Int>,
    
    @SerialName("max_sequence_length")
    val maxSequenceLength: Int,
    
    @SerialName("num_classes")
    val numClasses: Int,
    
    @SerialName("confidence_threshold")
    val confidenceThreshold: Float,
    
    @SerialName("intent_labels")
    val intentLabels: List<String>,
    
    @SerialName("special_tokens")
    val specialTokens: SpecialTokens,
    
    @SerialName("model_metadata")
    val modelMetadata: ModelMetadata
)

@Serializable
data class SpecialTokens(
    @SerialName("unknown")
    val unknown: Int,
    
    @SerialName("padding")
    val padding: Int,
    
    @SerialName("cls")
    val cls: Int,
    
    @SerialName("sep")
    val sep: Int
)

@Serializable
data class ModelMetadata(
    @SerialName("format")
    val format: String,
    
    @SerialName("opset_version")
    val opsetVersion: Int,
    
    @SerialName("quantization")
    val quantization: String,
    
    @SerialName("estimated_size_mb")
    val estimatedSizeMb: Int,
    
    @SerialName("target_inference_time_ms")
    val targetInferenceTimeMs: Int,
    
    @SerialName("warmup_required")
    val warmupRequired: Boolean
)
