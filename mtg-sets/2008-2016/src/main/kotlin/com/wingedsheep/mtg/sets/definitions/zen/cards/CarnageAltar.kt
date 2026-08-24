package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Carnage Altar
 * {2}
 * Artifact
 * {3}, Sacrifice a creature: Draw a card.
 *
 * A two-atom activation cost in the order the card prints it: [Costs.Composite] of
 * [Costs.Mana] `{3}` and [Costs.Sacrifice] over `GameObjectFilter.Creature`. The payoff is the
 * plain [Effects.DrawCards] facade at its default controller target.
 */
val CarnageAltar = card("Carnage Altar") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, Sacrifice a creature: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "James Paick"
        flavorText = "\"In these bloodstains I will find the fingerprints of our oppressors.\"\n—Anowon, the Ruin Sage"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52688a03-26a4-40ff-8a99-a268113a9802.jpg"
    }
}
