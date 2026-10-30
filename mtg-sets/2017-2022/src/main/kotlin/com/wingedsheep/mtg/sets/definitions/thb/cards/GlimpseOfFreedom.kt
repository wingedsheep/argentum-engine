package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Glimpse of Freedom
 * {1}{U}
 * Instant
 * Draw a card.
 * Escape—{2}{U}, Exile five other cards from your graveyard.
 *
 * A one-card draw with Cling to Dust's escape; the escaped instant returns to the graveyard after
 * resolving, ready to escape again.
 */
val GlimpseOfFreedom = card("Glimpse of Freedom") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card.\n" +
        "Escape—{2}{U}, Exile five other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    spell {
        effect = Effects.DrawCards(1)
    }

    keywordAbility(KeywordAbility.escape("{2}{U}", Costs.additional.ExileOtherCards(5)))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Clint Cearley"
        flavorText = "\"Freedom lies in accepting a single path.\"\n—Nyklos, Meletian philosopher"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1e9c025-1cdb-4da8-8d28-14ad5efb512d.jpg?1783931584"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
