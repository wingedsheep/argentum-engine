package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Timid Shieldbearer
 * {1}{W}
 * Creature — Kithkin Soldier
 * 2/2
 *
 * {4}{W}: Creatures you control get +1/+1 until end of turn.
 */
val TimidShieldbearer = card("Timid Shieldbearer") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Kithkin Soldier"
    power = 2
    toughness = 2
    oracleText = "{4}{W}: Creatures you control get +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = EffectPatterns.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "Fear is especially potent through the thoughtweft. A lone kithkin's panic can motivate a doun."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c672d38-a1a6-4912-a9a6-b11e7bf0cc67.jpg?1767956979"
    }
}
