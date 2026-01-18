package com.wingedsheep.rulesengine.game

import kotlinx.serialization.Serializable

@Serializable
enum class Step(val phase: Phase, val displayName: String) {
    // Beginning Phase
    UNTAP(Phase.BEGINNING, "Untap Step"),
    UPKEEP(Phase.BEGINNING, "Upkeep Step"),
    DRAW(Phase.BEGINNING, "Draw Step"),

    // Precombat Main Phase (no steps, just the phase)
    PRECOMBAT_MAIN(Phase.PRECOMBAT_MAIN, "Precombat Main Phase"),

    // Combat Phase
    BEGIN_COMBAT(Phase.COMBAT, "Beginning of Combat Step"),
    DECLARE_ATTACKERS(Phase.COMBAT, "Declare Attackers Step"),
    DECLARE_BLOCKERS(Phase.COMBAT, "Declare Blockers Step"),
    COMBAT_DAMAGE(Phase.COMBAT, "Combat Damage Step"),
    END_COMBAT(Phase.COMBAT, "End of Combat Step"),

    // Postcombat Main Phase (no steps, just the phase)
    POSTCOMBAT_MAIN(Phase.POSTCOMBAT_MAIN, "Postcombat Main Phase"),

    // Ending Phase
    END(Phase.ENDING, "End Step"),
    CLEANUP(Phase.ENDING, "Cleanup Step");

    val isMainPhase: Boolean
        get() = this == PRECOMBAT_MAIN || this == POSTCOMBAT_MAIN

    val allowsSorcerySpeed: Boolean
        get() = isMainPhase

    val hasPriority: Boolean
        get() = this != UNTAP && this != CLEANUP

    fun next(): Step = when (this) {
        UNTAP -> UPKEEP
        UPKEEP -> DRAW
        DRAW -> PRECOMBAT_MAIN
        PRECOMBAT_MAIN -> BEGIN_COMBAT
        BEGIN_COMBAT -> DECLARE_ATTACKERS
        DECLARE_ATTACKERS -> DECLARE_BLOCKERS
        DECLARE_BLOCKERS -> COMBAT_DAMAGE
        COMBAT_DAMAGE -> END_COMBAT
        END_COMBAT -> POSTCOMBAT_MAIN
        POSTCOMBAT_MAIN -> END
        END -> CLEANUP
        CLEANUP -> UNTAP // Wraps to next turn
    }

    companion object {
        val FIRST = UNTAP

        fun stepsInPhase(phase: Phase): List<Step> = entries.filter { it.phase == phase }

        fun firstStepOf(phase: Phase): Step = when (phase) {
            Phase.BEGINNING -> UNTAP
            Phase.PRECOMBAT_MAIN -> PRECOMBAT_MAIN
            Phase.COMBAT -> BEGIN_COMBAT
            Phase.POSTCOMBAT_MAIN -> POSTCOMBAT_MAIN
            Phase.ENDING -> END
        }

        fun lastStepOf(phase: Phase): Step = when (phase) {
            Phase.BEGINNING -> DRAW
            Phase.PRECOMBAT_MAIN -> PRECOMBAT_MAIN
            Phase.COMBAT -> END_COMBAT
            Phase.POSTCOMBAT_MAIN -> POSTCOMBAT_MAIN
            Phase.ENDING -> CLEANUP
        }
    }
}
