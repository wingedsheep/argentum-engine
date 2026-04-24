package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Noggle Robber
 * {1}{R/G}{R/G}
 * Creature — Noggle Rogue
 * 3/3
 *
 * When this creature enters or dies, create a Treasure token.
 */
val NoggleRobber = card("Noggle Robber") {
    manaCost = "{1}{R/G}{R/G}"
    typeLine = "Creature — Noggle Rogue"
    power = 3
    toughness = 3
    oracleText = "When this creature enters or dies, create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTreasure()
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Steve Ellis"
        flavorText = "Noggles have no concept of money, but they do have a high affinity for \"shiny.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0082ca96-10f3-4823-be16-117556b2afc3.jpg?1767864446"
    }
}
