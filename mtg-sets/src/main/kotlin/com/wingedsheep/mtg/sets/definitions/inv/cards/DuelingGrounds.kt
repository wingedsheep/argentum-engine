package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackerCountLimit
import com.wingedsheep.sdk.scripting.BlockerCountLimit

/**
 * Dueling Grounds
 * {1}{G}{W}
 * Enchantment
 * No more than one creature can attack each combat.
 * No more than one creature can block each combat.
 */
val DuelingGrounds = card("Dueling Grounds") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment"
    oracleText = "No more than one creature can attack each combat.\n" +
        "No more than one creature can block each combat."

    staticAbility {
        ability = AttackerCountLimit(maxAttackers = 1)
    }

    staticAbility {
        ability = BlockerCountLimit(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Pete Venters"
        flavorText = "\"You think you can stop me?\" hissed Tsabo. \"I think I can kill you,\" replied Gerrard."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52760183-bee0-4ce0-96c0-074b88f78980.jpg?1562911742"
    }
}
