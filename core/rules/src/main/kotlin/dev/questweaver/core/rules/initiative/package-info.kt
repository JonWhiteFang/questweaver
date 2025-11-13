/**
 * Initiative & Turn Management System
 *
 * This package implements D&D 5e SRD-compatible initiative and turn order mechanics.
 * It provides deterministic initiative rolling, turn order management, turn phase tracking,
 * and round progression.
 *
 * Key components:
 * - InitiativeRoller: Rolls initiative for creatures
 * - InitiativeTracker: Manages turn order and progression
 * - TurnPhaseManager: Tracks action economy within a turn
 * - SurpriseHandler: Handles surprise round mechanics
 *
 * All components are pure Kotlin with no Android dependencies and use seeded random
 * number generation for deterministic behavior.
 */
package dev.questweaver.core.rules.initiative
