package dev.questweaver.ai.tactical.behavior

import dev.questweaver.ai.tactical.ActionCandidate
import dev.questweaver.ai.tactical.ActionType
import dev.questweaver.ai.tactical.ResourceCost
import dev.questweaver.ai.tactical.TacticalAction
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.domain.entities.Creature

/**
 * Factory for creating common behavior trees.
 * Provides pre-built trees for different creature archetypes.
 */
class BehaviorTreeFactory {
    
    companion object {
        private const val LOW_HP_THRESHOLD = 0.3
        private const val MELEE_RANGE = 1
        private const val CLUSTERED_ENEMY_COUNT = 3
        private const val SPELL_LEVEL_FIREBALL = 3
        private const val FEET_PER_GRID_SQUARE = 5
    }
    /**
     * Creates an aggressive melee behavior tree.
     * Prioritizes attacking enemies in melee range, moving to attack, or dashing toward enemies.
     * Falls back to defensive actions when HP is low.
     */
    @Suppress("LongMethod")
    fun aggressiveMelee(): BehaviorNode {
        return SelectorNode(
            listOf(
                // Low HP: defensive actions
                SequenceNode(
                    listOf(
                        ConditionNode(
                            predicate = { creature, _ -> 
                                creature.hitPointsCurrent < creature.hitPointsMax * LOW_HP_THRESHOLD
                            },
                            description = "HP < 30%"
                        ),
                        ActionNode(
                            actionType = ActionType.DISENGAGE,
                            priority = 10,
                            generator = { creature, context ->
                                listOf(
                                    ActionCandidate(
                                        action = TacticalAction.Disengage,
                                        targets = emptyList(),
                                        positions = emptyList(),
                                        resourceCost = ResourceCost()
                                    )
                                )
                            }
                        )
                    )
                ),
                // Can attack in melee range
                SequenceNode(
                    listOf(
                        ConditionNode(
                            predicate = { creature, context ->
                                val enemies = context.getEnemies(creature)
                                val creaturePos = context.getPosition(creature.id)
                                creaturePos != null && enemies.any { enemy ->
                                    val enemyPos = context.getPosition(enemy.id)
                                    enemyPos != null && creaturePos.distanceTo(enemyPos) <= MELEE_RANGE
                                }
                            },
                            description = "Enemy in melee range"
                        ),
                        ActionNode(
                            actionType = ActionType.ATTACK,
                            priority = 8,
                            generator = { creature, context ->
                                // Generate attack candidates (simplified for now)
                                listOf(
                                    ActionCandidate(
                                        action = TacticalAction.Attack("Melee Weapon"),
                                        targets = context.getEnemies(creature),
                                        positions = emptyList(),
                                        resourceCost = ResourceCost()
                                    )
                                )
                            }
                        )
                    )
                ),
                // Move to attack
                SequenceNode(
                    listOf(
                        ConditionNode(
                            predicate = { creature, context ->
                                val enemies = context.getEnemies(creature)
                                val creaturePos = context.getPosition(creature.id)
                                creaturePos != null && enemies.any { enemy ->
                                    val enemyPos = context.getPosition(enemy.id)
                                    val maxDistance = creature.speed / FEET_PER_GRID_SQUARE + MELEE_RANGE
                                    enemyPos != null && creaturePos.distanceTo(enemyPos) <= maxDistance
                                }
                            },
                            description = "Enemy within movement + melee range"
                        ),
                        ActionNode(
                            actionType = ActionType.MOVE,
                            priority = 6,
                            generator = { creature, context ->
                                listOf(
                                    ActionCandidate(
                                        action = TacticalAction.Move(creature.speed),
                                        targets = context.getEnemies(creature),
                                        positions = emptyList(), // Will be filled by positioning strategy
                                        resourceCost = ResourceCost()
                                    )
                                )
                            }
                        )
                    )
                ),
                // Dash toward nearest enemy
                ActionNode(
                    actionType = ActionType.DASH,
                    priority = 4,
                    generator = { creature, context ->
                        listOf(
                            ActionCandidate(
                                action = TacticalAction.Dash,
                                targets = context.getEnemies(creature),
                                positions = emptyList(),
                                resourceCost = ResourceCost()
                            )
                        )
                    }
                )
            )
        )
    }
    
    /**
     * Creates a ranged attacker behavior tree.
     * Prioritizes maintaining distance, using cover, and attacking from range.
     */
    @Suppress("LongMethod")
    fun rangedAttacker(): BehaviorNode {
        return SelectorNode(
            listOf(
                // Threatened in melee: disengage and move to cover
                SequenceNode(
                    listOf(
                        ConditionNode(
                            predicate = { creature, context ->
                                val enemies = context.getEnemies(creature)
                                val creaturePos = context.getPosition(creature.id)
                                creaturePos != null && enemies.any { enemy ->
                                    val enemyPos = context.getPosition(enemy.id)
                                    enemyPos != null && creaturePos.distanceTo(enemyPos) <= MELEE_RANGE
                                }
                            },
                            description = "Enemy in melee range"
                        ),
                        ActionNode(
                            actionType = ActionType.DISENGAGE,
                            priority = 10,
                            generator = { creature, context ->
                                listOf(
                                    ActionCandidate(
                                        action = TacticalAction.Disengage,
                                        targets = emptyList(),
                                        positions = emptyList(),
                                        resourceCost = ResourceCost()
                                    )
                                )
                            }
                        )
                    )
                ),
                // Has clear shot: attack
                SequenceNode(
                    listOf(
                        ConditionNode(
                            predicate = { creature, context ->
                                context.getEnemies(creature).isNotEmpty()
                            },
                            description = "Has targets"
                        ),
                        ActionNode(
                            actionType = ActionType.ATTACK,
                            priority = 8,
                            generator = { creature, context ->
                                listOf(
                                    ActionCandidate(
                                        action = TacticalAction.Attack("Ranged Weapon"),
                                        targets = context.getEnemies(creature),
                                        positions = emptyList(),
                                        resourceCost = ResourceCost()
                                    )
                                )
                            }
                        )
                    )
                ),
                // Hold position and dodge
                ActionNode(
                    actionType = ActionType.DODGE,
                    priority = 4,
                    generator = { creature, context ->
                        listOf(
                            ActionCandidate(
                                action = TacticalAction.Dodge,
                                targets = emptyList(),
                                positions = emptyList(),
                                resourceCost = ResourceCost()
                            )
                        )
                    }
                )
            )
        )
    }
    
    /**
     * Creates a spellcaster behavior tree.
     * Prioritizes high-value spells, AoE opportunities, and cantrips.
     */
    fun spellcaster(): BehaviorNode {
        return SelectorNode(
            listOf(
                // High-value AoE opportunity
                SequenceNode(
                    listOf(
                        ConditionNode(
                            predicate = { creature, context ->
                                // Check if 3+ enemies are clustered (simplified)
                                context.getEnemies(creature).size >= CLUSTERED_ENEMY_COUNT
                            },
                            description = "3+ enemies clustered"
                        ),
                        ActionNode(
                            actionType = ActionType.CAST_SPELL,
                            priority = 10,
                            generator = { creature, context ->
                                listOf(
                                    ActionCandidate(
                                        action = TacticalAction.CastSpell("Fireball", SPELL_LEVEL_FIREBALL),
                                        targets = context.getEnemies(creature),
                                        positions = emptyList(),
                                        resourceCost = ResourceCost(spellSlot = SPELL_LEVEL_FIREBALL)
                                    )
                                )
                            }
                        )
                    )
                ),
                // Cantrip attack
                ActionNode(
                    actionType = ActionType.CAST_SPELL,
                    priority = 6,
                    generator = { creature, context ->
                        listOf(
                            ActionCandidate(
                                action = TacticalAction.CastSpell("Fire Bolt", 0),
                                targets = context.getEnemies(creature),
                                positions = emptyList(),
                                resourceCost = ResourceCost()
                            )
                        )
                    }
                )
            )
        )
    }
    
    /**
     * Creates a defensive behavior tree.
     * Prioritizes survival, healing, and defensive actions.
     */
    fun defensive(): BehaviorNode {
        return SelectorNode(
            listOf(
                // Critical HP: dodge
                SequenceNode(
                    listOf(
                        ConditionNode(
                            predicate = { creature, _ ->
                                creature.hitPointsCurrent < creature.hitPointsMax * LOW_HP_THRESHOLD
                            },
                            description = "HP < 30%"
                        ),
                        ActionNode(
                            actionType = ActionType.DODGE,
                            priority = 10,
                            generator = { creature, context ->
                                listOf(
                                    ActionCandidate(
                                        action = TacticalAction.Dodge,
                                        targets = emptyList(),
                                        positions = emptyList(),
                                        resourceCost = ResourceCost()
                                    )
                                )
                            }
                        )
                    )
                ),
                // Help action
                ActionNode(
                    actionType = ActionType.HELP,
                    priority = 6,
                    generator = { creature, context ->
                        listOf(
                            ActionCandidate(
                                action = TacticalAction.Help,
                                targets = context.getAllies(creature),
                                positions = emptyList(),
                                resourceCost = ResourceCost()
                            )
                        )
                    }
                )
            )
        )
    }
}
