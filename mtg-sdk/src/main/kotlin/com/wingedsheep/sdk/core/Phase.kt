package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

@Serializable
enum class Phase(val displayName: String) {
    BEGINNING("Beginning Phase"),
    PRECOMBAT_MAIN("Precombat Main Phase"),
    COMBAT("Combat Phase"),
    POSTCOMBAT_MAIN("Postcombat Main Phase"),
    ENDING("Ending Phase");

    val isMainPhase: Boolean
        get() = this == PRECOMBAT_MAIN || this == POSTCOMBAT_MAIN

    fun next(): Phase = when (this) {
        BEGINNING -> PRECOMBAT_MAIN
        PRECOMBAT_MAIN -> COMBAT
        COMBAT -> POSTCOMBAT_MAIN
        POSTCOMBAT_MAIN -> ENDING
        ENDING -> BEGINNING // Wraps to next turn
    }

    companion object {
        val FIRST = BEGINNING
    }
}
