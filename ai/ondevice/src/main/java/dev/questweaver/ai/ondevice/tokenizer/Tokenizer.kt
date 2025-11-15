package dev.questweaver.ai.ondevice.tokenizer

/**
 * Interface for converting text into token indices for ONNX model input.
 */
interface Tokenizer {
    /**
     * Converts text into an array of token indices.
     * 
     * @param text The input text to tokenize
     * @return Array of token indices padded/truncated to the model's max length
     */
    fun tokenize(text: String): IntArray
}

/**
 * Simple tokenizer implementation that splits on whitespace and punctuation.
 * 
 * Converts text to lowercase, splits on whitespace and punctuation boundaries,
 * maps tokens to vocabulary indices, and pads or truncates to the specified max length.
 * 
 * @param vocabulary Map of tokens to their vocabulary indices
 * @param maxLength Maximum sequence length (tokens will be padded or truncated to this length)
 * @param unknownTokenId Index for unknown tokens not in vocabulary
 * @param paddingTokenId Index for padding tokens
 */
class SimpleTokenizer(
    private val vocabulary: Map<String, Int>,
    private val maxLength: Int = 128,
    private val unknownTokenId: Int = 0,
    private val paddingTokenId: Int = 1
) : Tokenizer {
    
    override fun tokenize(text: String): IntArray {
        // 1. Lowercase and split on whitespace/punctuation
        val tokens = text.lowercase()
            .split(Regex("\\s+|(?=[.,!?;:])"))
            .filter { it.isNotBlank() }
        
        // 2. Map to vocabulary indices
        val indices = tokens.map { token ->
            vocabulary[token] ?: unknownTokenId
        }
        
        // 3. Pad or truncate to maxLength
        return when {
            indices.size > maxLength -> indices.take(maxLength).toIntArray()
            indices.size < maxLength -> {
                (indices + List(maxLength - indices.size) { paddingTokenId }).toIntArray()
            }
            else -> indices.toIntArray()
        }
    }
}
