package com.wingedsheep.engine.core

/**
 * Engine-wide feature flags. Defaults preserve legacy behaviour; flip a flag to
 * opt in to an in-progress migration without affecting other consumers of the
 * engine.
 *
 * See `docs/plans/combat-resolution-board.md` for the bipartite combat damage
 * UX migration this is currently used for.
 */
data class EngineFeatures(
    /**
     * When true, the combat damage manager emits the bipartite
     * [CombatResolutionDecision] in place of the legacy per-chooser
     * [CombatDamagePlanDecision]. Defaults to false — call sites that have
     * adopted the new board UI opt in explicitly.
     */
    val combatResolutionBoardEnabled: Boolean = false,
)
