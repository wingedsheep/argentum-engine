package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Desert Sandstorm
 * {2}{R}
 * Sorcery
 * Desert Sandstorm deals 1 damage to each creature.
 */
val DesertSandstorm = card("Desert Sandstorm") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Desert Sandstorm deals 1 damage to each creature."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.DealDamage(1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Xu Tan"
        flavorText = "While pursuing the remnants of Yuan Shao's forces into the Wuhan Desert, Cao Cao was temporarily turned back by a fierce sandstorm."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/588ad2bf-405d-4c36-b485-e415c22f2703.jpg"
    }
}
