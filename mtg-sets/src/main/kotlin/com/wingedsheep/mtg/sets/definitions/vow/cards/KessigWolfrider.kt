package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kessig Wolfrider
 * {R}
 * Creature — Human Knight
 * 1/2
 *
 * Menace
 * {2}{R}, {T}, Exile three cards from your graveyard: Create a 3/2 red Wolf creature token.
 *
 * The activated ability composes {2}{R} + tap + exile-three-from-graveyard ([Costs.ExileFromGraveyard],
 * the Mines of Moria cost idiom) and creates one 3/2 red Wolf token.
 */
val KessigWolfrider = card("Kessig Wolfrider") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 1
    toughness = 2
    oracleText = "Menace\n" +
        "{2}{R}, {T}, Exile three cards from your graveyard: Create a 3/2 red Wolf creature token."

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{R}"),
            Costs.Tap,
            Costs.ExileFromGraveyard(3)
        )
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Wolf")
        )
        description = "{2}{R}, {T}, Exile three cards from your graveyard: Create a 3/2 red Wolf " +
            "creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Bram Sels"
        flavorText = "\"It's a perfect partnership. My village is safe from wolf attacks, and she " +
            "gets to eat any vampires we catch.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/180b1a4f-c071-4742-9c57-9d775be0ed4f.jpg?1782703072"
    }
}
