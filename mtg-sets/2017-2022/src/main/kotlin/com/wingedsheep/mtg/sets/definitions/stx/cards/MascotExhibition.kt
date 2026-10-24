package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mascot Exhibition — Strixhaven: School of Mages #5 (canonical printing)
 * {7} · Sorcery — Lesson
 *
 * Create a 2/1 white and black Inkling creature token with flying, a 3/2 red and white Spirit
 * creature token, and a 4/4 blue and red Elemental creature token.
 *
 * Three [Effects.CreateToken]s in sequence — the Inkling, Spirit and Elemental Summoning tokens.
 * Token art resolves through STX's synced token printings. Lesson is only a subtype.
 */
val MascotExhibition = card("Mascot Exhibition") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Sorcery — Lesson"
    oracleText = "Create a 2/1 white and black Inkling creature token with flying, a 3/2 red and " +
        "white Spirit creature token, and a 4/4 blue and red Elemental creature token."

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Inkling"),
            keywords = setOf(Keyword.FLYING)
        ) then Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Spirit")
        ) then Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.BLUE, Color.RED),
            creatureTypes = setOf("Elemental")
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Randy Vargas"
        flavorText = "\"Don't just win the game. Win the crowd.\"\n—Moriana, Mage Tower coach"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2047592c-739c-41dd-b3e8-31ae1c7688ab.jpg?1783927395"
    }
}
