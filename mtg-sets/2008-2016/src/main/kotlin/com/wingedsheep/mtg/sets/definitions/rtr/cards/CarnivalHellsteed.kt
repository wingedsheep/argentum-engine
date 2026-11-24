package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Carnival Hellsteed — Return to Ravnica #147 (canonical printing)
 * {4}{B}{R} · Creature — Nightmare Horse · 5/4
 *
 * First strike, haste
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val CarnivalHellsteed = card("Carnival Hellsteed") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Nightmare Horse"
    power = 5
    toughness = 4
    oracleText = "First strike, haste\nUnleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)
    unleash()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8ada7ce-c693-48f0-a6e3-766f61d93370.jpg?1783940343"
    }
}
