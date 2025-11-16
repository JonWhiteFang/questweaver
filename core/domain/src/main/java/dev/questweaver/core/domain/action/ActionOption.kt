package dev.questweaver.core.domain.action

import dev.questweaver.core.domain.intent.NLAction

/**
 * Represents a disambiguated action option that the player can choose.
 * 
 * When an action is ambiguous (e.g., "attack the goblin" when there are
 * multiple goblins), this represents one specific interpretation that
 * the player can select.
 * 
 * @property description Human-readable description of this option (e.g., "Attack Goblin #1 at E5")
 * @property action The fully-specified action that will be executed if chosen
 */
data class ActionOption(
    val description: String,
    val action: NLAction
)
