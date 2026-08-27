package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Magmatic Insight
 * {R}
 * Sorcery
 * As an additional cost to cast this spell, discard a land card.
 * Draw two cards.
 */
val MagmaticInsight = card("Magmatic Insight") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, discard a land card.\nDraw two cards."

    additionalCost(Costs.additional.DiscardCards(filter = GameObjectFilter.Land))

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Ryan Barger"
        flavorText = "Chief among the tenets of Purphoros is that one must destroy in order to create."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f00192e0-439d-43b2-882c-90a2d52103f8.jpg?1783938328"

        ruling("2015-06-22", "You must discard exactly one land card to cast Magmatic Insight. You can't cast it without discarding a land card, and you can't discard additional cards.")
    }
}
