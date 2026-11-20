package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spike Jester
 * {B}{R}
 * Creature — Goblin Warrior
 * 3/1
 * Haste
 */
val SpikeJester = card("Spike Jester") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Goblin Warrior"
    power = 3
    toughness = 1
    oracleText = "Haste"

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Ryan Barger"
        flavorText = "For the Rakdos, \"suicidal charge\" and \"good showmanship\" are one and the same."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cec50499-70d4-4dc1-9cae-abbecfc8e87d.jpg?1783940021"
    }
}
