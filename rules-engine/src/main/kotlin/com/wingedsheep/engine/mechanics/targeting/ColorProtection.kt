package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.model.EntityId

/**
 * The colour axis of protection (CR 702.16a): "protection from <colour>" and "protection from
 * colorless". A colorless object has no color (CR 105.2c), so protection from colorless matches a
 * source whose colour set is empty rather than one that contains some named colour.
 *
 * Every protection read site (targeting, attach, blocking, damage) asks this one question so the
 * colorless case can't be forgotten at one of them.
 */
object ColorProtection {
    /** The quality name protection from colorless is keyed by: `PROTECTION_FROM_COLORLESS`. */
    const val COLORLESS = "COLORLESS"
    const val PROTECTION_FROM_COLORLESS = "PROTECTION_FROM_$COLORLESS"

    /**
     * The colour quality [protectedId] has protection from that a source with [sourceColorNames]
     * carries — one of its colours, or [COLORLESS] for a source with none — or null when protection
     * doesn't apply. Callers pass colours only for a source they actually know: an unknown source
     * has no colours either, and must not read as colorless.
     */
    fun matchedQuality(projected: ProjectedState, protectedId: EntityId, sourceColorNames: Collection<String>): String? =
        if (sourceColorNames.isEmpty()) COLORLESS.takeIf { projected.hasKeyword(protectedId, PROTECTION_FROM_COLORLESS) }
        else sourceColorNames.firstOrNull { projected.hasKeyword(protectedId, "PROTECTION_FROM_$it") }

    fun isProtected(projected: ProjectedState, protectedId: EntityId, sourceColorNames: Collection<String>): Boolean =
        matchedQuality(projected, protectedId, sourceColorNames) != null

    /** Display text for a matched quality: "red", "colorless". */
    fun describe(quality: String): String = quality.lowercase()
}
