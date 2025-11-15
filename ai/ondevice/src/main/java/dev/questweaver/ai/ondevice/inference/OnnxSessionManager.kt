package dev.questweaver.ai.ondevice.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.nio.LongBuffer

/**
 * Manages ONNX Runtime session lifecycle and inference execution.
 *
 * This class handles:
 * - Background initialization of ONNX model
 * - Model warmup to reduce first-inference latency
 * - Thread-safe inference execution
 * - Request queuing during initialization
 * - Graceful error handling and resource cleanup
 *
 * @param context Android context for asset loading
 * @param modelPath Path to ONNX model in assets (default: "models/intent_classifier.onnx")
 */
class OnnxSessionManager(
    private val context: Context,
    private val modelPath: String = "models/intent_classifier.onnx"
) {
    private val logger = LoggerFactory.getLogger(OnnxSessionManager::class.java)
    
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private val sessionMutex = Mutex()
    
    @Volatile
    private var initialized = false
    
    @Volatile
    private var initializationFailed = false
    
    private val pendingRequests = mutableListOf<suspend () -> Unit>()
    
    /**
     * Initializes the ONNX Runtime session on a background thread.
     *
     * This method:
     * 1. Loads the ONNX model from assets
     * 2. Creates an OrtEnvironment and OrtSession
     * 3. Warms up the model with a dummy inference
     * 4. Processes any queued requests
     *
     * @throws Exception if model loading fails (caught and logged internally)
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            if (initialized || initializationFailed) {
                return@withContext
            }
            
            try {
                logger.info { "Initializing ONNX Runtime session from $modelPath" }
                
                // Load model from assets
                val modelBytes = context.assets.open(modelPath).use { it.readBytes() }
                logger.debug { "Loaded model: ${modelBytes.size} bytes" }
                
                // Create ONNX Runtime environment and session
                ortEnvironment = OrtEnvironment.getEnvironment()
                ortSession = ortEnvironment!!.createSession(modelBytes)
                
                logger.info { "ONNX session created successfully" }
                
                // Warm up model with dummy inference
                warmupModel()
                
                initialized = true
                logger.info { "ONNX Runtime initialization complete" }
                
                // Process pending requests
                processPendingRequests()
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to initialize ONNX Runtime from $modelPath" }
                initializationFailed = true
                
                // Clear pending requests on failure
                pendingRequests.clear()
            }
        }
    }
    
    /**
     * Runs inference on the ONNX model with the provided token indices.
     *
     * @param tokens IntArray of token indices (must be length 128)
     * @return FloatArray of intent probabilities (length 12)
     * @throws IllegalStateException if session not initialized or initialization failed
     * @throws IllegalArgumentException if tokens array is not length 128
     */
    suspend fun infer(tokens: IntArray): FloatArray = withContext(Dispatchers.IO) {
        require(tokens.size == 128) { "Token array must be length 128, got ${tokens.size}" }
        
        check(!initializationFailed) { "ONNX Runtime initialization failed, cannot perform inference" }
        check(initialized) { "ONNX Runtime not initialized, call initialize() first" }
        
        sessionMutex.withLock {
            val session = checkNotNull(ortSession) { "OrtSession is null" }
            val env = checkNotNull(ortEnvironment) { "OrtEnvironment is null" }
            
            try {
                // Convert IntArray to LongBuffer (ONNX expects Int64)
                val longTokens = tokens.map { it.toLong() }.toLongArray()
                val inputBuffer = LongBuffer.wrap(longTokens)
                
                // Create input tensor with shape [1, 128]
                val inputTensor = OnnxTensor.createTensor(
                    env,
                    inputBuffer,
                    longOf(1, 128)
                )
                
                // Run inference
                val results = session.run(mapOf("input" to inputTensor))
                
                // Extract output tensor
                val outputTensor = results[0] as OnnxTensor
                val outputArray = outputTensor.floatBuffer.array()
                
                // Clean up
                inputTensor.close()
                results.close()
                
                logger.debug { "Inference complete: ${outputArray.size} probabilities" }
                
                return@withLock outputArray
                
            } catch (e: Exception) {
                logger.error(e) { "ONNX inference failed" }
                throw e
            }
        }
    }
    
    /**
     * Checks if the ONNX Runtime session is ready for inference.
     *
     * @return true if initialized and ready, false otherwise
     */
    fun isReady(): Boolean = initialized && !initializationFailed
    
    /**
     * Closes the ONNX Runtime session and releases resources.
     *
     * This method should be called when the session is no longer needed,
     * typically in ViewModel.onCleared() or similar lifecycle methods.
     */
    fun close() {
        sessionMutex.tryLock().let { locked ->
            if (locked) {
                try {
                    ortSession?.close()
                    ortSession = null
                    ortEnvironment = null
                    initialized = false
                    logger.info { "ONNX Runtime session closed" }
                } finally {
                    sessionMutex.unlock()
                }
            }
        }
    }
    
    /**
     * Warms up the model by running a dummy inference.
     *
     * This reduces latency on the first real inference by pre-allocating
     * internal buffers and caches.
     */
    private suspend fun warmupModel() {
        try {
            logger.debug { "Warming up ONNX model" }
            
            // Create dummy input (all zeros)
            val dummyTokens = IntArray(128) { 0 }
            
            // Run dummy inference (result is discarded)
            infer(dummyTokens)
            
            logger.debug { "Model warmup complete" }
            
        } catch (e: Exception) {
            logger.warn(e) { "Model warmup failed, continuing anyway" }
        }
    }
    
    /**
     * Processes any requests that were queued during initialization.
     */
    private suspend fun processPendingRequests() {
        if (pendingRequests.isEmpty()) {
            return
        }
        
        logger.debug { "Processing ${pendingRequests.size} pending requests" }
        
        val requests = pendingRequests.toList()
        pendingRequests.clear()
        
        requests.forEach { request ->
            try {
                request()
            } catch (e: Exception) {
                logger.error(e) { "Failed to process pending request" }
            }
        }
    }
    
    /**
     * Helper function to create a LongArray from varargs.
     */
    private fun longOf(vararg values: Long): LongArray = values
}
