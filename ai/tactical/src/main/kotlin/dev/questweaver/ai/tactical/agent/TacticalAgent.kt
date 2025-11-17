package dev.questweaver.ai.tactical.agent

import dev.questweaver.ai.tactical.ActionCandidate
import dev.questweaver.ai.tactical.ActionType
import dev.questweaver.ai.tactical.DecisionReasoning
import dev.questweaver.ai.tactical.Difficulty
import dev.questweaver.ai.tactical.Resource
import dev.questweaver.ai.tactical.ScoredAction
import dev.questweaver.ai.tactical.TacticalAction
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.ai.tactical.TacticalDecision
import dev.questweaver.ai.tactical.behavior.BehaviorNode
import dev.questweaver.ai.tactical.behavior.BehaviorResult
import dev.questweaver.ai.tactical.behavior.BehaviorTreeFactory
import dev.questweaver.ai.tactical.positioning.PositioningStrategy
import dev.questweaver.ai.tactical.scoring.ActionScorer
import dev.questweaver.ai.tactical.targeting.TargetSelector
import dev.questweaver.core.rules.RulesEngine
import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.values.GridPos
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

/**
 * Main AI agent that decides actions for NPCs and AI-controlled party members.
 * 
 * Orchestrates the decision-making pipeline:
 * 1. Evaluate behavior tree to get action types
 * 2. Generate action candidates
 * 3. Score all candidates
 * 4. Select best action
 * 5. Select target if needed
 * 6. Select position if needed
 * 7. Validate with RulesEngine
 * 
 * Performance target: ≤300ms per decision
 * 
 * Requirements:
 * - 1.1: Traverse behavior tree to select action
 * - 6.1: Use seeded randomness for deterministic behavior
 * - 6.4: Log decision rationale
 * - 7.1: Complete decision within 300ms
 * - 7.2: Log warning if exceeds budget
 * - 8.1: Validate action with RulesEngine
 * - 8.2: Handle invalid actions
 * 
 * @property behaviorTreeFactory Factory for creating behavior trees
 * @property actionCandidateGenerator Generates valid action candidates
 * @property actionScorer Scores action candidates
 * @property targetSelector Selects optimal targets
 * @property positioningStrategy Determines optimal positioning
 * @property rulesEngine Validates actions against game rules
 * @property roller Seeded dice roller for deterministic randomness
 */
@Suppress("LongParameterList", "TooManyFunctions") 
// Acceptable: Dependency injection requires all components, orchestration requires helper functions
class TacticalAgent(
    private val behaviorTreeFactory: BehaviorTreeFactory,
    private val actionCandidateGenerator: ActionCandidateGenerator,
    private val actionScorer: ActionScorer,
    private val targetSelector: TargetSelector,
    private val positioningStrategy: PositioningStrategy,
    private val rulesEngine: RulesEngine,
    private val roller: DiceRoller
) {
    /**
     * Decides the best action for a creature's turn.
     * 
     * Task 11.1: Main entry point for tactical decisions
     * Task 11.3: Performance monitoring with 300ms budget
     * Task 11.6: Deterministic behavior using seeded roller
     * 
     * @param creature The creature making the decision
     * @param context Current tactical situation
     * @param difficulty AI difficulty level (affects decision quality)
     * @return TacticalDecision with action, target, position, and reasoning
     */
    suspend fun decideTurn(
        creature: Creature,
        context: TacticalContext,
        difficulty: Difficulty = Difficulty.NORMAL
    ): TacticalDecision {
        logger.info { "TacticalAgent deciding turn for ${creature.name} (difficulty: $difficulty)" }
        
        var decision: TacticalDecision? = null
        val totalTime = measureTimeMillis {
            decision = executePipeline(creature, context, difficulty)
        }
        
        logger.info { "Decision completed in ${totalTime}ms" }
        
        // Task 11.3: Check performance budget
        if (totalTime > PERFORMANCE_BUDGET_MS) {
            logger.warn { "Decision exceeded performance budget: ${totalTime}ms > ${PERFORMANCE_BUDGET_MS}ms" }
        }
        
        return decision!!
    }
    
    /**
     * Executes the full decision pipeline.
     * 
     * Task 11.2: Implement decision pipeline with all stages
     * Task 11.3: Track time for each pipeline stage
     * 
     * Pipeline stages:
     * 1. Evaluate behavior tree to get action types
     * 2. Generate action candidates
     * 3. Score all candidates
     * 4. Select best action
     * 5. Select target if needed
     * 6. Select position if needed
     * 7. Validate with RulesEngine
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount") 
    // Acceptable: Pipeline orchestration requires multiple stages
    private suspend fun executePipeline(
        creature: Creature,
        context: TacticalContext,
        difficulty: Difficulty
    ): TacticalDecision {
        val stageTimings = mutableMapOf<String, Long>()
        
        // Stage 1: Evaluate behavior tree
        var behaviorTree: BehaviorNode
        var behaviorResult: BehaviorResult
        var behaviorPath: String
        
        stageTimings["behaviorTree"] = measureTimeMillis {
            logger.debug { "Stage 1: Evaluating behavior tree" }
            behaviorTree = getBehaviorTree(creature, context)
            behaviorResult = behaviorTree.evaluate(creature, context)
            behaviorPath = extractBehaviorPath(behaviorResult)
        }
        
        logger.debug { "Behavior tree evaluation: ${stageTimings["behaviorTree"]}ms, path: $behaviorPath" }
        
        // Stage 2: Generate action candidates
        var candidates: List<ActionCandidate>
        
        stageTimings["candidateGeneration"] = measureTimeMillis {
            logger.debug { "Stage 2: Generating action candidates" }
            val actionTypes = extractActionTypes(behaviorResult)
            candidates = actionCandidateGenerator.generate(creature, context, actionTypes)
        }
        
        logger.debug { "Generated ${candidates.size} candidates in ${stageTimings["candidateGeneration"]}ms" }
        
        if (candidates.isEmpty()) {
            logger.warn { "No action candidates generated, falling back to default" }
            return createFallbackDecision(creature, context, behaviorPath)
        }
        
        // Stage 3: Score all candidates
        var scoredActions: List<ScoredAction>
        
        stageTimings["scoring"] = measureTimeMillis {
            logger.debug { "Stage 3: Scoring action candidates" }
            scoredActions = actionScorer.scoreAll(candidates, context, difficulty)
        }
        
        logger.debug { "Scored ${scoredActions.size} actions in ${stageTimings["scoring"]}ms" }
        
        if (scoredActions.isEmpty()) {
            logger.warn { "No scored actions available, falling back to default" }
            return createFallbackDecision(creature, context, behaviorPath)
        }
        
        // Stage 4: Select best action
        val bestAction = scoredActions.first()
        logger.debug { "Selected action: ${bestAction.candidate.action} (score: ${bestAction.score})" }
        
        // Stage 5: Select target if needed
        var target: Creature?
        
        stageTimings["targetSelection"] = measureTimeMillis {
            logger.debug { "Stage 5: Selecting target" }
            target = if (bestAction.candidate.targets.isNotEmpty()) {
                targetSelector.selectTarget(
                    action = bestAction.candidate.action,
                    candidates = bestAction.candidate.targets,
                    context = context
                )
            } else {
                null
            }
        }
        
        logger.debug { "Target selection: ${stageTimings["targetSelection"]}ms, target: ${target?.name}" }
        
        // Stage 6: Select position if needed
        var positionDecision: dev.questweaver.ai.tactical.positioning.PositionDecision?
        
        stageTimings["positioning"] = measureTimeMillis {
            logger.debug { "Stage 6: Selecting position" }
            positionDecision = if (requiresPositioning(bestAction.candidate.action)) {
                // Placeholder: would need MapGrid from context
                // For now, return current position
                null
            } else {
                null
            }
        }
        
        logger.debug { "Positioning: ${stageTimings["positioning"]}ms" }
        
        // Stage 7: Validate with RulesEngine
        var isValid: Boolean
        
        stageTimings["validation"] = measureTimeMillis {
            logger.debug { "Stage 7: Validating action" }
            isValid = validateAction(bestAction.candidate, creature, target, context)
        }
        
        logger.debug { "Validation: ${stageTimings["validation"]}ms, valid: $isValid" }
        
        // Log performance breakdown
        logPerformanceBreakdown(stageTimings)
        
        // Task 11.4: Handle invalid actions
        if (!isValid) {
            logger.warn { "Selected action is invalid, trying next-best" }
            return selectNextBestAction(scoredActions, creature, context, behaviorPath)
        }
        
        // Build decision reasoning
        val reasoning = DecisionReasoning(
            behaviorPath = behaviorPath,
            topScores = scoredActions.take(TOP_SCORES_TO_LOG),
            selectedScore = bestAction.score,
            opportunities = emptyList(), // Would extract from breakdown
            resourcesUsed = extractResourcesUsed(bestAction.candidate)
        )
        
        // Task 11.5: Log decision
        logDecision(creature, bestAction, target, reasoning)
        
        return TacticalDecision(
            action = bestAction.candidate.action,
            target = target,
            position = positionDecision?.position,
            path = positionDecision?.path ?: emptyList(),
            reasoning = reasoning
        )
    }
    
    /**
     * Gets the appropriate behavior tree for a creature.
     */
    private fun getBehaviorTree(creature: Creature, context: TacticalContext): BehaviorNode {
        val hpPercent = creature.hitPointsCurrent.toFloat() / creature.hitPointsMax
        
        return when {
            hpPercent < LOW_HP_THRESHOLD -> behaviorTreeFactory.defensive()
            hasSpellSlots(creature, context) -> behaviorTreeFactory.spellcaster()
            hasRangedWeapon(creature) -> behaviorTreeFactory.rangedAttacker()
            else -> behaviorTreeFactory.aggressiveMelee()
        }
    }
    
    /**
     * Extracts behavior path from behavior result.
     */
    private fun extractBehaviorPath(result: BehaviorResult): String {
        return when (result) {
            is BehaviorResult.Success -> "Success"
            is BehaviorResult.Failure -> "Failure"
            is BehaviorResult.ActionCandidates -> "ActionCandidates(${result.candidates.size})"
        }
    }
    
    /**
     * Extracts action types from behavior result.
     */
    private fun extractActionTypes(result: BehaviorResult): List<ActionType> {
        return when (result) {
            is BehaviorResult.ActionCandidates -> {
                result.candidates.map { it.action.actionType }.distinct()
            }
            else -> listOf(ActionType.DODGE)
        }
    }
    
    /**
     * Checks if action requires positioning.
     */
    private fun requiresPositioning(action: TacticalAction): Boolean {
        return action.actionType in listOf(
            ActionType.MOVE,
            ActionType.DASH,
            ActionType.DISENGAGE
        )
    }
    
    /**
     * Validates action with RulesEngine.
     * 
     * Task 11.2: Validate with RulesEngine
     */
    @Suppress("UnusedParameter")
    private fun validateAction(
        candidate: ActionCandidate,
        creature: Creature,
        target: Creature?,
        context: TacticalContext
    ): Boolean {
        // Placeholder: always valid for now
        // In full implementation, would call rulesEngine.validateAction()
        return true
    }
    
    /**
     * Selects next-best action when primary action is invalid.
     * 
     * Task 11.4: Handle invalid actions by selecting next-best
     */
    private suspend fun selectNextBestAction(
        scoredActions: List<ScoredAction>,
        creature: Creature,
        context: TacticalContext,
        behaviorPath: String
    ): TacticalDecision {
        for (action in scoredActions.drop(1)) {
            val target = if (action.candidate.targets.isNotEmpty()) {
                targetSelector.selectTarget(
                    action = action.candidate.action,
                    candidates = action.candidate.targets,
                    context = context
                )
            } else {
                null
            }
            
            val isValid = validateAction(action.candidate, creature, target, context)
            
            if (isValid) {
                logger.info { "Selected next-best action: ${action.candidate.action}" }
                
                val reasoning = DecisionReasoning(
                    behaviorPath = behaviorPath,
                    topScores = scoredActions.take(TOP_SCORES_TO_LOG),
                    selectedScore = action.score,
                    opportunities = emptyList(),
                    resourcesUsed = extractResourcesUsed(action.candidate)
                )
                
                return TacticalDecision(
                    action = action.candidate.action,
                    target = target,
                    position = null,
                    path = emptyList(),
                    reasoning = reasoning
                )
            }
        }
        
        // All actions invalid, fall back to default
        logger.warn { "All actions invalid, falling back to default" }
        return createFallbackDecision(creature, context, behaviorPath)
    }
    
    /**
     * Creates fallback decision when no valid actions available.
     * 
     * Task 11.4: Fall back to Dodge or Attack nearest enemy
     */
    private fun createFallbackDecision(
        creature: Creature,
        context: TacticalContext,
        behaviorPath: String
    ): TacticalDecision {
        val enemies = context.getEnemies(creature)
        
        val action = if (enemies.isEmpty()) {
            TacticalAction.Dodge
        } else {
            val nearestEnemy = findNearestEnemy(creature, enemies, context)
            if (nearestEnemy != null) {
                TacticalAction.Attack("Basic Attack")
            } else {
                TacticalAction.Dodge
            }
        }
        
        logger.info { "Fallback decision: $action" }
        
        val reasoning = DecisionReasoning(
            behaviorPath = "$behaviorPath > Fallback",
            topScores = emptyList(),
            selectedScore = 0f,
            opportunities = emptyList(),
            resourcesUsed = emptyList()
        )
        
        return TacticalDecision(
            action = action,
            target = if (action is TacticalAction.Attack) findNearestEnemy(creature, enemies, context) else null,
            position = null,
            path = emptyList(),
            reasoning = reasoning
        )
    }
    
    /**
     * Finds nearest enemy to creature.
     */
    private fun findNearestEnemy(
        creature: Creature,
        enemies: List<Creature>,
        context: TacticalContext
    ): Creature? {
        val creaturePos = context.getPosition(creature.id) ?: return enemies.firstOrNull()
        
        return enemies.minByOrNull { enemy ->
            val enemyPos = context.getPosition(enemy.id)
            if (enemyPos != null) {
                calculateDistance(creaturePos, enemyPos)
            } else {
                Int.MAX_VALUE
            }
        }
    }
    
    /**
     * Calculates grid distance between positions.
     */
    private fun calculateDistance(pos1: GridPos, pos2: GridPos): Int {
        return kotlin.math.max(kotlin.math.abs(pos1.x - pos2.x), kotlin.math.abs(pos1.y - pos2.y))
    }
    
    /**
     * Extracts resources used from action candidate.
     */
    private fun extractResourcesUsed(candidate: ActionCandidate): List<Resource> {
        val resources = mutableListOf<Resource>()
        
        candidate.resourceCost.spellSlot?.let { level ->
            resources.add(Resource.SpellSlot(level))
        }
        
        candidate.resourceCost.abilityUse?.let { name ->
            resources.add(Resource.LimitedAbility(name, 1))
        }
        
        candidate.resourceCost.consumableItem?.let { name ->
            resources.add(Resource.ConsumableItem(name))
        }
        
        return resources
    }
    
    /**
     * Logs decision details for debugging.
     * 
     * Task 11.5: Log decision rationale
     */
    private fun logDecision(
        creature: Creature,
        bestAction: ScoredAction,
        target: Creature?,
        reasoning: DecisionReasoning
    ) {
        logger.info { 
            "Decision for ${creature.name}: " +
            "action=${bestAction.candidate.action}, " +
            "target=${target?.name}, " +
            "score=${bestAction.score}, " +
            "path=${reasoning.behaviorPath}"
        }
        
        logger.debug {
            "Top 3 actions: ${reasoning.topScores.take(TOP_SCORES_TO_LOG).joinToString { 
                "${it.candidate.action} (${it.score})" 
            }}"
        }
        
        if (reasoning.resourcesUsed.isNotEmpty()) {
            logger.debug {
                "Resources used: ${reasoning.resourcesUsed.joinToString { 
                    when (it) {
                        is Resource.SpellSlot -> "Spell Slot ${it.level}"
                        is Resource.LimitedAbility -> it.name
                        is Resource.ConsumableItem -> it.name
                    }
                }}"
            }
        }
        
        if (reasoning.opportunities.isNotEmpty()) {
            logger.debug {
                "Opportunities: ${reasoning.opportunities.joinToString { it.toString() }}"
            }
        }
    }
    
    /**
     * Logs performance breakdown for each pipeline stage.
     * 
     * Task 11.3: Track time for each pipeline stage
     * Task 11.3: Log warning if exceeds 300ms budget
     */
    private fun logPerformanceBreakdown(stageTimings: Map<String, Long>) {
        val totalTime = stageTimings.values.sum()
        
        logger.debug {
            "Performance breakdown: " +
            "behaviorTree=${stageTimings["behaviorTree"]}ms, " +
            "candidateGen=${stageTimings["candidateGeneration"]}ms, " +
            "scoring=${stageTimings["scoring"]}ms, " +
            "targetSelection=${stageTimings["targetSelection"]}ms, " +
            "positioning=${stageTimings["positioning"]}ms, " +
            "validation=${stageTimings["validation"]}ms, " +
            "total=${totalTime}ms"
        }
        
        // Check if any stage exceeded its budget
        stageTimings.forEach { (stage, time) ->
            val budget = when (stage) {
                "behaviorTree" -> BEHAVIOR_TREE_BUDGET_MS
                "candidateGeneration" -> CANDIDATE_GEN_BUDGET_MS
                "scoring" -> SCORING_BUDGET_MS
                "targetSelection" -> TARGET_SELECTION_BUDGET_MS
                "positioning" -> POSITIONING_BUDGET_MS
                "validation" -> VALIDATION_BUDGET_MS
                else -> Long.MAX_VALUE
            }
            
            if (time > budget) {
                logger.warn { "Stage '$stage' exceeded budget: ${time}ms > ${budget}ms" }
            }
        }
    }
    
    /**
     * Checks if creature has spell slots.
     */
    private fun hasSpellSlots(creature: Creature, context: TacticalContext): Boolean {
        return context.availableSpellSlots[creature.id]?.values?.any { it > 0 } == true
    }
    
    /**
     * Checks if creature has ranged weapon.
     * Placeholder implementation.
     */
    @Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
    private fun hasRangedWeapon(creature: Creature): Boolean {
        // Placeholder: would check creature's equipment
        return false
    }
    
    companion object {
        private const val LOW_HP_THRESHOLD = 0.3f
        private const val TOP_SCORES_TO_LOG = 3
        private const val PERFORMANCE_BUDGET_MS = 300L
        
        // Stage-specific performance budgets (from design doc)
        private const val BEHAVIOR_TREE_BUDGET_MS = 20L
        private const val CANDIDATE_GEN_BUDGET_MS = 50L
        private const val SCORING_BUDGET_MS = 150L
        private const val TARGET_SELECTION_BUDGET_MS = 30L
        private const val POSITIONING_BUDGET_MS = 40L
        private const val VALIDATION_BUDGET_MS = 10L
    }
}
