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
        rarity = Rarity.RARE
    }
}
