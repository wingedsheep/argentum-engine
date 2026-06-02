package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Sultai Ascendancy
 * {B}{G}{U}
 * Enchantment
 * At the beginning of your upkeep, surveil 2.
 *
 * Oracle errata: Original text said "look at the top two cards..."
 * Updated to use the surveil keyword action.
 */
val SultaiAscendancy = card("Sultai Ascendancy") {
    manaCost = "{B}{G}{U}"
    colorIdentity = "UBG"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, surveil 2. (Look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)"

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = LibraryPatterns.surveil(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3314d77a-039f-43e4-a457-6ceba20c0ffe.jpg?1665819808"
    }
}
