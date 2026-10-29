package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * O'aka, Traveling Merchant — Final Fantasy Commander #39
 * {1}{U} · Legendary Creature — Human Citizen · 1/2
 *
 * {T}, Remove a counter from a nonland permanent you control: Draw a card.
 *
 * The counter is of any kind (`counterType = null`) off any nonland permanent its controller
 * controls — O'aka itself included (Power Conduit's cost, narrowed to nonland permanents).
 */
val OakaTravelingMerchant = card("O'aka, Traveling Merchant") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Citizen"
    power = 1
    toughness = 2
    oracleText = "{T}, Remove a counter from a nonland permanent you control: Draw a card."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounters(count = 1, counterType = null, filter = GameObjectFilter.NonlandPermanent),
        )
        effect = Effects.DrawCards(1)
        description = "{T}, Remove a counter from a nonland permanent you control: Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Fang Xinyu"
        flavorText = "\"Say, lad, you wouldn't have a bit o' gil to lend?\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d860089-fb88-4736-b7f4-b605551bedc8.jpg?1783906362"
    }
}
