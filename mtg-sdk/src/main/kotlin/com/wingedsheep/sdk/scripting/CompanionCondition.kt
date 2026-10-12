package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The deck-building condition of a companion ability (CR 702.139a: "Companion—[Condition]"),
 * checked against the player's **starting deck** — the deck after sideboard cards are set aside
 * and, in Commander, *before* the commander is set aside, so the commander counts (CR 702.139b).
 *
 * Pure data: [com.wingedsheep.sdk.model.CompanionEligibility] evaluates it against the printed
 * cards of a deck, which is all deck construction can see. Four shapes cover the printed
 * companions; each is parameterized by a [GameObjectFilter] read against the printed card, so a
 * new companion composes rather than adding a type:
 *
 * | Card    | Condition |
 * |---------|-----------|
 * | Gyruda  | `EachCard(Any.manaValueIsEven())` |
 * | Obosh   | `EachCard(Land or Any.manaValueIsOdd())` |
 * | Keruga  | `EachCard(Land or Any.manaValueAtLeast(3))` |
 * | Lurrus  | `EachCard(Nonpermanent or Permanent.manaValueAtMost(2))` |
 * | Kaheera | `EachCard(Noncreature or Any.withAnyOfSubtypes(Cat, Elemental, …))` |
 * | Zirda   | `EachCard(Nonpermanent or Any.withCardPredicate(HasActivatedAbility))` |
 * | Yorion  | `DeckSizeAboveMinimum(20)` |
 * | Umori   | `ShareACardType(Nonland)` |
 * | Lutri   | `DistinctNames(Nonland)` |
 *
 * Jegantha ("no card has more than one of the same mana symbol in its mana cost") reads the
 * mana cost's symbol multiset, which no filter predicate expresses; it is not modelled yet.
 */
@Serializable
sealed interface CompanionCondition {
    /** The condition as it reads after "Companion — ". */
    val description: String

    /**
     * Every card in the starting deck matches [filter] — "Each permanent card in your starting
     * deck has mana value 2 or less" is `EachCard(nonpermanent or permanent-with-MV≤2)`. A card
     * the condition doesn't speak about must be admitted by the filter (an `or` branch), since
     * the condition quantifies over the whole deck.
     *
     * The filter is read against the printed card only: card types, subtypes, colors, mana value
     * (X counts as 0 — ruling), keywords, and whether the card has an activated ability.
     */
    @SerialName("EachCard")
    @Serializable
    data class EachCard(val filter: GameObjectFilter) : CompanionCondition {
        override val description: String = "each card in your starting deck is ${filter.description}"
    }

    /**
     * The starting deck contains at least [extra] cards more than the format's minimum deck size
     * (Yorion, Sky Nomad). In Commander, whose deck size is exact, it can never hold (ruling).
     */
    @SerialName("DeckSizeAboveMinimum")
    @Serializable
    data class DeckSizeAboveMinimum(val extra: Int) : CompanionCondition {
        override val description: String =
            "your starting deck contains at least $extra cards more than the minimum deck size"
    }

    /**
     * Some single card type is shared by every card in the starting deck matching [among]
     * (Umori, the Collector: `among = Nonland`). One type must be common to *all* of them — an
     * artifact creature, an artifact and a creature share none (ruling). Supertypes and subtypes
     * aren't card types.
     */
    @SerialName("ShareACardType")
    @Serializable
    data class ShareACardType(val among: GameObjectFilter) : CompanionCondition {
        override val description: String = "each ${among.description} card in your starting deck shares a card type"
    }

    /**
     * No two cards in the starting deck matching [among] have the same name (Lutri, the
     * Spellchaser: `among = Nonland`). Only the deck is read — the companion itself is outside it.
     */
    @SerialName("DistinctNames")
    @Serializable
    data class DistinctNames(val among: GameObjectFilter) : CompanionCondition {
        override val description: String = "each ${among.description} card in your starting deck has a different name"
    }
}
