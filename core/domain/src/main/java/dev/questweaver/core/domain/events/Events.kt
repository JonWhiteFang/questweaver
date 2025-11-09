package dev.questweaver.core.domain.events

import kotlinx.serialization.Serializable

@Serializable
sealed interface Event {
    val ts: Long
    val type: String
}

@Serializable
data class AttackRolled(
    override val ts: Long,
    val attackerId: String,
    val targetId: String,
    val roll: Int,
    val adv: Int,
): Event { override val type: String = "AttackRolled" }
