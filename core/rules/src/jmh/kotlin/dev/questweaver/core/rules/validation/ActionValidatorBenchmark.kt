package dev.questweaver.core.rules.validation

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.state.ConcentrationInfo
import dev.questweaver.core.rules.validation.state.ConcentrationState
import dev.questweaver.core.rules.validation.state.EncounterState
import dev.questweaver.core.rules.validation.state.GridPos
import dev.questweaver.core.rules.validation.state.ResourcePool
import dev.questweaver.core.rules.validation.state.TurnState
import dev.questweaver.core.rules.validation.validators.ActionEconomyValidator
import dev.questweaver.core.rules.validation.validators.ConcentrationValidator
import dev.questweaver.core.rules.validation.validators.ConditionValidator
import dev.questweaver.core.rules.validation.validators.RangeValidator
import dev.questweaver.core.rules.validation.validators.ResourceValidator
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.conditions.ConditionRegistry
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * JMH benchmarks for ActionValidator and individual validators.
 *
 * Verifies that validation completes within the 50ms performance target
 * specified in requirement 1.5.
 *
 * Run with: gradle jmh
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class ActionValidatorBenchmark {

    // Validators
    private lateinit var actionValidator: ActionValidator
    private lateinit var actionEconomyValidator: ActionEconomyValidator
    private lateinit var resourceValidator: ResourceValidator
    private lateinit var rangeValidator: RangeValidator
    private lateinit var concentrationValidator: ConcentrationValidator
    private lateinit var conditionValidator: ConditionValidator

    // Test data
    private lateinit var attackAction: GameAction.Attack
    private lateinit var spellAction: GameAction.CastSpell
    private lateinit var moveAction: GameAction.Move
    private lateinit var opportunityAttackAction: GameAction.OpportunityAttack
    
    private lateinit var turnState: TurnState
    private lateinit var encounterState: EncounterState
    private lateinit var conditions: Set<Condition>

    @Setup
    fun setup() {
        // Initialize validators
        actionEconomyValidator = ActionEconomyValidator()
        resourceValidator = ResourceValidator()
        rangeValidator = RangeValidator()
        concentrationValidator = ConcentrationValidator()
        conditionValidator = ConditionValidator(ConditionRegistry)
        
        actionValidator = ActionValidator(
            actionEconomyValidator,
            resourceValidator,
            rangeValidator,
            concentrationValidator,
            conditionValidator
        )

        // Create test data
        val actorId = 1L
        val targetId = 2L

        attackAction = GameAction.Attack(
            actorId = actorId,
            targetId = targetId,
            weaponId = null
        )

        spellAction = GameAction.CastSpell(
            actorId = actorId,
            spellId = "magic_missile_1st",
            targetIds = listOf(targetId),
            targetPos = null,
            slotLevel = 1
        )

        moveAction = GameAction.Move(
            actorId = actorId,
            path = listOf(
                GridPos(0, 0),
                GridPos(1, 0),
                GridPos(2, 0),
                GridPos(3, 0)
            )
        )

        opportunityAttackAction = GameAction.OpportunityAttack(
            actorId = actorId,
            targetId = targetId
        )

        turnState = TurnState(
            creatureId = actorId,
            round = 1,
            actionUsed = false,
            bonusActionUsed = false,
            reactionUsed = false,
            movementUsed = 0,
            movementTotal = 30,
            resourcePool = ResourcePool(
                spellSlots = mapOf(
                    1 to 4,
                    2 to 3,
                    3 to 3,
                    4 to 2,
                    5 to 1
                )
            ),
            concentrationState = ConcentrationState.Empty
        )

        encounterState = EncounterState(
            positions = mapOf(
                actorId to GridPos(0, 0),
                targetId to GridPos(5, 0)
            ),
            obstacles = setOf(
                GridPos(2, 1),
                GridPos(3, 1),
                GridPos(4, 1)
            )
        )

        conditions = emptySet()
    }

    // ========== ActionValidator Benchmarks ==========

    @Benchmark
    fun benchmarkValidateAttack(): Any {
        return actionValidator.validate(
            attackAction,
            conditions,
            turnState,
            encounterState
        )
    }

    @Benchmark
    fun benchmarkValidateSpell(): Any {
        return actionValidator.validate(
            spellAction,
            conditions,
            turnState,
            encounterState
        )
    }

    @Benchmark
    fun benchmarkValidateMove(): Any {
        return actionValidator.validate(
            moveAction,
            conditions,
            turnState,
            encounterState
        )
    }

    @Benchmark
    fun benchmarkValidateOpportunityAttack(): Any {
        return actionValidator.validate(
            opportunityAttackAction,
            conditions,
            turnState,
            encounterState
        )
    }

    @Benchmark
    fun benchmarkValidateWithConditions(): Any {
        return actionValidator.validate(
            attackAction,
            setOf(Condition.Prone),
            turnState,
            encounterState
        )
    }

    @Benchmark
    fun benchmarkValidateWithConcentration(): Any {
        val concentratingState = turnState.copy(
            concentrationState = ConcentrationState(
                activeConcentrations = mapOf(
                    1L to ConcentrationInfo("bless", 1, 10)
                )
            )
        )
        return actionValidator.validate(
            spellAction,
            conditions,
            concentratingState,
            encounterState
        )
    }

    // ========== Individual Validator Benchmarks ==========

    @Benchmark
    fun benchmarkActionEconomyValidator(): Any {
        return actionEconomyValidator.validateActionEconomy(
            attackAction,
            turnState
        )
    }

    @Benchmark
    fun benchmarkResourceValidator(): Any {
        return resourceValidator.validateResources(
            spellAction,
            turnState.resourcePool
        )
    }

    @Benchmark
    fun benchmarkRangeValidator(): Any {
        return rangeValidator.validateRange(
            attackAction,
            GridPos(0, 0),
            GridPos(5, 0),
            encounterState
        )
    }

    @Benchmark
    fun benchmarkConcentrationValidator(): Any {
        return concentrationValidator.validateConcentration(
            spellAction,
            1L,
            turnState.concentrationState
        )
    }

    @Benchmark
    fun benchmarkConditionValidator(): Any {
        return conditionValidator.validateConditions(
            attackAction,
            conditions
        )
    }

    // ========== Complex Scenario Benchmarks ==========

    @Benchmark
    fun benchmarkComplexScenarioWithObstacles(): Any {
        val complexEncounterState = EncounterState(
            positions = mapOf(
                1L to GridPos(0, 0),
                2L to GridPos(10, 10)
            ),
            obstacles = (0..20).flatMap { x ->
                (0..20).map { y -> GridPos(x, y) }
            }.filter { it.x % 3 == 0 && it.y % 3 == 0 }.toSet()
        )
        
        return actionValidator.validate(
            spellAction,
            conditions,
            turnState,
            complexEncounterState
        )
    }

    @Benchmark
    fun benchmarkMultipleValidationsSequential(): Any {
        val results = mutableListOf<Any>()
        results.add(actionValidator.validate(attackAction, conditions, turnState, encounterState))
        results.add(actionValidator.validate(spellAction, conditions, turnState, encounterState))
        results.add(actionValidator.validate(moveAction, conditions, turnState, encounterState))
        results.add(actionValidator.validate(opportunityAttackAction, conditions, turnState, encounterState))
        return results
    }

    @Benchmark
    fun benchmarkFailFastCondition(): Any {
        return actionValidator.validate(
            attackAction,
            setOf(Condition.Stunned),
            turnState,
            encounterState
        )
    }

    @Benchmark
    fun benchmarkFailFastActionEconomy(): Any {
        val exhaustedState = turnState.copy(actionUsed = true)
        return actionValidator.validate(
            attackAction,
            conditions,
            exhaustedState,
            encounterState
        )
    }

    @Benchmark
    fun benchmarkFailFastResources(): Any {
        val noResourcesState = turnState.copy(
            resourcePool = ResourcePool(spellSlots = mapOf(1 to 0))
        )
        return actionValidator.validate(
            spellAction,
            conditions,
            noResourcesState,
            encounterState
        )
    }
}
