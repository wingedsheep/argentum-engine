package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId

/**
 * The single "does this source match one of the target's hexproof-from qualities" check
 * (CR 702.11d: "hexproof from [quality]" means the permanent can't be the target of [quality]
 * spells your opponents control or abilities your opponents control from [quality] sources).
 *
 * Every targeting site — cast/activation validation (`TargetValidator`), legal-target enumeration
 * (`TargetFinder`, `TargetEnumerationUtils`) and the resolution-time re-check
 * (`ResolutionTargetValidator`) — asks this one function, so a new quality is wired once instead of
 * four times. The *who* half (opponents only, hexproof-suppressing effects) stays with each caller;
 * this answers only the *what* half, reading the projected `HEXPROOF_FROM_*` keywords.
 */
object HexproofFromRules {

    private val nonColorKeywords: Map<Color, String> = Color.entries.associateWith { nonColorKeyword(it) }

    /** Projected keyword for "hexproof from non<color>" (Thrun, Breaker of Silence). */
    fun nonColorKeyword(color: Color): String = "HEXPROOF_FROM_NON_${color.name}"

    /**
     * The quality of [targetId]'s hexproof the source matches — "white", "monocolored",
     * "nongreen", "instants" — or null when none does.
     *
     * @param sourceColors the source's color names; empty means colorless when [sourceKnown].
     * @param sourceCardTypes the source's card-type names (any case).
     * @param sourceKnown whether the colors describe a real source. An unknown source matches no
     *   non-color quality, since "colorless" can't be told from "no information".
     */
    fun blockingQuality(
        projected: ProjectedState,
        targetId: EntityId,
        sourceColors: Set<String>,
        sourceCardTypes: Set<String>,
        sourceKnown: Boolean
    ): String? {
        for (colorName in sourceColors) {
            if (projected.hasKeyword(targetId, "HEXPROOF_FROM_$colorName")) return colorName.lowercase()
        }
        // Hexproof from monocolored / multicolored: exactly one / two or more colors (CR 105.2a/b).
        if (sourceColors.size == 1 && projected.hasKeyword(targetId, "HEXPROOF_FROM_MONOCOLORED")) {
            return "monocolored"
        }
        if (sourceColors.size >= 2 && projected.hasKeyword(targetId, "HEXPROOF_FROM_MULTICOLORED")) {
            return "multicolored"
        }
        // Hexproof from non<color>: every source that isn't that color, colorless included
        // (CR 105.2c) — a green-white source is green, so it isn't "nongreen".
        if (sourceKnown) {
            for ((color, keyword) in nonColorKeywords) {
                if (color.name !in sourceColors && projected.hasKeyword(targetId, keyword)) {
                    return "non${color.name.lowercase()}"
                }
            }
        }
        for (cardType in sourceCardTypes) {
            if (projected.hasKeyword(targetId, "HEXPROOF_FROM_CARDTYPE_${cardType.uppercase()}")) {
                return "${cardType.lowercase()}s"
            }
        }
        return null
    }
}
