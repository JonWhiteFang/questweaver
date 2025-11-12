package dev.questweaver.domain.map.geometry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LineOfEffectTest : FunSpec({
    
    context("Bresenham line for horizontal paths") {
        test("horizontal line from left to right") {
            val from = GridPos(0, 5)
            val to = GridPos(5, 5)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            path shouldBe listOf(
                GridPos(0, 5),
                GridPos(1, 5),
                GridPos(2, 5),
                GridPos(3, 5),
                GridPos(4, 5),
                GridPos(5, 5)
            )
        }
        
        test("horizontal line from right to left") {
            val from = GridPos(5, 5)
            val to = GridPos(0, 5)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            path shouldBe listOf(
                GridPos(5, 5),
                GridPos(4, 5),
                GridPos(3, 5),
                GridPos(2, 5),
                GridPos(1, 5),
                GridPos(0, 5)
            )
        }
    }
    
    context("Bresenham line for vertical paths") {
        test("vertical line from top to bottom") {
            val from = GridPos(5, 0)
            val to = GridPos(5, 5)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            path shouldBe listOf(
                GridPos(5, 0),
                GridPos(5, 1),
                GridPos(5, 2),
                GridPos(5, 3),
                GridPos(5, 4),
                GridPos(5, 5)
            )
        }
        
        test("vertical line from bottom to top") {
            val from = GridPos(5, 5)
            val to = GridPos(5, 0)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            path shouldBe listOf(
                GridPos(5, 5),
                GridPos(5, 4),
                GridPos(5, 3),
                GridPos(5, 2),
                GridPos(5, 1),
                GridPos(5, 0)
            )
        }
    }
    
    context("Bresenham line for diagonal paths") {
        test("diagonal line from top-left to bottom-right") {
            val from = GridPos(0, 0)
            val to = GridPos(5, 5)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            path shouldBe listOf(
                GridPos(0, 0),
                GridPos(1, 1),
                GridPos(2, 2),
                GridPos(3, 3),
                GridPos(4, 4),
                GridPos(5, 5)
            )
        }
        
        test("diagonal line from bottom-right to top-left") {
            val from = GridPos(5, 5)
            val to = GridPos(0, 0)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            path shouldBe listOf(
                GridPos(5, 5),
                GridPos(4, 4),
                GridPos(3, 3),
                GridPos(2, 2),
                GridPos(1, 1),
                GridPos(0, 0)
            )
        }
        
        test("diagonal line from top-right to bottom-left") {
            val from = GridPos(5, 0)
            val to = GridPos(0, 5)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            path shouldBe listOf(
                GridPos(5, 0),
                GridPos(4, 1),
                GridPos(3, 2),
                GridPos(2, 3),
                GridPos(1, 4),
                GridPos(0, 5)
            )
        }
    }
    
    context("Bresenham line for arbitrary angles") {
        test("shallow angle line (more horizontal than vertical)") {
            val from = GridPos(0, 0)
            val to = GridPos(6, 2)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            // Path should include start and end
            path.first() shouldBe from
            path.last() shouldBe to
            // Path should be continuous
            path shouldHaveSize 7
        }
        
        test("steep angle line (more vertical than horizontal)") {
            val from = GridPos(0, 0)
            val to = GridPos(2, 6)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            // Path should include start and end
            path.first() shouldBe from
            path.last() shouldBe to
            // Path should be continuous
            path shouldHaveSize 7
        }
        
        test("knight's move pattern") {
            val from = GridPos(0, 0)
            val to = GridPos(2, 1)
            
            val path = LineOfEffect.bresenhamLine(from, to)
            
            // Path should include start and end
            path.first() shouldBe from
            path.last() shouldBe to
            // Path should connect the positions
            path shouldHaveSize 3
        }
    }
    
    context("Line-of-effect with no obstacles (clear path)") {
        val grid = MapGrid(width = 20, height = 20)
        
        test("clear horizontal line-of-effect") {
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
        
        test("clear vertical line-of-effect") {
            val from = GridPos(5, 0)
            val to = GridPos(5, 10)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
        
        test("clear diagonal line-of-effect") {
            val from = GridPos(0, 0)
            val to = GridPos(10, 10)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
        
        test("clear arbitrary angle line-of-effect") {
            val from = GridPos(2, 3)
            val to = GridPos(8, 6)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
    }
    
    context("Line-of-effect blocked by single obstacle") {
        test("obstacle directly in the middle of horizontal path") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(5, 5), CellProperties(hasObstacle = true))
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe false
        }
        
        test("obstacle in the middle of diagonal path") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(5, 5), CellProperties(hasObstacle = true))
            
            val from = GridPos(0, 0)
            val to = GridPos(10, 10)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe false
        }
        
        test("obstacle near the start does not block") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(0, 5), CellProperties(hasObstacle = true))
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            // Start position obstacle doesn't block (excluded from check)
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
        
        test("obstacle at the end does not block") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(10, 5), CellProperties(hasObstacle = true))
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            // End position obstacle doesn't block (excluded from check)
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
    }
    
    context("Line-of-effect blocked by multiple obstacles") {
        test("multiple obstacles along horizontal path") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(3, 5), CellProperties(hasObstacle = true))
                .withCellProperties(GridPos(5, 5), CellProperties(hasObstacle = true))
                .withCellProperties(GridPos(7, 5), CellProperties(hasObstacle = true))
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe false
        }
        
        test("multiple obstacles along diagonal path") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(3, 3), CellProperties(hasObstacle = true))
                .withCellProperties(GridPos(7, 7), CellProperties(hasObstacle = true))
            
            val from = GridPos(0, 0)
            val to = GridPos(10, 10)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe false
        }
        
        test("obstacles adjacent to path do not block") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(5, 4), CellProperties(hasObstacle = true))
                .withCellProperties(GridPos(5, 6), CellProperties(hasObstacle = true))
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            // Obstacles not on the path don't block
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
    }
    
    context("Line-of-effect through creature-occupied cells (allowed)") {
        test("creature in the middle of path does not block") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(5, 5), CellProperties(occupiedBy = 123L))
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
        
        test("multiple creatures along path do not block") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(3, 5), CellProperties(occupiedBy = 100L))
                .withCellProperties(GridPos(5, 5), CellProperties(occupiedBy = 200L))
                .withCellProperties(GridPos(7, 5), CellProperties(occupiedBy = 300L))
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe true
        }
        
        test("creature and obstacle - obstacle blocks") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(3, 5), CellProperties(occupiedBy = 100L))
                .withCellProperties(GridPos(7, 5), CellProperties(hasObstacle = true))
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe false
        }
        
        test("creature on obstacle cell - obstacle blocks") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(
                    GridPos(5, 5), 
                    CellProperties(hasObstacle = true, occupiedBy = 100L)
                )
            
            val from = GridPos(0, 5)
            val to = GridPos(10, 5)
            
            val hasLOS = LineOfEffect.hasLineOfEffect(from, to, grid)
            
            hasLOS shouldBe false
        }
    }
    
    context("positionsWithinRangeAndLOS combining both constraints") {
        test("returns positions within range and with clear line-of-effect") {
            val grid = MapGrid(width = 20, height = 20)
            val origin = GridPos(10, 10)
            
            val positions = LineOfEffect.positionsWithinRangeAndLOS(origin, 15, grid)
            
            // Should include positions within 15 feet (3 squares)
            positions shouldContain GridPos(10, 10) // Origin
            positions shouldContain GridPos(11, 10) // 1 square away
            positions shouldContain GridPos(13, 10) // 3 squares away
            positions shouldContain GridPos(13, 13) // 3 squares diagonal
        }
        
        test("excludes positions blocked by obstacles even if in range") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(11, 10), CellProperties(hasObstacle = true))
            
            val origin = GridPos(10, 10)
            
            val positions = LineOfEffect.positionsWithinRangeAndLOS(origin, 15, grid)
            
            // Should include origin
            positions shouldContain GridPos(10, 10)
            // Should not include positions behind the obstacle
            positions.contains(GridPos(12, 10)) shouldBe false
            positions.contains(GridPos(13, 10)) shouldBe false
        }
        
        test("excludes positions beyond range even with clear line-of-effect") {
            val grid = MapGrid(width = 50, height = 50)
            val origin = GridPos(10, 10)
            
            val positions = LineOfEffect.positionsWithinRangeAndLOS(origin, 15, grid)
            
            // Should not include positions beyond 15 feet (3 squares)
            positions.contains(GridPos(14, 10)) shouldBe false // 4 squares away
            positions.contains(GridPos(15, 10)) shouldBe false // 5 squares away
        }
        
        test("includes positions with creatures but excludes obstacles") {
            val grid = MapGrid(width = 20, height = 20)
                .withCellProperties(GridPos(11, 10), CellProperties(occupiedBy = 100L))
                .withCellProperties(GridPos(10, 11), CellProperties(hasObstacle = true))
            
            val origin = GridPos(10, 10)
            
            val positions = LineOfEffect.positionsWithinRangeAndLOS(origin, 15, grid)
            
            // Should include positions through creatures
            positions shouldContain GridPos(11, 10)
            positions shouldContain GridPos(12, 10)
            // Should not include positions behind obstacles
            positions.contains(GridPos(10, 12)) shouldBe false
        }
        
        test("handles corner positions correctly") {
            val grid = MapGrid(width = 20, height = 20)
            val origin = GridPos(0, 0)
            
            val positions = LineOfEffect.positionsWithinRangeAndLOS(origin, 10, grid)
            
            // Should include positions within range from corner
            positions shouldContain GridPos(0, 0) // Origin
            positions shouldContain GridPos(1, 0) // Adjacent
            positions shouldContain GridPos(0, 1) // Adjacent
            positions shouldContain GridPos(2, 2) // Diagonal within range
        }
    }
})
