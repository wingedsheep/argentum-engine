package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * The four elemental "bending" keyword actions of *Avatar: The Last Airbender*
 * (CR 701.65 Airbend, 701.66 Earthbend, 701.67 Waterbend, 702.189 Firebending).
 *
 * A [com.wingedsheep.sdk.scripting.EventPattern.BendPerformedEvent] carries which one a player just
 * performed, so a "Whenever you waterbend, earthbend, firebend, or airbend, …" trigger
 * ([com.wingedsheep.sdk.dsl.Triggers.YouBend]) fires uniformly off any of them, and the per-turn
 * distinct-bend tracker ([com.wingedsheep.sdk.scripting.values.TurnTracker.DISTINCT_BENDS]) backs
 * "if you've done all four this turn" (Avatar Aang).
 */
@Serializable
enum class BendType(val oracleVerb: String) {
    WATER("waterbend"),
    EARTH("earthbend"),
    FIRE("firebend"),
    AIR("airbend");

    companion object {
        /** All four bend types — the default set matched by "whenever you … bend" triggers. */
        val ALL: Set<BendType> = entries.toSet()
    }
}
