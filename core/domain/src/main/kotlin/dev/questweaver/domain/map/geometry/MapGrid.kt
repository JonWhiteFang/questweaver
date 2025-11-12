package dev.questweaver.domain.map.geometry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a tactical combat grid with configurable dimensions and cell properties.
 *
 * Each cell represents a 5-foot square per D&D 5e SRD rules.
 *
 * @property width The width of the grid in cells (10-100)
 * @property height The height of the grid in cells (10-100)
 * @property cells Map of grid positions to their properties
 */
@Serializable
@SerialName("map_grid")
data class MapGrid(
    val width: Int,
    val height: Int,
    val cells: Map<GridPos, CellProperties> = emptyMap()
) {
    init {
        require(width in MIN_DIMENSION..MAX_DIMENSION) { 
            "Width must be between $MIN_DIMENSION and $MAX_DIMENSION, got $width" 
        }
        require(height in MIN_DIMENSION..MAX_DIMENSION) { 
            "Height must be between $MIN_DIMENSION and $MAX_DIMENSION, got $height" 
        }
    }
    
    companion object {
        /** Minimum grid dimension (width or height) */
        const val MIN_DIMENSION = 10
        
        /** Maximum grid dimension (width or height) */
        const val MAX_DIMENSION = 100
    }
    
    /**
     * Checks if a position is within the grid boundaries.
     *
     * @param pos The position to check
     * @return true if the position is within bounds, false otherwise
     */
    fun isInBounds(pos: GridPos): Boolean {
        return pos.x in 0 until width && pos.y in 0 until height
    }
    
    /**
     * Gets the properties of a cell at the given position.
     *
     * @param pos The position to query
     * @return The cell properties, or default empty properties if not set
     */
    fun getCellProperties(pos: GridPos): CellProperties {
        return cells[pos] ?: CellProperties()
    }
    
    /**
     * Returns a new MapGrid with updated cell properties at the given position.
     * This operation is immutable and returns a new instance.
     *
     * @param pos The position to update
     * @param properties The new properties for the cell
     * @return A new MapGrid with the updated cell
     */
    fun withCellProperties(pos: GridPos, properties: CellProperties): MapGrid {
        return copy(cells = cells + (pos to properties))
    }
    
    /**
     * Returns a lazy sequence of all positions in the grid.
     * Positions are generated on-demand to avoid materializing large collections.
     *
     * @return A sequence of all grid positions
     */
    fun allPositions(): Sequence<GridPos> = sequence {
        for (y in 0 until height) {
            for (x in 0 until width) {
                yield(GridPos(x, y))
            }
        }
    }
}
