package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Costs
/**
 * Quarrel's End
 * {2}{R}
 * Sorcery
 *
 * As an additional cost to cast this spell, discard a card.
 * Draw two cards and create a 1/1 white Human Soldier creature token.
 */
val QuarrelsEnd = card("Quarrel's End") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, discard a card.\nDraw two cards and create a 1/1 white Human Soldier creature token."

    additionalCost(Costs.additional.DiscardCards())

    spell {
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(2),
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Human", "Soldier"),
                    imageUri = "https://cards.scryfall.io/normal/front/a/6/a6181330-7521-4ec6-be6c-b35487c2d2d4.jpg?1699974464"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Javier Charro"
        flavorText = "\"It is a joy to us to see you return into your own.\"\n—Éomer"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a7e61a6-c602-4089-ae45-828d8e516a63.jpg?1686969095"
    }
}
