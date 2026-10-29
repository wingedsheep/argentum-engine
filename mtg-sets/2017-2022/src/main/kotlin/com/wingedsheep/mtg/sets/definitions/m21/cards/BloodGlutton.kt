package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Blood Glutton
 * {4}{B}
 * Creature — Vampire
 * 4/3
 *
 * Lifelink
 */
val BloodGlutton = card("Blood Glutton") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 3
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)"

    keywords(Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Anna Podedworna"
        flavorText = "\"Some savor the hunt. I savor the kill.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28ff52a2-4223-4551-b388-b4dd21cc1437.jpg?1783930713"
    }
}
