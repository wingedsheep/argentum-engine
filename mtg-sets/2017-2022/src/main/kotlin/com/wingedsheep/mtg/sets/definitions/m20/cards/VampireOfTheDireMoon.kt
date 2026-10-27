package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vampire of the Dire Moon
 * {B}
 * Creature — Vampire
 * 1/1
 * Deathtouch
 * Lifelink
 */
val VampireOfTheDireMoon = card("Vampire of the Dire Moon") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 1
    toughness = 1
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\n" +
        "Lifelink (Damage dealt by this creature also causes you to gain that much life.)"

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Anna Podedworna"
        flavorText = "\"This place suits me: craven authorities and plentiful prey.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3c185b9-5d97-4a5a-af0b-8b9c44dcd235.jpg?1783932986"
    }
}
