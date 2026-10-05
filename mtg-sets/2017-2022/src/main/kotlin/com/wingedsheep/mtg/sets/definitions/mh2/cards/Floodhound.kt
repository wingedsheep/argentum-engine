package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Floodhound — Modern Horizons 2 #42
 * {U} · Creature — Elemental Dog · 1 / 2
 *
 * {3}, {T}: Investigate.
 *
 * Canonical definition lives in Modern Horizons 2, the earliest real printing (2021-06-18).
 * Reprinted in Jumpstart 2022 — see J22 `FloodhoundReprint`.
 */
val Floodhound = card("Floodhound") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Dog"
    power = 1
    toughness = 2
    oracleText = "{3}, {T}: Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this " +
        "token: Draw a card.\")"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Lius Lasahido"
        flavorText = "The thief doubled back twice, doused herself with perfume, crossed the ocean, " +
            "and still woke up with her pursuer close on her heels."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5b1ac05-bd87-4605-8443-0469276e1e3a.jpg?1783926879"

        ruling(
            "2021-06-18",
            "The token is named Clue Token and has the artifact subtype Clue. Clue isn't a creature type."
        )
    }
}
