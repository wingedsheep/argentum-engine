package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants a keyword (or ability flag) to a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for Equipment/Auras granting a keyword to the
 * attached creature, [GroupFilter.source] for "this creature has flying", or any
 * battlefield-scoped filter for lord effects ("Other creatures you control have flying").
 *
 * The [keyword] field stores the `Keyword`/`AbilityFlag` enum name (e.g., "FLYING", "DOESNT_UNTAP"),
 * or a parameterized keyword's projected string (`KeywordAbility.BandsWithOther.projectedKeyword`),
 * which the engine uses for string-based keyword checks in projected state.
 */
@SerialName("GrantKeyword")
@Serializable
data class GrantKeyword(
    val keyword: String,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    constructor(keyword: Keyword, filter: GroupFilter = GroupFilter.attachedCreature()) :
        this(keyword.name, filter)

    /**
     * Grant a "bands with other [quality]" ability (CR 702.22b) — "White legendary creatures you
     * control have 'bands with other legendary creatures'" (Cathedral of Serra), or a token's own
     * "It has 'bands with other creatures named Wolves of the Hunt'" with [GroupFilter.source].
     */
    constructor(ability: KeywordAbility.BandsWithOther, filter: GroupFilter = GroupFilter.attachedCreature()) :
        this(ability.projectedKeyword, filter)

    override val description: String =
        "${filter.description} have ${com.wingedsheep.sdk.scripting.util.keywordDisplayText(keyword)}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * "As long as a creature card with flying is in a graveyard, this creature has flying. The same is
 * true for fear, first strike, …" — Cairn Wanderer; "As long as a card exiled with Eater of Virtue
 * has flying, equipped creature has flying. The same is true for …" — Eater of Virtue.
 *
 * The affected permanents have each of [keywords] that some card in the [cards] pool has.
 * [families] are the printed words that name a *family* rather than one keyword: "landwalk" means
 * every landwalk ability found (swampwalk, nonbasic landwalk, …), "protection" every protection
 * ability found, quality and all — a card with protection from red grants protection from red, not
 * a generic "protection" — and "hexproof" every hexproof-from ability on top of plain hexproof (the
 * printed rulings: Cairn Wanderer "gains any landwalk abilities and any protection abilities";
 * Eater of Virtue's creature "gains any hexproof and protection abilities of cards exiled with" it).
 *
 * The pool is read each time characteristics are determined, so the keywords come and go as cards
 * enter and leave it. A card in a graveyard or in exile has only its printed abilities (nothing on
 * the battlefield can grant it one), so the card's own keywords are what count.
 */
@SerialName("GainKeywordsOfCards")
@Serializable
data class GainKeywordsOfCards(
    val keywords: List<Keyword>,
    val families: Set<KeywordFamily> = emptySet(),
    val cards: KeywordCardPool = KeywordCardPool.CREATURE_CARDS_IN_GRAVEYARDS,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = buildString {
        val names = keywords.map { it.displayName.lowercase() } +
            families.sortedBy { it.ordinal }.map { it.displayName }
        val first = names.firstOrNull() ?: "an ability"
        when (cards) {
            KeywordCardPool.CREATURE_CARDS_IN_GRAVEYARDS ->
                append("As long as a creature card with $first is in a graveyard, ")
            KeywordCardPool.CARDS_EXILED_WITH_SOURCE ->
                append("As long as a card exiled with this permanent has $first, ")
        }
        append("${filter.description} has ${names.firstOrNull() ?: "it"}")
        if (names.size > 1) {
            append(". The same is true for ")
            val rest = names.drop(1)
            append(if (rest.size == 1) rest.single() else rest.dropLast(1).joinToString(", ") + ", and " + rest.last())
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/** Which cards [GainKeywordsOfCards] reads keywords off. */
@Serializable
enum class KeywordCardPool {
    /** Every creature card in every graveyard (Cairn Wanderer). */
    CREATURE_CARDS_IN_GRAVEYARDS,

    /**
     * The source's linked-exile pile (CR 607 — "cards exiled with this permanent"), only the ones
     * still in exile; a new object after a zone change starts with an empty pile (Eater of Virtue).
     */
    CARDS_EXILED_WITH_SOURCE
}

/** A printed word naming every keyword of a family, qualities and all — see [GainKeywordsOfCards]. */
@Serializable
enum class KeywordFamily(val displayName: String) {
    /** Every landwalk ability (swampwalk, nonbasic landwalk, …). */
    LANDWALK("landwalk"),

    /** Every protection ability, with its quality. */
    PROTECTION("protection"),

    /** Plain hexproof and every "hexproof from [quality]". */
    HEXPROOF("hexproof")
}

/**
 * Removes a keyword from the affected permanents (continuous static ability).
 * Used for Equipment that causes the equipped creature to lose a keyword.
 * E.g., Starforged Sword: "Equipped creature loses flying."
 */
@SerialName("RemoveKeywordStatic")
@Serializable
data class RemoveKeywordStatic(
    val keyword: String,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    constructor(keyword: Keyword, filter: GroupFilter = GroupFilter.attachedCreature()) :
        this(keyword.name, filter)

    override val description: String = "Removes ${com.wingedsheep.sdk.scripting.util.keywordDisplayText(keyword)}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants ward (with a configurable cost) to a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for Auras/Equipment that grant ward to the
 * attached creature, or any battlefield-scoped filter for lord-style "Other
 * creatures you control have ward {1}" effects.
 *
 * Both the WARD keyword display (via the layer system) and the enforcement
 * trigger (via TriggerAbilityResolver.getWardTriggeredAbilities) are generated
 * by the engine.
 *
 * Examples:
 *   Innkeeper's Talent L2 — "Permanents you control with counters have ward {1}."
 *     → GrantWard(WardCost.Mana("{1}"), GroupFilter(...))
 *   Hexing Squelcher — "Other creatures you control have 'Ward—Pay 2 life.'"
 *     → GrantWard(WardCost.Life(2), GroupFilter.OtherCreaturesYouControl)
 *   Aura granting ward to enchanted creature → GrantWard(cost) (default attachedCreature scope)
 */
@SerialName("GrantWard")
@Serializable
data class GrantWard(
    val cost: WardCost,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = when (cost) {
        is WardCost.Mana -> "${filter.description} have ward ${cost.manaCost}"
        is WardCost.Life -> "${filter.description} have \"Ward—Pay ${cost.amount} life.\""
        is WardCost.DynamicLife -> "${filter.description} have \"Ward—Pay life equal to ${cost.amount.description}.\""
        is WardCost.Discard -> "${filter.description} have \"Ward—Discard ${cost.description}.\""
        is WardCost.Sacrifice -> "${filter.description} have \"Ward—Sacrifice ${cost.description}.\""
        is WardCost.CollectEvidence ->
            "${filter.description} have \"Ward—Collect evidence ${cost.amount}.\""
        is WardCost.PlayerCounters -> "${filter.description} have \"Ward—Get ${cost.description}.\""
        is WardCost.Choice ->
            "${filter.description} have \"Ward—${cost.clause.replaceFirstChar { it.uppercase() }}.\""
        is WardCost.Composite -> "${filter.description} have \"Ward—${cost.description}.\""
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants the landwalk keyword matching the basic land type chosen as the source entered
 * (resolved at projection time from the source's `CastChoicesComponent`):
 * Plains→Plainswalk, Island→Islandwalk, Swamp→Swampwalk, Mountain→Mountainwalk, Forest→Forestwalk.
 *
 * Used for Auras like Traveler's Cloak ("As this Aura enters, choose a basic land type. Enchanted
 * creature has landwalk of the chosen type."). The chosen-value counterpart to [GrantKeyword],
 * mirroring [SetEnchantedLandTypeFromChosen]'s relationship to [SetEnchantedLandType]. If the source
 * has no chosen land type, nothing is granted.
 *
 * Pair with `replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))` so the chosen type is
 * recorded when the permanent enters.
 */
@SerialName("GrantLandwalkOfChosenType")
@Serializable
data class GrantLandwalkOfChosenType(
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} have landwalk of the chosen type"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants a keyword to all creatures that have a specific counter type.
 * Used for Aurification: "Each creature with a gold counter on it has defender."
 * With [controllerOnly] = true, only affects creatures you control (e.g., outlast lords).
 *
 * @property keyword The keyword to grant
 * @property counterType The counter type that creatures must have
 * @property controllerOnly If true, only affects creatures you control
 */
@SerialName("GrantKeywordByCounter")
@Serializable
data class GrantKeywordByCounter(
    val keyword: Keyword,
    val counterType: CounterType,
    val controllerOnly: Boolean = false
) : StaticAbility {
    override val description: String =
        "Each creature ${if (controllerOnly) "you control " else ""}with a ${counterType.printed} counter on it has ${keyword.name.lowercase().replace('_', ' ')}"
}
