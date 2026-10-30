package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Fruit of Tizerus
 * {B}
 * Sorcery
 * Target player loses 2 life.
 * Escape—{3}{B}, Exile three other cards from your graveyard.
 *
 * A targeted [Effects.LoseLife] with Cling to Dust's escape; an escaped sorcery returns to the
 * graveyard after resolving, ready to escape again.
 */
val FruitOfTizerus = card("Fruit of Tizerus") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player loses 2 life.\n" +
        "Escape—{3}{B}, Exile three other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    spell {
        val player = target(Targets.Player)
        effect = Effects.LoseLife(2, player)
    }

    keywordAbility(KeywordAbility.escape("{3}{B}", Costs.additional.ExileOtherCards(3)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Bastien L. Deharme"
        flavorText = "One spiny tree with bitter fruit grows in the realm of Tizerus, outside the palace of Erebos."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66e577a0-e5d7-4e6a-919c-d85c2ae819ce.jpg?1783931567"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
