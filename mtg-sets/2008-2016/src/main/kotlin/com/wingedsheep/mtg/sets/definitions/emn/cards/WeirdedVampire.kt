package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity

/** Weirded Vampire — Eldritch Moon #113. */
val WeirdedVampire = card("Weirded Vampire") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Horror"
    oracleText = "Madness {2}{B} (If you discard this card, discard it into exile. When you do, cast " +
        "it for its madness cost or put it into your graveyard.)"
    power = 3
    toughness = 3

    madness("{2}{B}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "David Gaillet"
        flavorText = "\"Emrakul makes apparent those whom she has favored.\"\n—Runo Stromkirk"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb5234ac-e308-417d-96b0-d860f3186e6b.jpg"
    }
}
