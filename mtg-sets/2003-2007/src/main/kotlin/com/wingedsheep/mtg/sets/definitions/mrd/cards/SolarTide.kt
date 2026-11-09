package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Solar Tide
 * {4}{W}{W}
 * Sorcery
 * Choose one —
 * • Destroy all creatures with power 2 or less.
 * • Destroy all creatures with power 3 or greater.
 * Entwine—Sacrifice two lands. (Choose both if you pay the entwine cost.)
 */
val SolarTide = card("Solar Tide") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Destroy all creatures with power 2 or less.\n" +
        "• Destroy all creatures with power 3 or greater.\n" +
        "Entwine—Sacrifice two lands. (Choose both if you pay the entwine cost.)"

    entwine(Costs.additional.SacrificePermanent(GameObjectFilter.Land, count = 2))

    spell {
        entwineModal {
            mode(
                "Destroy all creatures with power 2 or less",
                Effects.DestroyAll(GameObjectFilter.Creature.powerAtMost(2)),
            )
            mode(
                "Destroy all creatures with power 3 or greater",
                Effects.DestroyAll(GameObjectFilter.Creature.powerAtLeast(3)),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Dave Dorman"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57ce33b6-267f-4ee8-a3f7-f41c619d0cfa.jpg?1783944559"
    }
}
