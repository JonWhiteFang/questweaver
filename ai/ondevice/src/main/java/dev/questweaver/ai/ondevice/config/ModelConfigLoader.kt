package dev.questweaver.ai.ondevice.config

import android.content.Context
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Loads model configuration from assets/models/model_config.json.
 * 
 * The configuration includes model parameters like confidence threshold,
 * max sequence length, intent labels, and special token IDs.
 */
class ModelConfigLoader(private val context: Context) {
    
    private val logger = LoggerFactory.getLogger(ModelConfigLoader::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }
    
    /**
     * Loads the model configuration from assets.
     * 
     * @param configPath Path to config file in assets (default: "models/model_config.json")
     * @return ModelConfig instance with loaded configuration
     * @throws IOException if config file cannot be read
     * @throws kotlinx.serialization.SerializationException if JSON is malformed
     */
    fun loadConfig(configPath: String = "models/model_config.json"): ModelConfig {
        logger.info("Loading model config from: $configPath")
        
        return try {
            val configJson = context.assets.open(configPath).use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            
            val config = json.decodeFromString<ModelConfig>(configJson)
            
            logger.info(
                "Model config loaded successfully: version=${config.modelVersion}, " +
                "maxSeqLen=${config.maxSequenceLength}, " +
                "numClasses=${config.numClasses}, " +
                "threshold=${config.confidenceThreshold}"
            )
            
            validateConfig(config)
            
            config
        } catch (e: IOException) {
            logger.error("Failed to read model config file: $configPath", e)
            throw e
        } catch (e: kotlinx.serialization.SerializationException) {
            logger.error("Failed to parse model config JSON", e)
            throw e
        }
    }
    
    /**
     * Validates that the loaded configuration has sensible values.
     * Logs warnings for suspicious values but doesn't throw exceptions.
     */
    private fun validateConfig(config: ModelConfig) {
        // Validate input shape
        if (config.inputShape.size != 2) {
            logger.warn("Expected input_shape to have 2 dimensions, got ${config.inputShape.size}")
        }
        if (config.inputShape.getOrNull(1) != config.maxSequenceLength) {
            logger.warn(
                "input_shape[1] (${config.inputShape.getOrNull(1)}) doesn't match " +
                "max_sequence_length (${config.maxSequenceLength})"
            )
        }
        
        // Validate output shape
        if (config.outputShape.size != 2) {
            logger.warn("Expected output_shape to have 2 dimensions, got ${config.outputShape.size}")
        }
        if (config.outputShape.getOrNull(1) != config.numClasses) {
            logger.warn(
                "output_shape[1] (${config.outputShape.getOrNull(1)}) doesn't match " +
                "num_classes (${config.numClasses})"
            )
        }
        
        // Validate confidence threshold
        if (config.confidenceThreshold < 0.0f || config.confidenceThreshold > 1.0f) {
            logger.warn(
                "confidence_threshold (${config.confidenceThreshold}) should be between 0.0 and 1.0"
            )
        }
        
        // Validate intent labels count
        if (config.intentLabels.size != config.numClasses) {
            logger.warn(
                "Number of intent_labels (${config.intentLabels.size}) doesn't match " +
                "num_classes (${config.numClasses})"
            )
        }
        
        // Validate max sequence length
        if (config.maxSequenceLength <= 0 || config.maxSequenceLength > MAX_REASONABLE_SEQUENCE_LENGTH) {
            logger.warn(
                "max_sequence_length (${config.maxSequenceLength}) seems unusual " +
                "(expected 1-$MAX_REASONABLE_SEQUENCE_LENGTH)"
            )
        }
        
        logger.debug("Model config validation complete")
    }
    
    companion object {
        /**
         * Default path to model config in assets.
         */
        const val DEFAULT_CONFIG_PATH = "models/model_config.json"
        
        /**
         * Maximum reasonable sequence length for validation.
         */
        private const val MAX_REASONABLE_SEQUENCE_LENGTH = 512
    }
}
