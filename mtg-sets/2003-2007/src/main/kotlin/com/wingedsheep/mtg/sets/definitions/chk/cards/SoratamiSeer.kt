package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Soratami Seer
 * {4}{U}
 * Creature — Moonfolk Wizard
 * 2/3
 * Flying
 * {4}, Return two lands you control to their owner's hand: Discard all the cards in your hand,
 * then draw that many cards.
 *
 * The bounced lands are a cost, so they are already in hand when the ability resolves — they are
 * discarded with the rest of the hand and count toward "that many". The draw reads
 * [Patterns.Hand.discardedHand]'s count, the same handle Collective Defiance and Balin use.
 */
val SoratamiSeer = card("Soratami Seer") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Moonfolk Wizard"
    oracleText = "Flying\n{4}, Return two lands you control to their owner's hand: Discard all " +
        "the cards in your hand, then draw that many cards."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.ReturnToHand(Filters.Land, count = 2))
        effect = Patterns.Hand.discardHand() then
            Effects.DrawCards(Patterns.Hand.discardedHand.count)
        description = "{4}, Return two lands you control to their owner's hand: Discard all the " +
            "cards in your hand, then draw that many cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Glen Angus"
        flavorText = "Their mirrors show two worlds: that which is and that which should be."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/462a861d-e5c5-48b1-95c0-3ef04b690eac.jpg?1783944320"
    }
}
