package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.model.EntityId

/**
 * The type axis of protection (CR 702.16a): "protection from Goblins" (a subtype), "protection
 * from legendary creatures" (a supertype narrowed to a card type) and "protection from God
 * creatures" (a subtype narrowed to a card type) — [com.wingedsheep.sdk.scripting.ProtectionScope.Subtype]
 * and [com.wingedsheep.sdk.scripting.ProtectionScope.Supertype].
 *
 * Projected as `PROTECTION_FROM_SUBTYPE_<X>` / `PROTECTION_FROM_SUPERTYPE_<X>`, with
 * `@<CARDTYPE>` appended when the quality is narrowed. Every protection read site (targeting,
 * attach, blocking, damage, the AI's combat math) asks this one question, so the card-type
 * narrowing can't be forgotten at one of them.
 */
object TypeQualityProtection {
    const val SUBTYPE_PREFIX = "PROTECTION_FROM_SUBTYPE_"
    const val SUPERTYPE_PREFIX = "PROTECTION_FROM_SUPERTYPE_"
    private const val CARD_TYPE_SEPARATOR = '@'

    /** The keyword for protection from [subtype], optionally narrowed to [cardType]. */
    fun subtypeKeyword(subtype: String, cardType: String? = null): String = SUBTYPE_PREFIX + quality(subtype, cardType)

    /** The keyword for protection from [supertype], optionally narrowed to [cardType]. */
    fun supertypeKeyword(supertype: String, cardType: String? = null): String = SUPERTYPE_PREFIX + quality(supertype, cardType)

    /**
     * A quality as [com.wingedsheep.engine.state.components.identity.ProtectionComponent] stores it
     * and the keyword suffixes it: `GOBLIN`, `GOD@CREATURE`.
     */
    fun quality(name: String, cardType: String?): String =
        name.uppercase() + (cardType?.let { "$CARD_TYPE_SEPARATOR${it.uppercase()}" } ?: "")

    /** Splits a stored [quality] into its type name and its card-type narrowing, if any. */
    fun split(quality: String): Pair<String, String?> {
        val at = quality.indexOf(CARD_TYPE_SEPARATOR)
        return if (at < 0) quality to null else quality.substring(0, at) to quality.substring(at + 1)
    }

    /**
     * The type quality [protectedId] has protection from that a source with these characteristics
     * carries, described for a message ("Goblins", "God creatures", "legendary creatures"), or null
     * when none applies. Callers pass the source's projected characteristics on the battlefield and
     * its card's elsewhere (a spell on the stack, an Aura about to enter).
     */
    fun matchedQuality(
        projected: ProjectedState,
        protectedId: EntityId,
        sourceSubtypes: Collection<String>,
        sourceSupertypes: Collection<String>,
        sourceCardTypes: Collection<String>
    ): String? {
        for (keyword in projected.getKeywords(protectedId)) {
            val isSubtype = keyword.startsWith(SUBTYPE_PREFIX)
            if (!isSubtype && !keyword.startsWith(SUPERTYPE_PREFIX)) continue
            val (name, cardType) = split(keyword.removePrefix(if (isSubtype) SUBTYPE_PREFIX else SUPERTYPE_PREFIX))
            val sourceNames = if (isSubtype) sourceSubtypes else sourceSupertypes
            if (sourceNames.none { it.equals(name, ignoreCase = true) }) continue
            if (cardType != null && sourceCardTypes.none { it.equals(cardType, ignoreCase = true) }) continue
            val shown = if (isSubtype) name.lowercase().replaceFirstChar { it.uppercase() } else name.lowercase()
            return when {
                cardType != null -> "$shown ${cardType.lowercase()}s"
                isSubtype -> "${shown}s"
                else -> "$shown permanents"
            }
        }
        return null
    }

    /** [matchedQuality] for a source on the battlefield, read entirely from [projected]. */
    fun matchedQuality(projected: ProjectedState, protectedId: EntityId, sourceId: EntityId): String? =
        matchedQuality(
            projected, protectedId,
            projected.getSubtypes(sourceId), projected.getSupertypes(sourceId), projected.getTypes(sourceId)
        )
}
