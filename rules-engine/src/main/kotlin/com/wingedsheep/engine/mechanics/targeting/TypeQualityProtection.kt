package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * The type axis of protection (CR 702.16a): "protection from Goblins" (a subtype), "protection
 * from legendary creatures" (a supertype narrowed to a card type) and "protection from God
 * creatures" (a subtype narrowed to a card type) — [com.wingedsheep.sdk.scripting.ProtectionScope.Subtype]
 * and [com.wingedsheep.sdk.scripting.ProtectionScope.Supertype] — and the subtype's complement,
 * "protection from non-Human creatures" (`Subtype(negated = true)`).
 *
 * Projected as `PROTECTION_FROM_SUBTYPE_<X>` / `PROTECTION_FROM_SUPERTYPE_<X>`, with
 * `@<CARDTYPE>` appended when the quality is narrowed and a leading `!` on a negated subtype
 * (`PROTECTION_FROM_SUBTYPE_!HUMAN@CREATURE`). Every protection read site (targeting,
 * attach, blocking, damage, the AI's combat math) asks this one question, so the card-type
 * narrowing can't be forgotten at one of them.
 */
object TypeQualityProtection {
    const val SUBTYPE_PREFIX = ProtectionScope.Subtype.KEYWORD_PREFIX
    const val SUPERTYPE_PREFIX = "PROTECTION_FROM_SUPERTYPE_"
    private const val CARD_TYPE_SEPARATOR = ProtectionScope.Subtype.CARD_TYPE_SEPARATOR
    private const val NEGATION_MARK = ProtectionScope.Subtype.NEGATION_MARK

    /** The keyword for protection from [subtype], optionally narrowed to [cardType] or [negated]. */
    fun subtypeKeyword(subtype: String, cardType: String? = null, negated: Boolean = false): String =
        ProtectionScope.Subtype(subtype, cardType, negated).protectionKeyword()

    /** The keyword for protection from [supertype], optionally narrowed to [cardType]. */
    fun supertypeKeyword(supertype: String, cardType: String? = null): String = SUPERTYPE_PREFIX + quality(supertype, cardType)

    /**
     * A quality as [com.wingedsheep.engine.state.components.identity.ProtectionComponent] stores it
     * and the keyword suffixes it: `GOBLIN`, `GOD@CREATURE`.
     */
    fun quality(name: String, cardType: String?): String =
        name.uppercase() + (cardType?.let { "$CARD_TYPE_SEPARATOR${it.uppercase()}" } ?: "")

    /** A stored quality taken apart: its type name, its card-type narrowing, and its negation. */
    data class Quality(val name: String, val cardType: String?, val negated: Boolean)

    /** Splits a stored [quality] into its type name, card-type narrowing and negation mark. */
    fun split(quality: String): Quality {
        val negated = quality.startsWith(NEGATION_MARK)
        val body = if (negated) quality.substring(1) else quality
        val at = body.indexOf(CARD_TYPE_SEPARATOR)
        return if (at < 0) Quality(body, null, negated)
        else Quality(body.substring(0, at), body.substring(at + 1), negated)
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
            val (name, cardType, negated) = split(keyword.removePrefix(if (isSubtype) SUBTYPE_PREFIX else SUPERTYPE_PREFIX))
            val sourceNames = if (isSubtype) sourceSubtypes else sourceSupertypes
            // A negated quality ("non-Human creatures") matches the sources lacking the type. Every
            // negated printing narrows to a card type, which a source with no known types fails; an
            // un-narrowed "protection from non-Xs" would need a known-source guard first.
            if (sourceNames.any { it.equals(name, ignoreCase = true) } == negated) continue
            if (cardType != null && sourceCardTypes.none { it.equals(cardType, ignoreCase = true) }) continue
            val shown = (if (negated) "non-" else "") +
                if (isSubtype) name.lowercase().replaceFirstChar { it.uppercase() } else name.lowercase()
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
