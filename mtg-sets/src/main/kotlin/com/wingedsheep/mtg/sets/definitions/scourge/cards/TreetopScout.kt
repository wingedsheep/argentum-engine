package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptByKeyword

/**
 * Treetop Scout
 * {G}
 * Creature — Elf Scout
 * 1/1
 * Treetop Scout can't be blocked except by creatures with flying.
 */
val TreetopScout = card("Treetop Scout") {
    manaCost = "{G}"
    typeLine = "Creature — Elf Scout"
    power = 1
    toughness = 1
    oracleText = "Treetop Scout can't be blocked except by creatures with flying."

    staticAbility {
        ability = CantBeBlockedExceptByKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Alan Pollack"
        flavorText = "At home among the swaying, supple branches of the treetops, some scouts live their entire lives never touching ground."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fa39646-a609-4b37-b8de-97893ae43c49.jpg?1562527114"
    }
}
