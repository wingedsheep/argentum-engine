package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vengeful Vampire
 * {4}{B}{B}
 * Creature — Vampire
 * 3/2
 * Flying
 * Undying
 */
val VengefulVampire = card("Vengeful Vampire") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"

    keywords(Keyword.FLYING, Keyword.UNDYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Lucas Graciano"
        flavorText = "He wields the full power of wrath unfettered by the sympathies of a soul."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d03c64a7-37d2-4d8f-bd7a-9435bc2f4101.jpg"
    }
}
