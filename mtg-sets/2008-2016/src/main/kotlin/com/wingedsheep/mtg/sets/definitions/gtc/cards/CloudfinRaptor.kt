package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Cloudfin Raptor — Gatecrash #32
 * {U} · Creature — Bird Mutant · 0/1
 *
 * Flying
 * Evolve
 */
val CloudfinRaptor = card("Cloudfin Raptor") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Mutant"
    power = 0
    toughness = 1
    oracleText = "Flying\nEvolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)"

    keywords(Keyword.FLYING)
    evolve()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2931f27-43f9-4e52-aab3-967c26739e43.jpg?1783940138"
    }
}
