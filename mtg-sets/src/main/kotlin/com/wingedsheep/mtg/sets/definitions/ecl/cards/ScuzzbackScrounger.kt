package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.dsl.MiscPatterns

/**
 * Scuzzback Scrounger
 * {1}{R}
 * Creature — Goblin Warrior
 * 3/2
 *
 * At the beginning of your first main phase, you may blight 1. If you do,
 * create a Treasure token. (To blight 1, put a -1/-1 counter on a creature you control.)
 */
val ScuzzbackScrounger = card("Scuzzback Scrounger") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 3
    toughness = 2
    oracleText = "At the beginning of your first main phase, you may blight 1. " +
        "If you do, create a Treasure token. " +
        "(To blight 1, put a -1/-1 counter on a creature you control. " +
        "A Treasure token is an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = OptionalCostEffect(
            cost = MiscPatterns.blight(1),
            ifPaid = Effects.CreateTreasure(),
            descriptionOverride = "You may blight 1. If you do, create a Treasure token"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Mark Zug"
        flavorText = "The only metal polish he uses is blood."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ea4a895-19c0-47af-ad9c-5db88ea9ae05.jpg?1767952147"
    }
}
