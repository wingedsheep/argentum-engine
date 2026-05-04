package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Drill Too Deep
 * {1}{R}
 * Instant
 * Choose one —
 * • Put five charge counters on target Spacecraft or Planet you control.
 * • Destroy target artifact.
 */
val DrillTooDeep = card("Drill Too Deep") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Put five charge counters on target Spacecraft or Planet you control.\n• Destroy target artifact."

    spell {
        modal(chooseCount = 1) {
            mode("Put five charge counters on target Spacecraft or Planet you control") {
                val t = target("target planet or spacecraft", TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype("Planet") or GameObjectFilter.Permanent.withSubtype("Spacecraft"))))
                effect = AddCountersEffect(Counters.CHARGE, 5, t)
            }
            
            mode("Destroy target artifact") {
                val t = target("target artifact", Targets.Artifact)
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Bartek Fedyczak"
        flavorText = "The rapacious appetite that ended their world became the only means by which the Kav could lift their civilization from it."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9d3b6e8-47c8-49e3-b204-8b659e127bde.jpg?1752947092"
    }
}
