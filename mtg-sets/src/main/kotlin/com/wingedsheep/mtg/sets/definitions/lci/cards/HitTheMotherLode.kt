package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hit the Mother Lode
 * {4}{R}{R}{R}
 * Sorcery
 * Discover 10. If the discovered card's mana value is less than 10, create a
 * number of tapped Treasure tokens equal to the difference.
 *
 * The discovered card is stored under "discovered"; the follow-up creates
 * `10 - (that card's mana value)` tapped Treasures (clamped at 0, so mana value
 * 10 makes none). The follow-up runs only when a card was discovered (CR 701.57c),
 * so a whiff makes no Treasures.
 */
val HitTheMotherLode = card("Hit the Mother Lode") {
    manaCost = "{4}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Discover 10. If the discovered card's mana value is less than 10, create a number of tapped Treasure tokens equal to the difference."
    spell {
        effect = Effects.Discover(
            amount = 10,
            storeDiscoveredAs = "discovered",
            thenEffect = Effects.CreateTreasure(
                count = DynamicAmount.IfPositive(
                    DynamicAmount.Subtract(
                        DynamicAmount.Fixed(10),
                        DynamicAmount.StoredCardManaValue("discovered")
                    )
                ),
                tapped = true,
            )
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e75f460c-43e2-4353-8b73-71ff8651a79d.jpg?1782694484"
    }
}
