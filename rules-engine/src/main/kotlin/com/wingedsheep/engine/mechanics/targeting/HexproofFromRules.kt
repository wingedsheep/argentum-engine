package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ProtectionScope

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

    /** Projected keyword for "hexproof from monocolored" (CR 105.2a — exactly one color). */
    const val MONOCOLORED: String = "HEXPROOF_FROM_MONOCOLORED"

    /** Projected keyword for "hexproof from multicolored" (CR 105.2b — two or more colors). */
    const val MULTICOLORED: String = "HEXPROOF_FROM_MULTICOLORED"

    /**
     * The projected `HEXPROOF_FROM_*` keywords that grant "hexproof from [scope]" — the same
     * spellings a printed hexproof-from projects (see `StateProjector`), so [blockingQuality] reads
     * a granted quality exactly as it reads a printed one. Empty for a scope with no hexproof
     * keyword (subtype, supertype, everything, each opponent); `GrantHexproofFromToGroup` rejects
     * those at construction.
     */
    fun keywordsFor(scope: ProtectionScope): Set<String> = when (scope) {
        is ProtectionScope.Color -> setOf("HEXPROOF_FROM_${scope.color.name}")
        is ProtectionScope.Colors -> scope.colors.mapTo(linkedSetOf()) { "HEXPROOF_FROM_${it.name}" }
        is ProtectionScope.NonColor -> setOf(nonColorKeyword(scope.color))
        ProtectionScope.Monocolored -> setOf(MONOCOLORED)
        ProtectionScope.Multicolored -> setOf(MULTICOLORED)
        is ProtectionScope.CardType -> setOf("HEXPROOF_FROM_CARDTYPE_${scope.cardType.uppercase()}")
        ProtectionScope.Spells,
        ProtectionScope.PermanentsCastThisTurn,
        ProtectionScope.ActivatedAbilities,
        ProtectionScope.TriggeredAbilities ->
            setOf(SourceKindProtection.hexproofKeyword(checkNotNull(SourceKind.of(scope))))
        is ProtectionScope.Subtype,
        is ProtectionScope.Supertype,
        ProtectionScope.Everything,
        ProtectionScope.EachOpponent -> emptySet()
    }

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
        if (sourceColors.size == 1 && projected.hasKeyword(targetId, MONOCOLORED)) {
            return "monocolored"
        }
        if (sourceColors.size >= 2 && projected.hasKeyword(targetId, MULTICOLORED)) {
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
