package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sun-Dappled Celebrant
 * {4}{W}{W}
 * Creature — Treefolk Cleric
 * 5/6
 *
 * Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell
 * pays for {1} or one mana of that creature's color.)
 * Vigilance
 */
val SunDappledCelebrant = card("Sun-Dappled Celebrant") {
    manaCost = "{4}{W}{W}"
    typeLine = "Creature — Treefolk Cleric"
    power = 5
    toughness = 6
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Vigilance"

    keywords(Keyword.CONVOKE, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Steve Ellis"
        flavorText = "Eirdu's adherents celebrate the sun on cold days and create warmth through spirited dances and revelry."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91c715a5-6643-4064-a8a8-3e05dce15979.jpg?1767659096"
    }
}
