package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Sea Gate Wreckage — Oath of the Gatewatch #177 (canonical printing)
 * Land
 * {T}: Add {C}.
 * {2}{C}, {T}: Draw a card. Activate only if you have no cards in hand.
 *
 * The hand check is an activation restriction, not an intervening "if": it's read only when the
 * ability is activated, so the card is drawn however many cards are in hand on resolution. The {C}
 * in the cost is colorless mana specifically, not generic.
 */
val SeaGateWreckage = card("Sea Gate Wreckage") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}. ({C} represents colorless mana.)\n" +
        "{2}{C}, {T}: Draw a card. Activate only if you have no cards in hand."

    // {T}: Add {C}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    // {2}{C}, {T}: Draw a card. Activate only if you have no cards in hand.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{C}"),
            Costs.Tap
        )
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.EmptyHand)
        )
        description = "Draw a card. Activate only if you have no cards in hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd42576b-9de5-4d77-9d30-1ef5b5a4fd73.jpg?1783937892"
        ruling(
            "2016-01-22",
            "It doesn't matter how many cards are in your hand as the last ability resolves. For example, if you " +
                "have no cards in hand and control two Sea Gate Wreckages, you can activate the last ability of " +
                "each of them. You'll draw a card as each ability resolves."
        )
    }
}
