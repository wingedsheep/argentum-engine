package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Prowler, Clawed Thief
 * {1}{U}{B}
 * Legendary Creature — Human Rogue Villain
 * 2/3
 * Menace
 * Connive — Whenever another Villain you control enters, Prowler, Clawed Thief connives.
 */
val ProwlerClawedThief = card("Prowler, Clawed Thief") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Rogue Villain"
    oracleText = "Menace\nConnive — Whenever another Villain you control enters, Prowler, Clawed Thief connives."
    power = 2
    toughness = 3

    keywords(Keyword.MENACE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Anthony Devine"
        flavorText = "\"It's nothing personal, kid. It's just money.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd31953a-7259-44e3-a94f-013bda68006d.jpg?1757377763"
    }
}
