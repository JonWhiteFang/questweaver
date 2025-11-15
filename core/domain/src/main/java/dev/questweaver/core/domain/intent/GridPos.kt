package dev.questweaver.core.domain.intent

/**
 * Represents a position on a grid-based tactical map.
 * 
 * @property x The x-coordinate (column) on the grid (0-indexed)
 * @property y The y-coordinate (row) on the grid (0-indexed)
 */
data class GridPos(
    val x: Int,
    val y: Int
) {
    /**
     * Converts grid notation (e.g., "E5") to GridPos.
     * 
     * @param notation Grid notation where letter is column (A=0, B=1, etc.) and number is row (1-indexed)
     * @return GridPos with 0-indexed coordinates
     */
    companion object {
        fun fromNotation(notation: String): GridPos? {
            val match = Regex("([A-Z])(\\d+)", RegexOption.IGNORE_CASE).matchEntire(notation)
            if (match == null) return null
            
            val (col, row) = match.destructured
            val x = col.uppercase()[0] - 'A'
            val y = row.toIntOrNull()?.minus(1)
            
            return if (y != null) GridPos(x, y) else null
        }
        
        fun fromCoordinates(coords: String): GridPos? {
            val match = Regex("\\((\\d+),(\\d+)\\)").matchEntire(coords) ?: return null
            val (x, y) = match.destructured
            return GridPos(x.toInt(), y.toInt())
        }
    }
    
    /**
     * Converts this GridPos to grid notation (e.g., "E5").
     */
    fun toNotation(): String {
        val col = ('A' + x).toString()
        val row = (y + 1).toString()
        return "$col$row"
    }
}
