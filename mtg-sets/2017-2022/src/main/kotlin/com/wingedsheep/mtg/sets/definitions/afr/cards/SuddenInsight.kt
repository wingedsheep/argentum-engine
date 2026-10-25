package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Sudden Insight
 * {4}{U}{U}
 * Instant
 *
 * Draw a card for each different mana value among nonland cards in your graveyard.
 *
 * The count is the number of *distinct* mana values among the nonland cards in your graveyard
 * (two cards sharing a mana value count once) — [DynamicAmounts.zone] over your graveyard filtered
 * to [GameObjectFilter.Nonland], aggregated with `distinctValues(MANA_VALUE)`. Mana value 0 is a
 * value like any other, and a card with {X} in its cost has mana value 0 there (per rulings). The
 * count is taken on resolution, while Sudden Insight is still on the stack, so it never counts
 * itself.
 */
val SuddenInsight = card("Sudden Insight") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card for each different mana value among nonland cards in your graveyard."

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Nonland)
                .distinctValues(CardNumericProperty.MANA_VALUE)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Dan Murayama Scott"
        flavorText = "A lucky draw from the Deck of Many Things gave Voltusk an inexplicable flash of " +
            "understanding that he spent the rest of his life trying to repeat."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06ec3c87-d5aa-4f8a-a9bf-ef4e38e21ad1.jpg?1783926506"
        ruling("2021-07-23", "If a card in your graveyard has {X} in its mana cost, X is 0 when determining that card's mana value.")
        ruling("2021-07-23", "Although land cards aren't considered, nonland cards with mana value 0 are. For example if the cards in your graveyard are an artifact card with mana cost {X} and a sorcery card with mana cost {2}{U}, you'll draw two cards.")
    }
}
