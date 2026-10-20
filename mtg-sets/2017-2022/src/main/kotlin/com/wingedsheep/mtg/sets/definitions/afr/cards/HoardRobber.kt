package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Hoard Robber
 * {1}{B}
 * Creature — Tiefling Rogue
 * 1/3
 * Whenever this creature deals combat damage to a player, create a Treasure token.
 */
val HoardRobber = card("Hoard Robber") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Tiefling Rogue"
    oracleText = "Whenever this creature deals combat damage to a player, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"
    power = 1
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTreasure()
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Anna Pavleeva"
        flavorText = "\"And to think, a year ago I was picking pockets in Waterdeep.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b723d9cd-05f5-4894-9f2b-52a434a3019c.jpg?1783926493"
    }
}
