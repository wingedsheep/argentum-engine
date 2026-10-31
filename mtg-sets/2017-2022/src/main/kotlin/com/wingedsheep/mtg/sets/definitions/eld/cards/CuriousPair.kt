package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Curious Pair // Treats to Share
 * {1}{G}
 * Creature — Human Peasant
 * 1/3
 *
 * Adventure: Treats to Share — {G}, Sorcery — Adventure
 * Create a Food token.
 */
val CuriousPair = card("Curious Pair") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Peasant"
    power = 1
    toughness = 3
    oracleText = ""

    adventure("Treats to Share") {
        manaCost = "{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create a Food token. (Then exile this card. You may cast the creature later " +
            "from exile. A Food token is an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"
        spell {
            effect = Effects.CreateFood()
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Daarken"
        flavorText = "\"It's gingerbread, like Mother makes. What is there to be afraid of?\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f78a570-d776-42f2-a609-6da0156c8de7.jpg?1783932614"
    }
}
