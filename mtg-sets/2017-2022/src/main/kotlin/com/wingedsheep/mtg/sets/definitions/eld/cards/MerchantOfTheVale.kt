package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Merchant of the Vale // Haggle
 * {2}{R}
 * Creature — Human Peasant
 * 2/3
 *
 * {2}{R}, Discard a card: Draw a card.
 *
 * Adventure: Haggle — {R}, Instant — Adventure
 * You may discard a card. If you do, draw a card.
 */
val MerchantOfTheVale = card("Merchant of the Vale") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 3
    oracleText = "{2}{R}, Discard a card: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.DiscardCard)
        effect = Effects.DrawCards(1)
        description = "{2}{R}, Discard a card: Draw a card."
    }

    adventure("Haggle") {
        manaCost = "{R}"
        typeLine = "Instant — Adventure"
        oracleText = "You may discard a card. If you do, draw a card. (Then exile this card. You may " +
            "cast the creature later from exile.)"
        spell {
            effect = Effects.May(
                effect = Effects.IfYouDo(
                    action = Patterns.Hand.discardCards(1),
                    then = Effects.DrawCards(1)
                ),
                descriptionOverride = "You may discard a card. If you do, draw a card."
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "David Gaillet"
        flavorText = "\"Buying old, worn-out junk. Selling marvels of real value.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b4399b6-e67f-40d8-8676-f5db7e04a6c9.jpg?1783932621"
    }
}
