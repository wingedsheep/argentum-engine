package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enduring Victory
 * {4}{W}
 * Instant
 * Destroy target attacking or blocking creature. Bolster 1. (Choose a creature with the least
 * toughness among creatures you control and put a +1/+1 counter on it.)
 */
val EnduringVictory = card("Enduring Victory") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target attacking or blocking creature. Bolster 1. (Choose a creature with the " +
        "least toughness among creatures you control and put a +1/+1 counter on it.)"

    spell {
        val creature = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.Destroy(creature) then Patterns.Mechanic.bolster(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Mike Sass"
        flavorText = "\"My dragonlord is immortal. Most dragons are not.\"\n—Kadri, Dromoka warrior"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54fef763-7ee2-4341-9c67-546e4b6710b7.jpg?1783938617"
    }
}
