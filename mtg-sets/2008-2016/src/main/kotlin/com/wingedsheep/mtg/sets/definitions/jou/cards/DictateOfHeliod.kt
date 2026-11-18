package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dictate of Heliod
 * {3}{W}{W}
 * Enchantment
 *
 * Flash
 * Creatures you control get +2/+2.
 */
val DictateOfHeliod = card("Dictate of Heliod") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Flash\nCreatures you control get +2/+2."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Terese Nielsen"
        flavorText = "\"In our war Heliod gave mortals some favor, yet other times he withheld aid. " +
            "Are we still no more than game pieces to him?\"\n—Polyxene the Doubter"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95602d52-988c-472b-ba8e-cf25aa92f1fc.jpg?1783939462"
    }
}
