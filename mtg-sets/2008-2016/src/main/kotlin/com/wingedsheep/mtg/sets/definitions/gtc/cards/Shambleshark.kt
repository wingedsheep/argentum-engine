package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Shambleshark — Gatecrash #193
 * {G}{U} · Creature — Shark Crab · 2/1
 *
 * Flash
 * Evolve
 */
val Shambleshark = card("Shambleshark") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Shark Crab"
    power = 2
    toughness = 1
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Evolve (Whenever a creature you control enters, if that creature has greater power or toughness " +
        "than this creature, put a +1/+1 counter on this creature.)"

    keywords(Keyword.FLASH)

    evolve()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "193"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07855a17-4e68-4257-af7f-275c9fb0a9b8.jpg?1783940101"
    }
}
