package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Warren Elder
 * {1}{W}
 * Creature — Rabbit Cleric
 * 2/2
 *
 * {3}{W}: Creatures you control get +1/+1 until end of turn.
 */
val WarrenElder = card("Warren Elder") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Rabbit Cleric"
    power = 2
    toughness = 2
    oracleText = "{3}{W}: Creatures you control get +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = EffectPatterns.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Kaitlyn McCulley"
        flavorText = "\"There is strength in numbers, yes. More importantly, there is joy.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bf20069-5a20-4f95-976b-6af2b69f3ad0.jpg?1721425988"
    }
}
