package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lonesome Unicorn // Rider in Need
 * {4}{W}
 * Creature — Unicorn
 * 3/3
 *
 * Vigilance
 *
 * Adventure: Rider in Need — {2}{W}, Sorcery — Adventure
 * Create a 2/2 white Knight creature token with vigilance.
 */
val LonesomeUnicorn = card("Lonesome Unicorn") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Unicorn"
    power = 3
    toughness = 3
    oracleText = "Vigilance"

    keywords(Keyword.VIGILANCE)

    adventure("Rider in Need") {
        manaCost = "{2}{W}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create a 2/2 white Knight creature token with vigilance. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Knight"),
                keywords = setOf(Keyword.VIGILANCE),
                imageUri = "https://cards.scryfall.io/normal/front/7/0/703e7ecf-3d73-40c1-8cfe-0758778817cf.jpg?1783932483",
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99083707-2152-42c0-b5c3-b4f97ec20190.jpg?1783932670"
    }
}
