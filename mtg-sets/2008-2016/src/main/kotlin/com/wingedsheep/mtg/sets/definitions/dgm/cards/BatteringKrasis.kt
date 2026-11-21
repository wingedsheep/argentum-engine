package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Battering Krasis — Dragon's Maze #41
 * {2}{G} · Creature — Shark Beast · 2/1
 *
 * Trample
 * Evolve
 */
val BatteringKrasis = card("Battering Krasis") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Shark Beast"
    power = 2
    toughness = 1
    oracleText = "Trample\nEvolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)"

    keywords(Keyword.TRAMPLE)
    evolve()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d9aa740-9adf-412a-b6ec-0b9bb1b4618b.jpg?1783940036"
    }
}
