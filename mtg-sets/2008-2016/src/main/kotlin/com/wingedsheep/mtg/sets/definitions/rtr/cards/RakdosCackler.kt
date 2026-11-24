package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Rakdos Cackler — Return to Ravnica #220 (canonical printing)
 * {B/R} · Creature — Devil · 1/1
 *
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val RakdosCackler = card("Rakdos Cackler") {
    manaCost = "{B/R}"
    colorIdentity = "BR"
    typeLine = "Creature — Devil"
    power = 1
    toughness = 1
    oracleText = "Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    unleash()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f873c0b-e779-4f09-8e9c-94a1765eb5da.jpg?1783940326"
    }
}
