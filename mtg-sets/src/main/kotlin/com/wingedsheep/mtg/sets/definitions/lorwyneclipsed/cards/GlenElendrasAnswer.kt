package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Glen Elendra's Answer
 * {2}{U}{U}
 * Instant
 *
 * This spell can't be countered.
 * Counter all spells your opponents control and all abilities your opponents control.
 * Create a 1/1 blue and black Faerie creature token with flying for each spell and ability
 * countered this way.
 */
val GlenElendrasAnswer = card("Glen Elendra's Answer") {
    manaCost = "{2}{U}{U}"
    typeLine = "Instant"
    oracleText = "This spell can't be countered.\n" +
        "Counter all spells your opponents control and all abilities your opponents control. " +
        "Create a 1/1 blue and black Faerie creature token with flying for each spell and ability countered this way."

    cantBeCountered = true

    spell {
        effect = Effects.CounterAllOpponentStackObjects(storeCountAs = "countered")
            .then(
                CreateTokenEffect(
                    count = DynamicAmount.VariableReference("countered_count"),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE, Color.BLACK),
                    creatureTypes = setOf("Faerie"),
                    keywords = setOf(Keyword.FLYING),
                    imageUri = "https://cards.scryfall.io/normal/front/0/1/01524db2-c96f-4902-8394-bc7a7128e573.jpg?1767956498"
                )
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "52"
        artist = "Sam Guay"
        flavorText = "\"This is my domain, and I am its Queen.\"\n—Maralen, fae ascendant"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa5bfbf9-dca2-42b7-a431-f9afedb54528.jpg?1767659156"
    }
}
