package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Sea Gate Restoration {4}{U}{U}{U} // Sea Gate, Reborn — Zendikar Rising #76
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Sea Gate Restoration: "Draw cards equal to the number of cards in your hand plus one. You
 * have no maximum hand size for the rest of the game."
 * Back — Sea Gate, Reborn: "As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {U}."
 *
 * The hand is counted on resolution, while the spell itself is on the stack, so an empty hand draws
 * one card (per the ruling).
 */
private val SeaGateRestorationFront = card("Sea Gate Restoration") {
    manaCost = "{4}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw cards equal to the number of cards in your hand plus one. You have no maximum " +
        "hand size for the rest of the game."

    spell {
        effect = Effects.DrawCards(DynamicAmounts.cardsInYourHand() + 1) then
            Effects.RemoveMaximumHandSize()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "76"
        artist = "Adam Paquette"
        flavorText = "\"We have much to remember ... and much we can never forget.\"\n—Tazri"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/193071fe-180b-4d35-ba78-9c16675c29fc.jpg?1783929396"
        ruling("2020-09-25", "If you have no cards in hand as Sea Gate Restoration resolves, you draw one card.")
    }
}

private val SeaGateRebornBack = card("Sea Gate, Reborn") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "76"
        artist = "Adam Paquette"
        flavorText = "\"We carry on the legacy of those who defended our world by continuing to unearth its secrets.\"\n—Tazri"
        imageUri = "https://cards.scryfall.io/normal/back/1/9/193071fe-180b-4d35-ba78-9c16675c29fc.jpg?1783929396"
    }
}

val SeaGateRestoration: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SeaGateRestorationFront,
    backFace = SeaGateRebornBack,
)
