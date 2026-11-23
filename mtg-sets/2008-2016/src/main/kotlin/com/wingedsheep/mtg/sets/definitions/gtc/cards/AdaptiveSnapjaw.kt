package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Adaptive Snapjaw — Gatecrash #113
 * {4}{G} · Creature — Lizard Beast · 6/2
 *
 * Evolve
 */
val AdaptiveSnapjaw = card("Adaptive Snapjaw") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard Beast"
    power = 6
    toughness = 2
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)"

    evolve()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Eh, needs more toad.\"\n—Gulistan, Simic biomancer"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d3c0c43-2d6d-49b8-a112-07611a23ae69.jpg?1783940120"
    }
}
