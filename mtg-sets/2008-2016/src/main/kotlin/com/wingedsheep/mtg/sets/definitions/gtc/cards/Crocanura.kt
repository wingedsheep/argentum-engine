package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Crocanura — Gatecrash #116
 * {2}{G} · Creature — Crocodile Frog · 1/3
 *
 * Reach
 * Evolve
 */
val Crocanura = card("Crocanura") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Crocodile Frog"
    power = 1
    toughness = 3
    oracleText = "Reach (This creature can block creatures with flying.)\nEvolve (Whenever a creature you control " +
        "enters, if that creature has greater power or toughness than this creature, put a +1/+1 counter on this creature.)"

    keywords(Keyword.REACH)
    evolve()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b459a988-97b0-4370-b89a-2565f8721b60.jpg?1783940119"
    }
}
