package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Squelching Leeches
 * {2}{B}{B}
 * Creature — Leech
 * * / *
 *
 * Squelching Leeches's power and toughness are each equal to the number of Swamps you control.
 */
val SquelchingLeeches = card("Squelching Leeches") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Leech"
    oracleText = "Squelching Leeches's power and toughness are each equal to the number of Swamps you control."

    // */* = the number of Swamps you control (a characteristic-defining ability).
    dynamicStats(
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype(Subtype.SWAMP)).count()
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Svetlin Velinov"
        flavorText = "Leeches are sacred to followers of Pharika for drawing poison from a wound, but feared by everyone else for drawing blood from the flesh."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af3fff48-ebc9-4fca-a098-77d973e7d238.jpg?1783939430"
    }
}
