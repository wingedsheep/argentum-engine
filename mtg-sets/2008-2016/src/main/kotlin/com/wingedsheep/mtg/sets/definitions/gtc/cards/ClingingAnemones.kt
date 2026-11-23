package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Clinging Anemones — Gatecrash #31
 * {3}{U} · Creature — Jellyfish · 1/4
 *
 * Defender
 * Evolve
 */
val ClingingAnemones = card("Clinging Anemones") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Jellyfish"
    power = 1
    toughness = 4
    oracleText = "Defender\nEvolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)"

    keywords(Keyword.DEFENDER)
    evolve()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e183069-096d-4977-8154-e7b60f17a787.jpg?1783940139"
    }
}
