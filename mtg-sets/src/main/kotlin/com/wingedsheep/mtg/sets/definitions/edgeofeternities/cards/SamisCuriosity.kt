package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect

/**
 * Sami's Curiosity
 * {G}
 * Sorcery
 * You gain 2 life. Create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 */
val SamisCuriosity = card("Sami's Curiosity") {
    manaCost = "{G}"
    typeLine = "Sorcery"
    oracleText = "You gain 2 life. Create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    // You gain 2 life, then create a Lander token
    spell {
        effect = CompositeEffect(
            listOf(
                Effects.GainLife(2),
                Effects.CreateLander()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Tuan Duong Chu"
        flavorText = "Tannuk's gut warned of danger, but Sami forged ahead without heed."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/703ad0f3-bd05-42b0-85fb-0cd37807dc91.jpg?1753360552"
    }
}
