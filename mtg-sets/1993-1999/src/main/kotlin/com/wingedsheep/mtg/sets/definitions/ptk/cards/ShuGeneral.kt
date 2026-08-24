package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shu General
 * {3}{W}
 * Creature — Human Soldier
 * 2/2
 * Vigilance; horsemanship
 */
val ShuGeneral = card("Shu General") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance; horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.VIGILANCE, Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Li Xiaohua"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9c3be7a-82a8-411c-bfe2-16749ada1244.jpg"
    }
}
