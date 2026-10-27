package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mahadi, Emporium Master
 * {1}{B}{R}
 * Legendary Creature — Devil
 * 3/3
 *
 * At the beginning of your end step, create a Treasure token for each creature that died this turn.
 *
 * "Each creature that died this turn" is game-wide — every player's creatures, tokens included —
 * so the count is [DynamicAmounts.creaturesDiedThisTurn] over [Player.Each] (the Scavenging Ghoul
 * count), read on resolution.
 */
val MahadiEmporiumMaster = card("Mahadi, Emporium Master") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Devil"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your end step, create a Treasure token for each creature " +
        "that died this turn. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateTreasure(DynamicAmounts.creaturesDiedThisTurn(Player.Each))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "282"
        artist = "Ilse Gort"
        flavorText = "\"Welcome to the Wandering Emporium. Feel free to browse the bazaar. We have " +
            "everything a lost soul could want, need, or afford.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3da2b1a-76c5-4d36-bdf8-025b86f61eb5.jpg?1783922689"
    }
}
