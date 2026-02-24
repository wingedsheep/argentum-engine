package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Accelerated Mutation
 * {3}{G}{G}
 * Instant
 * Target creature gets +X/+X until end of turn, where X is the highest
 * mana value among permanents you control.
 */
val AcceleratedMutation = card("Accelerated Mutation") {
    manaCost = "{3}{G}{G}"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/+X until end of turn, where X is the highest mana value among permanents you control."

    spell {
        target = Targets.Creature
        effect = Effects.ModifyStats(
            power = DynamicAmounts.battlefield(Player.You).maxManaValue(),
            toughness = DynamicAmounts.battlefield(Player.You).maxManaValue()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Edward P. Beard, Jr."
        flavorText = "The Mirari's destruction fed the land with waves of mutating energy."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/282f808c-0b58-4b98-aeda-f606a10d1a4b.jpg?1562526627"
    }
}
